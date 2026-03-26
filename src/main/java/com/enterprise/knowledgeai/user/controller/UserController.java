package com.enterprise.knowledgeai.user.controller;

import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.common.response.ApiResponse;
import com.enterprise.knowledgeai.user.dto.InviteResponse;
import com.enterprise.knowledgeai.user.dto.UserDTO;
import com.enterprise.knowledgeai.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> listUsers(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.listUsers(user, pageable)));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InviteResponse>> inviteUser(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        InviteResponse response = userService.inviteUser(
                body.get("email"),
                body.getOrDefault("role", "EMPLOYEE"),
                body.getOrDefault("firstName", ""),
                body.getOrDefault("lastName", ""),
                user);
        return ResponseEntity.ok(ApiResponse.success("User invited successfully", response));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> changeRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        UserDTO dto = userService.changeRole(id, body.get("role"), user);
        return ResponseEntity.ok(ApiResponse.success("Role updated", dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> deactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        UserDTO dto = userService.deactivateUser(id, user);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", dto));
    }
}
