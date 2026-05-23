# wiki-audit-anvils.ps1
#
# Audits smithingplus/data/AnvilLocations.java against the OSRS Wiki Anvil page.
# Catches:
#   - Phantom entries: anvils we list that the wiki doesn't have (e.g. a Lumbridge entry
#     based on the regular anvil page would be a phantom because Lumbridge has the RUSTED
#     variant only — which lives on its own page).
#   - Missing entries: anvils the wiki lists that we don't have yet (mostly members).
#
# KNOWN BLIND SPOT (caught by user 2026-05-14):
#   The wiki "Anvil" page only lists REGULAR anvils (object ID 2097). Variant anvils
#   (Rusted, Workbench, etc.) live on their own pages and are not in this audit. The
#   Lumbridge Rusted anvil (object ID 39620, bronze-only) is a real F2P smithing anvil
#   that this audit will NOT find. Add variant pages individually for v0.2.0+ hardening.
#
# Limitations:
#   - Wiki page doesn't include WorldPoint coordinates. Coordinate accuracy must be
#     verified separately (in-game inspection or by cross-reference with existing Hub
#     plugins like VarrockAnvil).
#   - Members/F2P status is partially auditable; the wiki uses inconsistent column formats.
#
# Run:  pwsh ./tools/wiki-audit-anvils.ps1

$ErrorActionPreference = 'Stop'

# === Our data: REGULAR anvils in AnvilLocations.java (v0.1.0). Lumbridge Rusted is a variant
# and lives on a separate wiki page; not part of this audit. ===
$ourAnvils = @(
    "Varrock"   # "Varrock West" / "Varrock (Western)" on the wiki
)

# Variant anvils we have but that AREN'T in the main Anvil locations table:
$variantAnvilsWeTrack = @(
    "Lumbridge Rusted Anvil (object ID 39620; from Rusted_anvil wiki page)"
)

Write-Output "=== OSRS Wiki audit of AutoSmithingPlus AnvilLocations.java ==="
Write-Output ""
Write-Output "NOTE: this audit covers REGULAR anvils only. Variant anvils tracked separately:"
foreach ($v in $variantAnvilsWeTrack) {
    Write-Output ("  + {0}" -f $v)
}
Write-Output ""

# Fetch the wiki Anvil page wikitext.
$uri = "https://oldschool.runescape.wiki/api.php?action=parse&page=Anvil&prop=wikitext&format=json"
try {
    $resp = Invoke-RestMethod -Uri $uri -UserAgent "AutoSmithingPlus-Audit/0.1" -ErrorAction Stop
    $wikitext = $resp.parse.wikitext.'*'
} catch {
    Write-Error "Failed to fetch wiki Anvil page: $_"
    exit 1
}

# Restrict to the Locations section.
$sectionMatch = [regex]::Match($wikitext, '(?si)==Locations==(.*?)(?=\n==[^=]|\Z)')
$locationsSection = if ($sectionMatch.Success) { $sectionMatch.Groups[1].Value } else { $wikitext }

$patterns = @(
    '\{\{plink\|([^}|]+)\}\}',
    '\{\{plinkp\|([^}|]+)\}\}',
    '\[\[([A-Z][^\]\|]+?)(?:\|[^\]]+)?\]\]'
)

$locationNames = @{}
foreach ($pattern in $patterns) {
    [regex]::Matches($locationsSection, $pattern) | ForEach-Object {
        $name = $_.Groups[1].Value.Trim()
        if ($name -match '^[A-Z][a-zA-Z]' -and $name.Length -lt 50 -and $name -notmatch '\.(png|gif|jpg|svg)$' -and $name -notmatch '^File:|^Image:|^Category:|^Update:|^Members|^Free-to-play$') {
            $locationNames[$name] = $true
        }
    }
}

Write-Output ("Found {0} candidate location names on wiki Anvil page" -f $locationNames.Count)
Write-Output ""

# === Diff: our entries vs wiki entries ===
$phantoms = @()
foreach ($ourName in $ourAnvils) {
    $hit = $false
    foreach ($wikiName in $locationNames.Keys) {
        if ($wikiName -like "*$ourName*") { $hit = $true; break }
    }
    if (-not $hit) {
        $phantoms += $ourName
    }
}

$potentialAdds = @()
foreach ($wikiName in ($locationNames.Keys | Sort-Object)) {
    $match = $false
    foreach ($ourName in $ourAnvils) {
        if ($wikiName -like "*$ourName*") { $match = $true; break }
    }
    if (-not $match) {
        $potentialAdds += $wikiName
    }
}

Write-Output "=== Phantoms (in our REGULAR anvil data but NOT in wiki Anvil page) ==="
if ($phantoms.Count -eq 0) {
    Write-Output "  (none) -- every regular-anvil entry in AnvilLocations.java has a wiki counterpart."
} else {
    foreach ($p in $phantoms) {
        Write-Output ("  - {0} <-- PHANTOM, investigate" -f $p)
    }
}
Write-Output ""

Write-Output "=== Wiki entries we don't have ($($potentialAdds.Count) candidates) ==="
Write-Output "Most are members-only or quest-locked. Curate for v0.2+ additions."
Write-Output "The list picks up some non-anvil links (NPC names, quest names) that the regex can't filter perfectly."
Write-Output ""
foreach ($a in $potentialAdds | Sort-Object) {
    Write-Output ("  - {0}" -f $a)
}

Write-Output ""
Write-Output "=== Summary ==="
Write-Output ("  Our regular anvils:  {0}" -f $ourAnvils.Count)
Write-Output ("  Our variant anvils:  {0}  (not audited via main Anvil page)" -f $variantAnvilsWeTrack.Count)
Write-Output ("  Wiki candidates:     {0}" -f $locationNames.Count)
Write-Output ("  Phantoms:            {0}" -f $phantoms.Count)
Write-Output ("  Possible adds:       {0}  (mostly members; curate for v0.2+)" -f $potentialAdds.Count)
