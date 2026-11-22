<#
.SYNOPSIS
Rebuilds Radical Cobblemon Trainers' trainer loot for CJP, from RCT's own loot tables.

.DESCRIPTION
Usage:
    powershell -File scripts/generate-trainer-loot.ps1 <rctmod jar>

Reads data/rctmod/loot_table from the RCT jar and writes overrides into the built-in leagues datapack
(src/main/resources/resourcepacks/kanto_league/data/rctmod/loot_table), replacing whatever the script wrote before.
Every trainer class keeps RCT's item categories (fishers still drop fishing gear, breeders evolution items). Changes:

- Every win drops at least one item: each level bracket has a pool with 1 roll plus a pool with 2 chance rolls.
- Rarity depends on the level of the trainer's strongest Pokemon (BRACKETS below), instead of being the same at
  every level. Epic items start at level 26 and legendary ones at 51. Luck (RCT raises it for each completed series)
  still pushes drops toward rarer tiers.
- Strong items need stronger trainers (CATEGORY_GATES and ENTRY_GATES).
- Filler (vanilla flowers, saplings, paintings, pottery sherds, armor trims, music discs...) is swapped for Cobblemon
  items (REMOVE and ADD).
- Team Rocket Boss Giovanni (Radical Red) no longer drops a guaranteed Master Ball.

Brackets line up with the gates, so a category is either offered for a whole bracket or not at all. A category is only
offered in a bracket if at least one of its items can drop for every level in it, so no roll comes up empty.
#>
param([Parameter(Mandatory = $true)][string]$RctJar)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$inv = [System.Globalization.CultureInfo]::InvariantCulture

$repo = Split-Path -Parent $PSScriptRoot
$out = Join-Path $repo 'src/main/resources/resourcepacks/kanto_league/data/rctmod/loot_table'

# ------------------------------------------------------------------ tuning

$TIERS = 'common', 'uncommon', 'rare', 'epic', 'legendary'

# Level of the trainer's strongest Pokemon -> chance of each bonus roll, and the share (%) of each rarity tier.
$BRACKETS = @(
    @{ min = 1;  max = 25;  p = 0.15; share = @{ common = 75; uncommon = 22; rare = 3;  epic = 0; legendary = 0 } },
    @{ min = 26; max = 39;  p = 0.25; share = @{ common = 60; uncommon = 28; rare = 9;  epic = 3; legendary = 0 } },
    @{ min = 40; max = 50;  p = 0.25; share = @{ common = 60; uncommon = 28; rare = 9;  epic = 3; legendary = 0 } },
    @{ min = 51; max = 75;  p = 0.35; share = @{ common = 50; uncommon = 30; rare = 13; epic = 6; legendary = 1 } },
    @{ min = 76; max = 100; p = 0.45; share = @{ common = 40; uncommon = 32; rare = 17; epic = 9; legendary = 2 } }
)

# How much a tier's weight grows with luck: weight * (1 + LUCK_BOOST * 8 * luck). One completed series gives luck 0.125.
$LUCK_BOOST = @{ uncommon = 0.25; rare = 0.5; epic = 0.5; legendary = 1.0 }

# Categories only offered from this trainer level.
$CATEGORY_GATES = @{
    'uncommon/evolution' = 26   # Fire, Water, Thunder, Leaf and Ice Stones
    'rare/evolution'     = 40   # Moon, Sun, Dusk, Dawn and Shiny Stones
    'epic/evolution'     = 40   # trade evolution items, sweets
    'rare/tms'           = 26   # RCT gated each TM at 30; the gate moves here, to a bracket edge
    'epic/tms'           = 26
}

# Items only dropped from this trainer level (other items in the same category still drop below it).
$ENTRY_GATES = @{
    'epic/medicine' = @{ 'cobblemon:rare_candy' = 40 }
    'epic/training' = @{ 'cobblemon:ability_capsule' = 40; 'cobblemon:ability_patch' = 40; 'cobblemon:lucky_egg' = 40 }
    'epic/battle'   = @{ 'cobblemon:assault_vest' = 51 }
}

# Level conditions removed from items (their category gate replaces them).
$STRIP_LEVEL_CONDITIONS = 'rare/tms', 'epic/tms'

# Filler removed, as regexes on the item id.
$REMOVE = @{
    'common/diverse'      = '^minecraft:(candle|painting|suspicious_stew|milk_bucket|beetroot_soup|mushroom_stew)$'
    'common/nature'       = '^minecraft:(.*_sapling|mangrove_propagule|allium|azure_bluet|blue_orchid|cornflower|dandelion|lily_of_the_valley|.*_tulip|oxeye_daisy|poppy|.*_mushroom|cactus|sea_pickle|pumpkin)$'
    'common/fishing'      = '^minecraft:(stick|string)$'
    'uncommon/archeology' = '^minecraft:.*_pottery_sherd$'
    'uncommon/diverse'    = '^minecraft:(cookie|flint|turtle_egg)$'
    'uncommon/nature'     = '^minecraft:(carrot|potato|melon_slice|pumpkin_pie)$'
    'rare/archeology'     = '^cobblemon:.*_sherd$'
    'rare/nature'         = '^minecraft:(beetroot_seeds|cocoa_beans|melon_seeds|pumpkin_seeds)$'
    'epic/archeology'     = '^minecraft:(.*_armor_trim_smithing_template|disc_fragment_5)$'
    'epic/diverse'        = '^minecraft:(.*_banner_pattern|firework_star|music_disc_.*|disc_fragment_5)$'
}

