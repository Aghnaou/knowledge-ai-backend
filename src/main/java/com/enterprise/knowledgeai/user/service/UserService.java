package com.enterprise.knowledgeai.user.service;

import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.auth.repository.UserRepository;
import com.enterprise.knowledgeai.user.dto.InviteResponse;
import com.enterprise.knowledgeai.user.dto.UserDTO;
import com.enterprise.knowledgeai.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public Page<UserDTO> listUsers(User currentUser, Pageable pageable) {
        List<UserDTO> users = userRepository.findByTenantId(currentUser.getTenantId())
                .stream().map(UserDTO::from).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), users.size());
        return new PageImpl<>(users.subList(start, end), pageable, users.size());
    }

    public UserDTO changeRole(UUID userId, String role, User currentUser) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (!target.getTenantId().equals(currentUser.getTenantId())) {
            throw new RuntimeException("Access denied");
        }
        target.setRole(Role.valueOf(role.toUpperCase()));
        return UserDTO.from(userRepository.save(target));
    }

    public UserDTO deactivateUser(UUID userId, User currentUser) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (!target.getTenantId().equals(currentUser.getTenantId())) {
            throw new RuntimeException("Access denied");
        }
        target.setActive(false);
        return UserDTO.from(userRepository.save(target));
    }

    public InviteResponse inviteUser(String email, String role, String firstName, String lastName, User currentUser) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }

        String tempPassword = generateTempPassword(10);

        User newUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(tempPassword))
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.valueOf(role.toUpperCase()))
                .tenantId(currentUser.getTenantId())
                .active(true)
                .build();

        UserDTO dto = UserDTO.from(userRepository.save(newUser));
        return InviteResponse.builder()
                .user(dto)
                .temporaryPassword(tempPassword)
                .build();
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
