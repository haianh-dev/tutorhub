package com.tutorhub.schedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tutorhub.schedule.entity.ScheduleRuleEntity;

/**
 * Repository cho bảng `schedule_rules`.
 */
public interface ScheduleRuleRepository extends JpaRepository<ScheduleRuleEntity, Long> {

    @Query("SELECT r FROM ScheduleRuleEntity r " +
           "JOIN FETCH r.clazz c " +
           "WHERE c.id = :classId " +
           "ORDER BY r.dayOfWeek ASC, r.startTime ASC")
    List<ScheduleRuleEntity> findByClassId(@Param("classId") Long classId);
}
