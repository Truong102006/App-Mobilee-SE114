# Local Backend Debug Setup

The Android app now resolves the backend URL like this:

- Explicit `BACKEND_BASE_URL` override wins.
- Emulator default: `http://10.0.2.2:8080/`
- Physical device default: `http://localhost:8080/`

## Recommended one-command workflow

These scripts now bootstrap a new Windows machine for you on first run: they check Java, install Android command-line SDK tools if needed, write `local.properties`, and only then start the backend, build the debug APK, install it, and launch the app.

## First-time machine setup

If you want to prepare a machine before the first app run, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev-machine.ps1 -EnsureAvd
```

This setup script:

- Installs a JDK if Java 17+ is missing.
- Installs Android command-line SDK tools if the machine has no SDK yet.
- Installs the Android packages required by this project.
- Creates or reuses a `Pixel_5` AVD when `-EnsureAvd` is passed.
- Updates `local.properties`, `ANDROID_SDK_ROOT`, and related PATH entries.

The backend now prefers `backend-spring\mvnw.cmd`, so a separate global Maven install is no longer required on a new machine.

### Emulator

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1
```

If you have multiple AVDs:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -AvdName Pixel_5
```

### Physical Android device over USB

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1
```

If multiple physical devices are connected:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1 -Serial <device-serial>
```

If you already know the machine is ready and want to skip the bootstrap checks:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1 -SkipMachineSetup
```

### Stop local dev services

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local-dev.ps1
```

## Backend behavior

The helper scripts start the backend in the background and store logs under `.agent/local-dev/`.

For local development, App Check is disabled by default because the current Android app does not send the `X-Firebase-AppCheck` header yet. If you want to test with App Check enabled, add `-RequireAppCheck`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone.ps1 -RequireAppCheck
```

The scripts call the existing backend launcher in `backend-spring/scripts/run-backend-local.ps1`, so you can still use your `.env` file and local Firebase credentials as before.

## Manual fallback

### Android Emulator

1. Start the Spring backend on your laptop at port `8080`.
2. Run the app normally.

The app will use `10.0.2.2` automatically, so no extra setup is required.

### Physical Android device over USB

1. Start the Spring backend on your laptop at port `8080`.
2. Connect the device with USB debugging enabled.
3. Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-android-local-backend.ps1
```

The app will use `http://localhost:8080/`, and `adb reverse` will forward that traffic to the laptop backend.

## Custom URL overrides

Use these when you need Wi-Fi/LAN testing or another backend:

```powershell
.\gradlew installDebug -PBACKEND_BASE_URL=http://192.168.1.10:8080/
```

You can also override the defaults independently:

```powershell
.\gradlew installDebug -PBACKEND_BASE_URL_DEVICE=http://localhost:8080/ -PBACKEND_BASE_URL_EMULATOR=http://10.0.2.2:8080/
```
