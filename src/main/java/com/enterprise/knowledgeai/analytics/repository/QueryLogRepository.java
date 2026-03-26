package com.enterprise.knowledgeai.analytics.repository;

import com.enterprise.knowledgeai.analytics.entity.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface QueryLogRepository extends JpaRepository<QueryLog, UUID> {

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndCreatedAtAfter(UUID tenantId, LocalDateTime since);

    // Questions per day: returns [date_string, count]
    @Query(value = """
            SELECT TO_CHAR(created_at, 'YYYY-MM-DD') AS day, COUNT(*) AS cnt
            FROM query_logs
            WHERE tenant_id = :tenantId AND created_at >= :since
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> questionsPerDay(@Param("tenantId") UUID tenantId,
                                   @Param("since") LocalDateTime since);

    // Top referenced documents: returns [doc_name, count]
    @Query(value = """
            SELECT doc_name, COUNT(*) AS cnt
            FROM query_logs, UNNEST(documents_referenced) AS doc_name
            WHERE tenant_id = :tenantId
            GROUP BY doc_name
            ORDER BY cnt DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> topReferencedDocuments(@Param("tenantId") UUID tenantId);

    // Per-user question counts: returns [user_id, count]
    @Query(value = """
            SELECT user_id, COUNT(*) AS cnt
            FROM query_logs
            WHERE tenant_id = :tenantId
            GROUP BY user_id
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> questionsPerUser(@Param("tenantId") UUID tenantId);

    // Average response time
    @Query(value = """
            SELECT AVG(response_time_ms)
            FROM query_logs
            WHERE tenant_id = :tenantId
            """, nativeQuery = true)
    Double avgResponseTimeMs(@Param("tenantId") UUID tenantId);

    List<QueryLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
