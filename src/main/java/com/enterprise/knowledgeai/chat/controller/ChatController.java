package com.enterprise.knowledgeai.chat.controller;

import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.chat.dto.ConversationDTO;
import com.enterprise.knowledgeai.chat.dto.MessageDTO;
import com.enterprise.knowledgeai.chat.service.ChatService;
import com.enterprise.knowledgeai.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ─── Conversations ───────────────────────────────────────────────────────

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationDTO>> createConversation(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User user) {

        String title = body != null ? body.get("title") : null;
        ConversationDTO dto = chatService.createConversation(title, user);
        return ResponseEntity.ok(ApiResponse.success("Conversation created", dto));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Page<ConversationDTO>>> listConversations(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<ConversationDTO> page = chatService.listConversations(user, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<ConversationDTO>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        ConversationDTO dto = chatService.getConversation(id, user);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        chatService.deleteConversation(id, user);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted", null));
    }

    // ─── SSE Streaming ───────────────────────────────────────────────────────

    @GetMapping(value = "/conversations/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable UUID id,
            @RequestParam String question,
            @AuthenticationPrincipal User user) {

        log.debug("SSE stream request for conversation {} question: {}", id, question);
        return chatService.stream(id, question, user);
    }

    // ─── Feedback ────────────────────────────────────────────────────────────

    @PostMapping("/messages/{id}/feedback")
    public ResponseEntity<ApiResponse<MessageDTO>> submitFeedback(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        String feedback = body.get("feedback"); // POSITIVE / NEGATIVE / NONE
        MessageDTO dto = chatService.submitFeedback(id, feedback, user);
        return ResponseEntity.ok(ApiResponse.success("Feedback recorded", dto));
    }
}
