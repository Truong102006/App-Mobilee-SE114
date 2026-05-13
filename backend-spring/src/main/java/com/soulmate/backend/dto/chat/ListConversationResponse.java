package com.soulmate.backend.dto.chat;

import java.util.List;

public record ListConversationResponse(
    String conversationId,
    List<ChatMessageItemResponse> messages
) {
}
