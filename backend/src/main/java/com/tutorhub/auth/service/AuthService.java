package com.tutorhub.auth.service;

import com.tutorhub.auth.dto.AuthResponse;
import com.tutorhub.auth.dto.LoginRequest;
import com.tutorhub.auth.dto.RegisterTutorRequest;
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

        // Sinh refresh token ngẫu nhiên an toàn và lưu băm vào DB
        String rawRefreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusSeconds(refreshTokenTtlSeconds))
            .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, UserResponse.from(user));
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
