package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.soulmate.backend.dto.chat.*;
import com.soulmate.backend.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ChatServiceTest {

    private Firestore firestore;
    private OneSignalPushNotificationService pushNotificationService;
    private CollectionReference chatsCollection;
    private CollectionReference usersCollection;
    private DocumentReference messageDocRef;
    private DocumentReference userDocRef;
    private ChatService chatService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        firestore = Mockito.mock(Firestore.class);
        pushNotificationService = Mockito.mock(OneSignalPushNotificationService.class);
        chatsCollection = Mockito.mock(CollectionReference.class);
        usersCollection = Mockito.mock(CollectionReference.class);
        messageDocRef = Mockito.mock(DocumentReference.class);
        userDocRef = Mockito.mock(DocumentReference.class);

        Mockito.when(firestore.collection("chats")).thenReturn(chatsCollection);
        Mockito.when(firestore.collection("users")).thenReturn(usersCollection);
        Mockito.when(chatsCollection.document(any(String.class))).thenReturn(messageDocRef);
        Mockito.when(usersCollection.document(any(String.class))).thenReturn(userDocRef);

        chatService = new ChatService(firestore, pushNotificationService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_Success() throws Exception {
        SendChatMessageRequest request = new SendChatMessageRequest("receiver-456", "Hello text", null, null);

        // Mock block validation (not blocked)
        ApiFuture<DocumentSnapshot> userFuture = Mockito.mock(ApiFuture.class);
        DocumentSnapshot userSnapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(userDocRef.get()).thenReturn(userFuture);
        Mockito.when(userFuture.get()).thenReturn(userSnapshot);
        Mockito.when(userSnapshot.exists()).thenReturn(true);
        Mockito.when(userSnapshot.get("blockedUsers")).thenReturn(Collections.emptyList());

        // Mock add message
        ApiFuture<DocumentReference> addFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(chatsCollection.add(any(Map.class))).thenReturn(addFuture);
        DocumentReference addedDocRef = Mockito.mock(DocumentReference.class);
        Mockito.when(addFuture.get()).thenReturn(addedDocRef);
        Mockito.when(addedDocRef.getId()).thenReturn("msg-789");

        SendChatMessageResponse res = chatService.sendMessage("sender-123", request);

        assertEquals("msg-789", res.messageId());
        Mockito.verify(pushNotificationService).sendChatMessageNotification("sender-123", "receiver-456", "Hello text", null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_BlockedThrowsForbidden() throws Exception {
        SendChatMessageRequest request = new SendChatMessageRequest("receiver-456", "Hello text", null, null);

        // Mock block validation (blocked)
        ApiFuture<DocumentSnapshot> userFuture = Mockito.mock(ApiFuture.class);
        DocumentSnapshot userSnapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(userDocRef.get()).thenReturn(userFuture);
        Mockito.when(userFuture.get()).thenReturn(userSnapshot);
        Mockito.when(userSnapshot.exists()).thenReturn(true);
        Mockito.when(userSnapshot.get("blockedUsers")).thenReturn(List.of("sender-123"));

        ApiException ex = assertThrows(ApiException.class, () -> chatService.sendMessage("sender-123", request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Bạn đã bị chặn.", ex.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_WithReplyContext() throws Exception {
        SendChatMessageRequest request = new SendChatMessageRequest("receiver-456", "Reply text", null, "orig-msg-123");

        // Mock block validation
        ApiFuture<DocumentSnapshot> userFuture = Mockito.mock(ApiFuture.class);
        DocumentSnapshot userSnapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(userDocRef.get()).thenReturn(userFuture);
        Mockito.when(userFuture.get()).thenReturn(userSnapshot);
        Mockito.when(userSnapshot.exists()).thenReturn(true);
        Mockito.when(userSnapshot.get("blockedUsers")).thenReturn(Collections.emptyList());

        // Mock fetch original message
        DocumentReference origDocRef = Mockito.mock(DocumentReference.class);
        Mockito.when(chatsCollection.document("orig-msg-123")).thenReturn(origDocRef);
        ApiFuture<DocumentSnapshot> origFuture = Mockito.mock(ApiFuture.class);
        DocumentSnapshot origSnapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(origDocRef.get()).thenReturn(origFuture);
        Mockito.when(origFuture.get()).thenReturn(origSnapshot);
        Mockito.when(origSnapshot.exists()).thenReturn(true);
        Mockito.when(origSnapshot.getString("senderId")).thenReturn("receiver-456");
        Mockito.when(origSnapshot.getString("messageText")).thenReturn("Original long message payload text...");

        // Mock add message
        ApiFuture<DocumentReference> addFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(chatsCollection.add(any(Map.class))).thenReturn(addFuture);
        DocumentReference addedDocRef = Mockito.mock(DocumentReference.class);
        Mockito.when(addFuture.get()).thenReturn(addedDocRef);
        Mockito.when(addedDocRef.getId()).thenReturn("msg-789");

        chatService.sendMessage("sender-123", request);

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(chatsCollection).add(mapCaptor.capture());
        Map<String, Object> payload = mapCaptor.getValue();

        assertEquals("orig-msg-123", payload.get("replyToMessageId"));
        assertEquals("Original long message payload text...", payload.get("replyToMessageText"));
        assertEquals("receiver-456", payload.get("replyToSenderId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reactToMessage_ToggleReaction() throws Exception {
        ApiFuture<DocumentSnapshot> msgFuture = Mockito.mock(ApiFuture.class);
        DocumentSnapshot msgSnapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(messageDocRef.get()).thenReturn(msgFuture);
        Mockito.when(msgFuture.get()).thenReturn(msgSnapshot);
        Mockito.when(msgSnapshot.exists()).thenReturn(true);

        // Set initial empty reactions
        Mockito.when(msgSnapshot.getData()).thenReturn(new HashMap<>());

        ApiFuture<WriteResult> writeFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(messageDocRef.update(eq("reactions"), any(Map.class))).thenReturn(writeFuture);

        chatService.reactToMessage("user-123", "msg-789", "❤️");

        ArgumentCaptor<Map<String, List<String>>> cap = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(messageDocRef).update(eq("reactions"), cap.capture());
        Map<String, List<String>> newReactions = cap.getValue();

        assertTrue(newReactions.containsKey("❤️"));
        assertTrue(newReactions.get("❤️").contains("user-123"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void editMessage_Success() throws Exception {
        ApiFuture<DocumentSnapshot> msgFuture = Mockito.mock(ApiFuture.class);
        DocumentSnapshot msgSnapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(messageDocRef.get()).thenReturn(msgFuture);
        Mockito.when(msgFuture.get()).thenReturn(msgSnapshot);
        Mockito.when(msgSnapshot.exists()).thenReturn(true);
        Mockito.when(msgSnapshot.getString("senderId")).thenReturn("sender-123");

        ApiFuture<WriteResult> writeFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(messageDocRef.update(any(Map.class))).thenReturn(writeFuture);

        chatService.editMessage("sender-123", "msg-789", "Updated content");

        Mockito.verify(messageDocRef).update(any(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void editMessage_ForbiddenOnOthersMessage() throws Exception {
        ApiFuture<DocumentSnapshot> msgFuture = Mockito.mock(ApiFuture.class);
        DocumentSnapshot msgSnapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(messageDocRef.get()).thenReturn(msgFuture);
        Mockito.when(msgFuture.get()).thenReturn(msgSnapshot);
        Mockito.when(msgSnapshot.exists()).thenReturn(true);
        Mockito.when(msgSnapshot.getString("senderId")).thenReturn("other-user");

        ApiException ex = assertThrows(ApiException.class, () ->
            chatService.editMessage("sender-123", "msg-789", "Updated content")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listConversationMedia_Success() throws Exception {
        Query query = Mockito.mock(Query.class);
        Mockito.when(chatsCollection.whereEqualTo(eq("conversationId"), any(String.class))).thenReturn(query);
        ApiFuture<QuerySnapshot> qFuture = Mockito.mock(ApiFuture.class);
        Mockito.when(query.get()).thenReturn(qFuture);
        QuerySnapshot qSnapshot = Mockito.mock(QuerySnapshot.class);
        Mockito.when(qFuture.get()).thenReturn(qSnapshot);

        QueryDocumentSnapshot doc1 = Mockito.mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot doc2 = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(qSnapshot.getDocuments()).thenReturn(List.of(doc1, doc2));

        Mockito.when(doc1.getString("imageUrl")).thenReturn("http://img1");
        Mockito.when(doc2.getString("imageUrl")).thenReturn(null);

        List<String> media = chatService.listConversationMedia("user-123", "receiver-456");
        assertEquals(1, media.size());
        assertEquals("http://img1", media.get(0));
    }
}
