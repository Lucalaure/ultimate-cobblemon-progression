# Shared by the scripts that write Radical Cobblemon Trainers trainer files from game data
# (generate-galar-trainers.ps1, generate-kanto-trainers.ps1). Dot-source it: . "$PSScriptRoot/lib/TrainerBuild.ps1"
# Kept ASCII: Windows PowerShell reads scripts as ANSI.

Add-Type -AssemblyName System.IO.Compression.FileSystem

$NATURES = 'hardy', 'lonely', 'brave', 'adamant', 'naughty', 'bold', 'docile', 'relaxed', 'impish', 'lax', 'timid',
        'hasty', 'serious', 'jolly', 'naive', 'modest', 'mild', 'quiet', 'bashful', 'rash', 'calm', 'gentle', 'sassy',
        'careful', 'quirky'

function Get-Plain([string]$s) { $s.Replace([string][char]0xE9, 'e').Replace([string][char]0xE8, 'e') }
function Get-Slug([string]$s) { ((Get-Plain $s).ToLowerInvariant() -replace '[^a-z0-9]+', '_').Trim('_') }
function Get-Key([string]$s) { (Get-Plain $s).ToLowerInvariant() -replace '[^a-z0-9]', '' }
# A stable number from a string, so generated values are the same every run.
function Get-Hash([string]$s) { $h = 0; foreach ($ch in $s.ToCharArray()) { $h = ($h * 31 + [int]$ch) % 2147483647 }; $h }

