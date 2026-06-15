package com.soulmate.backend.exception;

import com.google.api.gax.rpc.ResourceExhaustedException;
import org.springframework.http.HttpStatus;

public final class FirestoreApiExceptionMapper {

    private FirestoreApiExceptionMapper() {
    }

    public static ApiException map(Throwable throwable, String fallbackMessage, String quotaMessage) {
        if (isResourceExhausted(throwable)) {
            return new ApiException(HttpStatus.TOO_MANY_REQUESTS, quotaMessage);
        }
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, fallbackMessage);
    }

    private static boolean isResourceExhausted(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceExhaustedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
