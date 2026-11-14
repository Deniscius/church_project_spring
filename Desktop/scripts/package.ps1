param([string]$ApiBaseUrl, [string]$DeploymentFile, [string]$AppVersion = '0.2.0')
$ErrorActionPreference = 'Stop'
if ($AppVersion -notmatch '^\d+\.\d+\.\d+$') { throw 'AppVersion doit respecter major.minor.patch.' }
Set-Location (Join-Path $PSScriptRoot '..')
$buildArguments = @('-B', 'clean', 'verify', 'dependency:copy-dependencies', '-DincludeScope=runtime', '-DoutputDirectory=target/package', "-Ddesktop.release.version=$AppVersion")
if ($ApiBaseUrl) { $buildArguments += "-Ddesktop.api.url=$ApiBaseUrl" }
& mvn @buildArguments
if ($LASTEXITCODE -ne 0) { throw 'La compilation ou les tests ont échoué.' }
Copy-Item target/missanye-desktop-0.1.0-SNAPSHOT.jar target/package/
if ($DeploymentFile) {
    if (-not (Test-Path -LiteralPath $DeploymentFile)) { throw 'Profil de paroisse introuvable.' }
    Copy-Item -LiteralPath $DeploymentFile -Destination target/package/parish.properties
}
& jpackage --type app-image --icon branding/missanye.ico --name Missanye --app-version $AppVersion --input target/package --main-jar missanye-desktop-0.1.0-SNAPSHOT.jar --main-class com.missanye.desktop.Launcher --java-options '-Dmissanye.deploymentFile=$APPDIR/parish.properties' --dest target/distribution
if ($LASTEXITCODE -ne 0) { throw 'La création du paquet a échoué.' }
