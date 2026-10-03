package com.tutorhub.auth.service;

import com.tutorhub.auth.dto.PasswordResetLinkResponse;
import com.tutorhub.auth.dto.ResetPasswordRequest;
import com.tutorhub.auth.entity.PasswordResetToken;
import com.tutorhub.auth.repository.PasswordResetOwnershipRepository;
import com.tutorhub.auth.repository.PasswordResetTokenRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetOwnershipRepository ownershipRepository;
    private final com.tutorhub.auth.repository.RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.password-reset-ttl-seconds:1800}")
    private long passwordResetTtlSeconds;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public PasswordResetLinkResponse createResetLink(Long actorId, Role actorRole, Long targetUserId) {
        if (actorRole != Role.ADMIN && actorRole != Role.TUTOR) {
            throw new AppException(ErrorCode.AUTH_ACCESS_DENIED, "Bạn không có quyền tạo link đặt lại mật khẩu");
        }

        User target = userRepository.findByIdForUpdate(targetUserId)
            .orElseThrow(() -> notFound());
        if (actorRole == Role.TUTOR) {
            boolean allowedRole = target.getRole() == Role.STUDENT || target.getRole() == Role.PARENT;
            if (!allowedRole || !ownershipRepository.tutorOwnsResetTarget(actorId, targetUserId, target.getRole())) {
                throw notFound();
            }
        }

        Instant now = Instant.now();
        passwordResetTokenRepository.invalidateUnusedByUserId(targetUserId, now);
        String rawToken = generateRawToken();
        Instant expiresAt = now.plusSeconds(passwordResetTtlSeconds);
        passwordResetTokenRepository.save(PasswordResetToken.builder()
            .user(target)
            .tokenHash(AuthService.hashToken(rawToken))
            .expiresAt(expiresAt)
            .build());

        String baseUrl = frontendBaseUrl.replaceAll("/+$", "");
        String link = UriComponentsBuilder.fromUriString(baseUrl)
            .path("/reset-password")
            .queryParam("token", rawToken)
            .build()
            .encode()
            .toUriString();
        return new PasswordResetLinkResponse(link, expiresAt);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = AuthService.hashToken(request.token());
        Long userId = passwordResetTokenRepository.findUserIdByTokenHash(tokenHash)
            .orElseThrow(this::invalidToken);
        User user = userRepository.findByIdForUpdate(userId)
            .orElseThrow(this::invalidToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(this::invalidToken);

        Instant now = Instant.now();
        if (!token.isUsableAt(now)) {
            throw invalidToken();
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        token.setUsedAt(now);
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.invalidateUnusedByUserId(userId, now);
        refreshTokenRepository.revokeActiveByUserId(userId, now);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AppException invalidToken() {
        return new AppException(ErrorCode.PASSWORD_RESET_INVALID, "Link đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng");
    }

    private AppException notFound() {
        return new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng trong phạm vi quản lý");
    }
}