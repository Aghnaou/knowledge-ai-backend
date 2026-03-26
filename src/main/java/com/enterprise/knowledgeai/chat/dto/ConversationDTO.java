package com.enterprise.knowledgeai.chat.dto;

import com.enterprise.knowledgeai.chat.entity.Conversation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConversationDTO {

    private UUID id;
    private String title;
    private UUID tenantId;
    private List<MessageDTO> messages; // null on list view, populated on detail view
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConversationDTO from(Conversation c) {
        return ConversationDTO.builder()
                .id(c.getId())
                .title(c.getTitle())
                .tenantId(c.getTenantId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public static ConversationDTO from(Conversation c, List<MessageDTO> messages) {
        return ConversationDTO.builder()
                .id(c.getId())
                .title(c.getTitle())
                .tenantId(c.getTenantId())
                .messages(messages)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
