<#
.SYNOPSIS
Gives Kanto's wild trainers their original FireRed teams.

.DESCRIPTION
Usage:
    powershell -File scripts/generate-kanto-trainers.ps1 <rctmod jar> <Cobblemon jar> [-Pokefirered <folder>]

RCT's Kanto trainers are FireRed's own trainers (same people, places and Vs. Seeker rematches, numbered as FireRed
numbers them), but with Radical Red's rebuilt teams: harder, at higher levels, and about a quarter of their Pokemon
from later games. This script swaps in FireRed's teams for every trainer in Kanto's wild_trainers.json, writing
override files into the built-in leagues datapack (data/rctmod/trainers/<id>.json). RCT's name, AI and battle rules
are kept; the team, items and single/double battle come from FireRed.

FireRed's trainers are in scripts/data/kanto-trainers.json, from pret's FireRed decompilation
(https://github.com/pret/pokefirered): src/data/trainers.h, src/data/trainer_parties.h,
include/constants/opponents.h, src/data/pokemon/level_up_learnsets.h and level_up_learnset_pointers.h. With
-Pokefirered (a folder holding those five files), the data file is rebuilt first.

Each Pokemon gets FireRed's species, level, IVs (FireRed's 0-255 scale, turned into 0-31), held item and moves: its
custom moves when FireRed gives it some, otherwise the moves FireRed itself gives it, worked out from FireRed's own
learnsets (its level-up moves up to its level, the four most recent).
Gender, ability and nature come from Cobblemon's data (see lib/TrainerBuild.ps1). Each id is checked against
FireRed's trainer of the same number by name, so a mismatch is reported instead of getting the wrong team.
#>
param(
    [Parameter(Mandatory = $true)][string]$RctJar,
    [Parameter(Mandatory = $true)][string]$CobblemonJar,
    [string]$Pokefirered
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib/TrainerBuild.ps1')

$repo = Split-Path -Parent $PSScriptRoot
$dataFile = Join-Path $PSScriptRoot 'data/kanto-trainers.json'
$pack = Join-Path $repo 'src/main/resources/resourcepacks/kanto_league/data'
$trainerDir = Join-Path $pack 'rctmod/trainers'
$listFile = Join-Path $pack 'ultimate-cobblemon-progression/regions/kanto/wild_trainers.json'

# Generation 3 names that later games changed (pret name -> Cobblemon id).
$MOVE_NAMES = @{ hijumpkick = 'highjumpkick'; faintattack = 'feintattack'; smellingsalt = 'smellingsalts'
                 vicegrip = 'visegrip' }
$SPECIES_NAMES = @{ nidoranf = 'nidoranf'; nidoranm = 'nidoranm' }
# RCT ids whose class Radical Red made up for a FireRed slot; those trainers get FireRed's name instead.
$RENAMED = '^(gatekeeper|dragon_tamer|professor_oak)_'

# ------------------------------------------------------------------ 1. pret's FireRed source -> kanto-trainers.json

if ($Pokefirered) {
    $numbers = @{}
    foreach ($m in [regex]::Matches([IO.File]::ReadAllText((Join-Path $Pokefirered 'opponents.h')), '#define (TRAINER_\w+)\s+(\d+)')) {
        $numbers[$m.Groups[1].Value] = [int]$m.Groups[2].Value
    }
    # FireRed's level-up learnsets, for the moves of Pokemon without custom ones.
    $learnsetOf = @{}
    foreach ($m in [regex]::Matches([IO.File]::ReadAllText((Join-Path $Pokefirered 'level_up_learnset_pointers.h')), '\[SPECIES_(\w+)\] = (\w+),')) {
        $learnsetOf[$m.Groups[1].Value] = $m.Groups[2].Value
    }
    $learnsets = @{}
    foreach ($m in [regex]::Matches([IO.File]::ReadAllText((Join-Path $Pokefirered 'level_up_learnsets.h')), 'static const u16 (\w+)\[\] = \{(.*?)\};', 'Singleline')) {
        $learnsets[$m.Groups[1].Value] = @([regex]::Matches($m.Groups[2].Value, 'LEVEL_UP_MOVE\(\s*(\d+),\s*MOVE_(\w+)\)') |
                ForEach-Object { [pscustomobject]@{ level = [int]$_.Groups[1].Value; move = $_.Groups[2].Value.ToLower() } })
    }
    # What FireRed gives a trainer's Pokemon without custom moves: it learns its level-up moves in order up to its
    # level, forgetting the oldest once it knows four.
    function Get-FireRedMoves([string]$species, [int]$level) {
        $known = New-Object System.Collections.Generic.List[string]
        foreach ($l in @($learnsets[$learnsetOf[$species]])) {
            if ($l.level -gt $level -or $known.Contains($l.move)) { continue }
            if ($known.Count -eq 4) { $known.RemoveAt(0) }
            $known.Add($l.move)
        }
        return $known.ToArray()
    }

    $parties = @{}
    $partyText = [IO.File]::ReadAllText((Join-Path $Pokefirered 'trainer_parties.h'))
    # A party can't run into the next one: some are one-line placeholders ({DUMMY_TRAINER_MON};).
    foreach ($m in [regex]::Matches($partyText, 'sParty_(\w+)\[\] = \{((?:(?!sParty_).)*?)\};', 'Singleline')) {
        $mons = foreach ($mon in [regex]::Matches($m.Groups[2].Value, '\{\s*\.iv = (\d+),\s*\.lvl = (\d+),\s*\.species = SPECIES_(\w+),(.*?)\n    \}', 'Singleline')) {
            $rest = $mon.Groups[4].Value
            $entry = [ordered]@{ species = $mon.Groups[3].Value.ToLower(); level = [int]$mon.Groups[2].Value; iv = [int]$mon.Groups[1].Value }
            $item = [regex]::Match($rest, '\.heldItem = ITEM_(\w+)')
            if ($item.Success -and $item.Groups[1].Value -ne 'NONE') { $entry.item = $item.Groups[1].Value.ToLower() }
            $moves = [regex]::Match($rest, '\.moves = \{([^}]*)\}')
            if ($moves.Success) {
                $entry.moves = @([regex]::Matches($moves.Groups[1].Value, 'MOVE_(\w+)') | ForEach-Object { $_.Groups[1].Value.ToLower() } |
                        Where-Object { $_ -ne 'none' })
            } else {
                $entry.moves = @(Get-FireRedMoves $mon.Groups[3].Value ([int]$mon.Groups[2].Value))
            }
            $entry
        }
        $parties[$m.Groups[1].Value] = @($mons)
    }
    $trainers = New-Object System.Collections.Generic.List[object]
    foreach ($m in [regex]::Matches([IO.File]::ReadAllText((Join-Path $Pokefirered 'trainers.h')), '\[(TRAINER_\w+)\] = \{(.*?)\n    \},', 'Singleline')) {
        $b = $m.Groups[2].Value
        if (-not $numbers.ContainsKey($m.Groups[1].Value)) { continue }
        $party = [regex]::Match($b, '\.party = \w+\(sParty_(\w+)\)').Groups[1].Value
        if (-not $party -or -not $parties.ContainsKey($party)) { continue }
        $items = @([regex]::Matches([regex]::Match($b, '\.items = \{([^}]*)\}').Groups[1].Value, 'ITEM_(\w+)') |
                ForEach-Object { $_.Groups[1].Value.ToLower() } | Where-Object { $_ -ne 'none' })
        $t = [ordered]@{
            number = $numbers[$m.Groups[1].Value]
            id = $m.Groups[1].Value
            class = [regex]::Match($b, 'trainerClass = TRAINER_CLASS_(\w+)').Groups[1].Value
            name = [regex]::Match($b, 'trainerName = _\("([^"]*)"\)').Groups[1].Value
            double = [bool]($b -match 'doubleBattle = TRUE')
        }
        if ($items.Count) { $t.items = $items }
        $t.team = $parties[$party]
        $trainers.Add($t)
    }
    Write-JsonFile $dataFile ([ordered]@{
        source = 'pret/pokefirered: src/data/trainers.h, src/data/trainer_parties.h, include/constants/opponents.h'
        trainers = @($trainers | Sort-Object { $_.number })
    })
    Write-Host "Read $($trainers.Count) FireRed trainers into $dataFile"
}

# ------------------------------------------------------------------ 2. RCT's Kanto trainer files and the data

$rct = @{}
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $RctJar))
try {
    foreach ($e in $zip.Entries) {
        if ($e.FullName -notmatch '^data/rctmod/trainers/([^/]+)\.json$') { continue }
        $reader = New-Object System.IO.StreamReader($e.Open())
        try { $rct[$Matches[1]] = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    }
} finally { $zip.Dispose() }

$db = Import-CobblemonSpecies $CobblemonJar
$fireRed = @{}
foreach ($t in @((Get-Content $dataFile -Raw | ConvertFrom-Json).trainers)) { $fireRed[[int]$t.number] = $t }
$ids = @((Get-Content $listFile -Raw | ConvertFrom-Json).trainers | ForEach-Object { $_ })
$warnings = New-Object System.Collections.Generic.List[string]

# ------------------------------------------------------------------ 3. one override per Kanto wild trainer

# Files from an earlier run, for trainers no longer listed: every file numbered in FireRed's range (0059-02e6), except
# the league trainers' own (CJP's hand-made gym leaders, listed in the region tier files).
$league = New-Object System.Collections.Generic.HashSet[string]
foreach ($f in Get-ChildItem (Join-Path $pack 'ultimate-cobblemon-progression/regions') -Recurse -Filter 'tier_*.json') {
    foreach ($slot in @((Get-Content $f.FullName -Raw | ConvertFrom-Json).trainers)) { foreach ($i in @($slot)) { if ($i) { [void]$league.Add($i) } } }
}
$listed = New-Object System.Collections.Generic.HashSet[string]
foreach ($i in $ids) { [void]$listed.Add($i) }
$removed = 0
foreach ($f in Get-ChildItem $trainerDir -Filter '*.json') {
    if ($f.BaseName -notmatch '_([0-9a-f]{4})$') { continue }
    $num = [Convert]::ToInt32($Matches[1], 16)
    if ($num -lt 0x59 -or $num -gt 0x2e6 -or $league.Contains($f.BaseName) -or $listed.Contains($f.BaseName)) { continue }
    Remove-Item $f.FullName -Confirm:$false
    $removed++
}
if ($removed) { Write-Host "Removed $removed FireRed teams of trainers no longer listed" }

$written = 0
foreach ($id in $ids) {
    if ($id -notmatch '_([0-9a-f]{4})$') { $warnings.Add("$id has no FireRed number; kept RCT's team"); continue }
    $t = $fireRed[[Convert]::ToInt32($Matches[1], 16)]
    $base = $rct[$id]
    if (-not $t) { $warnings.Add("$id has no FireRed trainer; kept RCT's team"); continue }
    if (-not $base) { $warnings.Add("RCT has no trainer $id; skipped"); continue }
    # Nameless slots are leftovers FireRed never uses (generate-wild-trainers.ps1 leaves them off the list).
    if (-not $t.name) { $warnings.Add("$id is in FireRed's unused slot $($t.id); kept RCT's team"); continue }

    # Same trainer? RCT's id ends with FireRed's name. Pairs ("ELI & ANNE") are written with "and" in RCT, and two of
    # its ids have typos ("cooprt" for Cooper, "parkker" for Parker), so for those the first three letters must match.
    $base_name = $id -replace '_[0-9a-f]{4}$', ''
    $want = Get-Key $t.name
    $last = ($base_name -split '_')[-1]
    $first = (($t.name -split '[ &]+')[0]).ToLower()
    $same = (Get-Key $base_name).EndsWith($want) -or ($t.name -match '&' -and $base_name -match "_$first`_and_") -or
            ($want.Length -ge 3 -and $last.StartsWith($want.Substring(0, 3)))
    if (-not $same) { $warnings.Add("$id doesn't look like FireRed's $($t.class) $($t.name); kept RCT's team"); continue }

    $team = New-Object System.Collections.Generic.List[object]
    foreach ($p in @($t.team)) {
        $key = Get-Key $p.species
        if ($SPECIES_NAMES.ContainsKey($key)) { $key = $SPECIES_NAMES[$key] }
        $sp = $db.species[$key]
        if (-not $sp) { $warnings.Add("${id}: unknown species '$($p.species)'; left out"); continue }
        $moves = @($p.moves | Where-Object { $_ } | ForEach-Object {
            $m = Get-Key $_; if ($MOVE_NAMES.ContainsKey($m)) { $MOVE_NAMES[$m] } else { $m } })
        $seed = Get-Hash "$id|$($team.Count)|$($sp.id)"
        $iv = [int][Math]::Round([int]$p.iv * 31 / 255)
        $team.Add((New-TrainerPokemon $db $sp $null $false ([int]$p.level) $moves ([string]$p.item) $seed $warnings $id $iv))
    }
    if ($team.Count -eq 0) { $warnings.Add("${id}: no Pokemon left; kept RCT's team"); continue }

    # RCT's name, unless Radical Red gave the slot a class FireRed doesn't have ("Dragon Tamer Ramiro" is FireRed's
    # Bird Keeper Ramiro, "Gatekeeper Owen" a nameless Super Nerd): then FireRed's.
    $name = $base.name
    if ($id -match $RENAMED) {
        $class = (Get-Culture).TextInfo.ToTitleCase(($t.class -replace '_', ' ').ToLower()) -replace '^Pkmn ', ('Pok' + [char]0xE9 + 'mon ')
        $name = if ($t.name) { "$class $((Get-Culture).TextInfo.ToTitleCase($t.name.ToLower()))" } else { $class }
    }
    $trainer = [ordered]@{ name = $name }
    foreach ($prop in 'identity', 'ai', 'battleRules') {
        if ($base.PSObject.Properties[$prop]) { $trainer[$prop] = $base.$prop }
    }
    if ($t.double -and $team.Count -ge 2) { $trainer.battleFormat = 'GEN_9_DOUBLES' }
    # Bag items Cobblemon doesn't have are left out, like held items.
    $bag = @($t.items | Where-Object { $_ -and $db.items.Contains($_) } | Group-Object | ForEach-Object { [ordered]@{ item = "cobblemon:$($_.Name)"; quantity = $_.Count } })
    if ($bag.Count) { $trainer.bag = $bag }
    $trainer.team = $team.ToArray()
    Write-JsonFile (Join-Path $trainerDir "$id.json") $trainer
    $written++
}

$warnings | Sort-Object -Unique | ForEach-Object { Write-Host "  note: $_" }
Write-Host "kanto: gave $written of $($ids.Count) trainer versions their FireRed team"
