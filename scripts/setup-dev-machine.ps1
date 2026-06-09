param(
    [string]$SdkRoot = "",
    [string]$AvdName = "Pixel_5",
    [switch]$EnsureAvd,
    [switch]$InstallAndroidStudio
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "dev-common.ps1")

$requiredJavaMajor = 17
$recommendedJdkWingetId = "EclipseAdoptium.Temurin.21.JDK"
$androidStudioWingetId = "Google.AndroidStudio"
$requiredPlatformPackage = "platforms;android-34"
$requiredBuildToolsPackage = "build-tools;34.0.0"
$requiredPlatformToolsPackage = "platform-tools"
$requiredEmulatorPackage = "emulator"
$requiredSystemImagePackage = "system-images;android-34;google_apis;x86_64"
$defaultAvdDeviceId = "pixel_5"

function Write-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host "[setup] $Message"
}

function Ensure-Directory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Refresh-SessionEnvironment {
    $machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    $pathParts = @($machinePath, $userPath) | Where-Object { $_ }
    if ($pathParts.Count -gt 0) {
        $env:Path = ($pathParts -join ";")
    }

    foreach ($name in @("JAVA_HOME", "ANDROID_SDK_ROOT", "ANDROID_HOME")) {
        $userValue = [System.Environment]::GetEnvironmentVariable($name, "User")
        $machineValue = [System.Environment]::GetEnvironmentVariable($name, "Machine")
        if ($userValue) {
            Set-Item -Path ("Env:" + $name) -Value $userValue
        } elseif ($machineValue) {
            Set-Item -Path ("Env:" + $name) -Value $machineValue
        }
    }
}

function Set-UserEnvironmentVariable {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $current = [System.Environment]::GetEnvironmentVariable($Name, "User")
    if ($current -ne $Value) {
        [System.Environment]::SetEnvironmentVariable($Name, $Value, "User")
    }
    Set-Item -Path ("Env:" + $Name) -Value $Value
}

function Add-UserPathEntry {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Entry
    )

    $resolvedEntry = $Entry
    if (Test-Path -LiteralPath $Entry) {
        $resolvedEntry = (Resolve-Path -LiteralPath $Entry).Path
    }

    $normalizedEntry = $resolvedEntry.Trim().TrimEnd('\')
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    $entries = @()
    if ($userPath) {
        $entries = @($userPath -split ';' | Where-Object { $_.Trim() })
    }

    foreach ($existing in $entries) {
        if ($existing.Trim().TrimEnd('\') -ieq $normalizedEntry) {
            return
        }
    }

    $newEntries = @($entries + $resolvedEntry) | Where-Object { $_ }
    [System.Environment]::SetEnvironmentVariable("Path", ($newEntries -join ';'), "User")
}

function Get-JavaVersionOutputFromPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaPath
    )

    if (-not (Test-Path -LiteralPath $JavaPath)) {
        return ""
    }

    try {
        $escapedJavaPath = $JavaPath.Replace('"', '""')
        (& cmd.exe /d /c ('"' + $escapedJavaPath + '" -version 2>&1') | Out-String).Trim()
    } catch {
        ""
    }
}

function Get-JavaVersionOutput {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\java.exe")
    }

    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) {
        $candidates += $java.Source
    }

    foreach ($candidate in $candidates | Where-Object { $_ }) {
        $versionOutput = Get-JavaVersionOutputFromPath -JavaPath $candidate
        if ($versionOutput) {
            return $versionOutput
        }
    }

    return ""
}

function Get-JavaMajorVersionFromOutput {
    param(
        [string]$VersionOutput
    )

    if (-not $VersionOutput) {
        return 0
    }

    $match = [regex]::Match($VersionOutput, 'version\s+"(?<version>[0-9]+(?:\.[0-9]+)*)"', 'IgnoreCase')
    if (-not $match.Success) {
        return 0
    }

    $versionText = $match.Groups["version"].Value
    $firstPart = ($versionText -split '\.')[0]
    if ($firstPart -eq "1") {
        $parts = $versionText -split '\.'
        if ($parts.Count -ge 2) {
            return [int]$parts[1]
        }
    }

    [int]$firstPart
}

