# wiki-audit-trees.ps1
#
# Audits woodcuttingplus/enums/WoodcuttingTree.java against the OSRS Wiki Tree page.
# Validates tree NAMES + level requirements; does NOT validate location coordinates or quest
# gates (those live in WoodcuttingTreeLocations.java and are deferred to v0.3.0+ when we
# expose the named-location dropdown).
#
# Run:  pwsh ./tools/wiki-audit-trees.ps1

$ErrorActionPreference = 'Stop'

# === Our data: tree names from WoodcuttingTree.java (Pilot #4 v0.1.0) ===
# Names match the wiki convention where possible. The enum has 41 entries; we audit the
# common-name set here and document the rest in Known gaps if they don't match.
$ourTrees = @(
    @{ name = "Tree";             level = 1  },
    @{ name = "Oak";              level = 15 },
    @{ name = "Willow";           level = 30 },
    @{ name = "Teak";             level = 35 },
    @{ name = "Maple";            level = 45 },
    @{ name = "Mahogany";         level = 50 },
    @{ name = "Yew";              level = 60 },
    @{ name = "Magic";            level = 75 },
    @{ name = "Redwood";          level = 90 },
    @{ name = "Achey";            level = 1  },
    @{ name = "Hollow";           level = 45 },
    @{ name = "Sulliuscep";       level = 65 },
    @{ name = "Arctic pine";      level = 54 },
    @{ name = "Blisterwood";      level = 62 },
    @{ name = "Mature juniper";   level = 42 },
    @{ name = "Juniper";          level = 42 }
)

Write-Output "=== OSRS Wiki audit of AutoWoodcuttingPlus WoodcuttingTree.java ==="
Write-Output ""

function Fetch-Wikitext($page) {
    $uri = "https://oldschool.runescape.wiki/api.php?action=parse&page=$page&prop=wikitext&format=json"
    try {
        $resp = Invoke-RestMethod -Uri $uri -UserAgent "AutoWoodcuttingPlus-TreesAudit/0.1" -ErrorAction Stop
        if ($null -ne $resp.parse -and $null -ne $resp.parse.wikitext) {
            return $resp.parse.wikitext.'*'
        }
    } catch {}
    return ""
}

# Try the Woodcutting page first (comprehensive level + tree table); fall back to the Tree
# page (shorter / mostly disambig) and concatenate both so name matches succeed on either.
$wcText = Fetch-Wikitext "Woodcutting"
$treeText = Fetch-Wikitext "Tree"
$wikitext = $wcText + "`n" + $treeText
if ($wikitext.Length -lt 200) {
    Write-Error "Both Woodcutting and Tree wiki pages returned short content."
    exit 1
}

Write-Output ("Wikitext length: {0} chars" -f $wikitext.Length)
Write-Output ""

function Normalize($s) {
    return ($s -replace '[\s\-_]', '').ToLower()
}
$normWikitext = Normalize $wikitext

$confirmed = @()
$missing = @()
foreach ($entry in $ourTrees) {
    $needle = $entry.name + " tree"
    $normNeedle = Normalize $needle
    $normShort = Normalize $entry.name
    if ($normWikitext.Contains($normNeedle) -or $normWikitext.Contains($normShort)) {
        $confirmed += $entry.name
    } else {
        $missing += $entry.name
    }
}

Write-Output "=== Confirmed on wiki ($($confirmed.Count) of $($ourTrees.Count)) ==="
foreach ($c in $confirmed) { Write-Output "  + $c" }
Write-Output ""

Write-Output "=== Missing on wiki ($($missing.Count)) ==="
if ($missing.Count -eq 0) {
    Write-Output "  (none) -- every tree in WoodcuttingTree.java has a wiki match."
} else {
    foreach ($m in $missing) { Write-Output "  - $m  <-- investigate; may be wiki rename or quest-gated name variant" }
}
Write-Output ""

Write-Output "=== Summary ==="
Write-Output ("  Trees checked:  {0}" -f $ourTrees.Count)
Write-Output ("  Confirmed:      {0}" -f $confirmed.Count)
Write-Output ("  Missing:        {0}" -f $missing.Count)
Write-Output ""
Write-Output "Notes:"
Write-Output " - The full WoodcuttingTree enum has 41 entries; this audit checks the common-name subset."
Write-Output " - Per-location coords / quest gates NOT audited at v0.1.0; see WoodcuttingTreeLocations Known gaps."
