package com.soulmate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulmate.backend.dto.user.*;
import com.soulmate.backend.security.AuthContext;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.security.FirebaseSecurityFilter;
import com.soulmate.backend.service.UserService;
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
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirebaseSecurityFilter firebaseSecurityFilter;

    @MockBean
    private UserService userService;

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
    public void testGetUserProfile_Success() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
            "user-123", "Anonymous Name", "http://avatarUrl", "Bio", Collections.emptyList(), System.currentTimeMillis()
        );
        Mockito.when(userService.getUserProfile(eq("user-123"))).thenReturn(response);

        mockMvc.perform(get("/api/secure/users/user-123/profile")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(userService).getUserProfile(eq("user-123"));
    }

    @Test
    public void testUpdateProfile_Success() throws Exception {
        UpdateProfileRequest body = new UpdateProfileRequest("New Name", "New Bio", Collections.emptyList());

        mockMvc.perform(put("/api/secure/users/profile")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk());

        Mockito.verify(userService).updateProfile(eq("test-user-123"), any(UpdateProfileRequest.class));
    }

    @Test
    public void testUpdateAvatar_Success() throws Exception {
        UpdateAvatarRequest body = new UpdateAvatarRequest("http://newAvatarUrl");

        mockMvc.perform(put("/api/secure/users/profile/avatar")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk());

        Mockito.verify(userService).updateAvatar(eq("test-user-123"), eq("http://newAvatarUrl"));
    }

    @Test
    public void testBlockUser_Success() throws Exception {
        mockMvc.perform(post("/api/secure/users/block/user-456")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(userService).blockUser(eq("test-user-123"), eq("user-456"));
    }

    @Test
    public void testUnblockUser_Success() throws Exception {
        mockMvc.perform(delete("/api/secure/users/block/user-456")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(userService).unblockUser(eq("test-user-123"), eq("user-456"));
    }

    @Test
    public void testListBlockedUsers_Success() throws Exception {
        Mockito.when(userService.listBlockedUsers(eq("test-user-123")))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/secure/users/blocked")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(userService).listBlockedUsers(eq("test-user-123"));
    }
}
