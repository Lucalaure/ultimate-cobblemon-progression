<#
.SYNOPSIS
Builds Galar's wild trainers (Pokemon Sword & Shield) for Radical Cobblemon Trainers.

.DESCRIPTION
Usage:
    powershell -File scripts/generate-galar-trainers.ps1 <Cobblemon jar> [-Serebii <folder of saved pages>]

RCT ships no Galar trainers, so CJP brings its own. The trainers themselves are in scripts/data/galar-trainers.json:
every trainer battle on Serebii's PokeEarth pages for Galar (https://serebii.net/pokearth/galar/), base game and both
DLCs, with each Pokemon's species, form, level, moves and held item, and the items the trainer uses. With -Serebii, the
file is rebuilt from saved copies of those pages first (one .html per location, named like route2.html).

From that file the script writes, into the built-in leagues datapack:
- an RCT trainer file per battle (data/rctmod/trainers/<id>.json). Gender, ability and nature aren't on Serebii, so
  they come from Cobblemon's species data: gender by the species' ratio, its first regular ability, and a nature picked
  from the trainer's id (the same every run). A Pokemon listed without moves gets its last four level-up moves.
- Galar's wild_trainers.json: one entry per trainer, holding all its versions (Hop's 42 battles are one trainer).

Left out: gym leaders, the Champion, Chairman Rose, Mustard and Honey, the Stadium battles (Gym Challenge,
Champion Cup, tournaments) and Max Raid partners. Rivals and story trainers stay, as Kanto's list keeps its rival.

