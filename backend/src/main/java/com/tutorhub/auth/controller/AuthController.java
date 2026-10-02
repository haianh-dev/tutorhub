package com.tutorhub.auth.controller;

import com.tutorhub.auth.dto.AuthResponse;
import com.tutorhub.auth.dto.LoginRequest;
import com.tutorhub.auth.dto.RegisterResponse;
import com.tutorhub.auth.dto.RegisterTutorRequest;
import com.tutorhub.auth.service.AuthService;
import com.tutorhub.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-tutor")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse registerTutor(@Valid @RequestBody RegisterTutorRequest request) {
        UserResponse user = authService.registerTutor(request);
        return new RegisterResponse(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
