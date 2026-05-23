# wiki-audit-anvil-items.ps1
#
# Audits smithingplus/data/AnvilItem.java against the OSRS Wiki Smithing tables.
# For each of our 26 smithable-item names, verifies the name appears on the wiki's
# Smithing/Smithing_tables page (where the comprehensive bar-by-bar item list lives).
#
# Caveats:
#   - This is a name-presence check, NOT a widget childId check. ChildIds (e.g. Dagger = 9,
#     Plate body = 22) are forked from upstream VarrockAnvil v1.0.3; if those drift, this
#     audit won't catch it. Runtime widget-tree verification would (deferred to v0.4.0).
#   - Wiki page formatting is complex (multiple tier tables). The audit just searches the
#     raw wikitext for each item name; false positives possible if a name string appears
#     in a non-item context.
#
# Run:  pwsh ./tools/wiki-audit-anvil-items.ps1

$ErrorActionPreference = 'Stop'

# === Our data: smithable item names from AnvilItem.java (v0.1.0) ===
$ourItems = @(
    "Dagger",
    "Sword",
    "Scimitar",
    "Long sword",
    "2-hand sword",
    "Axe",
    "Mace",
    "Warhammer",
    "Battle axe",
    "Claws",
    "Chain body",
    "Plate legs",
    "Plate skirt",
    "Plate body",
    "Nails",
    "Medium helm",
    "Full helm",
    "Square shield",
    "Kite shield",
    "Oil lamp",
    "Dart tips",
    "Arrowtips",
    "Knives",
    "Bronze wire",
    "Bullseye lamp",
    "Bolts (unf)"
)

Write-Output "=== OSRS Wiki audit of AutoSmithingPlus AnvilItem.java ==="
Write-Output ""

# Fetch the Smithing page (contains the smithable-items tables inline).
# Earlier draft pointed at "Smithing/Smithing_tables" which returned 0 chars (page doesn't exist).
$uri = "https://oldschool.runescape.wiki/api.php?action=parse&page=Smithing&prop=wikitext&format=json"
try {
    $resp = Invoke-RestMethod -Uri $uri -UserAgent "AutoSmithingPlus-Audit/0.1" -ErrorAction Stop
    $wikitext = $resp.parse.wikitext.'*'
} catch {
    Write-Error "Failed to fetch wiki Smithing page: $_"
    exit 1
}
if ($wikitext.Length -lt 100) {
    Write-Error "Wiki Smithing page returned suspiciously short content ($($wikitext.Length) chars). Check page name."
    exit 1
}

Write-Output ("Wikitext length: {0} chars" -f $wikitext.Length)
Write-Output ""

# Search each item name in the wikitext. Normalize both sides (lowercase, strip spaces and
# hyphens) so wiki conventions like "Longsword" still match our "Long sword", "Platebody"
# matches "Plate body", etc.
function Normalize($s) {
    return ($s -replace '[\s\-_]', '').ToLower()
}
$normalizedWikitext = Normalize $wikitext

$missing = @()
$found = @()
foreach ($item in $ourItems) {
    # Strip "(unf)" type suffix and other annotations.
    $needle = $item -replace ' \(unf\)$', ''
    $normNeedle = Normalize $needle
    if ($normalizedWikitext.Contains($normNeedle)) {
        $found += $item
    } else {
        $missing += $item
    }
}

Write-Output "=== Items present on wiki Smithing tables page ($($found.Count) of $($ourItems.Count)) ==="
foreach ($f in $found) {
    Write-Output ("  + {0}" -f $f)
}
Write-Output ""

Write-Output "=== Items NOT found on wiki Smithing tables page ($($missing.Count)) ==="
if ($missing.Count -eq 0) {
    Write-Output "  (none) -- every item in AnvilItem.java appears on the wiki."
} else {
    foreach ($m in $missing) {
        Write-Output ("  - {0} <-- not found; possibly renamed or removed" -f $m)
    }
}
Write-Output ""

Write-Output "=== Summary ==="
Write-Output ("  Our items:        {0}" -f $ourItems.Count)
Write-Output ("  Found on wiki:    {0}" -f $found.Count)
Write-Output ("  Missing on wiki:  {0}  (investigate; may be a wiki rename or our typo)" -f $missing.Count)
