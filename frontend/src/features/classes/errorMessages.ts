import type { ApiProblemDetail } from '../../types';

export const CLASS_ERROR_MESSAGES: Record<string, string> = {
  // Class CRUD & Rules
  CLASS_NOT_FOUND: 'Không tìm thấy thông tin lớp học.',
  CLASS_FORBIDDEN: 'Bạn không có quyền quản lý lớp học này.',
  CLASS_ARCHIVED: 'Lớp học đã được lưu trữ, không thể chỉnh sửa.',
  CLASS_TYPE_CHANGE_INVALID:
    'Không thể chuyển sang lớp 1:1 khi lớp đang có từ 2 học sinh đang học trở lên.',

  // Enrollments
  ENROLLMENT_DUPLICATE: 'Học sinh này đã được ghi danh vào lớp học.',
  ONE_ON_ONE_FULL:
    'Lớp 1:1 chỉ có tối đa 1 học sinh đang học. Vui lòng chuyển lớp sang dạng nhóm nếu muốn thêm học sinh.',
  STUDENT_NOT_FOUND: 'Không tìm thấy thông tin học sinh với mã này.',
  ENROLLMENT_NOT_FOUND: 'Học sinh chưa từng tham gia lớp học này.',

  // Invitations
  INVITATION_PARENT_STUDENT_REQUIRED:
    'Lời mời cho phụ huynh bắt buộc phải chọn học sinh liên kết.',
  INVITATION_CLASS_INVALID:
    'Lớp học không tồn tại hoặc đã được lưu trữ.',
  INVITATION_FORBIDDEN:
    'Bạn không có quyền tạo lời mời cho lớp học này.',

  // Validation & General
  VALIDATION_ERROR: 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.',
  AUTH_ACCESS_DENIED: 'Bạn không có quyền thực hiện thao tác này.',
  INTERNAL_ERROR: 'Có lỗi xảy ra, vui lòng thử lại sau.',
};

export const FALLBACK_CLASS_ERROR = 'Có lỗi xảy ra, vui lòng thử lại sau.';

export function extractClassErrorMessage(err: unknown): string {
  if (!err) return FALLBACK_CLASS_ERROR;
  const anyErr = err as { response?: { data?: unknown } };
  const data = anyErr?.response?.data;

  if (
    data &&
    typeof data === 'object' &&
    'status' in data &&
    typeof (data as { status: unknown }).status === 'number'
  ) {
    const problem = data as ApiProblemDetail;
    if (problem.code && CLASS_ERROR_MESSAGES[problem.code]) {
      return CLASS_ERROR_MESSAGES[problem.code];
    }
    if (problem.detail) return problem.detail;
    if (problem.title) return problem.title;
  }

  return FALLBACK_CLASS_ERROR;
}