# Cobblemon items added in their place: id -> count range.
$ADD = @{
    'common/diverse'   = [ordered]@{ 'cobblemon:poke_ball' = 1, 3; 'cobblemon:exp_candy_xs' = 1, 2 }
    'common/nature'    = [ordered]@{ 'cobblemon:red_apricorn' = 1, 2; 'cobblemon:yellow_apricorn' = 1, 2; 'cobblemon:green_apricorn' = 1, 2; 'cobblemon:blue_apricorn' = 1, 2; 'cobblemon:pink_apricorn' = 1, 2 }
    'uncommon/diverse' = [ordered]@{ 'cobblemon:exp_candy_xs' = 2, 3 }
}

# ------------------------------------------------------------------ JSON

function ConvertTo-PrettyJson($value, [string]$indent = '') {
    $next = $indent + '  '
    if ($null -eq $value) { return 'null' }
    if ($value -is [bool]) { if ($value) { return 'true' } else { return 'false' } }
    if ($value -is [string]) {
        $s = $value.Replace('\', '\\').Replace('"', '\"').Replace("`n", '\n').Replace("`r", '\r').Replace("`t", '\t')
        return '"' + $s + '"'
    }
    if ($value -is [int] -or $value -is [long] -or $value -is [double] -or $value -is [decimal] -or $value -is [single]) {
        return ([System.Convert]::ToString($value, $inv))
    }
    if ($value -is [System.Collections.IDictionary]) {
        if ($value.Count -eq 0) { return '{}' }
        $parts = foreach ($k in $value.Keys) { $next + '"' + $k + '": ' + (ConvertTo-PrettyJson $value[$k] $next) }
        return "{`n" + ($parts -join ",`n") + "`n$indent}"
    }
    if ($value -is [System.Management.Automation.PSCustomObject]) {
        $props = @($value.PSObject.Properties)
        if ($props.Count -eq 0) { return '{}' }
        $parts = foreach ($p in $props) { $next + '"' + $p.Name + '": ' + (ConvertTo-PrettyJson $p.Value $next) }
        return "{`n" + ($parts -join ",`n") + "`n$indent}"
    }
    if ($value -is [System.Collections.IEnumerable]) {
        $items = @($value)
        if ($items.Count -eq 0) { return '[]' }
        $parts = foreach ($i in $items) { $next + (ConvertTo-PrettyJson $i $next) }
        return "[`n" + ($parts -join ",`n") + "`n$indent]"
    }
    throw "Can't write $($value.GetType()) as JSON"
}

function Write-Table([string]$relPath, $table) {
    $path = Join-Path $out $relPath
    New-Item -ItemType Directory -Force (Split-Path -Parent $path) | Out-Null
    [System.IO.File]::WriteAllText($path, (ConvertTo-PrettyJson $table) + "`n", (New-Object System.Text.UTF8Encoding $false))
}

# ------------------------------------------------------------------ read RCT's tables

