package com.tutorhub.schedule;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Kiểm thử tích hợp trực tiếp trên PostgreSQL thật cho Task T3.1:
 * - Extension btree_gist đã được nạp thành công.
 * - Migration V6 tạo bảng schedule_rules và sessions thành công.
 * - Ràng buộc loại trừ (EXCLUDE USING gist) chống trùng lịch dạy của cùng gia sư:
 *   + 2 buổi chồng giờ cùng gia sư bị DB từ chối.
 *   + 2 buổi liền kề (10:00 kết thúc và 10:00 bắt đầu) KHÔNG bị từ chối nhờ khoảng nửa mở [).
 *   + Buổi trùng giờ nhưng có trạng thái CANCELLED KHÔNG gây xung đột.
 *   + 2 buổi chồng giờ nhưng KHÁC gia sư KHÔNG bị xung đột.
 *   + Ràng buộc end_at > start_at hoạt động chính xác.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionConstraintIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long tutor1Id;
    private Long tutor2Id;
    private Long class1Id;
    private Long class2Id;

    @BeforeEach
    void setUp() {
        // Dọn dẹp dữ liệu các bảng liên quan theo thứ tự khóa ngoại
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM schedule_rules");
        jdbcTemplate.execute("DELETE FROM class_enrollments");
        jdbcTemplate.execute("DELETE FROM invitations");
        jdbcTemplate.execute("DELETE FROM password_reset_tokens");
        jdbcTemplate.execute("DELETE FROM classes");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM parent_students");
        jdbcTemplate.execute("DELETE FROM users");

        // Tạo 2 gia sư test
        tutor1Id = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, role, status) " +
                "VALUES ('tutor1@example.com', 'hash', 'Gia Sư 1', 'TUTOR', 'ACTIVE') RETURNING id",
                Long.class
        );

        tutor2Id = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, role, status) " +
                "VALUES ('tutor2@example.com', 'hash', 'Gia Sư 2', 'TUTOR', 'ACTIVE') RETURNING id",
                Long.class
        );

        // Tạo 2 lớp học thuộc 2 gia sư
        class1Id = jdbcTemplate.queryForObject(
                "INSERT INTO classes (tutor_id, name, subject, class_type, status) " +
                "VALUES (?, 'Lớp Toán 10', 'Toán', 'ONE_ON_ONE', 'ACTIVE') RETURNING id",
                Long.class, tutor1Id
        );

        class2Id = jdbcTemplate.queryForObject(
                "INSERT INTO classes (tutor_id, name, subject, class_type, status) " +
                "VALUES (?, 'Lớp Lý 11', 'Lý', 'GROUP', 'ACTIVE') RETURNING id",
                Long.class, tutor2Id
        );
    }

    @Test
    @DisplayName("Chèn 2 buổi học không trùng giờ của cùng gia sư -> Thành công")
    void shouldAllowNonOverlappingSessionsForSameTutor() {
        // Buổi 1: 08:00 -> 09:30
        insertSession(class1Id, tutor1Id, "2026-10-10T08:00:00Z", "2026-10-10T09:30:00Z", "SCHEDULED");

        // Buổi 2: 13:00 -> 14:30
        insertSession(class1Id, tutor1Id, "2026-10-10T13:00:00Z", "2026-10-10T14:30:00Z", "SCHEDULED");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE tutor_id = ?", Integer.class, tutor1Id);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Chèn 2 buổi liền kề (buổi 1 kết thúc 10:00, buổi 2 bắt đầu 10:00) -> Thành công, không xung đột")
    void shouldAllowAdjacentSessionsWithoutConflict() {
        // Buổi 1: 08:00 -> 10:00
        insertSession(class1Id, tutor1Id, "2026-10-10T08:00:00Z", "2026-10-10T10:00:00Z", "SCHEDULED");

        // Buổi 2: 10:00 -> 11:30 (liền kề đúng tại thời điểm 10:00:00)
        insertSession(class1Id, tutor1Id, "2026-10-10T10:00:00Z", "2026-10-10T11:30:00Z", "SCHEDULED");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE tutor_id = ?", Integer.class, tutor1Id);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Chèn 2 buổi cùng giờ của 2 gia sư khác nhau -> Thành công, không xung đột")
    void shouldAllowOverlappingSessionsForDifferentTutors() {
        // Gia sư 1 dạy: 09:00 -> 11:00
        insertSession(class1Id, tutor1Id, "2026-10-10T09:00:00Z", "2026-10-10T11:00:00Z", "SCHEDULED");

        // Gia sư 2 cũng dạy cùng khung giờ: 09:00 -> 11:00
        insertSession(class2Id, tutor2Id, "2026-10-10T09:00:00Z", "2026-10-10T11:00:00Z", "SCHEDULED");

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions", Integer.class);
        assertThat(total).isEqualTo(2);
    }

    @Test
    @DisplayName("Chèn 2 buổi chồng giờ của cùng gia sư -> DB từ chối với lỗi vi phạm exclusion constraint")
    void shouldRejectOverlappingSessionsForSameTutor() {
        // Buổi 1: 09:00 -> 11:00
        insertSession(class1Id, tutor1Id, "2026-10-10T09:00:00Z", "2026-10-10T11:00:00Z", "SCHEDULED");

        // Buổi 2: 10:30 -> 12:00 (chồng lấn 30 phút từ 10:30 đến 11:00)
        assertThatThrownBy(() ->
                insertSession(class1Id, tutor1Id, "2026-10-10T10:30:00Z", "2026-10-10T12:00:00Z", "SCHEDULED")
        ).isInstanceOf(DataIntegrityViolationException.class)
         .hasMessageContaining("exclude_tutor_overlapping_sessions");
    }

    @Test
    @DisplayName("Chèn buổi học mới trùng giờ với buổi đã bị CANCELLED -> Thành công (buổi hủy không gây xung đột)")
    void shouldAllowSessionOverlappingWithCancelledSession() {
        // Buổi 1: 09:00 -> 11:00 nhưng bị CANCELLED
        insertSession(class1Id, tutor1Id, "2026-10-10T09:00:00Z", "2026-10-10T11:00:00Z", "CANCELLED");

        // Buổi 2: 09:30 -> 10:30 (nằm lọt trong khung giờ cũ) với status SCHEDULED -> Phải được chấp nhận
        insertSession(class1Id, tutor1Id, "2026-10-10T09:30:00Z", "2026-10-10T10:30:00Z", "SCHEDULED");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE tutor_id = ?", Integer.class, tutor1Id);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Chèn buổi học có end_at <= start_at -> DB từ chối qua CHECK constraint chk_sessions_time_range")
    void shouldRejectSessionWithInvalidTimeRange() {
        // startAt == endAt
        assertThatThrownBy(() ->
                insertSession(class1Id, tutor1Id, "2026-10-10T09:00:00Z", "2026-10-10T09:00:00Z", "SCHEDULED")
        ).isInstanceOf(DataIntegrityViolationException.class)
         .hasMessageContaining("chk_sessions_time_range");

        // startAt > endAt
        assertThatThrownBy(() ->
                insertSession(class1Id, tutor1Id, "2026-10-10T10:00:00Z", "2026-10-10T09:00:00Z", "SCHEDULED")
        ).isInstanceOf(DataIntegrityViolationException.class)
         .hasMessageContaining("chk_sessions_time_range");
    }

    @Test
    @DisplayName("Kiểm tra bảng schedule_rules: Chèn quy tắc lặp hợp lệ và vi phạm constraint")
    void shouldValidateScheduleRulesTable() {
        // 1. Chèn quy tắc hợp lệ: Thứ Hai (1), 08:00 - 10:00
        Long ruleId = jdbcTemplate.queryForObject(
                "INSERT INTO schedule_rules (class_id, day_of_week, start_time, end_time, effective_from) " +
                "VALUES (?, 1, '08:00:00', '10:00:00', '2026-10-01') RETURNING id",
                Long.class, class1Id
        );
        assertThat(ruleId).isNotNull();

        // 2. Chèn session tham chiếu rule_id
        Long sessionId = jdbcTemplate.queryForObject(
                "INSERT INTO sessions (class_id, tutor_id, rule_id, start_at, end_at, status) " +
                "VALUES (?, ?, ?, '2026-10-12T08:00:00Z', '2026-10-12T10:00:00Z', 'SCHEDULED') RETURNING id",
                Long.class, class1Id, tutor1Id, ruleId
        );
        assertThat(sessionId).isNotNull();

        // 3. Vi phạm day_of_week (phải từ 1..7)
        assertThatThrownBy(() ->
                jdbcTemplate.execute(
                        "INSERT INTO schedule_rules (class_id, day_of_week, start_time, end_time, effective_from) " +
                        "VALUES (" + class1Id + ", 8, '08:00:00', '10:00:00', '2026-10-01')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class)
         .hasMessageContaining("chk_schedule_rules_day_of_week");

        // 4. Vi phạm start_time >= end_time
        assertThatThrownBy(() ->
                jdbcTemplate.execute(
                        "INSERT INTO schedule_rules (class_id, day_of_week, start_time, end_time, effective_from) " +
                        "VALUES (" + class1Id + ", 2, '10:00:00', '09:00:00', '2026-10-01')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class)
         .hasMessageContaining("chk_schedule_rules_time_range");
    }

    private void insertSession(Long classId, Long tutorId, String startAt, String endAt, String status) {
        jdbcTemplate.update(
                "INSERT INTO sessions (class_id, tutor_id, start_at, end_at, status) VALUES (?, ?, ?::timestamptz, ?::timestamptz, ?)",
                classId, tutorId, startAt, endAt, status
        );
    }
}
