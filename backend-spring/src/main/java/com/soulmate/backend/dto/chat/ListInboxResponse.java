package com.soulmate.backend.dto.chat;

import java.util.List;

public record ListInboxResponse(
    List<ChatMessageItemResponse> messages
) {
}
