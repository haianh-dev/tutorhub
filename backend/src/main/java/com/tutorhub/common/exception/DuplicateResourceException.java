package com.tutorhub.common.exception;

/**
 * Ném khi tạo/cập nhật tài nguyên nhưng bị trùng (ví dụ email đã tồn tại,
 * học sinh đã ghi danh vào lớp).
 * HTTP 409. Mã lỗi: {@code DUPLICATE_RESOURCE}.
 *
 * <p><b>Ví dụ:</b></p>
 * <pre>
 *   if (userRepository.existsByEmail(email)) {
 *       throw new DuplicateResourceException("User", "email", email);
 *   }
 * </pre>
 */
public class DuplicateResourceException extends AppException {

    /**
     * @param resourceName tên entity (ví dụ "User", "Enrollment")
     * @param fieldName    tên trường bị trùng (ví dụ "email")
     * @param fieldValue   giá trị trùng
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.DUPLICATE_RESOURCE,
                resourceName + " already exists with " + fieldName + ": " + fieldValue);
    }

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
