package com.tutorhub.classroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tutorhub.classroom.entity.ClassEnrollment;
import com.tutorhub.classroom.entity.EnrollmentStatus;

/**
 * Repository cho bảng `class_enrollments`.
 *
 * <p>
 * Sử dụng trong T2.1 chủ yếu để đếm sĩ số lớp khi:
 * </p>
 * <ul>
 * <li>Map ClassResponse (điền studentCount).</li>
 * <li>Validate đổi classType GROUP → ONE_ON_ONE (nếu ≥2 ACTIVE → 422).</li>
 * </ul>
 *
 * <p>
 * CRUD ghi danh (thêm/bỏ học sinh) là scope T2.2.
 * </p>
 */
@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    /**
     * Đếm số ghi danh có {@code status = ACTIVE} trong một lớp.
     * = số học sinh đang học của lớp.
     */
    int countByClazzIdAndStatus(Long classId, EnrollmentStatus status);

    boolean existsByClazzIdAndStudentIdAndStatus(Long classId, Long studentId, EnrollmentStatus status);

    @Query("SELECT e.clazz.id AS classId, COUNT(e.id) AS studentCount " +
            "FROM ClassEnrollment e WHERE e.clazz.id IN :classIds AND e.status = :status " +
            "GROUP BY e.clazz.id")
    java.util.List<ActiveEnrollmentCount> countActiveByClassIds(
            @Param("classIds") java.util.Collection<Long> classIds,
            @Param("status") EnrollmentStatus status);
}
