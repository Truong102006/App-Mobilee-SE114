package com.soulmate.backend.security;

public class AppCheckVerificationException extends RuntimeException {
    public AppCheckVerificationException(String message) {
        super(message);
    }

    public AppCheckVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
