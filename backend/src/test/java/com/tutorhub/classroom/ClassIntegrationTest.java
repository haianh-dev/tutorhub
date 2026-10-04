package com.tutorhub.classroom;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.classroom.dto.CreateClassRequest;
import com.tutorhub.classroom.dto.UpdateClassRequest;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

/**
 * Integration Test toàn diện cho Task T2.1 — Migration `classes`,
 * `class_enrollments`;
 * CRUD lớp + archive (ADMIN chỉ định tutor_id).
 *
 * <p>
 * Tiêu chí nghiệm thu (ROADMAP line 30):
 * </p>
 * <ol>
 * <li>✅ AC FR-2: loại lớp bắt buộc (không classType → 400).</li>
 * <li>✅ Gia sư B không truy cập lớp gia sư A → 404 (không lộ sự tồn tại).</li>
 * <li>✅ ADMIN quản lý được mọi lớp (bypass ownership filter) + tạo lớp chỉ định
 * tutorId.</li>
 * <li>✅ FR-2.6 đổi loại lớp GROUP → ONE_ON_ONE khi ≥2 HS → 422.</li>
 * </ol>
 *
 * <p>
 * Sử dụng Testcontainers PostgreSQL (@AutoConfigureTestDatabase Replace.NONE →
 * test profile sử dụng container real Postgres, không dùng H2).
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClassIntegrationTest {

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
    private User studentA;
    private User parentA;

    private String adminToken;
    private String tutorAToken;
    private String tutorBToken;
    private String studentAToken;
    private String parentAToken;

    @BeforeEach
    void setUp() {
        // ── Clean tables theo thứ tự FK (không có cycle) ─────────────
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM invitations");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM parent_students");
        jdbcTemplate.update("DELETE FROM class_enrollments");
        jdbcTemplate.update("DELETE FROM classes");
        userRepository.deleteAll();

        // ── Create users ──────────────────────────────────────────────
        adminUser = createUser("admin@example.com", "Admin", Role.ADMIN);
        tutorA = createUser("tutor-a@example.com", "Gia sư A", Role.TUTOR);
        tutorB = createUser("tutor-b@example.com", "Gia sư B", Role.TUTOR);
        studentA = createUser("student-a@example.com", "Học sinh A", Role.STUDENT);
        parentA = createUser("parent-a@example.com", "Phụ huynh A", Role.PARENT);

        // ── Tokens (sinh JWT trực tiếp bằng JwtService — nhanh hơn login API) ─
        adminToken = jwtService.generateAccessToken(adminUser);
        tutorAToken = jwtService.generateAccessToken(tutorA);
        tutorBToken = jwtService.generateAccessToken(tutorB);
        studentAToken = jwtService.generateAccessToken(studentA);
        parentAToken = jwtService.generateAccessToken(parentA);
    }

    // ── Helper: tạo user test ────────────────────────────────────────────────
    private User createUser(String email, String fullName, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName(fullName)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    // ── Helper: insert 1 record class thẳng vào DB (setup data cho test) ────
    private Long insertClass(Long tutorId, String name, String subject, ClassType type, String status) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO classes (tutor_id, name, subject, class_type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, tutorId, name, subject, type.name(), status);
    }

    // Helper: insert enrollment ACTIVE
    private void insertEnrollment(Long classId, Long studentId) {
        jdbcTemplate.update("""
                INSERT INTO class_enrollments (class_id, student_id, status)
                VALUES (?, ?, 'ACTIVE')
                """, classId, studentId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. NHÓM TEST: CRUD hợp lệ với vai trò TUTOR
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("1. CRUD hợp lệ — TUTOR (chủ sở hữu)")
    class TutorCrudTests {

        @Test
        @DisplayName("POST /classes: TUTOR tạo lớp GROUP mới → 201, tutorId tự lấy từ token")
        void shouldCreateClassByTutor() throws Exception {
            CreateClassRequest req = new CreateClassRequest(
                    "Toán 12A1", "Toán", ClassType.GROUP, "Luyện thi đại học", null);

            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Toán 12A1"))
                    .andExpect(jsonPath("$.subject").value("Toán"))
                    .andExpect(jsonPath("$.classType").value("GROUP"))
                    .andExpect(jsonPath("$.description").value("Luyện thi đại học"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.tutorId").value(tutorA.getId()))
                    .andExpect(jsonPath("$.studentCount").value(0))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("POST /classes: TUTOR tạo lớp ONE_ON_ONE (description null) → 201")
        void shouldCreateOneOnOneClassNullDescription() throws Exception {
            CreateClassRequest req = new CreateClassRequest(
                    "Toán riêng Hùng", "Toán", ClassType.ONE_ON_ONE, null, null);

            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.classType").value("ONE_ON_ONE"))
                    .andExpect(jsonPath("$.description").isEmpty())
                    .andExpect(jsonPath("$.tutorId").value(tutorA.getId()));
        }

        @Test
        @DisplayName("GET /classes/{id}: lấy chi tiết lớp của mình → 200, studentCount đúng")
        void shouldGetClassDetailForOwnerTutor() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lý 11", "Lý", ClassType.GROUP, "ACTIVE");
            insertEnrollment(classId, studentA.getId());

            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(classId))
                    .andExpect(jsonPath("$.name").value("Lý 11"))
                    .andExpect(jsonPath("$.tutorId").value(tutorA.getId()))
                    .andExpect(jsonPath("$.studentCount").value(1));
        }

        @Test
        @DisplayName("GET /classes: danh sách lớp của TUTOR (15 lớp với 10 Toán 5 Lý)")
        void shouldListTutorClassesPaginated() throws Exception {
            // Tạo 10 lớp "Toán..."
            for (int i = 1; i <= 10; i++) {
                insertClass(tutorA.getId(), "Toán lớp " + i, "Toán", ClassType.GROUP, "ACTIVE");
            }
            // Tạo 5 lớp "Lý..."
            for (int i = 1; i <= 5; i++) {
                insertClass(tutorA.getId(), "Lý lớp " + i, "Lý", ClassType.GROUP, "ACTIVE");
            }
            // Tạo 2 lớp ARCHIVED
            insertClass(tutorA.getId(), "Toán cũ 2024", "Toán", ClassType.GROUP, "ARCHIVED");
            insertClass(tutorA.getId(), "Lý cũ 2024", "Lý", ClassType.GROUP, "ARCHIVED");

            // Case 1: không filter, size=5
            mockMvc.perform(get("/api/v1/classes")
                    .param("size", "5")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.totalElements").value(17))
                    .andExpect(jsonPath("$.totalPages").value(4));

            // Case 2: search q="Toán" → 10+1=11 lớp
            mockMvc.perform(get("/api/v1/classes")
                    .param("q", "Toán")
                    .param("size", "20")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(11));

            // Case 3: filter status=ARCHIVED → 2 lớp
            mockMvc.perform(get("/api/v1/classes")
                    .param("status", "ARCHIVED")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));

            // Case 4: search q=Lý + status=ACTIVE → 5 lớp
            mockMvc.perform(get("/api/v1/classes")
                    .param("q", "Lý")
                    .param("status", "ACTIVE")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5));
        }

        @Test
        @DisplayName("PUT /classes/{id}: cập nhật name, subject, description → 200")
        void shouldUpdateClassFields() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Tên cũ", "Môn cũ", ClassType.GROUP, "ACTIVE");
            UpdateClassRequest req = new UpdateClassRequest(
                    "Tên mới", "Môn mới", null, "Mô tả mới");

            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Tên mới"))
                    .andExpect(jsonPath("$.subject").value("Môn mới"))
                    .andExpect(jsonPath("$.description").value("Mô tả mới"))
                    .andExpect(jsonPath("$.classType").value("GROUP")); // giữ nguyên do req.classType=null
        }

        @Test
        @DisplayName("PUT /classes/{id}: ONE_ON_ONE → GROUP luôn OK")
        void shouldChangeTypeOneOnOneToGroupAlwaysAllowed() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp riêng", "Toán", ClassType.ONE_ON_ONE, "ACTIVE");
            insertEnrollment(classId, studentA.getId()); // có 1 HS vẫn được
            // Thêm 1 HS nữa — ONE_ON_ONE vẫn accept do chưa chặn ở T2.1 (chặn ở T2.2 khi
            // ghi danh)
            User studentB = createUser("b@e.com", "B", Role.STUDENT);
            insertEnrollment(classId, studentB.getId());

            UpdateClassRequest req = new UpdateClassRequest(null, null, ClassType.GROUP, null);
            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classType").value("GROUP"))
                    .andExpect(jsonPath("$.studentCount").value(2));
        }

        @Test
        @DisplayName("POST /classes/{id}/archive: set status ARCHIVED, list ACTIVE không thấy nữa")
        void shouldArchiveClass() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp sắp lưu", "Hóa", ClassType.GROUP, "ACTIVE");

            // Archive
            mockMvc.perform(post("/api/v1/classes/{id}/archive", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));

            // GET chi tiết → status ARCHIVED
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));

            // List ACTIVE → 0
            mockMvc.perform(get("/api/v1/classes")
                    .param("status", "ACTIVE")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(jsonPath("$.totalElements").value(0));

            // List ARCHIVED → 1
            mockMvc.perform(get("/api/v1/classes")
                    .param("status", "ARCHIVED")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. NHÓM TEST: ADMIN chỉ định tutorId & quản lý mọi lớp
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("2. ADMIN quản lý mọi lớp & chỉ định tutorId khi tạo")
    class AdminTests {

        @Test
        @DisplayName("ADMIN POST /classes chỉ định tutorId=TUTOR_B → 201, lớp thuộc TUTOR_B")
        void adminCreatesClassAssigningTutorB() throws Exception {
            CreateClassRequest req = new CreateClassRequest(
                    "Lớp do admin giao cho B", "Văn", ClassType.GROUP, null, tutorB.getId());

            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tutorId").value(tutorB.getId()));

            // Verify TUTOR_B thấy lớp này trong danh sách
            mockMvc.perform(get("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Lớp do admin giao cho B"));

            // Verify TUTOR_A KHÔNG thấy lớp này (GET list TUTOR_A total=0)
            mockMvc.perform(get("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("ADMIN POST /classes không truyền tutorId → 400 VALIDATION_ERROR")
        void adminMissingTutorIdReturns400() throws Exception {
            CreateClassRequest req = new CreateClassRequest(
                    "Lớp admin thiếu tutorId", "Sinh", ClassType.GROUP, null, null);

            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("ADMIN POST /classes tutorId tồn tại nhưng là STUDENT → 400 VALIDATION_ERROR")
        void adminAssignsStudentAsTutorReturns400() throws Exception {
            CreateClassRequest req = new CreateClassRequest(
                    "Lớp giao nhầm", "Sử", ClassType.GROUP, null, studentA.getId());

            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("ADMIN xem/sửa/archive lớp TUTOR_A (không thuộc mình) → đều OK (bypass ownership)")
        void adminAccessesTutorAClass() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp của Tutor A", "GDCD", ClassType.GROUP, "ACTIVE");

            // GET detail
            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tutorId").value(tutorA.getId()));

            // PUT update
            UpdateClassRequest up = new UpdateClassRequest("Tên mới do Admin sửa", null, null, null);
            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(up)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Tên mới do Admin sửa"));

            // Archive
            mockMvc.perform(post("/api/v1/classes/{id}/archive", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));
        }

        @Test
        @DisplayName("ADMIN List: thấy toàn bộ mọi lớp của TUTOR A và TUTOR B")
        void adminListAllClassesFromMultipleTutors() throws Exception {
            insertClass(tutorA.getId(), "Lớp A1", "Toán", ClassType.GROUP, "ACTIVE");
            insertClass(tutorA.getId(), "Lớp A2", "Lý", ClassType.GROUP, "ACTIVE");
            insertClass(tutorB.getId(), "Lớp B1", "Văn", ClassType.GROUP, "ACTIVE");

            mockMvc.perform(get("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(3));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. NHÓM TEST: Ownership (Gia sư B không truy cập lớp gia sư A → 404)
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("3. Ownership: TUTOR_B truy cập lớp TUTOR_A → 404 (không lộ)")
    class OwnershipTests {

        @Test
        @DisplayName("TUTOR_B GET /classes/{id} với lớp TUTOR_A → 404 RESOURCE_NOT_FOUND")
        void tutorBAccessesTutorAClassDetailReturns404() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Toán của A", "Toán", ClassType.GROUP, "ACTIVE");

            mockMvc.perform(get("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.detail",
                            containsString("không tồn tại")));
        }

        @Test
        @DisplayName("TUTOR_B PUT /classes/{id} lớp TUTOR_A → 404")
        void tutorBUpdatesTutorAClassReturns404() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp A", "Toán", ClassType.GROUP, "ACTIVE");
            UpdateClassRequest req = new UpdateClassRequest("Đổi tên trộm", null, null, null);

            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("TUTOR_B POST /classes/{id}/archive lớp TUTOR_A → 404")
        void tutorBArchivesTutorAClassReturns404() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp A", "Toán", ClassType.GROUP, "ACTIVE");

            mockMvc.perform(post("/api/v1/classes/{id}/archive", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("TUTOR_B List: KHÔNG thấy lớp TUTOR_A trong danh sách")
        void tutorBListDoesNotIncludeTutorAClasses() throws Exception {
            insertClass(tutorA.getId(), "Lớp A1", "Toán", ClassType.GROUP, "ACTIVE");
            insertClass(tutorA.getId(), "Lớp A2", "Lý", ClassType.GROUP, "ACTIVE");
            insertClass(tutorB.getId(), "Lớp B1", "Văn", ClassType.GROUP, "ACTIVE");

            mockMvc.perform(get("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorBToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Lớp B1"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. NHÓM TEST: Validation — Loại lớp bắt buộc & input rỗng → 400
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("4. Validation — AC FR-2: loại lớp bắt buộc / rỗng")
    class ValidationTests {

        @Test
        @DisplayName("POST /classes thiếu classType → 400 VALIDATION_ERROR, errors có field classType")
        void missingClassTypeReturns400WithFieldError() throws Exception {
            // Dùng JSON thẳng để bỏ classType (record Java không cho null do @NotNull)
            String body = """
                    {"name":"Lớp thiếu loại","subject":"Toán"}
                    """;
            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.errors[?(@.field == 'classType')]").exists());
        }

        @Test
        @DisplayName("POST /classes name rỗng / subject rỗng → 400")
        void emptyNameOrSubjectReturns400() throws Exception {
            // name blank
            String noName = """
                    {"name":"   ","subject":"Toán","classType":"GROUP"}
                    """;
            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(noName))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            // subject blank
            String noSubject = """
                    {"name":"Lớp tên","subject":"   ","classType":"ONE_ON_ONE"}
                    """;
            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(noSubject))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("PUT /classes/{id} name or subject blank → 400 VALIDATION_ERROR")
        void updatingWithBlankNameOrSubjectReturns400() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp hiện tại", "Toán", ClassType.GROUP, "ACTIVE");
            UpdateClassRequest blankName = new UpdateClassRequest("   ", null, null, null);
            UpdateClassRequest blankSubject = new UpdateClassRequest(null, "   ", null, null);

            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(blankName)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(blankSubject)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GET /classes/{id} không tồn tại → 404 RESOURCE_NOT_FOUND")
        void getNonExistentClassReturns404() throws Exception {
            mockMvc.perform(get("/api/v1/classes/{id}", 99999L)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. NHÓM TEST: FR-2.6 Đổi loại lớp GROUP → ONE_ON_ONE vs sĩ số
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("5. FR-2.6 Đổi loại lớp GROUP → ONE_ON_ONE")
    class ClassTypeChangeTests {

        @Test
        @DisplayName("GROUP → ONE_ON_ONE: 0 HS đang học → 200 OK")
        void groupToOneOnOneZeroStudentsAllowed() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp nhóm chưa ai", "Toán", ClassType.GROUP, "ACTIVE");
            // Không insert enrollment nào.
            UpdateClassRequest req = new UpdateClassRequest(null, null, ClassType.ONE_ON_ONE, null);

            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classType").value("ONE_ON_ONE"));
        }

        @Test
        @DisplayName("GROUP → ONE_ON_ONE: 1 HS ACTIVE → 200 OK")
        void groupToOneOnOneOneStudentAllowed() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp nhóm 1 HS", "Lý", ClassType.GROUP, "ACTIVE");
            insertEnrollment(classId, studentA.getId());

            UpdateClassRequest req = new UpdateClassRequest(null, null, ClassType.ONE_ON_ONE, null);
            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classType").value("ONE_ON_ONE"))
                    .andExpect(jsonPath("$.studentCount").value(1));
        }

        @Test
        @DisplayName("GROUP → ONE_ON_ONE: ≥ 2 HS ACTIVE → 422 CLASS_TYPE_CHANGE_INVALID")
        void groupToOneOnOneTwoStudentsReturns422() throws Exception {
            Long classId = insertClass(tutorA.getId(), "Lớp nhóm 2 HS", "Sinh", ClassType.GROUP, "ACTIVE");
            insertEnrollment(classId, studentA.getId());
            User s2 = createUser("s2@e.com", "S2", Role.STUDENT);
            insertEnrollment(classId, s2.getId());

            UpdateClassRequest req = new UpdateClassRequest(null, null, ClassType.ONE_ON_ONE, null);
            mockMvc.perform(put("/api/v1/classes/{id}", classId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tutorAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.code").value("CLASS_TYPE_CHANGE_INVALID"))
                    .andExpect(jsonPath("$.detail", containsString("2 học sinh đang học")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. NHÓM TEST: Vô hiệu hóa — Không token / sai vai trò (401/403)
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("6. Security — 401 / 403")
    class SecurityTests {

        @Test
        @DisplayName("Không token gọi GET /classes → 401 AUTH_TOKEN_INVALID")
        void noTokenReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/classes"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("STUDENT gọi POST /classes → 403 AUTH_ACCESS_DENIED")
        void studentCreateReturns403() throws Exception {
            CreateClassRequest req = new CreateClassRequest("Lớp", "T", ClassType.GROUP, null, null);
            mockMvc.perform(post("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("STUDENT chỉ thấy lớp mình đang ghi danh, bỏ qua studentId giả từ client")
        void studentListShowsOnlyOwnActiveClasses() throws Exception {
            Long enrolledClass = insertClass(tutorA.getId(), "Lớp học sinh đang học", "Toán", ClassType.GROUP,
                    "ACTIVE");
            insertEnrollment(enrolledClass, studentA.getId());
            Long unrelatedClass = insertClass(tutorB.getId(), "Lớp không ghi danh", "Lý", ClassType.GROUP, "ACTIVE");

            mockMvc.perform(get("/api/v1/classes")
                    .param("studentId", "999999")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(enrolledClass));

            mockMvc.perform(get("/api/v1/classes/{id}", unrelatedClass)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PARENT chỉ thấy lớp của con đã liên kết; studentId có thể lọc theo con")
        void parentListShowsOnlyLinkedChildClasses() throws Exception {
            Long childClass = insertClass(tutorA.getId(), "Lớp của con", "Toán", ClassType.GROUP, "ACTIVE");
            insertEnrollment(childClass, studentA.getId());
            jdbcTemplate.update("INSERT INTO parent_students(parent_id, student_id) VALUES (?, ?)",
                    parentA.getId(), studentA.getId());
            insertClass(tutorB.getId(), "Lớp không liên quan", "Lý", ClassType.GROUP, "ACTIVE");

            mockMvc.perform(get("/api/v1/classes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(childClass));

            mockMvc.perform(get("/api/v1/classes")
                    .param("studentId", studentA.getId().toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentAToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));

            mockMvc.perform(get("/api/v1/classes/{id}", childClass)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentAToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PARENT gọi archive → 403; STUDENT gọi update → 403")
        void parentStudentMutationsReturn403() throws Exception {
            Long id = insertClass(tutorA.getId(), "X", "T", ClassType.GROUP, "ACTIVE");
            UpdateClassRequest up = new UpdateClassRequest("Tên", null, null, null);

            // Parent archive
            mockMvc.perform(post("/api/v1/classes/{id}/archive", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentAToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

            // Student update
            mockMvc.perform(put("/api/v1/classes/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(up)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
        }
    }
}
