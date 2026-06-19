# SoulMate Backend (Spring Boot)

Backend gateway for the Android app, focused on:
- Keeping Firebase as the data store.
- Moving sensitive logic and secrets out of the APK.
- Enforcing Firebase Auth + App Check at backend entrypoints.

## Architecture

Client flow:
1. Android signs in with Firebase Auth.
2. Android calls backend `/api/secure/**` endpoints with:
   - `Authorization: Bearer <Firebase ID Token>`
   - `X-Firebase-AppCheck: <App Check token>`
3. Backend verifies ID token using Firebase Admin SDK.
4. Backend verifies App Check token JWT using Firebase App Check JWKS.
5. Backend performs business logic and reads/writes Firestore.

## Endpoints

- `GET /api/secure/ping`
- `POST /api/secure/ai/predict-mood`
- `POST /api/secure/cloudinary/sign-upload`
- `POST /api/secure/diaries/save`
- `GET /api/secure/diaries/me`
- `DELETE /api/secure/diaries/{diaryId}`
- `POST /api/secure/chats/send`
- `GET /api/secure/chats/conversation/{otherUserId}?limit=100`
- `GET /api/secure/chats/inbox?limit=100`
- `DELETE /api/secure/chats/conversation/{otherUserId}`

## Setup

1. Copy env template and fill values:
   - `backend-spring/.env.example`
2. Keep `GOOGLE_APPLICATION_CREDENTIALS` as a repo-relative path, for example:
   - `secrets/service-account.json`
3. `backend-spring/.env` is auto-imported on startup, so you do not need to export those values into the shell for local runs.
4. Ensure App Check project number is configured:
   - `BACKEND_SECURITY_FIREBASE_PROJECT_NUMBER`

## Run

```powershell
# From the repo root
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
```

```powershell
# Or, if you are already inside backend-spring/
.\mvnw.cmd spring-boot:run
```

## Build

```powershell
cd .\backend-spring
.\mvnw.cmd clean package
```

## Test quickly (PowerShell)

1) Get Firebase ID token from email/password:

```powershell
cd .\backend-spring
$ID_TOKEN = .\scripts\get-firebase-id-token.ps1 -Email "your@email.com" -Password "your_password" -RawTokenOnly
```

2) Start backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
```

3) In another terminal, run smoke test:

```powershell
cd .\backend-spring
.\scripts\smoke-test.ps1 -IdToken $ID_TOKEN -SkipAi -SkipCloudinary
```

If you configured full secrets and App Check token:

```powershell
.\scripts\smoke-test.ps1 -IdToken "<id_token>" -RequireAppCheck -AppCheckToken "<app_check_token>"
```

## Notes

- App Check verification is implemented by verifying JWT signature against:
  - `https://firebaseappcheck.googleapis.com/v1/jwks`
- Cloudinary uses server-generated signed upload parameters.
- Gemini API key is read from environment and never shipped in APK.
- OneSignal push notifications are optional and only active when `ONESIGNAL_ENABLED=true` plus valid `ONESIGNAL_APP_ID` and `ONESIGNAL_API_KEY`.
