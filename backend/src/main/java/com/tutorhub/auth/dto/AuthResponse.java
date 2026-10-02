package com.tutorhub.auth.dto;

import com.tutorhub.user.dto.UserResponse;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserResponse user
) {}
