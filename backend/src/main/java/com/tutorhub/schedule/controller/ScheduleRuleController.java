package com.tutorhub.schedule.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tutorhub.auth.security.UserPrincipal;
import com.tutorhub.schedule.dto.CreateScheduleRuleRequest;
import com.tutorhub.schedule.dto.GenerateSessionsRequest;
import com.tutorhub.schedule.dto.GenerateSessionsResponse;
import com.tutorhub.schedule.dto.ScheduleRuleResponse;
import com.tutorhub.schedule.service.ScheduleRuleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý quy tắc lịch lặp và sinh buổi học tự động — Phase 3 (T3.4).
 */
@RestController
@RequestMapping("/api/v1/classes/{classId}/schedule-rules")
@RequiredArgsConstructor
public class ScheduleRuleController {

    private final ScheduleRuleService scheduleRuleService;

    /**
     * POST /api/v1/classes/{classId}/schedule-rules — Tạo quy tắc lịch lặp.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleRuleResponse createRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long classId,
            @Valid @RequestBody CreateScheduleRuleRequest request
    ) {
        return scheduleRuleService.createRule(principal.id(), principal.role(), classId, request);
    }

    /**
     * GET /api/v1/classes/{classId}/schedule-rules — Lấy danh sách quy tắc lịch của lớp.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public List<ScheduleRuleResponse> getRules(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long classId
    ) {
        return scheduleRuleService.getRulesByClass(principal.id(), principal.role(), classId);
    }

    /**
     * DELETE /api/v1/classes/{classId}/schedule-rules/{ruleId} — Xóa quy tắc lịch.
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long classId,
            @PathVariable Long ruleId
    ) {
        scheduleRuleService.deleteRule(principal.id(), principal.role(), classId, ruleId);
    }

    /**
     * POST /api/v1/classes/{classId}/schedule-rules/{ruleId}/generate — Sinh buổi học từ quy tắc.
     */
    @PostMapping("/{ruleId}/generate")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public GenerateSessionsResponse generateSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long classId,
            @PathVariable Long ruleId,
            @Valid @RequestBody GenerateSessionsRequest request
    ) {
        return scheduleRuleService.generateSessions(principal.id(), principal.role(), classId, ruleId, request);
    }
}
