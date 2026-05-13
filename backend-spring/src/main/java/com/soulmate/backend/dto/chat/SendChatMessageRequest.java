package com.soulmate.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(
    @NotBlank(message = "receiverId is required")
    @Size(max = 128, message = "receiverId max length is 128")
    String receiverId,
    @Size(max = 4000, message = "messageText max length is 4000")
    String messageText,
    @Size(max = 2048, message = "imageUrl max length is 2048")
    String imageUrl
) {
}
