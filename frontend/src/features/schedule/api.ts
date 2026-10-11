import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import type {
  CreateScheduleRuleRequest,
  CreateSessionRequest,
  GenerateSessionsRequest,
  GenerateSessionsResponse,
  ScheduleRule,
  Session,
  UpdateSessionRequest,
} from '../../types';

export interface GetSessionsParams {
  from?: string; // ISO-8601 UTC
  to?: string;
  classId?: number;
  studentId?: number;
}

// ─── API Functions ────────────────────────────────────────────────────────────

export async function fetchSessions(params: GetSessionsParams = {}): Promise<Session[]> {
  const { data } = await apiClient.get<Session[]>('/sessions', { params });
  return data;
}

export async function fetchSessionById(id: number): Promise<Session> {
  const { data } = await apiClient.get<Session>(`/sessions/${id}`);
  return data;
}

export async function createSession(classId: number, req: CreateSessionRequest): Promise<Session> {
  const { data } = await apiClient.post<Session>(`/classes/${classId}/sessions`, req);
  return data;
}

export async function updateSession(id: number, req: UpdateSessionRequest): Promise<Session> {
  const { data } = await apiClient.put<Session>(`/sessions/${id}`, req);
  return data;
}

export async function completeSession(id: number): Promise<Session> {
  const { data } = await apiClient.post<Session>(`/sessions/${id}/complete`);
  return data;
}

export async function cancelSession(id: number): Promise<Session> {
  const { data } = await apiClient.post<Session>(`/sessions/${id}/cancel`);
  return data;
}

export async function fetchScheduleRules(classId: number): Promise<ScheduleRule[]> {
  const { data } = await apiClient.get<ScheduleRule[]>(`/classes/${classId}/schedule-rules`);
  return data;
}

export async function createScheduleRule(
  classId: number,
  req: CreateScheduleRuleRequest,
): Promise<ScheduleRule> {
  const { data } = await apiClient.post<ScheduleRule>(`/classes/${classId}/schedule-rules`, req);
  return data;
}

export async function deleteScheduleRule(classId: number, ruleId: number): Promise<void> {
  await apiClient.delete(`/classes/${classId}/schedule-rules/${ruleId}`);
}

export async function generateSessionsFromRule(
  classId: number,
  ruleId: number,
  req: GenerateSessionsRequest,
): Promise<GenerateSessionsResponse> {
  const { data } = await apiClient.post<GenerateSessionsResponse>(
    `/classes/${classId}/schedule-rules/${ruleId}/generate`,
    req,
  );
  return data;
}

// ─── TanStack Query Hooks ────────────────────────────────────────────────────

export const scheduleKeys = {
  all: ['schedule'] as const,
  sessions: (params?: GetSessionsParams) => [...scheduleKeys.all, 'sessions', params] as const,
  sessionDetail: (id: number) => [...scheduleKeys.all, 'session', id] as const,
  rules: (classId: number) => [...scheduleKeys.all, 'rules', classId] as const,
};

export function useSessions(
  params: GetSessionsParams = {},
  options?: Omit<UseQueryOptions<Session[], Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: scheduleKeys.sessions(params),
    queryFn: () => fetchSessions(params),
    ...options,
  });
}

export function useSessionDetail(
  id?: number,
  options?: Omit<UseQueryOptions<Session, Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: scheduleKeys.sessionDetail(id ?? 0),
    queryFn: () => fetchSessionById(id!),
    enabled: typeof id === 'number' && id > 0,
    ...options,
  });
}

export function useCreateSession(
  options?: UseMutationOptions<Session, Error, { classId: number; req: CreateSessionRequest }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ classId, req }) => createSession(classId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
    },
    ...options,
  });
}

export function useUpdateSession(
  options?: UseMutationOptions<Session, Error, { id: number; req: UpdateSessionRequest }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }) => updateSession(id, req),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
      queryClient.invalidateQueries({ queryKey: scheduleKeys.sessionDetail(vars.id) });
    },
    ...options,
  });
}

export function useCompleteSession(
  options?: UseMutationOptions<Session, Error, number>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => completeSession(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
      queryClient.invalidateQueries({ queryKey: scheduleKeys.sessionDetail(id) });
    },
    ...options,
  });
}

export function useCancelSession(
  options?: UseMutationOptions<Session, Error, number>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => cancelSession(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
      queryClient.invalidateQueries({ queryKey: scheduleKeys.sessionDetail(id) });
    },
    ...options,
  });
}

export function useScheduleRules(
  classId?: number,
  options?: Omit<UseQueryOptions<ScheduleRule[], Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: scheduleKeys.rules(classId ?? 0),
    queryFn: () => fetchScheduleRules(classId!),
    enabled: typeof classId === 'number' && classId > 0,
    ...options,
  });
}

export function useCreateScheduleRule(
  options?: UseMutationOptions<ScheduleRule, Error, { classId: number; req: CreateScheduleRuleRequest }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ classId, req }) => createScheduleRule(classId, req),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.rules(vars.classId) });
    },
    ...options,
  });
}

export function useDeleteScheduleRule(
  options?: UseMutationOptions<void, Error, { classId: number; ruleId: number }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ classId, ruleId }) => deleteScheduleRule(classId, ruleId),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.rules(vars.classId) });
    },
    ...options,
  });
}

export function useGenerateSessions(
  options?: UseMutationOptions<
    GenerateSessionsResponse,
    Error,
    { classId: number; ruleId: number; req: GenerateSessionsRequest }
  >,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ classId, ruleId, req }) => generateSessionsFromRule(classId, ruleId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
    },
    ...options,
  });
}
