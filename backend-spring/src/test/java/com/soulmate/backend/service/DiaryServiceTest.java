package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.soulmate.backend.dto.diary.ListDiariesResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

class DiaryServiceTest {

    @Test
    void listMyDiaries_sortsNewestFirstWithoutNeedingFirestoreOrderBy() throws Exception {
        Firestore firestore = Mockito.mock(Firestore.class);
        CollectionReference collection = Mockito.mock(CollectionReference.class);
        Query filteredQuery = Mockito.mock(Query.class);
        @SuppressWarnings("unchecked")
        ApiFuture<QuerySnapshot> future = Mockito.mock(ApiFuture.class);
        QuerySnapshot snapshot = Mockito.mock(QuerySnapshot.class);
        QueryDocumentSnapshot olderDoc = Mockito.mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot newerDoc = Mockito.mock(QueryDocumentSnapshot.class);

        whenDiaryQuery(firestore, collection, filteredQuery, future, snapshot);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(olderDoc, newerDoc));

        long olderMillis = 1_700_000_000_000L;
        long newerMillis = 1_800_000_000_000L;

        mockDiaryDoc(
            olderDoc,
            "older-diary",
            "uid-1",
            "Older",
            "Older content",
            "Neutral",
            olderMillis,
            olderMillis
        );
        mockDiaryDoc(
            newerDoc,
            "newer-diary",
            "uid-1",
            "Newer",
            "Newer content",
            "Happy",
            newerMillis,
            newerMillis
        );

        DiaryService service = new DiaryService(firestore);

        ListDiariesResponse response = service.listMyDiaries("uid-1");

        assertEquals(2, response.diaries().size());
        assertEquals("newer-diary", response.diaries().get(0).diaryId());
        assertEquals("older-diary", response.diaries().get(1).diaryId());
        Mockito.verify(collection).whereEqualTo("user_id", "uid-1");
        Mockito.verify(filteredQuery, Mockito.never()).orderBy(eq("timestamp"), eq(Query.Direction.DESCENDING));
    }

    @Test
    void listMyDiaries_fallsBackToUpdatedAtWhenTimestampIsMissing() throws Exception {
        Firestore firestore = Mockito.mock(Firestore.class);
        CollectionReference collection = Mockito.mock(CollectionReference.class);
        Query filteredQuery = Mockito.mock(Query.class);
        @SuppressWarnings("unchecked")
        ApiFuture<QuerySnapshot> future = Mockito.mock(ApiFuture.class);
        QuerySnapshot snapshot = Mockito.mock(QuerySnapshot.class);
        QueryDocumentSnapshot missingTimestampDoc = Mockito.mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot withTimestampDoc = Mockito.mock(QueryDocumentSnapshot.class);

        whenDiaryQuery(firestore, collection, filteredQuery, future, snapshot);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(missingTimestampDoc, withTimestampDoc));

        long olderMillis = 1_700_000_000_000L;
        long fallbackUpdatedMillis = 1_900_000_000_000L;

        mockDiaryDoc(
            missingTimestampDoc,
            "updated-only-diary",
            "uid-1",
            "Updated only",
            "Updated content",
            "Calm",
            null,
            fallbackUpdatedMillis
        );
        mockDiaryDoc(
            withTimestampDoc,
            "timestamp-diary",
            "uid-1",
            "Timestamp",
            "Timestamp content",
            "Happy",
            olderMillis,
            olderMillis
        );

        DiaryService service = new DiaryService(firestore);

        ListDiariesResponse response = service.listMyDiaries("uid-1");

        assertEquals("updated-only-diary", response.diaries().get(0).diaryId());
        assertEquals("timestamp-diary", response.diaries().get(1).diaryId());
    }

    private void whenDiaryQuery(
        Firestore firestore,
        CollectionReference collection,
        Query filteredQuery,
        ApiFuture<QuerySnapshot> future,
        QuerySnapshot snapshot
    ) throws Exception {
        Mockito.when(firestore.collection("diaries")).thenReturn(collection);
        Mockito.when(collection.whereEqualTo("user_id", "uid-1")).thenReturn(filteredQuery);
        Mockito.when(filteredQuery.get()).thenReturn(future);
        Mockito.when(future.get()).thenReturn(snapshot);
    }

    private void mockDiaryDoc(
        QueryDocumentSnapshot doc,
        String diaryId,
        String userId,
        String title,
        String text,
        String moodTag,
        Long timestampMillis,
        Long updatedAt
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("title", title);
        data.put("text", text);
        data.put("mood_tag", moodTag);
        data.put("image_urls", List.of());
        data.put("updated_at", updatedAt);
        if (timestampMillis != null) {
            data.put("timestamp", Timestamp.of(new Date(timestampMillis)));
        }

        Mockito.when(doc.getId()).thenReturn(diaryId);
        Mockito.when(doc.getData()).thenReturn(data);
    }
}
