package com.tutorhub.auth.repository;

import com.tutorhub.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PasswordResetOwnershipRepository {

    private static final String STUDENT_OWNERSHIP_SQL = """
        SELECT EXISTS (
            SELECT 1
            FROM classes c
            JOIN class_enrollments e ON e.class_id = c.id
            WHERE c.tutor_id = ? AND c.status = 'ACTIVE'
              AND e.student_id = ? AND e.status = 'ACTIVE'
        )
        """;

    private static final String PARENT_OWNERSHIP_SQL = """
        SELECT EXISTS (
            SELECT 1
            FROM classes c
            JOIN class_enrollments e ON e.class_id = c.id
            JOIN parent_students ps ON ps.student_id = e.student_id
            WHERE c.tutor_id = ? AND c.status = 'ACTIVE'
              AND e.status = 'ACTIVE' AND ps.parent_id = ?
        )
        """;

    private final JdbcTemplate jdbcTemplate;

    public boolean tutorOwnsResetTarget(Long tutorId, Long targetUserId, Role targetRole) {
        String query = switch (targetRole) {
            case STUDENT -> STUDENT_OWNERSHIP_SQL;
            case PARENT -> PARENT_OWNERSHIP_SQL;
            default -> null;
        };
        if (query == null) {
            return false;
        }
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(query, Boolean.class, tutorId, targetUserId));
    }
}