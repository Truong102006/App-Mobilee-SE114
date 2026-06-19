package com.soulmate.backend.controller;

import com.soulmate.backend.dto.community.CommonResponse;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.CommunityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secure/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping("/report/{postId}")
    public CommonResponse reportPost(HttpServletRequest request, @PathVariable java.lang.String postId) {
        java.lang.String uid = AuthContextHolder.getRequired(request).uid();
        return communityService.reportPost(uid, postId);
    }
}
