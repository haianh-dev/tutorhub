import type { ApiProblemDetail } from '../../types';

export const SCHEDULE_ERROR_MESSAGES: Record<string, string> = {
  SESSION_CONFLICT: 'Khung giờ bị trùng với buổi dạy khác của gia sư.',
  RESOURCE_NOT_FOUND: 'Không tìm thấy thông tin buổi học hoặc lớp học.',
  VALIDATION_ERROR: 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thời gian.',
  AUTH_ACCESS_DENIED: 'Bạn không có quyền quản lý lịch của lớp này.',
  INTERNAL_ERROR: 'Lỗi hệ thống, vui lòng thử lại sau.',
};

export const FALLBACK_SCHEDULE_ERROR = 'Có lỗi xảy ra khi xử lý lịch học, vui lòng thử lại.';

export function extractScheduleError(err: unknown): { message: string; conflictingSessionId?: number } {
  if (!err) return { message: FALLBACK_SCHEDULE_ERROR };
  const anyErr = err as { response?: { data?: unknown } };
  const data = anyErr?.response?.data;

  if (
    data &&
    typeof data === 'object' &&
    'status' in data &&
    typeof (data as { status: unknown }).status === 'number'
  ) {
    const problem = data as ApiProblemDetail;
    const conflictingId = problem.conflictingSessionId;

    if (problem.code === 'SESSION_CONFLICT') {
      return {
        message: problem.detail || SCHEDULE_ERROR_MESSAGES.SESSION_CONFLICT,
        conflictingSessionId: conflictingId,
      };
    }

    if (problem.code && SCHEDULE_ERROR_MESSAGES[problem.code]) {
      return {
        message: SCHEDULE_ERROR_MESSAGES[problem.code],
        conflictingSessionId: conflictingId,
      };
    }

    if (problem.detail) return { message: problem.detail, conflictingSessionId: conflictingId };
    if (problem.title) return { message: problem.title, conflictingSessionId: conflictingId };
  }

  return { message: FALLBACK_SCHEDULE_ERROR };
}
