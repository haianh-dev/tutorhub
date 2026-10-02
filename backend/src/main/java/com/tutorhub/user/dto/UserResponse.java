package com.tutorhub.user.dto;

import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;

import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    String phone,
    Role role,
    UserStatus status,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt()
        );
    }
}
