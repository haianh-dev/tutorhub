package com.tutorhub.attendance.dto;

import java.time.Instant;

import com.tutorhub.attendance.entity.AttendanceStatus;

/**
 * DTO trả về thông tin điểm danh của một học sinh trong một buổi học.
 */
public record AttendanceRecordResponse(
        Long attendanceId,
        Long studentId,
        String studentName,
        AttendanceStatus status,
        String note,
        Instant updatedAt
) {
    public static AttendanceRecordResponse from(com.tutorhub.attendance.entity.AttendanceEntity entity) {
        return new AttendanceRecordResponse(
                entity.getId(),
                entity.getStudent().getId(),
                entity.getStudent().getFullName(),
                entity.getStatus(),
                entity.getNote(),
                entity.getUpdatedAt()
        );
    }
}
