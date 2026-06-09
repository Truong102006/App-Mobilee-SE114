param(
    [string]$AvdName = "",
    [string]$EnvFile = "",
    [int]$Port = 8080,
    [switch]$RequireAppCheck,
    [switch]$RestartBackend,
    [switch]$SkipBuild,
    [switch]$SkipMachineSetup
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "dev-common.ps1")

if (-not $SkipMachineSetup) {
    & (Join-Path $PSScriptRoot "setup-dev-machine.ps1") -EnsureAvd -AvdName $AvdName
}

$serial = Start-Or-ResolveEmulator -AvdName $AvdName
$backend = Ensure-BackendProcess -Port $Port -DisableAppCheck:(-not $RequireAppCheck) -Restart:$RestartBackend -EnvFile $EnvFile
$apkPath = Build-DebugApk -SkipBuild:$SkipBuild
Install-DebugApk -Serial $serial -ApkPath $apkPath
Start-MainActivity -Serial $serial

Write-Host "Emulator is ready: $serial"
Write-Host "App launched successfully."
if ($backend.Managed) {
    Write-Host "Backend started in the background on http://localhost:$Port/ (PID $($backend.Pid))."
    Write-Host "Backend logs: $($backend.StdOut)"
} else {
    Write-Host "Reused existing backend on http://localhost:$Port/ (PID $($backend.Pid))."
}
