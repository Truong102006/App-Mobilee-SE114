package com.soulmate.backend.security;

public record AuthContext(
    String uid,
    String appId
) {
}
