package com.enterprise.knowledgeai.chat.repository;

import com.enterprise.knowledgeai.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Page<Conversation> findByUserIdAndTenantId(UUID userId, UUID tenantId, Pageable pageable);
}