Each Galar trainer class is filed under the closest class RCT already has (CLASSES below), which RCT reads from the
start of the id: a Gym Trainer's id starts with ace_trainer_, a Backpacker's with hiker_. That class decides where the
trainer spawns, its loot and its skin, so Galar needs no new classes or art. Its own name ("Gym Trainer Annette") is
what players see. Ids end in g001, g002... in story order.
#>
param(
    [Parameter(Mandatory = $true)][string]$CobblemonJar,
    [string]$Serebii
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Web

$repo = Split-Path -Parent $PSScriptRoot
$dataFile = Join-Path $PSScriptRoot 'data/galar-trainers.json'
$pack = Join-Path $repo 'src/main/resources/resourcepacks/kanto_league/data'
$trainerDir = Join-Path $pack 'rctmod/trainers'
$galarDir = Join-Path $pack 'ultimate-cobblemon-progression/regions/galar'

# Galar trainer class -> RCT class whose spawning, loot and skin it uses. Classes not listed are left out.
$CLASSES = [ordered]@{
    'Pokemon Trainer' = 'pokemon_trainer'; 'Pokemon Breeder' = 'pokemon_breeder'; 'Gym Trainer' = 'ace_trainer'
    'League Staff' = 'ace_trainer'; "GAME FREAK's" = 'game_freaks'; "Macro Cosmos's" = 'scientist'
    'Team Yell Grunt' = 'biker'; 'Master Dojo Student' = 'black_belt'; 'Black Belt' = 'black_belt'
    'Office Worker' = 'gentleman'; 'Gentleman' = 'gentleman'; 'Worker' = 'worker'; 'Rail Staff' = 'worker'
    'Cabbie' = 'worker'; 'Postman' = 'worker'; 'Cook' = 'waitress'; 'Cafe Master' = 'waitress'
    'Beauty' = 'beauty'; 'Model' = 'beauty'; 'Madame' = 'madame'; 'Police Officer' = 'police'; 'Doctor' = 'scientist'
    'Hiker' = 'hiker'; 'Backpacker' = 'hiker'; 'Reporter' = 'reporter'; 'Cameraman' = 'cameraman'
    'Youngster' = 'youngster'; 'Lass' = 'lass'; 'Schoolgirl' = 'school_kid'; 'Schoolboy' = 'school_kid'
    'Poke Kid' = 'poke_kid'; 'Artist' = 'artist'; 'Musician' = 'guitarist'; 'Dancer' = 'idol'
    'Fisher' = 'fisher'; 'Swimmer' = 'swimmer'
}
$EXCLUDED_NAMES = '^(Gym Leader|Champion|Dojo Master|Dojo Matron|Gym Challenger) |^Macro Cosmos''s Rose$'
$EXCLUDED_NOTES = 'Max Raid Battle Partner'
$EXCLUDED_SECTIONS = '^Stadium$'
$EXCLUDED_PAGES = '^(wyndonstadium|battletower|maxlair)$'

# Story order for the ids; pages not listed come after, alphabetically.
$PAGE_ORDER = @(
    'postwick', 'wedgehurst', 'route1', 'slumberingweald', 'route2', 'motostokeoutskirts', 'motostoke', 'route3',
    'galarmine', 'route4', 'turffield', 'route5', 'hulbury', 'galarmineno.2', 'route6', 'stow-on-side',
    'glimwoodtangle', 'ballonlea', 'route7', 'route8', 'circhester', 'route9', 'route9tunnel', 'spikemuth',
    'hammerlocke', 'route10', 'wyndon', 'rosetower', 'energyplant',
    'warm-uptunnel', 'masterdojo', 'fieldsofhonor', 'towerofdarkness', 'towerofwaters', 'slipperyslope'
)

# Serebii's Pokemon image suffixes -> Cobblemon aspects (or a gender). Anything else is reported and ignored.
$FORMS = @{ '-g' = 'galarian'; '422-e' = 'east-sea'; '423-e' = 'east-sea'; '550-b' = 'bluestriped'
            '710-s' = 'pumpkin-size-small'; '710-l' = 'pumpkin-size-large'; '710-h' = 'pumpkin-size-super'
            '711-s' = 'pumpkin-size-small'; '711-l' = 'pumpkin-size-large'; '711-h' = 'pumpkin-size-super'
            '479-h' = 'heat-appliance'; '479-w' = 'wash-appliance'; '479-m' = 'mow-appliance'
            '479-f' = 'frost-appliance'; '479-s' = 'fan-appliance' }
$FEMALE_FORMS = @('876-f', '678-f')
# Kept as the regular form: Sinistea's antique form and Alcremie's flavours only look different, Gigantamax forms
# (-gi) need Dynamax, which Sword & Shield only allows at Power Spots, and Hop's Crowned Zacian / Zamazenta (-c) need
# the Rusted Sword / Shield, which Cobblemon doesn't have (they keep their Hero form, without Behemoth Blade / Bash).
$REGULAR_FORMS = '^(854-a|855-a|869-.+|888-c|889-c|\d+-gi)$'
# Items and moves Serebii lists that can't be used here, for the reasons above, so they're left out quietly.
$IGNORED_ITEMS = @('pokeball', 'rustedsword', 'rustedshield')
$IGNORED_MOVES = @('behemothblade', 'behemothbash')

# Serebii item names -> Cobblemon items. Others are reported and left out.
$ITEMS = @{
    potion = 'potion'; superpotion = 'super_potion'; hyperpotion = 'hyper_potion'; maxpotion = 'max_potion'
    fullrestore = 'full_restore'; fullheal = 'full_heal'; revive = 'revive'
    focussash = 'focus_sash'; sitrusberry = 'sitrus_berry'; hardstone = 'hard_stone'; eviolite = 'eviolite'
    oranberry = 'oran_berry'; leftovers = 'leftovers'; lifeorb = 'life_orb'; choicescarf = 'choice_scarf'
}

. (Join-Path $PSScriptRoot 'lib/TrainerBuild.ps1')

# ------------------------------------------------------------------ 1. Serebii pages -> galar-trainers.json

if ($Serebii) {
    $pages = @(Get-ChildItem $Serebii -Filter *.html | Sort-Object {
        $i = [array]::IndexOf($PAGE_ORDER, $_.BaseName); if ($i -lt 0) { 1000 } else { $i } }, Name)
    $trainers = New-Object System.Collections.Generic.List[object]
    foreach ($f in $pages) {
        if ($f.BaseName -match $EXCLUDED_PAGES) { continue }
        $html = [IO.File]::ReadAllText($f.FullName)
        foreach ($part in ([regex]::Split($html, '<h2>') | Select-Object -Skip 1)) {
            $section = [System.Web.HttpUtility]::HtmlDecode(($part -split '</h2>')[0])
            if ($section -match $EXCLUDED_SECTIONS) { continue }
            foreach ($c in ([regex]::Split($part, '<table class="trainer">') | Select-Object -Skip 1)) {
                # The trainer's table ends with its note; anything after belongs to the rest of the page.
                $end = $c.IndexOf('<td valign="top"><i>')
                if ($end -ge 0) { $c = $c.Substring(0, $c.IndexOf('</i>', $end) + 4) }
                $name = [System.Web.HttpUtility]::HtmlDecode([regex]::Match($c, '<td >([^<]+)</td>').Groups[1].Value).Trim()
                $note = ([regex]::Matches($c, '<i>([^<]+)</i>') | ForEach-Object { $_.Groups[1].Value } |
                        Where-Object { $_ -ne '(Details)' }) -join '; '
                if (-not $name -or $name -match $EXCLUDED_NAMES -or ($note -and $note -match $EXCLUDED_NOTES)) { continue }

                $mons = [regex]::Matches($c, 'href="/pokedex-swsh/([^"]+)"><img src="/swordshield/pokemon/small/(\d+)(-[a-z0-9]+)?\.png"')
                $levels = [regex]::Matches($c, 'class="level" height="1">Level (\d+)')
                # @(): with a single Pokemon, PowerShell would otherwise unwrap the one block into a plain string.
                $attackBlocks = @([regex]::Split($c, '<b>Attacks</b>') | Select-Object -Skip 1)
                $heldBlocks = [regex]::Matches($c, '<b>Hold Item</b>:<br />(.*?)</td>')
                $team = for ($i = 0; $i -lt $mons.Count; $i++) {
                    $moves = @()
                    if ($i -lt @($attackBlocks).Count) {
                        $block = ($attackBlocks[$i] -split '<b>Hold Item</b>')[0]
                        # Some move links have hyphens (double-edge, x-scissor); the ids drop them when built.
                        $moves = @([regex]::Matches($block, '/attackdex-swsh/([a-z0-9-]+)\.shtml') | ForEach-Object { $_.Groups[1].Value })
                    }
                    $held = $null
                    if ($i -lt $heldBlocks.Count) {
                        $m = [regex]::Match($heldBlocks[$i].Groups[1].Value, '/itemdex/([a-z0-9]+)\.shtml')
                        if ($m.Success) { $held = $m.Groups[1].Value }
                    }
                    $entry = [ordered]@{ species = $mons[$i].Groups[1].Value; dex = [int]$mons[$i].Groups[2].Value }
                    if ($mons[$i].Groups[3].Success) { $entry.form = $mons[$i].Groups[3].Value }
                    $entry.level = if ($i -lt $levels.Count) { [int]$levels[$i].Groups[1].Value } else { 0 }
                    $entry.moves = $moves
                    if ($held) { $entry.item = $held }
                    $entry
                }
                $bagCell = ''
                $b = $c.IndexOf('<b>Items</b>'); $l = $c.IndexOf('class="level"')
                if ($b -ge 0 -and $l -gt $b) { $bagCell = $c.Substring($b, $l - $b) }
                $bag = @([regex]::Matches($bagCell, '/itemdex/([a-z0-9]+)\.shtml') | ForEach-Object { $_.Groups[1].Value })

                $trainer = [ordered]@{ name = $name; location = $f.BaseName; section = $section }
                if ($note) { $trainer.note = $note }
                $trainer.team = @($team)
                if ($bag.Count) { $trainer.bag = $bag }
                $trainers.Add($trainer)
            }
        }
    }
    Write-JsonFile $dataFile ([ordered]@{
        source = 'https://serebii.net/pokearth/galar/ (Pokemon Sword & Shield, base game and DLC)'
        trainers = $trainers.ToArray()
    })
    Write-Host "Read $($trainers.Count) trainer battles from $(@($pages).Count) Serebii pages into $dataFile"
}

# ------------------------------------------------------------------ 2. Cobblemon's species data

$db = Import-CobblemonSpecies $CobblemonJar
$species = $db.species


# ------------------------------------------------------------------ 3. galar-trainers.json -> RCT trainers

$data = Get-Content $dataFile -Raw -Encoding UTF8 | ConvertFrom-Json
$warnings = New-Object System.Collections.Generic.List[string]

# Files from an earlier run (-Filter can't match digit ranges, so the name is checked with a regex).
Get-ChildItem $trainerDir -Filter '*.json' -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '_g\d{3}\.json$' } |
        Remove-Item -Confirm:$false

