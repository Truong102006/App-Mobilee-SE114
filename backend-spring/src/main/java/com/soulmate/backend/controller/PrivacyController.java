package com.soulmate.backend.controller;

import com.soulmate.backend.dto.privacy.ChangePasswordRequest;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.PrivacyService;
import com.soulmate.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/secure/privacy")
public class PrivacyController {

    private final PrivacyService privacyService;
    private final UserService userService;

    public PrivacyController(PrivacyService privacyService, UserService userService) {
        this.privacyService = privacyService;
        this.userService = userService;
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        privacyService.sendPasswordResetEmail(body.email());
    }

    @DeleteMapping("/account")
    public void deleteAccount(HttpServletRequest request) {
        String uid = AuthContextHolder.getRequired(request).uid();
        privacyService.deleteAccount(uid);
    }

    @GetMapping("/blocked-users")
    public List<String> listBlockedUsers(HttpServletRequest request) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return userService.listBlockedUsers(uid);
    }
}
