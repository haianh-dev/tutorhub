package com.tutorhub.assignment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tutorhub.assignment.entity.AssignmentEntity;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, Long> {

    List<AssignmentEntity> findByClassroomIdOrderByCreatedAtDesc(Long classId);

    @Query("SELECT a FROM AssignmentEntity a JOIN FETCH a.classroom c JOIN FETCH c.tutor WHERE a.id = :id")
    Optional<AssignmentEntity> findByIdWithClassroomAndTutor(@Param("id") Long id);
}
