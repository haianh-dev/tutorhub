package com.tutorhub.user.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ParentStudentRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsByParentIdAndStudentId(Long parentId, Long studentId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM parent_students WHERE parent_id = ? AND student_id = ?)",
                Boolean.class,
                parentId,
                studentId);
        return Boolean.TRUE.equals(exists);
    }

    public List<Long> findStudentIdsByParentId(Long parentId) {
        return jdbcTemplate.queryForList(
                "SELECT student_id FROM parent_students WHERE parent_id = ?",
                Long.class,
                parentId
        );
    }

    public boolean hasActiveEnrollmentInClass(Long parentId, Long classId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM parent_students ps
                    JOIN class_enrollments e ON e.student_id = ps.student_id
                    WHERE ps.parent_id = ? AND e.class_id = ? AND e.status = 'ACTIVE'
                )
                """, Boolean.class, parentId, classId);
        return Boolean.TRUE.equals(exists);
    }
}