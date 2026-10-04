package com.tutorhub.auth.controller;

import com.tutorhub.auth.dto.CreateInvitationRequest;
import com.tutorhub.auth.dto.InvitationResponse;
import com.tutorhub.auth.security.UserPrincipal;
import com.tutorhub.auth.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý lời mời (yêu cầu xác thực).
 * POST /api/v1/invitations — Gia sư/ADMIN tạo lời mời cho học sinh/phụ huynh.
 */
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * Tạo lời mời mới.
     * Chỉ TUTOR và ADMIN mới có quyền tạo lời mời.
     *
     * @param principal Người dùng hiện tại (từ JWT — không từ request body)
     * @param request   Thông tin lời mời
     * @return Link lời mời + thông tin
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public InvitationResponse createInvitation(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateInvitationRequest request
    ) {
        return invitationService.createInvitation(principal.id(), principal.role(), request);
    }
}
