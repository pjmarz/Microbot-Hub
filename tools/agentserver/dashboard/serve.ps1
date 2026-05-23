# Microbot Agent Server Dashboard - static file server + reverse proxy
#
# The Agent Server sets no CORS headers, so the browser can't fetch it directly
# from file:// or any cross-origin URL. This script serves the dashboard as
# same-origin static files AND proxies /api/* requests to the Agent Server,
# attaching the X-Agent-Token header from ~/.runelite/.agent-token.
#
# Usage:
#   cd tools/agentserver/dashboard
#   ./serve.ps1                # serves on http://localhost:8088
#   ./serve.ps1 -Port 9999     # custom port
#
# Then open http://localhost:8088/ in your browser.
# Stop with Ctrl+C.

param(
    [int]$Port = 8088,
    [int]$AgentPort = 8081,
    [string]$TokenPath = "$env:USERPROFILE\.runelite\.agent-token"
)

$root = $PSScriptRoot
$prefix = "http://localhost:$Port/"
$agentBase = "http://127.0.0.1:$AgentPort"

# Read the auth token at startup. Re-read on each proxied request so token
# rotation (plugin Reset → file rewritten) is picked up without restarting.
function Read-AgentToken {
    if (Test-Path -LiteralPath $TokenPath) {
        return (Get-Content -LiteralPath $TokenPath -Raw).Trim()
    }
    return $null
}

$initialToken = Read-AgentToken
if (-not $initialToken) {
    Write-Warning "Auth token file not found at $TokenPath."
    Write-Warning "Make sure the [M] Agent Server plugin is enabled at least once to generate it."
} else {
    Write-Host "Token loaded from $TokenPath (length: $($initialToken.Length))"
}

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add($prefix)
try {
    $listener.Start()
} catch {
    Write-Error "Failed to start listener on $prefix. Run as Admin or pick a different port."
    Write-Error $_.Exception.Message
    exit 1
}

Write-Host "Serving dashboard from $root at $prefix"
Write-Host "Proxying /api/* to $agentBase"
Write-Host "Open http://localhost:$Port/ in your browser"
Write-Host "Stop with Ctrl+C"

# MIME map for static files.
$mime = @{
    '.html' = 'text/html; charset=utf-8'
    '.css'  = 'text/css; charset=utf-8'
    '.js'   = 'application/javascript; charset=utf-8'
    '.json' = 'application/json; charset=utf-8'
    '.svg'  = 'image/svg+xml'
    '.png'  = 'image/png'
    '.ico'  = 'image/x-icon'
}

function Send-StaticFile($ctx, $relPath) {
    $resp = $ctx.Response
    if ([string]::IsNullOrEmpty($relPath)) { $relPath = 'index.html' }
    $filePath = Join-Path $root $relPath

    if (Test-Path -LiteralPath $filePath -PathType Leaf) {
        $ext = [System.IO.Path]::GetExtension($filePath).ToLower()
        $contentType = if ($mime.ContainsKey($ext)) { $mime[$ext] } else { 'application/octet-stream' }
        $bytes = [System.IO.File]::ReadAllBytes($filePath)
        $resp.ContentType = $contentType
        $resp.ContentLength64 = $bytes.Length
        $resp.OutputStream.Write($bytes, 0, $bytes.Length)
        Write-Host "200 GET /$relPath"
    } else {
        $resp.StatusCode = 404
        $msg = [System.Text.Encoding]::UTF8.GetBytes("404 Not Found: $relPath")
        $resp.OutputStream.Write($msg, 0, $msg.Length)
        Write-Host "404 GET /$relPath"
    }
}

function Send-Proxy($ctx, $relPath) {
    $resp = $ctx.Response
    $req = $ctx.Request
    $method = $req.HttpMethod
    $upstream = "$agentBase/$relPath"
    if ($req.Url.Query) { $upstream += $req.Url.Query }

    # Re-read token each request so rotation is transparent.
    $token = Read-AgentToken
    if (-not $token) {
        $resp.StatusCode = 503
        $msg = [System.Text.Encoding]::UTF8.GetBytes("Auth token unavailable at $TokenPath")
        $resp.OutputStream.Write($msg, 0, $msg.Length)
        Write-Host "503 $method /api/$relPath (no token)"
        return
    }

    try {
        $headers = @{ 'X-Agent-Token' = $token }
        $params = @{
            Uri             = $upstream
            Method          = $method
            Headers         = $headers
            UseBasicParsing = $true
            ErrorAction     = 'Stop'
        }

        # Forward request body for POST/PUT etc.
        if ($method -in @('POST', 'PUT', 'PATCH', 'DELETE')) {
            $reader = New-Object System.IO.StreamReader($req.InputStream, $req.ContentEncoding)
            $body = $reader.ReadToEnd()
            $reader.Close()
            if ($body) {
                $params['Body'] = $body
                $params['ContentType'] = $req.ContentType
            }
        }

        $upstreamResp = Invoke-WebRequest @params
        $resp.StatusCode = $upstreamResp.StatusCode
        $resp.ContentType = $upstreamResp.Headers['Content-Type']
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($upstreamResp.Content)
        $resp.ContentLength64 = $bytes.Length
        $resp.OutputStream.Write($bytes, 0, $bytes.Length)
        Write-Host "$($upstreamResp.StatusCode) $method /api/$relPath"
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 502 }
        $msg = "Proxy error ($status) on /api/$relPath - $($_.Exception.Message)"
        $resp.StatusCode = $status
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($msg)
        $resp.OutputStream.Write($bytes, 0, $bytes.Length)
        Write-Host "$status $method /api/$relPath (error: $($_.Exception.Message))"
    }
}

try {
    while ($listener.IsListening) {
        $ctx = $listener.GetContext()
        $relPath = $ctx.Request.Url.LocalPath.TrimStart('/')

        try {
            if ($relPath -like 'api/*') {
                Send-Proxy $ctx ($relPath.Substring(4))
            } elseif ($relPath -eq 'api') {
                # Edge case: /api with no trailing slash → forward to root.
                Send-Proxy $ctx ''
            } else {
                Send-StaticFile $ctx $relPath
            }
        } catch {
            Write-Host "ERROR handling request /$relPath - $($_.Exception.Message)"
        } finally {
            $ctx.Response.OutputStream.Close()
        }
    }
} finally {
    $listener.Stop()
    $listener.Close()
    Write-Host "Listener stopped"
}
