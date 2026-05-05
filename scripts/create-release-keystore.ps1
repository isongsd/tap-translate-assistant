$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$keystoreDir = Join-Path $root "keystore"
$keystoreFile = Join-Path $keystoreDir "tap-translate-release.jks"
$propertiesFile = Join-Path $keystoreDir "release-signing.properties"
$alias = "tap_translate_release"

if (Test-Path $keystoreFile) {
    throw "Release keystore already exists: $keystoreFile"
}

if (-not (Test-Path $keystoreDir)) {
    New-Item -ItemType Directory -Path $keystoreDir | Out-Null
}

$storePassword = [Convert]::ToBase64String([Guid]::NewGuid().ToByteArray()).TrimEnd("=")
$keyPassword = $storePassword

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    $fallback = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot\bin\keytool.exe"
    if (-not (Test-Path $fallback)) {
        throw "keytool not found."
    }
    $keytoolPath = $fallback
} else {
    $keytoolPath = $keytool.Source
}

& $keytoolPath -genkeypair `
    -keystore $keystoreFile `
    -storepass $storePassword `
    -keypass $keyPassword `
    -alias $alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -dname "CN=Lurela App Lab, O=Lurela App Lab, C=TW"

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

@(
    "storeFile=$keystoreFile"
    "storePassword=$storePassword"
    "keyAlias=$alias"
    "keyPassword=$keyPassword"
) | Set-Content -LiteralPath $propertiesFile -Encoding UTF8

Write-Host ""
Write-Host "Release keystore created: $keystoreFile"
Write-Host "Signing properties created: $propertiesFile"
Write-Host "Back up both files. Losing them may block future app updates."
