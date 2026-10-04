package com.tutorhub.common.exception;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Bộ xử lý lỗi tập trung cho toàn bộ ứng dụng.
 *
 * <p>Mọi lỗi đều trả về dạng {@link ProblemDetail} (RFC 7807) kèm
 * property {@code code} — mã ổn định để frontend ánh xạ sang thông báo
 * tiếng Việt.</p>
 *
 * <h3>Luồng xử lý:</h3>
 * <ol>
 *   <li>{@link AppException} → dùng {@code errorCode} + {@code httpStatus} từ exception.</li>
 *   <li>{@link MethodArgumentNotValidException} → 400, liệt kê field errors trong {@code errors}.</li>
 *   <li>{@link DataIntegrityViolationException} → 409 (phát hiện từ khóa constraint)
 *       hoặc 500 nếu không nhận diện được.</li>
 *   <li>Mọi exception khác → 500, ẩn stack trace.</li>
 * </ol>
 *
 * <p><b>Quan trọng:</b> Không bao giờ lộ stack trace ra response.
 * Stack trace chỉ log ở server (level ERROR).</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 1. Lỗi nghiệp vụ (AppException) ──────────────────────────────

    /**
     * Bắt mọi {@link AppException} (và subclass như ResourceNotFoundException,
     * DuplicateResourceException). Trả ProblemDetail với code ổn định.
     */
    @ExceptionHandler(AppException.class)
    public ProblemDetail handleAppException(AppException ex) {
        log.warn("Business error [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                ex.getHttpStatus(), ex.getMessage());
        problem.setTitle(ex.getErrorCode().getDefaultMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", ex.getErrorCode().name());

        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Bạn không có quyền thực hiện thao tác này.");
        problem.setTitle(ErrorCode.AUTH_ACCESS_DENIED.getDefaultMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", ErrorCode.AUTH_ACCESS_DENIED.name());
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Yêu cầu đăng nhập.");
        problem.setTitle(ErrorCode.AUTH_TOKEN_INVALID.getDefaultMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", ErrorCode.AUTH_TOKEN_INVALID.name());
        return problem;
    }

    // ─── 2. Bean Validation (400) ──────────────────────────────────────

    /**
     * Override handler mặc định của Spring để thêm property {@code code}
     * và danh sách {@code errors} (field + message) vào ProblemDetail.
     *
     * <p>Ví dụ response:</p>
     * <pre>
     * {
     *   "type": "about:blank",
     *   "title": "Validation failed",
     *   "status": 400,
     *   "detail": "Dữ liệu không hợp lệ. Kiểm tra các trường bên dưới.",
     *   "code": "VALIDATION_ERROR",
     *   "errors": [
     *     {"field": "email", "message": "must not be blank"},
     *     {"field": "name",  "message": "size must be between 1 and 100"}
     *   ]
     * }
     * </pre>
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu không hợp lệ. Kiểm tra các trường bên dưới.");
        problem.setTitle("Validation failed");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", ErrorCode.VALIDATION_ERROR.name());
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ─── 3. Data Integrity (constraint violation từ DB) ────────────────

    /**
     * Bắt lỗi constraint từ DB — chủ yếu là exclusion constraint (chống
     * trùng buổi học) và unique constraint. Trả 409 nếu nhận diện được,
     * hoặc 500 nếu không.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation: {}", rootMessage);

        // Exclusion constraint → SESSION_CONFLICT (409)
        if (rootMessage != null && rootMessage.contains("sessions_no_overlap")) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Buổi học trùng thời gian với buổi khác của gia sư.");
            problem.setTitle("Schedule conflict");
            problem.setType(URI.create("about:blank"));
            problem.setProperty("code", ErrorCode.SESSION_CONFLICT.name());
            return problem;
        }

        // Unique constraint → DUPLICATE_RESOURCE (409)
        if (rootMessage != null && (rootMessage.contains("duplicate key")
                || rootMessage.contains("unique constraint"))) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Tài nguyên đã tồn tại.");
            problem.setTitle("Resource already exists");
            problem.setType(URI.create("about:blank"));
            problem.setProperty("code", ErrorCode.DUPLICATE_RESOURCE.name());
            return problem;
        }

        // Không nhận diện → 500
        log.error("Unhandled data integrity violation", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Lỗi dữ liệu không xác định.");
        problem.setTitle("Internal server error");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", ErrorCode.INTERNAL_ERROR.name());
        return problem;
    }

    // ─── 4. Fallback: mọi exception chưa xử lý ────────────────────────

    /**
     * Bắt mọi {@link Exception} khác → 500. Ẩn chi tiết lỗi khỏi response,
     * chỉ log stack trace phía server.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        problem.setTitle("Internal server error");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", ErrorCode.INTERNAL_ERROR.name());

        return problem;
    }

    // ─── Inner DTO for validation errors ───────────────────────────────

    /**
     * DTO đơn giản biểu diễn một lỗi trên field cụ thể.
     * Sử dụng Java record (immutable, tự động serialize sang JSON).
     */
    public record FieldError(String field, String message) {
    }
}
