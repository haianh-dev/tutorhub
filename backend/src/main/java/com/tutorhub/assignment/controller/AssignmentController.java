package com.tutorhub.assignment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tutorhub.assignment.dto.AssignmentResponse;
import com.tutorhub.assignment.dto.CreateAssignmentRequest;
import com.tutorhub.assignment.dto.UpdateAssignmentRequest;
import com.tutorhub.assignment.service.AssignmentService;
import com.tutorhub.auth.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý Bài tập và Thang điểm 10 cố định (T5.1).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * POST /api/v1/classes/{classId}/assignments
     * TUTOR/ADMIN tạo bài tập mới cho lớp học.
     */
    @PostMapping("/classes/{classId}/assignments")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse createAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("classId") Long classId,
            @Valid @RequestBody CreateAssignmentRequest request
    ) {
        return assignmentService.createAssignment(
                principal.id(),
                principal.role(),
                classId,
                request
        );
    }

    /**
     * GET /api/v1/classes/{classId}/assignments
     * Danh sách bài tập của lớp (TUTOR phụ trách, ADMIN, STUDENT/PARENT trong lớp).
     */
    @GetMapping("/classes/{classId}/assignments")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByClass(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("classId") Long classId
    ) {
        List<AssignmentResponse> result = assignmentService.getAssignmentsByClass(
                principal.id(),
                principal.role(),
                classId
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/assignments/{id}
     * Chi tiết bài tập (TUTOR phụ trách, ADMIN, STUDENT/PARENT trong lớp).
     */
    @GetMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> getAssignmentDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id
    ) {
        AssignmentResponse result = assignmentService.getAssignmentDetail(
                principal.id(),
                principal.role(),
                id
        );
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/v1/assignments/{id}
     * TUTOR/ADMIN cập nhật thông tin bài tập.
     */
    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAssignmentRequest request
    ) {
        AssignmentResponse result = assignmentService.updateAssignment(
                principal.id(),
                principal.role(),
                id,
                request
        );
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/v1/assignments/{id}
     * TUTOR/ADMIN xóa bài tập (chỉ khi chưa có điểm đã chấm).
     */
    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id
    ) {
        assignmentService.deleteAssignment(
                principal.id(),
                principal.role(),
                id
        );
    }
}
