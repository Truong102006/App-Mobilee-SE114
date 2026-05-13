package com.soulmate.backend.dto.ai;

public record PredictMoodResponse(
    String mood,
    String raw,
    String model
) {
}
