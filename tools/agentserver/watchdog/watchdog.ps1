# Microbot Watchdog - polls the Agent Server, detects hangs, restarts the launcher.
#
# Hang detection covers four cases:
#   1. HTTP_TIMEOUT          - Agent Server unresponsive (total client freeze)
#   2. GAME_STATE_FROZEN     - stuck in LOGIN_SCREEN / HOPPING for too long
#   3. LOGGED_OUT_TOO_LONG   - not logged in but scripts active (login loop fail)
#   4. POSITION_FROZEN       - player position unchanged while script active
#   5. XP_FROZEN             - total XP unchanged while script active (soft hang)
#
# On detection: optionally notify Discord, kill matching processes, relaunch.
#
# Configuration via config.local.ps1 (preferred) or config.example.ps1 (fallback).
# Run with: .\watchdog.ps1  (or use start-watchdog.cmd)
# Stop with: Ctrl+C

param(
    [string]$ConfigPath = ""
)

# === Locate config ===
$scriptDir     = Split-Path -Parent $MyInvocation.MyCommand.Path
$localConfig   = Join-Path $scriptDir "config.local.ps1"
$exampleConfig = Join-Path $scriptDir "config.example.ps1"

if ($ConfigPath -and (Test-Path $ConfigPath)) {
    . $ConfigPath
    $cfgSrc = $ConfigPath
} elseif (Test-Path $localConfig) {
    . $localConfig
    $cfgSrc = $localConfig
} elseif (Test-Path $exampleConfig) {
    . $exampleConfig
    $cfgSrc = "$exampleConfig (defaults; copy to config.local.ps1 to customize)"
    Write-Warning "Using config.example.ps1 - copy to config.local.ps1 to customize"
} else {
    Write-Error "No config file found. Expected $localConfig or $exampleConfig"
    exit 1
}

# === Helpers ===

function Get-AgentToken {
    if (-not (Test-Path $TokenPath)) { return $null }
    return (Get-Content -LiteralPath $TokenPath -Raw).Trim()
}

