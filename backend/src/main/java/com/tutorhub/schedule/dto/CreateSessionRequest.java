package com.tutorhub.schedule.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body tạo buổi học đơn lẻ: POST /api/v1/classes/{id}/sessions.
 */
public record CreateSessionRequest(
        @NotNull(message = "Thời gian bắt đầu không được để trống")
        Instant startAt,

        @NotNull(message = "Thời gian kết thúc không được để trống")
        Instant endAt,

        @Size(max = 255, message = "Chủ đề tối đa 255 ký tự")
        String topic,

        @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
        String note
) {
}
