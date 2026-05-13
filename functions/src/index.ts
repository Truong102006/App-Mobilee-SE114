import { initializeApp } from "firebase-admin/app";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import { createHash } from "node:crypto";

initializeApp();

const db = getFirestore();
const REGION = "asia-southeast1";
const DEFAULT_GEMINI_MODEL = "gemini-1.5-flash";

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const CLOUDINARY_CLOUD_NAME = defineSecret("CLOUDINARY_CLOUD_NAME");
const CLOUDINARY_API_KEY = defineSecret("CLOUDINARY_API_KEY");
const CLOUDINARY_API_SECRET = defineSecret("CLOUDINARY_API_SECRET");
const CLOUDINARY_UPLOAD_FOLDER = defineSecret("CLOUDINARY_UPLOAD_FOLDER");

type JsonMap = Record<string, unknown>;

interface DiaryDoc {
  diary_id: string;
  user_id: string;
  title: string;
  text: string;
  mood_tag: string;
  image_urls: string[];
  audio_url: string | null;
  timestamp: FieldValue | Timestamp;
  updated_at: number;
}

interface ChatDoc {
  conversationId: string;
  senderId: string;
  receiverId: string;
  messageText: string;
  imageUrl: string | null;
  timestamp: FieldValue | Timestamp;
}

function asObject(data: unknown): JsonMap {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    throw new HttpsError("invalid-argument", "Request payload must be an object.");
  }
  return data as JsonMap;
}

function requireAuth(request: CallableRequest<unknown>): { uid: string } {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }
  return { uid: request.auth.uid };
}

function requireAppCheck(request: CallableRequest<unknown>): void {
  if (!request.app) {
    throw new HttpsError(
      "failed-precondition",
      "Invalid App Check token. Update the app and retry."
    );
  }
}

function toRequiredString(value: unknown, fieldName: string, maxLength = 5000): string {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${fieldName} must be a string.`);
  }
  const trimmed = value.trim();
  if (!trimmed) {
    throw new HttpsError("invalid-argument", `${fieldName} cannot be empty.`);
  }
  if (trimmed.length > maxLength) {
    throw new HttpsError("invalid-argument", `${fieldName} exceeds ${maxLength} characters.`);
  }
  return trimmed;
}

function toOptionalString(value: unknown, fieldName: string, maxLength = 1000): string | null {
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${fieldName} must be a string or null.`);
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  if (trimmed.length > maxLength) {
    throw new HttpsError("invalid-argument", `${fieldName} exceeds ${maxLength} characters.`);
  }
  return trimmed;
}

function toStringArray(value: unknown, fieldName: string, maxItems = 12): string[] {
  if (value === null || value === undefined) {
    return [];
  }
  if (!Array.isArray(value)) {
    throw new HttpsError("invalid-argument", `${fieldName} must be an array of strings.`);
  }
  if (value.length > maxItems) {
    throw new HttpsError("invalid-argument", `${fieldName} supports up to ${maxItems} items.`);
  }
  return value.map((entry, index) => {
    if (typeof entry !== "string") {
      throw new HttpsError("invalid-argument", `${fieldName}[${index}] must be a string.`);
    }
    const trimmed = entry.trim();
    if (!trimmed) {
      throw new HttpsError("invalid-argument", `${fieldName}[${index}] cannot be empty.`);
    }
    if (trimmed.length > 2048) {
      throw new HttpsError("invalid-argument", `${fieldName}[${index}] is too long.`);
    }
    return trimmed;
  });
}

function toPositiveInt(value: unknown, fieldName: string, fallbackValue: number): number {
  if (value === undefined || value === null) {
    return fallbackValue;
  }
  if (typeof value !== "number" || !Number.isInteger(value) || value <= 0) {
    throw new HttpsError("invalid-argument", `${fieldName} must be a positive integer.`);
  }
  return value;
}

function toMillis(value: unknown): number | null {
  if (value instanceof Timestamp) {
    return value.toMillis();
  }
  return null;
}

function buildConversationId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("__");
}