function Get-JavaMajorVersion {
    $versionOutput = Get-JavaVersionOutput
    if (-not $versionOutput) {
        return 0
    }

    Get-JavaMajorVersionFromOutput -VersionOutput $versionOutput
}

function Resolve-JavaHomePath {
    $candidateValues = @()
    foreach ($scope in @("Process", "User", "Machine")) {
        $value = [System.Environment]::GetEnvironmentVariable("JAVA_HOME", $scope)
        if ($value) {
            $candidateValues += $value
        }
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $candidateValues += (Split-Path -Parent (Split-Path -Parent $javaCommand.Source))
    }

    $searchRoots = @(
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path ${env:ProgramFiles(x86)} "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path ${env:ProgramFiles(x86)} "Java"),
        (Join-Path $env:ProgramFiles "Microsoft"),
        (Join-Path $env:ProgramFiles "Android"),
        (Join-Path $env:LOCALAPPDATA "Programs")
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    foreach ($root in $searchRoots) {
        $matches = Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
            Sort-Object LastWriteTime -Descending
        foreach ($match in $matches) {
            $candidateValues += $match.FullName
        }
    }

    foreach ($candidate in $candidateValues | Where-Object { $_ }) {
        if (Test-Path -LiteralPath (Join-Path $candidate "bin\java.exe")) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    return ""
}

function Ensure-WingetAvailable {
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw "winget was not found. Install App Installer from Microsoft first, then rerun this setup script."
    }
}

function Invoke-WingetInstall {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Id
    )

    Ensure-WingetAvailable

    Write-Step "Installing $Id with winget..."
    & winget install --id $Id -e --silent --accept-package-agreements --accept-source-agreements --disable-interactivity
    $installExitCode = $LASTEXITCODE
    if ($installExitCode -eq 0) {
        return
    }

    $listOutput = & winget list --id $Id -e --accept-source-agreements 2>$null
    if ($LASTEXITCODE -eq 0 -and $listOutput -match [regex]::Escape($Id)) {
        Write-Step "$Id is already installed."
        return
    }

    throw "winget install failed for $Id with exit code $installExitCode."
}

function Ensure-JavaRuntime {
    $javaMajor = Get-JavaMajorVersion
    if ($javaMajor -ge $requiredJavaMajor) {
        $javaHome = Resolve-JavaHomePath
        $resolvedMajor = $javaMajor
        if ($javaHome) {
            $resolvedMajor = Get-JavaMajorVersionFromOutput -VersionOutput (Get-JavaVersionOutputFromPath -JavaPath (Join-Path $javaHome "bin\java.exe"))
            Set-UserEnvironmentVariable -Name "JAVA_HOME" -Value $javaHome
            Add-UserPathEntry -Entry (Join-Path $javaHome "bin")
            Refresh-SessionEnvironment
        }
        Write-Step "Java $resolvedMajor detected."
        return
    }

    $javaHome = Resolve-JavaHomePath
    if ($javaHome) {
        $candidateJava = Join-Path $javaHome "bin\java.exe"
        $candidateMajor = Get-JavaMajorVersionFromOutput -VersionOutput (Get-JavaVersionOutputFromPath -JavaPath $candidateJava)
        if ($candidateMajor -ge $requiredJavaMajor) {
            Set-UserEnvironmentVariable -Name "JAVA_HOME" -Value $javaHome
            Add-UserPathEntry -Entry (Join-Path $javaHome "bin")
            Refresh-SessionEnvironment
            Write-Step "Java $candidateMajor detected via JAVA_HOME."
            return
        }
    }

    Write-Step "Java $requiredJavaMajor+ was not found. Installing a JDK..."
    Invoke-WingetInstall -Id $recommendedJdkWingetId
    Refresh-SessionEnvironment

    $javaHome = Resolve-JavaHomePath
    if (-not $javaHome) {
        throw "JDK installation finished, but JAVA_HOME could not be resolved automatically."
    }

    Set-UserEnvironmentVariable -Name "JAVA_HOME" -Value $javaHome
    Add-UserPathEntry -Entry (Join-Path $javaHome "bin")
    Refresh-SessionEnvironment

    $javaMajor = Get-JavaMajorVersion
    if ($javaMajor -lt $requiredJavaMajor) {
        throw "Java is still unavailable after installation."
    }

    Write-Step "Installed Java $javaMajor."
}

