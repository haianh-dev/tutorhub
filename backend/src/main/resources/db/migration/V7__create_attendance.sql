-- V7__create_attendance.sql
-- Phase 4 (T4.1): Bảng attendance phục vụ điểm danh học sinh theo từng buổi học.

CREATE TABLE attendance (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'LATE', 'ABSENT_EXCUSED', 'ABSENT_UNEXCUSED')),
    -- Mỗi học sinh chỉ có duy nhất 1 bản ghi điểm danh trong 1 buổi học
    CONSTRAINT uk_attendance_session_student UNIQUE (session_id, student_id)
);

CREATE INDEX idx_attendance_session_id ON attendance(session_id);
CREATE INDEX idx_attendance_student_id ON attendance(student_id);
