package com.soulmate.backend.service;

import com.soulmate.backend.config.BackendProperties;
import com.soulmate.backend.dto.cloudinary.SignUploadRequest;
import com.soulmate.backend.dto.cloudinary.SignUploadResponse;
import com.soulmate.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CloudinaryService {

    private final BackendProperties.Cloudinary cloudinaryProperties;

    public CloudinaryService(BackendProperties backendProperties) {
        this.cloudinaryProperties = backendProperties.getCloudinary();
    }

    public SignUploadResponse signUpload(String uid, SignUploadRequest request) {
        requireConfig(cloudinaryProperties.getCloudName(), "CLOUDINARY_CLOUD_NAME");
        requireConfig(cloudinaryProperties.getApiKey(), "CLOUDINARY_API_KEY");
        requireConfig(cloudinaryProperties.getApiSecret(), "CLOUDINARY_API_SECRET");

        long timestamp = Instant.now().getEpochSecond();
        String folder = cloudinaryProperties.getUploadFolder() + "/" + uid;
        String publicId = trimToNull(request.publicId());
        String context = trimToNull(request.context());

        Map<String, String> toSign = new LinkedHashMap<>();
        toSign.put("folder", folder);
        toSign.put("timestamp", String.valueOf(timestamp));
        if (publicId != null) {
            toSign.put("public_id", publicId);
        }
        if (context != null) {
            toSign.put("context", context);
        }

        String signature = sign(toSign, cloudinaryProperties.getApiSecret());

        return new SignUploadResponse(
            cloudinaryProperties.getCloudName(),
            cloudinaryProperties.getApiKey(),
            folder,
            timestamp,
            signature,
            publicId,
            context,
            "https://api.cloudinary.com/v1_1/" + cloudinaryProperties.getCloudName() + "/auto/upload"
        );
    }

    private String sign(Map<String, String> parameters, String apiSecret) {
        String serialized = parameters.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));

        String payload = serialized + apiSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate Cloudinary signature.");
        }
    }

    private void requireConfig(String value, String keyName) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing backend secret: " + keyName);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
