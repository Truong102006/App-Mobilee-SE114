package com.soulmate.backend.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class FirebaseJwtAppCheckVerifier implements AppCheckVerifier {

    private static final String FIREBASE_APP_CHECK_ISSUER = "https://firebaseappcheck.googleapis.com/";
    private static final String JWKS_URL = "https://firebaseappcheck.googleapis.com/v1/jwks";

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final String expectedProjectNumber;

    public FirebaseJwtAppCheckVerifier(com.soulmate.backend.config.BackendProperties backendProperties) {
        this.expectedProjectNumber = backendProperties.getSecurity().getFirebaseProjectNumber();
        this.jwtProcessor = buildProcessor();
    }

    @Override
    public String verifyAndGetAppId(String appCheckToken) {
        if (!StringUtils.hasText(expectedProjectNumber)) {
            throw new AppCheckVerificationException(
                "Missing BACKEND_SECURITY_FIREBASE_PROJECT_NUMBER for App Check verification."
            );
        }

        if (!StringUtils.hasText(appCheckToken)) {
            throw new AppCheckVerificationException("App Check token is missing.");
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(appCheckToken);
            validateHeader(signedJWT);

            JWTClaimsSet claims = jwtProcessor.process(appCheckToken, null);
            validateClaims(claims);

            String appId = claims.getSubject();
            if (!StringUtils.hasText(appId)) {
                throw new AppCheckVerificationException("Invalid App Check token subject.");
            }
            return appId;
        } catch (ParseException e) {
            throw new AppCheckVerificationException("Invalid App Check token format.", e);
        } catch (AppCheckVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new AppCheckVerificationException("App Check token verification failed.", e);
        }
    }

    private void validateHeader(SignedJWT signedJWT) {
        if (!Objects.equals(JWSAlgorithm.RS256, signedJWT.getHeader().getAlgorithm())) {
            throw new AppCheckVerificationException("App Check token must use RS256.");
        }

        JOSEObjectType type = signedJWT.getHeader().getType();
        if (type == null || !"JWT".equalsIgnoreCase(type.toString())) {
            throw new AppCheckVerificationException("App Check token type must be JWT.");
        }
    }

    private void validateClaims(JWTClaimsSet claims) {
        Instant now = Instant.now();

        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(now)) {
            throw new AppCheckVerificationException("App Check token is expired.");
        }

        if (claims.getIssueTime() == null || claims.getIssueTime().toInstant().isAfter(now.plusSeconds(60))) {
            throw new AppCheckVerificationException("App Check token issue time is invalid.");
        }

        String expectedIssuer = FIREBASE_APP_CHECK_ISSUER + expectedProjectNumber;
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new AppCheckVerificationException("App Check token issuer is invalid.");
        }

        List<String> audience = claims.getAudience();
        String expectedAudience = "projects/" + expectedProjectNumber;
        if (audience == null || !audience.contains(expectedAudience)) {
            throw new AppCheckVerificationException("App Check token audience is invalid.");
        }
    }

    private ConfigurableJWTProcessor<SecurityContext> buildProcessor() {
        try {
            URL jwksUrl = new URL(JWKS_URL);
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(jwksUrl);
            JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(selector);
            return processor;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Firebase App Check JWKS URL.", e);
        }
    }
}
