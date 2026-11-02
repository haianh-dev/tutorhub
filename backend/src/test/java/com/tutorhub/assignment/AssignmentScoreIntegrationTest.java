package com.tutorhub.assignment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.tutorhub.assignment.dto.BulkUpdateScoresRequest;
import com.tutorhub.assignment.dto.ScoreItemRequest;
import com.tutorhub.assignment.entity.AssignmentEntity;
import com.tutorhub.assignment.entity.AssignmentScoreEntity;
import com.tutorhub.assignment.entity.AssignmentType;
import com.tutorhub.assignment.entity.SubmissionStatus;
import com.tutorhub.assignment.repository.AssignmentRepository;
import com.tutorhub.assignment.repository.AssignmentScoreRepository;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

/**
 * Integration Test cho Task T5.2 — Nhập điểm và nhận xét bulk (thang điểm 10 cố định).
 *
 * <p>Tiêu chí nghiệm thu:</p>
 * <ul>
 *   <li>Bulk update điểm & feedback thành công</li>
 *   <li>Test biên điểm số: 0, 10 (hợp lệ); -0.1, 10.1 (lỗi 400 validation)</li>
 *   <li>Transaction rollback: nếu có studentId không thuộc lớp -> 422 STUDENT_NOT_ENROLLED và không lưu bản ghi nào</li>
 *   <li>Phân quyền: TUTOR khác -> 404; STUDENT cố PUT -> 403; STUDENT/PARENT chỉ xem điểm học sinh liên quan</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AssignmentScoreIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentScoreRepository assignmentScoreRepository;
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
    private AssignmentEntity assignment;
    private AssignmentScoreEntity score1;
    private AssignmentScoreEntity score2;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM assignment_scores");
        jdbcTemplate.execute("DELETE FROM assignments");
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

        tutor             = createUser("tutor@example.com",        "Gia Sư Toán",       Role.TUTOR);
        tutorOther        = createUser("tutor2@example.com",       "Gia Sư Khác",       Role.TUTOR);
        student1          = createUser("student1@example.com",     "Học Sinh 1",        Role.STUDENT);
        student2          = createUser("student2@example.com",     "Học Sinh 2",        Role.STUDENT);
        studentNotInClass = createUser("stranger@example.com",     "Học Sinh Ngoài",    Role.STUDENT);
        parent            = createUser("parent@example.com",       "Phụ Huynh",         Role.PARENT);

        tokenTutor      = jwtService.generateAccessToken(tutor);
        tokenTutorOther = jwtService.generateAccessToken(tutorOther);
        tokenStudent1   = jwtService.generateAccessToken(student1);
        tokenStudent2   = jwtService.generateAccessToken(student2);
        tokenParent     = jwtService.generateAccessToken(parent);

        clazz = classRepository.save(ClassEntity.builder()
                .name("Toán Lớp 10A")
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

        // Tạo bài tập và score rows ban đầu cho student1 & student2
        assignment = assignmentRepository.save(AssignmentEntity.builder()
                .classroom(clazz)
                .title("Kiểm tra 15 phút Đại số")
                .description("Hàm số bậc nhất và bậc hai")
                .assignmentType(AssignmentType.QUIZ)
                .dueAt(Instant.parse("2026-11-15T10:00:00Z"))
                .build());

        score1 = assignmentScoreRepository.save(AssignmentScoreEntity.builder()
                .assignment(assignment)
                .student(student1)
                .score(null)
                .status(SubmissionStatus.ASSIGNED)
                .feedback(null)
                .build());

        score2 = assignmentScoreRepository.save(AssignmentScoreEntity.builder()
                .assignment(assignment)
                .student(student2)
                .score(null)
                .status(SubmissionStatus.ASSIGNED)
                .feedback(null)
                .build());
    }

    // ─── 1. GET /assignments/{id}/scores ───────────────────────────────────────

    @Nested
    @DisplayName("1. GET /assignments/{id}/scores — Xem bảng điểm bài tập")
    class GetScoresTests {

        @Test
        @DisplayName("TUTOR phụ trách xem được toàn bộ bảng điểm của bài tập")
        void getScores_tutorOwner_success() throws Exception {
            mockMvc.perform(get("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].assignmentId", is(assignment.getId().intValue())))
                    .andExpect(jsonPath("$[0].assignmentTitle", is("Kiểm tra 15 phút Đại số")));
        }

        @Test
        @DisplayName("TUTOR khác cố xem bảng điểm → 404 Not Found")
        void getScores_otherTutor_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorOther))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("STUDENT cố xem bảng điểm cả lớp → 403 Forbidden")
        void getScores_student_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── 2. PUT /assignments/{id}/scores ───────────────────────────────────────

    @Nested
    @DisplayName("2. PUT /assignments/{id}/scores — Bulk update điểm & nhận xét")
    class BulkUpdateScoresTests {

        @Test
        @DisplayName("Nhập điểm và nhận xét bulk thành công cho cả lớp")
        void bulkUpdateScores_success() throws Exception {
            BulkUpdateScoresRequest request = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("9.50"), SubmissionStatus.GRADED, "Làm bài rất tốt, trình bày rõ ràng"),
                    new ScoreItemRequest(student2.getId(), new BigDecimal("8.00"), SubmissionStatus.GRADED, "Cần chú ý câu hỏi phụ")
            ));

            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            // Kiểm tra DB
            AssignmentScoreEntity updated1 = assignmentScoreRepository.findByAssignmentIdAndStudentId(assignment.getId(), student1.getId()).orElseThrow();
            assertEquals(new BigDecimal("9.50"), updated1.getScore());
            assertEquals(SubmissionStatus.GRADED, updated1.getStatus());
            assertEquals("Làm bài rất tốt, trình bày rõ ràng", updated1.getFeedback());
            assertNotNull(updated1.getGradedAt());

            AssignmentScoreEntity updated2 = assignmentScoreRepository.findByAssignmentIdAndStudentId(assignment.getId(), student2.getId()).orElseThrow();
            assertEquals(new BigDecimal("8.00"), updated2.getScore());
            assertEquals(SubmissionStatus.GRADED, updated2.getStatus());
            assertNotNull(updated2.getGradedAt());
        }

        @Test
        @DisplayName("Cập nhật lại lần 2 (upsert/update điểm) thành công")
        void bulkUpdateScores_updateAgain_success() throws Exception {
            // Lần 1: Chấm điểm 7.0
            BulkUpdateScoresRequest request1 = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("7.00"), SubmissionStatus.GRADED, "Cần cố gắng")
            ));
            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isOk());

            // Lần 2: Phúc khảo sửa thành 8.5
            BulkUpdateScoresRequest request2 = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("8.50"), SubmissionStatus.GRADED, "Đã chấm lại câu 3")
            ));
            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isOk());

            AssignmentScoreEntity updated = assignmentScoreRepository.findByAssignmentIdAndStudentId(assignment.getId(), student1.getId()).orElseThrow();
            assertEquals(new BigDecimal("8.50"), updated.getScore());
            assertEquals("Đã chấm lại câu 3", updated.getFeedback());
        }

        @Test
        @DisplayName("Test biên điểm hợp lệ: score = 0.00 và score = 10.00 → 200 OK")
        void bulkUpdateScores_boundaryScores_valid_success() throws Exception {
            BulkUpdateScoresRequest request = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("0.00"), SubmissionStatus.GRADED, "Không làm được bài"),
                    new ScoreItemRequest(student2.getId(), new BigDecimal("10.00"), SubmissionStatus.GRADED, "Xuất sắc tuyệt đối")
            ));

            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            AssignmentScoreEntity updated1 = assignmentScoreRepository.findByAssignmentIdAndStudentId(assignment.getId(), student1.getId()).orElseThrow();
            assertEquals(new BigDecimal("0.00"), updated1.getScore());

            AssignmentScoreEntity updated2 = assignmentScoreRepository.findByAssignmentIdAndStudentId(assignment.getId(), student2.getId()).orElseThrow();
            assertEquals(new BigDecimal("10.00"), updated2.getScore());
        }

        @Test
        @DisplayName("Test biên điểm không hợp lệ: score = -0.1 → 400 Bad Request")
        void bulkUpdateScores_negativeScore_returns400() throws Exception {
            BulkUpdateScoresRequest request = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("-0.1"), SubmissionStatus.GRADED, "Điểm âm")
            ));

            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Test biên điểm không hợp lệ: score = 10.1 → 400 Bad Request")
        void bulkUpdateScores_greaterThan10_returns400() throws Exception {
            BulkUpdateScoresRequest request = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("10.1"), SubmissionStatus.GRADED, "Điểm quá 10")
            ));

            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("StudentId không thuộc lớp → 422 STUDENT_NOT_ENROLLED & Rollback toàn bộ transaction")
        void bulkUpdateScores_studentNotInClass_rollbackAll() throws Exception {
            BulkUpdateScoresRequest request = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("9.00"), SubmissionStatus.GRADED, "Học sinh hợp lệ"),
                    new ScoreItemRequest(studentNotInClass.getId(), new BigDecimal("7.00"), SubmissionStatus.GRADED, "Học sinh ngoài lớp")
            ));

            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code", is("STUDENT_NOT_ENROLLED")));

            // Xác nhận student1 KHÔNG bị đổi điểm (được rollback hoàn toàn)
            AssignmentScoreEntity revertedScore1 = assignmentScoreRepository.findByAssignmentIdAndStudentId(assignment.getId(), student1.getId()).orElseThrow();
            assertNull(revertedScore1.getScore());
            assertEquals(SubmissionStatus.ASSIGNED, revertedScore1.getStatus());
        }

        @Test
        @DisplayName("TUTOR khác cố sửa điểm → 404 Not Found")
        void bulkUpdateScores_otherTutor_returns404() throws Exception {
            BulkUpdateScoresRequest request = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("9.00"), SubmissionStatus.GRADED, null)
            ));

            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorOther)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("STUDENT cố sửa điểm → 403 Forbidden")
        void bulkUpdateScores_student_returns403() throws Exception {
            BulkUpdateScoresRequest request = new BulkUpdateScoresRequest(List.of(
                    new ScoreItemRequest(student1.getId(), new BigDecimal("10.00"), SubmissionStatus.GRADED, null)
            ));

            mockMvc.perform(put("/api/v1/assignments/{id}/scores", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── 3. GET /students/{studentId}/scores ───────────────────────────────────

    @Nested
    @DisplayName("3. GET /students/{studentId}/scores — Xem điểm theo quyền học sinh")
    class GetStudentScoresTests {

        @BeforeEach
        void gradeStudent1() {
            score1.setScore(new BigDecimal("9.00"));
            score1.setStatus(SubmissionStatus.GRADED);
            score1.setFeedback("Bài làm tốt");
            score1.setGradedAt(Instant.now());
            assignmentScoreRepository.save(score1);
        }

        @Test
        @DisplayName("STUDENT xem được điểm của chính mình")
        void getStudentScores_ownStudent_success() throws Exception {
            mockMvc.perform(get("/api/v1/students/{studentId}/scores", student1.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].studentId", is(student1.getId().intValue())))
                    .andExpect(jsonPath("$[0].score", is(9.0)))
                    .andExpect(jsonPath("$[0].status", is("GRADED")));
        }

        @Test
        @DisplayName("STUDENT cố xem điểm của học sinh khác → 404 Not Found")
        void getStudentScores_otherStudent_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/students/{studentId}/scores", student1.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent2))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PARENT xem được điểm của con mình")
        void getStudentScores_parent_success() throws Exception {
            mockMvc.perform(get("/api/v1/students/{studentId}/scores", student1.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenParent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].score", is(9.0)));
        }

        @Test
        @DisplayName("PARENT cố xem điểm của học sinh không phải con mình → 404 Not Found")
        void getStudentScores_parentOtherChild_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/students/{studentId}/scores", student2.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenParent))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TUTOR xem điểm của học sinh trong lớp mình kèm classId thành công")
        void getStudentScores_tutorWithClassId_success() throws Exception {
            mockMvc.perform(get("/api/v1/students/{studentId}/scores", student1.getId())
                            .param("classId", clazz.getId().toString())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].score", is(9.0)));
        }

        @Test
        @DisplayName("TUTOR không truyền classId → 400 VALIDATION_ERROR")
        void getStudentScores_tutorWithoutClassId_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/students/{studentId}/scores", student1.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────────

    private User createUser(String email, String fullName, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
