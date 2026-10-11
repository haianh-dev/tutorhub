package com.tutorhub.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.tutorhub.schedule.entity.ScheduleRuleEntity;

public record ScheduleRuleResponse(
        Long id,
        Long classId,
        short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
    public static ScheduleRuleResponse from(ScheduleRuleEntity entity) {
        return new ScheduleRuleResponse(
                entity.getId(),
                entity.getClazz().getId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo()
        );
    }
}
