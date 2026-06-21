package com.soulmate.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
    @NotBlank(message = "avatarUrl is required")
    @Size(max = 2048, message = "avatarUrl max length is 2048")
    String avatarUrl
) {
}
