package com.tutorhub.schedule;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.tutorhub.classroom.entity.ClassEnrollment;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ClassEnrollmentRepository;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.schedule.dto.CreateSessionRequest;
import com.tutorhub.schedule.dto.UpdateSessionRequest;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

/**
 * Integration Test chuyên biệt cho Task T3.5:
 * Cảnh báo trùng lịch phía học sinh (FR-3.4 — không chặn).
 * Có trường `warnings` trong response.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionStudentConflictIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User tutorMath;
    private User tutorPhysics;
    private User student1;
    private String tokenTutorMath;
    private String tokenTutorPhysics;

    private ClassEntity mathClass;
    private ClassEntity physicsClass;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM schedule_rules");
        jdbcTemplate.execute("DELETE FROM class_enrollments");
        jdbcTemplate.execute("DELETE FROM classes");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM parent_students");
        jdbcTemplate.execute("DELETE FROM users");

        tutorMath = userRepository.save(User.builder()
                .email("tutormath_" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Thầy Toán")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .build());

        tutorPhysics = userRepository.save(User.builder()
                .email("tutorphysics_" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Thầy Lý")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .build());

        student1 = userRepository.save(User.builder()
                .email("student1_" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Nguyễn Văn An")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build());

        tokenTutorMath = jwtService.generateAccessToken(tutorMath);
        tokenTutorPhysics = jwtService.generateAccessToken(tutorPhysics);

        mathClass = classRepository.save(ClassEntity.builder()
                .name("Lớp Toán Nâng Cao")
                .subject("Toán")
                .classType(ClassType.GROUP)
                .tutor(tutorMath)
                .status(ClassStatus.ACTIVE)
                .build());

        physicsClass = classRepository.save(ClassEntity.builder()
                .name("Lớp Vật Lý 10")
                .subject("Vật lý")
                .classType(ClassType.GROUP)
                .tutor(tutorPhysics)
                .status(ClassStatus.ACTIVE)
                .build());

        // Học sinh An ghi danh ACTIVE vào cả Lớp Toán và Lớp Lý
        classEnrollmentRepository.save(ClassEnrollment.builder()
                .clazz(mathClass)
                .student(student1)
                .status(EnrollmentStatus.ACTIVE)
                .build());

        classEnrollmentRepository.save(ClassEnrollment.builder()
                .clazz(physicsClass)
                .student(student1)
                .status(EnrollmentStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Học sinh bị trùng lịch giữa 2 lớp -> Tạo buổi thành công 201 Created và trả về cảnh báo trong warnings")
    void createSession_StudentConflict_CreatesSessionWithWarning() throws Exception {
        // 1. Thầy Toán tạo buổi học: 2026-11-10 08:00 - 09:30 UTC
        CreateSessionRequest mathSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T08:00:00Z"),
                Instant.parse("2026-11-10T09:30:00Z"),
                "Giải tích", null
        );
        mockMvc.perform(post("/api/v1/classes/{id}/sessions", mathClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorMath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mathSessionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", hasSize(0)));

        // 2. Thầy Lý tạo buổi học trùng khung giờ: 2026-11-10 08:30 - 10:00 UTC
        // Vì khác gia sư nên DB Exclusion Constraint KHÔNG chặn.
        // Học sinh An học cả 2 lớp -> Phải có cảnh báo trong response, nhưng request vẫn thành công 201 Created!
        CreateSessionRequest physicsSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T08:30:00Z"),
                Instant.parse("2026-11-10T10:00:00Z"),
                "Quang học", null
        );

        mockMvc.perform(post("/api/v1/classes/{id}/sessions", physicsClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorPhysics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(physicsSessionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.warnings", hasSize(1)))
                .andExpect(jsonPath("$.warnings[0]", containsString("Nguyễn Văn An")))
                .andExpect(jsonPath("$.warnings[0]", containsString("Lớp Toán Nâng Cao")));

        // DB vẫn lưu buổi học của Thầy Lý
        Integer physicsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE class_id = ?",
                Integer.class,
                physicsClass.getId()
        );
        org.assertj.core.api.Assertions.assertThat(physicsCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Sửa giờ buổi học làm học sinh trùng lịch -> 200 OK và trả về warnings")
    void updateSession_StudentConflict_UpdatesWithWarning() throws Exception {
        // Thầy Toán: 08:00 - 09:30
        CreateSessionRequest mathSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T08:00:00Z"),
                Instant.parse("2026-11-10T09:30:00Z"),
                "Toán", null
        );
        mockMvc.perform(post("/api/v1/classes/{id}/sessions", mathClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorMath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mathSessionReq)))
                .andExpect(status().isCreated());

        // Thầy Lý ban đầu tạo buổi chiều: 14:00 - 15:30 (không trùng)
        CreateSessionRequest physicsSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T14:00:00Z"),
                Instant.parse("2026-11-10T15:30:00Z"),
                "Lý", null
        );
        String physicsRes = mockMvc.perform(post("/api/v1/classes/{id}/sessions", physicsClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorPhysics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(physicsSessionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", hasSize(0)))
                .andReturn().getResponse().getContentAsString();
        long physicsSessionId = objectMapper.readTree(physicsRes).get("id").asLong();

        // Thầy Lý dời giờ lên sáng: 09:00 - 10:30 (chồng lên buổi Toán của học sinh An)
        UpdateSessionRequest updateReq = new UpdateSessionRequest(
                Instant.parse("2026-11-10T09:00:00Z"),
                Instant.parse("2026-11-10T10:30:00Z"),
                "Lý dời giờ", null
        );

        mockMvc.perform(put("/api/v1/sessions/{id}", physicsSessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorPhysics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings", hasSize(1)))
                .andExpect(jsonPath("$.warnings[0]", containsString("Nguyễn Văn An")));
    }

    @Test
    @DisplayName("Buổi ở lớp khác đã CANCELLED -> Không sinh cảnh báo trùng lịch")
    void createSession_OtherClassCancelled_NoWarning() throws Exception {
        // Thầy Toán tạo buổi rồi hủy
        CreateSessionRequest mathSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T08:00:00Z"),
                Instant.parse("2026-11-10T09:30:00Z"),
                "Toán", null
        );
        String res = mockMvc.perform(post("/api/v1/classes/{id}/sessions", mathClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorMath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mathSessionReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long mathSessionId = objectMapper.readTree(res).get("id").asLong();

        // Hủy buổi Toán
        mockMvc.perform(post("/api/v1/sessions/{id}/cancel", mathSessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorMath))
                .andExpect(status().isOk());

        // Thầy Lý tạo buổi trùng khung giờ cũ
        CreateSessionRequest physicsSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T08:30:00Z"),
                Instant.parse("2026-11-10T10:00:00Z"),
                "Lý", null
        );

        mockMvc.perform(post("/api/v1/classes/{id}/sessions", physicsClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorPhysics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(physicsSessionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }

    @Test
    @DisplayName("Học sinh đã nghỉ (LEFT) lớp kia -> Không sinh cảnh báo trùng lịch")
    void createSession_StudentLeftOtherClass_NoWarning() throws Exception {
        // Thầy Toán tạo buổi
        CreateSessionRequest mathSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T08:00:00Z"),
                Instant.parse("2026-11-10T09:30:00Z"),
                "Toán", null
        );
        mockMvc.perform(post("/api/v1/classes/{id}/sessions", mathClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorMath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mathSessionReq)))
                .andExpect(status().isCreated());

        // Học sinh An nghỉ lớp Toán (status = LEFT)
        jdbcTemplate.update(
                "UPDATE class_enrollments SET status = 'LEFT' WHERE class_id = ? AND student_id = ?",
                mathClass.getId(), student1.getId()
        );

        // Thầy Lý tạo buổi học
        CreateSessionRequest physicsSessionReq = new CreateSessionRequest(
                Instant.parse("2026-11-10T08:30:00Z"),
                Instant.parse("2026-11-10T10:00:00Z"),
                "Lý", null
        );

        mockMvc.perform(post("/api/v1/classes/{id}/sessions", physicsClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorPhysics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(physicsSessionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }
}
