package com.tutorhub.schedule.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorhub.classroom.entity.ClassEntity;
import com.tutorhub.classroom.entity.ClassStatus;
import com.tutorhub.classroom.repository.ClassRepository;
import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;
import com.tutorhub.common.exception.ResourceNotFoundException;
import com.tutorhub.schedule.dto.CreateScheduleRuleRequest;
import com.tutorhub.schedule.dto.GenerateSessionsRequest;
import com.tutorhub.schedule.dto.GenerateSessionsResponse;
import com.tutorhub.schedule.dto.GenerateSessionsResponse.SessionConflictItem;
import com.tutorhub.schedule.dto.ScheduleRuleResponse;
import com.tutorhub.schedule.entity.ScheduleRuleEntity;
import com.tutorhub.schedule.entity.SessionEntity;
import com.tutorhub.schedule.entity.SessionStatus;
import com.tutorhub.schedule.repository.ScheduleRuleRepository;
import com.tutorhub.schedule.repository.SessionRepository;
import com.tutorhub.user.entity.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý quy tắc lịch lặp (Schedule Rules) và sinh buổi học hàng loạt — Phase 3 (T3.4).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleRuleService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private final ScheduleRuleRepository scheduleRuleRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final com.tutorhub.classroom.repository.ClassEnrollmentRepository classEnrollmentRepository;

    /**
     * Tạo quy tắc lịch lặp cho lớp: POST /classes/{classId}/schedule-rules.
     */
    @Transactional
    public ScheduleRuleResponse createRule(
            Long actorId,
            Role actorRole,
            Long classId,
            CreateScheduleRuleRequest request
    ) {
        ClassEntity clazz = findClassOrThrow(classId);
        validateOwnership(clazz, actorId, actorRole);

        if (clazz.getStatus() != ClassStatus.ACTIVE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể tạo lịch cho lớp đã đóng hoặc đã lưu trữ.");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Giờ kết thúc phải sau giờ bắt đầu.");
        }

        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Ngày kết thúc hiệu lực phải sau hoặc bằng ngày bắt đầu hiệu lực.");
        }

        ScheduleRuleEntity rule = ScheduleRuleEntity.builder()
                .clazz(clazz)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .build();

        ScheduleRuleEntity saved = scheduleRuleRepository.save(rule);
        return ScheduleRuleResponse.from(saved);
    }

    /**
     * Lấy danh sách quy tắc lịch của một lớp: GET /classes/{classId}/schedule-rules.
     */
    @Transactional(readOnly = true)
    public List<ScheduleRuleResponse> getRulesByClass(Long actorId, Role actorRole, Long classId) {
        ClassEntity clazz = findClassOrThrow(classId);
        validateOwnership(clazz, actorId, actorRole);

        return scheduleRuleRepository.findByClassId(classId).stream()
                .map(ScheduleRuleResponse::from)
                .toList();
    }

    /**
     * Xóa quy tắc lịch: DELETE /classes/{classId}/schedule-rules/{ruleId}.
     */
    @Transactional
    public void deleteRule(Long actorId, Role actorRole, Long classId, Long ruleId) {
        ClassEntity clazz = findClassOrThrow(classId);
        validateOwnership(clazz, actorId, actorRole);

        ScheduleRuleEntity rule = scheduleRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc lịch."));

        if (!rule.getClazz().getId().equals(classId)) {
            throw new ResourceNotFoundException("Không tìm thấy quy tắc lịch.");
        }

        scheduleRuleRepository.delete(rule);
    }

    /**
     * Sinh buổi học tự động từ quy tắc lịch: POST /classes/{classId}/schedule-rules/{ruleId}/generate.
     * Quy tắc:
     * - Chỉ sinh các ngày trong khoảng [from, to] mà khớp với dayOfWeek của rule.
     * - Ngày đó phải nằm trong khoảng hiệu lực [effectiveFrom, effectiveTo] của rule.
     * - Nếu ngày đó sinh ra khung giờ bị trùng lịch gia sư -> không dừng, thêm vào danh sách conflicts.
     * - Nếu không trùng -> tạo SessionEntity mới với status SCHEDULED và lưu DB.
     * - Trả về: { created: N, conflicts: [...] }.
     */
    @Transactional
    public GenerateSessionsResponse generateSessions(
            Long actorId,
            Role actorRole,
            Long classId,
            Long ruleId,
            GenerateSessionsRequest request
    ) {
        ClassEntity clazz = findClassOrThrow(classId);
        validateOwnership(clazz, actorId, actorRole);

        if (clazz.getStatus() != ClassStatus.ACTIVE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể sinh lịch cho lớp đã đóng hoặc đã lưu trữ.");
        }

        ScheduleRuleEntity rule = scheduleRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc lịch."));

        if (!rule.getClazz().getId().equals(classId)) {
            throw new ResourceNotFoundException("Không tìm thấy quy tắc lịch.");
        }

        if (request.to().isBefore(request.from())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }

        LocalDate currentDate = request.from();
        int createdCount = 0;
        List<SessionConflictItem> conflicts = new ArrayList<>();
        Long tutorId = clazz.getTutor().getId();

        List<String> warnings = new ArrayList<>();
        List<com.tutorhub.classroom.entity.ClassEnrollment> enrollments =
                classEnrollmentRepository.findByClazzIdAndStatusWithStudentOrderByEnrolledAtDesc(
                        classId, com.tutorhub.classroom.entity.EnrollmentStatus.ACTIVE
                );
        List<Long> studentIds = enrollments.stream().map(e -> e.getStudent().getId()).toList();

        while (!currentDate.isAfter(request.to())) {
            // Kiểm tra thứ trong tuần (DayOfWeek: Monday=1, ..., Sunday=7)
            if (currentDate.getDayOfWeek().getValue() == rule.getDayOfWeek()) {
                // Kiểm tra nằm trong phạm vi hiệu lực của rule
                boolean isEffective = !currentDate.isBefore(rule.getEffectiveFrom()) &&
                        (rule.getEffectiveTo() == null || !currentDate.isAfter(rule.getEffectiveTo()));

                if (isEffective) {
                    // Chuyển sang Instant theo múi giờ UTC
                    Instant startAt = currentDate.atTime(rule.getStartTime()).toInstant(ZoneOffset.UTC);
                    Instant endAt = currentDate.atTime(rule.getEndTime()).toInstant(ZoneOffset.UTC);

                    // Kiểm tra xem có buổi nào trùng thời gian không
                    List<SessionEntity> existingConflicts = sessionRepository.findOverlappingSessions(
                            tutorId, startAt, endAt, null
                    );

                    if (!existingConflicts.isEmpty()) {
                        SessionEntity firstConflict = existingConflicts.get(0);
                        String reason = String.format("Trùng với buổi #%d (Lớp %s, %s–%s UTC).",
                                firstConflict.getId(),
                                firstConflict.getClazz().getName(),
                                TIME_FORMATTER.format(firstConflict.getStartAt()),
                                TIME_FORMATTER.format(firstConflict.getEndAt())
                        );
                        conflicts.add(new SessionConflictItem(currentDate, firstConflict.getId(), reason));
                    } else {
                        // Tạo và lưu session mới
                        SessionEntity newSession = SessionEntity.builder()
                                .clazz(clazz)
                                .tutor(clazz.getTutor())
                                .ruleId(rule.getId())
                                .startAt(startAt)
                                .endAt(endAt)
                                .status(SessionStatus.SCHEDULED)
                                .topic(clazz.getName())
                                .build();

                        SessionEntity savedSession = sessionRepository.save(newSession);
                        createdCount++;

                        // Thu thập cảnh báo trùng lịch học sinh (nếu có)
                        if (!studentIds.isEmpty()) {
                            List<Object[]> studentConflicts = sessionRepository.findStudentOverlappingSessions(
                                    studentIds, classId, startAt, endAt, savedSession.getId()
                            );
                            for (Object[] row : studentConflicts) {
                                SessionEntity confSession = (SessionEntity) row[0];
                                String studentName = (String) row[2];
                                warnings.add(String.format(
                                        "Ngày %s: Học sinh %s bị trùng lịch với buổi #%d (Lớp %s, %s–%s UTC).",
                                        currentDate,
                                        studentName,
                                        confSession.getId(),
                                        confSession.getClazz().getName(),
                                        TIME_FORMATTER.format(confSession.getStartAt()),
                                        TIME_FORMATTER.format(confSession.getEndAt())
                                ));
                            }
                        }
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return new GenerateSessionsResponse(createdCount, conflicts, warnings);
    }

    private ClassEntity findClassOrThrow(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài nguyên lớp học."));
    }

    private void validateOwnership(ClassEntity clazz, Long actorId, Role actorRole) {
        if (actorRole == Role.ADMIN) {
            return;
        }
        if (actorRole != Role.TUTOR || !clazz.getTutor().getId().equals(actorId)) {
            throw new ResourceNotFoundException("Không tìm thấy tài nguyên lớp học.");
        }
    }
}
