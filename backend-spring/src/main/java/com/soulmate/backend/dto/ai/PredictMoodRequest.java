package com.soulmate.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PredictMoodRequest(
    @NotBlank(message = "text is required")
    @Size(max = 12000, message = "text max length is 12000")
    String text
) {
}
