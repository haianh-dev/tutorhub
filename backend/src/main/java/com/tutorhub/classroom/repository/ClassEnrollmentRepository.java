package com.tutorhub.classroom.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tutorhub.classroom.entity.ClassEnrollment;
import com.tutorhub.classroom.entity.EnrollmentStatus;

/**
 * Repository cho bảng `class_enrollments`.
 */
@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    /**
     * Đếm số ghi danh có {@code status = ACTIVE} trong một lớp.
     * = số học sinh đang học của lớp.
     */
    int countByClazzIdAndStatus(Long classId, EnrollmentStatus status);

    boolean existsByClazzIdAndStudentIdAndStatus(Long classId, Long studentId, EnrollmentStatus status);

    Optional<ClassEnrollment> findByClazzIdAndStudentId(Long classId, Long studentId);

    @Query("SELECT e.clazz.id AS classId, COUNT(e.id) AS studentCount " +
            "FROM ClassEnrollment e WHERE e.clazz.id IN :classIds AND e.status = :status " +
            "GROUP BY e.clazz.id")
    List<ActiveEnrollmentCount> countActiveByClassIds(
            @Param("classIds") Collection<Long> classIds,
            @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM ClassEnrollment e JOIN FETCH e.student JOIN FETCH e.clazz " +
            "WHERE e.clazz.id = :classId ORDER BY e.enrolledAt DESC")
    List<ClassEnrollment> findByClazzIdWithStudentOrderByEnrolledAtDesc(@Param("classId") Long classId);

    @Query("SELECT e FROM ClassEnrollment e JOIN FETCH e.student JOIN FETCH e.clazz " +
            "WHERE e.clazz.id = :classId AND e.status = :status ORDER BY e.enrolledAt DESC")
    List<ClassEnrollment> findByClazzIdAndStatusWithStudentOrderByEnrolledAtDesc(
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status);
}
