-- V6__create_schedule_rules_and_sessions.sql
-- Phase 3 (T3.1): Bảng schedule_rules và sessions kèm btree_gist extension và PostgreSQL EXCLUDE constraint chống trùng buổi.

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 1. Bảng quy tắc lịch lặp hàng tuần của lớp
CREATE TABLE schedule_rules (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_rules_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_schedule_rules_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_schedule_rules_date_range CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_schedule_rules_class_id ON schedule_rules(class_id);

-- 2. Bảng buổi học đơn lẻ
CREATE TABLE sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    tutor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rule_id BIGINT REFERENCES schedule_rules(id) ON DELETE SET NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    topic VARCHAR(255),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sessions_time_range CHECK (end_at > start_at),
    CONSTRAINT chk_sessions_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    -- Quyết định D-10 & AC FR-3.3: Chống trùng buổi cùng gia sư bằng exclusion constraint
    -- Dải nửa mở '[)' bảo đảm buổi kết thúc 10:00 và buổi bắt đầu 10:00 KHÔNG bị coi là xung đột.
    -- Buổi CANCELLED không gây xung đột (WHERE status <> 'CANCELLED').
    CONSTRAINT exclude_tutor_overlapping_sessions
    EXCLUDE USING gist (
        tutor_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status <> 'CANCELLED')
);

CREATE INDEX idx_sessions_class_id ON sessions(class_id);
CREATE INDEX idx_sessions_tutor_time ON sessions(tutor_id, start_at);
CREATE INDEX idx_sessions_rule_id ON sessions(rule_id);
