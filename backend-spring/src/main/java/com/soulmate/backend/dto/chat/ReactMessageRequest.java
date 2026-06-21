package com.soulmate.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactMessageRequest(
    @NotBlank(message = "emoji is required")
    @Size(max = 10, message = "emoji max length is 10")
    String emoji
) {
}
