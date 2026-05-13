package com.soulmate.backend.dto.cloudinary;

public record SignUploadResponse(
    String cloudName,
    String apiKey,
    String folder,
    long timestamp,
    String signature,
    String publicId,
    String context,
    String uploadUrl
) {
}
