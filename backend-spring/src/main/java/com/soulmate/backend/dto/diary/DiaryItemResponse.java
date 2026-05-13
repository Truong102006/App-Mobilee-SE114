package com.soulmate.backend.dto.diary;

import java.util.List;

public record DiaryItemResponse(
    String diaryId,
    String userId,
    String title,
    String text,
    String moodTag,
    List<String> imageUrls,
    String audioUrl,
    Long createdAt,
    Long updatedAt
) {
}
