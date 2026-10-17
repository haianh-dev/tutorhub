package com.tutorhub.attendance.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorhub.attendance.dto.AttendanceHistoryResponse;
import com.tutorhub.attendance.dto.AttendanceItemRequest;
import com.tutorhub.attendance.dto.AttendanceRecordResponse;
import com.tutorhub.attendance.dto.BulkAttendanceRequest;
import com.tutorhub.attendance.entity.AttendanceEntity;
import com.tutorhub.attendance.repository.AttendanceRepository;
import com.tutorhub.classroom.entity.EnrollmentStatus;
import com.tutorhub.classroom.repository.ClassEnrollmentRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.common.exception.ResourceNotFoundException;
import com.tutorhub.schedule.entity.SessionEntity;
import com.tutorhub.schedule.repository.SessionRepository;
import com.tutorhub.user.entity.Role;
import com.tutorhub.user.entity.User;
import com.tutorhub.user.repository.ParentStudentRepository;
import com.tutorhub.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service điểm danh (T4.1).
 *
 * <p>Quy tắc nghiệp vụ quan trọng:</p>
 * <ul>
 *   <li>Chỉ TUTOR phụ trách lớp (hoặc ADMIN) mới được ghi điểm danh.</li>
 *   <li>Mỗi studentId trong bulk request phải có enrollment ACTIVE trong lớp của session đó;
 *       nếu không → 422 STUDENT_NOT_ENROLLED. Cả batch rollback.</li>
 *   <li>Upsert theo (session_id, student_id): update nếu tồn tại, insert nếu chưa có.</li>
 *   <li>Toàn bộ bulk update trong 1 @Transactional → rollback nếu có bất kỳ lỗi.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;

    /**
     * GET /sessions/{sessionId}/attendance — TUTOR xem danh sách điểm danh của buổi học.
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getAttendanceBySession(Long actorId, Role actorRole, Long sessionId) {
        SessionEntity session = findSessionOrThrow(sessionId);
        validateTutorOwnership(session, actorId, actorRole);
        return attendanceRepository.findBySessionIdWithStudent(sessionId)
                .stream()
                .map(AttendanceRecordResponse::from)
                .toList();
    }

    /**
     * PUT /sessions/{sessionId}/attendance — TUTOR cập nhật bulk điểm danh.
     *
     * <p>Luồng:</p>
     * <ol>
     *   <li>Xác thực session tồn tại và thuộc về TUTOR.</li>
     *   <li>Với mỗi record: kiểm tra studentId có enrollment ACTIVE trong lớp của session không.</li>
     *   <li>Upsert AttendanceEntity.</li>
     *   <li>@Transactional đảm bảo rollback toàn bộ nếu có lỗi.</li>
     * </ol>
     */
    @Transactional
    public List<AttendanceRecordResponse> bulkUpdateAttendance(
            Long actorId,
            Role actorRole,
            Long sessionId,
            BulkAttendanceRequest request
    ) {
        SessionEntity session = findSessionOrThrow(sessionId);
        validateTutorOwnership(session, actorId, actorRole);

        Long classId = session.getClazz().getId();

        // Lấy tập studentId ACTIVE để validate
        List<Long> activeStudentIds = classEnrollmentRepository
                .findByClazzIdAndStatusWithStudentOrderByEnrolledAtDesc(classId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(e -> e.getStudent().getId())
                .toList();

        // Tải trước các AttendanceEntity đã có để upsert
        List<AttendanceEntity> existing = attendanceRepository.findBySessionIdWithStudent(sessionId);
        Map<Long, AttendanceEntity> existingByStudentId = existing.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        List<AttendanceEntity> toSave = new java.util.ArrayList<>();

        for (AttendanceItemRequest item : request.records()) {
            // Validate: học sinh phải có enrollment ACTIVE trong lớp này
            if (!activeStudentIds.contains(item.studentId())) {
                throw new AppException(
                        ErrorCode.STUDENT_NOT_ENROLLED,
                        "Học sinh ID " + item.studentId() + " không có ghi danh ACTIVE trong lớp này."
                );
            }

            Optional<AttendanceEntity> maybeExisting = Optional.ofNullable(existingByStudentId.get(item.studentId()));

            if (maybeExisting.isPresent()) {
                // Update
                AttendanceEntity att = maybeExisting.get();
                att.setStatus(item.status());
                att.setNote(item.note());
                toSave.add(att);
            } else {
                // Insert — cần load User reference
                User student = userRepository.findById(item.studentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh ID: " + item.studentId()));
                AttendanceEntity att = AttendanceEntity.builder()
                        .session(session)
                        .student(student)
                        .status(item.status())
                        .note(item.note())
                        .build();
                toSave.add(att);
            }
        }

        List<AttendanceEntity> saved = attendanceRepository.saveAll(toSave);
        attendanceRepository.flush();

        log.info("Bulk attendance updated: sessionId={}, count={}", sessionId, saved.size());

        return saved.stream()
                .map(AttendanceRecordResponse::from)
                .toList();
    }

    /**
     * GET /students/{studentId}/attendance?classId=&from=&to= — lịch sử điểm danh.
     * Quyền:
     * - TUTOR: chỉ xem học sinh trong lớp mình dạy.
     * - STUDENT: chỉ xem của chính mình.
     * - PARENT: chỉ xem con của mình.
     */
    @Transactional(readOnly = true)
    public List<AttendanceHistoryResponse> getAttendanceHistory(
            Long actorId,
            Role actorRole,
            Long targetStudentId,
            Long classId,
            Instant from,
            Instant to
    ) {
        validateHistoryAccess(actorId, actorRole, targetStudentId, classId);

        return attendanceRepository.findAttendanceHistory(targetStudentId, classId, from, to)
                .stream()
                .map(AttendanceHistoryResponse::from)
                .toList();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────────

    private SessionEntity findSessionOrThrow(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + sessionId));
    }

    private void validateTutorOwnership(SessionEntity session, Long actorId, Role actorRole) {
        if (actorRole == Role.ADMIN) return;
        if (actorRole != Role.TUTOR || !session.getTutor().getId().equals(actorId)) {
            throw new ResourceNotFoundException("Không tìm thấy buổi học.");
        }
    }

    private void validateHistoryAccess(Long actorId, Role actorRole, Long targetStudentId, Long classId) {
        switch (actorRole) {
            case ADMIN -> { /* toàn quyền */ }
            case STUDENT -> {
                if (!actorId.equals(targetStudentId)) {
                    throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
                }
            }
            case PARENT -> {
                if (!parentStudentRepository.existsByParentIdAndStudentId(actorId, targetStudentId)) {
                    throw new ResourceNotFoundException("Học sinh không thuộc quyền giám hộ của bạn.");
                }
            }
            case TUTOR -> {
                // TUTOR chỉ xem được học sinh trong lớp mình (nếu classId được chỉ định)
                if (classId == null) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR,
                            "TUTOR phải truyền classId khi xem lịch sử điểm danh của học sinh.");
                }
                boolean enrolled = classEnrollmentRepository
                        .existsByClazzIdAndStudentIdAndStatus(classId, targetStudentId, EnrollmentStatus.ACTIVE);
                if (!enrolled) {
                    throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
                }
                // Kiểm tra lớp thuộc về tutor này
                com.tutorhub.classroom.entity.ClassEntity clazz =
                        classEnrollmentRepository.findByClazzIdAndStudentId(classId, targetStudentId)
                                .map(e -> e.getClazz())
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài nguyên."));
                if (!clazz.getTutor().getId().equals(actorId)) {
                    throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
                }
            }
            default -> throw new ResourceNotFoundException("Không tìm thấy tài nguyên.");
        }
    }
}
