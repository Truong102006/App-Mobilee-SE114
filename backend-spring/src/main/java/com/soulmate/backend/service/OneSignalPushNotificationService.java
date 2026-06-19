package com.soulmate.backend.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.soulmate.backend.config.BackendProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OneSignalPushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OneSignalPushNotificationService.class);
    private static final int MESSAGE_PREVIEW_LIMIT = 120;

    private final Firestore firestore;
    private final BackendProperties.OneSignal oneSignalProperties;
    private final RestClient restClient;

    public OneSignalPushNotificationService(
        Firestore firestore,
        BackendProperties backendProperties,
        RestClient.Builder restClientBuilder
    ) {
        this.firestore = firestore;
        this.oneSignalProperties = backendProperties.getOneSignal();
        this.restClient = restClientBuilder
            .baseUrl(this.oneSignalProperties.getApiUrl())
            .build();
    }

    public void sendChatMessageNotification(String senderUserId, String recipientUserId, String messageText, String imageUrl) {
        if (!shouldSendToUser(recipientUserId) || senderUserId.equals(recipientUserId)) {
            return;
        }

        UserProfile sender = loadUserProfile(senderUserId, false);
        String senderName = sender.name();
        String preview = StringUtils.hasText(messageText)
            ? abbreviate(messageText.trim(), MESSAGE_PREVIEW_LIMIT)
            : "sent you an image.";

        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "chat_message");
        data.put("screen", "chat");
        data.put("userId", senderUserId);
        data.put("userName", senderName);
        if (StringUtils.hasText(sender.avatarUrl())) {
            data.put("avatarUrl", sender.avatarUrl());
        }
        if (StringUtils.hasText(imageUrl)) {
            data.put("imageUrl", imageUrl.trim());
        }

        sendPush(
            recipientUserId,
            "New message",
            StringUtils.hasText(messageText) ? senderName + ": " + preview : senderName + " " + preview,
            data
        );
    }

    public void sendPostLikeNotification(String actorUserId, String recipientUserId, String postId) {
        if (!shouldSendToUser(recipientUserId) || actorUserId.equals(recipientUserId)) {
            return;
        }

        UserProfile actor = loadUserProfile(actorUserId, false);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "post_like");
        data.put("screen", "community");
        data.put("postId", postId);
        data.put("actorUserId", actorUserId);
        data.put("actorName", actor.name());

        sendPush(
            recipientUserId,
            "New like",
            actor.name() + " liked your post.",
            data
        );
    }

    public void sendPostCommentNotification(String actorUserId, String recipientUserId, String postId, String commentContent) {
        if (!shouldSendToUser(recipientUserId) || actorUserId.equals(recipientUserId)) {
            return;
        }

        UserProfile actor = loadUserProfile(actorUserId, false);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "post_comment");
        data.put("screen", "community");
        data.put("postId", postId);
        data.put("actorUserId", actorUserId);
        data.put("actorName", actor.name());

        String preview = abbreviate(commentContent, MESSAGE_PREVIEW_LIMIT);
        sendPush(
            recipientUserId,
            "New comment",
            actor.name() + " commented on your post: " + preview,
            data
        );
    }

    private void sendPush(String recipientUserId, String title, String body, Map<String, String> data) {
        if (!isConfigured()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app_id", oneSignalProperties.getAppId().trim());
        payload.put("target_channel", "push");
        payload.put("include_aliases", Map.of("external_id", List.of(recipientUserId)));
        payload.put("headings", Map.of("en", title));
        payload.put("contents", Map.of("en", body));
        payload.put("data", data);

        try {
            restClient.post()
                .uri("/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Key " + oneSignalProperties.getApiKey().trim())
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn(
                "OneSignal push failed for recipientUserId={} status={} body={}",
                recipientUserId,
                e.getStatusCode(),
                e.getResponseBodyAsString(),
                e
            );
        } catch (Exception e) {
            log.warn("OneSignal push failed for recipientUserId={}", recipientUserId, e);
        }
    }

    private boolean shouldSendToUser(String userId) {
        if (!isConfigured() || !StringUtils.hasText(userId)) {
            return false;
        }

        UserProfile recipient = loadUserProfile(userId, true);
        return recipient.notificationEnabled();
    }

    private UserProfile loadUserProfile(String userId, boolean conservativeOnFailure) {
        UserProfile fallback = new UserProfile(
            userId,
            "SoulMate User",
            null,
            !conservativeOnFailure
        );

        if (!StringUtils.hasText(userId)) {
            return fallback;
        }

        try {
            DocumentSnapshot snapshot = firestore.collection("users").document(userId).get().get();
            if (!snapshot.exists()) {
                return fallback;
            }

            String name = firstNonBlank(snapshot.getString("anonymousName"), fallback.name());
            String avatarUrl = trimToNull(snapshot.getString("avatarUrl"));
            Boolean notificationEnabled = snapshot.getBoolean("notificationEnabled");

            return new UserProfile(
                userId,
                name,
                avatarUrl,
                notificationEnabled == null ? true : notificationEnabled
            );
        } catch (Exception e) {
            log.warn("Failed to load user profile for notification userId={}", userId, e);
            return fallback;
        }
    }

    private boolean isConfigured() {
        return oneSignalProperties.isEnabled()
            && StringUtils.hasText(oneSignalProperties.getAppId())
            && StringUtils.hasText(oneSignalProperties.getApiKey());
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength - 3) + "...";
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record UserProfile(
        String userId,
        String name,
        String avatarUrl,
        boolean notificationEnabled
    ) {
    }
}
