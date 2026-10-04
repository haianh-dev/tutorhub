import type { ApiProblemDetail } from '../../types';

export const AUTH_ERROR_MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: 'Email hoặc mật khẩu không đúng.',
  AUTH_TOKEN_INVALID: 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.',
  AUTH_TOKEN_EXPIRED: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  AUTH_ACCESS_DENIED: 'Bạn không có quyền thực hiện thao tác này.',
  DUPLICATE_RESOURCE:
    'Email này đã được đăng ký tài khoản. Hãy dùng email khác hoặc đăng nhập.',
  INVITATION_EXPIRED:
    'Lời mời đã hết hạn. Vui lòng yêu cầu gia sư gửi lại lời mời mới.',
  INVITATION_USED:
    'Lời mời này đã được sử dụng. Vui lòng yêu cầu gia sư gửi lại lời mời mới.',
  PASSWORD_RESET_INVALID:
    'Liên kết đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu liên kết mới.',
  VALIDATION_ERROR:
    'Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường dưới đây.',
  INTERNAL_ERROR: 'Có lỗi xảy ra, vui lòng thử lại sau.',
};

export const FALLBACK_ERROR_MESSAGE = 'Có lỗi xảy ra, vui lòng thử lại sau.';

/**
 * Trích xuất message tổng quát (cho alert phía trên form) từ axios ApiProblemDetail.
 * Ưu tiên: code → AUTH_ERROR_MESSAGES → detail → FALLBACK.
 */
export function extractAlertMessage(err: unknown): string {
  const problem = extractProblemDetail(err);
  if (!problem) return FALLBACK_ERROR_MESSAGE;
  if (problem.code && AUTH_ERROR_MESSAGES[problem.code]) {
    return AUTH_ERROR_MESSAGES[problem.code];
  }
  return problem.detail ?? problem.title ?? FALLBACK_ERROR_MESSAGE;
}

/**
 * Map field errors từ ProblemDetail sang object { [field]: message }
 * dùng với RHF setError.
 */
export function extractFieldErrors(
  err: unknown,
): Record<string, string> {
  const problem = extractProblemDetail(err);
  const out: Record<string, string> = {};
  if (!problem?.errors) return out;
  for (const e of problem.errors) {
    if (e.field && e.message && !out[e.field]) {
      out[e.field] = e.message;
    }
  }
  return out;
}

export function extractProblemDetail(err: unknown): ApiProblemDetail | null {
  if (!err) return null;
  const any = err as { response?: { data?: unknown }; isAxiosError?: boolean };
  const data =
    any?.response?.data ?? ('data' in any ? (any as { data?: unknown }).data : null);
  if (
    data &&
    typeof data === 'object' &&
    'status' in data &&
    typeof (data as { status: unknown }).status === 'number'
  ) {
    return data as ApiProblemDetail;
  }
  return null;
}
