package com.tutorhub.assignment.dto;

import java.math.BigDecimal;

import com.tutorhub.assignment.entity.SubmissionStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ScoreItemRequest(
    @NotNull(message = "studentId không được để trống")
    Long studentId,

    @DecimalMin(value = "0.00", message = "Điểm số không được nhỏ hơn 0")
    @DecimalMax(value = "10.00", message = "Điểm số không được lớn hơn 10")
    @Digits(integer = 2, fraction = 2, message = "Điểm số chỉ được tối đa 2 chữ số thập phân")
    BigDecimal score,

    @NotNull(message = "Trạng thái nộp bài không được để trống")
    SubmissionStatus status,

    String feedback
) {}
