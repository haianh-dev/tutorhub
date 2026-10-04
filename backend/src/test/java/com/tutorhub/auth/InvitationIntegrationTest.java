package com.tutorhub.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.dto.AcceptInvitationRequest;
import com.tutorhub.auth.dto.CreateInvitationRequest;
import com.tutorhub.auth.entity.Invitation;
import com.tutorhub.auth.repository.InvitationRepository;
import com.tutorhub.auth.service.AuthService;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests cho Task T1.4: Lời mời (Invitations).
 *
 * <p>Kiểm tra:</p>
 * <ol>
 *   <li>Tạo lời mời STUDENT thành công (TUTOR)</li>
 *   <li>Tạo lời mời PARENT thành công (TUTOR, có studentId)</li>
 *   <li>Kiểm tra lời mời hợp lệ qua GET</li>
 *   <li>Chấp nhận lời mời STUDENT → tạo tài khoản + ghi danh</li>
 *   <li>Chấp nhận lời mời PARENT → tạo tài khoản + liên kết parent_students</li>
 *   <li>Token đã dùng bị từ chối (410 INVITATION_USED)</li>
 *   <li>Token hết hạn bị từ chối (410 INVITATION_EXPIRED)</li>
 *   <li>Token sai/không tồn tại bị từ chối (410)</li>
 *   <li>STUDENT không có quyền tạo lời mời (403)</li>
 *   <li>TUTOR không được mời học sinh không thuộc lớp mình (404)</li>
 *   <li>Chấp nhận lời mời PARENT thiếu studentId → lỗi validation</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InvitationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private InvitationRepository invitationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private User tutor;
    private User student;
    private Long classId;
    private String tutorBearerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Xóa dữ liệu cũ theo thứ tự FK
        jdbcTemplate.update("DELETE FROM invitations");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM parent_students");
        jdbcTemplate.update("DELETE FROM class_enrollments");
        jdbcTemplate.update("DELETE FROM classes");
        userRepository.deleteAll();

        // Tạo gia sư
        tutor = userRepository.save(User.builder()
            .email("tutor@example.com")
            .passwordHash(passwordEncoder.encode("TutorPass123!"))
            .fullName("Nguyễn Văn Gia Sư")
            .role(Role.TUTOR)
            .status(UserStatus.ACTIVE)
            .build());

        // Tạo học sinh
        student = userRepository.save(User.builder()
            .email("student@example.com")
            .passwordHash(passwordEncoder.encode("StudentPass123!"))
            .fullName("Trần Văn Học Sinh")
            .role(Role.STUDENT)
            .status(UserStatus.ACTIVE)
            .build());

        // Tạo lớp học của gia sư
        classId = jdbcTemplate.queryForObject(
            "INSERT INTO classes (tutor_id, name, subject, class_type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class, tutor.getId(), "Toán 12A", "Toán", "GROUP", "ACTIVE");

        // Ghi danh học sinh vào lớp
        jdbcTemplate.update(
            "INSERT INTO class_enrollments (class_id, student_id, status, enrolled_at, created_at) VALUES (?, ?, 'ACTIVE', NOW(), NOW())",
            classId, student.getId());

        // Lấy Bearer token cho gia sư
        tutorBearerToken = loginAndGetToken("tutor@example.com", "TutorPass123!");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
            {"email": "%s", "password": "%s"}
            """.formatted(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asText();
    }

    private String createExpiredInvitation(Role role, String email, Long classIdParam, Long studentIdParam) {
        // Sinh raw token và lưu invitation đã hết hạn
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = AuthService.hashToken(rawToken);

        User studentRef = studentIdParam != null ? student : null;

        Invitation invitation = Invitation.builder()
            .tokenHash(tokenHash)
            .role(role)
            .invitedBy(tutor)
            .email(email)
            .classId(classIdParam)
            .student(studentRef)
            .expiresAt(Instant.now().minusSeconds(3600)) // đã hết hạn 1 giờ trước
            .build();
        invitationRepository.save(invitation);
        return rawToken;
    }

    // ─── Tạo lời mời ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/invitations — Tạo lời mời")
    class CreateInvitationTests {

        @Test
        @DisplayName("TUTOR tạo lời mời STUDENT kèm classId thành công → 201 + link")
        void tutorCreatesStudentInvitationWithClass() throws Exception {
            CreateInvitationRequest req = new CreateInvitationRequest(
                Role.STUDENT, "newstudent@example.com", classId, null);

            mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.link").isNotEmpty())
                .andExpect(jsonPath("$.link", containsString("accept-invitation")))
                .andExpect(jsonPath("$.link", containsString("token=")))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("newstudent@example.com"))
                .andExpect(jsonPath("$.className").value("Toán 12A"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

            // Kiểm tra DB: lưu đúng 1 invitation
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM invitations WHERE invited_by = ?", tutor.getId());
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).get("role")).isEqualTo("STUDENT");
            assertThat(rows.get(0).get("class_id")).isEqualTo(classId);
        }

        @Test
        @DisplayName("TUTOR tạo lời mời PARENT với studentId hợp lệ → 201")
        void tutorCreatesParentInvitationWithStudentId() throws Exception {
            CreateInvitationRequest req = new CreateInvitationRequest(
                Role.PARENT, "parent@example.com", null, student.getId());

            mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PARENT"))
                .andExpect(jsonPath("$.link").isNotEmpty());

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM invitations WHERE invited_by = ? AND role = 'PARENT'", tutor.getId());
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).get("student_id")).isEqualTo(student.getId());
        }

        @Test
        @DisplayName("STUDENT không có quyền tạo lời mời → 403")
        void studentCannotCreateInvitation() throws Exception {
            // Đăng nhập với tài khoản học sinh
            String studentToken = loginAndGetToken("student@example.com", "StudentPass123!");

            CreateInvitationRequest req = new CreateInvitationRequest(
                Role.STUDENT, "another@example.com", classId, null);

            mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TUTOR mời PARENT thiếu studentId → 400 VALIDATION_ERROR")
        void createParentInvitationWithoutStudentIdFails() throws Exception {
            CreateInvitationRequest req = new CreateInvitationRequest(
                Role.PARENT, "parent@example.com", null, null); // studentId null

            mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("TUTOR mời học sinh không thuộc lớp của mình → 404")
        void tutorCannotInviteStudentFromOtherClass() throws Exception {
            // Tạo gia sư khác
            User otherTutor = userRepository.save(User.builder()
                .email("other.tutor@example.com")
                .passwordHash(passwordEncoder.encode("OtherTutor123!"))
                .fullName("Gia Sư Khác")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .build());
            Long otherClassId = jdbcTemplate.queryForObject(
                "INSERT INTO classes (tutor_id, name, subject, class_type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, otherTutor.getId(), "Lý 11B", "Lý", "GROUP", "ACTIVE");

            // TUTOR hiện tại cố mời học sinh thuộc lớp của gia sư khác — bằng classId của gia sư khác
            CreateInvitationRequest req = new CreateInvitationRequest(
                Role.STUDENT, "newstudent2@example.com", otherClassId, null);

            mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Không có token → 401")
        void createInvitationWithoutAuthFails() throws Exception {
            CreateInvitationRequest req = new CreateInvitationRequest(
                Role.STUDENT, "x@example.com", classId, null);

            mockMvc.perform(post("/api/v1/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
        }
    }

    // ─── Verify lời mời ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/auth/invitations/{token} — Kiểm tra lời mời")
    class VerifyInvitationTests {

        @Test
        @DisplayName("Token hợp lệ → 200 + thông tin lời mời")
        void validTokenReturnsInvitationInfo() throws Exception {
            // Tạo invitation qua API
            CreateInvitationRequest req = new CreateInvitationRequest(
                Role.STUDENT, "verify.test@example.com", classId, null);
            MvcResult createResult = mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

            // Trích xuất token từ link
            String link = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").asText();
            String rawToken = link.substring(link.indexOf("token=") + 6);

            mockMvc.perform(get("/api/v1/auth/invitations/{token}", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("verify.test@example.com"))
                .andExpect(jsonPath("$.className").value("Toán 12A"));
        }

        @Test
        @DisplayName("Token hết hạn → 410 INVITATION_EXPIRED")
        void expiredTokenReturns410() throws Exception {
            String expiredToken = createExpiredInvitation(Role.STUDENT, "expired@example.com", classId, null);

            mockMvc.perform(get("/api/v1/auth/invitations/{token}", expiredToken))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
        }

        @Test
        @DisplayName("Token không tồn tại → 410")
        void invalidTokenReturns410() throws Exception {
            mockMvc.perform(get("/api/v1/auth/invitations/this-token-does-not-exist"))
                .andExpect(status().isGone());
        }
    }

    // ─── Accept lời mời ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/accept-invitation — Chấp nhận lời mời")
    class AcceptInvitationTests {

        @Test
        @DisplayName("STUDENT chấp nhận → tạo tài khoản STUDENT + ghi danh vào lớp + tự đăng nhập")
        void studentAcceptsInvitationAndEnrollsInClass() throws Exception {
            // Tạo invitation cho học sinh mới với classId
            CreateInvitationRequest createReq = new CreateInvitationRequest(
                Role.STUDENT, "newstudent@example.com", classId, null);
            MvcResult createResult = mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

            String link = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").asText();
            String rawToken = link.substring(link.indexOf("token=") + 6);

            // Chấp nhận lời mời
            AcceptInvitationRequest acceptReq = new AcceptInvitationRequest(
                rawToken, "NewPassword123!", "Học Sinh Mới", "0987654321");

            mockMvc.perform(post("/api/v1/auth/accept-invitation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("newstudent@example.com"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.fullName").value("Học Sinh Mới"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));

            // Kiểm tra tài khoản tạo thành công
            assertThat(userRepository.findByEmail("newstudent@example.com")).isPresent();

            // Kiểm tra ghi danh vào lớp
            Long enrollmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM class_enrollments WHERE class_id = ? AND student_id = (SELECT id FROM users WHERE email = ?) AND status = 'ACTIVE'",
                Long.class, classId, "newstudent@example.com");
            assertThat(enrollmentCount).isEqualTo(1L);

            // Kiểm tra invitation đã được đánh dấu used
            Invitation inv = invitationRepository.findByTokenHash(AuthService.hashToken(rawToken)).orElseThrow();
            assertThat(inv.isUsed()).isTrue();
        }

        @Test
        @DisplayName("PARENT chấp nhận → tạo tài khoản PARENT + liên kết parent_students")
        void parentAcceptsInvitationAndLinksToStudent() throws Exception {
            // Tạo invitation cho phụ huynh
            CreateInvitationRequest createReq = new CreateInvitationRequest(
                Role.PARENT, "parent@example.com", null, student.getId());
            MvcResult createResult = mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

            String link = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").asText();
            String rawToken = link.substring(link.indexOf("token=") + 6);

            // Chấp nhận lời mời
            AcceptInvitationRequest acceptReq = new AcceptInvitationRequest(
                rawToken, "ParentPass123!", "Phụ Huynh", "0911222333");

            mockMvc.perform(post("/api/v1/auth/accept-invitation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PARENT"))
                .andExpect(jsonPath("$.user.email").value("parent@example.com"));

            // Kiểm tra liên kết parent_students
            User parent = userRepository.findByEmail("parent@example.com").orElseThrow();
            Long linkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parent_students WHERE parent_id = ? AND student_id = ?",
                Long.class, parent.getId(), student.getId());
            assertThat(linkCount).isEqualTo(1L);
        }

        @Test
        @DisplayName("Token đã sử dụng → 410 INVITATION_USED")
        void usedTokenIsRejected() throws Exception {
            // Tạo và sử dụng invitation
            CreateInvitationRequest createReq = new CreateInvitationRequest(
                Role.STUDENT, "onetime@example.com", classId, null);
            MvcResult createResult = mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

            String link = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").asText();
            String rawToken = link.substring(link.indexOf("token=") + 6);

            // Lần 1: Chấp nhận thành công
            AcceptInvitationRequest acceptReq = new AcceptInvitationRequest(
                rawToken, "Password123!", "User Mới", null);
            mockMvc.perform(post("/api/v1/auth/accept-invitation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isCreated());

            // Lần 2: Cùng token → 410 INVITATION_USED
            AcceptInvitationRequest acceptReq2 = new AcceptInvitationRequest(
                rawToken, "Password123!", "Người Khác", null);
            mockMvc.perform(post("/api/v1/auth/accept-invitation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(acceptReq2)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_USED"));
        }

        @Test
        @DisplayName("Token hết hạn → 410 INVITATION_EXPIRED")
        void expiredTokenCannotBeAccepted() throws Exception {
            String expiredToken = createExpiredInvitation(Role.STUDENT, "expired2@example.com", classId, null);

            AcceptInvitationRequest acceptReq = new AcceptInvitationRequest(
                expiredToken, "Password123!", "User Hết Hạn", null);

            mockMvc.perform(post("/api/v1/auth/accept-invitation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
        }

        @Test
        @DisplayName("Token giả → 410")
        void fakeTokenIsRejected() throws Exception {
            AcceptInvitationRequest acceptReq = new AcceptInvitationRequest(
                "completely-fake-token-xyz-123", "Password123!", "Hacker", null);

            mockMvc.perform(post("/api/v1/auth/accept-invitation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isGone());
        }

        @Test
        @DisplayName("Mật khẩu quá ngắn → 400 VALIDATION_ERROR")
        void shortPasswordIsRejected() throws Exception {
            CreateInvitationRequest createReq = new CreateInvitationRequest(
                Role.STUDENT, "shortpw@example.com", classId, null);
            MvcResult createResult = mockMvc.perform(post("/api/v1/invitations")
                    .header("Authorization", "Bearer " + tutorBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

            String link = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").asText();
            String rawToken = link.substring(link.indexOf("token=") + 6);

            AcceptInvitationRequest acceptReq = new AcceptInvitationRequest(
                rawToken, "short", "User", null); // mật khẩu < 8 ký tự

            mockMvc.perform(post("/api/v1/auth/accept-invitation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}
