package com.enterprise.knowledgeai.document.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String originalFilename;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    private String filePath;
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    private String category;

    @Column(columnDefinition = "text[]")
    private String[] tags;

    private UUID tenantId;
    private UUID uploadedBy;

    @Builder.Default
    private int chunkCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum FileType {
        PDF, WORD, TXT, URL
    }

    public enum Status {
        PENDING, PROCESSING, READY, FAILED
    }
}
