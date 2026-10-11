package com.tutorhub.schedule.dto;

import java.time.Instant;
import java.util.List;

import com.tutorhub.schedule.entity.SessionEntity;
import com.tutorhub.schedule.entity.SessionStatus;

/**
 * Response DTO cho các endpoint buổi học (Phase 3 — T3.2, T3.5).
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
        Instant updatedAt,
        List<String> warnings
) {
    public static SessionResponse from(SessionEntity session) {
        return from(session, List.of());
    }

    public static SessionResponse from(SessionEntity session, List<String> warnings) {
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
                session.getUpdatedAt(),
                warnings != null ? warnings : List.of()
        );
    }
}
