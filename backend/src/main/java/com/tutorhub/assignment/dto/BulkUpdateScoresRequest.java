package com.tutorhub.assignment.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BulkUpdateScoresRequest(
    @NotEmpty(message = "Danh sách điểm không được để trống")
    @Valid
    List<ScoreItemRequest> scores
) {}
