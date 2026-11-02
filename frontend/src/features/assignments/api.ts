import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import type {
  Assignment,
  AssignmentScore,
  BulkUpdateScoresRequest,
  CreateAssignmentRequest,
  UpdateAssignmentRequest,
} from './types';

// ─── Query Keys ───────────────────────────────────────────────────────────────

export const assignmentKeys = {
  all: ['assignments'] as const,
  byClass: (classId: number) => ['assignments', 'class', classId] as const,
  detail: (assignmentId: number) => ['assignments', 'detail', assignmentId] as const,
  scores: (assignmentId: number) => ['assignments', 'scores', assignmentId] as const,
  studentScores: (studentId: number, classId?: number) =>
    ['assignments', 'studentScores', studentId, classId] as const,
};

// ─── API Functions ────────────────────────────────────────────────────────────

export async function fetchClassAssignments(classId: number): Promise<Assignment[]> {
  const { data } = await apiClient.get<Assignment[]>(`/classes/${classId}/assignments`);
  return data;
}

export async function fetchAssignmentDetail(assignmentId: number): Promise<Assignment> {
  const { data } = await apiClient.get<Assignment>(`/assignments/${assignmentId}`);
  return data;
}

export async function fetchAssignmentScores(assignmentId: number): Promise<AssignmentScore[]> {
  const { data } = await apiClient.get<AssignmentScore[]>(`/assignments/${assignmentId}/scores`);
  return data;
}

export async function createAssignment(
  classId: number,
  req: CreateAssignmentRequest,
): Promise<Assignment> {
  const { data } = await apiClient.post<Assignment>(`/classes/${classId}/assignments`, req);
  return data;
}

export async function updateAssignment(
  assignmentId: number,
  req: UpdateAssignmentRequest,
): Promise<Assignment> {
  const { data } = await apiClient.put<Assignment>(`/assignments/${assignmentId}`, req);
  return data;
}

export async function deleteAssignment(assignmentId: number): Promise<void> {
  await apiClient.delete(`/assignments/${assignmentId}`);
}

export async function bulkUpdateScores(
  assignmentId: number,
  req: BulkUpdateScoresRequest,
): Promise<AssignmentScore[]> {
  const { data } = await apiClient.put<AssignmentScore[]>(`/assignments/${assignmentId}/scores`, req);
  return data;
}

export async function fetchStudentScores(
  studentId: number,
  classId?: number,
): Promise<AssignmentScore[]> {
  const { data } = await apiClient.get<AssignmentScore[]>(`/students/${studentId}/scores`, {
    params: classId ? { classId } : undefined,
  });
  return data;
}

// ─── Hooks ────────────────────────────────────────────────────────────────────

export function useClassAssignments(
  classId?: number,
  options?: Omit<UseQueryOptions<Assignment[], Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: classId ? assignmentKeys.byClass(classId) : ['assignments', 'disabled'],
    queryFn: () => fetchClassAssignments(classId!),
    enabled: typeof classId === 'number' && classId > 0,
    ...options,
  });
}

export function useAssignmentDetail(
  assignmentId?: number,
  options?: Omit<UseQueryOptions<Assignment, Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: assignmentId ? assignmentKeys.detail(assignmentId) : ['assignments', 'disabled'],
    queryFn: () => fetchAssignmentDetail(assignmentId!),
    enabled: typeof assignmentId === 'number' && assignmentId > 0,
    ...options,
  });
}

export function useAssignmentScores(
  assignmentId?: number,
  options?: Omit<UseQueryOptions<AssignmentScore[], Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: assignmentId ? assignmentKeys.scores(assignmentId) : ['assignments', 'disabled'],
    queryFn: () => fetchAssignmentScores(assignmentId!),
    enabled: typeof assignmentId === 'number' && assignmentId > 0,
    ...options,
  });
}

export function useCreateAssignment(
  classId: number,
  options?: UseMutationOptions<Assignment, Error, CreateAssignmentRequest>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: CreateAssignmentRequest) => createAssignment(classId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byClass(classId) });
    },
    ...options,
  });
}

export function useUpdateAssignment(
  classId: number,
  options?: UseMutationOptions<Assignment, Error, { id: number; data: UpdateAssignmentRequest }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateAssignmentRequest }) =>
      updateAssignment(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byClass(classId) });
      queryClient.invalidateQueries({ queryKey: assignmentKeys.detail(id) });
    },
    ...options,
  });
}

export function useDeleteAssignment(
  classId: number,
  options?: UseMutationOptions<void, Error, number>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteAssignment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assignmentKeys.byClass(classId) });
    },
    ...options,
  });
}

export function useBulkUpdateScores(
  assignmentId: number,
  options?: UseMutationOptions<AssignmentScore[], Error, BulkUpdateScoresRequest>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: BulkUpdateScoresRequest) => bulkUpdateScores(assignmentId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assignmentKeys.scores(assignmentId) });
    },
    ...options,
  });
}

export function useStudentScores(
  studentId: number,
  classId?: number,
  options?: Omit<UseQueryOptions<AssignmentScore[], Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: assignmentKeys.studentScores(studentId, classId),
    queryFn: () => fetchStudentScores(studentId, classId),
    enabled: studentId > 0,
    ...options,
  });
}
