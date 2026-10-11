package com.tutorhub.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateScheduleRuleRequest(
        @NotNull(message = "dayOfWeek không được để trống")
        @Min(value = 1, message = "dayOfWeek phải từ 1 (Thứ Hai) đến 7 (Chủ Nhật)")
        @Max(value = 7, message = "dayOfWeek phải từ 1 (Thứ Hai) đến 7 (Chủ Nhật)")
        Short dayOfWeek,

        @NotNull(message = "startTime không được để trống")
        LocalTime startTime,

        @NotNull(message = "endTime không được để trống")
        LocalTime endTime,

        @NotNull(message = "effectiveFrom không được để trống")
        LocalDate effectiveFrom,

        LocalDate effectiveTo
) {}
