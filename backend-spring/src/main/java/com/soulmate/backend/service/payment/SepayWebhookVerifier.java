package com.soulmate.backend.service.payment;

import com.soulmate.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;

@Component
public class SepayWebhookVerifier {

    private final Clock clock;

    public SepayWebhookVerifier() {
        this(Clock.systemDefaultZone());
    }

    SepayWebhookVerifier(Clock clock) {
        this.clock = clock;
    }

    public void verify(String rawBody, String signatureHeader, String timestampHeader, String secret, long maxSkewSeconds) {
        if (!StringUtils.hasText(secret)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SePay webhook secret is not configured.");
        }
        if (!StringUtils.hasText(rawBody)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook payload is empty.");
        }
        if (!StringUtils.hasText(signatureHeader) || !StringUtils.hasText(timestampHeader)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing SePay signature headers.");
        }

        long timestamp = parseTimestamp(timestampHeader);
        long now = clock.instant().getEpochSecond();
        if (Math.abs(now - timestamp) > maxSkewSeconds) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SePay webhook timestamp is outside the allowed window.");
        }

        String expected = sign(rawBody, timestampHeader, secret);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = signatureHeader.trim().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid SePay webhook signature.");
        }
    }

    String sign(String rawBody, String timestampHeader, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = timestampHeader + "." + rawBody;
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + toHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot compute SePay webhook signature.", exception);
        }
    }

    private long parseTimestamp(String rawTimestamp) {
        try {
            return Long.parseLong(rawTimestamp.trim());
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid SePay webhook timestamp.");
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
