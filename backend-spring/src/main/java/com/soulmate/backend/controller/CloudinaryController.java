package com.soulmate.backend.controller;

import com.soulmate.backend.dto.cloudinary.SignUploadRequest;
import com.soulmate.backend.dto.cloudinary.SignUploadResponse;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secure/cloudinary")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    public CloudinaryController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/sign-upload")
    public SignUploadResponse signUpload(
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody(required = false) SignUploadRequest request
    ) {
        SignUploadRequest safeRequest = request == null ? new SignUploadRequest(null, null) : request;
        String uid = AuthContextHolder.getRequired(httpServletRequest).uid();
        return cloudinaryService.signUpload(uid, safeRequest);
    }
}
