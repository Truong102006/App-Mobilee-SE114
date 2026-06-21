package com.soulmate.backend.dto.user;

import java.util.List;

public record UserProfileResponse(
    String userId,
    String anonymousName,
    String avatarUrl,
    String bio,
    List<SocialLink> socialLinks,
    Long createdAt
) {
}
