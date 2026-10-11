package com.tutorhub.schedule.dto;

import java.time.LocalDate;
import java.util.List;

public record GenerateSessionsResponse(
        int created,
        List<SessionConflictItem> conflicts,
        List<String> warnings
) {
    public GenerateSessionsResponse(int created, List<SessionConflictItem> conflicts) {
        this(created, conflicts, List.of());
    }

    public record SessionConflictItem(
            LocalDate date,
            Long conflictingSessionId,
            String reason
    ) {}
}
