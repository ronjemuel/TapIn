$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force build/classes,dist/lib | Out-Null
    $sourceFiles = Get-ChildItem src -Filter *.java -Recurse | ForEach-Object FullName
    & javac --release 17 -encoding UTF-8 -d build/classes $sourceFiles
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    Copy-Item src/resources build/classes/ -Recurse -Force
    Copy-Item lib/*.jar dist/lib/ -Force
    $jarCommand = Get-Command jar -ErrorAction SilentlyContinue
    if ($jarCommand) { $jarExecutable = $jarCommand.Source }
    else {
        $ErrorActionPreference = 'Continue'
        $javaSettings = & java -XshowSettings:properties -version 2>&1 | Out-String
        $ErrorActionPreference = 'Stop'
        $jdkDirectory = [regex]::Match($javaSettings, 'java.home\s*=\s*([^\r\n]+)').Groups[1].Value.Trim()
        $jarExecutable = Join-Path $jdkDirectory 'bin/jar.exe'
    }
    & $jarExecutable --create --file dist/TapIn.jar --manifest manifest.mf -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
    Write-Output 'Built dist/TapIn.jar (Java 17).'
} finally { Pop-Location }
