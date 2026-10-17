package com.tutorhub.attendance.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tutorhub.attendance.dto.AttendanceHistoryResponse;
import com.tutorhub.attendance.dto.AttendanceRecordResponse;
import com.tutorhub.attendance.dto.BulkAttendanceRequest;
import com.tutorhub.attendance.service.AttendanceService;
import com.tutorhub.auth.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller cho API điểm danh (T4.1).
 *
 * <ul>
 *   <li>GET  /api/v1/sessions/{id}/attendance     → TUTOR</li>
 *   <li>PUT  /api/v1/sessions/{id}/attendance     → TUTOR</li>
 *   <li>GET  /api/v1/students/{studentId}/attendance → TUTOR/STUDENT/PARENT/ADMIN</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * GET /api/v1/sessions/{id}/attendance
     * Lấy danh sách điểm danh của một buổi học.
     * Chỉ TUTOR phụ trách buổi đó hoặc ADMIN.
     */
    @GetMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ResponseEntity<List<AttendanceRecordResponse>> getAttendanceBySession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long sessionId
    ) {
        List<AttendanceRecordResponse> result = attendanceService.getAttendanceBySession(
                principal.id(),
                principal.role(),
                sessionId
        );
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/v1/sessions/{id}/attendance
     * Cập nhật bulk điểm danh trong 1 transaction.
     * Chỉ TUTOR phụ trách buổi đó hoặc ADMIN.
     */
    @PutMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ResponseEntity<List<AttendanceRecordResponse>> bulkUpdateAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long sessionId,
            @Valid @RequestBody BulkAttendanceRequest request
    ) {
        List<AttendanceRecordResponse> result = attendanceService.bulkUpdateAttendance(
                principal.id(),
                principal.role(),
                sessionId,
                request
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/students/{studentId}/attendance?classId=&from=&to=
     * Xem lịch sử điểm danh của học sinh.
     * TUTOR: học sinh trong lớp mình (bắt buộc classId).
     * STUDENT: chỉ xem của chính mình.
     * PARENT: chỉ xem con của mình.
     */
    @GetMapping("/students/{studentId}/attendance")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<List<AttendanceHistoryResponse>> getAttendanceHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        List<AttendanceHistoryResponse> result = attendanceService.getAttendanceHistory(
                principal.id(),
                principal.role(),
                studentId,
                classId,
                from,
                to
        );
        return ResponseEntity.ok(result);
    }
}
