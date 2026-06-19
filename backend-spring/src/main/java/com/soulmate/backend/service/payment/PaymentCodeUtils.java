package com.soulmate.backend.service.payment;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentCodeUtils {

    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("(SMPRM[A-Z0-9]{8})", Pattern.CASE_INSENSITIVE);
    private static final String PREFIX = "SMPRM";

    private PaymentCodeUtils() {
    }

    public static String generate() {
        String compact = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return PREFIX + compact.substring(0, 8);
    }

    public static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static String extract(String... candidates) {
        if (candidates == null) {
            return "";
        }

        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            Matcher matcher = PAYMENT_CODE_PATTERN.matcher(candidate.toUpperCase(Locale.ROOT));
            if (matcher.find()) {
                return matcher.group(1).toUpperCase(Locale.ROOT);
            }
        }

        return "";
    }
}
