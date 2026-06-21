package com.soulmate.backend.dto.chat;

import java.util.List;
import java.util.Map;

public record ChatMessageItemResponse(
    String id,
    String senderId,
    String receiverId,
    String messageText,
    String imageUrl,
    Long timestamp,
    String replyToMessageId,
    String replyToMessageText,
    String replyToSenderId,
    Map<String, List<String>> reactions,
    Boolean isEdited,
    Long editedAt
) {
}
