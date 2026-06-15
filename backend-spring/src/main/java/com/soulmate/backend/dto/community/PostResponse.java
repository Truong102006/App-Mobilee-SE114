package com.soulmate.backend.dto.community;

import java.util.List;

public record PostResponse(
    String id,
    String userId,
    String userName,
    String userAvatarUrl,
    Boolean isVerified,
    String mood,
    String textContent,
    List<String> imageUrls,
    Integer likeCount,
    Integer commentCount,
    Integer viewCount,
    List<String> likedBy,
    Long timestamp
) {}
