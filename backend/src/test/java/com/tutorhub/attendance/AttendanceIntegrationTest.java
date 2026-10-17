package com.tutorhub.attendance;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.attendance.entity.AttendanceStatus;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.schedule.entity.SessionEntity;
import com.tutorhub.schedule.entity.SessionStatus;
import com.tutorhub.schedule.repository.SessionRepository;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

/**
 * Integration Test cho Task T4.1 — Điểm danh.
 *
 * <p>Các kịch bản test:</p>
 * <ol>
 *   <li>GET /sessions/{id}/attendance → danh sách rỗng khi chưa điểm danh</li>
 *   <li>PUT bulk hợp lệ → 200, lưu đúng trạng thái</li>
 *   <li>PUT bulk lại → upsert, update status</li>
 *   <li>PUT có studentId không enrolled → 422 STUDENT_NOT_ENROLLED, toàn bộ rollback (AC FR-4)</li>
 *   <li>TUTOR khác cố ghi → 404</li>
 *   <li>STUDENT cố ghi → 403</li>
 *   <li>GET /students/{id}/attendance → lịch sử điểm danh đúng</li>
 *   <li>STUDENT chỉ xem của chính mình, không xem của học sinh khác → 404</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AttendanceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User tutor;
    private User tutorOther;
    private User student1;
    private User student2;
    private User studentNotInClass;
    private User parent;

    private String tokenTutor;
    private String tokenTutorOther;
    private String tokenStudent1;
    private String tokenStudent2;
    private String tokenParent;

    private ClassEntity clazz;
    private SessionEntity session;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM attendance");
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM schedule_rules");
        jdbcTemplate.execute("DELETE FROM class_enrollments");
        jdbcTemplate.execute("DELETE FROM invitations");
        jdbcTemplate.execute("DELETE FROM password_reset_tokens");
        jdbcTemplate.execute("DELETE FROM classes");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM parent_students");
        jdbcTemplate.execute("DELETE FROM users");

        tutor           = createUser("tutor@example.com",        "Gia Sư",            Role.TUTOR);
        tutorOther      = createUser("tutor2@example.com",       "Gia Sư Khác",       Role.TUTOR);
        student1        = createUser("student1@example.com",     "Học Sinh 1",        Role.STUDENT);
        student2        = createUser("student2@example.com",     "Học Sinh 2",        Role.STUDENT);
        studentNotInClass = createUser("stranger@example.com",   "Học Sinh Ngoài",    Role.STUDENT);
        parent          = createUser("parent@example.com",       "Phụ Huynh",         Role.PARENT);

        tokenTutor      = jwtService.generateAccessToken(tutor);
        tokenTutorOther = jwtService.generateAccessToken(tutorOther);
        tokenStudent1   = jwtService.generateAccessToken(student1);
        tokenStudent2   = jwtService.generateAccessToken(student2);
        tokenParent     = jwtService.generateAccessToken(parent);

        clazz = classRepository.save(ClassEntity.builder()
                .name("Toán Nhóm A")
                .subject("Toán")
                .classType(ClassType.GROUP)
                .status(ClassStatus.ACTIVE)
                .tutor(tutor)
                .build());

        // Ghi danh student1 và student2 vào lớp (ACTIVE)
        jdbcTemplate.update(
                "INSERT INTO class_enrollments (class_id, student_id, status) VALUES (?, ?, ?)",
                clazz.getId(), student1.getId(), EnrollmentStatus.ACTIVE.name()
        );
        jdbcTemplate.update(
                "INSERT INTO class_enrollments (class_id, student_id, status) VALUES (?, ?, ?)",
                clazz.getId(), student2.getId(), EnrollmentStatus.ACTIVE.name()
        );

        // Liên kết parent -> student1
        jdbcTemplate.update(
                "INSERT INTO parent_students (parent_id, student_id) VALUES (?, ?)",
                parent.getId(), student1.getId()
        );

        // Tạo một buổi học
        session = sessionRepository.save(SessionEntity.builder()
                .clazz(clazz)
                .tutor(tutor)
                .startAt(Instant.parse("2026-10-15T08:00:00Z"))
                .endAt(Instant.parse("2026-10-15T09:30:00Z"))
                .status(SessionStatus.SCHEDULED)
                .topic("Hàm số mũ")
                .build());
    }

    // ─── GET /sessions/{id}/attendance ─────────────────────────────────────────

    @Nested
    @DisplayName("1. GET /sessions/{id}/attendance")
    class GetAttendanceTests {

        @Test
        @DisplayName("Chưa điểm danh → trả danh sách rỗng")
        void getAttendance_empty() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("TUTOR khác → 404")
        void getAttendance_otherTutor_404() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorOther))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("STUDENT cố xem → 403")
        void getAttendance_student_403() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── PUT /sessions/{id}/attendance (bulk) ───────────────────────────────────

    @Nested
    @DisplayName("2. PUT /sessions/{id}/attendance (bulk)")
    class BulkUpdateAttendanceTests {

        @Test
        @DisplayName("Bulk hợp lệ → 200, trả đúng số records")
        void bulkUpdate_success() throws Exception {
            var body = Map.of("records", List.of(
                    Map.of("studentId", student1.getId(), "status", "PRESENT"),
                    Map.of("studentId", student2.getId(), "status", "ABSENT_EXCUSED", "note", "Nghỉ phép")
            ));

            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].status", notNullValue()));
        }

        @Test
        @DisplayName("Bulk lần 2 → upsert, cập nhật trạng thái mới")
        void bulkUpdate_upsert_updatesExisting() throws Exception {
            // Lần 1
            var body1 = Map.of("records", List.of(
                    Map.of("studentId", student1.getId(), "status", "ABSENT_UNEXCUSED")
            ));
            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body1)))
                    .andExpect(status().isOk());

            // Lần 2 — đổi status của student1 sang PRESENT
            var body2 = Map.of("records", List.of(
                    Map.of("studentId", student1.getId(), "status", "PRESENT", "note", "Đến muộn")
            ));
            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is(AttendanceStatus.PRESENT.name())))
                    .andExpect(jsonPath("$[0].note", is("Đến muộn")));
        }

        @Test
        @DisplayName("AC FR-4: studentId không enrolled → 422 STUDENT_NOT_ENROLLED, toàn bộ rollback")
        void bulkUpdate_studentNotEnrolled_422_rollback() throws Exception {
            var body = Map.of("records", List.of(
                    Map.of("studentId", student1.getId(), "status", "PRESENT"),   // hợp lệ
                    Map.of("studentId", studentNotInClass.getId(), "status", "PRESENT") // KHÔNG enrolled
            ));

            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code", is("STUDENT_NOT_ENROLLED")));

            // Xác nhận rollback: không có record nào được lưu
            mockMvc.perform(get("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Body rỗng records → 400 Validation")
        void bulkUpdate_emptyRecords_400() throws Exception {
            var body = Map.of("records", List.of());

            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TUTOR khác cố ghi → 404")
        void bulkUpdate_otherTutor_404() throws Exception {
            var body = Map.of("records", List.of(
                    Map.of("studentId", student1.getId(), "status", "PRESENT")
            ));

            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorOther)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("STUDENT cố ghi → 403")
        void bulkUpdate_student_403() throws Exception {
            var body = Map.of("records", List.of(
                    Map.of("studentId", student1.getId(), "status", "PRESENT")
            ));

            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── GET /students/{studentId}/attendance ───────────────────────────────────

    @Nested
    @DisplayName("3. GET /students/{studentId}/attendance")
    class GetAttendanceHistoryTests {

        @BeforeEach
        void seedAttendance() throws Exception {
            // Tạo điểm danh cho student1
            var body = Map.of("records", List.of(
                    Map.of("studentId", student1.getId(), "status", "PRESENT"),
                    Map.of("studentId", student2.getId(), "status", "ABSENT_EXCUSED")
            ));
            mockMvc.perform(put("/api/v1/sessions/{id}/attendance", session.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TUTOR xem lịch sử của student1 (có classId) → 200")
        void history_tutor_success() throws Exception {
            mockMvc.perform(get("/api/v1/students/{id}/attendance", student1.getId())
                            .param("classId", clazz.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PRESENT")))
                    .andExpect(jsonPath("$[0].classId", is(clazz.getId().intValue())));
        }

        @Test
        @DisplayName("STUDENT xem lịch sử của chính mình → 200")
        void history_student_self() throws Exception {
            mockMvc.perform(get("/api/v1/students/{id}/attendance", student1.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("STUDENT xem lịch sử học sinh khác → 404")
        void history_student_otherStudent_404() throws Exception {
            mockMvc.perform(get("/api/v1/students/{id}/attendance", student2.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PARENT xem lịch sử của con → 200")
        void history_parent_child() throws Exception {
            mockMvc.perform(get("/api/v1/students/{id}/attendance", student1.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenParent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("PARENT xem học sinh không phải con → 404")
        void history_parent_nonChild_404() throws Exception {
            mockMvc.perform(get("/api/v1/students/{id}/attendance", student2.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenParent))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TUTOR xem mà thiếu classId → 422")
        void history_tutor_missingClassId_422() throws Exception {
            mockMvc.perform(get("/api/v1/students/{id}/attendance", student1.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User createUser(String email, String fullName, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName(fullName)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
