package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class PrivacyServiceTest {

    private Firestore firestore;
    private FirebaseAuth firebaseAuth;
    private PrivacyService privacyService;

    @BeforeEach
    void setUp() {
        firestore = Mockito.mock(Firestore.class);
        firebaseAuth = Mockito.mock(FirebaseAuth.class);
        privacyService = new PrivacyService(firestore, firebaseAuth);
    }

    @Test
    void sendPasswordResetEmail_Success() throws Exception {
        Mockito.when(firebaseAuth.generatePasswordResetLink("user@example.com"))
            .thenReturn("https://reset-link");

        privacyService.sendPasswordResetEmail("user@example.com");

        Mockito.verify(firebaseAuth).generatePasswordResetLink("user@example.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAccount_Success() throws Exception {
        WriteBatch batch = Mockito.mock(WriteBatch.class);
        Mockito.when(firestore.batch()).thenReturn(batch);

        DocumentReference userDoc = Mockito.mock(DocumentReference.class);
        CollectionReference usersCollection = Mockito.mock(CollectionReference.class);
        Mockito.when(firestore.collection("users")).thenReturn(usersCollection);
        Mockito.when(usersCollection.document("uid-123")).thenReturn(userDoc);

        // Mock diaries queries
        CollectionReference diariesCollection = Mockito.mock(CollectionReference.class);
        Mockito.when(firestore.collection("diaries")).thenReturn(diariesCollection);
        Query diariesQuery = Mockito.mock(Query.class);
        Mockito.when(diariesCollection.whereEqualTo("userId", "uid-123")).thenReturn(diariesQuery);
        ApiFuture<QuerySnapshot> diariesFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(diariesQuery.get()).thenReturn(diariesFuture);
        QuerySnapshot diariesSnapshot = Mockito.mock(QuerySnapshot.class);
        Mockito.when(diariesFuture.get()).thenReturn(diariesSnapshot);
        Mockito.when(diariesSnapshot.getDocuments()).thenReturn(Collections.emptyList());

        // Mock posts queries
        CollectionReference postsCollection = Mockito.mock(CollectionReference.class);
        Mockito.when(firestore.collection("community_posts")).thenReturn(postsCollection);
        Query postsQuery = Mockito.mock(Query.class);
        Mockito.when(postsCollection.whereEqualTo("userId", "uid-123")).thenReturn(postsQuery);
        ApiFuture<QuerySnapshot> postsFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(postsQuery.get()).thenReturn(postsFuture);
        QuerySnapshot postsSnapshot = Mockito.mock(QuerySnapshot.class);
        Mockito.when(postsFuture.get()).thenReturn(postsSnapshot);
        Mockito.when(postsSnapshot.getDocuments()).thenReturn(Collections.emptyList());

        ApiFuture<List<WriteResult>> commitFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(batch.commit()).thenReturn(commitFuture);
        Mockito.when(commitFuture.get()).thenReturn(Collections.emptyList());

        privacyService.deleteAccount("uid-123");

        Mockito.verify(batch).delete(userDoc);
        Mockito.verify(batch).commit();
        Mockito.verify(firebaseAuth).deleteUser("uid-123");
    }
}
