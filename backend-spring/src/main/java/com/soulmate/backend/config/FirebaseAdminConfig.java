package com.soulmate.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class FirebaseAdminConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseAdminConfig.class);
    private static final List<String> DEFAULT_SERVICE_ACCOUNT_LOCATIONS = List.of(
        "secrets/service-account.json",
        "backend-spring/secrets/service-account.json"
    );

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties firebaseProperties, Environment environment) throws IOException {
        List<FirebaseApp> existingApps = FirebaseApp.getApps();
        if (!existingApps.isEmpty()) {
            return existingApps.get(0);
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
            .setCredentials(resolveCredentials(environment));

        if (StringUtils.hasText(firebaseProperties.getProjectId())) {
            optionsBuilder.setProjectId(firebaseProperties.getProjectId());
        }

        if (StringUtils.hasText(firebaseProperties.getStorageBucket())) {
            optionsBuilder.setStorageBucket(firebaseProperties.getStorageBucket());
        }

        return FirebaseApp.initializeApp(optionsBuilder.build());
    }

    GoogleCredentials resolveCredentials(Environment environment) throws IOException {
        Path credentialsPath = resolveCredentialsPath(environment);
        if (credentialsPath != null) {
            LOGGER.info("Initializing Firebase Admin with service account at {}", credentialsPath);
            try (InputStream inputStream = Files.newInputStream(credentialsPath)) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        LOGGER.info("Initializing Firebase Admin with application default credentials");
        return GoogleCredentials.getApplicationDefault();
    }

    Path resolveCredentialsPath(Environment environment) throws IOException {
        String systemCredentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(systemCredentialsPath)) {
            return requireExistingPath(systemCredentialsPath);
        }

        for (Path candidate : defaultServiceAccountCandidates()) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        String configuredPath = firstNonBlank(
            environment.getProperty("GOOGLE_APPLICATION_CREDENTIALS"),
            environment.getProperty("google.application.credentials")
        );
        if (StringUtils.hasText(configuredPath)) {
            return requireExistingPath(configuredPath);
        }

        return null;
    }

    private Path requireExistingPath(String rawPath) throws IOException {
        Path resolvedPath = Path.of(rawPath.trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(resolvedPath)) {
            throw new IOException("Firebase service account file was not found at " + resolvedPath);
        }
        return resolvedPath;
    }

    List<Path> defaultServiceAccountCandidates() {
        return DEFAULT_SERVICE_ACCOUNT_LOCATIONS.stream()
            .map(candidate -> Path.of(candidate).toAbsolutePath().normalize())
            .toList();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
