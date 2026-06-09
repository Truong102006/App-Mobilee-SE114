param(
    [int]$Port = 8080,
    [string]$Serial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "dev-common.ps1")

$adb = Resolve-AdbPath
$connectedDevices = @(
    Get-ConnectedAndroidDevices |
        Where-Object { $_.State -eq "device" } |
        Select-Object -ExpandProperty Serial
)

if ($connectedDevices.Count -eq 0) {
    throw "No authorized Android device or emulator is connected."
}

$targetSerial = $Serial
if (-not $targetSerial) {
    if ($connectedDevices.Count -gt 1) {
        throw "Multiple devices detected: $($connectedDevices -join ', '). Re-run with -Serial <serial>."
    }
    $targetSerial = $connectedDevices[0]
}

if ($connectedDevices -notcontains $targetSerial) {
    throw "Device '$targetSerial' is not connected."
}

Ensure-ReversePort -Serial $targetSerial -Port $Port

Write-Host "Configured adb reverse for ${targetSerial}: tcp:$Port -> tcp:$Port"
Write-Host "Your app can now reach the laptop backend at http://localhost:${Port}/"
