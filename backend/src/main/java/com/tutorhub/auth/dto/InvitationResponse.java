package com.tutorhub.auth.dto;

import com.tutorhub.user.entity.Role;

import java.time.Instant;

/**
 * Response khi tạo lời mời thành công (POST /api/v1/invitations)
 * hoặc khi kiểm tra lời mời (GET /api/v1/auth/invitations/{token}).
 */
public record InvitationResponse(
    /** Link đầy đủ gửi cho người được mời qua Zalo/tin nhắn. */
    String link,

    /** Vai trò sẽ nhận (STUDENT hoặc PARENT). */
    Role role,

    /** Email gợi ý (nếu có). */
    String email,

    /** Tên lớp học (nếu kèm lớp). */
    String className,

    /** Thời điểm hết hạn của lời mời (UTC). */
    Instant expiresAt
) {}
