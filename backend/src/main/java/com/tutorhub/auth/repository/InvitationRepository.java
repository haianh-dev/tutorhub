package com.tutorhub.auth.repository;

import com.tutorhub.auth.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository cho bảng invitations.
 * Dùng SELECT ... FOR UPDATE để tránh race condition khi hai request
 * cùng cố gắng chấp nhận một lời mời.
 */
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    /** Tìm lời mời theo token hash (không lock). */
    Optional<Invitation> findByTokenHash(String tokenHash);

    /**
     * Tìm lời mời theo token hash với pessimistic write lock.
     * Dùng khi thực hiện accept-invitation để chặn concurrent acceptance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.tokenHash = :tokenHash")
    Optional<Invitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
