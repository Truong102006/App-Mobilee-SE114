package com.soulmate.backend.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SavePostRequest(
    String mood,
    @NotBlank(message = "content is required")
    @Size(max = 5000, message = "content max length is 5000")
    String textContent,
    List<String> imageUrls
) {}
