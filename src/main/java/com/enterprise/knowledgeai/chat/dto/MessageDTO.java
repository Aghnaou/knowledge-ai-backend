package com.enterprise.knowledgeai.chat.dto;

import com.enterprise.knowledgeai.chat.entity.Message;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MessageDTO {

    private UUID id;
    private String role;
    private String content;
    private List<String> sources;
    private String feedback;
    private LocalDateTime createdAt;

    public static MessageDTO from(Message msg) {
        return MessageDTO.builder()
                .id(msg.getId())
                .role(msg.getRole().name())
                .content(msg.getContent())
                .sources(msg.getSources())
                .feedback(msg.getFeedback().name())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
