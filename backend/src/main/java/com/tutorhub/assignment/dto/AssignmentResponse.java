package com.tutorhub.assignment.dto;

import java.time.Instant;

import com.tutorhub.assignment.entity.AssignmentEntity;
import com.tutorhub.assignment.entity.AssignmentType;

public record AssignmentResponse(
    Long id,
    Long classId,
    String className,
    String title,
    String description,
    AssignmentType type,
    Instant dueAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static AssignmentResponse from(AssignmentEntity entity) {
        return new AssignmentResponse(
            entity.getId(),
            entity.getClassroom().getId(),
            entity.getClassroom().getName(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getAssignmentType(),
            entity.getDueAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
