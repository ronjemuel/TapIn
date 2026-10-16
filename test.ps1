param([string]$Config = 'config/database.properties')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & ./build.ps1
    New-Item -ItemType Directory -Force build/test/classes | Out-Null
    $testFiles = Get-ChildItem test -Filter *.java -Recurse | ForEach-Object FullName
    & javac --release 17 -encoding UTF-8 -cp 'build/classes;lib/*' -d build/test/classes $testFiles
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & java "-Dtapin.config=$Config" -cp 'build/classes;build/test/classes;lib/*' tapin.TestRunner
    if ($LASTEXITCODE -ne 0) { throw 'Integration tests failed.' }
} finally { Pop-Location }
