package com.tutorhub.schedule.dto;

import java.time.Instant;

import com.tutorhub.schedule.entity.SessionEntity;
import com.tutorhub.schedule.entity.SessionStatus;

/**
 * Response DTO cho các endpoint buổi học.
 */
public record SessionResponse(
        Long id,
        Long classId,
        String className,
        Long tutorId,
        Long ruleId,
        Instant startAt,
        Instant endAt,
        SessionStatus status,
        String topic,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
    public static SessionResponse from(SessionEntity session) {
        return new SessionResponse(
                session.getId(),
                session.getClazz().getId(),
                session.getClazz().getName(),
                session.getTutor().getId(),
                session.getRuleId(),
                session.getStartAt(),
                session.getEndAt(),
                session.getStatus(),
                session.getTopic(),
                session.getNote(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
