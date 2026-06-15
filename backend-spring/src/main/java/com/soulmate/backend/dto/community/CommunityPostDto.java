package com.soulmate.backend.dto.community;

import java.util.List;
import com.google.cloud.firestore.annotation.DocumentId;

public class CommunityPostDto {
    @DocumentId
    public String id;
    public String userId;
    public String userName;
    public String userAvatarUrl;
    public String textContent;
    public int reportCount;
    public List<String> reportedBy;
    public Long timestamp;

    public CommunityPostDto() {}
}