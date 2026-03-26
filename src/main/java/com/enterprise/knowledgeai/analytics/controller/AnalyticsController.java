package com.enterprise.knowledgeai.analytics.controller;

import com.enterprise.knowledgeai.analytics.service.AnalyticsService;
import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOverview(user)));
    }

    @GetMapping("/questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> questionsPerDay(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getQuestionsPerDay(user)));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> topDocuments(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getTopDocuments(user)));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> userActivity(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getUserActivity(user)));
    }
}
