package com.tutorhub.assignment;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.assignment.dto.CreateAssignmentRequest;
import com.tutorhub.assignment.dto.UpdateAssignmentRequest;
import com.tutorhub.assignment.entity.AssignmentEntity;
import com.tutorhub.assignment.entity.AssignmentScoreEntity;
import com.tutorhub.assignment.entity.AssignmentType;
import com.tutorhub.assignment.entity.SubmissionStatus;
import com.tutorhub.assignment.repository.AssignmentRepository;
import com.tutorhub.assignment.repository.AssignmentScoreRepository;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.classroom.dto.EnrollStudentRequest;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.classroom.service.ClassService;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

/**
 * Integration Test cho Task T5.1 — Quản lý Bài tập và Thang điểm 10 cố định (D-30).
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AssignmentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentScoreRepository assignmentScoreRepository;
    @Autowired private ClassService classService;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User tutor;
    private User tutorOther;
    private User student1;
    private User student2;
    private User studentNew;
    private User parent;

    private String tokenTutor;
    private String tokenTutorOther;
    private String tokenStudent1;
    private String tokenStudent2;
    private String tokenStudentNew;
    private String tokenParent;

    private ClassEntity clazz;

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

        tutor           = createUser("tutor@example.com",        "Gia Sư Toán",       Role.TUTOR);
        tutorOther      = createUser("tutor2@example.com",       "Gia Sư Khác",       Role.TUTOR);
        student1        = createUser("student1@example.com",     "Học Sinh 1",        Role.STUDENT);
        student2        = createUser("student2@example.com",     "Học Sinh 2",        Role.STUDENT);
        studentNew      = createUser("studentnew@example.com",   "Học Sinh Mới",      Role.STUDENT);
        parent          = createUser("parent@example.com",       "Phụ Huynh",         Role.PARENT);

        tokenTutor      = jwtService.generateAccessToken(tutor);
        tokenTutorOther = jwtService.generateAccessToken(tutorOther);
        tokenStudent1   = jwtService.generateAccessToken(student1);
        tokenStudent2   = jwtService.generateAccessToken(student2);
        tokenStudentNew = jwtService.generateAccessToken(studentNew);
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
    }

    // ─── 1. POST /classes/{classId}/assignments ─────────────────────────────────

    @Nested
    @DisplayName("1. POST /classes/{classId}/assignments — Tạo bài tập")
    class CreateAssignmentTests {

        @Test
        @DisplayName("Tạo bài tập thành công → 201 Created & tự sinh score rows cho toàn bộ học sinh ACTIVE")
        void createAssignment_success_createsScoresForActiveStudents() throws Exception {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    "Bài tập Đại số 1",
                    "Giải các bài tập phương trình bậc hai trang 24",
                    AssignmentType.HOMEWORK,
                    Instant.parse("2026-10-25T17:00:00Z")
            );

            mockMvc.perform(post("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.title", is("Bài tập Đại số 1")))
                    .andExpect(jsonPath("$.type", is("HOMEWORK")));

            // Kiểm tra DB có đúng 2 score rows dạng ASSIGNED, score null
            List<AssignmentScoreEntity> scores = assignmentScoreRepository.findAll();
            assertEquals(2, scores.size());
            assertTrue(scores.stream().allMatch(s -> s.getStatus() == SubmissionStatus.ASSIGNED));
            assertTrue(scores.stream().allMatch(s -> s.getScore() == null));
        }

        @Test
        @DisplayName("Học sinh mới ghi danh sau khi bài tập đã tạo → Tự động tạo score row cho học sinh mới (FR-5.2)")
        void enrollStudentLater_createsScoreForNewStudent() throws Exception {
            // Bước 1: Tạo bài tập
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    "Kiểm tra 15 phút",
                    "Trắc nghiệm hàm số",
                    AssignmentType.QUIZ,
                    Instant.parse("2026-10-22T17:00:00Z")
            );

            mockMvc.perform(post("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            assertEquals(2, assignmentScoreRepository.findAll().size());

            // Bước 2: Thêm học sinh studentNew vào lớp qua classService
            classService.enrollStudent(tutor.getId(), Role.TUTOR, clazz.getId(), new EnrollStudentRequest(studentNew.getId()));

            // Bước 3: Kiểm tra assignment_scores đã tăng lên 3
            List<AssignmentScoreEntity> updatedScores = assignmentScoreRepository.findAll();
            assertEquals(3, updatedScores.size());
            assertTrue(updatedScores.stream().anyMatch(s -> s.getStudent().getId().equals(studentNew.getId())));
        }

        @Test
        @DisplayName("Tutor khác cố tạo bài tập cho lớp → 404")
        void createAssignment_tutorOther_returns404() throws Exception {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    "Bài tập", null, AssignmentType.HOMEWORK, null
            );

            mockMvc.perform(post("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorOther)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Student cố tạo bài tập → 403 Forbidden")
        void createAssignment_studentRole_returns403() throws Exception {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    "Bài tập", null, AssignmentType.HOMEWORK, null
            );

            mockMvc.perform(post("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Thiếu thông tin bắt buộc (title blank) → 400 Bad Request")
        void createAssignment_blankTitle_returns400() throws Exception {
            CreateAssignmentRequest request = new CreateAssignmentRequest(
                    "   ", null, AssignmentType.HOMEWORK, null
            );

            mockMvc.perform(post("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── 2. GET /classes/{classId}/assignments & GET /assignments/{id} ───────────

    @Nested
    @DisplayName("2. GET Bài tập — Phân quyền xem")
    class GetAssignmentTests {

        private AssignmentEntity assignment;

        @BeforeEach
        void initAssignment() {
            assignment = assignmentRepository.save(AssignmentEntity.builder()
                    .classroom(clazz)
                    .title("Đề ôn thi giữa kỳ")
                    .assignmentType(AssignmentType.MOCK_TEST)
                    .dueAt(Instant.parse("2026-11-01T12:00:00Z"))
                    .build());
        }

        @Test
        @DisplayName("Tutor và Student trong lớp xem được danh sách bài tập của lớp")
        void getAssignmentsByClass_tutorAndEnrolledStudent_success() throws Exception {
            // Tutor xem
            mockMvc.perform(get("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title", is("Đề ôn thi giữa kỳ")));

            // Student1 trong lớp xem
            mockMvc.perform(get("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudent1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            // Parent của Student1 xem
            mockMvc.perform(get("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenParent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Student ngoài lớp không xem được bài tập của lớp khác → 404")
        void getAssignmentsByClass_strangerStudent_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/classes/{classId}/assignments", clazz.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStudentNew))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Xem chi tiết một bài tập thành công")
        void getAssignmentDetail_success() throws Exception {
            mockMvc.perform(get("/api/v1/assignments/{id}", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(assignment.getId().intValue())))
                    .andExpect(jsonPath("$.title", is("Đề ôn thi giữa kỳ")))
                    .andExpect(jsonPath("$.type", is("MOCK_TEST")));
        }
    }

    // ─── 3. PUT /assignments/{id} ───────────────────────────────────────────────

    @Nested
    @DisplayName("3. PUT /assignments/{id} — Cập nhật bài tập")
    class UpdateAssignmentTests {

        private AssignmentEntity assignment;

        @BeforeEach
        void initAssignment() {
            assignment = assignmentRepository.save(AssignmentEntity.builder()
                    .classroom(clazz)
                    .title("Bài tập Hình học")
                    .assignmentType(AssignmentType.HOMEWORK)
                    .build());
        }

        @Test
        @DisplayName("Tutor cập nhật bài tập thành công")
        void updateAssignment_success() throws Exception {
            UpdateAssignmentRequest updateRequest = new UpdateAssignmentRequest(
                    "Bài tập Hình học Không gian",
                    "Vẽ hình chóp tứ giác đều",
                    AssignmentType.EXAM,
                    Instant.parse("2026-11-10T12:00:00Z")
            );

            mockMvc.perform(put("/api/v1/assignments/{id}", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Bài tập Hình học Không gian")))
                    .andExpect(jsonPath("$.type", is("EXAM")));
        }

        @Test
        @DisplayName("Tutor khác cố cập nhật → 404")
        void updateAssignment_otherTutor_returns404() throws Exception {
            UpdateAssignmentRequest updateRequest = new UpdateAssignmentRequest(
                    "Hack title", null, AssignmentType.OTHER, null
            );

            mockMvc.perform(put("/api/v1/assignments/{id}", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorOther)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── 4. DELETE /assignments/{id} ────────────────────────────────────────────

    @Nested
    @DisplayName("4. DELETE /assignments/{id} — Xóa bài tập")
    class DeleteAssignmentTests {

        private AssignmentEntity assignment;

        @BeforeEach
        void initAssignment() {
            assignment = assignmentRepository.save(AssignmentEntity.builder()
                    .classroom(clazz)
                    .title("Bài tập cần xóa")
                    .assignmentType(AssignmentType.HOMEWORK)
                    .build());
        }

        @Test
        @DisplayName("Chưa có điểm đã chấm → Xóa thành công (204 No Content)")
        void deleteAssignment_noGrades_success() throws Exception {
            // Tạo 1 bản ghi điểm chưa chấm (score null, ASSIGNED)
            assignmentScoreRepository.save(AssignmentScoreEntity.builder()
                    .assignment(assignment)
                    .student(student1)
                    .status(SubmissionStatus.ASSIGNED)
                    .score(null)
                    .build());

            mockMvc.perform(delete("/api/v1/assignments/{id}", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isNoContent());

            assertFalse(assignmentRepository.existsById(assignment.getId()));
            assertEquals(0, assignmentScoreRepository.count());
        }

        @Test
        @DisplayName("Đã có điểm đã chấm → Từ chối xóa với lỗi 422 ASSIGNMENT_HAS_GRADES")
        void deleteAssignment_hasGradedScores_returns422() throws Exception {
            // Tạo 1 bản ghi điểm đã chấm (score 9.5, GRADED)
            assignmentScoreRepository.save(AssignmentScoreEntity.builder()
                    .assignment(assignment)
                    .student(student1)
                    .status(SubmissionStatus.GRADED)
                    .score(new java.math.BigDecimal("9.50"))
                    .build());

            mockMvc.perform(delete("/api/v1/assignments/{id}", assignment.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutor))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code", is("ASSIGNMENT_HAS_GRADES")));

            assertTrue(assignmentRepository.existsById(assignment.getId()));
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