$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $RctJar))
$tables = @{}
try {
    foreach ($e in $zip.Entries) {
        if ($e.FullName -match '^data/rctmod/loot_table/(.+)\.json$') {
            $reader = New-Object System.IO.StreamReader($e.Open())
            try { $tables[$Matches[1]] = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        }
    }
} finally { $zip.Dispose() }
if (-not $tables.ContainsKey('trainers/default')) { throw "$RctJar has no data/rctmod/loot_table/trainers/default.json" }

if (Test-Path $out) { Remove-Item -Recurse -Force $out }

# ------------------------------------------------------------------ item categories (generic/<tier>/<category>)

function New-LevelCondition([int]$min, [int]$max) {
    [ordered]@{ condition = 'rctmod:level_range'; range = [ordered]@{ min = $min; max = $max } }
}

# Can this item drop for every trainer level in the bracket?
function Test-AlwaysDrops($entry, $bracket) {
    foreach ($c in @($entry.conditions)) {
        if ($null -eq $c) { continue }
        if ($c.condition -ne 'rctmod:level_range') { return $false }
        if ($c.range.min -gt $bracket.min -or $c.range.max -lt $bracket.max) { return $false }
    }
    return $true
}

$categories = @{}   # 'tier/category' -> its (edited) entries
$editedCategories = 0
foreach ($key in @($tables.Keys | Where-Object { $_ -match '^generic/[a-z]+/[a-z]+$' } | Sort-Object)) {
    $id = $key.Substring('generic/'.Length)
    $table = $tables[$key]
    if (@($table.pools).Count -ne 1) { throw "$key has more than one pool" }
    $pool = $table.pools[0]
    $entries = New-Object System.Collections.ArrayList
    $changed = $false
    foreach ($entry in @($pool.entries)) {
        if ($REMOVE.ContainsKey($id) -and $entry.name -match $REMOVE[$id]) { $changed = $true; continue }
        if ($STRIP_LEVEL_CONDITIONS -contains $id -and $entry.conditions) {
            $kept = @($entry.conditions | Where-Object { $_.condition -ne 'rctmod:level_range' })
            if ($kept.Count) { $entry.conditions = $kept } else { $entry.PSObject.Properties.Remove('conditions') }
            $changed = $true
        }
        if ($ENTRY_GATES.ContainsKey($id) -and $ENTRY_GATES[$id].ContainsKey($entry.name)) {
            $gate = New-LevelCondition $ENTRY_GATES[$id][$entry.name] 100
            if ($entry.conditions) { $entry.conditions = @($entry.conditions) + $gate }
            else { $entry | Add-Member -NotePropertyName conditions -NotePropertyValue @($gate) }
            $changed = $true
        }
        [void]$entries.Add($entry)
    }
    if ($ADD.ContainsKey($id)) {
        foreach ($item in $ADD[$id].Keys) {
            $range = $ADD[$id][$item]
            [void]$entries.Add([ordered]@{
                type      = 'minecraft:item'
                name      = $item
                functions = @([ordered]@{ function = 'minecraft:set_count'; count = [ordered]@{ min = $range[0]; max = $range[1] } })
            })
        }
        $changed = $true
    }
    if ($entries.Count -eq 0) { throw "$key would be left empty" }
    $categories[$id] = $entries
    if ($changed) {
        $pool.entries = $entries.ToArray()
        Write-Table "$key.json" $table
        $editedCategories++
    }
}

# ------------------------------------------------------------------ trainer tables (default, groups)

function Get-CategoryIds($table) {
    $ids = New-Object System.Collections.Generic.List[string]
    foreach ($pool in @($table.pools)) {
        foreach ($entry in @($pool.entries)) {
            if ($entry.type -ne 'minecraft:loot_table' -or $entry.value -notmatch '^rctmod:generic/([a-z]+/[a-z]+)$') {
                throw "Unexpected entry in a trainer loot table: $($entry | ConvertTo-Json -Compress)"
            }
            if (-not $ids.Contains($Matches[1])) { $ids.Add($Matches[1]) }
        }
    }
    return $ids
}

function New-TrainerTable([string]$key, $categoryIds) {
    $pools = foreach ($b in $BRACKETS) {
        $offered = @($categoryIds | Where-Object {
            $gate = if ($CATEGORY_GATES.ContainsKey($_)) { $CATEGORY_GATES[$_] } else { 0 }
            $b.min -ge $gate -and (@($categories[$_] | Where-Object { Test-AlwaysDrops $_ $b }).Count -gt 0)
        })
        $entries = foreach ($tier in $TIERS) {
            $inTier = @($offered | Where-Object { $_.StartsWith("$tier/") })
            if ($b.share[$tier] -eq 0 -or $inTier.Count -eq 0) { continue }
            $weight = [math]::Max(1, [math]::Round($b.share[$tier] * 100 / $inTier.Count))
            foreach ($id in $inTier) {
                $entry = [ordered]@{ type = 'minecraft:loot_table'; value = "rctmod:generic/$id"; weight = [int]$weight }
                if ($LUCK_BOOST.ContainsKey($tier)) { $entry.quality = [int][math]::Round($weight * $LUCK_BOOST[$tier] * 8) }
                $entry
            }
        }
        if (@($entries).Count -eq 0) { throw "$key offers nothing for levels $($b.min)-$($b.max)" }
        $condition = New-LevelCondition $b.min $b.max
        [ordered]@{ rolls = 1; entries = @($entries); conditions = @($condition) }
        [ordered]@{ rolls = [ordered]@{ type = 'minecraft:binomial'; n = 2; p = $b.p }; entries = @($entries); conditions = @($condition) }
    }
    [ordered]@{ pools = @($pools); random_sequence = "rctmod:$key" }
}

$written = 0
foreach ($key in @($tables.Keys | Where-Object { $_ -eq 'trainers/default' -or $_ -like 'trainers/groups/*' } | Sort-Object)) {
    Write-Table "$key.json" (New-TrainerTable $key (Get-CategoryIds $tables[$key]))
    $written++
}

# ------------------------------------------------------------------ single trainers: drop guaranteed Master Balls

foreach ($key in @($tables.Keys | Where-Object { $_ -like 'trainers/single/*' } | Sort-Object)) {
    $table = $tables[$key]
    $pools = @($table.pools | Where-Object { -not (@($_.entries) | Where-Object { $_.value -eq 'rctmod:generic/legendary/masterball' }) })
    if ($pools.Count -ne @($table.pools).Count) {
        $table.pools = $pools
        Write-Table "$key.json" $table
        Write-Host "Removed the Master Ball from $key"
    }
}

Write-Host "Wrote $written trainer loot tables and $editedCategories edited item categories to $out"
