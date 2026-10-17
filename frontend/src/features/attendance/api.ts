import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import type {
  AttendanceRecord,
  BulkAttendanceRequest,
  StudentAttendanceSummary,
} from './types';

export async function fetchSessionAttendance(sessionId: number): Promise<AttendanceRecord[]> {
  const { data } = await apiClient.get<AttendanceRecord[]>(`/sessions/${sessionId}/attendance`);
  return data;
}

export async function bulkUpdateAttendance(
  sessionId: number,
  req: BulkAttendanceRequest,
): Promise<AttendanceRecord[]> {
  const { data } = await apiClient.put<AttendanceRecord[]>(`/sessions/${sessionId}/attendance`, req);
  return data;
}

export async function fetchStudentAttendanceHistory(
  studentId: number,
  params?: { classId?: number; from?: string; to?: string },
): Promise<StudentAttendanceSummary> {
  const { data } = await apiClient.get<StudentAttendanceSummary>(
    `/students/${studentId}/attendance`,
    { params },
  );
  return data;
}

export function useSessionAttendance(sessionId: number | undefined) {
  return useQuery({
    queryKey: ['session-attendance', sessionId],
    queryFn: () => fetchSessionAttendance(sessionId!),
    enabled: typeof sessionId === 'number' && sessionId > 0,
  });
}

export function useBulkUpdateAttendance(
  sessionId: number,
  options?: {
    onSuccess?: (data: AttendanceRecord[]) => void;
    onError?: (err: unknown) => void;
  },
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: BulkAttendanceRequest) => bulkUpdateAttendance(sessionId, req),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['session-attendance', sessionId] });
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
      options?.onSuccess?.(data);
    },
    onError: options?.onError,
  });
}

export function useStudentAttendanceHistory(
  studentId: number | undefined,
  params?: { classId?: number; from?: string; to?: string },
) {
  return useQuery({
    queryKey: ['student-attendance', studentId, params],
    queryFn: () => fetchStudentAttendanceHistory(studentId!, params),
    enabled: typeof studentId === 'number' && studentId > 0,
  });
}
