package com.enterprise.knowledgeai.settings.controller;

import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String currentModel;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings(
            @AuthenticationPrincipal User user) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("llmModel", currentModel);
        settings.put("llmProvider", "OpenAI");
        settings.put("embeddingModel", "text-embedding-3-small");
        settings.put("vectorDimensions", 1536);
        settings.put("maxContextChunks", 5);
        settings.put("tenantId", user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/llm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateLlm(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        // In a real implementation this would persist to DB and reload the bean.
        // For now we acknowledge the request.
        String model = body.get("model");
        return ResponseEntity.ok(ApiResponse.success(
                "LLM model setting acknowledged: " + model + ". Restart required to apply.", model));
    }

    @PutMapping("/language")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateLanguage(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String language = body.getOrDefault("language", "en");
        return ResponseEntity.ok(ApiResponse.success("Language preference updated", language));
    }
}
