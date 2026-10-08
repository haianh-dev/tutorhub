package com.tutorhub.schedule.entity;

/**
 * Trạng thái của một buổi học (Session).
 * Lưu trong DB dưới dạng VARCHAR(20) theo constraint chk_sessions_status.
 */
public enum SessionStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
