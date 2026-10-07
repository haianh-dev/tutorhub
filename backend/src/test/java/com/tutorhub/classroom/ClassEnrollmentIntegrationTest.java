package com.tutorhub.classroom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.classroom.dto.EnrollStudentRequest;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test toàn diện cho Task T2.2:
 * Ghi danh/bỏ học sinh; danh sách học sinh của lớp; quy tắc lớp 1:1 (tối đa 1) và cảnh báo lớp nhóm dưới 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClassEnrollmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private User adminUser;
    private User tutorA;
    private User tutorB;
    private User student1;
    private User student2;
    private User student3;
    private User parentA;

    private String adminToken;
    private String tutorAToken;
    private String tutorBToken;
    private String student1Token;
    private String parentAToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM invitations");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM parent_students");
        jdbcTemplate.update("DELETE FROM class_enrollments");
        jdbcTemplate.update("DELETE FROM classes");
        userRepository.deleteAll();

        adminUser = createUser("admin@example.com", "Admin", Role.ADMIN);
        tutorA = createUser("tutor-a@example.com", "Gia sư A", Role.TUTOR);
        tutorB = createUser("tutor-b@example.com", "Gia sư B", Role.TUTOR);
        student1 = createUser("student-1@example.com", "Học sinh 1", Role.STUDENT);
        student2 = createUser("student-2@example.com", "Học sinh 2", Role.STUDENT);
        student3 = createUser("student-3@example.com", "Học sinh 3", Role.STUDENT);
        parentA = createUser("parent-a@example.com", "Phụ huynh A", Role.PARENT);

        adminToken = jwtService.generateAccessToken(adminUser);
        tutorAToken = jwtService.generateAccessToken(tutorA);
        tutorBToken = jwtService.generateAccessToken(tutorB);
        student1Token = jwtService.generateAccessToken(student1);
        parentAToken = jwtService.generateAccessToken(parentA);
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

    private Long insertClass(Long tutorId, String name, String subject, ClassType type, String status) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO classes (tutor_id, name, subject, class_type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, tutorId, name, subject, type.name(), status);
    }

    private void insertEnrollment(Long classId, Long studentId, EnrollmentStatus status) {
        jdbcTemplate.update("""
                INSERT INTO class_enrollments (class_id, student_id, status)
                VALUES (?, ?, ?)
                """, classId, studentId, status.name());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Ghi danh học sinh thành công (TUTOR & ADMIN)
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("1. Ghi danh thành công (TUTOR & ADMIN)")
    class SuccessfulEnrollmentTests {

        @Test
        @DisplayName("TUTOR ghi danh học sinh vào lớp của mình → 201 Created")
        void tutorEnrollsStudentSuccessfully() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Toán 12A", "Toán", ClassType.GROUP, "ACTIVE");

            EnrollStudentRequest request = new EnrollStudentRequest(student1.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.classId", is(classId.intValue())))
                    .andExpect(jsonPath("$.studentId", is(student1.getId().intValue())))
                    .andExpect(jsonPath("$.studentName", is("Học sinh 1")))
                    .andExpect(jsonPath("$.studentEmail", is("student-1@example.com")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.enrolledAt").isNotEmpty());

            // Kiểm tra sĩ số lớp tăng lên 1
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentCount", is(1)));
        }

        @Test
        @DisplayName("ADMIN ghi danh học sinh vào lớp của bất kỳ gia sư nào → 201 Created")
        void adminEnrollsStudentSuccessfully() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Lý 11", "Lý", ClassType.GROUP, "ACTIVE");

            EnrollStudentRequest request = new EnrollStudentRequest(student2.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.studentId", is(student2.getId().intValue())))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Ghi danh trùng lặp → 409 ENROLLMENT_DUPLICATE
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("2. Ghi danh trùng lặp → 409")
    class DuplicateEnrollmentTests {

        @Test
        @DisplayName("Ghi danh học sinh đã có enrollment ACTIVE trong lớp → 409 ENROLLMENT_DUPLICATE")
        void enrollingAlreadyActiveStudentReturns409() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Hóa 10", "Hóa", ClassType.GROUP, "ACTIVE");
            insertEnrollment(classId, student1.getId(), EnrollmentStatus.ACTIVE);

            EnrollStudentRequest request = new EnrollStudentRequest(student1.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("ENROLLMENT_DUPLICATE")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Quy tắc lớp 1:1 (ONE_ON_ONE) — Tối đa 1 học sinh ACTIVE
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("3. Quy tắc lớp 1:1 (ONE_ON_ONE)")
    class OneOnOneClassRulesTests {

        @Test
        @DisplayName("Lớp 1:1 trống: thêm học sinh thứ nhất → 201 Created")
        void addingFirstStudentToOneOnOneClassSucceeds() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Toán 1:1", "Toán", ClassType.ONE_ON_ONE, "ACTIVE");

            EnrollStudentRequest request = new EnrollStudentRequest(student1.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Lớp 1:1 đã có 1 học sinh ACTIVE: thêm học sinh thứ hai → 422 ONE_ON_ONE_FULL")
        void addingSecondStudentToOneOnOneClassFailsWith422() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Toán 1:1", "Toán", ClassType.ONE_ON_ONE, "ACTIVE");
            insertEnrollment(classId, student1.getId(), EnrollmentStatus.ACTIVE);

            EnrollStudentRequest request = new EnrollStudentRequest(student2.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code", is("ONE_ON_ONE_FULL")));
        }

        @Test
        @DisplayName("Lớp 1:1: học sinh 1 đã LEFT rồi thêm học sinh 2 → thành công 201 Created")
        void addingNewStudentAfterPreviousStudentLeftSucceeds() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Toán 1:1", "Toán", ClassType.ONE_ON_ONE, "ACTIVE");
            insertEnrollment(classId, student1.getId(), EnrollmentStatus.LEFT);

            EnrollStudentRequest request = new EnrollStudentRequest(student2.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.studentId", is(student2.getId().intValue())))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Hai request đồng thời thêm 2 học sinh vào lớp 1:1 trống → Đúng 1 thành công 201, 1 thất bại 422")
        void concurrentEnrollmentToOneOnOneClassAllowsExactlyOne() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp 1:1 Cạnh tranh", "Anh", ClassType.ONE_ON_ONE, "ACTIVE");

            int threadCount = 2;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictOrFullCount = new AtomicInteger(0);

            List<Future<?>> futures = new ArrayList<>();

            // Luồng 1 thêm student1
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    EnrollStudentRequest req = new EnrollStudentRequest(student1.getId());
                    MvcResult res = mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int statusCode = res.getResponse().getStatus();
                    if (statusCode == 201) successCount.incrementAndGet();
                    else if (statusCode == 422) conflictOrFullCount.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            // Luồng 2 thêm student2
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    EnrollStudentRequest req = new EnrollStudentRequest(student2.getId());
                    MvcResult res = mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int statusCode = res.getResponse().getStatus();
                    if (statusCode == 201) successCount.incrementAndGet();
                    else if (statusCode == 422) conflictOrFullCount.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown(); // Phát lệnh chạy đồng thời!

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(conflictOrFullCount.get()).isEqualTo(1);

            // Kiểm tra sĩ số thực tế trong DB đúng bằng 1
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_enrollments WHERE class_id = ? AND status = 'ACTIVE'",
                    Integer.class, classId);
            assertThat(count).isEqualTo(1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Bỏ học sinh (DELETE) & Ghi danh lại
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("4. Bỏ học sinh & Tái ghi danh")
    class RemoveAndReEnrollTests {

        @Test
        @DisplayName("DELETE /classes/{id}/students/{studentId} đặt status=LEFT thành công → 204")
        void removeStudentSetsStatusLeft() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Văn 12", "Văn", ClassType.GROUP, "ACTIVE");
            insertEnrollment(classId, student1.getId(), EnrollmentStatus.ACTIVE);

            mockMvc.perform(delete("/api/v1/classes/{id}/students/{studentId}", classId, student1.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isNoContent());

            String statusInDb = jdbcTemplate.queryForObject(
                    "SELECT status FROM class_enrollments WHERE class_id = ? AND student_id = ?",
                    String.class, classId, student1.getId());
            assertThat(statusInDb).isEqualTo("LEFT");

            // Sĩ số lớp giảm về 0
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentCount", is(0)));
        }

        @Test
        @DisplayName("Bỏ học sinh rồi ghi danh lại chính học sinh đó → Tái kích hoạt ACTIVE thành công (201)")
        void reEnrollingStudentAfterLeftSucceeds() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Sinh 11", "Sinh", ClassType.GROUP, "ACTIVE");
            insertEnrollment(classId, student1.getId(), EnrollmentStatus.ACTIVE);

            // 1. Cho học sinh rời lớp
            mockMvc.perform(delete("/api/v1/classes/{id}/students/{studentId}", classId, student1.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isNoContent());

            // 2. Ghi danh lại
            EnrollStudentRequest request = new EnrollStudentRequest(student1.getId());
            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));

            String statusInDb = jdbcTemplate.queryForObject(
                    "SELECT status FROM class_enrollments WHERE class_id = ? AND student_id = ?",
                    String.class, classId, student1.getId());
            assertThat(statusInDb).isEqualTo("ACTIVE");

            // Chỉ có đúng 1 dòng bản ghi trong bảng class_enrollments do UNIQUE constraint
            Integer rowCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_enrollments WHERE class_id = ? AND student_id = ?",
                    Integer.class, classId, student1.getId());
            assertThat(rowCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Xóa học sinh không tồn tại trong lớp hoặc đã LEFT → 404 RESOURCE_NOT_FOUND")
        void removingNonEnrolledStudentReturns404() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Địa 10", "Địa", ClassType.GROUP, "ACTIVE");

            mockMvc.perform(delete("/api/v1/classes/{id}/students/{studentId}", classId, 9999L)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Cảnh báo lớp nhóm dưới 2 học sinh & Thêm nhiều học sinh
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("5. Lớp nhóm (GROUP) & Cảnh báo dưới 2 học sinh")
    class GroupClassRulesTests {

        @Test
        @DisplayName("Lớp GROUP có < 2 học sinh trả warnings trong ClassResponse; ≥ 2 học sinh warnings rỗng")
        void groupClassShowsWarningsWhenUnderTwoStudents() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Nhóm Tin", "Tin học", ClassType.GROUP, "ACTIVE");

            // Chưa có học sinh nào (< 2) → có warning
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentCount", is(0)))
                    .andExpect(jsonPath("$.warnings", hasSize(1)))
                    .andExpect(jsonPath("$.warnings[0]", is("Lớp nhóm hiện có ít hơn 2 học sinh")));

            // Thêm 1 học sinh (vẫn < 2) → có warning
            insertEnrollment(classId, student1.getId(), EnrollmentStatus.ACTIVE);
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentCount", is(1)))
                    .andExpect(jsonPath("$.warnings", hasSize(1)));

            // Thêm học sinh thứ 2 (đủ 2) → warnings rỗng
            insertEnrollment(classId, student2.getId(), EnrollmentStatus.ACTIVE);
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentCount", is(2)))
                    .andExpect(jsonPath("$.warnings", hasSize(0)));

            // Thêm học sinh thứ 3 thành công không giới hạn
            insertEnrollment(classId, student3.getId(), EnrollmentStatus.ACTIVE);
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentCount", is(3)))
                    .andExpect(jsonPath("$.warnings", hasSize(0)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Danh sách học sinh của lớp (GET /classes/{id}/students)
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("6. Danh sách học sinh ghi danh")
    class GetStudentsListTests {

        @Test
        @DisplayName("Lấy danh sách học sinh: lọc theo status hoặc lấy tất cả")
        void getStudentsWithAndWithoutStatusFilter() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Sử 11", "Lịch Sử", ClassType.GROUP, "ACTIVE");
            insertEnrollment(classId, student1.getId(), EnrollmentStatus.ACTIVE);
            insertEnrollment(classId, student2.getId(), EnrollmentStatus.LEFT);

            // Không truyền status → trả cả 2
            mockMvc.perform(get("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            // Lọc status=ACTIVE → trả 1
            mockMvc.perform(get("/api/v1/classes/{id}/students?status=ACTIVE", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].studentId", is(student1.getId().intValue())))
                    .andExpect(jsonPath("$[0].status", is("ACTIVE")));

            // Lọc status=LEFT → trả 1
            mockMvc.perform(get("/api/v1/classes/{id}/students?status=LEFT", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].studentId", is(student2.getId().intValue())))
                    .andExpect(jsonPath("$[0].status", is("LEFT")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. Bảo mật, Phân quyền & Validation
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("7. Bảo mật & Validation")
    class SecurityAndValidationTests {

        @Test
        @DisplayName("TUTOR_B thao tác trên lớp của TUTOR_A → 404 RESOURCE_NOT_FOUND (không lộ thông tin)")
        void tutorBCannotAccessTutorAClassStudents() throws Exception {
            Long classA = insertClass(tutorA.getId(), "Lớp Toán A", "Toán", ClassType.GROUP, "ACTIVE");

            // TUTOR_B thêm học sinh vào lớp của TUTOR_A → 404
            EnrollStudentRequest request = new EnrollStudentRequest(student1.getId());
            mockMvc.perform(post("/api/v1/classes/{id}/students", classA)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));

            // TUTOR_B xem danh sách học sinh lớp của TUTOR_A → 404
            mockMvc.perform(get("/api/v1/classes/{id}/students", classA)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));

            // TUTOR_B xóa học sinh lớp của TUTOR_A → 404
            mockMvc.perform(delete("/api/v1/classes/{id}/students/{studentId}", classA, student1.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
        }

        @Test
        @DisplayName("STUDENT / PARENT gọi API ghi danh/bỏ học sinh → 403 AUTH_ACCESS_DENIED")
        void studentAndParentForbiddenToManageEnrollments() throws Exception {
            Long classA = insertClass(tutorA.getId(), "Lớp Toán A", "Toán", ClassType.GROUP, "ACTIVE");
            EnrollStudentRequest request = new EnrollStudentRequest(student2.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classA)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("AUTH_ACCESS_DENIED")));

            mockMvc.perform(post("/api/v1/classes/{id}/students", classA)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("AUTH_ACCESS_DENIED")));
        }

        @Test
        @DisplayName("Không truyền token → 401 AUTH_TOKEN_INVALID")
        void unauthenticatedAccessReturns401() throws Exception {
            EnrollStudentRequest request = new EnrollStudentRequest(student1.getId());
            mockMvc.perform(post("/api/v1/classes/{id}/students", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("AUTH_TOKEN_INVALID")));
        }

        @Test
        @DisplayName("Thêm tài khoản không phải vai trò STUDENT (vd: TUTOR) → 400 VALIDATION_ERROR")
        void enrollingNonStudentAccountReturns400() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Toán A", "Toán", ClassType.GROUP, "ACTIVE");

            EnrollStudentRequest request = new EnrollStudentRequest(tutorB.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Ghi danh vào lớp đã lưu trữ (ARCHIVED) → 400 VALIDATION_ERROR")
        void enrollingIntoArchivedClassReturns400() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp Đã Lưu Trữ", "Toán", ClassType.GROUP, "ARCHIVED");

            EnrollStudentRequest request = new EnrollStudentRequest(student1.getId());

            mockMvc.perform(post("/api/v1/classes/{id}/students", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }
    }
}
