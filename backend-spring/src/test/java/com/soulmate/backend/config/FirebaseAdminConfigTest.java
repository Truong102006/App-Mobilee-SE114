package com.soulmate.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FirebaseAdminConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveCredentialsPathUsesConfiguredProperty() throws IOException {
        FirebaseAdminConfig firebaseAdminConfig = new FirebaseAdminConfig() {
            @Override
            List<Path> defaultServiceAccountCandidates() {
                return List.of();
            }
        };

        Path serviceAccount = tempDir.resolve("service-account.json");
        Files.writeString(serviceAccount, "{}");

        MockEnvironment environment = new MockEnvironment()
            .withProperty("GOOGLE_APPLICATION_CREDENTIALS", serviceAccount.toString());

        Path resolvedPath = firebaseAdminConfig.resolveCredentialsPath(environment);

        assertEquals(serviceAccount.toAbsolutePath().normalize(), resolvedPath);
    }

    @Test
    void resolveCredentialsPathFailsFastForMissingConfiguredFile() {
        FirebaseAdminConfig firebaseAdminConfig = new FirebaseAdminConfig() {
            @Override
            List<Path> defaultServiceAccountCandidates() {
                return List.of();
            }
        };

        Path missingPath = tempDir.resolve("missing-service-account.json");
        MockEnvironment environment = new MockEnvironment()
            .withProperty("GOOGLE_APPLICATION_CREDENTIALS", missingPath.toString());

        IOException error = assertThrows(
            IOException.class,
            () -> firebaseAdminConfig.resolveCredentialsPath(environment)
        );

        assertEquals(
            "Firebase service account file was not found at " + missingPath.toAbsolutePath().normalize(),
            error.getMessage()
        );
    }

    @Test
    void resolveCredentialsPathUsesDefaultRepoLocationWhenPresent() throws IOException {
        Path serviceAccount = tempDir.resolve("service-account.json");
        Files.writeString(serviceAccount, "{}");

        FirebaseAdminConfig firebaseAdminConfig = new FirebaseAdminConfig() {
            @Override
            List<Path> defaultServiceAccountCandidates() {
                return List.of(serviceAccount.toAbsolutePath().normalize());
            }
        };

        Path resolvedPath = firebaseAdminConfig.resolveCredentialsPath(new MockEnvironment());

        assertEquals(serviceAccount.toAbsolutePath().normalize(), resolvedPath);
    }
}
