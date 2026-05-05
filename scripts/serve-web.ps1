$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$webRoot = Join-Path $root "web"
$port = 4173

if (-not (Test-Path $webRoot)) {
    throw "web directory not found: $webRoot"
}

Write-Host "Serving mobile test page from: $webRoot"
Write-Host "Computer URL: http://127.0.0.1:$port/"
Write-Host "Phone URL candidates:"
ipconfig | Select-String -Pattern "IPv4" | ForEach-Object {
    $ip = ($_ -split ":\s*", 2)[1].Trim()
    Write-Host "  http://$ip`:$port/"
}
Write-Host ""
Write-Host "Keep this PowerShell window open while testing on the phone."
Write-Host "If Windows Firewall asks, allow access on Private networks."
Write-Host ""

$python = Get-Command py -ErrorAction SilentlyContinue
if ($python) {
    Set-Location $webRoot
    py -3 -m http.server $port --bind 0.0.0.0
    exit $LASTEXITCODE
}

$python = Get-Command python -ErrorAction SilentlyContinue
if ($python) {
    Set-Location $webRoot
    python -m http.server $port --bind 0.0.0.0
    exit $LASTEXITCODE
}

throw "Python was not found. Install Python or open web/index.html directly on the phone."
