package com.soulmate.backend.dto.chat;

public record SendChatMessageResponse(
    String messageId,
    String conversationId
) {
}
