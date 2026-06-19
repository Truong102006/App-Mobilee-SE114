package com.soulmate.backend.service.payment;

import com.soulmate.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SepayWebhookVerifierTest {

    @Test
    void verifyAcceptsValidHmacSignature() {
        Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        SepayWebhookVerifier verifier = new SepayWebhookVerifier(fixedClock);
        String rawBody = "{\"id\":92704,\"content\":\"SMPRM1A2B3C4D\"}";
        String timestamp = "1700000000";
        String secret = "test-secret";
        String signature = verifier.sign(rawBody, timestamp, secret);

        assertDoesNotThrow(() -> verifier.verify(rawBody, signature, timestamp, secret, 300));
    }

    @Test
    void verifyRejectsInvalidSignature() {
        Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        SepayWebhookVerifier verifier = new SepayWebhookVerifier(fixedClock);

        assertThrows(ApiException.class, () -> verifier.verify(
            "{\"id\":1}",
            "sha256=invalid",
            "1700000000",
            "test-secret",
            300
        ));
    }

    @Test
    void verifyRejectsStaleTimestamp() {
        Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        SepayWebhookVerifier verifier = new SepayWebhookVerifier(fixedClock);
        String rawBody = "{\"id\":92704}";
        String secret = "test-secret";
        String timestamp = "1699990000";
        String signature = verifier.sign(rawBody, timestamp, secret);

        assertThrows(ApiException.class, () -> verifier.verify(rawBody, signature, timestamp, secret, 300));
    }
}
