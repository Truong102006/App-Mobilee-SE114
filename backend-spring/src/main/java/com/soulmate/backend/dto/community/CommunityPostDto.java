package com.soulmate.backend.dto.community;

import java.util.List;

public class CommunityPostDto {
    public java.lang.String id;
    public java.lang.String userId;
    public java.lang.String userName;
    public java.lang.String userAvatarUrl;
    public java.lang.String mood;
    public java.lang.String textContent;
    public java.util.List<java.lang.String> imageUrls;
    public int likeCount;
    public int commentCount;
    public int reportCount;
    public java.util.List<java.lang.String> reportedBy;
    public java.lang.Long timestamp;
}