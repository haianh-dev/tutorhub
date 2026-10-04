package com.tutorhub.classroom.entity;

/**
 * Trạng thái lớp học theo D-20 (xóa mềm/archive thay vì xóa cứng):
 * - ACTIVE: đang hoạt động, hiển thị trong danh sách mặc định.
 * - ARCHIVED: đã lưu trữ (archive), không hiển thị mặc định; vẫn giữ dữ liệu lịch sử điểm danh/điểm/học phí.
 */
public enum ClassStatus {
    ACTIVE,
    ARCHIVED
}
