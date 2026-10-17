package com.tutorhub.assignment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tutorhub.assignment.entity.AssignmentScoreEntity;
import com.tutorhub.assignment.entity.SubmissionStatus;

public interface AssignmentScoreRepository extends JpaRepository<AssignmentScoreEntity, Long> {

    @Query("SELECT s FROM AssignmentScoreEntity s JOIN FETCH s.student WHERE s.assignment.id = :assignmentId ORDER BY s.student.fullName ASC")
    List<AssignmentScoreEntity> findByAssignmentIdWithStudent(@Param("assignmentId") Long assignmentId);

    Optional<AssignmentScoreEntity> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    boolean existsByAssignmentIdAndStatus(Long assignmentId, SubmissionStatus status);

    boolean existsByAssignmentIdAndScoreIsNotNull(Long assignmentId);

    @Query("SELECT s FROM AssignmentScoreEntity s JOIN FETCH s.assignment a JOIN FETCH a.classroom c WHERE s.student.id = :studentId AND (:classId IS NULL OR c.id = :classId) ORDER BY a.createdAt DESC")
    List<AssignmentScoreEntity> findByStudentIdAndOptionalClassId(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId
    );
}