function Resolve-TargetSdkRoot {
    param(
        [string]$PreferredPath = ""
    )

    $candidates = @(
        $PreferredPath,
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME
    ) | Where-Object { $_ }

    $localPropertiesPath = Join-Path (Get-RepoRoot) "local.properties"
    if (Test-Path -LiteralPath $localPropertiesPath) {
        $sdkLine = Get-Content -LiteralPath $localPropertiesPath |
            Where-Object { $_ -like "sdk.dir=*" } |
            Select-Object -First 1
        if ($sdkLine) {
            $candidates += (Convert-GradlePathToWindowsPath -PathValue $sdkLine.Substring("sdk.dir=".Length))
        }
    }

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

function Convert-WindowsPathToGradlePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PathValue
    )

    $PathValue.Replace('\', '\\').Replace(':', '\:')
}

function Update-LocalProperties {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath
    )

    $repoRoot = Get-RepoRoot
    $localPropertiesPath = Join-Path $repoRoot "local.properties"
    $sdkLine = "sdk.dir=" + (Convert-WindowsPathToGradlePath -PathValue $SdkRootPath)
    $lines = @()

    if (Test-Path -LiteralPath $localPropertiesPath) {
        $lines = @(Get-Content -LiteralPath $localPropertiesPath)
    }

    $updated = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -like "sdk.dir=*") {
            $lines[$i] = $sdkLine
            $updated = $true
            break
        }
    }

    if (-not $updated) {
        $lines += $sdkLine
    }

    Set-Content -LiteralPath $localPropertiesPath -Value $lines
}

function Resolve-SdkManagerPathForRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath
    )

    $preferred = Join-Path $SdkRootPath "cmdline-tools\latest\bin\sdkmanager.bat"
    if (Test-Path -LiteralPath $preferred) {
        return $preferred
    }

    $cmdlineToolsRoot = Join-Path $SdkRootPath "cmdline-tools"
    if (Test-Path -LiteralPath $cmdlineToolsRoot) {
        $fallback = Get-ChildItem -LiteralPath $cmdlineToolsRoot -Filter "sdkmanager.bat" -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
        if ($fallback) {
            return $fallback
        }
    }

    return ""
}

function Get-CommandLineToolsArchiveUrl {
    $downloadPage = Invoke-WebRequest -UseBasicParsing "https://developer.android.com/studio"
    $match = [regex]::Match($downloadPage.Content, 'commandlinetools-win-[0-9]+_latest\.zip', 'IgnoreCase')
    if (-not $match.Success) {
        throw "Could not locate the latest Android command-line tools URL from developer.android.com."
    }

    "https://dl.google.com/android/repository/$($match.Value)"
}

function Invoke-FileDownload {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [Parameter(Mandatory = $true)]
        [string]$DestinationPath
    )

    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
        & $curl.Source "--location" "--fail" "--retry" "3" "--output" $DestinationPath $Url
        if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $DestinationPath) -and (Get-Item -LiteralPath $DestinationPath).Length -gt 0) {
            return
        }
    }

    Invoke-WebRequest -UseBasicParsing $Url -OutFile $DestinationPath
    if (-not (Test-Path -LiteralPath $DestinationPath) -or (Get-Item -LiteralPath $DestinationPath).Length -eq 0) {
        throw "Download failed for $Url."
    }
}

