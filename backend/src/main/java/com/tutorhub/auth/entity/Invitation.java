package com.tutorhub.auth.entity;

import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity đại diện cho lời mời do gia sư/ADMIN tạo cho học sinh hoặc phụ huynh.
 * Token được lưu dạng SHA-256 hash (không lưu raw token).
 * Dùng một lần (single-use), hết hạn 7 ngày.
 *
 * <p>Nghiệp vụ:</p>
 * <ul>
 *   <li>role=STUDENT: tạo tài khoản STUDENT; nếu classId != null thì ghi danh vào lớp đó.</li>
 *   <li>role=PARENT: tạo tài khoản PARENT; student != null → tạo liên kết parent_students.</li>
 * </ul>
 */
@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 hash của raw token. Không lưu raw token vào DB. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Vai trò sẽ được cấp cho người nhận (STUDENT hoặc PARENT). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Người tạo lời mời (gia sư hoặc ADMIN). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    /** Email gợi ý cho người được mời (tùy chọn). */
    @Column(length = 255)
    private String email;

    /**
     * ID lớp học — dùng Long thay vì @ManyToOne vì Class entity chưa tồn tại ở T1.4.
     * T2.1 sẽ tạo Class entity; lúc đó có thể nâng lên @ManyToOne nếu cần.
     */
    @Column(name = "class_id")
    private Long classId;

    /**
     * Học sinh được liên kết — bắt buộc khi role = PARENT.
     * Phụ huynh chấp nhận mời → tạo parent_students(parent_id, student_id).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Thời điểm lời mời đã được sử dụng (null = chưa dùng). */
    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Lời mời đã hết hạn (quá thời điểm expires_at). */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Lời mời đã được sử dụng. */
    public boolean isUsed() {
        return usedAt != null;
    }

    /** Lời mời hợp lệ tại thời điểm now: chưa hết hạn và chưa dùng. */
    public boolean isUsableAt(Instant now) {
        return !now.isAfter(expiresAt) && usedAt == null;
    }
}
