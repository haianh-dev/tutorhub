package com.tutorhub.classroom.dto;

import com.tutorhub.classroom.entity.ClassType;
import jakarta.validation.constraints.Size;

/**
 * Request body cho PUT /api/v1/classes/{id} (cập nhật lớp).
 * Partial update: nếu trường nào là null thì giữ nguyên giá trị cũ.
 *
 * <p>Quy tắc nghiệp vụ (validate ở service layer):</p>
 * <ul>
 *   <li>Đổi classType từ GROUP → ONE_ON_ONE: chỉ được khi lớp có ≤ 1 học sinh ACTIVE (≥2 → 422).</li>
 *   <li>Đổi classType từ ONE_ON_ONE → GROUP: luôn được.</li>
 * </ul>
 */
public record UpdateClassRequest(

    @Size(max = 100, message = "Tên lớp tối đa 100 ký tự")
    String name,

    @Size(max = 100, message = "Môn học tối đa 100 ký tự")
    String subject,

    ClassType classType,

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    String description
) {}
