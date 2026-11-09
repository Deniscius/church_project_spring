param([string]$ApiBaseUrl)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
$buildArguments = @('-B', 'clean', 'verify', 'dependency:copy-dependencies', '-DincludeScope=runtime', '-DoutputDirectory=target/package')
if ($ApiBaseUrl) { $buildArguments += "-Ddesktop.api.url=$ApiBaseUrl" }
& mvn @buildArguments
if ($LASTEXITCODE -ne 0) { throw 'La compilation ou les tests ont échoué.' }
Copy-Item target/missanye-desktop-0.1.0-SNAPSHOT.jar target/package/
& jpackage --type app-image --name Missanye --app-version 0.1.0 --input target/package --main-jar missanye-desktop-0.1.0-SNAPSHOT.jar --main-class com.missanye.desktop.Launcher --dest target/distribution
if ($LASTEXITCODE -ne 0) { throw 'La création du paquet a échoué.' }
