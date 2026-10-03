package com.tutorhub.user.controller;

import com.tutorhub.auth.dto.ChangePasswordRequest;
import com.tutorhub.auth.security.UserPrincipal;
import com.tutorhub.auth.service.AuthService;
import com.tutorhub.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthService authService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.getCurrentUser(principal.id());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.id(), request);
    }
}