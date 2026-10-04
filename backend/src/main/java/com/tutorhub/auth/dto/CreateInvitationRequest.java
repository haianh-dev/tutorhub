package com.tutorhub.auth.dto;

import com.tutorhub.user.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request body cho POST /api/v1/invitations.
 * Gia sư (TUTOR) hoặc ADMIN tạo lời mời cho học sinh/phụ huynh.
 *
 * <p>Quy tắc:</p>
 * <ul>
 *   <li>role chỉ được là STUDENT hoặc PARENT.</li>
 *   <li>Khi role = PARENT: studentId bắt buộc (phụ huynh phải được liên kết với một học sinh).</li>
 *   <li>classId tùy chọn — nếu có, học sinh được tự động ghi danh khi chấp nhận.</li>
 *   <li>email tùy chọn — gợi ý cho người nhận khi điền form.</li>
 * </ul>
 */
public record CreateInvitationRequest(
    @NotNull(message = "Vai trò không được để trống")
    Role role,

    String email,

    Long classId,

    Long studentId
) {}
