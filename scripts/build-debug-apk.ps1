$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "android-build-env.ps1")
$sdk = "C:\Users\User\AppData\Local\Android\Sdk"
$gradle = "C:\Users\User\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat"

if (-not (Test-Path $sdk)) {
    throw "Android SDK not found: $sdk"
}

if (-not (Test-Path $gradle)) {
    $gradleCommand = Get-Command gradle -ErrorAction SilentlyContinue
    if (-not $gradleCommand) {
        throw "Gradle not found. Install Gradle or open the project once in Android Studio."
    }
    $gradle = $gradleCommand.Source
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
Set-AndroidBuildJavaHome

Set-Location $root
& $gradle assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    Write-Host ""
    Write-Host "APK ready: $apk"
}
