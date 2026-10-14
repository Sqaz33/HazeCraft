param(
    [Parameter(Mandatory=$true)][string]$Name,
    [Parameter(Mandatory=$true)][string[]]$Addresses,
    [string]$Build = '25939744',
    [hashtable]$Kinds = @{}
)
$ErrorActionPreference = 'Stop'
$lab = 'D:/DeadlockHazeLab'
$root = Split-Path -Parent $PSScriptRoot
$python = "$lab/tools/pyenv/Scripts/python.exe"
$json = "$lab/evidence/$Build-$Name-functions.json"
$targets = "$lab/evidence/$Build-$Name-targets.txt"
& $python "$PSScriptRoot/native_fn.py" @Addresses --dll "$lab/reference/$Build/client.dll" --out $json
if ($LASTEXITCODE -ne 0) { throw 'Function boundary extraction failed' }
$rows = Get-Content -LiteralPath $json -Raw | ConvertFrom-Json
$lines = foreach ($row in $rows) {
    $address = $row.start.Substring(2)
    $ranges = ($row.ranges | ForEach-Object { $_.start.Substring(2) + '-' + $_.end.Substring(2) }) -join ','
    $kind = if ($Kinds.ContainsKey($address)) { '|' + $Kinds[$address] } else { '' }
    "Movement_$address|$ranges$kind"
}
[System.IO.File]::WriteAllLines($targets, [string[]]$lines)
$env:JAVA_HOME = 'D:/Projects/ExpeditionCraft/work/toolchains/temurin25/jdk-25.0.4.1+1'
$runner = "$lab/tools/ghidra/ghidra_12.1.4_PUBLIC/support/analyzeHeadless.bat"
& $runner "$lab/ghidra-project" "Retail$Build" -process client.dll -noanalysis -scriptPath "$root/tools" -postScript HazeDecompileTargets.java $targets "$lab/decompiled/$Build/movement" *> "$lab/evidence/$Build-ghidra-$Name.log"
if ($LASTEXITCODE -ne 0) { throw "Decompilation failed: $lab/evidence/$Build-ghidra-$Name.log" }
Get-Content -LiteralPath "$lab/evidence/$Build-ghidra-$Name.log" -Tail 6
