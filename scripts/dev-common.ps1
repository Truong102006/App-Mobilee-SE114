Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
}

function Get-LocalDevDir {
    Join-Path (Get-RepoRoot) ".agent\local-dev"
}

function Ensure-LocalDevDir {
    $dir = Get-LocalDevDir
    if (-not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
    $dir
}

function Resolve-BackendStatePath {
    Join-Path (Ensure-LocalDevDir) "backend.json"
}

function Resolve-BackendStdOutPath {
    Join-Path (Ensure-LocalDevDir) "backend.stdout.log"
}

function Resolve-BackendStdErrPath {
    Join-Path (Ensure-LocalDevDir) "backend.stderr.log"
}

function Convert-GradlePathToWindowsPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PathValue
    )

    ($PathValue -replace '\\:', ':' -replace '\\\\', '\').Trim()
}

function Resolve-AndroidSdkDir {
    if ($env:ANDROID_SDK_ROOT -and (Test-Path -LiteralPath $env:ANDROID_SDK_ROOT)) {
        return (Resolve-Path -LiteralPath $env:ANDROID_SDK_ROOT).Path
    }

    if ($env:ANDROID_HOME -and (Test-Path -LiteralPath $env:ANDROID_HOME)) {
        return (Resolve-Path -LiteralPath $env:ANDROID_HOME).Path
    }

    $localPropertiesPath = Join-Path (Get-RepoRoot) "local.properties"
    if (-not (Test-Path -LiteralPath $localPropertiesPath)) {
        throw "Android SDK was not found. local.properties is missing and ANDROID_SDK_ROOT is not set."
    }

    $sdkLine = Get-Content -LiteralPath $localPropertiesPath |
        Where-Object { $_ -like "sdk.dir=*" } |
        Select-Object -First 1

    if (-not $sdkLine) {
        throw "sdk.dir was not found in local.properties."
    }

    $sdkDir = Convert-GradlePathToWindowsPath -PathValue $sdkLine.Substring("sdk.dir=".Length)
    if (-not (Test-Path -LiteralPath $sdkDir)) {
        throw "Android SDK directory does not exist: $sdkDir"
    }

    (Resolve-Path -LiteralPath $sdkDir).Path
}

function Resolve-AdbPath {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCommand) {
        return $adbCommand.Source
    }

    $adbPath = Join-Path (Resolve-AndroidSdkDir) "platform-tools\adb.exe"
    if (-not (Test-Path -LiteralPath $adbPath)) {
        throw "adb.exe was not found at $adbPath."
    }

    $adbPath
}

function Resolve-EmulatorPath {
    $emulatorPath = Join-Path (Resolve-AndroidSdkDir) "emulator\emulator.exe"
    if (-not (Test-Path -LiteralPath $emulatorPath)) {
        throw "emulator.exe was not found at $emulatorPath."
    }

    $emulatorPath
}

function Resolve-GradleWrapperPath {
    $gradlePath = Join-Path (Get-RepoRoot) "gradlew.bat"
    if (-not (Test-Path -LiteralPath $gradlePath)) {
        throw "gradlew.bat was not found at $gradlePath."
    }

    $gradlePath
}

function Resolve-DebugApkPath {
    Join-Path (Get-RepoRoot) "app\build\outputs\apk\debug\app-debug.apk"
}

function Save-BackendState {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$State
    )

    $State | ConvertTo-Json | Set-Content -LiteralPath (Resolve-BackendStatePath)
}

function Get-BackendState {
    $statePath = Resolve-BackendStatePath
    if (-not (Test-Path -LiteralPath $statePath)) {
        return $null
    }

    Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
}

function Remove-BackendState {
    $statePath = Resolve-BackendStatePath
    if (Test-Path -LiteralPath $statePath) {
        Remove-Item -LiteralPath $statePath -Force
    }
}

function Get-LogTail {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [int]$LineCount = 20
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return ""
    }

    (Get-Content -LiteralPath $Path -Tail $LineCount -ErrorAction SilentlyContinue) -join [Environment]::NewLine
}

