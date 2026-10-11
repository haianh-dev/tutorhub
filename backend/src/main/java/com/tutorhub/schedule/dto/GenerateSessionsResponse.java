package com.tutorhub.schedule.dto;

import java.time.LocalDate;
import java.util.List;

public record GenerateSessionsResponse(
        int created,
        List<SessionConflictItem> conflicts
) {
    public record SessionConflictItem(
            LocalDate date,
            Long conflictingSessionId,
            String reason
    ) {}
}
