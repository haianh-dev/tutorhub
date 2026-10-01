package com.tutorhub.user.entity;

/**
 * Vai trò người dùng trong hệ thống TutorHub.
 * Bốn vai trò theo quyết định D-27:
 * - ADMIN: Quản trị viên hệ thống, toàn quyền truy cập mọi tài nguyên.
 * - TUTOR: Gia sư quản lý lớp, học sinh, lịch dạy, điểm danh, điểm số, học phí.
 * - STUDENT: Học sinh xem lịch học, bài tập, điểm số, báo cáo của chính mình.
 * - PARENT: Phụ huynh xem lịch học, bài tập, điểm số, báo cáo của con.
 */
public enum Role {
    ADMIN,
    TUTOR,
    STUDENT,
    PARENT
}
