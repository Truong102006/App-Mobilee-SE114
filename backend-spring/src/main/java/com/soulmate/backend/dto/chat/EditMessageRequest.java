package com.soulmate.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(
    @NotBlank(message = "newMessageText is required")
    @Size(max = 4000, message = "newMessageText max length is 4000")
    String newMessageText
) {
}
