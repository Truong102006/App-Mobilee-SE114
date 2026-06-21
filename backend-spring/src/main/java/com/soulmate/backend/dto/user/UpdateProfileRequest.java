package com.soulmate.backend.dto.user;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProfileRequest(
    @Size(max = 50, message = "anonymousName max length is 50")
    String anonymousName,
    @Size(max = 200, message = "bio max length is 200")
    String bio,
    List<SocialLink> socialLinks
) {
}
