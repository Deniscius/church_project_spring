param(
    [string]$IsccPath,
    [string]$AppVersion = '0.2.1'
)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$appImage = Join-Path (Get-Location) 'target/distribution/Missanye'
if (-not (Test-Path (Join-Path $appImage 'Missanye.exe'))) {
    throw "Image Windows absente. Exécuter ./scripts/package.ps1 avant de construire l'installateur."
}
if ($AppVersion -notmatch '^\d+\.\d+\.\d+(\.\d+)?$') {
    throw 'AppVersion doit être une version numérique, par exemple 0.1.0.'
}
if (-not $IsccPath) {
    $command = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($command) { $IsccPath = $command.Source }
}
if (-not $IsccPath) {
    $candidates = @(
        "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
        "$env:ProgramFiles\Inno Setup 6\ISCC.exe"
    )
    $IsccPath = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $IsccPath -or -not (Test-Path $IsccPath)) {
    throw 'Inno Setup 6 introuvable. Installer Inno Setup depuis https://jrsoftware.org/isinfo.php ou préciser -IsccPath.'
}
$output = Join-Path (Get-Location) 'target/installer'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$installer = Join-Path $output 'Missanye-Setup.exe'
# Refuse to report a stale installer as a successful build.
if (Test-Path $installer) { Remove-Item $installer }
& $IsccPath "/DAppImage=$appImage" "/DAppVersion=$AppVersion" "/O$output" 'installer/Missanye.iss'
if ($LASTEXITCODE -ne 0) { throw 'La compilation Inno Setup a échoué.' }
if (-not (Test-Path $installer)) { throw "Inno Setup n'a pas produit Missanye-Setup.exe." }
(Get-FileHash $installer -Algorithm SHA256).Hash | Set-Content "$installer.sha256"
Write-Output "Installateur créé : $installer"
