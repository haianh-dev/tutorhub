package com.tutorhub.assignment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.tutorhub.assignment.entity.AssignmentScoreEntity;
import com.tutorhub.assignment.entity.SubmissionStatus;

public record AssignmentScoreResponse(
    Long id,
    Long assignmentId,
    String assignmentTitle,
    Long classId,
    String className,
    Long studentId,
    String studentName,
    String studentEmail,
    BigDecimal score,
    SubmissionStatus status,
    String feedback,
    Instant gradedAt,
    Instant updatedAt
) {
    public static AssignmentScoreResponse from(AssignmentScoreEntity entity) {
        return new AssignmentScoreResponse(
            entity.getId(),
            entity.getAssignment().getId(),
            entity.getAssignment().getTitle(),
            entity.getAssignment().getClassroom() != null ? entity.getAssignment().getClassroom().getId() : null,
            entity.getAssignment().getClassroom() != null ? entity.getAssignment().getClassroom().getName() : null,
            entity.getStudent().getId(),
            entity.getStudent().getFullName(),
            entity.getStudent().getEmail(),
            entity.getScore(),
            entity.getStatus(),
            entity.getFeedback(),
            entity.getGradedAt(),
            entity.getUpdatedAt()
        );
    }
}
