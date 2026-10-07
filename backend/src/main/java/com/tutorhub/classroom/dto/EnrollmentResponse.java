package com.tutorhub.classroom.dto;

import com.tutorhub.classroom.entity.ClassEnrollment;
import com.tutorhub.classroom.entity.EnrollmentStatus;

import java.time.Instant;

/**
 * Response DTO biểu diễn bản ghi ghi danh của học sinh trong một lớp.
 */
public record EnrollmentResponse(
    Long id,
    Long classId,
    Long studentId,
    String studentName,
    String studentEmail,
    String studentPhone,
    EnrollmentStatus status,
    Instant enrolledAt,
    Instant leftAt
) {
    public static EnrollmentResponse from(ClassEnrollment enrollment) {
        return new EnrollmentResponse(
            enrollment.getId(),
            enrollment.getClazz().getId(),
            enrollment.getStudent().getId(),
            enrollment.getStudent().getFullName(),
            enrollment.getStudent().getEmail(),
            enrollment.getStudent().getPhone(),
            enrollment.getStatus(),
            enrollment.getEnrolledAt(),
            enrollment.getLeftAt()
        );
    }
}
