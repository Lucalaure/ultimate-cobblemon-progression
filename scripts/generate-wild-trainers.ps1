<#
.SYNOPSIS
Lists which of Radical Cobblemon Trainers' wild trainers belong to which CJP league region.

.DESCRIPTION
Usage:
    powershell -File scripts/generate-wild-trainers.ps1 <rctmod jar>

Writes regions/<region>/wild_trainers.json into the built-in leagues datapack
(src/main/resources/resourcepacks/kanto_league/data/ultimate-cobblemon-progression). In region mode a wild trainer only
spawns for players who have its region open, and the Trainer Card's Trainers tab counts the ones they've beaten.

RCT's trainers come from three games, and their ids end in a 4-digit hex number grouped by game (REGIONS below):
Radical Red (Kanto), Brilliant Diamond / Shining Pearl (Sinnoh) and Pokemon Unbound (Borrius, which isn't a CJP region,
so its trainers are listed nowhere). Double battles end in m000-m00f instead and are placed by hand (DOUBLES).

Left out, so every listed trainer can actually be met and beaten:
- league trainers (listed in a region's tiers; they only appear in gyms),
- trainers that never spawn (spawnWeightFactor 0, after the datapack's own overrides),
- trainers that need other trainers beaten first, unless each requirement includes a league trainer. RCT only
  records a defeat towards those requirements while the trainer is part of the player's current series, and CJP
  players are only ever in its league series, so requirements like Radical Red's rival battles can never be met.
#>
param([Parameter(Mandatory = $true)][string]$RctJar)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repo = Split-Path -Parent $PSScriptRoot
$pack = Join-Path $repo 'src/main/resources/resourcepacks/kanto_league/data'

# Hex id ranges per region (inclusive).
$REGIONS = [ordered]@{
    kanto  = @{ from = 0x0000; to = 0x02ff }
    sinnoh = @{ from = 0x0300; to = 0x05a9 }
}
# Double battles (ids ending in m0xx). Anything not listed here belongs to no region.
$DOUBLES = @{
    'rocket_admin_archer_ariana_m000' = 'kanto'
    'twins_liv_liz_m001'              = 'sinnoh'
    'young_couple_sue_ty_m002'        = 'sinnoh'
    'twins_teri_tia_m003'             = 'sinnoh'
    'double_team_jen_zac_m004'        = 'sinnoh'
    'double_team_jo_pat_m005'         = 'sinnoh'
    'ranchers_ava_matt_m006'          = 'sinnoh'
    'twins_emma_lil_m007'             = 'sinnoh'
    'interviewers_oli_roxy_m008'      = 'sinnoh'
    'commander_mars_jupiter_m009'     = 'sinnoh'
    'ranchers_beth_bob_m00a'          = 'sinnoh'
    'young_couple_mike_nat_m00b'      = 'sinnoh'
    'twins_teri_tia_m00c'             = 'sinnoh'
    'twins_teri_tia_m00d'             = 'sinnoh'
}

# PowerShell 5.1's ConvertTo-Json turns one-element and empty arrays into plain values, so JSON is written by hand.
function ConvertTo-PrettyJson($value, [string]$indent = '') {
    $next = $indent + '  '
    if ($null -eq $value) { return 'null' }
    if ($value -is [bool]) { if ($value) { return 'true' } else { return 'false' } }
    if ($value -is [string]) { return '"' + $value.Replace('\', '\\').Replace('"', '\"') + '"' }
    if ($value -is [ValueType]) { return [System.Convert]::ToString($value, [System.Globalization.CultureInfo]::InvariantCulture) }
    if ($value -is [System.Collections.IDictionary]) {
        if ($value.Count -eq 0) { return '{}' }
        $parts = foreach ($k in $value.Keys) { $next + '"' + $k + '": ' + (ConvertTo-PrettyJson $value[$k] $next) }
        return "{`n" + ($parts -join ",`n") + "`n$indent}"
    }
    if ($value -is [System.Management.Automation.PSCustomObject]) {
        $d = [ordered]@{}
        foreach ($p in $value.PSObject.Properties) { $d[$p.Name] = $p.Value }
        return ConvertTo-PrettyJson $d $indent
    }
    if ($value -is [System.Collections.IEnumerable]) {
        $items = @($value)
        if ($items.Count -eq 0) { return '[]' }
        $parts = foreach ($i in $items) { $next + (ConvertTo-PrettyJson $i $next) }
        return "[`n" + ($parts -join ",`n") + "`n$indent]"
    }
    throw "Can't write $($value.GetType()) as JSON"
}

# ------------------------------------------------------------------ read RCT's data, then the datapack's overrides

$trainerIds = New-Object System.Collections.Generic.List[string]
$mobs = @{}   # 'default', 'groups/<name>', 'single/<id>' -> mob data
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $RctJar))
try {
    foreach ($e in $zip.Entries) {
        if ($e.FullName -match '^data/rctmod/trainers/([^/]+)\.json$') {
            $trainerIds.Add($Matches[1])
        } elseif ($e.FullName -match '^data/rctmod/mobs/trainers/(.+)\.json$') {
            $reader = New-Object System.IO.StreamReader($e.Open())
            try { $mobs[$Matches[1]] = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        }
    }
} finally { $zip.Dispose() }
if ($trainerIds.Count -eq 0) { throw "$RctJar has no data/rctmod/trainers" }

$overrides = Join-Path $pack 'rctmod/mobs/trainers'

# Gym leaders, Elite Four members and Champions only appear in CJP's gyms. Besides each series' own leaders, RCT has
# rematch and cameo versions that spawn like ordinary trainers (Sinnoh's leader rematches, Radical Red's Johto leaders),
# through their own files or the leader, gym_leader, elite_four and champion classes. Each gets a copy with
# spawnWeightFactor 0, unless the datapack already overrides it (CJP's own league trainers).
$LEADER_CLASSES = 'leader', 'gym_leader', 'elite_four', 'champion'
$leaderPattern = '^(' + ($LEADER_CLASSES -join '|') + ')_'
$turnedOff = 0
foreach ($key in @($mobs.Keys | Sort-Object)) {
    $isLeader = ($key -like 'groups/*' -and $LEADER_CLASSES -contains $key.Substring(7)) -or
            ($key -like 'single/*' -and ($key.Substring(7) -match $leaderPattern -or @('leader', 'e4', 'champ') -contains $mobs[$key].type))
    if (-not $isLeader -or $mobs[$key].spawnWeightFactor -eq 0) { continue }
    $path = Join-Path $overrides "$key.json"
    if (Test-Path $path) { continue }
    New-Item -ItemType Directory -Force (Split-Path -Parent $path) | Out-Null
    $copy = [ordered]@{}
    # Plain assignments: an if expression would flatten one-element and empty arrays.
    foreach ($p in $mobs[$key].PSObject.Properties) { $copy[$p.Name] = $p.Value }
    $copy['spawnWeightFactor'] = 0
    [System.IO.File]::WriteAllText($path, (ConvertTo-PrettyJson $copy) + "`n", (New-Object System.Text.UTF8Encoding $false))
    $turnedOff++
}
if ($turnedOff) { Write-Host "Turned off natural spawning for $turnedOff leader, Elite Four and Champion files" }

if (Test-Path $overrides) {
    foreach ($f in Get-ChildItem $overrides -Recurse -Filter *.json) {
        $key = $f.FullName.Substring($overrides.Length + 1).Replace('\', '/') -replace '\.json$', ''
        $mobs[$key] = Get-Content $f.FullName -Raw | ConvertFrom-Json
    }
}

# League trainers: every id in a tier file's "trainers" slots.
$league = New-Object System.Collections.Generic.HashSet[string]
foreach ($f in Get-ChildItem (Join-Path $pack 'ultimate-cobblemon-progression/regions') -Recurse -Filter 'tier_*.json') {
    $tier = Get-Content $f.FullName -Raw | ConvertFrom-Json
    foreach ($slot in @($tier.trainers)) { foreach ($id in @($slot)) { if ($id) { [void]$league.Add($id) } } }
}

# The mob data RCT uses for a trainer: its own, else the longest group whose name the id starts with, else the default.
$groupNames = @($mobs.Keys | Where-Object { $_ -like 'groups/*' } | ForEach-Object { $_.Substring(7) } | Sort-Object Length -Descending)
function Get-MobData([string]$id) {
    if ($mobs.ContainsKey("single/$id")) { return $mobs["single/$id"] }
    foreach ($g in $groupNames) { if ($id -eq $g -or $id.StartsWith($g)) { return $mobs["groups/$g"] } }
    return $mobs['default']
}

function Get-Region([string]$id) {
    if ($DOUBLES.ContainsKey($id)) { return $DOUBLES[$id] }
    if ($id -notmatch '_([0-9a-f]{4})$') { return $null }
    $n = [Convert]::ToInt32($Matches[1], 16)
    foreach ($r in $REGIONS.Keys) { if ($n -ge $REGIONS[$r].from -and $n -le $REGIONS[$r].to) { return $r } }
    return $null
}

# ------------------------------------------------------------------ sort them into regions

$lists = [ordered]@{}
foreach ($r in $REGIONS.Keys) { $lists[$r] = New-Object System.Collections.Generic.List[string] }
$skipped = [ordered]@{ 'no region' = 0; 'league trainer' = 0; 'never spawns' = 0; 'unmeetable requirement' = 0 }

foreach ($id in ($trainerIds | Sort-Object)) {
    $region = Get-Region $id
    if (-not $region) { $skipped['no region']++; continue }
    if ($league.Contains($id)) { $skipped['league trainer']++; continue }
    $mob = Get-MobData $id
    $weight = if ($null -ne $mob.spawnWeightFactor) { [double]$mob.spawnWeightFactor } else { 1.0 }
    if ($weight -le 0) { $skipped['never spawns']++; continue }
    $meetable = $true
    foreach ($group in @($mob.requiredDefeats)) {
        if ($null -eq $group) { continue }
        if (-not (@($group) | Where-Object { $league.Contains($_) })) { $meetable = $false; break }
    }
    if (-not $meetable) { $skipped['unmeetable requirement']++; continue }
    $lists[$region].Add($id)
}

foreach ($r in $lists.Keys) {
    $dir = Join-Path $pack "ultimate-cobblemon-progression/regions/$r"
    if (-not (Test-Path (Join-Path $dir 'region.json'))) { throw "No region.json for '$r' in $dir" }
    $body = ($lists[$r] | ForEach-Object { "    `"$_`"" }) -join ",`n"
    $json = "{`n  `"trainers`": [`n$body`n  ]`n}`n"
    [System.IO.File]::WriteAllText((Join-Path $dir 'wild_trainers.json'), $json, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "$r`: $($lists[$r].Count) wild trainers"
}
Write-Host ("Left out: " + (($skipped.Keys | ForEach-Object { "$($skipped[$_]) $_" }) -join ', '))