function Test-TcpPort {
    param(
        [string]$ComputerName = "127.0.0.1",
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutMs = 1000
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($ComputerName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }

        $client.EndConnect($async) | Out-Null
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Wait-ForTcpPort {
    param(
        [string]$ComputerName = "127.0.0.1",
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutSeconds = 120,
        [int]$ProcessId = 0,
        [string]$StdErrPath = ""
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort -ComputerName $ComputerName -Port $Port) {
            return
        }

        if ($ProcessId -gt 0 -and -not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            $tail = if ($StdErrPath) { Get-LogTail -Path $StdErrPath } else { "" }
            $message = "Backend process exited before port $Port became ready."
            if ($tail) {
                $message += [Environment]::NewLine + $tail
            }
            throw $message
        }

        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for localhost:$Port."
}

function Wait-ForTcpPortToClose {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 15
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-TcpPort -Port $Port)) {
            return
        }
        Start-Sleep -Seconds 1
    }
}

function Get-PortOwnerPid {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($connection) {
        return [int]$connection.OwningProcess
    }

    return 0
}

function Stop-ManagedBackend {
    param(
        [switch]$Quiet
    )

    $state = Get-BackendState
    if (-not $state) {
        if (-not $Quiet) {
            Write-Host "No managed backend process is currently tracked."
        }
        return
    }

    if (Get-Process -Id $state.Pid -ErrorAction SilentlyContinue) {
        cmd /c "taskkill /PID $($state.Pid) /T /F" | Out-Null
    }

    Start-Sleep -Seconds 2

    if ($state.RuntimePid -and [int]$state.RuntimePid -gt 0) {
        $runtimePid = [int]$state.RuntimePid
        if (Get-Process -Id $runtimePid -ErrorAction SilentlyContinue) {
            cmd /c "taskkill /PID $runtimePid /T /F" | Out-Null
        }
    } elseif ($state.Port) {
        $portOwnerPid = Get-PortOwnerPid -Port ([int]$state.Port)
        if ($portOwnerPid -gt 0) {
            cmd /c "taskkill /PID $portOwnerPid /T /F" | Out-Null
        }
    }

    if ($state.Port) {
        Wait-ForTcpPortToClose -Port ([int]$state.Port)
    }

    Remove-BackendState

    if (-not $Quiet) {
        Write-Host "Stopped managed backend process $($state.Pid)."
    }
}

function Ensure-BackendBuildToolAvailable {
    $backendRoot = Join-Path (Get-RepoRoot) "backend-spring"
    $wrapperPath = Join-Path $backendRoot "mvnw.cmd"
    if (Test-Path -LiteralPath $wrapperPath) {
        return
    }

    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        return
    }

    throw "Neither backend-spring\\mvnw.cmd nor global Maven was found. Run scripts\\setup-dev-machine.ps1 or install Maven first."
}

