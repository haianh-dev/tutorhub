package com.tutorhub.common.exception;

/**
 * Ném khi không tìm thấy tài nguyên (entity) theo ID hoặc điều kiện.
 * HTTP 404. Mã lỗi: {@code RESOURCE_NOT_FOUND}.
 *
 * <p><b>Ví dụ:</b></p>
 * <pre>
 *   Class cls = classRepository.findById(id)
 *       .orElseThrow(() -&gt; new ResourceNotFoundException("Class", id));
 * </pre>
 */
public class ResourceNotFoundException extends AppException {

    /**
     * @param resourceName tên entity (ví dụ "Class", "User")
     * @param identifier   giá trị ID hoặc điều kiện tìm kiếm
     */
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                resourceName + " not found with identifier: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
