param(
    [string]$EnvFile = "",
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

if (-not $EnvFile) {
    $EnvFile = Join-Path $projectRoot ".env"
}

if (Test-Path -LiteralPath $EnvFile) {
    Get-Content -LiteralPath $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $idx = $line.IndexOf("=")
        if ($idx -lt 1) {
            return
        }

        $name = $line.Substring(0, $idx).Trim()
        $name = $name.TrimStart([char]0xFEFF)
        $value = $line.Substring($idx + 1).Trim()

        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path ("Env:" + $name) -Value $value
    }
}

if (-not $env:GOOGLE_APPLICATION_CREDENTIALS) {
    $defaultSa = Join-Path $projectRoot "secrets\service-account.json"
    if (Test-Path -LiteralPath $defaultSa) {
        $env:GOOGLE_APPLICATION_CREDENTIALS = (Resolve-Path -LiteralPath $defaultSa).Path
    }
}

if ($DisableAppCheck) {
    $env:BACKEND_SECURITY_REQUIRE_APP_CHECK = "false"
}

$mavenCommand = Resolve-MavenCommand -ProjectRoot $projectRoot
& $mavenCommand "spring-boot:run"

if ($LASTEXITCODE -ne 0) {
    throw "Backend launch failed with exit code $LASTEXITCODE."
}
