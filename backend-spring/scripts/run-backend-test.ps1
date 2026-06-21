param(
    [int]$Port = 18080,
    [string]$ServiceAccountPath = "",
    [string]$GoogleServicesPath = "..\\app\\google-services.json",
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

function Resolve-GoogleServicesConfig {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $resolvedPath = (Resolve-Path -LiteralPath $Path).Path
    $raw = Get-Content -LiteralPath $resolvedPath -Raw -Encoding UTF8
    $json = $raw | ConvertFrom-Json
    $projectInfo = $json.project_info

    if (-not $projectInfo.project_id) {
        throw "Cannot find project_info.project_id in google-services.json."
    }

    if (-not $projectInfo.storage_bucket) {
        throw "Cannot find project_info.storage_bucket in google-services.json."
    }

    return [pscustomobject]@{
        ProjectId = [string]$projectInfo.project_id
        StorageBucket = [string]$projectInfo.storage_bucket
        ProjectNumber = if ($projectInfo.project_number) { [string]$projectInfo.project_number } else { "" }
    }
}

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $projectRoot

if (-not $ServiceAccountPath) {
    $ServiceAccountPath = Join-Path $projectRoot "secrets\\service-account.json"
}

$resolvedSa = (Resolve-Path -LiteralPath $ServiceAccountPath).Path

if (-not [System.IO.Path]::IsPathRooted($GoogleServicesPath)) {
    $GoogleServicesPath = Join-Path $projectRoot $GoogleServicesPath
}

$firebaseConfig = Resolve-GoogleServicesConfig -Path $GoogleServicesPath

$env:SERVER_PORT = "$Port"
$env:GOOGLE_APPLICATION_CREDENTIALS = $resolvedSa
$env:FIREBASE_PROJECT_ID = $firebaseConfig.ProjectId
$env:FIREBASE_STORAGE_BUCKET = $firebaseConfig.StorageBucket

if ($firebaseConfig.ProjectNumber) {
    $env:BACKEND_SECURITY_FIREBASE_PROJECT_NUMBER = $firebaseConfig.ProjectNumber
}

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
