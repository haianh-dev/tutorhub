package com.tutorhub.auth.dto;

import com.tutorhub.user.dto.UserResponse;

public record RegisterResponse(
    UserResponse user
) {}
