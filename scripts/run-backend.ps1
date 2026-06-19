param(
    [int]$Port = 8080,
    [string]$EnvFile = "",
    [switch]$DisableAppCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$backendScript = Join-Path $repoRoot "backend-spring\scripts\run-backend-local.ps1"

if (-not (Test-Path -LiteralPath $backendScript)) {
    throw "Backend run script was not found at $backendScript."
}

$argumentList = @{
    Port = $Port
}

if ($EnvFile) {
    if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
        $EnvFile = Join-Path $repoRoot $EnvFile
    }
    $argumentList["EnvFile"] = $EnvFile
}

if ($DisableAppCheck) {
    $argumentList["DisableAppCheck"] = $true
}

& $backendScript @argumentList
exit $LASTEXITCODE
