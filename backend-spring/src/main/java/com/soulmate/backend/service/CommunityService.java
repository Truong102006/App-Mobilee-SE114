package com.soulmate.backend.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.soulmate.backend.dto.community.*;
import com.soulmate.backend.exception.ApiException;
import com.soulmate.backend.exception.FirestoreApiExceptionMapper;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityService.class);

    private final Firestore firestore;

    public CommunityService(Firestore firestore) {
        this.firestore = firestore;
    }

    public PostResponse createPost(String uid, SavePostRequest request) {
        Map<String, Object> userDetails = getUserDetails(uid);
        DocumentReference docRef = firestore.collection("community_posts").document();

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
        Query query = firestore.collection("community_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING);

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
        DocumentReference docRef = firestore.collection("community_posts").document(postId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Post not found.");
            }
            if (!Objects.equals(snapshot.getString("user_id"), uid)) {
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
        DocumentReference docRef = firestore.collection("community_posts").document(postId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Post not found.");
            }
            if (!Objects.equals(snapshot.getString("user_id"), uid)) {
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

            // Fetch the updated post to return it
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
        DocumentReference docRef = firestore.collection("community_posts").document(postId);
        try {
            firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef).get();
                if (!snapshot.exists()) {
                    throw new FirebaseException("Post not found.", OpenRecordException.NOT_FOUND);
                }

                @SuppressWarnings("unchecked")
                List<String> likedBy = (List<String>) snapshot.get("liked_by");
                if (likedBy == null) likedBy = new ArrayList<>();

                List<String> newLikedBy = new ArrayList<>(likedBy);
                if (newLikedBy.contains(uid)) {
                    newLikedBy.remove(uid);
                } else {
                    newLikedBy.add(uid);
                }

                transaction.update(docRef, "liked_by", newLikedBy);
                transaction.update(docRef, "like_count", newLikedBy.size());
                return null;
            }).get();
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
        DocumentReference postRef = firestore.collection("community_posts").document(postId);
        DocumentReference commentRef = postRef.collection("comments").document();

        try {
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

            return new CommentResponse(
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
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to add comment on post={} for uid={}", postId, uid, e);
            throw FirestoreApiExceptionMapper.map(e, "Failed to add comment.", "Firestore error.");
        }
    }

    public void toggleCommentLike(String uid, String postId, String commentId) {
        DocumentReference commentRef = firestore.collection("community_posts").document(postId).collection("comments").document(commentId);
        try {
            firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(commentRef).get();
                if (!snapshot.exists()) {
                    throw new FirebaseException("Comment not found.", OpenRecordException.NOT_FOUND);
                }

                @SuppressWarnings("unchecked")
                List<String> likedBy = (List<String>) snapshot.get("liked_by");
                if (likedBy == null) likedBy = new ArrayList<>();

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
        Query query = firestore.collection("community_posts").document(postId).collection("comments")
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

    private PostResponse toPostResponse(DocumentSnapshot doc) {
        @SuppressWarnings("unchecked")
        List<String> likedBy = (List<String>) doc.get("liked_by");
        if (likedBy == null) likedBy = Collections.emptyList();

        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) doc.get("image_urls");
        if (imageUrls == null) imageUrls = Collections.emptyList();

        String userId = doc.getString("user_id");
        if (userId == null) {
            userId = doc.getString("userId");
        }
        if (userId == null) {
            userId = "";
        }

        String userName = doc.getString("user_name");
        if (userName == null) {
            userName = doc.getString("userName");
        }
        if (userName == null) {
            userName = "SoulMate User";
        }

        String mood = doc.getString("mood");
        if (mood == null) {
            mood = "Neutral";
        }

        String textContent = doc.getString("text_content");
        if (textContent == null) {
            textContent = doc.getString("textContent");
        }
        if (textContent == null) {
            textContent = "";
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
            userId,
            userName,
            doc.getString("user_avatar_url") != null ? doc.getString("user_avatar_url") : doc.getString("userAvatarUrl"),
            isVerified,
            mood,
            textContent,
            imageUrls,
            doc.getLong("like_count") != null ? doc.getLong("like_count").intValue() : 0,
            doc.getLong("comment_count") != null ? doc.getLong("comment_count").intValue() : 0,
            doc.getLong("view_count") != null ? doc.getLong("view_count").intValue() : 0,
            likedBy,
            toMillis(doc.get("timestamp"))
        );
    }

    private CommentResponse toCommentResponse(DocumentSnapshot doc) {
        @SuppressWarnings("unchecked")
        List<String> likedBy = (List<String>) doc.get("liked_by");
        if (likedBy == null) likedBy = Collections.emptyList();

        return new CommentResponse(
            doc.getId(),
            doc.getString("user_id"),
            doc.getString("user_name"),
            doc.getString("user_avatar_url"),
            doc.getString("content"),
            toMillis(doc.get("timestamp")),
            likedBy,
            doc.getString("parent_id"),
            doc.getString("reply_to_user_name")
        );
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
                if (name != null) details.put("name", name);
                details.put("avatar", snapshot.getString("avatarUrl"));
                details.put("isVerified", Objects.equals(snapshot.getString("role"), "admin"));
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve user info from Firestore for uid={}, falling back to default.", uid, e);
        }
        return details;
    }

    private Long toMillis(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toDate().getTime();
        }
        return System.currentTimeMillis();
    }

    private static class FirebaseException extends RuntimeException {
        public FirebaseException(String message, OpenRecordException exceptionType) {
            super(message);
        }
    }

    private enum OpenRecordException {
        NOT_FOUND
    }
}
