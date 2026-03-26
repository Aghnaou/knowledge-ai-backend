package com.enterprise.knowledgeai.analytics.service;

import com.enterprise.knowledgeai.analytics.repository.QueryLogRepository;
import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.auth.repository.UserRepository;
import com.enterprise.knowledgeai.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final QueryLogRepository queryLogRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    /**
     * Overview: totals for the tenant dashboard.
     */
    public Map<String, Object> getOverview(User user) {
        UUID tenantId = user.getTenantId();
        long totalQuestions = queryLogRepository.countByTenantId(tenantId);
        long questionsToday = queryLogRepository.countByTenantIdAndCreatedAtAfter(
                tenantId, LocalDateTime.now().toLocalDate().atStartOfDay());
        long totalDocuments = documentRepository.countByTenantId(tenantId);
        long totalUsers = userRepository.countByTenantId(tenantId);
        Double avgResponseMs = queryLogRepository.avgResponseTimeMs(tenantId);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalQuestions", totalQuestions);
        overview.put("questionsToday", questionsToday);
        overview.put("totalDocuments", totalDocuments);
        overview.put("totalUsers", totalUsers);
        overview.put("avgResponseTimeMs", avgResponseMs != null ? Math.round(avgResponseMs) : 0);
        return overview;
    }

    /**
     * Questions per day for the last 30 days — for chart rendering.
     */
    public List<Map<String, Object>> getQuestionsPerDay(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = queryLogRepository.questionsPerDay(user.getTenantId(), since);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0]);
            entry.put("count", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Top 10 most referenced documents.
     */
    public List<Map<String, Object>> getTopDocuments(User user) {
        List<Object[]> rows = queryLogRepository.topReferencedDocuments(user.getTenantId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("documentName", row[0]);
            entry.put("count", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Per-user activity (questions asked).
     */
    public List<Map<String, Object>> getUserActivity(User user) {
        List<Object[]> rows = queryLogRepository.questionsPerUser(user.getTenantId());
        List<com.enterprise.knowledgeai.auth.entity.User> users =
                userRepository.findByTenantId(user.getTenantId());
        Map<UUID, String> userNames = new HashMap<>();
        for (com.enterprise.knowledgeai.auth.entity.User u : users) {
            userNames.put(u.getId(), u.getFirstName() + " " + u.getLastName());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID userId = UUID.fromString(row[0].toString());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userId", userId);
            entry.put("name", userNames.getOrDefault(userId, "Unknown"));
            entry.put("questionsCount", ((Number) row[1]).longValue());
            result.add(entry);
        }
        return result;
    }
}
