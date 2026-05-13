package com.soulmate.backend.dto.diary;

public record DeleteDiaryResponse(
    boolean deleted,
    String diaryId
) {
}
