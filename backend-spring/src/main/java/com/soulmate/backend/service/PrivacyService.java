package com.soulmate.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.soulmate.backend.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrivacyService {
    private static final Logger log = LoggerFactory.getLogger(PrivacyService.class);

    private final Firestore firestore;
    private final FirebaseAuth firebaseAuth;

    public PrivacyService(Firestore firestore, FirebaseAuth firebaseAuth) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
    }

    public void sendPasswordResetEmail(String email) {
        try {
            // FirebaseAuth generates a password reset link
            String link = firebaseAuth.generatePasswordResetLink(email);
            log.info("Generated password reset link for {}: {}", email, link);
            // In a real application, you would email this link to the user.
            // Since there's no actual email service configured here, we'll log it and succeed.
        } catch (Exception e) {
            log.error("Failed to generate password reset link for {}", email, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send password reset email: " + e.getMessage());
        }
    }

    public void deleteAccount(String uid) {
        try {
            WriteBatch batch = firestore.batch();

            // 1. Delete user document from "users"
            DocumentReference userRef = firestore.collection("users").document(uid);
            batch.delete(userRef);

            // 2. Find and delete user's diaries from "diaries"
            ApiFuture<QuerySnapshot> diariesFuture = firestore.collection("diaries")
                    .whereEqualTo("userId", uid).get();
            List<QueryDocumentSnapshot> diaries = diariesFuture.get().getDocuments();
            for (QueryDocumentSnapshot doc : diaries) {
                batch.delete(doc.getReference());
            }

            // 3. Find and delete user's community posts from "community_posts"
            ApiFuture<QuerySnapshot> postsFuture = firestore.collection("community_posts")
                    .whereEqualTo("userId", uid).get();
            List<QueryDocumentSnapshot> posts = postsFuture.get().getDocuments();
            for (QueryDocumentSnapshot doc : posts) {
                batch.delete(doc.getReference());
            }

            // Commit the Firestore deletes
            batch.commit().get();

            // 4. Delete user from Firebase Auth
            firebaseAuth.deleteUser(uid);
            log.info("Successfully deleted user account: {}", uid);
        } catch (Exception e) {
            log.error("Failed to delete user account: {}", uid, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete account: " + e.getMessage());
        }
    }
}
