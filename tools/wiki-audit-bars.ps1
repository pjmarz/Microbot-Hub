# wiki-audit-bars.ps1
#
# Audits smeltingplus/data/Bars.java against the OSRS Wiki Smithing/Smelting bars page.
# Catches name renames, level changes, and missing/phantom bar entries.
#
# Run:  pwsh ./tools/wiki-audit-bars.ps1

$ErrorActionPreference = 'Stop'

# === Our data: bars from Bars.java (smeltingplus + smithingplus). Tuples of (name, level) ===
$ourBars = @(
    @{ name = "Bronze";     level = 1  },
    @{ name = "Iron";       level = 15 },
    @{ name = "Steel";      level = 30 },
    @{ name = "Gold";       level = 40 },
    @{ name = "Mithril";    level = 50 },
    @{ name = "Adamantite"; level = 70 },
    @{ name = "Runite";     level = 85 },
    @{ name = "Silver";     level = 20 },
    @{ name = "Blurite";    level = 13 }
)

Write-Output "=== OSRS Wiki audit of smeltingplus/data/Bars.java ==="
Write-Output ""

function Fetch-Wikitext($page) {
    $uri = "https://oldschool.runescape.wiki/api.php?action=parse&page=$page&prop=wikitext&format=json"
    try {
        $resp = Invoke-RestMethod -Uri $uri -UserAgent "AutoSmeltingPlus-BarsAudit/0.1" -ErrorAction Stop
        if ($null -ne $resp.parse -and $null -ne $resp.parse.wikitext) {
            return $resp.parse.wikitext.'*'
        }
    } catch {}
    return ""
}

$wikitext = Fetch-Wikitext "Smithing/Smelting_bars"
if ($wikitext.Length -lt 100) {
    Write-Output "Smithing/Smelting_bars not found; falling back to Smelting page..."
    $wikitext = Fetch-Wikitext "Smelting"
}
if ($wikitext.Length -lt 100) {
    Write-Output "Smelting not found; falling back to main Smithing page..."
    $wikitext = Fetch-Wikitext "Smithing"
}
if ($wikitext.Length -lt 100) {
    Write-Error "All wiki pages returned suspiciously short content."
    exit 1
}

Write-Output ("Wikitext length: {0} chars" -f $wikitext.Length)
Write-Output ""

# Search each bar name. Optional level check via nearby digit.
$confirmed = @()
$missing = @()
$levelMismatch = @()

foreach ($entry in $ourBars) {
    $needle = $entry.name + " bar"
    if ($wikitext -match "(?i)\b$([regex]::Escape($needle))\b") {
        $confirmed += $entry.name
        # Look for a level near this bar mention (within 200 chars).
        $idx = $wikitext.ToLower().IndexOf($needle.ToLower())
        $window = $wikitext.Substring([Math]::Max(0, $idx - 100), [Math]::Min(300, $wikitext.Length - [Math]::Max(0, $idx - 100)))
        $levelHits = [regex]::Matches($window, '(?<![\d])([1-9][0-9]?)(?![\d])')
        $foundLevel = $false
        foreach ($m in $levelHits) {
            if ([int]$m.Value -eq $entry.level) { $foundLevel = $true; break }
        }
        if (-not $foundLevel) {
            $levelMismatch += "$($entry.name) bar (expected level $($entry.level), no nearby match in wiki window)"
        }
    } else {
        $missing += $needle
    }
}

Write-Output "=== Confirmed on wiki ($($confirmed.Count) of $($ourBars.Count)) ==="
foreach ($c in $confirmed) { Write-Output "  + $c bar" }
Write-Output ""

Write-Output "=== Missing on wiki ($($missing.Count)) ==="
if ($missing.Count -eq 0) {
    Write-Output "  (none) -- every bar in Bars.java has a wiki match."
} else {
    foreach ($m in $missing) { Write-Output "  - $m  <-- investigate" }
}
Write-Output ""

Write-Output "=== Level mismatches ($($levelMismatch.Count)) ==="
if ($levelMismatch.Count -eq 0) {
    Write-Output "  (none, within window-based fuzzy check)."
} else {
    foreach ($lm in $levelMismatch) { Write-Output "  ? $lm" }
    Write-Output "  Note: fuzzy check; cross-verify these manually on the wiki page."
}
Write-Output ""

Write-Output "=== Summary ==="
Write-Output ("  Our bars:        {0}" -f $ourBars.Count)
Write-Output ("  Confirmed:       {0}" -f $confirmed.Count)
Write-Output ("  Missing:         {0}" -f $missing.Count)
Write-Output ("  Level warnings:  {0}  (fuzzy)" -f $levelMismatch.Count)
