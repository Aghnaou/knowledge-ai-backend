package com.enterprise.knowledgeai.auth.dto;

import lombok.Data;

@Data
public class AcceptInviteRequest {
    private String email;
    private String temporaryPassword;
    private String newPassword;
}
