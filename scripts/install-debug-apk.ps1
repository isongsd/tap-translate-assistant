$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$adb = "C:\Users\User\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $adb)) {
    throw "adb not found: $adb"
}

if (-not (Test-Path $apk)) {
    throw "APK not found. Run .\scripts\build-debug-apk.ps1 first."
}

Write-Host "Connected devices:"
& $adb devices
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Installing APK..."
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
