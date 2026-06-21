package com.soulmate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulmate.backend.dto.privacy.ChangePasswordRequest;
import com.soulmate.backend.security.AuthContext;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.security.FirebaseSecurityFilter;
import com.soulmate.backend.service.PrivacyService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PrivacyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirebaseSecurityFilter firebaseSecurityFilter;

    @MockBean
    private PrivacyService privacyService;

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
    public void testChangePassword_Success() throws Exception {
        ChangePasswordRequest body = new ChangePasswordRequest("user@example.com");

        mockMvc.perform(post("/api/secure/privacy/change-password")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk());

        Mockito.verify(privacyService).sendPasswordResetEmail(eq("user@example.com"));
    }

    @Test
    public void testDeleteAccount_Success() throws Exception {
        mockMvc.perform(delete("/api/secure/privacy/account")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(privacyService).deleteAccount(eq("test-user-123"));
    }

    @Test
    public void testListBlockedUsers_Success() throws Exception {
        Mockito.when(userService.listBlockedUsers(eq("test-user-123")))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/secure/privacy/blocked-users")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(userService).listBlockedUsers(eq("test-user-123"));
    }
}
