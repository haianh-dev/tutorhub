package com.tutorhub.classroom.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO để ghi danh học sinh vào lớp học.
 * POST /api/v1/classes/{id}/students
 */
public record EnrollStudentRequest(
    @NotNull(message = "studentId không được để trống")
    Long studentId
) {}
