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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
        ReflectionTestUtils.setField(authService, "refreshTokenTtlSeconds", 604800L);
    }

    @Nested
    @DisplayName("registerTutor()")
    class RegisterTutorTests {

        @Test
        @DisplayName("Đăng ký thành công: mã hóa BCrypt, role=TUTOR, status=ACTIVE")
        void shouldRegisterTutorSuccessfully() {
            RegisterTutorRequest request = new RegisterTutorRequest(
                "tutor@example.com",
                "Password123@",
                "Nguyễn Văn Gia Sư",
                "0901234567"
            );

            when(userRepository.existsByEmail("tutor@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123@")).thenReturn("$2a$10$encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L);
                u.setCreatedAt(Instant.now());
                u.setUpdatedAt(Instant.now());
                return u;
            });

            UserResponse response = authService.registerTutor(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("tutor@example.com");
            assertThat(response.fullName()).isEqualTo("Nguyễn Văn Gia Sư");
            assertThat(response.role()).isEqualTo(Role.TUTOR);
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$encodedPassword");
        }

        @Test
        @DisplayName("Đăng ký thất bại khi email đã tồn tại -> ném AppException DUPLICATE_RESOURCE (409)")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            RegisterTutorRequest request = new RegisterTutorRequest(
                "existing@example.com",
                "Password123@",
                "Người Dùng Mới",
                null
            );

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerTutor(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
                    assertThat(appEx.getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                });

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập thành công: trả accessToken, raw refreshToken và lưu băm vào DB")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest("tutor@example.com", "CorrectPassword123");

            User existingUser = User.builder()
                .id(10L)
                .email("tutor@example.com")
                .passwordHash("$2a$10$storedHash")
                .fullName("Gia Sư Tuyệt Vời")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            when(userRepository.findByEmail("tutor@example.com")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("CorrectPassword123", "$2a$10$storedHash")).thenReturn(true);
            when(jwtService.generateAccessToken(existingUser)).thenReturn("jwt.access.token");

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("jwt.access.token");
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.user().id()).isEqualTo(10L);
            assertThat(response.user().email()).isEqualTo("tutor@example.com");

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedRefreshToken = tokenCaptor.getValue();
            assertThat(savedRefreshToken.getUser()).isEqualTo(existingUser);
            // Kiểm tra token hash được băm từ raw token trả về
            assertThat(savedRefreshToken.getTokenHash()).isEqualTo(AuthService.hashToken(response.refreshToken()));
            assertThat(savedRefreshToken.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("Đăng nhập sai mật khẩu -> ném AppException AUTH_INVALID_CREDENTIALS (401)")
        void shouldThrowExceptionWhenPasswordIsIncorrect() {
            LoginRequest request = new LoginRequest("tutor@example.com", "WrongPassword");

            User existingUser = User.builder()
                .id(10L)
                .email("tutor@example.com")
                .passwordHash("$2a$10$storedHash")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .build();

            when(userRepository.findByEmail("tutor@example.com")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("WrongPassword", "$2a$10$storedHash")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
                    assertThat(appEx.getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
                });

            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Đăng nhập email không tồn tại -> ném AppException AUTH_INVALID_CREDENTIALS (401)")
        void shouldThrowExceptionWhenEmailNotFound() {
            LoginRequest request = new LoginRequest("notfound@example.com", "AnyPassword");

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
                    assertThat(appEx.getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
                });
        }

        @Test
        @DisplayName("Đăng nhập tài khoản bị vô hiệu hóa -> ném AppException AUTH_ACCESS_DENIED (403)")
        void shouldThrowExceptionWhenUserIsDisabled() {
            LoginRequest request = new LoginRequest("disabled@example.com", "CorrectPassword");

            User disabledUser = User.builder()
                .id(20L)
                .email("disabled@example.com")
                .passwordHash("$2a$10$storedHash")
                .role(Role.STUDENT)
                .status(UserStatus.DISABLED)
                .build();

            when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(disabledUser));
            when(passwordEncoder.matches("CorrectPassword", "$2a$10$storedHash")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
                    assertThat(appEx.getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
                });
        }
    }
}
