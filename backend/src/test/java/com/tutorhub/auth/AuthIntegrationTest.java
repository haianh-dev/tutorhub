package com.tutorhub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.dto.LoginRequest;
import com.tutorhub.auth.dto.RegisterTutorRequest;
import com.tutorhub.auth.entity.RefreshToken;
import com.tutorhub.auth.repository.RefreshTokenRepository;
import com.tutorhub.auth.service.AuthService;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-tutor - Đăng ký thành công, mật khẩu lưu BCrypt, role TUTOR")
    void shouldRegisterTutorSuccessfully() throws Exception {
        RegisterTutorRequest request = new RegisterTutorRequest(
            "tutor.test@example.com",
            "StrongPass123@",
            "Nguyễn Văn Gia Sư",
            "0912345678"
        );

        mockMvc.perform(post("/api/v1/auth/register-tutor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.id").isNumber())
            .andExpect(jsonPath("$.user.email").value("tutor.test@example.com"))
            .andExpect(jsonPath("$.user.fullName").value("Nguyễn Văn Gia Sư"))
            .andExpect(jsonPath("$.user.role").value("TUTOR"))
            .andExpect(jsonPath("$.user.status").value("ACTIVE"))
            .andExpect(jsonPath("$.user.password").doesNotExist())
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        // Kiểm tra lưu vào database thật
        Optional<User> userOpt = userRepository.findByEmail("tutor.test@example.com");
        assertThat(userOpt).isPresent();
        User savedUser = userOpt.get();
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("StrongPass123@");
        assertThat(passwordEncoder.matches("StrongPass123@", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-tutor - Email trùng trả về 409 DUPLICATE_RESOURCE")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        // Tạo trước một user
        userRepository.save(User.builder()
            .email("duplicate@example.com")
            .passwordHash(passwordEncoder.encode("Password123@"))
            .fullName("User Cu")
            .role(Role.TUTOR)
            .build());

        RegisterTutorRequest request = new RegisterTutorRequest(
            "duplicate@example.com",
            "Password123@",
            "User Moi",
            null
        );

        mockMvc.perform(post("/api/v1/auth/register-tutor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
            .andExpect(jsonPath("$.detail").value("Email đã được sử dụng"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-tutor - Mật khẩu < 8 ký tự trả về 400 VALIDATION_ERROR")
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        RegisterTutorRequest request = new RegisterTutorRequest(
            "valid.email@example.com",
            "short", // < 8 ký tự
            "Tên Hợp Lệ",
            null
        );

        mockMvc.perform(post("/api/v1/auth/register-tutor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "password"))));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-tutor - Email không hợp lệ trả về 400 VALIDATION_ERROR")
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        RegisterTutorRequest request = new RegisterTutorRequest(
            "not-an-email",
            "ValidPassword123@",
            "Tên Hợp Lệ",
            null
        );

        mockMvc.perform(post("/api/v1/auth/register-tutor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors", hasItem(hasEntry("field", "email"))));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Đăng nhập thành công trả về JWT accessToken và refreshToken")
    void shouldLoginSuccessfully() throws Exception {
        // Đăng ký trước một user
        User user = userRepository.save(User.builder()
            .email("login.test@example.com")
            .passwordHash(passwordEncoder.encode("MySecretPass123"))
            .fullName("Gia Sư Login Test")
            .role(Role.TUTOR)
            .build());

        LoginRequest request = new LoginRequest("login.test@example.com", "MySecretPass123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.user.id").value(user.getId()))
            .andExpect(jsonPath("$.user.email").value("login.test@example.com"))
            .andExpect(jsonPath("$.user.role").value("TUTOR"))
            .andReturn();

        // Trích xuất token từ response để kiểm tra
        String responseBody = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        String rawRefreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();

        // Kiểm tra accessToken hợp lệ qua JwtService
        assertThat(jwtService.validateToken(accessToken)).isTrue();
        assertThat(jwtService.extractUserId(accessToken)).isEqualTo(user.getId());

        // Kiểm tra refreshToken được lưu băm trong DB
        String expectedHash = AuthService.hashToken(rawRefreshToken);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(expectedHash);
        assertThat(tokenOpt).isPresent();
        assertThat(tokenOpt.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(tokenOpt.get().isValid()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Sai mật khẩu trả về 401 AUTH_INVALID_CREDENTIALS")
    void shouldReturn401WhenPasswordIsIncorrect() throws Exception {
        userRepository.save(User.builder()
            .email("user@example.com")
            .passwordHash(passwordEncoder.encode("CorrectPassword123"))
            .fullName("User")
            .role(Role.TUTOR)
            .build());

        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.detail").value("Email hoặc mật khẩu không chính xác"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Email không tồn tại trả về 401 AUTH_INVALID_CREDENTIALS")
    void shouldReturn401WhenEmailDoesNotExist() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "SomePassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.detail").value("Email hoặc mật khẩu không chính xác"));
    }
}
