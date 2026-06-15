package com.soulmate.backend.controller;

import com.soulmate.backend.dto.community.CommonResponse;
import com.soulmate.backend.dto.community.CommunityPostDto;
import com.soulmate.backend.dto.community.ResolveReportRequest;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.CommunityService;
import com.soulmate.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
//@RequestMapping("/api/secure/admin")
@RequestMapping("/api/admin")
public class AdminController {

    private final CommunityService communityService;
    private final UserService userService;

    public AdminController(CommunityService communityService, UserService userService) {
        this.communityService = communityService;
        this.userService = userService;
    }

    @GetMapping("/community/reported-posts")
    public java.util.List<CommunityPostDto> getReportedPosts(HttpServletRequest request) {
//        validateAdmin(request);
        return communityService.getReportedPosts();
    }

    @PostMapping("/community/resolve-report")
    public CommonResponse resolveReport(HttpServletRequest request, @RequestBody ResolveReportRequest body) {
//        validateAdmin(request);
        return communityService.resolveReport(body.postId(), body.action());
    }

    @PostMapping("/users/social-ban")
    public CommonResponse toggleBan(
//            HttpServletRequest request,
            @RequestParam String targetUserId,
            @RequestParam boolean isBanned
    ) {
//        validateAdmin(request);
        return userService.toggleSocialBan(targetUserId, isBanned);
    }

    private void validateAdmin(HttpServletRequest request) {
//        var auth = AuthContextHolder.getRequired(request);
//        if (!"admin".equalsIgnoreCase(auth.role())) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền Admin");
//        }
        System.out.println(">>> Admin API được gọi - Đã bỏ qua bảo mật để test");
    }
}