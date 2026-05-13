# SoulMate Backend Functions

This folder is a backend gateway for the Android app.

Goal:
- Keep Firebase as the data store.
- Move sensitive logic and secrets out of the APK.
- Enforce Auth + App Check before any sensitive action.

## What this backend exposes

Callable functions (region `asia-southeast1`):
- `pingSecure`: auth/app-check sanity check.
- `predictMoodSecure`: calls Gemini from server (API key stays on server).
- `getCloudinarySignedUpload`: returns signed upload params (Cloudinary secret stays on server).
- `saveDiarySecure`, `listMyDiariesSecure`, `deleteDiarySecure`: diary operations with ownership checks.
- `sendChatMessageSecure`, `listConversationSecure`, `deleteConversationSecure`: chat operations with participant checks.

## Security model

1. Android client signs in with Firebase Auth.
2. Client calls callable functions.
3. Function verifies:
   - Firebase Auth (`request.auth.uid`)
   - Firebase App Check (`enforceAppCheck: true`)
4. Function writes/reads Firestore via Admin SDK.
5. Sensitive secrets are read from Secret Manager, not from app code.

## Setup

1. Install dependencies:

```bash
cd functions
npm install
```

2. Set backend secrets (from repo root):

```bash
firebase functions:secrets:set GEMINI_API_KEY
firebase functions:secrets:set CLOUDINARY_CLOUD_NAME
firebase functions:secrets:set CLOUDINARY_API_KEY
firebase functions:secrets:set CLOUDINARY_API_SECRET
firebase functions:secrets:set CLOUDINARY_UPLOAD_FOLDER
```

3. Build:

```bash
npm run build
```

4. Deploy functions only:

```bash
firebase deploy --only functions
```

## Android integration notes

- Replace direct Gemini calls with `predictMoodSecure`.
- Replace unsigned Cloudinary upload with:
  1) call `getCloudinarySignedUpload`
  2) upload using returned signature.
- Replace direct diary/chat writes with secure callable functions.
- After migration is done, tighten Firestore rules so client cannot bypass backend.