# PowerShell 5.1's ConvertTo-Json turns one-element and empty arrays into plain values, so JSON is written by hand.
function ConvertTo-PrettyJson($value, [string]$indent = '') {
    $next = $indent + '  '
    if ($null -eq $value) { return 'null' }
    if ($value -is [bool]) { if ($value) { return 'true' } else { return 'false' } }
    if ($value -is [string]) {
        $sb = New-Object System.Text.StringBuilder
        foreach ($ch in $value.ToCharArray()) {
            if ($ch -eq '"' -or $ch -eq '\') { [void]$sb.Append('\').Append($ch) }
            elseif ([int]$ch -gt 126) { [void]$sb.AppendFormat('\u{0:x4}', [int]$ch) }
            else { [void]$sb.Append($ch) }
        }
        return '"' + $sb.ToString() + '"'
    }
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

function Write-JsonFile([string]$path, $value) {
    New-Item -ItemType Directory -Force (Split-Path -Parent $path) | Out-Null
    [System.IO.File]::WriteAllText($path, (ConvertTo-PrettyJson $value) + "`n", (New-Object System.Text.UTF8Encoding $false))
}

# Cobblemon's species data from its jar: species (by name and by id, accent- and punctuation-free), every move
# any of them learns, and every Cobblemon item id (without namespace).
function Import-CobblemonSpecies([string]$jar) {
    $species = @{}
    $moves = New-Object System.Collections.Generic.HashSet[string]
    $items = New-Object System.Collections.Generic.HashSet[string]
    $zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $jar))
    try {
        foreach ($e in $zip.Entries) {
            if ($e.FullName -eq 'assets/cobblemon/lang/en_us.json') {
                $reader = New-Object System.IO.StreamReader($e.Open())
                try { $lang = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
                foreach ($key in $lang.PSObject.Properties.Name) {
                    if ($key -match '^item\.cobblemon\.([a-z0-9_]+)$') { [void]$items.Add($Matches[1]) }
                }
                continue
            }
            if ($e.FullName -notmatch '^data/cobblemon/species/[^/]+/([^/]+)\.json$') { continue }
            $id = $Matches[1]
            $reader = New-Object System.IO.StreamReader($e.Open())
            try { $json = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
            $entry = @{ id = $id; json = $json }
            $species[(Get-Key $json.name)] = $entry
            $species[(Get-Key $id)] = $entry
            foreach ($m in (@($json.moves) + @($json.forms | ForEach-Object { $_.moves }))) {
                if ($m) { [void]$moves.Add(($m -split ':')[-1]) }
            }
        }
    } finally { $zip.Dispose() }
    if ($species.Count -eq 0) { throw "$jar has no data/cobblemon/species" }
    return @{ species = $species; moves = $moves; items = $items }
}

# The species' form for an aspect, or null.
function Get-Form($sp, [string]$aspect) {
    if (-not $aspect) { return $null }
    foreach ($form in @($sp.json.forms)) { if ($form -and @($form.aspects) -contains $aspect) { return $form } }
    return $null
}

<#
One Pokemon of an RCT trainer team. $moves are move ids from the game data; unknown ones are reported and left out,
and with none left the Pokemon gets the last four moves it learns by level (what trainers in the games get by
default). Gender (by the species' ratio), ability (its first regular one) and nature come from Cobblemon's data and
$seed, so they're the same every run. $heldItem is a Cobblemon item id without namespace, or empty; one Cobblemon
doesn't have (FireRed's Stardust and Nugget) is reported and left out, since RCT rejects it. $iv, from 0 to 31,
sets every IV; -1 leaves them to RCT.
#>
function New-TrainerPokemon($db, $sp, [string]$aspect, [bool]$female, [int]$level, $moves, [string]$heldItem,
                            [int]$seed, $warnings, [string]$who, [int]$iv = -1) {
    $form = Get-Form $sp $aspect
    $level = [Math]::Max(1, $level)
    $ratio = if ($form -and $null -ne $form.maleRatio) { [double]$form.maleRatio } else { [double]$sp.json.maleRatio }
    $gender = if ($female) { 'FEMALE' } elseif ($ratio -lt 0) { 'GENDERLESS' } elseif ($ratio -eq 0) { 'FEMALE' }
              elseif ($ratio -ge 1) { 'MALE' } elseif (($seed % 1000) / 1000.0 -lt $ratio) { 'MALE' } else { 'FEMALE' }
    $abilities = if ($form -and @($form.abilities).Count) { @($form.abilities) } else { @($sp.json.abilities) }
    $ability = @($abilities | Where-Object { $_ -and -not $_.StartsWith('h:') })[0]

    $valid = @($moves | Where-Object { $_ } | ForEach-Object {
        if ($db.moves.Contains($_)) { $_ } else { $warnings.Add("${who}: unknown move '$_' on $($sp.id); left out") }
    })
    if ($valid.Count -eq 0) {
        $learnset = if ($form -and @($form.moves).Count) { @($form.moves) } else { @($sp.json.moves) }
        $byLevel = @($learnset | Where-Object { $_ -match '^(\d+):(.+)$' -and [int]$Matches[1] -le $level } |
                ForEach-Object { $_ -match '^(\d+):(.+)$' | Out-Null; [pscustomobject]@{ l = [int]$Matches[1]; m = $Matches[2] } } |
                Sort-Object l)
        $valid = @($byLevel | ForEach-Object { $_.m } | Select-Object -Unique | Select-Object -Last 4)
    }

    $mon = [ordered]@{ species = $sp.id; gender = $gender; level = $level; nature = $NATURES[$seed % 25] }
    if ($ability) { $mon.ability = $ability }
    $mon.moveset = @($valid | Select-Object -First 4)
    $mon.ivs = if ($iv -ge 0) { [ordered]@{ hp = $iv; atk = $iv; def = $iv; spa = $iv; spd = $iv; spe = $iv } } else { [ordered]@{} }
    $mon.evs = [ordered]@{}
    if ($heldItem -and $db.items.Count -and -not $db.items.Contains($heldItem)) {
        $warnings.Add("${who}: Cobblemon has no item '$heldItem' for $($sp.id) to hold; left out")
        $heldItem = ''
    }
    if ($heldItem) { $mon.heldItem = @($heldItem) }
    if ($aspect) { $mon.aspects = @($aspect) }
    return $mon
}
