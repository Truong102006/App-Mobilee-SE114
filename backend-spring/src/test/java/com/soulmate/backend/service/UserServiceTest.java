package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.soulmate.backend.dto.user.SocialLink;
import com.soulmate.backend.dto.user.UpdateProfileRequest;
import com.soulmate.backend.dto.user.UserProfileResponse;
import com.soulmate.backend.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class UserServiceTest {

    private Firestore firestore;
    private CollectionReference usersCollection;
    private DocumentReference docRef;
    private ApiFuture<DocumentSnapshot> future;
    private DocumentSnapshot snapshot;
    private UserService userService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        firestore = Mockito.mock(Firestore.class);
        usersCollection = Mockito.mock(CollectionReference.class);
        docRef = Mockito.mock(DocumentReference.class);
        future = Mockito.mock(ApiFuture.class);
        snapshot = Mockito.mock(DocumentSnapshot.class);

        Mockito.when(firestore.collection("users")).thenReturn(usersCollection);
        Mockito.when(usersCollection.document(any(String.class))).thenReturn(docRef);
        Mockito.when(docRef.get()).thenReturn(future);
        try {
            Mockito.when(future.get()).thenReturn(snapshot);
        } catch (Exception e) {
            fail(e);
        }

        userService = new UserService(firestore);
    }

    @Test
    void getUserProfile_Success() {
        Mockito.when(snapshot.exists()).thenReturn(true);
        Mockito.when(snapshot.getId()).thenReturn("user-123");
        Mockito.when(snapshot.getString("anonymousName")).thenReturn("Bob");
        Mockito.when(snapshot.getString("avatarUrl")).thenReturn("http://avatar");
        Mockito.when(snapshot.getString("bio")).thenReturn("Testing bio");
        Mockito.when(snapshot.get("socialLinks")).thenReturn(List.of(
            Map.of("platform", "instagram", "url", "http://instagram.com/bob")
        ));
        Mockito.when(snapshot.get("createdAt")).thenReturn(1700000000L);

        UserProfileResponse res = userService.getUserProfile("user-123");
        assertEquals("user-123", res.userId());
        assertEquals("Bob", res.anonymousName());
        assertEquals("http://avatar", res.avatarUrl());
        assertEquals("Testing bio", res.bio());
        assertEquals(1, res.socialLinks().size());
        assertEquals("instagram", res.socialLinks().get(0).platform());
    }

    @Test
    void getUserProfile_NotFound() {
        Mockito.when(snapshot.exists()).thenReturn(false);
        ApiException exception = assertThrows(ApiException.class, () -> userService.getUserProfile("user-123"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "New Name",
            "New Bio",
            List.of(new SocialLink("facebook", "http://fb.com"))
        );
        ApiFuture<WriteResult> writeFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(docRef.update(any(Map.class))).thenReturn(writeFuture);

        userService.updateProfile("uid-123", request);
        Mockito.verify(docRef).update(any(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateAvatar_Success() throws Exception {
        ApiFuture<WriteResult> writeFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(docRef.update(eq("avatarUrl"), eq("http://avatar"))).thenReturn(writeFuture);

        userService.updateAvatar("uid-123", "http://avatar");
        Mockito.verify(docRef).update(eq("avatarUrl"), eq("http://avatar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void blockUser_Success() throws Exception {
        ApiFuture<WriteResult> writeFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(docRef.update(eq("blockedUsers"), any(FieldValue.class))).thenReturn(writeFuture);

        userService.blockUser("uid-123", "target-456");
        Mockito.verify(docRef).update(eq("blockedUsers"), any(FieldValue.class));
    }

    @Test
    void blockUser_SelfBlockThrowsBadRequest() {
        ApiException ex = assertThrows(ApiException.class, () -> userService.blockUser("uid-123", "uid-123"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unblockUser_Success() throws Exception {
        ApiFuture<WriteResult> writeFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(docRef.update(eq("blockedUsers"), any(FieldValue.class))).thenReturn(writeFuture);

        userService.unblockUser("uid-123", "target-456");
        Mockito.verify(docRef).update(eq("blockedUsers"), any(FieldValue.class));
    }

    @Test
    void listBlockedUsers_Success() {
        Mockito.when(snapshot.exists()).thenReturn(true);
        Mockito.when(snapshot.get("blockedUsers")).thenReturn(List.of("user-A", "user-B"));

        List<String> blocked = userService.listBlockedUsers("uid-123");
        assertEquals(2, blocked.size());
        assertTrue(blocked.contains("user-A"));
        assertTrue(blocked.contains("user-B"));
    }
}
