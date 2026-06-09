param(
    [int]$Port = 18080,
    [string]$ServiceAccountPath = "",
    [switch]$DisableAppCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-MavenCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    $wrapperPath = Join-Path $ProjectRoot "mvnw.cmd"
    if (Test-Path -LiteralPath $wrapperPath) {
        return $wrapperPath
    }

    $maven = Get-Command mvn -ErrorAction SilentlyContinue
    if ($maven) {
        return $maven.Source
    }

    throw "Neither Maven Wrapper nor mvn was found. Run scripts\\setup-dev-machine.ps1 or install Maven first."
}

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $projectRoot

if (-not $ServiceAccountPath) {
    $ServiceAccountPath = Join-Path $projectRoot "secrets\\service-account.json"
}

$resolvedSa = (Resolve-Path -LiteralPath $ServiceAccountPath).Path

$env:SERVER_PORT = "$Port"
$env:GOOGLE_APPLICATION_CREDENTIALS = $resolvedSa
$env:FIREBASE_PROJECT_ID = "soulmate-app-777bc"
$env:FIREBASE_STORAGE_BUCKET = "soulmate-app-777bc.appspot.com"

if ($DisableAppCheck) {
    $env:BACKEND_SECURITY_REQUIRE_APP_CHECK = "false"
}

# Dummy Cloudinary values are enough for signature generation tests.
if (-not $env:CLOUDINARY_CLOUD_NAME) { $env:CLOUDINARY_CLOUD_NAME = "demo-cloud" }
if (-not $env:CLOUDINARY_API_KEY) { $env:CLOUDINARY_API_KEY = "demo-key" }
if (-not $env:CLOUDINARY_API_SECRET) { $env:CLOUDINARY_API_SECRET = "demo-secret" }
if (-not $env:CLOUDINARY_UPLOAD_FOLDER) { $env:CLOUDINARY_UPLOAD_FOLDER = "soulmate_uploads" }

$mavenCommand = Resolve-MavenCommand -ProjectRoot $projectRoot
& $mavenCommand "spring-boot:run"

if ($LASTEXITCODE -ne 0) {
    throw "Backend test launcher failed with exit code $LASTEXITCODE."
}
