package com.tutorhub.classroom.entity;

import java.time.Instant;

import com.tutorhub.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity biểu diễn việc ghi danh (enrollment) một học sinh vào một lớp học.
 * Ánh xạ bảng `class_enrollments`.
 *
 * <p>
 * Ràng buộc từ V4 migration:
 * </p>
 * <ul>
 * <li>UNIQUE (class_id, student_id) — một học sinh không ghi danh 2 lần cùng
 * lớp</li>
 * <li>CHECK status IN ('ACTIVE','LEFT')</li>
 * <li>FK ON DELETE CASCADE cho cả class_id và student_id</li>
 * </ul>
 *
 * <p>
 * Sử dụng trong T2.1 để:
 * </p>
 * <ul>
 * <li>Đếm sĩ số lớp (studentCount = count ACTIVE enrollment cho lớp)</li>
 * <li>Validate đổi GROUP → ONE_ON_ONE (nếu ≥2 ACTIVE → 422 không cho đổi)</li>
 * </ul>
 */
@Entity
@Table(name = "class_enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity clazz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (enrolledAt == null) {
            enrolledAt = now;
        }
        if (status == null) {
            status = EnrollmentStatus.ACTIVE;
        }
    }
}
