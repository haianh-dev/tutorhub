package com.tutorhub.attendance.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * DTO cho bulk PUT /sessions/{id}/attendance.
 * Toàn bộ records phải hợp lệ; bất kỳ lỗi nào sẽ rollback toàn bộ transaction.
 */
public record BulkAttendanceRequest(
        @NotEmpty(message = "Danh sách điểm danh không được để trống")
        @Valid
        List<AttendanceItemRequest> records
) {}
