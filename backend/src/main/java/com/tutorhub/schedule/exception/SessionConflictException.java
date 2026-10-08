package com.tutorhub.schedule.exception;

import com.tutorhub.common.exception.AppException;
import com.tutorhub.common.exception.ErrorCode;

import lombok.Getter;

/**
 * Ngoại lệ ném ra khi tạo hoặc sửa buổi học bị trùng với một buổi khác của gia sư.
 * Chứa {@code conflictingSessionId} để controller/handler trả về chi tiết theo API_SPEC §10.
 */
@Getter
public class SessionConflictException extends AppException {

    private final Long conflictingSessionId;

    public SessionConflictException(String detail, Long conflictingSessionId) {
        super(ErrorCode.SESSION_CONFLICT, detail);
        this.conflictingSessionId = conflictingSessionId;
    }
}
