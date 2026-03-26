package com.enterprise.knowledgeai.document.service;

import com.enterprise.knowledgeai.document.dto.DocumentDTO;
import com.enterprise.knowledgeai.document.entity.Document;
import com.enterprise.knowledgeai.document.repository.DocumentRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MinioClient minioClient;
    private final DocumentIngestionService ingestionService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public DocumentDTO uploadDocument(MultipartFile file, String category, UUID tenantId, UUID userId) {
        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            Document.FileType fileType = detectFileType(originalFilename);

            // 1. Save metadata to DB (status = PENDING)
            Document document = Document.builder()
                    .name(stripExtension(originalFilename))
                    .originalFilename(originalFilename)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .status(Document.Status.PENDING)
                    .category(category)
                    .tenantId(tenantId)
                    .uploadedBy(userId)
                    .build();
            document = documentRepository.save(document);

            // 2. Store file in MinIO: {tenantId}/{documentId}/{filename}
            String minioPath = tenantId + "/" + document.getId() + "/" + originalFilename;
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(minioPath)
                    .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                    .contentType(file.getContentType())
                    .build());

            document.setFilePath(minioPath);
            document = documentRepository.save(document);

            // 3. Trigger async ingestion
            ingestionService.ingest(document, fileBytes);

            log.info("Document uploaded: {} by user {}", originalFilename, userId);
            return DocumentDTO.from(document);

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    public Page<DocumentDTO> getDocuments(UUID tenantId, Pageable pageable) {
        return documentRepository.findByTenantId(tenantId, pageable)
                .map(DocumentDTO::from);
    }

    public DocumentDTO getDocument(UUID id, UUID tenantId) {
        Document doc = findDocumentForTenant(id, tenantId);
        return DocumentDTO.from(doc);
    }

    public DocumentDTO getDocumentStatus(UUID id, UUID tenantId) {
        return getDocument(id, tenantId);
    }

    public DocumentDTO updateCategory(UUID id, String category, String[] tags, UUID tenantId) {
        Document doc = findDocumentForTenant(id, tenantId);
        doc.setCategory(category);
        if (tags != null) doc.setTags(tags);
        return DocumentDTO.from(documentRepository.save(doc));
    }

    public DocumentDTO reprocessDocument(UUID id, UUID tenantId) {
        Document doc = findDocumentForTenant(id, tenantId);
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(doc.getFilePath())
                    .build());
            byte[] fileBytes = stream.readAllBytes();
            ingestionService.ingest(doc, fileBytes);
            log.info("Reprocessing triggered for document {}", id);
            return DocumentDTO.from(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reprocess document: " + e.getMessage(), e);
        }
    }

    public void deleteDocument(UUID id, UUID tenantId) {
        Document doc = findDocumentForTenant(id, tenantId);
        try {
            if (doc.getFilePath() != null) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(doc.getFilePath())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Could not remove file from MinIO for document {}: {}", id, e.getMessage());
        }
        documentRepository.delete(doc);
        log.info("Document {} deleted", id);
    }

    private Document findDocumentForTenant(UUID id, UUID tenantId) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        if (!doc.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied to document: " + id);
        }
        return doc;
    }

    private Document.FileType detectFileType(String filename) {
        if (filename == null) return Document.FileType.TXT;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return Document.FileType.PDF;
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return Document.FileType.WORD;
        return Document.FileType.TXT;
    }

    private String stripExtension(String filename) {
        if (filename == null) return "Untitled";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
