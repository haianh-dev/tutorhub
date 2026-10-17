package com.tutorhub.attendance.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tutorhub.attendance.entity.AttendanceEntity;

/**
 * Repository cho bảng `attendance`.
 */
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    @Query("SELECT a FROM AttendanceEntity a " +
           "JOIN FETCH a.student s " +
           "WHERE a.session.id = :sessionId " +
           "ORDER BY s.fullName ASC")
    List<AttendanceEntity> findBySessionIdWithStudent(@Param("sessionId") Long sessionId);

    Optional<AttendanceEntity> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query("SELECT a FROM AttendanceEntity a " +
           "JOIN FETCH a.session se " +
           "JOIN FETCH se.clazz c " +
           "WHERE a.student.id = :studentId " +
           "AND (:classId IS NULL OR c.id = :classId) " +
           "AND (cast(:from as timestamp) IS NULL OR se.startAt >= :from) " +
           "AND (cast(:to as timestamp) IS NULL OR se.startAt <= :to) " +
           "ORDER BY se.startAt DESC")
    List<AttendanceEntity> findAttendanceHistory(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
