package com.soulmate.backend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.soulmate.backend.config.BackendProperties;
import com.soulmate.backend.exception.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FirebaseSecurityFilter extends OncePerRequestFilter {

    private static final Pattern BEARER_PATTERN = Pattern.compile("^Bearer\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final String APP_CHECK_HEADER = "X-Firebase-AppCheck";

    private final FirebaseAuth firebaseAuth;
    private final AppCheckVerifier appCheckVerifier;
    private final BackendProperties backendProperties;
    private final ObjectMapper objectMapper;

    public FirebaseSecurityFilter(
        FirebaseAuth firebaseAuth,
        AppCheckVerifier appCheckVerifier,
        BackendProperties backendProperties,
        ObjectMapper objectMapper
    ) {
        this.firebaseAuth = firebaseAuth;
        this.appCheckVerifier = appCheckVerifier;
        this.backendProperties = backendProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator")
            || path.equals("/error")
            || !path.startsWith("/api/secure");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            String idToken = extractBearerToken(request.getHeader("Authorization"));
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken, true);
            String uid = decodedToken.getUid();

            String appId = null;
            if (backendProperties.getSecurity().isRequireAppCheck()) {
                String appCheckToken = request.getHeader(APP_CHECK_HEADER);
                appId = appCheckVerifier.verifyAndGetAppId(appCheckToken);
            }

            AuthContextHolder.set(request, new AuthContext(uid, appId));
            filterChain.doFilter(request, response);
        } catch (FirebaseAuthException | AppCheckVerificationException | IllegalArgumentException e) {
            writeUnauthorized(response, e.getMessage());
        }
    }

    private String extractBearerToken(String rawHeader) {
        if (!StringUtils.hasText(rawHeader)) {
            throw new IllegalArgumentException("Missing Authorization header.");
        }
        Matcher matcher = BEARER_PATTERN.matcher(rawHeader);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Authorization header must use Bearer token.");
        }
        String token = matcher.group(1).trim();
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Bearer token is empty.");
        }
        return token;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse error = new ApiErrorResponse(
            Instant.now().toString(),
            HttpStatus.UNAUTHORIZED.value(),
            "UNAUTHORIZED",
            StringUtils.hasText(message) ? message : "Unauthorized request."
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
