package com.tutorhub.schedule.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record GenerateSessionsRequest(
        @NotNull(message = "from không được để trống")
        LocalDate from,

        @NotNull(message = "to không được để trống")
        LocalDate to
) {}
