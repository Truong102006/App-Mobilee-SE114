package com.soulmate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulmate.backend.dto.chat.*;
import com.soulmate.backend.security.AuthContext;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.security.FirebaseSecurityFilter;
import com.soulmate.backend.service.ChatService;
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
public class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirebaseSecurityFilter firebaseSecurityFilter;

    @MockBean
    private ChatService chatService;

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
    public void testSendMessage_Success() throws Exception {
        SendChatMessageRequest body = new SendChatMessageRequest("user-456", "Hello", null, null);
        Mockito.when(chatService.sendMessage(eq("test-user-123"), any(SendChatMessageRequest.class)))
            .thenReturn(new SendChatMessageResponse("msg-123", "conv-123"));

        mockMvc.perform(post("/api/secure/chats/send")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk());

        Mockito.verify(chatService).sendMessage(eq("test-user-123"), any(SendChatMessageRequest.class));
    }

    @Test
    public void testListConversation_Success() throws Exception {
        Mockito.when(chatService.listConversation(eq("test-user-123"), eq("user-456"), eq(100)))
            .thenReturn(new ListConversationResponse("conv-123", Collections.emptyList()));

        mockMvc.perform(get("/api/secure/chats/conversation/user-456")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(chatService).listConversation(eq("test-user-123"), eq("user-456"), eq(100));
    }

    @Test
    public void testListInbox_Success() throws Exception {
        Mockito.when(chatService.listInbox(eq("test-user-123"), eq(100)))
            .thenReturn(new ListInboxResponse(Collections.emptyList()));

        mockMvc.perform(get("/api/secure/chats/inbox")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(chatService).listInbox(eq("test-user-123"), eq(100));
    }

    @Test
    public void testDeleteConversation_Success() throws Exception {
        Mockito.when(chatService.deleteConversation(eq("test-user-123"), eq("user-456")))
            .thenReturn(new DeleteConversationResponse("conv-123", 5));

        mockMvc.perform(delete("/api/secure/chats/conversation/user-456")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(chatService).deleteConversation(eq("test-user-123"), eq("user-456"));
    }

    @Test
    public void testMarkAsRead_Success() throws Exception {
        Mockito.when(chatService.markAsRead(eq("test-user-123"), eq("user-456")))
            .thenReturn(Collections.emptyMap());

        mockMvc.perform(post("/api/secure/chats/conversation/user-456/read")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(chatService).markAsRead(eq("test-user-123"), eq("user-456"));
    }

    @Test
    public void testDeleteMessage_Success() throws Exception {
        Mockito.when(chatService.deleteMessage(eq("test-user-123"), eq("msg-123")))
            .thenReturn(Collections.emptyMap());

        mockMvc.perform(delete("/api/secure/chats/messages/msg-123")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(chatService).deleteMessage(eq("test-user-123"), eq("msg-123"));
    }

    @Test
    public void testReactToMessage_Success() throws Exception {
        ReactMessageRequest body = new ReactMessageRequest("❤️");

        mockMvc.perform(post("/api/secure/chats/messages/msg-123/react")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk());

        Mockito.verify(chatService).reactToMessage(eq("test-user-123"), eq("msg-123"), eq("❤️"));
    }

    @Test
    public void testEditMessage_Success() throws Exception {
        EditMessageRequest body = new EditMessageRequest("Edited content");

        mockMvc.perform(put("/api/secure/chats/messages/msg-123/edit")
                .header("Authorization", "Bearer test-user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk());

        Mockito.verify(chatService).editMessage(eq("test-user-123"), eq("msg-123"), eq("Edited content"));
    }

    @Test
    public void testListConversationMedia_Success() throws Exception {
        Mockito.when(chatService.listConversationMedia(eq("test-user-123"), eq("user-456")))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/secure/chats/conversation/user-456/media")
                .header("Authorization", "Bearer test-user-123"))
            .andExpect(status().isOk());

        Mockito.verify(chatService).listConversationMedia(eq("test-user-123"), eq("user-456"));
    }
}
