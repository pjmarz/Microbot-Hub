# wiki-audit-furnaces.ps1
#
# Audits our smeltingplus/data/FurnaceLocations.java against the OSRS Wiki Furnace page.
# Catches two classes of bug:
#   - Phantom entries: a furnace we list that doesn't appear on the wiki's canonical list
#     (e.g. v0.1.0's "Varrock West Furnace" — Varrock has anvils, not a furnace)
#   - Missing entries: furnaces the wiki lists that we don't have (e.g. v0.1.1 added Lumbridge)
#
# Limitations:
#   - The wiki's Furnace locations table doesn't include exact WorldPoint coordinates,
#     so coordinate accuracy must be verified separately (in-game or via the wiki's
#     interactive map). This audit catches name-level discrepancies, not coordinate drift.
#   - Members/F2P status is partially auditable but the wiki uses inconsistent column
#     formats; we don't currently parse it.
#
# Run:  pwsh ./tools/wiki-audit-furnaces.ps1

$ErrorActionPreference = 'Stop'

# === Our data: furnace location names from FurnaceLocations.java (smeltingplus v0.1.1) ===
# Keep these in sync with the Furnace constants in FurnaceLocations.all().
$ourFurnaces = @(
    "Lumbridge",            # in our data as "Lumbridge Furnace"
    "Al Kharid",            # "Al Kharid Furnace"
    "Edgeville",            # "Edgeville Furnace"
    "Falador",              # "Falador West Furnace" (Falador on wiki)
    "Port Phasmatys",       # "Port Phasmatys Furnace"
    "Neitiznot",            # "Neitiznot Furnace"
    "Keldagrim",            # "Keldagrim Furnace"
    "Prifddinas",           # "Prifddinas Furnace"
    "Shilo Village",        # "Shilo Village Furnace"
    "Mor Ul Rek"            # "Mor Ul Rek Furnace"
)

Write-Output "=== OSRS Wiki audit of AutoSmeltingPlus FurnaceLocations.java ==="
Write-Output ""

# Fetch the wiki Furnace page wikitext.
$uri = "https://oldschool.runescape.wiki/api.php?action=parse&page=Furnace&prop=wikitext&format=json"
try {
    $resp = Invoke-RestMethod -Uri $uri -UserAgent "AutoSmeltingPlus-Audit/0.1" -ErrorAction Stop
    $wikitext = $resp.parse.wikitext.'*'
} catch {
    Write-Error "Failed to fetch wiki Furnace page: $_"
    exit 1
}

# Parse furnace location names. The wiki page lists them in a table; entries appear as
# either `{{plink|Name}}`, `{{plinkp|Name}}`, or `[[Name|display]]` style links.
# Capture anything that looks like a furnace location reference.
$wikiNames = @{}
$patterns = @(
    '\{\{plink\|([^}|]+)\}\}',
    '\{\{plinkp\|([^}|]+)\}\}',
    '\[\[([A-Z][^\]\|]+?)(?:\|[^\]]+)?\]\]'
)
foreach ($pattern in $patterns) {
    [regex]::Matches($wikitext, $pattern) | ForEach-Object {
        $name = $_.Groups[1].Value.Trim()
        # Filter out obvious non-location matches (templates, image refs, etc.)
        if ($name -match '^[A-Z][a-zA-Z]' -and $name.Length -lt 40 -and $name -notmatch '\.(png|gif|jpg|svg)$' -and $name -notmatch '^File:|^Image:|^Category:|^Update:|^Members') {
            $wikiNames[$name] = $true
        }
    }
}

# Now restrict to names that are actually in the Furnace page's "Locations" section.
# The locations section starts at ==Locations== or appears as a sortable table.
$sectionMatch = [regex]::Match($wikitext, '(?si)==Locations==(.*?)(?=\n==[^=]|\Z)')
$locationsSection = if ($sectionMatch.Success) { $sectionMatch.Groups[1].Value } else { $wikitext }

# Re-extract names from JUST the locations section.
$locationNames = @{}
foreach ($pattern in $patterns) {
    [regex]::Matches($locationsSection, $pattern) | ForEach-Object {
        $name = $_.Groups[1].Value.Trim()
        if ($name -match '^[A-Z][a-zA-Z]' -and $name.Length -lt 40 -and $name -notmatch '\.(png|gif|jpg|svg)$' -and $name -notmatch '^File:|^Image:|^Category:|^Update:|^Members|^Free-to-play$') {
            $locationNames[$name] = $true
        }
    }
}

Write-Output ("Found {0} candidate location names on wiki Furnace page" -f $locationNames.Count)
Write-Output ""

# === Diff: our entries vs wiki entries ===
$phantoms = @()
foreach ($ourName in $ourFurnaces) {
    if (-not $locationNames.ContainsKey($ourName)) {
        $phantoms += $ourName
    }
}

# Wiki entries we don't have — we curate by hand since most members-only entries don't apply
# to throwaway F2P testing, but we want them listed for v0.2+.
$potentialAdds = @()
foreach ($wikiName in ($locationNames.Keys | Sort-Object)) {
    if ($ourFurnaces -notcontains $wikiName) {
        $potentialAdds += $wikiName
    }
}

Write-Output "=== Phantoms (in our data but NOT in wiki Furnace locations list) ==="
if ($phantoms.Count -eq 0) {
    Write-Output "  (none) — every entry in FurnaceLocations.java has a wiki counterpart."
} else {
    foreach ($p in $phantoms) {
        Write-Output ("  - {0} <-- PHANTOM, remove from our data" -f $p)
    }
}
Write-Output ""

Write-Output "=== Wiki entries we don't have ($($potentialAdds.Count) candidates) ==="
Write-Output "Many are members-only or quest-locked — curate by hand for v0.2+ additions."
Write-Output "Inspect each carefully; the wiki name list also picks up some non-furnace links"
Write-Output "(achievement diaries, NPC names, etc.) that the regex couldn't filter."
Write-Output ""
foreach ($a in $potentialAdds | Sort-Object) {
    Write-Output ("  - {0}" -f $a)
}

Write-Output ""
Write-Output "=== Summary ==="
Write-Output ("  Our entries:      {0}" -f $ourFurnaces.Count)
Write-Output ("  Wiki candidates:  {0}" -f $locationNames.Count)
Write-Output ("  Phantoms:         {0}" -f $phantoms.Count)
Write-Output ("  Possible adds:    {0}  (mostly members-only; curate for v0.2+)" -f $potentialAdds.Count)
