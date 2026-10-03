package com.tutorhub.user.controller;

import com.tutorhub.auth.dto.PasswordResetLinkResponse;
import com.tutorhub.auth.security.UserPrincipal;
import com.tutorhub.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/{userId}/password-reset-link")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public PasswordResetLinkResponse createPasswordResetLink(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long userId
    ) {
        return passwordResetService.createResetLink(principal.id(), principal.role(), userId);
    }
}