package com.soulmate.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Configuration
public class FirebaseAdminConfig {

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties firebaseProperties) throws IOException {
        List<FirebaseApp> existingApps = FirebaseApp.getApps();
        if (!existingApps.isEmpty()) {
            return existingApps.getFirst();
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault());

        if (StringUtils.hasText(firebaseProperties.getProjectId())) {
            optionsBuilder.setProjectId(firebaseProperties.getProjectId());
        }

        if (StringUtils.hasText(firebaseProperties.getStorageBucket())) {
            optionsBuilder.setStorageBucket(firebaseProperties.getStorageBucket());
        }

        return FirebaseApp.initializeApp(optionsBuilder.build());
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
