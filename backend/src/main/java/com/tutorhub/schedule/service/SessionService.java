package com.tutorhub.schedule.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.repository.ClassEnrollmentRepository;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.common.exception.ResourceNotFoundException;
import com.tutorhub.schedule.dto.CreateSessionRequest;
import com.tutorhub.schedule.dto.SessionResponse;
import com.tutorhub.schedule.dto.UpdateSessionRequest;
import com.tutorhub.schedule.entity.SessionEntity;
import com.tutorhub.schedule.entity.SessionStatus;
import com.tutorhub.schedule.exception.SessionConflictException;
import com.tutorhub.schedule.repository.SessionRepository;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.repository.ParentStudentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý buổi học (Session) — Phase 3 (T3.2).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("UTC"));

    private final SessionRepository sessionRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ParentStudentRepository parentStudentRepository;

    /**
     * Tạo buổi học đơn lẻ cho một lớp: POST /classes/{classId}/sessions.
     * Quy tắc:
     * - Chỉ TUTOR phụ trách lớp hoặc ADMIN mới có quyền tạo buổi.
     * - Lớp phải ở trạng thái ACTIVE (không tạo buổi cho lớp ARCHIVED).
     * - endAt phải lớn hơn startAt (ngược lại -> 400 VALIDATION_ERROR).
     * - Nếu trùng giờ với buổi khác của cùng gia sư -> 409 SESSION_CONFLICT kèm conflictingSessionId.
     */
    @Transactional
    public SessionResponse createSession(
            Long actorId,
            Role actorRole,
            Long classId,
            CreateSessionRequest request
    ) {
        validateTimeRange(request.startAt(), request.endAt());

        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        validateTutorClassOwnership(clazz, actorId, actorRole);

        if (clazz.getStatus() == ClassStatus.ARCHIVED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể tạo buổi học cho lớp đã lưu trữ.");
        }

        Long tutorId = clazz.getTutor().getId();

        // 1. Kiểm tra chủ động xem có buổi nào đang trùng không để trích xuất conflictingSessionId
        List<SessionEntity> conflicts = sessionRepository.findOverlappingSessions(
                tutorId, request.startAt(), request.endAt(), null
        );
        if (!conflicts.isEmpty()) {
            throw buildConflictException(conflicts.get(0));
        }

        // 2. Tạo entity và lưu vào DB
        SessionEntity entity = SessionEntity.builder()
                .clazz(clazz)
                .tutor(clazz.getTutor())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .status(SessionStatus.SCHEDULED)
                .topic(request.topic())
                .note(request.note())
                .build();

        try {
            SessionEntity saved = sessionRepository.saveAndFlush(entity);
            return SessionResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Đề phòng trường hợp race condition lọt qua bước 1 và bị DB exclusion constraint chặn
            log.warn("Exclusion constraint caught during session creation: {}", ex.getMessage());
            List<SessionEntity> lateConflicts = sessionRepository.findOverlappingSessions(
                    tutorId, request.startAt(), request.endAt(), null
            );
            if (!lateConflicts.isEmpty()) {
                throw buildConflictException(lateConflicts.get(0));
            }
            throw new SessionConflictException(
                    "Buổi học trùng thời gian với buổi khác của gia sư.", null
            );
        }
    }

    /**
     * Cập nhật buổi học: PUT /sessions/{id}.
     * Quy tắc:
     * - Chỉ TUTOR phụ trách buổi đó hoặc ADMIN mới được sửa.
     * - Nếu buổi đã bị CANCELLED -> không được chỉnh sửa.
     * - endAt phải lớn hơn startAt.
     * - Re-check trùng giờ; nếu trùng -> 409 SESSION_CONFLICT.
     */
    @Transactional
    public SessionResponse updateSession(
            Long actorId,
            Role actorRole,
            Long sessionId,
            UpdateSessionRequest request
    ) {
        SessionEntity session = findSessionOrThrow(sessionId);
        validateTutorClassOwnership(session.getClazz(), actorId, actorRole);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể cập nhật buổi học đã bị hủy.");
        }

        Instant newStartAt = request.startAt() != null ? request.startAt() : session.getStartAt();
        Instant newEndAt = request.endAt() != null ? request.endAt() : session.getEndAt();
        validateTimeRange(newStartAt, newEndAt);

        // Kiểm tra xung đột với các buổi khác của cùng gia sư (loại trừ chính session này)
        List<SessionEntity> conflicts = sessionRepository.findOverlappingSessions(
                session.getTutor().getId(), newStartAt, newEndAt, session.getId()
        );
        if (!conflicts.isEmpty()) {
            throw buildConflictException(conflicts.get(0));
        }

        session.setStartAt(newStartAt);
        session.setEndAt(newEndAt);
        if (request.topic() != null) {
            session.setTopic(request.topic());
        }
        if (request.note() != null) {
            session.setNote(request.note());
        }

        try {
            SessionEntity updated = sessionRepository.saveAndFlush(session);
            return SessionResponse.from(updated);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Exclusion constraint caught during session update: {}", ex.getMessage());
            List<SessionEntity> lateConflicts = sessionRepository.findOverlappingSessions(
                    session.getTutor().getId(), newStartAt, newEndAt, session.getId()
            );
            if (!lateConflicts.isEmpty()) {
                throw buildConflictException(lateConflicts.get(0));
            }
            throw new SessionConflictException(
                    "Buổi học trùng thời gian với buổi khác của gia sư.", null
            );
        }
    }

    /**
     * Đánh dấu hoàn thành buổi học: POST /sessions/{id}/complete.
     */
    @Transactional
    public SessionResponse completeSession(Long actorId, Role actorRole, Long sessionId) {
        SessionEntity session = findSessionOrThrow(sessionId);
        validateTutorClassOwnership(session.getClazz(), actorId, actorRole);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể hoàn thành buổi học đã bị hủy.");
        }

        session.setStatus(SessionStatus.COMPLETED);
        return SessionResponse.from(sessionRepository.save(session));
    }

    /**
     * Hủy buổi học: POST /sessions/{id}/cancel.
     * Khi chuyển status sang CANCELLED, ràng buộc exclude_tutor_overlapping_sessions
     * sẽ tự động bỏ qua buổi này, giải phóng khung giờ.
     */
    @Transactional
    public SessionResponse cancelSession(Long actorId, Role actorRole, Long sessionId) {
        SessionEntity session = findSessionOrThrow(sessionId);
        validateTutorClassOwnership(session.getClazz(), actorId, actorRole);

        session.setStatus(SessionStatus.CANCELLED);
        return SessionResponse.from(sessionRepository.save(session));
    }

    /**
     * Xem chi tiết một buổi học: GET /sessions/{id}.
     */
    @Transactional(readOnly = true)
    public SessionResponse getSessionById(Long actorId, Role actorRole, Long sessionId) {
        SessionEntity session = findSessionOrThrow(sessionId);
        validateReadAccess(session.getClazz(), actorId, actorRole);
        return SessionResponse.from(session);
    }

    /**
     * Lấy danh sách buổi học theo khoảng ngày và lớp học: GET /sessions?from=&to=&classId=&studentId=.
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(
            Long actorId,
            Role actorRole,
            Instant from,
            Instant to,
            Long classId,
            Long targetStudentId
    ) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        org.springframework.data.jpa.domain.Specification<SessionEntity> spec = (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("clazz", jakarta.persistence.criteria.JoinType.INNER);
            }
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (actorRole == Role.TUTOR) {
                predicates.add(cb.equal(root.get("tutor").get("id"), actorId));
            } else if (actorRole == Role.STUDENT) {
                jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<com.tutorhub.classroom.entity.ClassEnrollment> enrollRoot =
                        subquery.from(com.tutorhub.classroom.entity.ClassEnrollment.class);
                subquery.select(enrollRoot.get("clazz").get("id"))
                        .where(
                                cb.equal(enrollRoot.get("student").get("id"), actorId),
                                cb.equal(enrollRoot.get("status"), com.tutorhub.classroom.entity.EnrollmentStatus.ACTIVE)
                        );
                predicates.add(root.get("clazz").get("id").in(subquery));
            } else if (actorRole == Role.PARENT) {
                Long studentId = targetStudentId;
                if (studentId == null) {
                    List<Long> childIds = parentStudentRepository.findStudentIdsByParentId(actorId);
                    if (childIds.isEmpty()) {
                        return cb.disjunction();
                    }
                    studentId = childIds.get(0);
                } else {
                    if (!parentStudentRepository.existsByParentIdAndStudentId(actorId, studentId)) {
                        throw new ResourceNotFoundException("Học sinh không thuộc quyền giám hộ của bạn.");
                    }
                }
                jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<com.tutorhub.classroom.entity.ClassEnrollment> enrollRoot =
                        subquery.from(com.tutorhub.classroom.entity.ClassEnrollment.class);
                subquery.select(enrollRoot.get("clazz").get("id"))
                        .where(
                                cb.equal(enrollRoot.get("student").get("id"), studentId),
                                cb.equal(enrollRoot.get("status"), com.tutorhub.classroom.entity.EnrollmentStatus.ACTIVE)
                        );
                predicates.add(root.get("clazz").get("id").in(subquery));
            }

            if (classId != null) {
                predicates.add(cb.equal(root.get("clazz").get("id"), classId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), to));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "startAt"
        );

        return sessionRepository.findAll(spec, sort)
                .stream()
                .map(SessionResponse::from)
                .toList();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────────

    private SessionEntity findSessionOrThrow(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + sessionId));
    }

    private void validateTimeRange(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Thời gian bắt đầu và kết thúc không được để trống.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
    }

    private void validateTutorClassOwnership(ClassEntity clazz, Long actorId, Role actorRole) {
        if (actorRole == Role.ADMIN) {
            return;
        }
        if (actorRole != Role.TUTOR || !clazz.getTutor().getId().equals(actorId)) {
            // Trả về 404 để không để lộ sự tồn tại của lớp học/buổi học khác gia sư
            throw new ResourceNotFoundException("Không tìm thấy tài nguyên lớp học.");
        }
    }

    private void validateReadAccess(ClassEntity clazz, Long actorId, Role actorRole) {
        if (actorRole == Role.ADMIN) {
            return;
        }
        if (actorRole == Role.TUTOR) {
            if (!clazz.getTutor().getId().equals(actorId)) {
                throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
            }
            return;
        }
        if (actorRole == Role.STUDENT) {
            boolean enrolled = classEnrollmentRepository.existsByClazzIdAndStudentIdAndStatus(
                    clazz.getId(), actorId, com.tutorhub.classroom.entity.EnrollmentStatus.ACTIVE
            );
            if (!enrolled) {
                throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
            }
            return;
        }
        if (actorRole == Role.PARENT) {
            boolean hasChildInClass = parentStudentRepository.hasActiveEnrollmentInClass(actorId, clazz.getId());
            if (!hasChildInClass) {
                throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
            }
            return;
        }
        throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
    }

    private SessionConflictException buildConflictException(SessionEntity conflict) {
        String startTimeStr = TIME_FORMATTER.format(conflict.getStartAt());
        String endTimeStr = TIME_FORMATTER.format(conflict.getEndAt());
        String detail = String.format("Trùng với buổi #%d (Lớp %s, %s–%s UTC).",
                conflict.getId(),
                conflict.getClazz().getName(),
                startTimeStr,
                endTimeStr
        );
        return new SessionConflictException(detail, conflict.getId());
    }
}
