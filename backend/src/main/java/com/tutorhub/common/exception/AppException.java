package com.tutorhub.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception cơ sở cho mọi lỗi nghiệp vụ trong TutorHub.
 *
 * <p>Mỗi {@code AppException} mang theo {@link ErrorCode} — mã ổn định
 * để frontend ánh xạ sang thông báo tiếng Việt — và HTTP status tương ứng.</p>
 *
 * <p><b>Cách dùng phổ biến:</b></p>
 * <pre>
 *   throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
 *       "Lớp học với id=" + classId + " không tồn tại");
 * </pre>
 *
 * <p>Nếu cần custom HTTP status khác với mặc định của {@code ErrorCode},
 * dùng constructor 3 tham số.</p>
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    /**
     * Dùng HTTP status mặc định của {@code ErrorCode}.
     */
    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.valueOf(errorCode.getHttpStatus());
    }

    /**
     * Override HTTP status (ví dụ: cùng {@code RESOURCE_NOT_FOUND}
     * nhưng muốn trả 410 Gone thay vì 404).
     */
    public AppException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
