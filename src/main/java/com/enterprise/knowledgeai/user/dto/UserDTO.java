package com.enterprise.knowledgeai.user.dto;

import com.enterprise.knowledgeai.auth.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserDTO {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private UUID tenantId;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserDTO from(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .tenantId(user.getTenantId())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
