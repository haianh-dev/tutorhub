package com.tutorhub.classroom.entity;

/**
 * Loại lớp học theo D-23 CONFIRMED:
 * - ONE_ON_ONE: 1:1 (tối đa 1 học sinh đang học active)
 * - GROUP: nhóm (từ 2 học sinh trở lên mới coi là đủ — dưới 2 chỉ cảnh báo, không chặn)
 */
public enum ClassType {
    ONE_ON_ONE,
    GROUP
}
