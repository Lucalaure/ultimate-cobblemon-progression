<#
.SYNOPSIS
    Generates a region's tier_N.json files for UCP region mode.

.DESCRIPTION
    Reads one or more regional Pokedexes (e.g. cobblemon:kanto) and every species' base stats from the Cobblemon jar,
    ranks the region's species by Base Stat Total, and splits them into equal-sized tiers (weakest first).
    Each tier gets the level cap and unlock advancement given for it. region.json is not touched.

.EXAMPLE
    ./scripts/generate-region-tiers.ps1 -Dex cobblemon:kanto `
        -OutDir ../ucp-rct-regions/data/ultimate-cobblemon-progression/regions/kanto `
        -LevelCaps 15,21,24,29,43,43,47,50,54,100 `
        -Advancements "", "rctmod:trainers/defeat_leader_brock", ..., "rctmod:trainers/defeat_champion_terry"

.NOTES
    -LevelCaps and -Advancements have one entry per tier. Use "" for a tier with no requirement (normally tier 1).
    Without -CobblemonJar, the Cobblemon jar is looked up in the Gradle cache (run ./gradlew build once first).
#>
param(
    # One or more regional dexes, e.g. cobblemon:sinnoh,cobblemon:hisui. Regional-form entries (e.g. vulpix-alola) are skipped:
    # UCP tiers species, so a form always follows its base species' region.
    [Parameter(Mandatory = $true)][string[]]$Dex,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [Parameter(Mandatory = $true)][int[]]$LevelCaps,
    [Parameter(Mandatory = $true)][AllowEmptyString()][string[]]$Advancements,
    [string]$CobblemonJar = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ($LevelCaps.Count -ne $Advancements.Count) {
    throw "-LevelCaps has $($LevelCaps.Count) entries but -Advancements has $($Advancements.Count); give one of each per tier."
}
$tierCount = $LevelCaps.Count

if (-not $CobblemonJar) {
    $CobblemonJar = Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.cobblemon\fabric" -Recurse -Filter "fabric-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
    if (-not $CobblemonJar) { throw "Cobblemon jar not found in the Gradle cache; pass -CobblemonJar." }
}

function Read-JsonEntry($zip, [string]$path) {
    $entry = $zip.GetEntry($path)
    if (-not $entry) { return $null }
    $reader = New-Object System.IO.StreamReader($entry.Open())
    try { return $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Close() }
}

$zip = [System.IO.Compression.ZipFile]::OpenRead($CobblemonJar)
try {
    $entries = foreach ($d in $Dex) {
        $dexParts = $d -split ':'
        $dexJson = Read-JsonEntry $zip "data/$($dexParts[0])/dexes/$($dexParts[1]).json"
        if (-not $dexJson) { throw "Pokedex $d not found in $CobblemonJar" }
        $dexJson.entries
    }

    # species id path -> zip entry path
    $speciesFiles = @{}
    foreach ($e in $zip.Entries) {
        if ($e.FullName -match '^data/cobblemon/species/[^/]+/([^/]+)\.json$') { $speciesFiles[$Matches[1]] = $e.FullName }
    }

    $ranked = foreach ($id in ($entries | Select-Object -Unique)) {
        $name = ($id -split ':')[1]
        $file = $speciesFiles[$name]
        if (-not $file) { Write-Verbose "Skipping $id (a regional form, not a species)"; continue }
        $s = (Read-JsonEntry $zip $file).baseStats
        [pscustomobject]@{
            Id  = $id
            Bst = [int]$s.hp + [int]$s.attack + [int]$s.defence + [int]$s.special_attack + [int]$s.special_defence + [int]$s.speed
        }
    }
} finally {
    $zip.Dispose()
}

$ranked = @($ranked | Sort-Object Bst, Id)
if ($ranked.Count -eq 0) { throw "No species found for $Dex" }

New-Item -ItemType Directory -Force $OutDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)

for ($t = 1; $t -le $tierCount; $t++) {
    # Equal-count split: species at rank i go to tier floor(i * tiers / count) + 1.
    $inTier = for ($i = 0; $i -lt $ranked.Count; $i++) {
        if ([math]::Floor($i * $tierCount / $ranked.Count) + 1 -eq $t) { $ranked[$i] }
    }
    $inTier = @($inTier)
    $species = $inTier | ForEach-Object { $_.Id } | Sort-Object

    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("{")
    [void]$sb.AppendLine("  `"levelCap`": $($LevelCaps[$t - 1]),")
    if ($Advancements[$t - 1]) {
        [void]$sb.AppendLine("  `"requirements`": {")
        [void]$sb.AppendLine("    `"advancements`": [`"$($Advancements[$t - 1])`"]")
        [void]$sb.AppendLine("  },")
    }
    [void]$sb.AppendLine("  `"species`": [")
    [void]$sb.AppendLine((($species | ForEach-Object { "    `"$_`"" }) -join ",`n"))
    [void]$sb.AppendLine("  ]")
    [void]$sb.AppendLine("}")

    $path = Join-Path (Resolve-Path $OutDir) "tier_$t.json"
    [System.IO.File]::WriteAllText($path, $sb.ToString(), $utf8)
    $min = ($inTier | Measure-Object Bst -Minimum).Minimum
    $max = ($inTier | Measure-Object Bst -Maximum).Maximum
    Write-Host ("tier_{0}: {1,3} species, BST {2}-{3}, cap {4}" -f $t, $inTier.Count, $min, $max, $LevelCaps[$t - 1])
}