function Install-AndroidCommandLineTools {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath
    )

    $existing = Resolve-SdkManagerPathForRoot -SdkRootPath $SdkRootPath
    if ($existing) {
        Write-Step "Android command-line tools already available."
        return
    }

    $archiveUrl = Get-CommandLineToolsArchiveUrl
    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("app-mobilee-android-" + [guid]::NewGuid().ToString("N"))
    $zipPath = Join-Path $tempRoot "commandlinetools.zip"
    $extractRoot = Join-Path $tempRoot "extract"
    $latestDir = Join-Path $SdkRootPath "cmdline-tools\latest"

    try {
        Ensure-Directory -Path $tempRoot
        Ensure-Directory -Path $extractRoot

        Write-Step "Downloading Android command-line tools from Google..."
        Invoke-FileDownload -Url $archiveUrl -DestinationPath $zipPath

        Write-Step "Extracting Android command-line tools..."
        Expand-Archive -LiteralPath $zipPath -DestinationPath $extractRoot -Force

        $sourceRoot = Join-Path $extractRoot "cmdline-tools"
        if (-not (Test-Path -LiteralPath $sourceRoot)) {
            $sourceRoot = Get-ChildItem -LiteralPath $extractRoot -Directory -ErrorAction SilentlyContinue |
                Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin") } |
                Select-Object -First 1 -ExpandProperty FullName
        }

        if (-not $sourceRoot) {
            throw "Downloaded Android command-line tools archive had an unexpected folder layout."
        }

        Ensure-Directory -Path (Join-Path $SdkRootPath "cmdline-tools")
        if (Test-Path -LiteralPath $latestDir) {
            Remove-Item -LiteralPath $latestDir -Recurse -Force
        }
        Ensure-Directory -Path $latestDir

        Get-ChildItem -LiteralPath $sourceRoot -Force | ForEach-Object {
            Move-Item -LiteralPath $_.FullName -Destination $latestDir -Force
        }
    } finally {
        if (Test-Path -LiteralPath $tempRoot) {
            Remove-Item -LiteralPath $tempRoot -Recurse -Force
        }
    }

    $installed = Resolve-SdkManagerPathForRoot -SdkRootPath $SdkRootPath
    if (-not $installed) {
        throw "Android command-line tools installation completed, but sdkmanager.bat was still not found."
    }

    Write-Step "Installed Android command-line tools."
}

function Invoke-SdkManager {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $sdkManager = Resolve-SdkManagerPathForRoot -SdkRootPath $SdkRootPath
    if (-not $sdkManager) {
        throw "sdkmanager.bat was not found under $SdkRootPath."
    }

    & $sdkManager "--sdk_root=$SdkRootPath" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "sdkmanager failed with exit code $LASTEXITCODE."
    }
}

function Accept-AndroidLicenses {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath
    )

    Write-Step "Accepting Android SDK licenses..."
    $sdkManager = Resolve-SdkManagerPathForRoot -SdkRootPath $SdkRootPath
    $answers = for ($i = 0; $i -lt 40; $i++) { "y" }
    $answers | & $sdkManager "--sdk_root=$SdkRootPath" "--licenses"
    if ($LASTEXITCODE -ne 0) {
        throw "Accepting Android SDK licenses failed with exit code $LASTEXITCODE."
    }
}

function Get-MissingAndroidPackages {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath,
        [switch]$IncludeEmulator
    )

    $missing = New-Object System.Collections.Generic.List[string]

    if (-not (Test-Path -LiteralPath (Join-Path $SdkRootPath "platform-tools\adb.exe"))) {
        $missing.Add($requiredPlatformToolsPackage)
    }

    if (-not (Test-Path -LiteralPath (Join-Path $SdkRootPath "platforms\android-34\android.jar"))) {
        $missing.Add($requiredPlatformPackage)
    }

    if (-not (Test-Path -LiteralPath (Join-Path $SdkRootPath "build-tools\34.0.0\d8.bat"))) {
        $missing.Add($requiredBuildToolsPackage)
    }

    if ($IncludeEmulator) {
        if (-not (Test-Path -LiteralPath (Join-Path $SdkRootPath "emulator\emulator.exe"))) {
            $missing.Add($requiredEmulatorPackage)
        }

        if (-not (Test-Path -LiteralPath (Join-Path $SdkRootPath "system-images\android-34\google_apis\x86_64\package.xml"))) {
            $missing.Add($requiredSystemImagePackage)
        }
    }

    return @($missing.ToArray())
}

function Ensure-AndroidPackages {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath,
        [string[]]$Packages
    )

    if (-not $Packages -or $Packages.Count -eq 0) {
        Write-Step "Required Android SDK packages are already installed."
        return
    }

    Accept-AndroidLicenses -SdkRootPath $SdkRootPath
    Write-Step ("Installing Android SDK packages: " + ($Packages -join ", "))
    Invoke-SdkManager -SdkRootPath $SdkRootPath -Arguments $Packages
}

