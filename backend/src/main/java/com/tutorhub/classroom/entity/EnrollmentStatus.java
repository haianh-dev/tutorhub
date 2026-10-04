package com.tutorhub.classroom.entity;

/**
 * Trạng thái ghi danh (enrollment) của học sinh vào lớp:
 * - ACTIVE: đang học trong lớp (được tính vào sĩ số, điểm danh, v.v.)
 * - LEFT: đã nghỉ/bỏ lớp (không tính sĩ số, vẫn giữ dữ liệu lịch sử).
 */
public enum EnrollmentStatus {
    ACTIVE,
    LEFT
}
