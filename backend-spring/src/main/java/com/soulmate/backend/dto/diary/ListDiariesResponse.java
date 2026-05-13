package com.soulmate.backend.dto.diary;

import java.util.List;

public record ListDiariesResponse(
    List<DiaryItemResponse> diaries
) {
}
