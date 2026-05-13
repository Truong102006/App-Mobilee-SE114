package com.soulmate.backend.controller;

import com.soulmate.backend.dto.chat.*;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/secure/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public SendChatMessageResponse sendMessage(
        HttpServletRequest request,
        @Valid @RequestBody SendChatMessageRequest body
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return chatService.sendMessage(uid, body);
    }

    @GetMapping("/conversation/{otherUserId}")
    public ListConversationResponse listConversation(
        HttpServletRequest request,
        @PathVariable String otherUserId,
        @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return chatService.listConversation(uid, otherUserId, limit);
    }

    @GetMapping("/inbox")
    public ListInboxResponse listInbox(
        HttpServletRequest request,
        @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return chatService.listInbox(uid, limit);
    }

    @DeleteMapping("/conversation/{otherUserId}")
    public DeleteConversationResponse deleteConversation(
        HttpServletRequest request,
        @PathVariable String otherUserId
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return chatService.deleteConversation(uid, otherUserId);
    }
}
