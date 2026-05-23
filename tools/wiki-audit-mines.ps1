# wiki-audit-mines.ps1
#
# Audits our miningplus/data/MiningRockLocations.java against the OSRS Wiki.
# Pulls each named mine page via the MediaWiki API, parses the rocks table from
# the page's wikitext, and diffs the rock set against what our data file claims.
#
# Run:  pwsh ./tools/wiki-audit-mines.ps1
#
# Output: discrepancy report on stdout. Mines absent from either side are flagged.
# The wiki-side rock list is canonical; treat any mismatch as a data bug to fix
# in miningplus/data/MiningRockLocations.java.

$ErrorActionPreference = 'Stop'

# === Our data: { our mine name -> set of rocks } as encoded in MiningRockLocations.java (v0.1.3)
$ourData = @{
    "Lumbridge Swamp West Mine"           = @("Mithril", "Adamantite", "Coal")
    "Lumbridge Swamp East Mine"           = @("Tin", "Copper")
    "Varrock South West Mine"             = @("Tin", "Clay", "Iron", "Silver")
    "Al Kharid Mine"                      = @("Tin", "Copper", "Iron", "Silver", "Gold", "Mithril", "Adamantite", "Coal")
    "Dwarven Mine"                        = @("Tin", "Copper", "Clay", "Iron", "Coal", "Gold", "Mithril", "Adamantite")
    "Mining Guild (Members)"              = @("Iron", "Coal", "Mithril", "Adamantite", "Runite")
    "Varrock South East Mine"             = @("Iron", "Copper", "Tin")
    "Ardougne South East Mine"            = @("Iron", "Coal")
    "Bandit Camp Mine (Members)"          = @("Iron", "Coal", "Mithril", "Adamantite")
    "Barbarian Village Mine"              = @("Coal", "Tin")
    "Seers' Village Coal Trucks"          = @("Coal")
    "Crafting Guild"                      = @("Gold", "Clay", "Silver")
    "Shilo Village Gem Mine"              = @("Gem")
    "Neitiznot Mine"                      = @("Coal", "Runite")
    "Heroes' Guild Mine"                  = @("Runite", "Coal", "Mithril", "Adamantite")
    "Lava Maze Runite Mine (Wilderness)"  = @("Runite")
    "Weiss Basalt Mine"                   = @("Basalt")
}

# === Mapping: our internal mine name -> OSRS Wiki page title
$wikiTitles = @{
    "Lumbridge Swamp West Mine"           = "West Lumbridge Swamp mine"
    "Lumbridge Swamp East Mine"           = "East Lumbridge Swamp mine"
    "Varrock South West Mine"             = "South-west Varrock mine"
    "Al Kharid Mine"                      = "Al Kharid mine"
    "Dwarven Mine"                        = "Dwarven Mine"
    "Mining Guild (Members)"              = "Mining Guild"
    "Varrock South East Mine"             = "South-east Varrock mine"
    "Ardougne South East Mine"            = "South-east Ardougne mine"
    "Bandit Camp Mine (Members)"          = "Bandit Camp mine"
    "Barbarian Village Mine"              = "Barbarian Village mine"
    "Seers' Village Coal Trucks"          = "Coal Trucks"
    "Crafting Guild"                      = "Crafting Guild mine"
    "Shilo Village Gem Mine"              = "Shilo Village mine"
    "Neitiznot Mine"                      = "Central Fremennik Isles mine"
    "Heroes' Guild Mine"                  = "Heroes' Guild mine"
    "Lava Maze Runite Mine (Wilderness)"  = "Lava Maze runite mine"
    "Weiss Basalt Mine"                   = "Salt Mine"
}

# === Rock types we care about (from miningplus/data/Rocks.java)
$rockCanonical = @{
    "Tin"        = "Tin"
    "Copper"     = "Copper"
    "Clay"       = "Clay"
    "Iron"       = "Iron"
    "Silver"     = "Silver"
    "Coal"       = "Coal"
    "Gold"       = "Gold"
    "Gem"        = "Gem"
    "Mithril"    = "Mithril"
    "Adamantite" = "Adamantite"
    "Adamant"    = "Adamantite"  # wiki sometimes uses Adamant
    "Runite"     = "Runite"
    "Basalt"     = "Basalt"
}