function Resolve-AvdManagerPathForRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath
    )

    $preferred = Join-Path $SdkRootPath "cmdline-tools\latest\bin\avdmanager.bat"
    if (Test-Path -LiteralPath $preferred) {
        return $preferred
    }

    $cmdlineToolsRoot = Join-Path $SdkRootPath "cmdline-tools"
    if (Test-Path -LiteralPath $cmdlineToolsRoot) {
        $fallback = Get-ChildItem -LiteralPath $cmdlineToolsRoot -Filter "avdmanager.bat" -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
        if ($fallback) {
            return $fallback
        }
    }

    throw "avdmanager.bat was not found under $SdkRootPath."
}

function Test-AvdExists {
    param(
        [Parameter(Mandatory = $true)]
        [string]$TargetAvdName
    )

    $avdDir = Join-Path $env:USERPROFILE ".android\avd"
    (Test-Path -LiteralPath (Join-Path $avdDir ($TargetAvdName + ".ini"))) -or
        (Test-Path -LiteralPath (Join-Path $avdDir ($TargetAvdName + ".avd")))
}

function Ensure-AndroidAvd {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SdkRootPath,
        [Parameter(Mandatory = $true)]
        [string]$TargetAvdName
    )

    if (Test-AvdExists -TargetAvdName $TargetAvdName) {
        Write-Step "Android Virtual Device '$TargetAvdName' already exists."
        return
    }

    Write-Step "Creating Android Virtual Device '$TargetAvdName'..."
    $avdManager = Resolve-AvdManagerPathForRoot -SdkRootPath $SdkRootPath
    "no" | & $avdManager "--sdk_root=$SdkRootPath" "create" "avd" "--force" "--name" $TargetAvdName "--package" $requiredSystemImagePackage "--device" $defaultAvdDeviceId
    if ($LASTEXITCODE -ne 0) {
        throw "avdmanager failed with exit code $LASTEXITCODE."
    }
}

function Ensure-AndroidStudio {
    Write-Step "Installing Android Studio with winget..."
    Invoke-WingetInstall -Id $androidStudioWingetId
}

if (-not $AvdName) {
    $AvdName = "Pixel_5"
}

if ($InstallAndroidStudio) {
    Ensure-AndroidStudio
}

Ensure-JavaRuntime

$resolvedSdkRoot = Resolve-TargetSdkRoot -PreferredPath $SdkRoot
Ensure-Directory -Path $resolvedSdkRoot

Set-UserEnvironmentVariable -Name "ANDROID_SDK_ROOT" -Value $resolvedSdkRoot
Set-UserEnvironmentVariable -Name "ANDROID_HOME" -Value $resolvedSdkRoot
Add-UserPathEntry -Entry (Join-Path $resolvedSdkRoot "platform-tools")
if ($EnsureAvd) {
    Add-UserPathEntry -Entry (Join-Path $resolvedSdkRoot "emulator")
}

Update-LocalProperties -SdkRootPath $resolvedSdkRoot
Install-AndroidCommandLineTools -SdkRootPath $resolvedSdkRoot
Add-UserPathEntry -Entry (Join-Path $resolvedSdkRoot "cmdline-tools\latest\bin")
Refresh-SessionEnvironment

$env:ANDROID_SDK_ROOT = $resolvedSdkRoot
$env:ANDROID_HOME = $resolvedSdkRoot

$missingPackages = @(Get-MissingAndroidPackages -SdkRootPath $resolvedSdkRoot -IncludeEmulator:$EnsureAvd)
Ensure-AndroidPackages -SdkRootPath $resolvedSdkRoot -Packages $missingPackages

if ($EnsureAvd) {
    Ensure-AndroidAvd -SdkRootPath $resolvedSdkRoot -TargetAvdName $AvdName
}

Write-Step "Machine setup is ready."
$javaSummary = ((Get-JavaVersionOutput) -split "\r?\n" | Select-Object -First 1)
Write-Host "Java: $javaSummary"
Write-Host "Android SDK: $resolvedSdkRoot"
if ($EnsureAvd) {
    Write-Host "AVD ready: $AvdName"
}
Write-Host "Global Maven is optional now because backend-spring\mvnw.cmd will be used automatically."
