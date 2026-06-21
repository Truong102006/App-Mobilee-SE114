package com.soulmate.backend.controller;

import com.soulmate.backend.dto.community.CommentRequest;
import com.soulmate.backend.dto.community.CommentResponse;
import com.soulmate.backend.dto.community.CommonResponse;
import com.soulmate.backend.dto.community.PostResponse;
import com.soulmate.backend.dto.community.SavePostRequest;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.CommunityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/secure/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping("/posts")
    public PostResponse createPost(
        HttpServletRequest request,
        @Valid @RequestBody SavePostRequest body
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return communityService.createPost(uid, body);
    }

    @GetMapping("/posts")
    public List<PostResponse> listPosts() {
        return communityService.listPosts();
    }

    @PutMapping("/posts/{id}")
    public PostResponse updatePost(
        HttpServletRequest request,
        @PathVariable String id,
        @Valid @RequestBody SavePostRequest body
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return communityService.updatePost(uid, id, body);
    }

    @DeleteMapping("/posts/{id}")
    public void deletePost(
        HttpServletRequest request,
        @PathVariable String id
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        communityService.deletePost(uid, id);
    }

    @PostMapping("/posts/{id}/like")
    public void toggleLike(
        HttpServletRequest request,
        @PathVariable String id
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        communityService.toggleLike(uid, id);
    }

    @PostMapping("/posts/{id}/comments")
    public CommentResponse addComment(
        HttpServletRequest request,
        @PathVariable String id,
        @Valid @RequestBody CommentRequest body
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return communityService.addComment(uid, id, body);
    }

    @PostMapping("/posts/{id}/comments/{commentId}/like")
    public void toggleCommentLike(
        HttpServletRequest request,
        @PathVariable String id,
        @PathVariable String commentId
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        communityService.toggleCommentLike(uid, id, commentId);
    }

    @GetMapping("/posts/{id}/comments")
    public List<CommentResponse> listComments(@PathVariable String id) {
        return communityService.listComments(id);
    }

    @PostMapping("/report/{postId}")
    public CommonResponse reportPost(HttpServletRequest request, @PathVariable String postId) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return communityService.reportPost(uid, postId);
    }

    @GetMapping("/posts/{id}/author")
    public com.soulmate.backend.dto.user.UserProfileResponse getPostAuthorProfile(@PathVariable String id) {
        return communityService.getPostAuthorProfile(id);
    }
}
