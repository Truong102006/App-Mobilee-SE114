package com.soulmate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulmate.backend.dto.community.CommentRequest;
import com.soulmate.backend.dto.community.CommonResponse;
import com.soulmate.backend.dto.community.SavePostRequest;
import com.soulmate.backend.security.AuthContext;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.security.FirebaseSecurityFilter;
import com.soulmate.backend.service.CommunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CommunityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirebaseSecurityFilter firebaseSecurityFilter;

    @MockBean
    private CommunityService communityService;

    @MockBean
    private com.google.firebase.FirebaseApp firebaseApp;

    @MockBean
    private com.google.firebase.auth.FirebaseAuth firebaseAuth;

    @MockBean
    private com.google.cloud.firestore.Firestore firestore;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest request = invocation.getArgument(0);
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);

            String authHeader = request.getHeader("Authorization");
            String uid = "test-user-123";
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                uid = authHeader.substring(7).trim();
            }

            AuthContextHolder.set(request, new AuthContext(uid, "mock-app-id", "user"));
            chain.doFilter(request, response);
            return null;
        }).when(firebaseSecurityFilter).doFilter(any(), any(), any());
    }

    @Test
    public void testCreatePost_Success() throws Exception {
        SavePostRequest request = new SavePostRequest("Happy", "Hello Community!", Collections.emptyList());

        mockMvc.perform(post("/api/secure/community/posts")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        Mockito.verify(communityService).createPost(eq("test-user-123"), any(SavePostRequest.class));
    }

    @Test
    public void testListPosts_Success() throws Exception {
        mockMvc.perform(get("/api/secure/community/posts")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.verify(communityService).listPosts();
    }

    @Test
    public void testUpdatePost_Success() throws Exception {
        SavePostRequest request = new SavePostRequest(null, "Updated content", null);

        mockMvc.perform(put("/api/secure/community/posts/post-123")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        Mockito.verify(communityService).updatePost(eq("test-user-123"), eq("post-123"), any(SavePostRequest.class));
    }

    @Test
    public void testDeletePost_Success() throws Exception {
        mockMvc.perform(delete("/api/secure/community/posts/post-123")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.verify(communityService).deletePost(eq("test-user-123"), eq("post-123"));
    }

    @Test
    public void testToggleLikePost_Success() throws Exception {
        mockMvc.perform(post("/api/secure/community/posts/post-123/like")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.verify(communityService).toggleLike(eq("test-user-123"), eq("post-123"));
    }

    @Test
    public void testAddComment_Success() throws Exception {
        CommentRequest request = new CommentRequest("Nice post!", null, null);

        mockMvc.perform(post("/api/secure/community/posts/post-123/comments")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        Mockito.verify(communityService).addComment(eq("test-user-123"), eq("post-123"), any(CommentRequest.class));
    }

    @Test
    public void testListComments_Success() throws Exception {
        mockMvc.perform(get("/api/secure/community/posts/post-123/comments")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.verify(communityService).listComments(eq("post-123"));
    }

    @Test
    public void testToggleCommentLike_Success() throws Exception {
        mockMvc.perform(post("/api/secure/community/posts/post-123/comments/comment-456/like")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.verify(communityService).toggleCommentLike(eq("test-user-123"), eq("post-123"), eq("comment-456"));
    }

    @Test
    public void testReportPost_Success() throws Exception {
        Mockito.when(communityService.reportPost(eq("test-user-123"), eq("post-123")))
            .thenReturn(new CommonResponse(true, "Reported"));

        mockMvc.perform(post("/api/secure/community/report/post-123")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.verify(communityService).reportPost(eq("test-user-123"), eq("post-123"));
    }

    @Test
    public void testGetPostAuthorProfile_Success() throws Exception {
        com.soulmate.backend.dto.user.UserProfileResponse response = new com.soulmate.backend.dto.user.UserProfileResponse(
            "test-user-123", "Anonymous Name", "http://avatar", "Bio", Collections.emptyList(), System.currentTimeMillis()
        );
        Mockito.when(communityService.getPostAuthorProfile(eq("post-123"))).thenReturn(response);

        mockMvc.perform(get("/api/secure/community/posts/post-123/author")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.verify(communityService).getPostAuthorProfile(eq("post-123"));
    }
}
