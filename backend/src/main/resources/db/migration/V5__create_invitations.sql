-- V5: Bảng lời mời cho học sinh/phụ huynh (T1.4)
-- Token lưu SHA-256 hash, single-use, hết hạn 7 ngày.
-- class_id: nếu có, học sinh được tự động ghi danh khi chấp nhận.
-- student_id: bắt buộc khi role = PARENT (liên kết con trong parent_students).

CREATE TABLE invitations (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash    VARCHAR(64)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    invited_by    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email         VARCHAR(255),
    class_id      BIGINT       REFERENCES classes(id) ON DELETE SET NULL,
    student_id    BIGINT       REFERENCES users(id)   ON DELETE SET NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    used_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invitations_token_hash  UNIQUE (token_hash),
    CONSTRAINT chk_invitations_role       CHECK  (role IN ('STUDENT', 'PARENT'))
);

CREATE INDEX idx_invitations_token_hash ON invitations(token_hash);
CREATE INDEX idx_invitations_invited_by ON invitations(invited_by);
