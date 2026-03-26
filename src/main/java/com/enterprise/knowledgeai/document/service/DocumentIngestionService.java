package com.enterprise.knowledgeai.document.service;

import com.enterprise.knowledgeai.document.entity.Document;
import com.enterprise.knowledgeai.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Async("ingestionExecutor")
    public void ingest(Document document, byte[] fileBytes) {
        log.info("Starting ingestion for document: {} ({})", document.getName(), document.getId());

        // Mark as PROCESSING
        document.setStatus(Document.Status.PROCESSING);
        documentRepository.save(document);

        // Delete any existing vectors for this document before re-ingesting
        int deleted = jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'document_id' = ?",
                document.getId().toString());
        if (deleted > 0) log.info("Removed {} stale vectors for document {}", deleted, document.getId());

        try {
            // 1. Extract text with Apache Tika
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return document.getOriginalFilename();
                }
            };
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> docs = reader.get();

            // 2. Split into chunks (512 tokens, 50 overlap)
            TokenTextSplitter splitter = new TokenTextSplitter(512, 50, 5, 10000, true);
            List<org.springframework.ai.document.Document> chunks = splitter.apply(docs);

            // 3. Add tenant and document metadata to each chunk
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("tenant_id", document.getTenantId().toString());
                chunk.getMetadata().put("document_id", document.getId().toString());
                chunk.getMetadata().put("document_name", document.getName());
                chunk.getMetadata().put("file_type", document.getFileType().name());
            });

            // 4. Embed and store in PGVector
            vectorStore.add(chunks);

            // 5. Mark as READY
            document.setStatus(Document.Status.READY);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

            log.info("Ingestion complete: {} chunks stored for document {}", chunks.size(), document.getId());

        } catch (Exception e) {
            log.error("Ingestion failed for document {}: {}", document.getId(), e.getMessage(), e);
            document.setStatus(Document.Status.FAILED);
            documentRepository.save(document);
        }
    }
}
