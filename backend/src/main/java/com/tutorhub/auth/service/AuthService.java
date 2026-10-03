package com.tutorhub.auth.service;

import com.tutorhub.auth.dto.AuthResponse;
import com.tutorhub.auth.dto.ChangePasswordRequest;
import com.tutorhub.auth.dto.LoginRequest;
import com.tutorhub.auth.dto.RegisterTutorRequest;
import com.tutorhub.auth.dto.RefreshRequest;
import com.tutorhub.auth.dto.TokenPairResponse;
import com.tutorhub.auth.entity.RefreshToken;
import com.tutorhub.auth.repository.RefreshTokenRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.user.dto.UserResponse;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-ttl-seconds:604800}")
    private long refreshTokenTtlSeconds;

    /**
     * Đăng ký tài khoản cho gia sư mới.
     * Mặc định role = TUTOR, status = ACTIVE.
     * Mật khẩu được băm bằng BCrypt.
     */
    @Transactional
    public UserResponse registerTutor(RegisterTutorRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Email đã được sử dụng");
        }

        User user = User.builder()
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName().trim())
            .phone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : null)
            .role(Role.TUTOR)
            .status(UserStatus.ACTIVE)
            .build();

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    /**
     * Xác thực thông tin đăng nhập và sinh cặp token (accessToken + refreshToken).
     * RefreshToken được lưu băm SHA-256 vào database.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.AUTH_ACCESS_DENIED, "Tài khoản của bạn đã bị vô hiệu hóa");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = createRefreshToken(user, Instant.now());

        return new AuthResponse(accessToken, rawRefreshToken, UserResponse.from(user));
    }

    @Transactional
    public TokenPairResponse refresh(RefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        Long userId = refreshTokenRepository.findUserIdByTokenHash(tokenHash)
            .orElseThrow(this::invalidRefreshToken);

        User user = userRepository.findByIdForUpdate(userId)
            .orElseThrow(this::invalidRefreshToken);
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(this::invalidRefreshToken);
        if (!current.isValid()) {
            throw invalidRefreshToken();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.AUTH_ACCESS_DENIED, "Tài khoản của bạn đã bị vô hiệu hóa");
        }

        current.setRevokedAt(Instant.now());
        refreshTokenRepository.save(current);
        Instant now = Instant.now();
        return new TokenPairResponse(jwtService.generateAccessToken(user), createRefreshToken(user, now));
    }

    @Transactional
    public void logout(Long authenticatedUserId, String rawRefreshToken) {
        userRepository.findByIdForUpdate(authenticatedUserId)
            .orElseThrow(() -> new AppException(ErrorCode.AUTH_ACCESS_DENIED, "Tài khoản không hợp lệ"));
        RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawRefreshToken))
            .orElse(null);
        if (token == null) {
            return;
        }
        if (!token.getUser().getId().equals(authenticatedUserId)) {
            throw new AppException(ErrorCode.AUTH_ACCESS_DENIED, "Refresh token không thuộc tài khoản hiện tại");
        }
        if (!token.isRevoked()) {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Mật khẩu hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeActiveByUserId(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));
        return UserResponse.from(user);
    }

    private String createRefreshToken(User user, Instant now) {
        String rawToken = UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");
        refreshTokenRepository.save(RefreshToken.builder()
            .user(user)
            .tokenHash(hashToken(rawToken))
            .expiresAt(now.plusSeconds(refreshTokenTtlSeconds))
            .build());
        return rawToken;
    }

    private AppException invalidRefreshToken() {
        return new AppException(ErrorCode.AUTH_TOKEN_INVALID, "Refresh token không hợp lệ hoặc đã hết hạn");
    }

    /**
     * Hàm băm SHA-256 cho refresh token trước khi lưu vào DB.
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán băm SHA-256 không khả dụng", e);
        }
    }
}