function Ensure-BackendProcess {
    param(
        [int]$Port = 8080,
        [switch]$DisableAppCheck,
        [switch]$Restart,
        [string]$EnvFile = ""
    )

    Ensure-BackendBuildToolAvailable

    if ($Restart) {
        Stop-ManagedBackend -Quiet
    }

    $state = Get-BackendState
    if ($state) {
        $needsRestart = -not (Get-Process -Id $state.Pid -ErrorAction SilentlyContinue)
        if (-not $needsRestart -and [bool]$state.DisableAppCheck -ne [bool]$DisableAppCheck) {
            $needsRestart = $true
        }

        if ($needsRestart) {
            Stop-ManagedBackend -Quiet
            $state = $null
        }
    }

    if ($state -and (Test-TcpPort -Port $Port)) {
        return [pscustomobject]@{
            Managed = $true
            Pid = [int]$state.Pid
            RuntimePid = [int]($state.RuntimePid | ForEach-Object { $_ })
            Port = $Port
            StdOut = $state.StdOut
            StdErr = $state.StdErr
            DisableAppCheck = [bool]$state.DisableAppCheck
        }
    }

    if (Test-TcpPort -Port $Port) {
        $existingPid = Get-PortOwnerPid -Port $Port
        return [pscustomobject]@{
            Managed = $false
            Pid = $existingPid
            Port = $Port
            StdOut = ""
            StdErr = ""
            DisableAppCheck = $null
        }
    }

    $stdoutPath = Resolve-BackendStdOutPath
    $stderrPath = Resolve-BackendStdErrPath
    if (Test-Path -LiteralPath $stdoutPath) {
        Remove-Item -LiteralPath $stdoutPath -Force
    }
    if (Test-Path -LiteralPath $stderrPath) {
        Remove-Item -LiteralPath $stderrPath -Force
    }

    $entryScript = Join-Path $PSScriptRoot "dev-backend-entry.ps1"
    $argumentList = "-NoProfile -ExecutionPolicy Bypass -File `"$entryScript`" -Port $Port"
    if ($EnvFile) {
        $argumentList += " -EnvFile `"$EnvFile`""
    }
    if ($DisableAppCheck) {
        $argumentList += " -DisableAppCheck"
    }

    $process = Start-Process -FilePath "powershell.exe" `
        -ArgumentList $argumentList `
        -WorkingDirectory (Get-RepoRoot) `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru

    Save-BackendState -State ([pscustomobject]@{
        Pid = $process.Id
        Port = $Port
        StdOut = $stdoutPath
        StdErr = $stderrPath
        DisableAppCheck = [bool]$DisableAppCheck
        StartedAt = (Get-Date).ToString("o")
    })

    Wait-ForTcpPort -Port $Port -TimeoutSeconds 120 -ProcessId $process.Id -StdErrPath $stderrPath

    $runtimePid = Get-PortOwnerPid -Port $Port
    Save-BackendState -State ([pscustomobject]@{
        Pid = $process.Id
        RuntimePid = $runtimePid
        Port = $Port
        StdOut = $stdoutPath
        StdErr = $stderrPath
        DisableAppCheck = [bool]$DisableAppCheck
        StartedAt = (Get-Date).ToString("o")
    })

    [pscustomobject]@{
        Managed = $true
        Pid = $process.Id
        RuntimePid = $runtimePid
        Port = $Port
        StdOut = $stdoutPath
        StdErr = $stderrPath
        DisableAppCheck = [bool]$DisableAppCheck
    }
}

function Invoke-GradleTask {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Tasks
    )

    $gradlePath = Resolve-GradleWrapperPath
    Push-Location (Get-RepoRoot)
    try {
        & $gradlePath @Tasks
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle task failed: $($Tasks -join ' ')"
        }
    } finally {
        Pop-Location
    }
}

function Build-DebugApk {
    param(
        [switch]$SkipBuild
    )

    if (-not $SkipBuild) {
        Invoke-GradleTask -Tasks @(":app:assembleDebug")
    }

    $apkPath = Resolve-DebugApkPath
    if (-not (Test-Path -LiteralPath $apkPath)) {
        throw "Debug APK was not found at $apkPath."
    }

    $apkPath
}

function Get-ConnectedAndroidDevices {
    $adbPath = Resolve-AdbPath

    $lines = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        & $adbPath start-server | Out-Null
        $lines = & $adbPath devices
        if ($LASTEXITCODE -eq 0) {
            break
        }
        Start-Sleep -Seconds 2
    }

    if ($LASTEXITCODE -ne 0 -or -not $lines) {
        throw "adb devices failed."
    }

    $devices = @()
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("List of devices attached")) {
            continue
        }

        $parts = $line -split "\s+"
        if ($parts.Count -lt 2) {
            continue
        }

        $serial = $parts[0].Trim()
        $state = $parts[1].Trim()
        $devices += [pscustomobject]@{
            Serial = $serial
            State = $state
            IsEmulator = $serial.StartsWith("emulator-")
        }
    }

    $devices
}

function Get-AvailableAvdNames {
    $emulatorPath = Resolve-EmulatorPath
    @(
        & $emulatorPath -list-avds |
            ForEach-Object { $_.Trim() } |
            Where-Object { $_ }
    )
}

function Wait-ForAndroidBoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [int]$TimeoutSeconds = 240
    )

    $adbPath = Resolve-AdbPath
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $device = Get-ConnectedAndroidDevices |
            Where-Object { $_.Serial -eq $Serial } |
            Select-Object -First 1

        if ($device -and $device.State -eq "device") {
            $bootCompleted = (& $adbPath -s $Serial shell getprop sys.boot_completed 2>$null | Out-String).Trim()
            if ($bootCompleted -eq "1") {
                return
            }
        }

        Start-Sleep -Seconds 3
    }

    throw "Device $Serial did not finish booting within $TimeoutSeconds seconds."
}

function Start-Or-ResolveEmulator {
    param(
        [string]$AvdName = ""
    )

    $runningEmulators = @(
        Get-ConnectedAndroidDevices |
            Where-Object { $_.IsEmulator } |
            Select-Object -ExpandProperty Serial
    )

    if (-not $AvdName -and $runningEmulators.Count -gt 0) {
        $serial = $runningEmulators[0]
        Wait-ForAndroidBoot -Serial $serial
        return $serial
    }

    $availableAvds = @(Get-AvailableAvdNames)
    if (-not $AvdName) {
        if ($availableAvds.Count -eq 0) {
            throw "No Android Virtual Device is available. Create an AVD in Android Studio first."
        }
        if ($availableAvds.Count -gt 1) {
            throw "Multiple AVDs are available: $($availableAvds -join ', '). Re-run with -AvdName <name>."
        }
        $AvdName = $availableAvds[0]
    } elseif ($availableAvds -notcontains $AvdName) {
        throw "AVD '$AvdName' was not found. Available AVDs: $($availableAvds -join ', ')"
    }

    $emulatorPath = Resolve-EmulatorPath
    Start-Process -FilePath $emulatorPath -ArgumentList @("-avd", $AvdName) | Out-Null

    $deadline = (Get-Date).AddSeconds(240)
    while ((Get-Date) -lt $deadline) {
        $currentEmulators = @(
            Get-ConnectedAndroidDevices |
                Where-Object { $_.IsEmulator } |
                Select-Object -ExpandProperty Serial
        )

        $newEmulators = @($currentEmulators | Where-Object { $runningEmulators -notcontains $_ })
        if ($newEmulators.Count -gt 0) {
            $serial = $newEmulators[0]
            Wait-ForAndroidBoot -Serial $serial
            return $serial
        }

        Start-Sleep -Seconds 3
    }

    throw "Timed out waiting for emulator '$AvdName' to connect."
}

function Resolve-PhysicalDeviceSerial {
    param(
        [string]$Serial = ""
    )

    $physicalDevices = @(
        Get-ConnectedAndroidDevices |
            Where-Object { -not $_.IsEmulator -and $_.State -eq "device" }
    )

    if ($Serial) {
        $selectedDevice = $physicalDevices | Where-Object { $_.Serial -eq $Serial } | Select-Object -First 1
        if (-not $selectedDevice) {
            throw "Physical device '$Serial' is not connected or not authorized."
        }
        return $selectedDevice.Serial
    }

    if ($physicalDevices.Count -eq 0) {
        throw "No authorized physical Android device is connected."
    }

    if ($physicalDevices.Count -gt 1) {
        throw "Multiple physical devices are connected: $($physicalDevices.Serial -join ', '). Re-run with -Serial <serial>."
    }

    $physicalDevices[0].Serial
}

function Ensure-ReversePort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [int]$Port = 8080
    )

    $adbPath = Resolve-AdbPath
    & $adbPath -s $Serial reverse "tcp:$Port" "tcp:$Port" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "adb reverse failed for $Serial on port $Port."
    }
}

function Remove-ReversePort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [int]$Port = 8080
    )

    $adbPath = Resolve-AdbPath
    & $adbPath -s $Serial reverse --remove "tcp:$Port" | Out-Null
}

function Install-DebugApk {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [Parameter(Mandatory = $true)]
        [string]$ApkPath
    )

    $adbPath = Resolve-AdbPath
    & $adbPath -s $Serial install -r $ApkPath
    if ($LASTEXITCODE -ne 0) {
        throw "APK installation failed on $Serial."
    }
}

function Start-MainActivity {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [string]$ComponentName = "com.soulmate.app/.MainActivity"
    )

    $adbPath = Resolve-AdbPath
    & $adbPath -s $Serial shell am start -n $ComponentName | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to launch $ComponentName on $Serial."
    }
}
