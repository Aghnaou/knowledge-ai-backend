package com.enterprise.knowledgeai.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "query_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private UUID tenantId;

    @Column(columnDefinition = "TEXT")
    private String question;

    private Integer answerLength;

    @Column(columnDefinition = "text[]")
    private String[] documentsReferenced;

    private Long responseTimeMs;
    private Integer tokensUsed;
    private Boolean wasHelpful;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
