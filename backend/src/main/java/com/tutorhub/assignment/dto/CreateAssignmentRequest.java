package com.tutorhub.assignment.dto;

import java.time.Instant;

import com.tutorhub.assignment.entity.AssignmentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssignmentRequest(
    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    String title,

    String description,

    @NotNull(message = "Loại bài tập không được để trống")
    AssignmentType type,

    Instant dueAt
) {}
