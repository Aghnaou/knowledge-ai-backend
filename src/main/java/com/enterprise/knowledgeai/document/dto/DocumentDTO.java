package com.enterprise.knowledgeai.document.dto;

import com.enterprise.knowledgeai.document.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {

    private UUID id;
    private String name;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String status;
    private String category;
    private String[] tags;
    private UUID tenantId;
    private UUID uploadedBy;
    private int chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentDTO from(Document doc) {
        return DocumentDTO.builder()
                .id(doc.getId())
                .name(doc.getName())
                .originalFilename(doc.getOriginalFilename())
                .fileType(doc.getFileType() != null ? doc.getFileType().name() : null)
                .fileSize(doc.getFileSize())
                .status(doc.getStatus().name())
                .category(doc.getCategory())
                .tags(doc.getTags())
                .tenantId(doc.getTenantId())
                .uploadedBy(doc.getUploadedBy())
                .chunkCount(doc.getChunkCount())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
