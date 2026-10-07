package com.tutorhub.classroom.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.tutorhub.classroom.dto.ClassResponse;
import com.tutorhub.classroom.dto.CreateClassRequest;
import com.tutorhub.classroom.dto.EnrollStudentRequest;
import com.tutorhub.classroom.dto.EnrollmentResponse;
import com.tutorhub.classroom.dto.UpdateClassRequest;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.service.ClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý lớp học (Phase 2 — T2.1, T2.2).
 */
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    /**
     * GET /classes — Danh sách lớp theo quyền.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT', 'PARENT', 'ADMIN')")
    public Page<ClassResponse> getClasses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) ClassStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return classService.getClasses(principal.id(), principal.role(), studentId, status, q, pageable);
    }

    /**
     * POST /classes — Tạo lớp mới.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateClassRequest request) {
        return classService.createClass(principal.id(), principal.role(), request);
    }

    /**
     * GET /classes/{id} — Chi tiết một lớp học.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT', 'PARENT', 'ADMIN')")
    public ClassResponse getClassById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return classService.getClassById(principal.id(), principal.role(), id);
    }

    /**
     * PUT /classes/{id} — Cập nhật lớp.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ClassResponse updateClass(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request) {
        return classService.updateClass(principal.id(), principal.role(), id, request);
    }

    /**
     * POST /classes/{id}/archive — Lưu trữ (archive) lớp.
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ClassResponse archiveClass(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return classService.archiveClass(principal.id(), principal.role(), id);
    }

    // ─── T2.2: Ghi danh & Quản lý học sinh ─────────────────────────────────

    /**
     * POST /classes/{id}/students — Ghi danh học sinh vào lớp.
     * Quy tắc:
     * - Trùng học sinh đang ACTIVE → 409
     * - Lớp 1:1 đã có 1 học sinh đang ACTIVE → 422 ONE_ON_ONE_FULL
     * - Khóa bi quan chống 2 request ghi danh đồng thời vào lớp 1:1
     */
    @PostMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enrollStudent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EnrollStudentRequest request) {
        return classService.enrollStudent(principal.id(), principal.role(), id, request);
    }

    /**
     * GET /classes/{id}/students — Danh sách học sinh ghi danh của lớp.
     */
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public List<EnrollmentResponse> getStudents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) EnrollmentStatus status) {
        return classService.getStudents(principal.id(), principal.role(), id, status);
    }

    /**
     * DELETE /classes/{id}/students/{studentId} — Cho học sinh rời lớp (đặt status=LEFT).
     */
    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long studentId) {
        classService.removeStudent(principal.id(), principal.role(), id, studentId);
    }
}
