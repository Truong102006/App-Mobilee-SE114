package com.soulmate.backend.security;

import jakarta.servlet.http.HttpServletRequest;

public final class AuthContextHolder {

    public static final String ATTR_NAME = "SOULMATE_AUTH_CONTEXT";

    private AuthContextHolder() {
    }

    public static void set(HttpServletRequest request, AuthContext context) {
        request.setAttribute(ATTR_NAME, context);
    }

    public static AuthContext getRequired(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_NAME);
        if (value instanceof AuthContext authContext) {
            return authContext;
        }
        throw new IllegalStateException("Auth context not found in request.");
    }
}
