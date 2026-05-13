package com.soulmate.backend.dto.chat;

public record ChatMessageItemResponse(
    String id,
    String senderId,
    String receiverId,
    String messageText,
    String imageUrl,
    Long timestamp
) {
}
