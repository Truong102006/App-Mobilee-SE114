# Backend Security Setup (Spring Boot, Monorepo)

This repo now has:
- Android app: `app/`
- Backend (Spring Boot): `backend-spring/`

## Security objective

Keep Firebase as data store, but prevent APK reverse-engineering from exposing sensitive secrets and protected business logic.

## How this backend solves it

1. Secrets move from app to backend environment variables:
   - `GEMINI_API_KEY`
   - `CLOUDINARY_API_SECRET` (and related Cloudinary credentials)
2. Android calls backend endpoints instead of direct secret-facing APIs.
3. Backend verifies:
   - Firebase ID token (Auth)
   - Firebase App Check token (custom backend verification)
4. Backend enforces ownership/permissions for diary/chat data before Firestore access.

## Implemented secure endpoints

- `GET /api/secure/ping`
- `POST /api/secure/ai/predict-mood`
- `POST /api/secure/cloudinary/sign-upload`
- `POST /api/secure/diaries/save`
- `GET /api/secure/diaries/me`
- `DELETE /api/secure/diaries/{diaryId}`
- `POST /api/secure/chats/send`
- `GET /api/secure/chats/conversation/{otherUserId}?limit=100`
- `DELETE /api/secure/chats/conversation/{otherUserId}`

## Added Spring backend files

- `backend-spring/pom.xml`
- `backend-spring/src/main/java/com/soulmate/backend/**`
- `backend-spring/src/main/resources/application.yml`
- `backend-spring/.env.example`
- `backend-spring/README.md`

## Run and build

1. Configure env vars from `backend-spring/.env.example`.
2. Ensure `GOOGLE_APPLICATION_CREDENTIALS` points to service account json.
3. Start backend:
   - `cd backend-spring`
   - `mvn spring-boot:run`
4. Build:
   - `mvn clean package`

## Important migration note

Current Android app still contains direct calls to Gemini/Cloudinary/Firebase client writes in some flows.

To fully satisfy the security requirement:
1. App must call new backend `/api/secure/**` endpoints.
2. Remove all hardcoded keys/secrets from Android code.
3. Tighten Firestore rules after migration so client cannot bypass backend writes.
