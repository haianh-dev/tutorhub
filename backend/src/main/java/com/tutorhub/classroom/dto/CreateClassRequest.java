package com.tutorhub.classroom.dto;

import com.tutorhub.classroom.entity.ClassType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body cho POST /api/v1/classes (tạo lớp mới).
 *
 * <p>Quy tắc validate:</p>
 * <ul>
 *   <li>{@code name} và {@code subject} không được rỗng, tối đa 100 ký tự.</li>
 *   <li>{@code classType} bắt buộc phải có (ONE_ON_ONE hoặc GROUP) — AC T2.1: loại lớp bắt buộc.</li>
 *   <li>{@code tutorId} không đánh @NotNull ở đây:
 *       TUTOR tự lấy từ token JWT; ADMIN bắt buộc truyền (validate ở service layer).</li>
 * </ul>
 */
public record CreateClassRequest(

    @NotBlank(message = "Tên lớp không được để trống")
    @Size(max = 100, message = "Tên lớp tối đa 100 ký tự")
    String name,

    @NotBlank(message = "Môn học không được để trống")
    @Size(max = 100, message = "Môn học tối đa 100 ký tự")
    String subject,

    @NotNull(message = "Loại lớp không được để trống")
    ClassType classType,

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    String description,

    /**
     * Tùy chọn: chỉ ADMIN dùng để chỉ định gia sư phụ trách.
     * Với vai trò TUTOR: giá trị này bị bỏ qua, lấy tutorId từ token JWT.
     */
    Long tutorId
) {}