function buildCloudinarySignature(params: Record<string, string | number>, apiSecret: string): string {
  const sorted = Object.entries(params)
    .filter(([, value]) => value !== null && value !== undefined && `${value}`.length > 0)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${key}=${value}`)
    .join("&");

  return createHash("sha1").update(`${sorted}${apiSecret}`).digest("hex");
}

function extractGeminiText(payload: unknown): string | null {
  if (!payload || typeof payload !== "object") {
    return null;
  }

  const maybeCandidates = (payload as { candidates?: unknown }).candidates;
  if (!Array.isArray(maybeCandidates) || maybeCandidates.length === 0) {
    return null;
  }

  const firstCandidate = maybeCandidates[0] as { content?: { parts?: Array<{ text?: string }> } };
  const parts = firstCandidate.content?.parts;
  if (!Array.isArray(parts)) {
    return null;
  }

  for (const part of parts) {
    if (typeof part.text === "string" && part.text.trim()) {
      return part.text.trim();
    }
  }
  return null;
}

export const pingSecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    return {
      ok: true,
      uid,
      serverTime: Date.now()
    };
  }
);

export const predictMoodSecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true,
    secrets: [GEMINI_API_KEY]
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const payload = asObject(request.data);
    const diaryText = toRequiredString(payload.text, "text", 12000);

    const prompt = [
      "You are a mental health assistant.",
      "Analyze the diary text and return exactly one English mood word.",
      "Examples: Happy, Sad, Angry, Neutral, Excited, Tired.",
      "If unclear, return Neutral.",
      `Diary: """${diaryText}"""`
    ].join("\n");

    const apiKey = GEMINI_API_KEY.value();
    const model = DEFAULT_GEMINI_MODEL;
    const endpoint =
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const geminiResponse = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: {
          temperature: 0.2,
          maxOutputTokens: 20
        }
      })
    });

    if (!geminiResponse.ok) {
      const body = await geminiResponse.text();
      logger.error("Gemini request failed", {
        uid,
        status: geminiResponse.status,
        body
      });
      throw new HttpsError("internal", "Failed to call Gemini model.");
    }

    const rawPayload = await geminiResponse.json();
    const rawText = extractGeminiText(rawPayload) ?? "Neutral";
    const normalized = rawText.split(/[\s,.:;!?]+/)[0] || "Neutral";

    return {
      mood: normalized,
      raw: rawText,
      model
    };
  }
);

export const getCloudinarySignedUpload = onCall(
  {
    region: REGION,
    enforceAppCheck: true,
    secrets: [CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET, CLOUDINARY_UPLOAD_FOLDER]
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const payload = asObject(request.data);
    const optionalPublicId = toOptionalString(payload.publicId, "publicId", 140);
    const uploadContext = toOptionalString(payload.context, "context", 500);

    const cloudName = CLOUDINARY_CLOUD_NAME.value();
    const apiKey = CLOUDINARY_API_KEY.value();
    const apiSecret = CLOUDINARY_API_SECRET.value();
    const folderBase = CLOUDINARY_UPLOAD_FOLDER.value();

    const timestamp = Math.floor(Date.now() / 1000);
    const folder = `${folderBase}/${uid}`;

    const paramsToSign: Record<string, string | number> = {
      folder,
      timestamp
    };

    if (optionalPublicId) {
      paramsToSign.public_id = optionalPublicId;
    }

    if (uploadContext) {
      paramsToSign.context = uploadContext;
    }

    const signature = buildCloudinarySignature(paramsToSign, apiSecret);

    return {
      cloudName,
      apiKey,
      folder,
      timestamp,
      signature,
      publicId: optionalPublicId,
      context: uploadContext,
      uploadUrl: `https://api.cloudinary.com/v1_1/${cloudName}/auto/upload`
    };
  }
);

export const saveDiarySecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const payload = asObject(request.data);
    const inputDiaryId = toOptionalString(payload.diaryId, "diaryId", 160);
    const title = toOptionalString(payload.title, "title", 200) ?? "";
    const text = toRequiredString(payload.text, "text", 12000);
    const moodTag = toOptionalString(payload.moodTag, "moodTag", 64) ?? "Neutral";
    const imageUrls = toStringArray(payload.imageUrls, "imageUrls");
    const audioUrl = toOptionalString(payload.audioUrl, "audioUrl", 2048);

    const docRef = inputDiaryId
      ? db.collection("diaries").doc(inputDiaryId)
      : db.collection("diaries").doc();

    const snapshot = await docRef.get();
    if (snapshot.exists) {
      const existingUserId = snapshot.get("user_id");
      if (existingUserId !== uid) {
        throw new HttpsError("permission-denied", "You cannot edit this diary entry.");
      }
    }

    const diaryDoc: DiaryDoc = {
      diary_id: docRef.id,
      user_id: uid,
      title,
      text,
      mood_tag: moodTag,
      image_urls: imageUrls,
      audio_url: audioUrl,
      timestamp: snapshot.exists ? (snapshot.get("timestamp") as Timestamp) ?? FieldValue.serverTimestamp() : FieldValue.serverTimestamp(),
      updated_at: Date.now()
    };

    await docRef.set(diaryDoc, { merge: true });

    return {
      diaryId: docRef.id,
      updatedAt: diaryDoc.updated_at
    };
  }
);

