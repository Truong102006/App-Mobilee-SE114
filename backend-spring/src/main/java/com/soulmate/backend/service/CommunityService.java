package com.soulmate.backend.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.soulmate.backend.dto.community.CommentRequest;
import com.soulmate.backend.dto.community.CommentResponse;
import com.soulmate.backend.dto.community.CommonResponse;
import com.soulmate.backend.dto.community.CommunityPostDto;
import com.soulmate.backend.dto.community.PostResponse;
import com.soulmate.backend.dto.community.SavePostRequest;
import com.soulmate.backend.exception.ApiException;
import com.soulmate.backend.exception.FirestoreApiExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
public class CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityService.class);

    private final Firestore firestore;
    private final OneSignalPushNotificationService pushNotificationService;

    public CommunityService(Firestore firestore, OneSignalPushNotificationService pushNotificationService) {
        this.firestore = firestore;
        this.pushNotificationService = pushNotificationService;
    }

    public PostResponse createPost(String uid, SavePostRequest request) {
        Map<String, Object> userDetails = getUserDetails(uid);
        DocumentReference docRef = postsCollection().document();

        try {
            Map<String, Object> postData = new HashMap<>();
            postData.put("user_id", uid);
            postData.put("user_name", userDetails.get("name"));
            postData.put("user_avatar_url", userDetails.get("avatar"));
            postData.put("is_verified", userDetails.get("isVerified"));
            postData.put("mood", StringUtils.hasText(request.mood()) ? request.mood().trim() : "Neutral");
            postData.put("text_content", request.textContent().trim());
            postData.put("image_urls", request.imageUrls() != null ? request.imageUrls() : Collections.emptyList());
            postData.put("like_count", 0);
            postData.put("comment_count", 0);
            postData.put("view_count", 0);
            postData.put("liked_by", Collections.emptyList());
            postData.put("reportCount", 0);
            postData.put("reportedBy", Collections.emptyList());
            postData.put("timestamp", FieldValue.serverTimestamp());

            docRef.set(postData).get();

            return new PostResponse(
                docRef.getId(),
                uid,
                (String) userDetails.get("name"),
                (String) userDetails.get("avatar"),
                (Boolean) userDetails.get("isVerified"),
                StringUtils.hasText(request.mood()) ? request.mood().trim() : "Neutral",
                request.textContent().trim(),
                request.imageUrls() != null ? request.imageUrls() : Collections.emptyList(),
                0,
                0,
                0,
                Collections.emptyList(),
                System.currentTimeMillis()
            );
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to create post for uid={}", uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to create post", "Firestore quota exceeded.");
        }
    }

    public List<PostResponse> listPosts() {
        Query query = postsCollection().orderBy("timestamp", Query.Direction.DESCENDING);

        try {
            QuerySnapshot snapshot = query.get().get();
            return snapshot.getDocuments().stream()
                .map(this::toPostResponse)
                .toList();
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to list community posts", e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to list community posts", "Firestore error.");
        }
    }

    public void deletePost(String uid, String postId) {
        DocumentReference docRef = postsCollection().document(postId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Post not found.");
            }
            if (!Objects.equals(resolveUserId(snapshot), uid)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You cannot delete this post.");
            }
            docRef.delete().get();
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to delete post={} for uid={}", postId, uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to delete post", "Firestore error.");
        }
    }

    public PostResponse updatePost(String uid, String postId, SavePostRequest request) {
        DocumentReference docRef = postsCollection().document(postId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Post not found.");
            }
            if (!Objects.equals(resolveUserId(snapshot), uid)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You cannot update this post.");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("text_content", request.textContent().trim());
            if (request.imageUrls() != null) {
                updates.put("image_urls", request.imageUrls());
            }
            if (StringUtils.hasText(request.mood())) {
                updates.put("mood", request.mood().trim());
            }

            docRef.update(updates).get();
            DocumentSnapshot updatedSnapshot = docRef.get().get();
            return toPostResponse(updatedSnapshot);
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to update post={} for uid={}", postId, uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to update post", "Firestore error.");
        }
    }

    public void toggleLike(String uid, String postId) {
        DocumentReference docRef = postsCollection().document(postId);
        try {
            LikeToggleResult toggleResult = firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef).get();
                if (!snapshot.exists()) {
                    throw new IllegalStateException("Post not found.");
                }

                List<String> likedBy = getStringList(snapshot, "liked_by");
                List<String> newLikedBy = new ArrayList<>(likedBy);
                boolean addedLike;
                if (newLikedBy.contains(uid)) {
                    newLikedBy.remove(uid);
                    addedLike = false;
                } else {
                    newLikedBy.add(uid);
                    addedLike = true;
                }

                transaction.update(docRef, "liked_by", newLikedBy);
                transaction.update(docRef, "like_count", newLikedBy.size());
                return new LikeToggleResult(addedLike, resolveUserId(snapshot));
            }).get();

            if (toggleResult.addedLike()) {
                pushNotificationService.sendPostLikeNotification(uid, toggleResult.postOwnerUserId(), postId);
            }
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to toggle like on post={} for uid={}", postId, uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to toggle like.", "Firestore error.");
        }
    }

    public CommentResponse addComment(String uid, String postId, CommentRequest request) {
        Map<String, Object> userDetails = getUserDetails(uid);
        DocumentReference postRef = postsCollection().document(postId);
        DocumentReference commentRef = postRef.collection("comments").document();

        try {
            DocumentSnapshot postSnapshot = postRef.get().get();
            if (!postSnapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Post not found.");
            }

            Map<String, Object> commentData = new HashMap<>();
            commentData.put("user_id", uid);
            commentData.put("user_name", userDetails.get("name"));
            commentData.put("user_avatar_url", userDetails.get("avatar"));
            commentData.put("content", request.content().trim());
            commentData.put("liked_by", Collections.emptyList());
            commentData.put("parent_id", request.parentId());
            commentData.put("reply_to_user_name", request.replyToUserName());
            commentData.put("timestamp", FieldValue.serverTimestamp());

            WriteBatch batch = firestore.batch();
            batch.set(commentRef, commentData);
            batch.update(postRef, "comment_count", FieldValue.increment(1));
            batch.commit().get();

            CommentResponse response = new CommentResponse(
                commentRef.getId(),
                uid,
                (String) userDetails.get("name"),
                (String) userDetails.get("avatar"),
                request.content().trim(),
                System.currentTimeMillis(),
                Collections.emptyList(),
                request.parentId(),
                request.replyToUserName()
            );
            pushNotificationService.sendPostCommentNotification(
                uid,
                resolveUserId(postSnapshot),
                postId,
                request.content().trim()
            );
            return response;
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to add comment on post={} for uid={}", postId, uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to add comment.", "Firestore error.");
        }
    }

    public void toggleCommentLike(String uid, String postId, String commentId) {
        DocumentReference commentRef = postsCollection().document(postId).collection("comments").document(commentId);
        try {
            firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(commentRef).get();
                if (!snapshot.exists()) {
                    throw new IllegalStateException("Comment not found.");
                }

                List<String> likedBy = getStringList(snapshot, "liked_by");
                List<String> newLikedBy = new ArrayList<>(likedBy);
                if (newLikedBy.contains(uid)) {
                    newLikedBy.remove(uid);
                } else {
                    newLikedBy.add(uid);
                }

                transaction.update(commentRef, "liked_by", newLikedBy);
                return null;
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to toggle like on comment={} for uid={}", commentId, uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to toggle comment like.", "Firestore error.");
        }
    }

    public List<CommentResponse> listComments(String postId) {
        Query query = postsCollection().document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING);

        try {
            QuerySnapshot snapshot = query.get().get();
            return snapshot.getDocuments().stream()
                .map(this::toCommentResponse)
                .toList();
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to list comments for post={}", postId, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to list comments", "Firestore error.");
        }
    }

    public CommonResponse reportPost(String userId, String postId) {
        try {
            DocumentReference postRef = postsCollection().document(postId);
            DocumentReference userRef = firestore.collection("users").document(userId);

            firestore.runTransaction(transaction -> {
                DocumentSnapshot postSnap = transaction.get(postRef).get();
                if (!postSnap.exists()) {
                    throw new IllegalStateException("Post not found.");
                }

                List<String> reportedBy = getStringList(postSnap, "reportedBy");
                if (!reportedBy.contains(userId)) {
                    List<String> updatedReportedBy = new ArrayList<>(reportedBy);
                    updatedReportedBy.add(userId);
                    long currentCount = postSnap.getLong("reportCount") != null ? postSnap.getLong("reportCount") : 0L;
                    transaction.update(postRef, "reportedBy", updatedReportedBy);
                    transaction.update(postRef, "reportCount", currentCount + 1L);
                    transaction.update(userRef, "hiddenPostIds", FieldValue.arrayUnion(postId));
                }
                return null;
            }).get();

            return new CommonResponse(true, "Da bao cao bai viet");
        } catch (Exception e) {
            log.error("Failed to report post={} for uid={}", postId, userId, e);
            return new CommonResponse(false, e.getMessage());
        }
    }

    public List<CommunityPostDto> getReportedPosts() {
        try {
            QuerySnapshot querySnapshot = postsCollection()
                .whereGreaterThan("reportCount", 0)
                .get()
                .get();

            return querySnapshot.getDocuments().stream()
                .map(this::toCommunityPostDto)
                .toList();
        } catch (Exception e) {
            log.error("Failed to fetch reported posts", e);
            return new ArrayList<>();
        }
    }

    public CommonResponse resolveReport(String postId, String action) {
        try {
            DocumentReference postRef = postsCollection().document(postId);
            if ("delete".equalsIgnoreCase(action)) {
                postRef.delete().get();
            } else {
                postRef.update("reportCount", 0, "reportedBy", new ArrayList<>()).get();
            }
            return new CommonResponse(true, "Thuc hien thanh cong");
        } catch (Exception e) {
            log.error("Failed to resolve report for post={} with action={}", postId, action, e);
            return new CommonResponse(false, e.getMessage());
        }
    }

    private CollectionReference postsCollection() {
        return firestore.collection("community_posts");
    }

    private PostResponse toPostResponse(DocumentSnapshot doc) {
        List<String> likedBy = getStringList(doc, "liked_by");

        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) doc.get("image_urls");
        if (imageUrls == null) {
            imageUrls = (List<String>) doc.get("imageUrls");
        }
        if (imageUrls == null) {
            imageUrls = Collections.emptyList();
        }

        Boolean isVerified = doc.getBoolean("is_verified");
        if (isVerified == null) {
            isVerified = doc.getBoolean("isVerified");
        }
        if (isVerified == null) {
            isVerified = false;
        }

        return new PostResponse(
            doc.getId(),
            resolveUserId(doc),
            resolveUserName(doc),
            firstNonBlank(doc.getString("user_avatar_url"), doc.getString("userAvatarUrl")),
            isVerified,
            firstNonBlank(doc.getString("mood"), "Neutral"),
            firstNonBlank(doc.getString("text_content"), doc.getString("textContent"), ""),
            imageUrls,
            getIntValue(doc, "like_count"),
            getIntValue(doc, "comment_count"),
            getIntValue(doc, "view_count"),
            likedBy,
            toMillis(doc.get("timestamp"))
        );
    }

    private CommentResponse toCommentResponse(DocumentSnapshot doc) {
        List<String> likedBy = getStringList(doc, "liked_by");

        return new CommentResponse(
            doc.getId(),
            firstNonBlank(doc.getString("user_id"), doc.getString("userId"), ""),
            firstNonBlank(doc.getString("user_name"), doc.getString("userName"), "SoulMate User"),
            firstNonBlank(doc.getString("user_avatar_url"), doc.getString("userAvatarUrl")),
            firstNonBlank(doc.getString("content"), ""),
            toMillis(doc.get("timestamp")),
            likedBy,
            firstNonBlank(doc.getString("parent_id"), doc.getString("parentId")),
            firstNonBlank(doc.getString("reply_to_user_name"), doc.getString("replyToUserName"))
        );
    }

    private CommunityPostDto toCommunityPostDto(DocumentSnapshot doc) {
        CommunityPostDto dto = new CommunityPostDto();
        dto.id = doc.getId();
        dto.userId = resolveUserId(doc);
        dto.userName = resolveUserName(doc);
        dto.userAvatarUrl = firstNonBlank(doc.getString("user_avatar_url"), doc.getString("userAvatarUrl"));
        dto.textContent = firstNonBlank(doc.getString("text_content"), doc.getString("textContent"), "");
        dto.reportCount = getIntValue(doc, "reportCount");
        dto.reportedBy = getStringList(doc, "reportedBy");
        dto.timestamp = toMillis(doc.get("timestamp"));
        return dto;
    }

    private String resolveUserId(DocumentSnapshot snapshot) {
        return firstNonBlank(snapshot.getString("user_id"), snapshot.getString("userId"), "");
    }

    private String resolveUserName(DocumentSnapshot snapshot) {
        return firstNonBlank(snapshot.getString("user_name"), snapshot.getString("userName"), "SoulMate User");
    }

    private int getIntValue(DocumentSnapshot snapshot, String field) {
        Long value = snapshot.getLong(field);
        return value != null ? value.intValue() : 0;
    }

    private List<String> getStringList(DocumentSnapshot snapshot, String field) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) snapshot.get(field);
        return values != null ? values : Collections.emptyList();
    }

    private Map<String, Object> getUserDetails(String uid) {
        Map<String, Object> details = new HashMap<>();
        details.put("name", "SoulMate User");
        details.put("avatar", null);
        details.put("isVerified", false);

        try {
            DocumentSnapshot snapshot = firestore.collection("users").document(uid).get().get();
            if (snapshot.exists()) {
                String name = snapshot.getString("anonymousName");
                if (name != null) {
                    details.put("name", name);
                }
                details.put("avatar", snapshot.getString("avatarUrl"));
                details.put("isVerified", Objects.equals(snapshot.getString("role"), "admin"));
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve user info from Firestore for uid={}, falling back to default.", uid, e);
        }
        return details;
    }

    private Long toMillis(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toDate().getTime();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        return System.currentTimeMillis();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private record LikeToggleResult(boolean addedLike, String postOwnerUserId) {
    }
}
