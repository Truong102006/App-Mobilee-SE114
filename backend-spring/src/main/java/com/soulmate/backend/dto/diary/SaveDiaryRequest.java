package com.soulmate.backend.dto.diary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveDiaryRequest(
    String diaryId,
    @Size(max = 200, message = "title max length is 200")
    String title,
    @NotBlank(message = "text is required")
    @Size(max = 12000, message = "text max length is 12000")
    String text,
    @Size(max = 64, message = "moodTag max length is 64")
    String moodTag,
    List<String> imageUrls,
    @Size(max = 2048, message = "audioUrl max length is 2048")
    String audioUrl
) {
}
