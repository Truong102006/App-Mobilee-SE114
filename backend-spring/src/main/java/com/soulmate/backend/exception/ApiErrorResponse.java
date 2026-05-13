package com.soulmate.backend.exception;

public record ApiErrorResponse(
    String timestamp,
    int status,
    String error,
    String message
) {
}
