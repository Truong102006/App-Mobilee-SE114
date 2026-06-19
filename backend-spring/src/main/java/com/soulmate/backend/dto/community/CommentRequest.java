package com.soulmate.backend.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
    @NotBlank(message = "content is required")
    @Size(max = 1000, message = "content max length is 1000")
    String content,
    String parentId,
    String replyToUserName
) {}
