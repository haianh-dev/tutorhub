package com.tutorhub.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorhub.auth.service.JwtService;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.schedule.dto.CreateScheduleRuleRequest;
import com.tutorhub.schedule.dto.CreateSessionRequest;
import com.tutorhub.schedule.dto.GenerateSessionsRequest;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScheduleRuleIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User tutorA;
    private User tutorB;
    private User admin;
    private String tokenTutorA;
    private String tokenTutorB;
    private String tokenAdmin;

    private ClassEntity classA;
    private ClassEntity classB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM schedule_rules");
        jdbcTemplate.execute("DELETE FROM class_enrollments");
        jdbcTemplate.execute("DELETE FROM classes");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM parent_students");
        jdbcTemplate.execute("DELETE FROM users");

        tutorA = userRepository.save(User.builder()
                .email("tutorA_" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Thầy A")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .build());

        tutorB = userRepository.save(User.builder()
                .email("tutorB_" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Thầy B")
                .role(Role.TUTOR)
                .status(UserStatus.ACTIVE)
                .build());

        admin = userRepository.save(User.builder()
                .email("admin_" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        tokenTutorA = jwtService.generateAccessToken(tutorA);
        tokenTutorB = jwtService.generateAccessToken(tutorB);
        tokenAdmin = jwtService.generateAccessToken(admin);

        classA = classRepository.save(ClassEntity.builder()
                .name("Lớp Toán 10A")
                .subject("Toán")
                .classType(ClassType.GROUP)
                .tutor(tutorA)
                .status(ClassStatus.ACTIVE)
                .build());

        classB = classRepository.save(ClassEntity.builder()
                .name("Lớp Lý 10B")
                .subject("Vật lý")
                .classType(ClassType.GROUP)
                .tutor(tutorB)
                .status(ClassStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Tạo quy tắc lịch lặp (POST /classes/{id}/schedule-rules) -> 201 Created")
    void createScheduleRule_Success() throws Exception {
        CreateScheduleRuleRequest req = new CreateScheduleRuleRequest(
                (short) 1, // Thứ 2
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );

        mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.dayOfWeek", is(1)))
                .andExpect(jsonPath("$.startTime", is("08:00:00")))
                .andExpect(jsonPath("$.endTime", is("09:30:00")));
    }

    @Test
    @DisplayName("Tạo quy tắc với endTime <= startTime -> 400 Bad Request")
    void createScheduleRule_InvalidTimeRange() throws Exception {
        CreateScheduleRuleRequest req = new CreateScheduleRuleRequest(
                (short) 2,
                LocalTime.of(10, 0),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 11, 1),
                null
        );

        mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Gia sư khác không được tạo/xem/xóa rule của lớp -> 404 Not Found")
    void scheduleRule_UnauthorizedTutor_NotFound() throws Exception {
        CreateScheduleRuleRequest req = new CreateScheduleRuleRequest(
                (short) 1,
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 1),
                null
        );

        // Gia sư B cố tạo rule cho lớp của Gia sư A
        mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sinh buổi học từ quy tắc: không có xung đột -> Tạo đủ các buổi hợp lệ")
    void generateSessions_Success_NoConflict() throws Exception {
        // Tạo rule: Thứ 2 (dayOfWeek=1), 08:00 - 09:30, từ 2026-11-01 đến 2026-11-30
        // Trong tháng 11/2026:
        // 2026-11-02 (Mon), 2026-11-09 (Mon), 2026-11-16 (Mon), 2026-11-23 (Mon), 2026-11-30 (Mon) -> 5 buổi
        CreateScheduleRuleRequest req = new CreateScheduleRuleRequest(
                (short) 1,
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );

        String ruleJson = mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ruleId = objectMapper.readTree(ruleJson).get("id").asLong();

        // Kích hoạt sinh buổi cho 2 tuần đầu tháng 11: 2026-11-01 đến 2026-11-15 (Gồm 2 ngày thứ Hai: ngày 2 và ngày 9)
        GenerateSessionsRequest genReq = new GenerateSessionsRequest(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15)
        );

        mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules/{ruleId}/generate", classA.getId(), ruleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(2)))
                .andExpect(jsonPath("$.conflicts", hasSize(0)));

        // Kiểm tra trong DB đúng 2 buổi đã được lưu
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE class_id = ? AND rule_id = ?",
                Integer.class,
                classA.getId(),
                ruleId
        );
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Sinh buổi học: Ngày bị trùng lịch báo về conflicts, ngày hợp lệ vẫn được tạo thành công")
    void generateSessions_PartialConflict_ReportsConflictsAndCreatesValid() throws Exception {
        // Tạo trước 1 buổi đơn lẻ của Thầy A vào 2026-11-09 lúc 08:30 - 10:00 (chồng lên khung 08:00 - 09:30)
        CreateSessionRequest existingSessionReq = new CreateSessionRequest(
                java.time.Instant.parse("2026-11-09T08:30:00Z"),
                java.time.Instant.parse("2026-11-09T10:00:00Z"),
                "Buổi đã có sẵn", null
        );
        String sessionRes = mockMvc.perform(post("/api/v1/classes/{id}/sessions", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingSessionReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long existingSessionId = objectMapper.readTree(sessionRes).get("id").asLong();

        // Tạo rule: Thứ 2 (dayOfWeek=1), 08:00 - 09:30
        CreateScheduleRuleRequest ruleReq = new CreateScheduleRuleRequest(
                (short) 1,
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );
        String ruleJson = mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ruleId = objectMapper.readTree(ruleJson).get("id").asLong();

        // Sinh buổi từ 2026-11-01 đến 2026-11-15 (gồm ngày 02/11 và 09/11)
        // Ngày 02/11: Không trùng -> Tạo thành công
        // Ngày 09/11: Trùng với existingSessionId -> Báo vào conflicts
        GenerateSessionsRequest genReq = new GenerateSessionsRequest(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15)
        );

        mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules/{ruleId}/generate", classA.getId(), ruleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(1)))
                .andExpect(jsonPath("$.conflicts", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[0].date", is("2026-11-09")))
                .andExpect(jsonPath("$.conflicts[0].conflictingSessionId", is((int) existingSessionId)));

        // Kiểm tra trong DB: có tổng cộng 2 sessions (1 buổi cũ + 1 buổi mới sinh ra từ rule)
        Integer totalSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE class_id = ?",
                Integer.class,
                classA.getId()
        );
        org.assertj.core.api.Assertions.assertThat(totalSessions).isEqualTo(2);
    }

    @Test
    @DisplayName("Xóa quy tắc lịch lặp (DELETE /classes/{id}/schedule-rules/{ruleId}) -> 204 No Content")
    void deleteScheduleRule_Success() throws Exception {
        CreateScheduleRuleRequest req = new CreateScheduleRuleRequest(
                (short) 5, // Thứ 6
                LocalTime.of(14, 0),
                LocalTime.of(15, 30),
                LocalDate.of(2026, 11, 1),
                null
        );

        String ruleJson = mockMvc.perform(post("/api/v1/classes/{id}/schedule-rules", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ruleId = objectMapper.readTree(ruleJson).get("id").asLong();

        // Xóa rule
        mockMvc.perform(delete("/api/v1/classes/{id}/schedule-rules/{ruleId}", classA.getId(), ruleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA))
                .andExpect(status().isNoContent());

        // Kiểm tra danh sách rules đã rỗng
        mockMvc.perform(get("/api/v1/classes/{id}/schedule-rules", classA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTutorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
