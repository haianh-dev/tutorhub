-- V8__create_assignments_and_scores.sql
-- Phase 5 (T5.1): Bảng assignments và assignment_scores quản lý bài tập và thang điểm 10 cố định (D-30).

CREATE TABLE assignments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assignment_type VARCHAR(50) NOT NULL,
    due_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_assignment_type CHECK (assignment_type IN ('HOMEWORK', 'QUIZ', 'EXAM', 'MOCK_TEST', 'OTHER'))
);

CREATE INDEX idx_assignments_class_id ON assignments(class_id);

CREATE TABLE assignment_scores (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score NUMERIC(4, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    feedback TEXT,
    graded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_score_range CHECK (score IS NULL OR (score >= 0 AND score <= 10)),
    CONSTRAINT chk_submission_status CHECK (status IN ('ASSIGNED', 'SUBMITTED', 'GRADED', 'MISSING')),
    CONSTRAINT uk_assignment_score_assignment_student UNIQUE (assignment_id, student_id)
);

CREATE INDEX idx_assignment_scores_assignment_id ON assignment_scores(assignment_id);
CREATE INDEX idx_assignment_scores_student_id ON assignment_scores(student_id);
