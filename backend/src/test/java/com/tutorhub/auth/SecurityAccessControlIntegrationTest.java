package com.tutorhub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.dto.CreateInvitationRequest;
import com.tutorhub.auth.repository.InvitationRepository;
import com.tutorhub.auth.repository.RefreshTokenRepository;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test toàn diện cho Task T1.5:
 * Cấu hình Security & Phân quyền theo vai trò (Role-Based Access Control).
 *
 * <p>Tiêu chí nghiệm thu:</p>
 * <ul>
 *   <li>Không token / token hỏng / token hết hạn / tài khoản inactive → 401 Unauthorized (AUTH_TOKEN_INVALID).</li>
 *   <li>Sai role (truy cập tài nguyên vượt quá thẩm quyền) → 403 Forbidden (AUTH_ACCESS_DENIED).</li>
 *   <li>Đúng role (hợp quyền) → 200/201 (vượt qua bộ lọc an ninh).</li>
 *   <li>Public endpoints → Không bị chặn 401/403 khi không gửi token.</li>
 *   <li>Ma trận phân quyền 4 vai trò (ADMIN, TUTOR, STUDENT, PARENT) hoạt động chính xác với @PreAuthorize.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SecurityAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private User adminUser;
    private User tutorUser;
    private User studentUser;
    private User parentUser;
    private User inactiveUser;

    private String adminToken;
    private String tutorToken;
    private String studentToken;
    private String parentToken;
    private String inactiveToken;

    @TestConfiguration
    static class RbacTestControllerConfig {
        @RestController
        @RequestMapping("/api/v1/test-rbac")
        static class TestRbacController {

            @GetMapping("/admin-only")
            @PreAuthorize("hasRole('ADMIN')")
            public String adminOnly() {
                return "ADMIN_OK";
            }

            @GetMapping("/tutor-only")
            @PreAuthorize("hasRole('TUTOR')")
            public String tutorOnly() {
                return "TUTOR_OK";
            }

            @GetMapping("/student-only")
            @PreAuthorize("hasRole('STUDENT')")
            public String studentOnly() {
                return "STUDENT_OK";
            }

            @GetMapping("/parent-only")
            @PreAuthorize("hasRole('PARENT')")
            public String parentOnly() {
                return "PARENT_OK";
            }

            @GetMapping("/tutor-or-admin")
            @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
            public String tutorOrAdmin() {
                return "TUTOR_OR_ADMIN_OK";
            }
        }
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM invitations");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        refreshTokenRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM parent_students");
        jdbcTemplate.update("DELETE FROM class_enrollments");
        jdbcTemplate.update("DELETE FROM classes");
        userRepository.deleteAll();

        adminUser = createUser("admin@example.com", "Admin User", Role.ADMIN, UserStatus.ACTIVE);
        tutorUser = createUser("tutor@example.com", "Tutor User", Role.TUTOR, UserStatus.ACTIVE);
        studentUser = createUser("student@example.com", "Student User", Role.STUDENT, UserStatus.ACTIVE);
        parentUser = createUser("parent@example.com", "Parent User", Role.PARENT, UserStatus.ACTIVE);
        inactiveUser = createUser("inactive@example.com", "Inactive User", Role.STUDENT, UserStatus.DISABLED);

        adminToken = jwtService.generateAccessToken(adminUser);
        tutorToken = jwtService.generateAccessToken(tutorUser);
        studentToken = jwtService.generateAccessToken(studentUser);
        parentToken = jwtService.generateAccessToken(parentUser);
        inactiveToken = jwtService.generateAccessToken(inactiveUser);
    }

    private User createUser(String email, String fullName, Role role, UserStatus status) {
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode("Password123!"))
            .fullName(fullName)
            .role(role)
            .status(status)
            .build();
        return userRepository.save(user);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        if (jwtSecret.length() == 64 && jwtSecret.matches("^[0-9a-fA-F]+$")) {
            keyBytes = HexFormat.of().parseHex(jwtSecret);
        } else {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. NHÓM TEST: Không token hoặc Token không hợp lệ → 401 Unauthorized
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. Không token / Token không hợp lệ → 401 Unauthorized")
    class UnauthenticatedTests {

        @Test
        @DisplayName("Không truyền Authorization header tới GET /api/v1/me → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenNoTokenOnGetMe() throws Exception {
            mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"))
                .andExpect(jsonPath("$.detail").value("Yêu cầu đăng nhập."));
        }

        @Test
        @DisplayName("Không truyền Authorization header tới PUT /api/v1/me/password → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenNoTokenOnChangePassword() throws Exception {
            mockMvc.perform(put("/api/v1/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPassword\":\"Old123!\",\"newPassword\":\"New123456!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Không truyền Authorization header tới POST /api/v1/auth/logout → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenNoTokenOnLogout() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"sample-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Không truyền Authorization header tới POST /api/v1/invitations → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenNoTokenOnCreateInvitation() throws Exception {
            CreateInvitationRequest request = new CreateInvitationRequest(Role.STUDENT, null, null, null);
            mockMvc.perform(post("/api/v1/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Authorization header không có tiền tố Bearer → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenAuthorizationHeaderLacksBearer() throws Exception {
            mockMvc.perform(get("/api/v1/me")
                    .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Token JWT rác / malformed → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenTokenIsMalformed() throws Exception {
            mockMvc.perform(get("/api/v1/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Token JWT sai chữ ký (ký bằng secret key khác) → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenTokenHasInvalidSignature() throws Exception {
            SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-that-is-at-least-256-bits-long!!".getBytes(StandardCharsets.UTF_8));
            String forgedToken = Jwts.builder()
                .subject(String.valueOf(studentUser.getId()))
                .claim("email", studentUser.getEmail())
                .claim("role", studentUser.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(wrongKey)
                .compact();

            mockMvc.perform(get("/api/v1/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Token JWT đã hết hạn (Expired) → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenTokenIsExpired() throws Exception {
            SecretKey key = getSigningKey();
            Instant now = Instant.now();
            String expiredToken = Jwts.builder()
                .subject(String.valueOf(studentUser.getId()))
                .claim("email", studentUser.getEmail())
                .claim("role", studentUser.getRole().name())
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(1800))) // Đã hết hạn từ 30 phút trước
                .signWith(key)
                .compact();

            mockMvc.perform(get("/api/v1/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Tài khoản bị vô hiệu hóa (INACTIVE) → 401 AUTH_TOKEN_INVALID")
        void shouldReturn401WhenUserIsInactive() throws Exception {
            mockMvc.perform(get("/api/v1/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + inactiveToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. NHÓM TEST: Sai vai trò (Forbidden) trên endpoint thực tế → 403
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. Sai vai trò → 403 Forbidden")
    class ForbiddenRoleTests {

        @Test
        @DisplayName("STUDENT gọi POST /api/v1/invitations → 403 AUTH_ACCESS_DENIED")
        void shouldReturn403WhenStudentCreatesInvitation() throws Exception {
            CreateInvitationRequest request = new CreateInvitationRequest(Role.STUDENT, null, null, null);
            mockMvc.perform(post("/api/v1/invitations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"))
                .andExpect(jsonPath("$.detail").value("Bạn không có quyền thực hiện thao tác này."));
        }

        @Test
        @DisplayName("PARENT gọi POST /api/v1/invitations → 403 AUTH_ACCESS_DENIED")
        void shouldReturn403WhenParentCreatesInvitation() throws Exception {
            CreateInvitationRequest request = new CreateInvitationRequest(Role.PARENT, null, null, studentUser.getId());
            mockMvc.perform(post("/api/v1/invitations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("STUDENT gọi POST /api/v1/users/{id}/password-reset-link → 403 AUTH_ACCESS_DENIED")
        void shouldReturn403WhenStudentCreatesResetLink() throws Exception {
            mockMvc.perform(post("/api/v1/users/{id}/password-reset-link", studentUser.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("PARENT gọi POST /api/v1/users/{id}/password-reset-link → 403 AUTH_ACCESS_DENIED")
        void shouldReturn403WhenParentCreatesResetLink() throws Exception {
            mockMvc.perform(post("/api/v1/users/{id}/password-reset-link", studentUser.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. NHÓM TEST: Đúng vai trò (Authorized) → Thành công vượt qua Security
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. Đúng vai trò → Hợp quyền (200 / 201 / không bị 401/403)")
    class AuthorizedRoleTests {

        @Test
        @DisplayName("TUTOR gọi POST /api/v1/invitations → Vượt qua Security filter (201 Created)")
        void shouldAllowTutorToCreateInvitation() throws Exception {
            CreateInvitationRequest request = new CreateInvitationRequest(Role.STUDENT, null, null, null);
            mockMvc.perform(post("/api/v1/invitations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.link").isNotEmpty());
        }

        @Test
        @DisplayName("ADMIN gọi POST /api/v1/invitations → Vượt qua Security filter (201 Created)")
        void shouldAllowAdminToCreateInvitation() throws Exception {
            CreateInvitationRequest request = new CreateInvitationRequest(Role.STUDENT, null, null, null);
            mockMvc.perform(post("/api/v1/invitations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
        }

        @Test
        @DisplayName("Cả 4 vai trò (ADMIN, TUTOR, STUDENT, PARENT) đều truy cập được GET /api/v1/me")
        void shouldAllowAllRolesToAccessMeEndpoint() throws Exception {
            mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

            mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TUTOR"));

            mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

            mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PARENT"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. NHÓM TEST: Public Endpoints (Không cần token, không bị chặn 401)
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. Public Endpoints → Không bị chặn 401 khi không gửi token")
    class PublicEndpointTests {

        @Test
        @DisplayName("POST /api/v1/auth/login không token → không bị 401 chặn (trả 400 do validation)")
        void shouldAllowPublicLogin() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/register-tutor không token → không bị 401 chặn (trả 400 do validation)")
        void shouldAllowPublicRegisterTutor() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register-tutor")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh không token → không bị 401 chặn (trả 400 do validation)")
        void shouldAllowPublicRefresh() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GET /api/v1/auth/invitations/{token} không token → không bị 401 chặn (trả 410 do token không tồn tại)")
        void shouldAllowPublicVerifyInvitation() throws Exception {
            mockMvc.perform(get("/api/v1/auth/invitations/non-existent-token"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
        }

        @Test
        @DisplayName("GET /actuator/health không token → 200 UP")
        void shouldAllowPublicActuatorHealth() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. NHÓM TEST: Ma trận phân quyền 4 vai trò mẫu (RBAC Matrix)
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("5. Ma trận phân quyền 4 vai trò (@PreAuthorize hasRole / hasAnyRole)")
    class RbacMatrixTests {

        @Test
        @DisplayName("/admin-only: ADMIN → 200; TUTOR/STUDENT/PARENT → 403; Anonymous → 401")
        void testAdminOnlyEndpointMatrix() throws Exception {
            // Anonymous -> 401
            mockMvc.perform(get("/api/v1/test-rbac/admin-only"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

            // ADMIN -> 200
            mockMvc.perform(get("/api/v1/test-rbac/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

            // TUTOR -> 403
            mockMvc.perform(get("/api/v1/test-rbac/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

            // STUDENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

            // PARENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("/tutor-only: TUTOR → 200; ADMIN/STUDENT/PARENT → 403; Anonymous → 401")
        void testTutorOnlyEndpointMatrix() throws Exception {
            // Anonymous -> 401
            mockMvc.perform(get("/api/v1/test-rbac/tutor-only"))
                .andExpect(status().isUnauthorized());

            // TUTOR -> 200
            mockMvc.perform(get("/api/v1/test-rbac/tutor-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorToken))
                .andExpect(status().isOk());

            // ADMIN -> 403 (chứng minh hasRole('TUTOR') chỉ dành riêng cho TUTOR)
            mockMvc.perform(get("/api/v1/test-rbac/tutor-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

            // STUDENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/tutor-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

            // PARENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/tutor-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/student-only: STUDENT → 200; TUTOR/ADMIN/PARENT → 403; Anonymous → 401")
        void testStudentOnlyEndpointMatrix() throws Exception {
            // Anonymous -> 401
            mockMvc.perform(get("/api/v1/test-rbac/student-only"))
                .andExpect(status().isUnauthorized());

            // STUDENT -> 200
            mockMvc.perform(get("/api/v1/test-rbac/student-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk());

            // TUTOR -> 403
            mockMvc.perform(get("/api/v1/test-rbac/student-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

            // PARENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/student-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/parent-only: PARENT → 200; TUTOR/ADMIN/STUDENT → 403; Anonymous → 401")
        void testParentOnlyEndpointMatrix() throws Exception {
            // Anonymous -> 401
            mockMvc.perform(get("/api/v1/test-rbac/parent-only"))
                .andExpect(status().isUnauthorized());

            // PARENT -> 200
            mockMvc.perform(get("/api/v1/test-rbac/parent-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isOk());

            // STUDENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/parent-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

            // TUTOR -> 403
            mockMvc.perform(get("/api/v1/test-rbac/parent-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/tutor-or-admin: TUTOR/ADMIN → 200; STUDENT/PARENT → 403; Anonymous → 401")
        void testTutorOrAdminEndpointMatrix() throws Exception {
            // Anonymous -> 401
            mockMvc.perform(get("/api/v1/test-rbac/tutor-or-admin"))
                .andExpect(status().isUnauthorized());

            // TUTOR -> 200
            mockMvc.perform(get("/api/v1/test-rbac/tutor-or-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorToken))
                .andExpect(status().isOk());

            // ADMIN -> 200
            mockMvc.perform(get("/api/v1/test-rbac/tutor-or-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

            // STUDENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/tutor-or-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

            // PARENT -> 403
            mockMvc.perform(get("/api/v1/test-rbac/tutor-or-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        }
    }
}
