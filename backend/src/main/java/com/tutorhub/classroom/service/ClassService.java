package com.tutorhub.classroom.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorhub.classroom.dto.ClassResponse;
import com.tutorhub.classroom.dto.CreateClassRequest;
import com.tutorhub.classroom.dto.UpdateClassRequest;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.entity.ClassType;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ActiveEnrollmentCount;
import com.tutorhub.classroom.repository.ClassEnrollmentRepository;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.repository.ParentStudentRepository;
import com.tutorhub.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service quản lý nghiệp vụ lớp học (T2.1: CRUD lớp + archive).
 *
 * <p>
 * Quy tắc cốt lõi:
 * </p>
 * <ul>
 * <li><b>Quyền sở hữu (BR-1):</b> TUTOR chỉ truy cập lớp có
 * {@code tutor_id = id của mình}.
 * Truy cập sai (lớp của gia sư khác) → trả <b>404 RESOURCE_NOT_FOUND</b> (không
 * lộ sự tồn tại của lớp).
 * ADMIN bỏ qua ownership filter — truy cập mọi lớp (D-27 CONFIRMED).</li>
 * <li><b>Loại lớp bắt buộc (AC T2.1):</b> classType không được null (Bean
 * Validation ở request DTO).</li>
 * <li><b>FR-2.6 Đổi loại lớp:</b>
 * GROUP → ONE_ON_ONE chỉ khi lớp có ≤ 1 học sinh ACTIVE (≥2 → 422
 * CLASS_TYPE_CHANGE_INVALID).
 * ONE_ON_ONE → GROUP luôn OK.</li>
 * <li><b>ADMIN chỉ định tutorId (D-31 CONFIRMED):</b> ADMIN tạo lớp có quyền
 * truyền tutorId → gia sư phụ trách khác.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;

    // ─── 1. Danh sách lớp (phân trang, filter status, search theo tên) ──────

    /**
     * Trả về trang {@link ClassResponse} dựa trên vai trò người gọi.
     *
     * @param actorId   ID người dùng (từ JWT)
     * @param actorRole Vai trò người dùng (từ JWT)
     * @param status    Lọc theo trạng thái (null = tất cả)
     * @param q         Từ khóa search theo tên lớp (LIKE %q%, không phân biệt hoa
     *                  thường)
     * @param pageable  Phân trang + sắp xếp
     */
    @Transactional(readOnly = true)
    public Page<ClassResponse> getClasses(
            Long actorId,
            Role actorRole,
            Long studentId,
            ClassStatus status,
            String q,
            Pageable pageable) {
        String search = q == null ? "" : q.trim();
        Page<ClassEntity> entities = switch (actorRole) {
            case ADMIN -> classRepository.findAllWithFilters(status, search, pageable);
            case TUTOR -> classRepository.findByTutorIdAndFilters(actorId, status, search, pageable);
            case STUDENT -> classRepository.findByStudentIdAndFilters(
                    actorId, EnrollmentStatus.ACTIVE, status, search, pageable);
            case PARENT -> classRepository.findByParentIdAndFilters(
                    actorId, studentId, status == null ? null : status.name(), search,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted()));
        };

        if (entities.isEmpty()) {
            return entities.map(e -> ClassResponse.from(e, 0));
        }
        Map<Long, Integer> activeCounts = classEnrollmentRepository.countActiveByClassIds(
                entities.getContent().stream().map(ClassEntity::getId).toList(), EnrollmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(ActiveEnrollmentCount::getClassId,
                        count -> Math.toIntExact(count.getStudentCount())));
        return entities.map(entity -> ClassResponse.from(entity, activeCounts.getOrDefault(entity.getId(), 0)));
    }

    // ─── 2. Tạo lớp mới ─────────────────────────────────────────────────────

    /**
     * Tạo lớp mới.
     *
     * @param actorId   Người tạo (từ JWT — không từ client).
     * @param actorRole Vai trò người tạo.
     * @param request   Thông tin lớp (name, subject, classType, description,
     *                  tutorId — chỉ ADMIN dùng).
     */
    @Transactional
    public ClassResponse createClass(Long actorId, Role actorRole, CreateClassRequest request) {
        Long resolvedTutorId = switch (actorRole) {
            case TUTOR -> actorId; // TUTOR: luôn lấy mình làm gia sư, bỏ qua request.tutorId()
            case ADMIN -> {
                // ADMIN bắt buộc phải truyền tutorId (quyết định D-31: ADMIN chỉ định gia sư
                // phụ trách)
                if (request.tutorId() == null) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR,
                            "ADMIN bắt buộc chỉ định tutorId khi tạo lớp học");
                }
                // Validate tutorId tồn tại và đúng vai trò TUTOR
                User tutor = userRepository.findById(request.tutorId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                                "Gia sư với id=" + request.tutorId() + " không tồn tại"));
                if (tutor.getRole() != Role.TUTOR) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR,
                            "tutorId phải là tài khoản có vai trò TUTOR (nhận được: " + tutor.getRole() + ")");
                }
                yield tutor.getId();
            }
            default -> throw new AppException(ErrorCode.AUTH_ACCESS_DENIED,
                    "Vai trò này không có quyền tạo lớp học");
        };

        // Lấy tutor entity cho mối quan hệ Many-to-One
        User tutor = userRepository.getReferenceById(resolvedTutorId);

        ClassEntity entity = ClassEntity.builder()
                .tutor(tutor)
                .name(request.name().trim())
                .subject(request.subject().trim())
                .description(request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null)
                .classType(request.classType())
                .status(ClassStatus.ACTIVE)
                .build();
        entity = classRepository.save(entity);

        // Lớp mới tạo → 0 học sinh
        return ClassResponse.from(entity, 0);
    }

    // ─── 3. Chi tiết một lớp ────────────────────────────────────────────────

    /**
     * Lấy chi tiết lớp theo ID.
     * TUTOR: phải là chủ sở hữu (tutor_id == actorId) — sai → 404.
     * ADMIN: OK mọi lớp.
     */
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long actorId, Role actorRole, Long classId) {
        ClassEntity entity = loadAndCheckOwnership(actorId, actorRole, classId);
        int studentCount = classRepository.countActiveEnrollmentsByClassId(classId);
        return ClassResponse.from(entity, studentCount);
    }

    // ─── 4. Cập nhật lớp (partial update) + validate đổi classType FR-2.6 ──

    /**
     * Cập nhật thông tin lớp.
     * Các trường null trong {@link UpdateClassRequest} giữ nguyên giá trị cũ.
     *
     * <p>
     * <b>FR-2.6 đổi classType:</b>
     * </p>
     * <ul>
     * <li>ONE_ON_ONE → GROUP: luôn OK (cho phép thêm học sinh).</li>
     * <li>GROUP → ONE_ON_ONE: chỉ khi lớp có ≤ 1 học sinh ACTIVE (≥2 → 422
     * CLASS_TYPE_CHANGE_INVALID).</li>
     * </ul>
     */
    @Transactional
    public ClassResponse updateClass(Long actorId, Role actorRole, Long classId, UpdateClassRequest request) {
        ClassEntity entity = loadAndCheckOwnership(actorId, actorRole, classId);

        // ── 4a. Validate đổi classType FR-2.6 ──────────────────────────────
        ClassType newType = request.classType();
        if (newType != null && !newType.equals(entity.getClassType())) {
            if (newType == ClassType.ONE_ON_ONE && entity.getClassType() == ClassType.GROUP) {
                int activeEnrollmentCount = classRepository.countActiveEnrollmentsByClassId(classId);
                if (activeEnrollmentCount >= 2) {
                    throw new AppException(ErrorCode.CLASS_TYPE_CHANGE_INVALID,
                            "Không thể đổi sang lớp 1:1 do đang có " + activeEnrollmentCount
                                    + " học sinh đang học (lớp 1:1 chỉ được tối đa 1 học sinh đang học). "
                                    + "Vui lòng cho học sinh nghỉ lớp trước khi đổi loại.");
                }
            }
            entity.setClassType(newType);
        }

        // ── 4b. Partial update các trường khác (null → giữ nguyên) ─────────
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Tên lớp không được để trống");
            }
            entity.setName(request.name().trim());
        }
        if (request.subject() != null) {
            if (request.subject().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Môn học không được để trống");
            }
            entity.setSubject(request.subject().trim());
        }
        if (request.description() != null) {
            entity.setDescription(request.description().isBlank() ? null : request.description().trim());
        }

        entity = classRepository.save(entity);
        int studentCount = classRepository.countActiveEnrollmentsByClassId(classId);
        return ClassResponse.from(entity, studentCount);
    }

    // ─── 5. Archive (lưu trữ) lớp ──────────────────────────────────────────

    /**
     * Đánh dấu lớp là ARCHIVED (xóa mềm theo D-20: không xóa cứng khi đã có
     * buổi/điểm).
     * Không hỗ trợ unarchive cho đơn giản MVP — nếu cần có thể thêm sau.
     */
    @Transactional
    public ClassResponse archiveClass(Long actorId, Role actorRole, Long classId) {
        ClassEntity entity = loadAndCheckOwnership(actorId, actorRole, classId);
        entity.setStatus(ClassStatus.ARCHIVED);
        entity = classRepository.save(entity);
        int studentCount = classRepository.countActiveEnrollmentsByClassId(classId);
        return ClassResponse.from(entity, studentCount);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Load ClassEntity theo id và kiểm tra quyền sở hữu (one-liner dùng chung mọi
     * method).
     *
     * <p>
     * Quy tắc:
     * </p>
     * <ul>
     * <li>Không tồn tại lớp theo id → 404.</li>
     * <li>TUTOR gọi: tutor_id khác actorId → 404 (không lộ sự tồn tại).</li>
     * <li>ADMIN gọi: mọi lớp đều OK.</li>
     * </ul>
     *
     * @return ClassEntity nếu vượt qua mọi check.
     */
    private ClassEntity loadAndCheckOwnership(Long actorId, Role actorRole, Long classId) {
        ClassEntity entity = classRepository.findByIdWithTutor(classId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Lớp học với id=" + classId + " không tồn tại"));

        if (actorRole == Role.TUTOR) {
            // TUTOR: kiểm tra tutor_id — sai → 404 (không nói rằng lớp của người khác)
            if (!entity.getTutor().getId().equals(actorId)) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Lớp học với id=" + classId + " không tồn tại");
            }
        } else if (actorRole == Role.STUDENT
                && !classEnrollmentRepository.existsByClazzIdAndStudentIdAndStatus(
                        classId, actorId, EnrollmentStatus.ACTIVE)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Lớp học với id=" + classId + " không tồn tại");
        } else if (actorRole == Role.PARENT
                && !parentStudentRepository.hasActiveEnrollmentInClass(actorId, classId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Lớp học với id=" + classId + " không tồn tại");
        }
        // ADMIN: không cần check, pass qua. STUDENT/PARENT đã được @PreAuthorize chặn ở
        // controller.

        return entity;
    }
}
