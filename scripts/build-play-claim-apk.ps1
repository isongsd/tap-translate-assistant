$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "android-build-env.ps1")
$sdk = "C:\Users\User\AppData\Local\Android\Sdk"
$gradle = "C:\Users\User\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat"
$debugKeystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
$claimApk = Join-Path $root "app\build\outputs\apk\release\app-play-claim.apk"
$releaseApk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$expectedSha256 = "66:83:A0:A9:EE:D1:57:26:11:FB:D1:6B:D2:11:F8:0F:6A:D7:89:08:F6:95:62:9C:E9:4F:B1:49:DC:AC:08:F5"
$registrationToken = Join-Path $root "app\src\main\assets\adi-registration.properties"

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

if (-not (Test-Path $debugKeystore)) {
    throw "Debug keystore not found: $debugKeystore"
}

if (-not (Test-Path $registrationToken)) {
    throw "Play Console token file not found: $registrationToken"
}

$tokenText = (Get-Content -LiteralPath $registrationToken -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($tokenText) -or $tokenText -eq "PASTE_PLAY_CONSOLE_ADI_REGISTRATION_SNIPPET_HERE") {
    throw "Paste the Play Console snippet into app\src\main\assets\adi-registration.properties before building the claim APK."
}

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if ($keytool) {
    $cert = & $keytool.Source -list -v -keystore $debugKeystore -alias androiddebugkey -storepass android -keypass android
    $fingerprintLine = $cert | Select-String -Pattern "SHA256:" | Select-Object -First 1
    if ($fingerprintLine -and ($fingerprintLine.ToString() -notlike "*$expectedSha256*")) {
        throw "Debug keystore SHA-256 does not match Play Console selected key. Expected: $expectedSha256"
    }
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
Set-AndroidBuildJavaHome

Set-Location $root
& $gradle assembleRelease --no-daemon `
    "-Pandroid.injected.signing.store.file=$debugKeystore" `
    "-Pandroid.injected.signing.store.password=android" `
    "-Pandroid.injected.signing.key.alias=androiddebugkey" `
    "-Pandroid.injected.signing.key.password=android"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not (Test-Path $releaseApk)) {
    throw "Signed release APK not found: $releaseApk"
}

Copy-Item -LiteralPath $releaseApk -Destination $claimApk -Force
Write-Host ""
Write-Host "Play claim APK ready: $claimApk"
Write-Host "Package name: com.ttt.liveassistant"
Write-Host "Expected signing SHA-256: $expectedSha256"
