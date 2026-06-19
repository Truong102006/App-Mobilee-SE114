package com.soulmate.backend.dto.community;

import java.util.List;

public record CommentResponse(
    String id,
    String userId,
    String userName,
    String userAvatarUrl,
    String content,
    Long timestamp,
    List<String> likedBy,
    String parentId,
    String replyToUserName
) {}
