package com.tutorhub.auth.dto;

import java.time.Instant;

public record PasswordResetLinkResponse(String link, Instant expiresAt) {}