function Get-WikitextRocks {
    param([string]$wikitext)

    # Restrict to the Rocks section to avoid false-positive prose matches.
    # Walk forward from ==Rocks== until a sibling-level section header (== Heading ==),
    # NOT a subsection (=== Heading ===). Matching `==[^=]` excludes triple-equals subheaders.
    $section = $wikitext
    $match = [regex]::Match($wikitext, '(?si)==\s*Rocks\s*==(.*?)(?=\n==[^=]|\Z)')
    if ($match.Success) {
        $section = $match.Groups[1].Value
    }

    $found = @{}
    foreach ($wikiName in $rockCanonical.Keys) {
        $pattern = "\[\[\s*$wikiName\s+rock"
        if ($section -match $pattern) {
            $canonical = $rockCanonical[$wikiName]
            $found[$canonical] = $true
        }
    }
    return @($found.Keys | Sort-Object)
}

function Get-WikiPageText {
    param([string]$title)
    $uri = "https://oldschool.runescape.wiki/api.php?action=parse&page=$([uri]::EscapeDataString($title))&prop=wikitext&format=json"
    try {
        $resp = Invoke-RestMethod -Uri $uri -UserAgent "AutoMiningPlus-Audit/0.1" -ErrorAction Stop
        if ($resp.parse) { return $resp.parse.wikitext.'*' }
    } catch {
        # Page may not exist under that title; surface a sentinel
    }
    return $null
}

Write-Output "=== OSRS Wiki audit of AutoMiningPlus MiningRockLocations.java ==="
Write-Output ""

$report = @()
foreach ($ourName in ($ourData.Keys | Sort-Object)) {
    $title = $wikiTitles[$ourName]
    $wikitext = Get-WikiPageText $title

    if (-not $wikitext) {
        $report += [pscustomobject]@{
            Mine = $ourName
            WikiTitle = $title
            Status = "WIKI_PAGE_NOT_FOUND"
            OurRocks = ($ourData[$ourName] | Sort-Object) -join ','
            WikiRocks = ""
            Discrepancies = "page lookup failed; check title mapping"
        }
        continue
    }

    $ourRocks  = @($ourData[$ourName] | Sort-Object | Get-Unique)
    $wikiRocks = @(Get-WikitextRocks -wikitext $wikitext)

    $missing = @($wikiRocks | Where-Object { $_ -notin $ourRocks })
    $extra   = @($ourRocks  | Where-Object { $_ -notin $wikiRocks })

    $discrepancies = @()
    if ($missing.Count -gt 0) { $discrepancies += "MISSING_FROM_OUR_DATA: $($missing -join ', ')" }
    if ($extra.Count   -gt 0) { $discrepancies += "EXTRA_IN_OUR_DATA: $($extra -join ', ')" }

    $report += [pscustomobject]@{
        Mine = $ourName
        WikiTitle = $title
        Status = if ($discrepancies.Count -eq 0) { "OK" } else { "MISMATCH" }
        OurRocks = $ourRocks -join ','
        WikiRocks = $wikiRocks -join ','
        Discrepancies = $discrepancies -join ' | '
    }
}

$ok = $report | Where-Object { $_.Status -eq 'OK' }
$mismatch = $report | Where-Object { $_.Status -eq 'MISMATCH' }
$notfound = $report | Where-Object { $_.Status -eq 'WIKI_PAGE_NOT_FOUND' }

Write-Output "=== Summary ==="
Write-Output ("  OK:        {0}" -f $ok.Count)
Write-Output ("  MISMATCH:  {0}" -f $mismatch.Count)
Write-Output ("  NOT FOUND: {0}" -f $notfound.Count)
Write-Output ""

if ($mismatch.Count -gt 0) {
    Write-Output "=== Mismatches ==="
    $mismatch | Format-Table Mine, OurRocks, WikiRocks, Discrepancies -AutoSize -Wrap
}

if ($notfound.Count -gt 0) {
    Write-Output "=== Wiki page lookup failures ==="
    $notfound | Format-Table Mine, WikiTitle -AutoSize
}

if ($ok.Count -gt 0) {
    Write-Output "=== OK ==="
    $ok | Format-Table Mine, OurRocks -AutoSize
}
