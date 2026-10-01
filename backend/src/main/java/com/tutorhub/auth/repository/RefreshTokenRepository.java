package com.tutorhub.auth.repository;

import com.tutorhub.auth.entity.RefreshToken;
import com.tutorhub.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository cho entity {@link RefreshToken}.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedAtIsNull(User user);

    void deleteByExpiresAtBefore(Instant time);
}
