package com.tutorhub.assignment.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorhub.assignment.dto.AssignmentResponse;
import com.tutorhub.assignment.dto.CreateAssignmentRequest;
import com.tutorhub.assignment.dto.UpdateAssignmentRequest;
import com.tutorhub.assignment.entity.AssignmentEntity;
import com.tutorhub.assignment.entity.AssignmentScoreEntity;
import com.tutorhub.assignment.entity.SubmissionStatus;
import com.tutorhub.assignment.repository.AssignmentRepository;
import com.tutorhub.assignment.repository.AssignmentScoreRepository;
import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassEnrollment;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ClassEnrollmentRepository;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.common.exception.ResourceNotFoundException;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.repository.ParentStudentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý bài tập và tự tạo bản ghi điểm (T5.1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentScoreRepository assignmentScoreRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ParentStudentRepository parentStudentRepository;

    /**
     * POST /classes/{id}/assignments — TUTOR/ADMIN tạo bài tập mới cho lớp.
     * Tự động tạo score rows (thang điểm 10 cố định) cho mọi học sinh đang học (ACTIVE).
     */
    @Transactional
    public AssignmentResponse createAssignment(
            Long actorId,
            Role actorRole,
            Long classId,
            CreateAssignmentRequest request
    ) {
        ClassEntity clazz = findClassOrThrow(classId);
        validateTutorOwnership(clazz, actorId, actorRole);

        AssignmentEntity assignment = AssignmentEntity.builder()
                .classroom(clazz)
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .assignmentType(request.type())
                .dueAt(request.dueAt())
                .build();

        AssignmentEntity savedAssignment = assignmentRepository.save(assignment);

        // Lấy danh sách học sinh đang học (ACTIVE) trong lớp
        List<ClassEnrollment> activeEnrollments =
                classEnrollmentRepository.findByClazzIdAndStatusWithStudentOrderByEnrolledAtDesc(classId, EnrollmentStatus.ACTIVE);

        List<AssignmentScoreEntity> scores = new ArrayList<>();
        for (ClassEnrollment enrollment : activeEnrollments) {
            AssignmentScoreEntity score = AssignmentScoreEntity.builder()
                    .assignment(savedAssignment)
                    .student(enrollment.getStudent())
                    .score(null)
                    .status(SubmissionStatus.ASSIGNED)
                    .feedback(null)
                    .build();
            scores.add(score);
        }

        if (!scores.isEmpty()) {
            assignmentScoreRepository.saveAll(scores);
        }

        log.info("Assignment created: id={}, classId={}, studentScoresCount={}",
                savedAssignment.getId(), classId, scores.size());

        return AssignmentResponse.from(savedAssignment);
    }

    /**
     * GET /classes/{id}/assignments — Lấy danh sách bài tập của lớp theo quyền.
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClass(
            Long actorId,
            Role actorRole,
            Long classId
    ) {
        ClassEntity clazz = findClassOrThrow(classId);
        validateClassReadAccess(clazz, actorId, actorRole);

        return assignmentRepository.findByClassroomIdOrderByCreatedAtDesc(classId)
                .stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    /**
     * GET /assignments/{id} — Xem chi tiết bài tập.
     */
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentDetail(
            Long actorId,
            Role actorRole,
            Long assignmentId
    ) {
        AssignmentEntity assignment = findAssignmentOrThrow(assignmentId);
        validateClassReadAccess(assignment.getClassroom(), actorId, actorRole);
        return AssignmentResponse.from(assignment);
    }

    /**
     * PUT /assignments/{id} — TUTOR/ADMIN cập nhật thông tin bài tập.
     */
    @Transactional
    public AssignmentResponse updateAssignment(
            Long actorId,
            Role actorRole,
            Long assignmentId,
            UpdateAssignmentRequest request
    ) {
        AssignmentEntity assignment = findAssignmentOrThrow(assignmentId);
        validateTutorOwnership(assignment.getClassroom(), actorId, actorRole);

        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description() != null ? request.description().trim() : null);
        assignment.setAssignmentType(request.type());
        assignment.setDueAt(request.dueAt());

        AssignmentEntity updated = assignmentRepository.save(assignment);
        log.info("Assignment updated: id={}", assignmentId);
        return AssignmentResponse.from(updated);
    }

    /**
     * DELETE /assignments/{id} — TUTOR/ADMIN xóa bài tập.
     * Chỉ cho phép xóa khi chưa có điểm đã chấm, ngược lại trả về 422 ASSIGNMENT_HAS_GRADES.
     */
    @Transactional
    public void deleteAssignment(
            Long actorId,
            Role actorRole,
            Long assignmentId
    ) {
        AssignmentEntity assignment = findAssignmentOrThrow(assignmentId);
        validateTutorOwnership(assignment.getClassroom(), actorId, actorRole);

        boolean hasGradedScores = assignmentScoreRepository.existsByAssignmentIdAndScoreIsNotNull(assignmentId)
                || assignmentScoreRepository.existsByAssignmentIdAndStatus(assignmentId, SubmissionStatus.GRADED);

        if (hasGradedScores) {
            throw new AppException(ErrorCode.ASSIGNMENT_HAS_GRADES,
                    "Không thể xóa bài tập đã có điểm được chấm.");
        }

        assignmentRepository.delete(assignment);
        log.info("Assignment deleted: id={}", assignmentId);
    }

    /**
     * Lắng nghe sự kiện học sinh được ghi danh vào lớp để tự động bổ sung score rows (FR-5.2).
     */
    @org.springframework.context.event.EventListener
    public void handleStudentEnrolled(com.tutorhub.classroom.event.StudentEnrolledEvent event) {
        syncScoresForNewEnrolledStudent(event.classId(), event.student());
    }

    /**
     * Bổ sung score rows cho học sinh mới ghi danh vào các bài tập hiện có của lớp (FR-5.2).
     */
    @Transactional
    public void syncScoresForNewEnrolledStudent(Long classId, User student) {
        List<AssignmentEntity> assignments = assignmentRepository.findByClassroomIdOrderByCreatedAtDesc(classId);
        List<AssignmentScoreEntity> newScores = new ArrayList<>();

        for (AssignmentEntity assignment : assignments) {
            boolean exists = assignmentScoreRepository.existsByAssignmentIdAndStudentId(assignment.getId(), student.getId());
            if (!exists) {
                newScores.add(AssignmentScoreEntity.builder()
                        .assignment(assignment)
                        .student(student)
                        .score(null)
                        .status(SubmissionStatus.ASSIGNED)
                        .feedback(null)
                        .build());
            }
        }

        if (!newScores.isEmpty()) {
            assignmentScoreRepository.saveAll(newScores);
            log.info("Synced {} assignment score rows for studentId={} in classId={}",
                    newScores.size(), student.getId(), classId);
        }
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────────

    private ClassEntity findClassOrThrow(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));
    }

    private AssignmentEntity findAssignmentOrThrow(Long assignmentId) {
        return assignmentRepository.findByIdWithClassroomAndTutor(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));
    }

    private void validateTutorOwnership(ClassEntity clazz, Long actorId, Role actorRole) {
        if (actorRole == Role.ADMIN) return;
        if (actorRole != Role.TUTOR || !clazz.getTutor().getId().equals(actorId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học.");
        }
    }

    private void validateClassReadAccess(ClassEntity clazz, Long actorId, Role actorRole) {
        switch (actorRole) {
            case ADMIN -> { /* toàn quyền */ }
            case TUTOR -> {
                if (!clazz.getTutor().getId().equals(actorId)) {
                    throw new ResourceNotFoundException("Không tìm thấy lớp học.");
                }
            }
            case STUDENT -> {
                boolean active = classEnrollmentRepository.existsByClazzIdAndStudentIdAndStatus(
                        clazz.getId(), actorId, EnrollmentStatus.ACTIVE);
                if (!active) {
                    throw new ResourceNotFoundException("Không tìm thấy lớp học.");
                }
            }
            case PARENT -> {
                boolean hasActiveChild = parentStudentRepository.hasActiveEnrollmentInClass(actorId, clazz.getId());
                if (!hasActiveChild) {
                    throw new ResourceNotFoundException("Không tìm thấy lớp học.");
                }
            }
            default -> throw new ResourceNotFoundException("Không tìm thấy lớp học.");
        }
    }
}
