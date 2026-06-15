package com.soulmate.backend.service;

import com.google.cloud.firestore.*;
import com.soulmate.backend.dto.community.CommonResponse;
import com.soulmate.backend.dto.community.CommunityPostDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommunityService {
    private final Firestore firestore;

    public CommunityService(Firestore firestore) {
        this.firestore = firestore;
    }

    public CommonResponse reportPost(java.lang.String userId, java.lang.String postId) {
        try {
            com.google.firebase.firestore.DocumentReference postRef = firestore.collection("community_posts").document(postId);
            com.google.firebase.firestore.DocumentReference userRef = firestore.collection("users").document(userId);

            firestore.runTransaction(transaction -> {
                com.google.firebase.firestore.DocumentSnapshot postSnap = transaction.get(postRef).get();
                List<String> reportedBy = (List<String>) postSnap.get("reportedBy");
                if (reportedBy == null) reportedBy = new ArrayList<>();

                if (!reportedBy.contains(userId)) {
                    reportedBy.add(userId);
                    transaction.update(postRef, "reportedBy", reportedBy);
                    transaction.update(postRef, "reportCount", com.google.firebase.firestore.FieldValue.increment(1));
                    transaction.update(userRef, "hiddenPostIds", com.google.firebase.firestore.FieldValue.arrayUnion(postId));
                }
                return null;
            }).get();

            return new CommonResponse(true, "Đã báo cáo bài viết");
        } catch (java.lang.Exception e) {
            return new CommonResponse(false, e.getMessage());
        }
    }

    public java.util.List<CommunityPostDto> getReportedPosts() {
        try {
            return firestore.collection("community_posts")
                    .whereGreaterThan("reportCount", 0)
                    .orderBy("reportCount", androidx.contentpager.content.Query.Direction.DESCENDING)
                    .get().get()
                    .toObjects(CommunityPostDto.class);
        } catch (java.lang.Exception e) {
            return new ArrayList<>();
        }
    }

    public CommonResponse resolveReport(java.lang.String postId, java.lang.String action) {
        try {
            com.google.firebase.firestore.DocumentReference postRef = firestore.collection("community_posts").document(postId);
            if ("delete".equalsIgnoreCase(action)) {
                postRef.delete().get();
            } else {
                postRef.update("reportCount", 0, "reportedBy", new ArrayList<>()).get();
            }
            return new CommonResponse(true, "Thực hiện thành công");
        } catch (java.lang.Exception e) {
            return new CommonResponse(false, e.getMessage());
        }
    }
}