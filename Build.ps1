param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if ($JavaHome) {
    if (!(Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) { throw 'JavaHome must point to an installed JDK 21 or newer.' }
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
}
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat build --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" }
} finally { Pop-Location }
