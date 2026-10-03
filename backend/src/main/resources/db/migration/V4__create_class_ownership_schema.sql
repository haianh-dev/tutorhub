CREATE TABLE classes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tutor_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    description TEXT,
    class_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_classes_type CHECK (class_type IN ('ONE_ON_ONE', 'GROUP')),
    CONSTRAINT chk_classes_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_classes_tutor_id ON classes(tutor_id);

CREATE TABLE class_enrollments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_class_enrollments_status CHECK (status IN ('ACTIVE', 'LEFT')),
    CONSTRAINT uk_class_enrollments_class_student UNIQUE (class_id, student_id)
);

CREATE INDEX idx_class_enrollments_student_id ON class_enrollments(student_id);
CREATE INDEX idx_class_enrollments_active ON class_enrollments(class_id, student_id) WHERE status = 'ACTIVE';

CREATE TABLE parent_students (
    parent_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_parent_students PRIMARY KEY (parent_id, student_id),
    CONSTRAINT chk_parent_student_different_users CHECK (parent_id <> student_id)
);

CREATE INDEX idx_parent_students_student_id ON parent_students(student_id);