package com.tutorhub.attendance.dto;

import java.time.Instant;

import com.tutorhub.attendance.entity.AttendanceStatus;

/**
 * DTO dùng cho GET /students/{studentId}/attendance (lịch sử điểm danh).
 * Bao gồm thêm thông tin buổi học và lớp.
 */
public record AttendanceHistoryResponse(
        Long attendanceId,
        Long sessionId,
        Instant sessionStartAt,
        Instant sessionEndAt,
        String sessionTopic,
        Long classId,
        String className,
        AttendanceStatus status,
        String note,
        Instant updatedAt
) {
    public static AttendanceHistoryResponse from(com.tutorhub.attendance.entity.AttendanceEntity entity) {
        var session = entity.getSession();
        return new AttendanceHistoryResponse(
                entity.getId(),
                session.getId(),
                session.getStartAt(),
                session.getEndAt(),
                session.getTopic(),
                session.getClazz().getId(),
                session.getClazz().getName(),
                entity.getStatus(),
                entity.getNote(),
                entity.getUpdatedAt()
        );
    }
}
