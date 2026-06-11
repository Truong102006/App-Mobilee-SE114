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

function Resolve-JavaCommand {
    if ($env:JAVA_HOME) {
        $javaFromHome = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path -LiteralPath $javaFromHome) {
            return $javaFromHome
        }
    }

    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) {
        return $java.Source
    }

    throw "Java executable was not found. Run scripts\\setup-dev-machine.ps1 or install Java first."
}

function Resolve-BuiltJarPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    $targetDir = Join-Path $ProjectRoot "target"
    $jar = Get-ChildItem -LiteralPath $targetDir -Filter "*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*.original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        throw "Could not find a built Spring Boot jar under $targetDir."
    }

    return $jar.FullName
}

function Add-SpringRunArgument {
    param(
        [System.Collections.Generic.List[string]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$PropertyName,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return
    }

    $Arguments.Add("--$PropertyName=$Value")
}

function Get-ConfigValue {
    param(
        [hashtable]$ConfigValues,
        [Parameter(Mandatory = $true)]
        [string]$Key,
        [string]$Fallback = ""
    )

    if ($ConfigValues.ContainsKey($Key) -and -not [string]::IsNullOrWhiteSpace($ConfigValues[$Key])) {
        return [string]$ConfigValues[$Key]
    }

    return $Fallback
}

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $projectRoot

if (-not $EnvFile) {
    $EnvFile = Join-Path $projectRoot ".env"
}

$configValues = @{}
Write-Host ("Using env file: " + $EnvFile)
if (Test-Path -LiteralPath $EnvFile) {
    foreach ($rawLine in Get-Content -LiteralPath $EnvFile) {
        $line = $rawLine.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            continue
        }

        $idx = $line.IndexOf("=")
        if ($idx -lt 1) {
            continue
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

        $configValues[$name] = $value
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
    $configValues["BACKEND_SECURITY_REQUIRE_APP_CHECK"] = "false"
}

$mavenCommand = Resolve-MavenCommand -ProjectRoot $projectRoot
$javaCommand = Resolve-JavaCommand
$springRunArguments = New-Object 'System.Collections.Generic.List[string]'
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "spring.profiles.active" -Value (Get-ConfigValue -ConfigValues $configValues -Key "SPRING_PROFILES_ACTIVE")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "server.port" -Value (Get-ConfigValue -ConfigValues $configValues -Key "SERVER_PORT" -Fallback $env:SERVER_PORT)
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "firebase.project-id" -Value (Get-ConfigValue -ConfigValues $configValues -Key "FIREBASE_PROJECT_ID")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "firebase.storage-bucket" -Value (Get-ConfigValue -ConfigValues $configValues -Key "FIREBASE_STORAGE_BUCKET")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.security.require-app-check" -Value (Get-ConfigValue -ConfigValues $configValues -Key "BACKEND_SECURITY_REQUIRE_APP_CHECK" -Fallback $env:BACKEND_SECURITY_REQUIRE_APP_CHECK)
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.security.firebase-project-number" -Value (Get-ConfigValue -ConfigValues $configValues -Key "BACKEND_SECURITY_FIREBASE_PROJECT_NUMBER")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.gemini.api-key" -Value (Get-ConfigValue -ConfigValues $configValues -Key "GEMINI_API_KEY")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.gemini.model" -Value (Get-ConfigValue -ConfigValues $configValues -Key "GEMINI_MODEL")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.cloudinary.cloud-name" -Value (Get-ConfigValue -ConfigValues $configValues -Key "CLOUDINARY_CLOUD_NAME")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.cloudinary.api-key" -Value (Get-ConfigValue -ConfigValues $configValues -Key "CLOUDINARY_API_KEY")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.cloudinary.api-secret" -Value (Get-ConfigValue -ConfigValues $configValues -Key "CLOUDINARY_API_SECRET")
Add-SpringRunArgument -Arguments $springRunArguments -PropertyName "backend.cloudinary.upload-folder" -Value (Get-ConfigValue -ConfigValues $configValues -Key "CLOUDINARY_UPLOAD_FOLDER")

$mavenBuildArgs = @("-q", "-DskipTests", "package")
& $mavenCommand @mavenBuildArgs

if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed with exit code $LASTEXITCODE."
}

$jarPath = Resolve-BuiltJarPath -ProjectRoot $projectRoot
$javaArgs = @("-jar", $jarPath) + @($springRunArguments)
Write-Host ("Launching backend jar with args: " + ($springRunArguments -join " "))

& $javaCommand @javaArgs

if ($LASTEXITCODE -ne 0) {
    throw "Backend launch failed with exit code $LASTEXITCODE."
}
