$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "android-build-env.ps1")
$sdk = "C:\Users\User\AppData\Local\Android\Sdk"
$gradle = "C:\Users\User\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat"
$signingProperties = Join-Path $root "keystore\release-signing.properties"
$releaseBundle = Join-Path $root "app\build\outputs\bundle\release\app-release.aab"
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

if (-not (Test-Path $signingProperties)) {
    throw "Release signing properties not found: $signingProperties. Run .\scripts\create-release-keystore.ps1 first."
}

if (-not (Test-Path $registrationToken)) {
    throw "Play Console token file not found: $registrationToken"
}

$tokenText = (Get-Content -LiteralPath $registrationToken -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($tokenText) -or $tokenText -eq "PASTE_PLAY_CONSOLE_ADI_REGISTRATION_SNIPPET_HERE") {
    throw "Paste the Play Console snippet into app\src\main\assets\adi-registration.properties before building the release AAB."
}

$props = @{}
Get-Content -LiteralPath $signingProperties | ForEach-Object {
    if ($_ -match "^\s*([^#=]+?)\s*=\s*(.*?)\s*$") {
        $props[$matches[1]] = $matches[2]
    }
}

foreach ($key in @("storeFile", "storePassword", "keyAlias", "keyPassword")) {
    if (-not $props.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($props[$key])) {
        throw "Missing $key in $signingProperties"
    }
}

if (-not (Test-Path $props["storeFile"])) {
    throw "Release keystore not found: $($props["storeFile"])"
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
Set-AndroidBuildJavaHome

Set-Location $root
& $gradle bundleRelease --no-daemon `
    "-Pandroid.injected.signing.store.file=$($props["storeFile"])" `
    "-Pandroid.injected.signing.store.password=$($props["storePassword"])" `
    "-Pandroid.injected.signing.key.alias=$($props["keyAlias"])" `
    "-Pandroid.injected.signing.key.password=$($props["keyPassword"])"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not (Test-Path $releaseBundle)) {
    throw "Signed release AAB not found: $releaseBundle"
}

Write-Host ""
Write-Host "Play release AAB ready: $releaseBundle"
Write-Host "Package name: com.ttt.liveassistant"
Write-Host "Signed with release keystore: $($props["storeFile"])"
