package com.tutorhub.schedule.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tutorhub.schedule.entity.SessionEntity;

/**
 * Repository cho bảng `sessions`.
 */
public interface SessionRepository extends JpaRepository<SessionEntity, Long>, JpaSpecificationExecutor<SessionEntity> {

    /**
     * Tìm các buổi học của gia sư đang chồng lấn thời gian với khoảng [startAt, endAt).
     * Chỉ xét các buổi không bị CANCELLED.
     * Có thể truyền {@code excludeSessionId} khi thực hiện cập nhật buổi học.
     */
    @Query("SELECT s FROM SessionEntity s " +
           "JOIN FETCH s.clazz c " +
           "WHERE s.tutor.id = :tutorId " +
           "AND s.status <> 'CANCELLED' " +
           "AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId) " +
           "AND s.startAt < :endAt AND s.endAt > :startAt " +
           "ORDER BY s.startAt ASC")
    List<SessionEntity> findOverlappingSessions(
            @Param("tutorId") Long tutorId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeSessionId") Long excludeSessionId
    );

    /**
     * Tìm các buổi học của một tập hợp học sinh đang bị chồng lấn thời gian trong các lớp khác
     * mà học sinh đang ghi danh ACTIVE (Phase 3 — T3.5: Cảnh báo trùng lịch học sinh).
     *
     * <p>Loại trừ:
     * - Các buổi bị CANCELLED.
     * - Chính buổi học hiện tại (:excludeSessionId).
     * - Các buổi thuộc cùng lớp học hiện tại (:excludeClassId).
     * </p>
     */
    @Query("SELECT s, e.student.id, e.student.fullName FROM SessionEntity s " +
           "JOIN s.clazz c " +
           "JOIN ClassEnrollment e ON e.clazz.id = c.id " +
           "WHERE e.student.id IN :studentIds " +
           "AND e.status = com.tutorhub.classroom.entity.EnrollmentStatus.ACTIVE " +
           "AND s.status <> com.tutorhub.schedule.entity.SessionStatus.CANCELLED " +
           "AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId) " +
           "AND c.id <> :excludeClassId " +
           "AND s.startAt < :endAt AND s.endAt > :startAt " +
           "ORDER BY s.startAt ASC")
    List<Object[]> findStudentOverlappingSessions(
            @Param("studentIds") Collection<Long> studentIds,
            @Param("excludeClassId") Long excludeClassId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeSessionId") Long excludeSessionId
    );
}
