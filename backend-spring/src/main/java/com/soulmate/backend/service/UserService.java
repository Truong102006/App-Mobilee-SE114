package com.soulmate.backend.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.soulmate.backend.dto.community.CommonResponse;
import com.soulmate.backend.dto.user.SocialLink;
import com.soulmate.backend.dto.user.UserProfileResponse;
import com.soulmate.backend.dto.user.UpdateProfileRequest;
import com.soulmate.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final Firestore firestore;

    public UserService(Firestore firestore) {
        this.firestore = firestore;
    }

    public CommonResponse toggleSocialBan(String userId, boolean isBanned) {
        try {
            firestore.collection("users").document(userId)
                    .update("isSocialBanned", isBanned).get();
            return new CommonResponse(true, "Cập nhật trạng thái ban thành công");
        } catch (Exception e) {
            return new CommonResponse(false, e.getMessage());
        }
    }

    public UserProfileResponse getUserProfile(String userId) {
        try {
            DocumentSnapshot snapshot = firestore.collection("users").document(userId).get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "User not found.");
            }

            String name = snapshot.getString("anonymousName");
            if (name == null || name.strip().isEmpty()) {
                name = "SoulMate User";
            }
            String avatarUrl = snapshot.getString("avatarUrl");
            String bio = snapshot.getString("bio");
            if (bio == null) {
                bio = "";
            }

            List<SocialLink> socialLinks = new ArrayList<>();
            Object rawLinks = snapshot.get("socialLinks");
            if (rawLinks instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        socialLinks.add(new SocialLink(
                            (String) map.get("platform"),
                            (String) map.get("url")
                        ));
                    }
                }
            }

            Long createdAt = null;
            Object rawCreated = snapshot.get("createdAt");
            if (rawCreated instanceof com.google.cloud.Timestamp ts) {
                createdAt = ts.toDate().getTime();
            } else if (rawCreated instanceof Long l) {
                createdAt = l;
            } else if (snapshot.getCreateTime() != null) {
                createdAt = snapshot.getCreateTime().toDate().getTime();
            }
            if (createdAt == null) {
                createdAt = System.currentTimeMillis();
            }

            return new UserProfileResponse(userId, name, avatarUrl, bio, socialLinks, createdAt);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch user profile: " + e.getMessage());
        }
    }

    public void updateProfile(String uid, UpdateProfileRequest request) {
        try {
            Map<String, Object> updates = new HashMap<>();
            if (request.anonymousName() != null) {
                updates.put("anonymousName", request.anonymousName().trim());
            }
            if (request.bio() != null) {
                updates.put("bio", request.bio().trim());
            }
            if (request.socialLinks() != null) {
                List<Map<String, String>> serializedLinks = new ArrayList<>();
                for (SocialLink link : request.socialLinks()) {
                    Map<String, String> m = new HashMap<>();
                    m.put("platform", link.platform());
                    m.put("url", link.url());
                    serializedLinks.add(m);
                }
                updates.put("socialLinks", serializedLinks);
            }

            if (!updates.isEmpty()) {
                firestore.collection("users").document(uid).update(updates).get();
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update profile: " + e.getMessage());
        }
    }

    public void updateAvatar(String uid, String avatarUrl) {
        try {
            firestore.collection("users").document(uid).update("avatarUrl", avatarUrl.trim()).get();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update avatar: " + e.getMessage());
        }
    }

    public void blockUser(String uid, String targetUserId) {
        if (uid.equals(targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot block yourself.");
        }
        try {
            firestore.collection("users").document(uid)
                    .update("blockedUsers", FieldValue.arrayUnion(targetUserId)).get();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to block user: " + e.getMessage());
        }
    }

    public void unblockUser(String uid, String targetUserId) {
        try {
            firestore.collection("users").document(uid)
                    .update("blockedUsers", FieldValue.arrayRemove(targetUserId)).get();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to unblock user: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> listBlockedUsers(String uid) {
        try {
            DocumentSnapshot snapshot = firestore.collection("users").document(uid).get().get();
            if (!snapshot.exists()) {
                return Collections.emptyList();
            }
            List<String> blockedUsers = (List<String>) snapshot.get("blockedUsers");
            return blockedUsers == null ? Collections.emptyList() : blockedUsers;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list blocked users: " + e.getMessage());
        }
    }
}