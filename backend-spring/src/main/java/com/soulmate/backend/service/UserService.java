package com.soulmate.backend.service;

import com.google.cloud.firestore.Firestore;
import com.soulmate.backend.dto.community.CommonResponse;
import org.springframework.stereotype.Service;

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
}