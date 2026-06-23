package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.soulmate.backend.config.BackendProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class OneSignalPushNotificationServiceTest {

    private Firestore firestore;
    private CollectionReference usersCollection;
    private DocumentReference senderDocRef;
    private DocumentReference recipientDocRef;
    private BackendProperties backendProperties;
    private BackendProperties.OneSignal oneSignalProperties;
    private RestClient.Builder restClientBuilder;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    private OneSignalPushNotificationService notificationService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        firestore = Mockito.mock(Firestore.class);
        usersCollection = Mockito.mock(CollectionReference.class);
        senderDocRef = Mockito.mock(DocumentReference.class);
        recipientDocRef = Mockito.mock(DocumentReference.class);
        backendProperties = Mockito.mock(BackendProperties.class);
        oneSignalProperties = Mockito.mock(BackendProperties.OneSignal.class);
        restClientBuilder = Mockito.mock(RestClient.Builder.class);
        restClient = Mockito.mock(RestClient.class);
        requestBodyUriSpec = Mockito.mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = Mockito.mock(RestClient.RequestBodySpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);

        // Mock OneSignal Config
        Mockito.when(oneSignalProperties.isEnabled()).thenReturn(true);
        Mockito.when(oneSignalProperties.getAppId()).thenReturn("app-123");
        Mockito.when(oneSignalProperties.getApiKey()).thenReturn("key-456");
        Mockito.when(oneSignalProperties.getApiUrl()).thenReturn("https://api.onesignal.com");
        Mockito.when(backendProperties.getOneSignal()).thenReturn(oneSignalProperties);

        // Mock RestClient Builder fluent calls
        Mockito.when(restClientBuilder.baseUrl(any(String.class))).thenReturn(restClientBuilder);
        Mockito.when(restClientBuilder.build()).thenReturn(restClient);

        // Mock RestClient fluent post requests
        Mockito.when(restClient.post()).thenReturn(requestBodyUriSpec);
        Mockito.when(requestBodyUriSpec.uri(eq("/notifications"))).thenReturn(requestBodySpec);
        Mockito.when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), any(String.class))).thenReturn(requestBodySpec);
        Mockito.when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        Mockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        // Mock Firestore calls for users
        Mockito.when(firestore.collection("users")).thenReturn(usersCollection);
        Mockito.when(usersCollection.document("sender-uid")).thenReturn(senderDocRef);
        Mockito.when(usersCollection.document("recipient-uid")).thenReturn(recipientDocRef);

        notificationService = new OneSignalPushNotificationService(firestore, backendProperties, restClientBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendChatMessageNotification_Success() throws Exception {
        // Arrange
        mockUserProfileFetch(senderDocRef, "Sender Name", "http://sender-avatar", true);
        mockUserProfileFetch(recipientDocRef, "Recipient Name", "http://recipient-avatar", true);

        // Act
        notificationService.sendChatMessageNotification("sender-uid", "recipient-uid", "Hello, how are you?", "http://chat-image");

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(requestBodySpec).body(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();

        assertEquals("app-123", payload.get("app_id"));
        assertEquals("push", payload.get("target_channel"));
        assertEquals(Map.of("en", "New message"), payload.get("headings"));
        assertEquals(Map.of("en", "Sender Name: Hello, how are you?"), payload.get("contents"));

        Map<String, String> data = (Map<String, String>) payload.get("data");
        assertEquals("chat_message", data.get("type"));
        assertEquals("chat", data.get("screen"));
        assertEquals("sender-uid", data.get("userId"));
        assertEquals("Sender Name", data.get("userName"));
        assertEquals("http://sender-avatar", data.get("avatarUrl"));
        assertEquals("http://chat-image", data.get("imageUrl"));

        Map<String, List<String>> aliases = (Map<String, List<String>>) payload.get("include_aliases");
        assertEquals(List.of("recipient-uid"), aliases.get("external_id"));

        Mockito.verify(requestBodySpec).header(HttpHeaders.AUTHORIZATION, "Key key-456");
    }

    @Test
    void sendChatMessageNotification_DoesNotSendToSelf() {
        // Act & Assert
        notificationService.sendChatMessageNotification("sender-uid", "sender-uid", "Hello", null);
        Mockito.verifyNoInteractions(restClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendChatMessageNotification_DisabledRecipient_DoesNotSend() throws Exception {
        // Arrange
        mockUserProfileFetch(recipientDocRef, "Recipient Name", null, false); // notifications disabled

        // Act
        notificationService.sendChatMessageNotification("sender-uid", "recipient-uid", "Hello", null);

        // Assert
        Mockito.verifyNoInteractions(restClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendChatMessageNotification_OneSignalDisabled_DoesNotSend() throws Exception {
        // Arrange
        Mockito.when(oneSignalProperties.isEnabled()).thenReturn(false);
        mockUserProfileFetch(recipientDocRef, "Recipient Name", null, true);

        // Act
        notificationService.sendChatMessageNotification("sender-uid", "recipient-uid", "Hello", null);

        // Assert
        Mockito.verifyNoInteractions(restClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendChatMessageNotification_AbbreviatesLongMessage() throws Exception {
        // Arrange
        mockUserProfileFetch(senderDocRef, "Sender", null, true);
        mockUserProfileFetch(recipientDocRef, "Recipient", null, true);

        String longMessage = "a".repeat(150);

        // Act
        notificationService.sendChatMessageNotification("sender-uid", "recipient-uid", longMessage, null);

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(requestBodySpec).body(payloadCaptor.capture());
        Map<String, Object> contents = (Map<String, Object>) payloadCaptor.getValue().get("contents");

        String expectedPreview = "Sender: " + "a".repeat(117) + "...";
        assertEquals(expectedPreview, contents.get("en"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendPostLikeNotification_Success() throws Exception {
        // Arrange
        mockUserProfileFetch(senderDocRef, "Actor", null, true);
        mockUserProfileFetch(recipientDocRef, "Recipient", null, true);

        // Act
        notificationService.sendPostLikeNotification("sender-uid", "recipient-uid", "post-789");

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(requestBodySpec).body(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();

        assertEquals(Map.of("en", "New like"), payload.get("headings"));
        assertEquals(Map.of("en", "Actor liked your post."), payload.get("contents"));

        Map<String, String> data = (Map<String, String>) payload.get("data");
        assertEquals("post_like", data.get("type"));
        assertEquals("community", data.get("screen"));
        assertEquals("post-789", data.get("postId"));
        assertEquals("sender-uid", data.get("actorUserId"));
        assertEquals("Actor", data.get("actorName"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendPostCommentNotification_Success() throws Exception {
        // Arrange
        mockUserProfileFetch(senderDocRef, "Actor", null, true);
        mockUserProfileFetch(recipientDocRef, "Recipient", null, true);

        // Act
        notificationService.sendPostCommentNotification("sender-uid", "recipient-uid", "post-789", "Very nice post!");

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(requestBodySpec).body(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();

        assertEquals(Map.of("en", "New comment"), payload.get("headings"));
        assertEquals(Map.of("en", "Actor commented on your post: Very nice post!"), payload.get("contents"));

        Map<String, String> data = (Map<String, String>) payload.get("data");
        assertEquals("post_comment", data.get("type"));
        assertEquals("community", data.get("screen"));
        assertEquals("post-789", data.get("postId"));
        assertEquals("sender-uid", data.get("actorUserId"));
        assertEquals("Actor", data.get("actorName"));
    }

    @SuppressWarnings("unchecked")
    private void mockUserProfileFetch(DocumentReference docRef, String anonymousName, String avatarUrl, boolean notificationEnabled) throws Exception {
        ApiFuture<DocumentSnapshot> future = Mockito.mock(ApiFuture.class);
        DocumentSnapshot snapshot = Mockito.mock(DocumentSnapshot.class);

        Mockito.when(docRef.get()).thenReturn(future);
        Mockito.when(future.get()).thenReturn(snapshot);
        Mockito.when(snapshot.exists()).thenReturn(true);
        Mockito.when(snapshot.getString("anonymousName")).thenReturn(anonymousName);
        Mockito.when(snapshot.getString("avatarUrl")).thenReturn(avatarUrl);
        Mockito.when(snapshot.getBoolean("notificationEnabled")).thenReturn(notificationEnabled);
    }
}
