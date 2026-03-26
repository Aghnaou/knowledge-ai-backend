package com.enterprise.knowledgeai.chat.repository;

import com.enterprise.knowledgeai.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    List<Message> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.conversationId = :conversationId")
    void deleteByConversationId(UUID conversationId);
}
