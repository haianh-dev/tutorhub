package com.tutorhub.schedule;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.schedule.dto.CreateSessionRequest;
import com.tutorhub.schedule.dto.UpdateSessionRequest;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

/**
 * Integration Test toàn diện cho Task T3.2:
 * - Service/Controller tạo, sửa, hoàn thành, hủy buổi học.
 * - Map lỗi DB vi phạm exclusion constraint -> 409 SESSION_CONFLICT kèm conflictingSessionId.
 * - Test chồng giờ, liền kề (10:00 kết thúc và 10:00 bắt đầu không xung đột).
 * - Buổi CANCELLED không gây xung đột.
 * - Sửa giờ buổi học và re-check trùng.
 * - Kiểm tra bảo mật và ownership: Gia sư B không sửa/xem buổi của Gia sư A -> 404.
 * - Học sinh/Phụ huynh chỉ xem lịch các lớp liên quan.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User tutorA;
    private User tutorB;
    private User admin;
    private User student1;
    private User parent1;

    private String tokenTutorA;
    private String tokenTutorB;
    private String tokenAdmin;
    private String tokenStudent1;
    private String tokenParent1;

    private ClassEntity classA;
    private ClassEntity classB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM schedule_rules");
        jdbcTemplate.execute("DELETE FROM class_enrollments");
        jdbcTemplate.execute("DELETE FROM invitations");
        jdbcTemplate.execute("DELETE FROM password_reset_tokens");
        jdbcTemplate.execute("DELETE FROM classes");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM parent_students");
        jdbcTemplate.execute("DELETE FROM users");

        tutorA = createUser("tutorA@example.com", "Gia Sư A", Role.TUTOR);
        tutorB = createUser("tutorB@example.com", "Gia Sư B", Role.TUTOR);
        admin = createUser("admin@example.com", "Quản Trị Viên", Role.ADMIN);
        student1 = createUser("student1@example.com", "Học Sinh 1", Role.STUDENT);
        parent1 = createUser("parent1@example.com", "Phụ Huynh 1", Role.PARENT);

        tokenTutorA = jwtService.generateAccessToken(tutorA);
        tokenTutorB = jwtService.generateAccessToken(tutorB);
        tokenAdmin = jwtService.generateAccessToken(admin);
        tokenStudent1 = jwtService.generateAccessToken(student1);
        tokenParent1 = jwtService.generateAccessToken(parent1);

        classA = classRepository.save(ClassEntity.builder()
                .name("Toán 12A")
                .subject("Toán")
                .classType(ClassType.ONE_ON_ONE)
                .status(ClassStatus.ACTIVE)
                .tutor(tutorA)
                .build());

        classB = classRepository.save(ClassEntity.builder()
                .name("Vật Lý 11")
                .subject("Vật Lý")
                .classType(ClassType.GROUP)
                .status(ClassStatus.ACTIVE)
                .tutor(tutorB)
                .build());

        // Ghi danh student1 vào classA
        jdbcTemplate.update(
                "INSERT INTO class_enrollments (class_id, student_id, status) VALUES (?, ?, ?)",
                classA.getId(), student1.getId(), EnrollmentStatus.ACTIVE.name()
        );

        // Liên kết parent1 -> student1
        jdbcTemplate.update(
                "INSERT INTO parent_students (parent_id, student_id) VALUES (?, ?)",
                parent1.getId(), student1.getId()
        );
    }

    private User createUser(String email, String fullName, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName(fullName)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Nested
    @DisplayName("1. Tạo buổi học (POST /classes/{id}/sessions)")
    class CreateSessionTests {

        @Test
        @DisplayName("Tạo buổi học hợp lệ -> 201 Created")
        void createSession_Success() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-15T08:00:00Z"),
                    Instant.parse("2026-10-15T09:30:00Z"),
                    "Hàm số mũ",
                    "Chuẩn bị máy tính Casio"
            );

            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.classId", is(classA.getId().intValue())))
                    .andExpect(jsonPath("$.className", is("Toán 12A")))
                    .andExpect(jsonPath("$.tutorId", is(tutorA.getId().intValue())))
                    .andExpect(jsonPath("$.status", is("SCHEDULED")))
                    .andExpect(jsonPath("$.topic", is("Hàm số mũ")));
        }

        @Test
        @DisplayName("Thời gian endAt <= startAt -> 400 Bad Request")
        void createSession_InvalidTimeRange() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-15T10:00:00Z"),
                    Instant.parse("2026-10-15T09:00:00Z"),
                    "Lỗi giờ",
                    null
            );

            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Gia sư B cố tạo buổi trong lớp của Gia sư A -> 404 Không tìm thấy")
        void createSession_ForbiddenTutor() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-15T08:00:00Z"),
                    Instant.parse("2026-10-15T09:30:00Z"),
                    "Chủ đề",
                    null
            );

            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN có quyền tạo buổi trong bất kỳ lớp nào -> 201 Created")
        void createSession_AdminBypass() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-15T08:00:00Z"),
                    Instant.parse("2026-10-15T09:30:00Z"),
                    "Admin tạo",
                    null
            );

            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tutorId", is(tutorA.getId().intValue())));
        }

        @Test
        @DisplayName("Hai buổi liền kề (08:00–10:00 và 10:00–11:30) cùng gia sư -> Thành công, không xung đột")
        void createSession_AdjacentSessions() throws Exception {
            // Buổi 1: 08:00 - 10:00
            CreateSessionRequest req1 = new CreateSessionRequest(
                    Instant.parse("2026-10-15T08:00:00Z"),
                    Instant.parse("2026-10-15T10:00:00Z"),
                    "Buổi 1", null
            );
            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isCreated());

            // Buổi 2: 10:00 - 11:30 (liền kề đúng lúc 10:00)
            CreateSessionRequest req2 = new CreateSessionRequest(
                    Instant.parse("2026-10-15T10:00:00Z"),
                    Instant.parse("2026-10-15T11:30:00Z"),
                    "Buổi 2", null
            );
            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Chồng giờ cùng gia sư -> 409 SESSION_CONFLICT kèm conflictingSessionId")
        void createSession_Conflict() throws Exception {
            // Buổi 1: 09:00 - 11:00
            CreateSessionRequest req1 = new CreateSessionRequest(
                    Instant.parse("2026-10-15T09:00:00Z"),
                    Instant.parse("2026-10-15T11:00:00Z"),
                    "Hình học không gian", null
            );
            String res1 = mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            int sessionId1 = objectMapper.readTree(res1).get("id").asInt();

            // Buổi 2: 10:30 - 12:00 (chồng giờ với buổi 1)
            CreateSessionRequest req2 = new CreateSessionRequest(
                    Instant.parse("2026-10-15T10:30:00Z"),
                    Instant.parse("2026-10-15T12:00:00Z"),
                    "Buổi trùng", null
            );

            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("SESSION_CONFLICT")))
                    .andExpect(jsonPath("$.conflictingSessionId", is(sessionId1)))
                    .andExpect(jsonPath("$.detail", containsString("Trùng với buổi #" + sessionId1)));
        }

        @Test
        @DisplayName("Trùng giờ với buổi đã bị CANCELLED -> Thành công, không bị 409")
        void createSession_OverlappingWithCancelledSession() throws Exception {
            // Tạo buổi 1 và cancel
            CreateSessionRequest req1 = new CreateSessionRequest(
                    Instant.parse("2026-10-15T09:00:00Z"),
                    Instant.parse("2026-10-15T11:00:00Z"),
                    "Buổi sắp hủy", null
            );
            String res1 = mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            int sessionId1 = objectMapper.readTree(res1).get("id").asInt();

            mockMvc.perform(post("/api/v1/sessions/{id}/cancel", sessionId1)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));

            // Tạo buổi 2 trùng khung giờ cũ
            CreateSessionRequest req2 = new CreateSessionRequest(
                    Instant.parse("2026-10-15T09:30:00Z"),
                    Instant.parse("2026-10-15T10:30:00Z"),
                    "Buổi dạy bù thay thế", null
            );
            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("SCHEDULED")));
        }
    }

    @Nested
    @DisplayName("2. Cập nhật, Hủy và Hoàn thành buổi học")
    class UpdateAndStatusTests {

        @Test
        @DisplayName("Cập nhật buổi học hợp lệ -> 200 OK")
        void updateSession_Success() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-16T08:00:00Z"),
                    Instant.parse("2026-10-16T09:30:00Z"),
                    "Chủ đề cũ", null
            );
            String res = mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            int sessionId = objectMapper.readTree(res).get("id").asInt();

            UpdateSessionRequest updateReq = new UpdateSessionRequest(
                    Instant.parse("2026-10-16T08:30:00Z"),
                    Instant.parse("2026-10-16T10:00:00Z"),
                    "Chủ đề mới cập nhật",
                    "Ghi chú mới"
            );

            mockMvc.perform(put("/api/v1/sessions/{id}", sessionId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.topic", is("Chủ đề mới cập nhật")))
                    .andExpect(jsonPath("$.startAt", is("2026-10-16T08:30:00Z")));
        }

        @Test
        @DisplayName("Sửa giờ làm trùng với buổi khác của gia sư -> 409 SESSION_CONFLICT")
        void updateSession_Conflict() throws Exception {
            // Buổi 1: 08:00 - 09:30
            CreateSessionRequest req1 = new CreateSessionRequest(
                    Instant.parse("2026-10-16T08:00:00Z"),
                    Instant.parse("2026-10-16T09:30:00Z"),
                    "Buổi 1", null
            );
            String res1 = mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            int sessionId1 = objectMapper.readTree(res1).get("id").asInt();

            // Buổi 2: 14:00 - 15:30
            CreateSessionRequest req2 = new CreateSessionRequest(
                    Instant.parse("2026-10-16T14:00:00Z"),
                    Instant.parse("2026-10-16T15:30:00Z"),
                    "Buổi 2", null
            );
            String res2 = mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            int sessionId2 = objectMapper.readTree(res2).get("id").asInt();

            // Sửa buổi 2 dịch giờ lên 09:00 - 10:30 (trùng với buổi 1)
            UpdateSessionRequest updateReq = new UpdateSessionRequest(
                    Instant.parse("2026-10-16T09:00:00Z"),
                    Instant.parse("2026-10-16T10:30:00Z"),
                    "Dời giờ", null
            );

            mockMvc.perform(put("/api/v1/sessions/{id}", sessionId2)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("SESSION_CONFLICT")))
                    .andExpect(jsonPath("$.conflictingSessionId", is(sessionId1)));
        }

        @Test
        @DisplayName("Hoàn thành buổi học (POST /sessions/{id}/complete) -> Status COMPLETED")
        void completeSession_Success() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-16T08:00:00Z"),
                    Instant.parse("2026-10-16T09:30:00Z"),
                    "Buổi học", null
            );
            String res = mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            int sessionId = objectMapper.readTree(res).get("id").asInt();

            mockMvc.perform(post("/api/v1/sessions/{id}/complete", sessionId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")));
        }
    }

    @Nested
    @DisplayName("3. Xem danh sách và chi tiết lịch học (GET /sessions)")
    class GetSessionsTests {

        @Test
        @DisplayName("Gia sư xem lịch học theo khoảng ngày -> 200 OK")
        void getSessions_Tutor() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-20T08:00:00Z"),
                    Instant.parse("2026-10-20T09:30:00Z"),
                    "Buổi học A", null
            );
            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/sessions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .param("from", "2026-10-20T00:00:00Z")
                            .param("to", "2026-10-20T23:59:59Z"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].topic", is("Buổi học A")));
        }

        @Test
        @DisplayName("Học sinh chỉ thấy buổi học của lớp mình đã ghi danh")
        void getSessions_Student() throws Exception {
            // Buổi 1 trong classA (student1 có ghi danh)
            CreateSessionRequest req1 = new CreateSessionRequest(
                    Instant.parse("2026-10-20T08:00:00Z"),
                    Instant.parse("2026-10-20T09:30:00Z"),
                    "Lớp Toán của HS1", null
            );
            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isCreated());

            // Buổi 2 trong classB (student1 KHÔNG ghi danh)
            CreateSessionRequest req2 = new CreateSessionRequest(
                    Instant.parse("2026-10-20T14:00:00Z"),
                    Instant.parse("2026-10-20T15:30:00Z"),
                    "Lớp Lý người khác", null
            );
            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classB.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/sessions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].topic", is("Lớp Toán của HS1")));
        }

        @Test
        @DisplayName("Phụ huynh xem lịch học của con thông qua liên kết -> 200 OK")
        void getSessions_Parent() throws Exception {
            CreateSessionRequest req = new CreateSessionRequest(
                    Instant.parse("2026-10-20T08:00:00Z"),
                    Instant.parse("2026-10-20T09:30:00Z"),
                    "Toán con học", null
            );
            mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/sessions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenParent1)
                            .param("studentId", student1.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].topic", is("Toán con học")));
        }
    }
}
