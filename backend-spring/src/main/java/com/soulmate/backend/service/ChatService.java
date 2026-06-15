package com.soulmate.backend.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.soulmate.backend.dto.chat.*;
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
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final Firestore firestore;

    public ChatService(Firestore firestore) {
        this.firestore = firestore;
    }

    public SendChatMessageResponse sendMessage(String uid, SendChatMessageRequest request) {
        String receiverId = request.receiverId().trim();
        String messageText = trimToEmpty(request.messageText());
        String imageUrl = trimToNull(request.imageUrl());

        if (!StringUtils.hasText(messageText) && !StringUtils.hasText(imageUrl)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "messageText or imageUrl is required.");
        }

        if (uid.equals(receiverId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "receiverId cannot be the same as sender.");
        }

        String conversationId = buildConversationId(uid, receiverId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("senderId", uid);
        payload.put("receiverId", receiverId);
        payload.put("messageText", messageText);
        payload.put("imageUrl", imageUrl);
        payload.put("timestamp", FieldValue.serverTimestamp());

        try {
            DocumentReference created = firestore.collection("chats").add(payload).get();
            return new SendChatMessageResponse(created.getId(), conversationId);
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to send chat message for uid={} receiverId={}", uid, receiverId, e);
            throw FirestoreApiExceptionMapper.map(
                e,
                "Failed to send chat message.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    public ListConversationResponse listConversation(String uid, String otherUserId, int limit) {
        if (!StringUtils.hasText(otherUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "otherUserId is required.");
        }
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String conversationId = buildConversationId(uid, otherUserId.trim());

        // Avoid requiring a composite index on (conversationId, timestamp).
        Query query = firestore.collection("chats")
            .whereEqualTo("conversationId", conversationId);

        try {
            QuerySnapshot snapshot = query.get().get();
            List<ChatMessageItemResponse> messages = snapshot.getDocuments().stream()
                .map(this::toMessage)
                .sorted(Comparator.comparing(
                    ChatMessageItemResponse::timestamp,
                    Comparator.nullsLast(Long::compareTo)
                ))
                .limit(safeLimit)
                .toList();
            return new ListConversationResponse(conversationId, messages);
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to list conversation for uid={} otherUserId={}", uid, otherUserId, e);
            throw FirestoreApiExceptionMapper.map(
                e,
                "Failed to list conversation.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    public ListInboxResponse listInbox(String uid, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));

        Query sentQuery = firestore.collection("chats")
            .whereEqualTo("senderId", uid);
        Query receivedQuery = firestore.collection("chats")
            .whereEqualTo("receiverId", uid);

        try {
            QuerySnapshot sentSnapshot = sentQuery.get().get();
            QuerySnapshot receivedSnapshot = receivedQuery.get().get();

            Map<String, QueryDocumentSnapshot> docsById = new LinkedHashMap<>();
            for (QueryDocumentSnapshot doc : sentSnapshot.getDocuments()) {
                docsById.put(doc.getId(), doc);
            }
            for (QueryDocumentSnapshot doc : receivedSnapshot.getDocuments()) {
                docsById.put(doc.getId(), doc);
            }

            Map<String, ChatMessageItemResponse> latestByConversation = new HashMap<>();
            for (QueryDocumentSnapshot doc : docsById.values()) {
                ChatMessageItemResponse message = toMessage(doc);
                String conversationId = buildConversationId(
                    message.senderId(),
                    message.receiverId()
                );
                ChatMessageItemResponse existing = latestByConversation.get(conversationId);
                if (existing == null || isNewer(message, existing)) {
                    latestByConversation.put(conversationId, message);
                }
            }

            List<ChatMessageItemResponse> messages = latestByConversation.values().stream()
                .sorted((a, b) -> compareTimestampDesc(a.timestamp(), b.timestamp()))
                .limit(safeLimit)
                .toList();

            return new ListInboxResponse(messages);
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to list inbox for uid={}", uid, e);
            throw FirestoreApiExceptionMapper.map(
                e,
                "Failed to list inbox.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    public DeleteConversationResponse deleteConversation(String uid, String otherUserId) {
        if (!StringUtils.hasText(otherUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "otherUserId is required.");
        }
        String normalizedOtherUid = otherUserId.trim();
        String conversationId = buildConversationId(uid, normalizedOtherUid);

        Query query = firestore.collection("chats")
            .whereEqualTo("conversationId", conversationId);

        try {
            QuerySnapshot snapshot = query.get().get();
            WriteBatch batch = firestore.batch();
            int deletedCount = 0;

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                String senderId = doc.getString("senderId");
                String receiverId = doc.getString("receiverId");
                boolean isParticipant = (uid.equals(senderId) && normalizedOtherUid.equals(receiverId))
                    || (uid.equals(receiverId) && normalizedOtherUid.equals(senderId));
                if (isParticipant) {
                    batch.delete(doc.getReference());
                    deletedCount += 1;
                }
            }

            if (deletedCount > 0) {
                batch.commit().get();
            }

            return new DeleteConversationResponse(conversationId, deletedCount);
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to delete conversation for uid={} otherUserId={}", uid, otherUserId, e);
            throw FirestoreApiExceptionMapper.map(
                e,
                "Failed to delete conversation.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    public Map<String, Object> markAsRead(String uid, String otherUserId) {
        if (!StringUtils.hasText(otherUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "otherUserId is required.");
        }
        String conversationId = buildConversationId(uid, otherUserId.trim());

        // Mark all messages sent BY other user TO this user as read
        Query query = firestore.collection("chats")
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("senderId", otherUserId.trim())
            .whereEqualTo("receiverId", uid);

        try {
            QuerySnapshot snapshot = query.get().get();
            WriteBatch batch = firestore.batch();
            int markedCount = 0;

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                Boolean isRead = doc.getBoolean("isRead");
                if (isRead == null || !isRead) {
                    batch.update(doc.getReference(), "isRead", true);
                    markedCount++;
                }
            }

            if (markedCount > 0) {
                batch.commit().get();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("markedCount", markedCount);
            return result;
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to mark messages as read for uid={} otherUserId={}", uid, otherUserId, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to mark messages as read.", "Firestore quota exceeded.");
        }
    }

    public Map<String, Object> deleteMessage(String uid, String messageId) {
        if (!StringUtils.hasText(messageId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "messageId is required.");
        }

        DocumentReference docRef = firestore.collection("chats").document(messageId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Message not found.");
            }

            String senderId = snapshot.getString("senderId");
            if (!Objects.equals(senderId, uid)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own messages.");
            }

            docRef.delete().get();

            Map<String, Object> result = new HashMap<>();
            result.put("deleted", true);
            result.put("messageId", messageId);
            return result;
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to delete message={} for uid={}", messageId, uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to delete message.", "Firestore quota exceeded.");
        }
    }

    private ChatMessageItemResponse toMessage(QueryDocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        return new ChatMessageItemResponse(
            doc.getId(),
            asString(data.get("senderId")),
            asString(data.get("receiverId")),
            asString(data.get("messageText")),
            asNullableString(data.get("imageUrl")),
            toMillis(data.get("timestamp"))
        );
    }

    private String buildConversationId(String uidA, String uidB) {
        List<String> ids = new ArrayList<>(List.of(uidA, uidB));
        ids.sort(String::compareTo);
        return ids.get(0) + "__" + ids.get(1);
    }

    private Long toMillis(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toDate().getTime();
        }
        return null;
    }

    private String asString(Object value) {
        if (value instanceof String text) {
            return text;
        }
        return "";
    }

    private String asNullableString(Object value) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
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

    private boolean isNewer(ChatMessageItemResponse current, ChatMessageItemResponse existing) {
        Long currentTimestamp = current.timestamp();
        Long existingTimestamp = existing.timestamp();
        if (currentTimestamp == null) {
            return false;
        }
        if (existingTimestamp == null) {
            return true;
        }
        return currentTimestamp > existingTimestamp;
    }

    private int compareTimestampDesc(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Long.compare(right, left);
    }
}