$entries = [ordered]@{}      # trainer key -> its versions' ids
$personalIds = @{}           # trainer key -> the id stem its versions share
$ordinals = @{}              # nameless trainers: battles seen so far per (location, name, note)
$n = 0
foreach ($t in @($data.trainers)) {
    # Class titles are matched without accents (the script is kept ASCII: Windows PowerShell reads it as ANSI).
    $plainName = Get-Plain $t.name
    $class = $null
    foreach ($title in $CLASSES.Keys) {
        if ($plainName -eq $title -or $plainName.StartsWith("$title ")) { $class = $title; break }
    }
    if (-not $class) { $warnings.Add("No RCT class for '$($t.name)'; left out"); continue }
    $personal = $plainName.Substring($class.Length).Trim()

    # One entry per trainer. Nameless trainers are a battle each; a nameless trainer's rematches (same place, other
    # note) pair up in the order Serebii lists them.
    if ($personal) {
        $key = $t.name
    } else {
        $slot = "$($t.location)|$($t.name)|$($t.note)"
        $ordinals[$slot] = 1 + [int]$ordinals[$slot]
        $key = "$($t.name) #$($t.location) $($ordinals[$slot])"
    }

    $team = New-Object System.Collections.Generic.List[object]
    foreach ($p in @($t.team)) {
        $sp = $species[(Get-Key $p.species)]
        if (-not $sp) { $warnings.Add("$($t.name): unknown species '$($p.species)'; left out"); continue }
        if ($sp.json.implemented -eq $false) { $warnings.Add("$($t.name): $($sp.id) isn't in Cobblemon yet; left out"); continue }
        $aspect = $null; $female = $false
        if ($p.form) {
            $code = "$($p.dex)$($p.form)"
            if ($FEMALE_FORMS -contains $code) { $female = $true }
            elseif ($FORMS.ContainsKey($code)) { $aspect = $FORMS[$code] }
            elseif ($FORMS.ContainsKey($p.form)) { $aspect = $FORMS[$p.form] }
            elseif ($code -notmatch $REGULAR_FORMS) { $warnings.Add("$($t.name): unknown form $code of $($sp.id); used the regular form") }
        }
        if ($aspect -and -not (Get-Form $sp $aspect) -and $aspect -eq 'galarian') {
            $warnings.Add("$($t.name): Cobblemon has no Galarian $($sp.id); used the regular form"); $aspect = $null
        }
        $held = $null
        if ($p.item -and $IGNORED_ITEMS -notcontains $p.item) {
            if ($ITEMS.ContainsKey($p.item)) { $held = $ITEMS[$p.item] }
            else { $warnings.Add("$($t.name): unknown held item '$($p.item)'; left out") }
        }
        # Serebii's move names as Cobblemon ids: "double-edge" -> "doubleedge".
        $moves = @($p.moves | Where-Object { $_ } | ForEach-Object { Get-Key $_ } | Where-Object { $IGNORED_MOVES -notcontains $_ })
        $seed = Get-Hash "$($t.name)|$($t.location)|$($t.note)|$($team.Count)|$($sp.id)"
        $team.Add((New-TrainerPokemon $db $sp $aspect $female ([int]$p.level) $moves $held $seed $warnings $t.name))
    }
    if ($team.Count -eq 0) { $warnings.Add("$($t.name) ($($t.location)) has no Pokemon Cobblemon knows; left out"); continue }

    $n++
    if (-not $personalIds.ContainsKey($key)) {
        $stem = if ($personal) { Get-Slug $personal } else { Get-Slug $class }
        $personalIds[$key] = "$($CLASSES[$class])_$stem"
    }
    $id = '{0}_g{1:d3}' -f $personalIds[$key], $n
    $trainer = [ordered]@{ name = $t.name; team = $team.ToArray() }
    $bag = @($t.bag | Where-Object { $_ -and $IGNORED_ITEMS -notcontains $_ } | Group-Object | ForEach-Object {
        if ($ITEMS.ContainsKey($_.Name)) { [ordered]@{ item = "cobblemon:$($ITEMS[$_.Name])"; quantity = $_.Count } }
        else { $warnings.Add("$($t.name): unknown bag item '$($_.Name)'; left out") }
    })
    if ($bag.Count) { $trainer.bag = $bag }
    Write-JsonFile (Join-Path $trainerDir "$id.json") $trainer

    if (-not $entries.Contains($key)) { $entries[$key] = New-Object System.Collections.Generic.List[string] }
    $entries[$key].Add($id)
}

# ------------------------------------------------------------------ 4. Galar's wild_trainers.json

if (-not (Test-Path (Join-Path $galarDir 'region.json'))) { throw "No region.json for Galar in $galarDir" }
$lines = foreach ($versions in $entries.Values) {
    if ($versions.Count -eq 1) { "    `"$($versions[0])`"" }
    else { '    [' + (($versions | ForEach-Object { "`"$_`"" }) -join ', ') + ']' }
}
$json = "{`n  `"trainers`": [`n" + ($lines -join ",`n") + "`n  ]`n}`n"
[System.IO.File]::WriteAllText((Join-Path $galarDir 'wild_trainers.json'), $json, (New-Object System.Text.UTF8Encoding $false))

$warnings | Sort-Object -Unique | ForEach-Object { Write-Host "  note: $_" }
Write-Host "galar: $($entries.Count) trainers ($n battles in all)"