function Invoke-Agent($path) {
    $token = Get-AgentToken
    if (-not $token) { return $null }
    try {
        $resp = Invoke-WebRequest `
            -Uri "$AgentUrl$path" `
            -Headers @{"X-Agent-Token" = $token} `
            -TimeoutSec $HttpTimeoutSec `
            -UseBasicParsing `
            -ErrorAction Stop
        return ($resp.Content | ConvertFrom-Json)
    } catch {
        return $null
    }
}

function Test-LauncherRunning {
    $found = Get-Process | Where-Object { $_.ProcessName -like "*Microbot Launcher*" }
    return ($found -ne $null -and @($found).Count -gt 0)
}

function Send-DiscordNotification($message) {
    if (-not $DiscordWebhookUrl) { return }
    try {
        $body = @{ content = $message } | ConvertTo-Json
        Invoke-WebRequest `
            -Uri $DiscordWebhookUrl `
            -Method POST `
            -Body $body `
            -ContentType "application/json" `
            -TimeoutSec 10 `
            -UseBasicParsing | Out-Null
    } catch {
        Write-Warning "Discord notification failed: $($_.Exception.Message)"
    }
}

function Append-RestartLog($reason, $details) {
    try {
        $logDir = Split-Path -Parent $LogCsvPath
        if (-not (Test-Path $logDir)) {
            New-Item -ItemType Directory -Path $logDir -Force | Out-Null
        }
        if (-not (Test-Path $LogCsvPath)) {
            "timestamp,reason,details" | Out-File -FilePath $LogCsvPath -Encoding utf8
        }
        $ts = (Get-Date).ToString("o")
        # CSV-escape: replace commas + newlines in details
        $cleanDetails = ($details -replace ',', ';') -replace '\r?\n', ' '
        "$ts,$reason,$cleanDetails" | Out-File -FilePath $LogCsvPath -Encoding utf8 -Append
    } catch {
        Write-Warning "CSV log write failed: $($_.Exception.Message)"
    }
}

function Restart-Microbot($reason, $details) {
    $stamp = (Get-Date).ToString('HH:mm:ss')
    Write-Host ""
    Write-Host "[$stamp] === RESTART TRIGGERED: $reason ===" -ForegroundColor Yellow
    Write-Host "  Details: $details" -ForegroundColor Gray

    Send-DiscordNotification "Microbot watchdog restarting (`$reason`): $details at $stamp"
    Append-RestartLog $reason $details

    # Kill matching processes
    foreach ($pattern in $KillPatterns) {
        $matched = if ($pattern -eq "javaw" -and $JavawTitleFilter) {
            Get-Process | Where-Object {
                $_.ProcessName -eq $pattern -and $_.MainWindowTitle -like "*$JavawTitleFilter*"
            }
        } else {
            Get-Process | Where-Object { $_.ProcessName -like "*$pattern*" }
        }

        foreach ($p in @($matched)) {
            try {
                Write-Host "  Killing $($p.ProcessName) (PID $($p.Id))" -ForegroundColor Gray
                Stop-Process -Id $p.Id -Force -ErrorAction Stop
            } catch {
                Write-Warning "  Failed to kill PID $($p.Id): $($_.Exception.Message)"
            }
        }
    }

    Start-Sleep -Seconds 5

    # Relaunch
    if (Test-Path $LauncherPath) {
        Write-Host "  Relaunching: $LauncherPath" -ForegroundColor Gray
        try {
            Start-Process -FilePath $LauncherPath
            return $true
        } catch {
            Write-Warning "  Relaunch failed: $($_.Exception.Message)"
            Append-RestartLog "RESTART_FAILED" $_.Exception.Message
            return $false
        }
    } else {
        Write-Warning "  Launcher path not found: $LauncherPath"
        Append-RestartLog "RESTART_FAILED" "Launcher path missing: $LauncherPath"
        return $false
    }
}

# === State trackers ===
$lastPosition          = $null
$positionFrozenSince   = $null
$lastTotalXp           = $null
$xpFrozenSince         = $null
$lastGameState         = $null
$gameStateFrozenSince  = $null
$loggedOutSince        = $null
$lastRestartTime       = $null
$restartCount          = 0

# === Boot ===
Write-Host "" -ForegroundColor Cyan
Write-Host "=== Microbot Watchdog ===" -ForegroundColor Cyan
Write-Host "  Config:     $cfgSrc"
Write-Host "  Agent URL:  $AgentUrl"
Write-Host "  Launcher:   $LauncherPath"
Write-Host "  Token file: $TokenPath"
Write-Host "  Poll every: ${PollIntervalSec}s"
Write-Host "  Thresholds:"
Write-Host "    HTTP timeout:        ${HttpTimeoutSec}s"
Write-Host "    Position frozen:     ${PositionFreezeThresh}s"
Write-Host "    XP frozen:           ${XpFreezeThresh}s"
Write-Host "    Game state frozen:   ${GameStateFreezeThresh}s"
Write-Host "    Logged out too long: ${LoggedOutThresh}s"
Write-Host "    Restart grace:       ${RestartGracePeriodSec}s"
Write-Host "  Log file:   $LogCsvPath"
Write-Host "  Discord:    $(if ($DiscordWebhookUrl) { 'enabled' } else { 'disabled' })"
Write-Host "  Stop with Ctrl+C"
Write-Host ""

Append-RestartLog "WATCHDOG_START" "Polling every ${PollIntervalSec}s"

# === Main loop ===
while ($true) {
    $now = Get-Date

    # Grace period after a restart
    if ($lastRestartTime -and ($now - $lastRestartTime).TotalSeconds -lt $RestartGracePeriodSec) {
        if ($VerboseConsole) {
            $remaining = [int]($RestartGracePeriodSec - ($now - $lastRestartTime).TotalSeconds)
            Write-Host "[$($now.ToString('HH:mm:ss'))] grace-period (${remaining}s remaining)" -ForegroundColor DarkGray
        }
        Start-Sleep -Seconds $PollIntervalSec
        continue
    }

    # Launcher not running = user intentionally stopped. Don't restart.
    if (-not (Test-LauncherRunning)) {
        if ($VerboseConsole) {
            Write-Host "[$($now.ToString('HH:mm:ss'))] launcher-not-running (no auto-restart)" -ForegroundColor DarkGray
        }
        $lastPosition = $null; $positionFrozenSince = $null
        $lastTotalXp = $null; $xpFrozenSince = $null
        $lastGameState = $null; $gameStateFrozenSince = $null
        $loggedOutSince = $null
        Start-Sleep -Seconds $PollIntervalSec
        continue
    }

    # Fetch state
    $state = Invoke-Agent "/state"

    if ($state -eq $null) {
        # Launcher running but Agent Server unresponsive = hard hang
        Write-Host "[$($now.ToString('HH:mm:ss'))] agent-unresponsive" -ForegroundColor Red
        if (Restart-Microbot "HTTP_TIMEOUT" "Agent Server unresponsive while launcher running") {
            $lastRestartTime = $now
            $restartCount++
            $lastPosition = $null; $positionFrozenSince = $null
            $lastTotalXp = $null; $xpFrozenSince = $null
            $lastGameState = $null; $gameStateFrozenSince = $null
            $loggedOutSince = $null
        }
        Start-Sleep -Seconds $PollIntervalSec
        continue
    }

    # Extract fields
    $loggedIn = $state.loggedIn
    $gameState = $state.gameState
    $positionStr = if ($state.player -and $state.player.position) {
        "$($state.player.position.x),$($state.player.position.y),$($state.player.position.plane)"
    } else { $null }

    # Determine if any script is active
    $scripts = Invoke-Agent "/scripts"
    $activeScripts = @()
    if ($scripts -and $scripts.scripts) {
        $activeScripts = @($scripts.scripts | Where-Object { $_.active })
    }
    $hasActiveScript = ($activeScripts.Count -gt 0)

    # --- Detection: game state freeze (LOGIN_SCREEN / HOPPING stuck) ---
    if ($gameState -eq $lastGameState -and ($gameState -eq "LOGIN_SCREEN" -or $gameState -eq "HOPPING")) {
        if ($gameStateFrozenSince -eq $null) { $gameStateFrozenSince = $now }
        $duration = ($now - $gameStateFrozenSince).TotalSeconds
        if ($duration -gt $GameStateFreezeThresh) {
            if (Restart-Microbot "GAME_STATE_FROZEN" "Stuck in $gameState for $([int]$duration)s") {
                $lastRestartTime = $now
                $restartCount++
                $gameStateFrozenSince = $null
            }
            Start-Sleep -Seconds $PollIntervalSec
            continue
        }
    } else {
        $gameStateFrozenSince = $null
    }
    $lastGameState = $gameState

    # --- Detection: logged out but scripts running ---
    if (-not $loggedIn -and $hasActiveScript) {
        if ($loggedOutSince -eq $null) { $loggedOutSince = $now }
        $duration = ($now - $loggedOutSince).TotalSeconds
        if ($duration -gt $LoggedOutThresh) {
            if (Restart-Microbot "LOGGED_OUT_TOO_LONG" "loggedIn=false for $([int]$duration)s while $($activeScripts.Count) script(s) active") {
                $lastRestartTime = $now
                $restartCount++
                $loggedOutSince = $null
            }
            Start-Sleep -Seconds $PollIntervalSec
            continue
        }
    } else {
        $loggedOutSince = $null
    }

    # --- Detection: position frozen (only if logged in + active script) ---
    if ($loggedIn -and $hasActiveScript -and $positionStr) {
        if ($positionStr -eq $lastPosition) {
            if ($positionFrozenSince -eq $null) { $positionFrozenSince = $now }
            $duration = ($now - $positionFrozenSince).TotalSeconds
            if ($duration -gt $PositionFreezeThresh) {
                if (Restart-Microbot "POSITION_FROZEN" "Position $positionStr unchanged $([int]$duration)s with active script") {
                    $lastRestartTime = $now
                    $restartCount++
                    $positionFrozenSince = $null
                }
                Start-Sleep -Seconds $PollIntervalSec
                continue
            }
        } else {
            $positionFrozenSince = $null
            $lastPosition = $positionStr
        }
    } else {
        $positionFrozenSince = $null
        $lastPosition = $positionStr
    }

    # --- Detection: XP frozen (only if logged in + active script) ---
    if ($loggedIn -and $hasActiveScript) {
        $skillsResp = Invoke-Agent "/skills"
        if ($skillsResp -and $skillsResp.skills) {
            $totalXp = ($skillsResp.skills | Measure-Object -Property xp -Sum).Sum
            if ($lastTotalXp -ne $null -and $totalXp -eq $lastTotalXp) {
                if ($xpFrozenSince -eq $null) { $xpFrozenSince = $now }
                $duration = ($now - $xpFrozenSince).TotalSeconds
                if ($duration -gt $XpFreezeThresh) {
                    if (Restart-Microbot "XP_FROZEN" "Total XP unchanged $([int]$duration)s with active script") {
                        $lastRestartTime = $now
                        $restartCount++
                        $xpFrozenSince = $null
                    }
                    Start-Sleep -Seconds $PollIntervalSec
                    continue
                }
            } else {
                $xpFrozenSince = $null
                $lastTotalXp = $totalXp
            }
        }
    } else {
        $xpFrozenSince = $null
    }

    # --- Heartbeat output ---
    if ($VerboseConsole) {
        $statusFlag = "OK"
        if (-not $loggedIn) {
            $statusFlag = "LOGGED_OUT"
        } elseif (-not $hasActiveScript) {
            $statusFlag = "IDLE"
        } elseif ($positionFrozenSince) {
            $secs = [int]($now - $positionFrozenSince).TotalSeconds
            $statusFlag = "pos-frozen-${secs}s"
        } elseif ($xpFrozenSince) {
            $secs = [int]($now - $xpFrozenSince).TotalSeconds
            $statusFlag = "xp-frozen-${secs}s"
        }
        Write-Host "[$($now.ToString('HH:mm:ss'))] $statusFlag | gameState=$gameState pos=$positionStr active=$($activeScripts.Count) restarts=$restartCount" -ForegroundColor DarkGray
    }

    Start-Sleep -Seconds $PollIntervalSec
}
