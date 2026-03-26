package com.enterprise.knowledgeai.auth.controller;

import com.enterprise.knowledgeai.auth.dto.AcceptInviteRequest;
import com.enterprise.knowledgeai.auth.dto.AuthResponse;
import com.enterprise.knowledgeai.auth.dto.LoginRequest;
import com.enterprise.knowledgeai.auth.dto.RegisterRequest;
import com.enterprise.knowledgeai.auth.service.AuthService;
import com.enterprise.knowledgeai.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        AuthResponse response = authService.refresh(body.get("refreshToken"));
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<ApiResponse<AuthResponse>> acceptInvite(@RequestBody AcceptInviteRequest request) {
        AuthResponse response = authService.acceptInvite(request);
        return ResponseEntity.ok(ApiResponse.success("Password set. Welcome aboard!", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Stateless JWT — client discards the token
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
