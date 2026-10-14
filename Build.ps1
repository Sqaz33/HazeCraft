$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'D:/Projects/ExpeditionCraft/work/toolchains/temurin25/jdk-25.0.4.1+1'
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat build --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" }
} finally { Pop-Location }
