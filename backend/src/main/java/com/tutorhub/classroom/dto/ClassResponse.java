package com.tutorhub.classroom.dto;

import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;

import java.time.Instant;
import java.util.List;

/**
 * Response body cho mọi endpoint lớp học (danh sách, chi tiết, tạo mới, cập nhật, lưu trữ).
 * Định dạng theo quy ước DTO record: không trả entity trực tiếp ra API.
 *
 * @param studentCount Số học sinh đang học ACTIVE trong lớp (tính từ class_enrollments với status=ACTIVE).
 * @param warnings     Danh sách cảnh báo (ví dụ lớp GROUP có dưới 2 học sinh).
 */
public record ClassResponse(
    Long id,
    Long tutorId,
    String name,
    String subject,
    String description,
    ClassType classType,
    ClassStatus status,
    Instant createdAt,
    Instant updatedAt,
    Integer studentCount,
    List<String> warnings
) {
    /**
     * Map từ entity sang response DTO, kèm theo studentCount đã tính trước và tự động sinh warnings.
     */
    public static ClassResponse from(ClassEntity entity, int studentCount) {
        List<String> warnings = (entity.getClassType() == ClassType.GROUP && studentCount < 2)
            ? List.of("Lớp nhóm hiện có ít hơn 2 học sinh")
            : List.of();
        return from(entity, studentCount, warnings);
    }

    public static ClassResponse from(ClassEntity entity, int studentCount, List<String> warnings) {
        return new ClassResponse(
            entity.getId(),
            entity.getTutor().getId(),
            entity.getName(),
            entity.getSubject(),
            entity.getDescription(),
            entity.getClassType(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            studentCount,
            warnings != null ? warnings : List.of()
        );
    }
}
