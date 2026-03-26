package com.enterprise.knowledgeai.document.controller;

import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.common.response.ApiResponse;
import com.enterprise.knowledgeai.document.dto.DocumentDTO;
import com.enterprise.knowledgeai.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DocumentDTO>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal User user) {

        DocumentDTO dto = documentService.uploadDocument(file, category, user.getTenantId(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Document uploaded, ingestion started", dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentDTO>>> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<DocumentDTO> page = documentService.getDocuments(user.getTenantId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDTO>> getOne(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        DocumentDTO dto = documentService.getDocument(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DocumentDTO>> status(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        DocumentDTO dto = documentService.getDocumentStatus(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}/category")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DocumentDTO>> updateCategory(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {

        String category = (String) body.get("category");
        String[] tags = body.containsKey("tags")
                ? ((java.util.List<?>) body.get("tags")).stream().map(Object::toString).toArray(String[]::new)
                : null;

        DocumentDTO dto = documentService.updateCategory(id, category, tags, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Category updated", dto));
    }

    @PostMapping("/{id}/reprocess")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DocumentDTO>> reprocess(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        DocumentDTO dto = documentService.reprocessDocument(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Document reprocessing started", dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        documentService.deleteDocument(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }
}
