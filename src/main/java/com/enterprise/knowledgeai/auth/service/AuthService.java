package com.enterprise.knowledgeai.auth.service;

import com.enterprise.knowledgeai.auth.dto.AcceptInviteRequest;
import com.enterprise.knowledgeai.auth.dto.AuthResponse;
import com.enterprise.knowledgeai.auth.dto.LoginRequest;
import com.enterprise.knowledgeai.auth.dto.RegisterRequest;
import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.auth.repository.UserRepository;
import com.enterprise.knowledgeai.common.util.JwtUtil;
import com.enterprise.knowledgeai.tenant.entity.Tenant;
import com.enterprise.knowledgeai.tenant.service.TenantService;
import com.enterprise.knowledgeai.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Tenant tenant = tenantService.createTenant(request.getCompanyName());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.ADMIN)
                .tenantId(tenant.getId())
                .build();

        user = userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return buildAuthResponse(user, tenant, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = tenantService.findById(user.getTenantId());

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return buildAuthResponse(user, tenant, accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        String email = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtUtil.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        Tenant tenant = tenantService.findById(user.getTenantId());

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        return buildAuthResponse(user, tenant, newAccessToken, newRefreshToken);
    }

    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email"));

        if (!passwordEncoder.matches(request.getTemporaryPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid temporary password");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user = userRepository.save(user);

        Tenant tenant = tenantService.findById(user.getTenantId());

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return buildAuthResponse(user, tenant, accessToken, refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, Tenant tenant, String accessToken, String refreshToken) {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .tenantId(user.getTenantId())
                .tenantName(tenant.getName())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(86400000L)
                .user(userInfo)
                .build();
    }
}
