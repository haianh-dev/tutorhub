package com.tutorhub.auth.repository;

import com.tutorhub.auth.entity.RefreshToken;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration Test cho {@link RefreshTokenRepository}.
 * Kiểm tra lưu token, tìm theo hash, lọc token hợp lệ và xóa token hết hạn.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        // Cleanup dữ liệu đã commit từ @SpringBootTest integration tests chạy trước.
        jdbcTemplate.update("DELETE FROM invitations");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM parent_students");
        jdbcTemplate.update("DELETE FROM class_enrollments");
        jdbcTemplate.update("DELETE FROM classes");
        jdbcTemplate.update("DELETE FROM users");
        entityManager.flush();
        entityManager.clear();

        sampleUser = userRepository.save(User.builder()
                .email("tokenuser@example.com")
                .passwordHash("pwd")
                .fullName("Token User")
                .role(Role.TUTOR)
                .build());
    }

    @Test
    @DisplayName("Lưu và tìm RefreshToken theo tokenHash thành công")
    void shouldSaveAndFindByTokenHash() {
        RefreshToken token = RefreshToken.builder()
                .user(sampleUser)
                .tokenHash("sample-hash-123")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        RefreshToken saved = refreshTokenRepository.save(token);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isValid()).isTrue();

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("sample-hash-123");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(sampleUser.getId());
    }

    @Test
    @DisplayName("findByUserAndRevokedAtIsNull chỉ trả về các token chưa bị thu hồi")
    void shouldFindOnlyActiveTokensForUser() {
        RefreshToken activeToken = RefreshToken.builder()
                .user(sampleUser)
                .tokenHash("active-token")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .user(sampleUser)
                .tokenHash("revoked-token")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revokedAt(Instant.now())
                .build();

        refreshTokenRepository.saveAll(List.of(activeToken, revokedToken));

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedAtIsNull(sampleUser);
        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getTokenHash()).isEqualTo("active-token");
    }

    @Test
    @DisplayName("deleteByExpiresAtBefore xóa các token đã hết hạn")
    void shouldDeleteExpiredTokens() {
        RefreshToken expired = RefreshToken.builder()
                .user(sampleUser)
                .tokenHash("expired-token")
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        RefreshToken valid = RefreshToken.builder()
                .user(sampleUser)
                .tokenHash("valid-token")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.saveAll(List.of(expired, valid));

        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());

        assertThat(refreshTokenRepository.findByTokenHash("expired-token")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("valid-token")).isPresent();
    }

    @Test
    @DisplayName("Ném DataIntegrityViolationException khi trùng token_hash (Unique constraint)")
    void shouldFailWhenTokenHashIsDuplicate() {
        RefreshToken t1 = RefreshToken.builder()
                .user(sampleUser)
                .tokenHash("duplicate-hash")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.saveAndFlush(t1);

        RefreshToken t2 = RefreshToken.builder()
                .user(sampleUser)
                .tokenHash("duplicate-hash")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(t2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
