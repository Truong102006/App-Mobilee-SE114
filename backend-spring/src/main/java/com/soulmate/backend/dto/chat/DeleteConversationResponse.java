package com.soulmate.backend.dto.chat;

public record DeleteConversationResponse(
    String conversationId,
    int deletedCount
) {
}
