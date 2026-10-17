package com.tutorhub.attendance.dto;

import com.tutorhub.attendance.entity.AttendanceStatus;

import jakarta.validation.constraints.NotNull;

public record AttendanceItemRequest(
        @NotNull(message = "studentId không được để trống")
        Long studentId,

        @NotNull(message = "status không được để trống")
        AttendanceStatus status,

        String note
) {}