export const listMyDiariesSecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const snap = await db
      .collection("diaries")
      .where("user_id", "==", uid)
      .orderBy("timestamp", "desc")
      .get();

    const diaries = snap.docs.map((doc) => {
      const data = doc.data();
      return {
        diaryId: doc.id,
        userId: data.user_id ?? uid,
        title: data.title ?? "",
        text: data.text ?? "",
        moodTag: data.mood_tag ?? "Neutral",
        imageUrls: Array.isArray(data.image_urls) ? data.image_urls : [],
        audioUrl: data.audio_url ?? null,
        createdAt: toMillis(data.timestamp),
        updatedAt: typeof data.updated_at === "number" ? data.updated_at : null
      };
    });

    return { diaries };
  }
);

export const deleteDiarySecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const payload = asObject(request.data);
    const diaryId = toRequiredString(payload.diaryId, "diaryId", 160);
    const docRef = db.collection("diaries").doc(diaryId);
    const snapshot = await docRef.get();

    if (!snapshot.exists) {
      throw new HttpsError("not-found", "Diary entry not found.");
    }

    if (snapshot.get("user_id") !== uid) {
      throw new HttpsError("permission-denied", "You cannot delete this diary entry.");
    }

    await docRef.delete();
    return { deleted: true, diaryId };
  }
);

export const sendChatMessageSecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const payload = asObject(request.data);
    const receiverId = toRequiredString(payload.receiverId, "receiverId", 128);
    const messageText = toOptionalString(payload.messageText, "messageText", 4000) ?? "";
    const imageUrl = toOptionalString(payload.imageUrl, "imageUrl", 2048);

    if (!messageText && !imageUrl) {
      throw new HttpsError("invalid-argument", "messageText or imageUrl is required.");
    }

    if (receiverId === uid) {
      throw new HttpsError("invalid-argument", "receiverId cannot be the same as sender.");
    }

    const conversationId = buildConversationId(uid, receiverId);
    const chatDoc: ChatDoc = {
      conversationId,
      senderId: uid,
      receiverId,
      messageText,
      imageUrl,
      timestamp: FieldValue.serverTimestamp()
    };

    const docRef = await db.collection("chats").add(chatDoc);

    return {
      messageId: docRef.id,
      conversationId
    };
  }
);

export const listConversationSecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const payload = asObject(request.data);
    const otherUserId = toRequiredString(payload.otherUserId, "otherUserId", 128);
    const limit = Math.min(toPositiveInt(payload.limit, "limit", 100), 200);
    const conversationId = buildConversationId(uid, otherUserId);

    const snapshot = await db
      .collection("chats")
      .where("conversationId", "==", conversationId)
      .orderBy("timestamp", "asc")
      .limit(limit)
      .get();

    const messages = snapshot.docs.map((doc) => {
      const data = doc.data();
      return {
        id: doc.id,
        senderId: data.senderId ?? "",
        receiverId: data.receiverId ?? "",
        messageText: data.messageText ?? "",
        imageUrl: data.imageUrl ?? null,
        timestamp: toMillis(data.timestamp)
      };
    });

    return { conversationId, messages };
  }
);

export const deleteConversationSecure = onCall(
  {
    region: REGION,
    enforceAppCheck: true
  },
  async (request) => {
    const { uid } = requireAuth(request);
    requireAppCheck(request);

    const payload = asObject(request.data);
    const otherUserId = toRequiredString(payload.otherUserId, "otherUserId", 128);
    const conversationId = buildConversationId(uid, otherUserId);

    const snapshot = await db
      .collection("chats")
      .where("conversationId", "==", conversationId)
      .get();

    const batch = db.batch();
    let deletedCount = 0;
    snapshot.docs.forEach((doc) => {
      const senderId = doc.get("senderId");
      const receiverId = doc.get("receiverId");
      const isConversationParticipant =
        (senderId === uid && receiverId === otherUserId) ||
        (senderId === otherUserId && receiverId === uid);

      if (isConversationParticipant) {
        batch.delete(doc.ref);
        deletedCount += 1;
      }
    });

    if (deletedCount > 0) {
      await batch.commit();
    }

    return {
      conversationId,
      deletedCount
    };
  }
);
