package com.enterprise.knowledgeai.auth.repository;

import com.enterprise.knowledgeai.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByTenantId(UUID tenantId);
    List<User> findByTenantId(UUID tenantId);
}
