package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.soulmate.backend.dto.diary.*;
import com.soulmate.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class DiaryService {

    private final Firestore firestore;

    public DiaryService(Firestore firestore) {
        this.firestore = firestore;
    }

    public SaveDiaryResponse saveDiary(String uid, SaveDiaryRequest request) {
        String inputDiaryId = trimToNull(request.diaryId());
        DocumentReference docRef = inputDiaryId == null
            ? firestore.collection("diaries").document()
            : firestore.collection("diaries").document(inputDiaryId);

        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (snapshot.exists() && !Objects.equals(snapshot.getString("user_id"), uid)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You cannot edit this diary entry.");
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

            if (snapshot.exists()) {
                Object existingTimestamp = snapshot.get("timestamp");
                diaryData.put("timestamp", existingTimestamp != null ? existingTimestamp : FieldValue.serverTimestamp());
            } else {
                diaryData.put("timestamp", FieldValue.serverTimestamp());
            }

            docRef.set(diaryData, SetOptions.merge()).get();
            return new SaveDiaryResponse(docRef.getId(), now);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save diary.");
        }
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
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list diaries.");
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
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete diary.");
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
