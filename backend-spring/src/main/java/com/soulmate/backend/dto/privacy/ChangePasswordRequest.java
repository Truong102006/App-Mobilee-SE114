package com.soulmate.backend.dto.privacy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    String email
) {
}
