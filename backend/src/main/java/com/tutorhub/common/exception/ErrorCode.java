package com.tutorhub.common.exception;

/**
 * Mã lỗi ổn định (stable error code) để frontend có thể hiển thị
 * thông báo tiếng Việt theo từng mã, không phụ thuộc vào message text.
 *
 * <p>Quy ước: mỗi mã chứa HTTP status mặc định để {@link AppException}
 * và {@link GlobalExceptionHandler} biết trả status nào mà không cần
 * hard-code ở nhiều chỗ.</p>
 */
public enum ErrorCode {

    // ── Auth ──────────────────────────────────────────────
    AUTH_INVALID_CREDENTIALS(401, "Invalid credentials"),
    AUTH_TOKEN_EXPIRED(401, "Token expired"),
    AUTH_TOKEN_INVALID(401, "Invalid token"),
    AUTH_ACCESS_DENIED(403, "Access denied"),

    // ── Resource ─────────────────────────────────────────
    RESOURCE_NOT_FOUND(404, "Resource not found"),

    // ── Duplicate / Conflict ─────────────────────────────
    DUPLICATE_RESOURCE(409, "Resource already exists"),
    SESSION_CONFLICT(409, "Schedule conflict"),
    ENROLLMENT_DUPLICATE(409, "Student already enrolled"),

    // ── Validation ───────────────────────────────────────
    VALIDATION_ERROR(400, "Validation failed"),

    // ── Business rules ───────────────────────────────────
    CLASS_FULL(422, "Class enrollment limit reached"),
    CYCLE_INVALID(422, "Tuition cycle operation invalid"),
    INVITATION_EXPIRED(410, "Invitation expired"),
    INVITATION_USED(410, "Invitation already used"),

    // ── Generic ──────────────────────────────────────────
    INTERNAL_ERROR(500, "Internal server error");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
