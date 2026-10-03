package com.tutorhub.auth.repository;

import com.tutorhub.auth.entity.RefreshToken;
import com.tutorhub.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("select token.user.id from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<Long> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findByUserAndRevokedAtIsNull(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :revokedAt "
        + "where token.user.id = :userId and token.revokedAt is null")
    int revokeActiveByUserId(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);

    void deleteByExpiresAtBefore(Instant time);
}
