package com.soulmate.backend.dto.diary;

public record SaveDiaryResponse(
    String diaryId,
    long updatedAt
) {
}
