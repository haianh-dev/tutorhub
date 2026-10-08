package com.tutorhub.schedule.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tutorhub.auth.security.UserPrincipal;
import com.tutorhub.schedule.dto.CreateSessionRequest;
import com.tutorhub.schedule.dto.SessionResponse;
import com.tutorhub.schedule.dto.UpdateSessionRequest;
import com.tutorhub.schedule.service.SessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý buổi học (Phase 3 — T3.2).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * POST /classes/{id}/sessions — Tạo buổi học đơn lẻ cho lớp.
     */
    @PostMapping("/classes/{id}/sessions")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateSessionRequest request
    ) {
        return sessionService.createSession(principal.id(), principal.role(), id, request);
    }

    /**
     * PUT /sessions/{id} — Cập nhật buổi học (re-check trùng giờ).
     */
    @PutMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public SessionResponse updateSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSessionRequest request
    ) {
        return sessionService.updateSession(principal.id(), principal.role(), id, request);
    }

    /**
     * POST /sessions/{id}/complete — Đánh dấu hoàn thành buổi học.
     */
    @PostMapping("/sessions/{id}/complete")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public SessionResponse completeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return sessionService.completeSession(principal.id(), principal.role(), id);
    }

    /**
     * POST /sessions/{id}/cancel — Hủy buổi học (giải phóng khung giờ).
     */
    @PostMapping("/sessions/{id}/cancel")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public SessionResponse cancelSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return sessionService.cancelSession(principal.id(), principal.role(), id);
    }

    /**
     * GET /sessions/{id} — Xem chi tiết một buổi học.
     */
    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN', 'STUDENT', 'PARENT')")
    public SessionResponse getSessionById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return sessionService.getSessionById(principal.id(), principal.role(), id);
    }

    /**
     * GET /sessions — Xem lịch học theo khoảng ngày.
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN', 'STUDENT', 'PARENT')")
    public List<SessionResponse> getSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId
    ) {
        return sessionService.getSessions(
                principal.id(),
                principal.role(),
                from,
                to,
                classId,
                studentId
        );
    }
}
