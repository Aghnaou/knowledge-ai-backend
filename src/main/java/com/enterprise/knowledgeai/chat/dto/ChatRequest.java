package com.enterprise.knowledgeai.chat.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ChatRequest {
    private UUID conversationId; // null = create new conversation
    private String question;
}
