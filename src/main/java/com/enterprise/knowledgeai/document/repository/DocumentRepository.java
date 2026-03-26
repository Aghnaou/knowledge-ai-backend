package com.enterprise.knowledgeai.document.repository;

import com.enterprise.knowledgeai.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByTenantId(UUID tenantId, Pageable pageable);
    List<Document> findByTenantId(UUID tenantId);
    long countByTenantId(UUID tenantId);
}
