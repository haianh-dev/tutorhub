package com.tutorhub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body cho POST /api/v1/auth/accept-invitation.
 * Người được mời điền thông tin để tạo tài khoản và chấp nhận lời mời.
 */
public record AcceptInvitationRequest(
    /** Raw token từ link lời mời (chưa băm). */
    @NotBlank(message = "Token không được để trống")
    String token,

    /** Mật khẩu mới — tối thiểu 8 ký tự. */
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    String password,

    /** Họ tên đầy đủ — bắt buộc. */
    @NotBlank(message = "Họ tên không được để trống")
    String fullName,

    /** Số điện thoại — tùy chọn. */
    String phone
) {}
