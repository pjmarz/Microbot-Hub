# wiki-audit-banks.ps1
#
# Audits our BankLocationOption.java (3 sibling copies under miningplus / smeltingplus /
# smithingplus -- they share the same dataset) against the OSRS Wiki Bank page.
# Catches two classes of bug:
#   - Phantom entries: a bank we list that doesn't appear on the wiki's canonical list
#     (e.g. a renamed-by-Jagex bank we silently kept)
#   - Missing entries: banks the wiki lists that we don't have (e.g. v0.3.3 added Fadli's
#     Al Kharid Arena bank after Pete called it out post-deploy; v0.4.0 added Ferox Enclave)
#
# Limitations:
#   - The wiki's Bank locations table doesn't include exact WorldPoint coordinates,
#     so coordinate accuracy must be verified separately (in-game or via the wiki's
#     interactive map). This audit catches name-level discrepancies, not coordinate drift.
#   - The Bank page is large and mixes regular banks, deposit boxes, bank chests, and
#     guild bank booths in different sections. We pull anything from the "Locations" /
#     "List of banks" sections that looks like a place-name link.
#   - Members vs F2P status is partially auditable but parser quality is inconsistent.
#
# Run:  pwsh ./tools/wiki-audit-banks.ps1

$ErrorActionPreference = 'Stop'

# === Our data: bank location display-prefixes from BankLocationOption.java (v0.4.0) ===
# Keep these in sync with the enum constants. Use a short prefix that's likely to appear
# on the wiki (e.g. "Lumbridge Castle" not "Lumbridge Castle (2F)").
$ourBanks = @(
    "Lumbridge Castle",          # LUMBRIDGE_CASTLE
    "Draynor Village",           # DRAYNOR_VILLAGE
    "Al Kharid",                 # AL_KHARID
    "Emir's Arena",              # AL_KHARID_ARENA (Fadli's bank, added v0.3.3)
    "Varrock West",              # VARROCK_WEST
    "Varrock East",              # VARROCK_EAST
    "Grand Exchange",            # GRAND_EXCHANGE
    "Edgeville",                 # EDGEVILLE
    "Falador West",              # FALADOR_WEST
    "Falador East",              # FALADOR_EAST
    "Port Sarim",                # PORT_SARIM (deposit box)
    "Ferox Enclave",             # FEROX_ENCLAVE (added v0.4.0)
    "Seers' Village",            # SEERS_VILLAGE
    "Catherby",                  # CATHERBY
    "Ardougne",                  # ARDOUGNE_NORTH + ARDOUGNE_SOUTH (wiki may show as "Ardougne")
    "Burgh de Rott",             # BURGH_DE_ROTT
    "Shilo Village",             # SHILO_VILLAGE
    "Mining Guild"               # MINING_GUILD_FALADOR
)

Write-Output "=== OSRS Wiki audit of Plus plugins BankLocationOption.java ==="
Write-Output ""

# Fetch the wiki Bank page wikitext.
$uri = "https://oldschool.runescape.wiki/api.php?action=parse&page=Bank&prop=wikitext&format=json"
try {
    $resp = Invoke-RestMethod -Uri $uri -UserAgent "AutoSkillPlus-Audit/0.4" -ErrorAction Stop
    $wikitext = $resp.parse.wikitext.'*'
} catch {
    Write-Error "Failed to fetch wiki Bank page: $_"
    exit 1
}

# Extract candidate place-names. The Bank page references locations via {{plink|...}},
# {{plinkp|...}}, or [[Name|display]] links.
$patterns = @(
    '\{\{plink\|([^}|]+)\}\}',
    '\{\{plinkp\|([^}|]+)\}\}',
    '\[\[([A-Z][^\]\|]+?)(?:\|[^\]]+)?\]\]'
)

# Restrict to the "Locations" / "List of banks" portion if it exists, else fall back to
# whole-page scan.
$sectionMatch = [regex]::Match($wikitext, '(?si)==\s*(?:List of banks|Locations|Bank locations)\s*==(.*?)(?=\n==[^=]|\Z)')
$locationsSection = if ($sectionMatch.Success) { $sectionMatch.Groups[1].Value } else { $wikitext }

$locationNames = @{}
foreach ($pattern in $patterns) {
    [regex]::Matches($locationsSection, $pattern) | ForEach-Object {
        $name = $_.Groups[1].Value.Trim()
        # Filter out obvious non-location matches.
        if ($name -match '^[A-Z][a-zA-Z]' -and $name.Length -lt 40 `
                -and $name -notmatch '\.(png|gif|jpg|svg)$' `
                -and $name -notmatch '^File:|^Image:|^Category:|^Update:|^Members$|^Free-to-play$|^Bank$|^Deposit box$|^Bank chest$|^Bank booth$|^Banker$|^Achievement|^Quest$') {
            $locationNames[$name] = $true
        }
    }
}

Write-Output ("Found {0} candidate location names on wiki Bank page" -f $locationNames.Count)
Write-Output ""

# === Diff: phantoms (our entries not on wiki) ===
$phantoms = @()
foreach ($ourName in $ourBanks) {
    $match = $false
    foreach ($wikiName in $locationNames.Keys) {
        if ($wikiName -like "*$ourName*" -or $ourName -like "*$wikiName*") {
            $match = $true
            break
        }
    }
    if (-not $match) {
        $phantoms += $ourName
    }
}

# === Diff: possible adds (wiki entries we don't have) ===
$potentialAdds = @()
foreach ($wikiName in ($locationNames.Keys | Sort-Object)) {
    $match = $false
    foreach ($ourName in $ourBanks) {
        if ($wikiName -like "*$ourName*" -or $ourName -like "*$wikiName*") {
            $match = $true
            break
        }
    }
    if (-not $match) {
        $potentialAdds += $wikiName
    }
}

Write-Output "=== Phantoms (in our data but NOT cleanly matched on wiki Bank page) ==="
if ($phantoms.Count -eq 0) {
    Write-Output "  (none) -- every entry in BankLocationOption.java has a wiki counterpart."
} else {
    foreach ($p in $phantoms) {
        Write-Output ("  - {0} <-- PHANTOM or fuzzy mismatch, manually verify" -f $p)
    }
}
Write-Output ""

Write-Output "=== Wiki entries we don't have ($($potentialAdds.Count) candidates) ==="
Write-Output "Many are members-only, deposit-box-only, or quest-locked -- curate by hand."
Write-Output "Inspect each carefully; the wiki name list also picks up non-bank links"
Write-Output "(NPC pages, region names, etc.) that the regex couldn't filter."
Write-Output ""
foreach ($a in $potentialAdds | Sort-Object) {
    Write-Output ("  - {0}" -f $a)
}

Write-Output ""
Write-Output "=== Summary ==="
Write-Output ("  Our entries:      {0}" -f $ourBanks.Count)
Write-Output ("  Wiki candidates:  {0}" -f $locationNames.Count)
Write-Output ("  Phantoms:         {0}" -f $phantoms.Count)
Write-Output ("  Possible adds:    {0}  (mostly members-only or false-positives; curate)" -f $potentialAdds.Count)
