package com.tutorhub.classroom.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import com.tutorhub.classroom.dto.ClassResponse;
import com.tutorhub.classroom.dto.CreateClassRequest;
import com.tutorhub.classroom.dto.UpdateClassRequest;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.service.ClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý lớp học (Phase 2 — T2.1).
 *
 * <p>
 * Tất cả endpoints yêu cầu vai trò TUTOR hoặc ADMIN (@PreAuthorize
 * class-level).
 * STUDENT/PARENT xem lớp là scope của T8 Portal API (dữ liệu mình học / con
 * mình học),
 * không dùng group endpoints này.
 * </p>
 *
 * <p>
 * Controller mỏng theo quy ước AGENTS:
 * </p>
 * <ul>
 * <li>Nhận DTO (UserPrincipal từ JWT, không tin tutorId/studentId từ
 * client).</li>
 * <li>Gọi service — toàn bộ nghiệp vụ + ownership check nằm ở
 * {@link ClassService}.</li>
 * <li>Trả DTO record {@link ClassResponse} (không trả entity).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    /**
     * GET /classes — Danh sách lớp theo quyền.
     *
     * @param status   (Optional) Lọc theo trạng thái ACTIVE / ARCHIVED. Không
     *                 truyền → tất cả.
     * @param q        (Optional) Từ khóa search theo tên lớp (LIKE, không phân biệt
     *                 hoa thường).
     * @param pageable Phân trang (mặc định page=0, size=20, sort=createdAt,DESC).
     * @return Page<ClassResponse> theo phân quyền: TUTOR → lớp của mình; ADMIN →
     *         mọi lớp.
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
     * ADMIN bắt buộc truyền {@code tutorId} trong request body để chỉ định gia sư
     * phụ trách.
     * TUTOR lấy tutorId từ token JWT (bỏ qua giá trị request.tutorId).
     *
     * @return 201 Created với ClassResponse mới.
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
     * TUTOR: phải là gia sư chủ lớp — sai → 404 (không lộ sự tồn tại).
     * ADMIN: OK mọi lớp.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT', 'PARENT', 'ADMIN')")
    public ClassResponse getClassById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return classService.getClassById(principal.id(), principal.role(), id);
    }

    /**
     * PUT /classes/{id} — Cập nhật lớp (partial update: trường null giữ nguyên).
     * Validate FR-2.6 đổi loại lớp GROUP → ONE_ON_ONE khi ≥ 2 HS đang học → 422.
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
     * Đặt status=ARCHIVED (xóa mềm theo D-20, không xóa cứng dữ liệu lịch sử).
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ClassResponse archiveClass(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return classService.archiveClass(principal.id(), principal.role(), id);
    }
}
