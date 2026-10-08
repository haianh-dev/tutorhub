package com.tutorhub.schedule.repository;

import java.time.Instant;
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
}
