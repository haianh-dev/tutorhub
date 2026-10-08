package com.tutorhub.schedule.dto;

import java.time.Instant;

import jakarta.validation.constraints.Size;

/**
 * Request body cập nhật buổi học: PUT /api/v1/sessions/{id}.
 */
public record UpdateSessionRequest(
        Instant startAt,
        Instant endAt,

        @Size(max = 255, message = "Chủ đề tối đa 255 ký tự")
        String topic,

        @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
        String note
) {
}
