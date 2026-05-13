package com.soulmate.backend.dto.cloudinary;

import jakarta.validation.constraints.Size;

public record SignUploadRequest(
    @Size(max = 140, message = "publicId max length is 140")
    String publicId,
    @Size(max = 500, message = "context max length is 500")
    String context
) {
}
