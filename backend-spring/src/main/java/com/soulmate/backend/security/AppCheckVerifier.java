package com.soulmate.backend.security;

public interface AppCheckVerifier {

    String verifyAndGetAppId(String appCheckToken);
}
