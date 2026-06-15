param(
    [int]$Port = 8080
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "dev-common.ps1")

try {
    $physicalDevices = @(
        Get-ConnectedAndroidDevices |
            Where-Object { -not $_.IsEmulator -and $_.State -eq "device" }
    )

    foreach ($device in $physicalDevices) {
        try {
            Remove-ReversePort -Serial $device.Serial -Port $Port
        } catch {
        }
    }
} catch {
    Write-Warning "Skipping adb cleanup: $($_.Exception.Message)"
}

Stop-ManagedBackend -Quiet
Write-Host "Local dev helpers stopped."
