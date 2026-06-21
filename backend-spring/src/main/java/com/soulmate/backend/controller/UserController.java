package com.soulmate.backend.controller;

import com.soulmate.backend.dto.user.*;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/secure/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}/profile")
    public UserProfileResponse getUserProfile(@PathVariable String userId) {
        return userService.getUserProfile(userId);
    }

    @PutMapping("/profile")
    public void updateProfile(
        HttpServletRequest request,
        @Valid @RequestBody UpdateProfileRequest body
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        userService.updateProfile(uid, body);
    }

    @PutMapping("/profile/avatar")
    public void updateAvatar(
        HttpServletRequest request,
        @Valid @RequestBody UpdateAvatarRequest body
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        userService.updateAvatar(uid, body.avatarUrl());
    }

    @PostMapping("/block/{targetUserId}")
    public BlockUserResponse blockUser(
        HttpServletRequest request,
        @PathVariable String targetUserId
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        userService.blockUser(uid, targetUserId);
        return new BlockUserResponse(true, "Blocked successfully");
    }

    @DeleteMapping("/block/{targetUserId}")
    public BlockUserResponse unblockUser(
        HttpServletRequest request,
        @PathVariable String targetUserId
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        userService.unblockUser(uid, targetUserId);
        return new BlockUserResponse(true, "Unblocked successfully");
    }

    @GetMapping("/blocked")
    public List<String> listBlockedUsers(HttpServletRequest request) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return userService.listBlockedUsers(uid);
    }
}
