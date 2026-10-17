package com.tutorhub.classroom.event;

import com.tutorhub.user.entity.User;

/**
 * Event phát ra khi có học sinh được ghi danh (mới hoặc kích hoạt lại) vào lớp học.
 */
public record StudentEnrolledEvent(Long classId, User student) {}
