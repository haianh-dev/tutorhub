package com.tutorhub.classroom.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.EnrollmentStatus;

/**
 * Repository cho bảng `classes`.
 *
 * <p>
 * Các quy tắc ownership (BR-1):
 * </p>
 * <ul>
 * <li>TUTOR chỉ truy cập lớp có {@code tutor_id = id của người đăng nhập} —
 * filter ở service,
 * ở đây cung cấp phương thức {@link #existsByIdAndTutorId(Long, Long)} để kiểm
 * tra nhanh.</li>
 * <li>ADMIN không cần filter, truy cập mọi lớp.</li>
 * </ul>
 */
@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    // ─── Danh sách lớp theo phân quyền ─────────────────────────────────────

    /**
     * Danh sách lớp của một TUTOR (lọc theo status + search theo tên).
     * Dùng cho TUTOR lọc lớp của mình.
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE c.tutor.id = :tutorId " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ClassEntity> findByTutorIdAndFilters(
            @Param("tutorId") Long tutorId,
            @Param("status") ClassStatus status,
            @Param("q") String q,
            Pageable pageable);

    /**
     * Danh sách tất cả lớp (dành cho ADMIN — không lọc tutor_id).
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE (:status IS NULL OR c.status = :status) " +
            "AND (:q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ClassEntity> findAllWithFilters(
            @Param("status") ClassStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query("SELECT DISTINCT c FROM ClassEntity c JOIN ClassEnrollment e ON e.clazz = c " +
            "WHERE e.student.id = :studentId AND e.status = :enrollmentStatus " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ClassEntity> findByStudentIdAndFilters(
            @Param("studentId") Long studentId,
            @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus,
            @Param("status") ClassStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT c.*
            FROM classes c
            JOIN class_enrollments e ON e.class_id = c.id AND e.status = 'ACTIVE'
            JOIN parent_students ps ON ps.student_id = e.student_id
            WHERE ps.parent_id = :parentId
              AND (:studentId IS NULL OR ps.student_id = :studentId)
              AND (:status IS NULL OR c.status = :status)
              AND (:q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
                    ORDER BY c.created_at DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT c.id)
            FROM classes c
            JOIN class_enrollments e ON e.class_id = c.id AND e.status = 'ACTIVE'
            JOIN parent_students ps ON ps.student_id = e.student_id
            WHERE ps.parent_id = :parentId
              AND (:studentId IS NULL OR ps.student_id = :studentId)
              AND (:status IS NULL OR c.status = :status)
              AND (:q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
            """, nativeQuery = true)
    Page<ClassEntity> findByParentIdAndFilters(
            @Param("parentId") Long parentId,
            @Param("studentId") Long studentId,
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable);

    // ─── Ownership check nhanh ─────────────────────────────────────────────

    /**
     * Kiểm tra lớp có tồn tại và thuộc về tutorId hay không.
     * Dùng cho TUTOR: nếu false → trả 404 (không lộ sự tồn tại của lớp).
     */
    boolean existsByIdAndTutorId(Long classId, Long tutorId);

    // ─── Student count (sĩ số lớp: đếm enrollment ACTIVE) ──────────────────

    /**
     * Đếm số học sinh đang học (status=ACTIVE enrollment) trong một lớp.
     * Dùng cho ClassResponse.studentCount và validate đổi classType
     * GROUP→ONE_ON_ONE.
     */
    @Query("SELECT COUNT(e) FROM ClassEnrollment e " +
            "WHERE e.clazz.id = :classId AND e.status = 'ACTIVE'")
    int countActiveEnrollmentsByClassId(@Param("classId") Long classId);

    /**
     * Find class với tutor đã được fetch (dùng để tránh N+1 khi get detail/list).
     */
    @Query("SELECT c FROM ClassEntity c LEFT JOIN FETCH c.tutor WHERE c.id = :id")
    Optional<ClassEntity> findByIdWithTutor(@Param("id") Long id);
}
