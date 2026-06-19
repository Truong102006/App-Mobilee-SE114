package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.soulmate.backend.dto.diary.*;
import com.soulmate.backend.exception.ApiException;
import com.soulmate.backend.exception.FirestoreApiExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class DiaryService {

    private static final Logger log = LoggerFactory.getLogger(DiaryService.class);

    private final Firestore firestore;

    public DiaryService(Firestore firestore) {
        this.firestore = firestore;
    }

    public SaveDiaryResponse saveDiary(String uid, SaveDiaryRequest request) {
        String inputDiaryId = trimToNull(request.diaryId());
        boolean isNewEntry = (inputDiaryId == null);
        DocumentReference docRef = isNewEntry
            ? firestore.collection("diaries").document()
            : firestore.collection("diaries").document(inputDiaryId);

        int maxRetries = 2;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // Only read existing doc for UPDATES (ownership check).
                // For NEW entries, skip the read entirely to save quota.
                if (!isNewEntry) {
                    DocumentSnapshot snapshot = docRef.get().get();
                    if (snapshot.exists() && !Objects.equals(snapshot.getString("user_id"), uid)) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot edit this diary entry.");
                    }
                }

                long now = System.currentTimeMillis();
                Map<String, Object> diaryData = new HashMap<>();
                diaryData.put("diary_id", docRef.getId());
                diaryData.put("user_id", uid);
                diaryData.put("title", trimToEmpty(request.title()));
                diaryData.put("text", request.text().trim());
                diaryData.put("mood_tag", StringUtils.hasText(request.moodTag()) ? request.moodTag().trim() : "Neutral");
                diaryData.put("image_urls", normalizeStringList(request.imageUrls()));
                diaryData.put("audio_url", trimToNull(request.audioUrl()));
                diaryData.put("updated_at", now);
                diaryData.put("timestamp", FieldValue.serverTimestamp());

                docRef.set(diaryData, SetOptions.merge()).get();
                return new SaveDiaryResponse(docRef.getId(), now);
            } catch (ExecutionException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw FirestoreApiExceptionMapper.map(e, "Failed to save diary.", "Firestore quota exceeded. Please try again later.");
                }
                if (attempt < maxRetries && isRetryable(e)) {
                    log.warn("Retryable Firestore error on save diary attempt={}, retrying...", attempt, e);
                    try { Thread.sleep(500L * (attempt + 1)); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw FirestoreApiExceptionMapper.map(ie, "Failed to save diary.", "Firestore quota exceeded. Please try again later.");
                    }
                    continue;
                }
                log.error("Failed to save diary for uid={} diaryId={}", uid, docRef.getId(), e);
                throw FirestoreApiExceptionMapper.map(e, "Failed to save diary.", "Firestore quota exceeded. Please try again later.");
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save diary after retries.");
    }

    private boolean isRetryable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof com.google.api.gax.rpc.ResourceExhaustedException
                || current instanceof com.google.api.gax.rpc.UnavailableException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public ListDiariesResponse listMyDiaries(String uid) {
        Query query = firestore.collection("diaries")
            .whereEqualTo("user_id", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING);

        try {
            QuerySnapshot snapshot = query.get().get();
            List<DiaryItemResponse> diaries = snapshot.getDocuments().stream()
                .map(this::toDiaryItem)
                .toList();
            return new ListDiariesResponse(diaries);
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to list diaries for uid={}", uid, e);
            throw FirestoreApiExceptionMapper.map(
                e,
                "Failed to list diaries.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    public DeleteDiaryResponse deleteDiary(String uid, String diaryId) {
        if (!StringUtils.hasText(diaryId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "diaryId is required.");
        }

        DocumentReference docRef = firestore.collection("diaries").document(diaryId.trim());
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Diary entry not found.");
            }
            if (!Objects.equals(snapshot.getString("user_id"), uid)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You cannot delete this diary entry.");
            }
            docRef.delete().get();
            return new DeleteDiaryResponse(true, diaryId.trim());
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to delete diary for uid={} diaryId={}", uid, diaryId, e);
            throw FirestoreApiExceptionMapper.map(
                e,
                "Failed to delete diary.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private DiaryItemResponse toDiaryItem(QueryDocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        return new DiaryItemResponse(
            doc.getId(),
            asString(data.get("user_id"), ""),
            asString(data.get("title"), ""),
            asString(data.get("text"), ""),
            asString(data.get("mood_tag"), "Neutral"),
            normalizeStringList(data.get("image_urls")),
            asNullableString(data.get("audio_url")),
            toMillis(data.get("timestamp")),
            asLong(data.get("updated_at"))
        );
    }

    private Long toMillis(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toDate().getTime();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> normalizeStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> rawList) {
            List<String> result = new ArrayList<>();
            for (Object entry : rawList) {
                if (entry instanceof String item && StringUtils.hasText(item)) {
                    result.add(item.trim());
                }
            }
            return result;
        }
        return List.of();
    }

    private String asString(Object value, String fallback) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        return fallback;
    }

    private String asNullableString(Object value) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }
}
