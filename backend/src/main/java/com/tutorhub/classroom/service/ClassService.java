package com.tutorhub.classroom.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorhub.classroom.dto.ClassResponse;
import com.tutorhub.classroom.dto.CreateClassRequest;
import com.tutorhub.classroom.dto.EnrollStudentRequest;
import com.tutorhub.classroom.dto.EnrollmentResponse;
import com.tutorhub.classroom.dto.UpdateClassRequest;
import com.tutorhub.classroom.entity.ClassEnrollment;
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
 * Service quản lý nghiệp vụ lớp học & ghi danh (T2.1: CRUD lớp + archive; T2.2: Ghi danh/bỏ học sinh).
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
     */
    @Transactional
    public ClassResponse createClass(Long actorId, Role actorRole, CreateClassRequest request) {
        Long resolvedTutorId = switch (actorRole) {
            case TUTOR -> actorId;
            case ADMIN -> {
                if (request.tutorId() == null) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR,
                            "ADMIN bắt buộc chỉ định tutorId khi tạo lớp học");
                }
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

        return ClassResponse.from(entity, 0);
    }

    // ─── 3. Chi tiết một lớp ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long actorId, Role actorRole, Long classId) {
        ClassEntity entity = loadAndCheckOwnership(actorId, actorRole, classId);
        int studentCount = classRepository.countActiveEnrollmentsByClassId(classId);
        return ClassResponse.from(entity, studentCount);
    }

    // ─── 4. Cập nhật lớp (partial update) + validate đổi classType FR-2.6 ──

    @Transactional
    public ClassResponse updateClass(Long actorId, Role actorRole, Long classId, UpdateClassRequest request) {
        ClassEntity entity = loadAndCheckOwnership(actorId, actorRole, classId);

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

    @Transactional
    public ClassResponse archiveClass(Long actorId, Role actorRole, Long classId) {
        ClassEntity entity = loadAndCheckOwnership(actorId, actorRole, classId);
        entity.setStatus(ClassStatus.ARCHIVED);
        entity = classRepository.save(entity);
        int studentCount = classRepository.countActiveEnrollmentsByClassId(classId);
        return ClassResponse.from(entity, studentCount);
    }

    // ─── 6. Ghi danh học sinh vào lớp (T2.2) ────────────────────────────────

    /**
     * Ghi danh học sinh vào lớp học.
     * Sử dụng Pessimistic Lock trên dòng lớp (findByIdForUpdate) để chống race condition
     * khi có 2 request đồng thời thêm học sinh vào lớp 1:1.
     */
    @Transactional
    public EnrollmentResponse enrollStudent(Long actorId, Role actorRole, Long classId, EnrollStudentRequest request) {
        // Khóa bi quan dòng lớp học
        ClassEntity classEntity = loadAndCheckOwnershipForUpdate(actorId, actorRole, classId);

        if (classEntity.getStatus() == ClassStatus.ARCHIVED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể ghi danh vào lớp học đã lưu trữ (ARCHIVED)");
        }

        // Validate học sinh tồn tại và có role STUDENT
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Học sinh với id=" + request.studentId() + " không tồn tại"));
        if (student.getRole() != Role.STUDENT) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Tài khoản ghi danh phải có vai trò STUDENT (nhận được: " + student.getRole() + ")");
        }

        // Kiểm tra xem học sinh đã có enrollment trong lớp chưa
        Optional<ClassEnrollment> existingOpt = classEnrollmentRepository.findByClazzIdAndStudentId(classId, request.studentId());
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == EnrollmentStatus.ACTIVE) {
            throw new AppException(ErrorCode.ENROLLMENT_DUPLICATE, "Học sinh đã được ghi danh vào lớp học này");
        }

        // Validate quy tắc lớp 1:1 (ONE_ON_ONE)
        if (classEntity.getClassType() == ClassType.ONE_ON_ONE) {
            int activeCount = classEnrollmentRepository.countByClazzIdAndStatus(classId, EnrollmentStatus.ACTIVE);
            if (activeCount >= 1) {
                throw new AppException(ErrorCode.ONE_ON_ONE_FULL,
                        "Lớp 1:1 chỉ có tối đa 1 học sinh đang học. Không thể thêm học sinh thứ 2.");
            }
        }

        Instant now = Instant.now();
        ClassEnrollment enrollment;
        if (existingOpt.isPresent()) {
            // Tái ghi danh học sinh đã LEFT trước đó
            enrollment = existingOpt.get();
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setEnrolledAt(now);
            enrollment.setLeftAt(null);
        } else {
            // Ghi danh mới
            enrollment = ClassEnrollment.builder()
                    .clazz(classEntity)
                    .student(student)
                    .status(EnrollmentStatus.ACTIVE)
                    .enrolledAt(now)
                    .build();
        }

        enrollment = classEnrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(enrollment);
    }

    // ─── 7. Danh sách học sinh của lớp (T2.2) ──────────────────────────────

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getStudents(Long actorId, Role actorRole, Long classId, EnrollmentStatus status) {
        loadAndCheckOwnership(actorId, actorRole, classId);

        List<ClassEnrollment> enrollments = (status != null)
                ? classEnrollmentRepository.findByClazzIdAndStatusWithStudentOrderByEnrolledAtDesc(classId, status)
                : classEnrollmentRepository.findByClazzIdWithStudentOrderByEnrolledAtDesc(classId);

        return enrollments.stream().map(EnrollmentResponse::from).toList();
    }

    // ─── 8. Cho học sinh rời lớp (T2.2) ────────────────────────────────────

    @Transactional
    public void removeStudent(Long actorId, Role actorRole, Long classId, Long studentId) {
        loadAndCheckOwnership(actorId, actorRole, classId);

        ClassEnrollment enrollment = classEnrollmentRepository.findByClazzIdAndStudentId(classId, studentId)
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Học sinh không có ghi danh hoạt động trong lớp học này"));

        enrollment.setStatus(EnrollmentStatus.LEFT);
        enrollment.setLeftAt(Instant.now());
        classEnrollmentRepository.save(enrollment);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private ClassEntity loadAndCheckOwnership(Long actorId, Role actorRole, Long classId) {
        ClassEntity entity = classRepository.findByIdWithTutor(classId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Lớp học với id=" + classId + " không tồn tại"));

        if (actorRole == Role.TUTOR) {
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

        return entity;
    }

    private ClassEntity loadAndCheckOwnershipForUpdate(Long actorId, Role actorRole, Long classId) {
        ClassEntity entity = classRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Lớp học với id=" + classId + " không tồn tại"));

        if (actorRole == Role.TUTOR) {
            if (!entity.getTutor().getId().equals(actorId)) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Lớp học với id=" + classId + " không tồn tại");
            }
        }

        return entity;
    }
}
