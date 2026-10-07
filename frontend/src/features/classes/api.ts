import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import type {
  Classroom,
  ClassStatus,
  CreateClassRequest,
  CreateInvitationRequest,
  Enrollment,
  EnrollmentStatus,
  EnrollStudentRequest,
  InvitationResponse,
  PageResponse,
  UpdateClassRequest,
} from '../../types';

export interface GetClassesParams {
  studentId?: number;
  status?: ClassStatus;
  q?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ─── API Functions ────────────────────────────────────────────────────────────

export async function fetchClasses(
  params: GetClassesParams = {},
): Promise<PageResponse<Classroom>> {
  const { data } = await apiClient.get<PageResponse<Classroom>>('/classes', {
    params,
  });
  return data;
}

export async function fetchClassById(id: number): Promise<Classroom> {
  const { data } = await apiClient.get<Classroom>(`/classes/${id}`);
  return data;
}

export async function createClass(
  req: CreateClassRequest,
): Promise<Classroom> {
  const { data } = await apiClient.post<Classroom>('/classes', req);
  return data;
}

export async function updateClass(
  id: number,
  req: UpdateClassRequest,
): Promise<Classroom> {
  const { data } = await apiClient.put<Classroom>(`/classes/${id}`, req);
  return data;
}

export async function archiveClass(id: number): Promise<Classroom> {
  const { data } = await apiClient.post<Classroom>(`/classes/${id}/archive`);
  return data;
}

export async function fetchClassStudents(
  classId: number,
  status?: EnrollmentStatus,
): Promise<Enrollment[]> {
  const { data } = await apiClient.get<Enrollment[]>(
    `/classes/${classId}/students`,
    {
      params: status ? { status } : undefined,
    },
  );
  return data;
}

export async function enrollStudent(
  classId: number,
  req: EnrollStudentRequest,
): Promise<Enrollment> {
  const { data } = await apiClient.post<Enrollment>(
    `/classes/${classId}/students`,
    req,
  );
  return data;
}

export async function removeStudent(
  classId: number,
  studentId: number,
): Promise<void> {
  await apiClient.delete(`/classes/${classId}/students/${studentId}`);
}

export async function createInvitation(
  req: CreateInvitationRequest,
): Promise<InvitationResponse> {
  const { data } = await apiClient.post<InvitationResponse>('/invitations', req);
  return data;
}

// ─── TanStack Query Hooks ─────────────────────────────────────────────────────

export function useClassesQuery(
  params: GetClassesParams = {},
  options?: Omit<
    UseQueryOptions<
      PageResponse<Classroom>,
      unknown,
      PageResponse<Classroom>,
      ['classes', GetClassesParams]
    >,
    'queryKey' | 'queryFn'
  >,
) {
  return useQuery({
    queryKey: ['classes', params],
    queryFn: () => fetchClasses(params),
    ...options,
  });
}

export function useClassByIdQuery(
  id: number | undefined,
  options?: Omit<
    UseQueryOptions<Classroom, unknown, Classroom, ['class', number]>,
    'queryKey' | 'queryFn' | 'enabled'
  >,
) {
  return useQuery({
    queryKey: ['class', id ?? 0],
    queryFn: () => fetchClassById(id!),
    enabled: typeof id === 'number' && id > 0,
    ...options,
  });
}

export function useClassStudentsQuery(
  classId: number | undefined,
  status?: EnrollmentStatus,
  options?: Omit<
    UseQueryOptions<
      Enrollment[],
      unknown,
      Enrollment[],
      ['class-students', number, EnrollmentStatus | undefined]
    >,
    'queryKey' | 'queryFn' | 'enabled'
  >,
) {
  return useQuery({
    queryKey: ['class-students', classId ?? 0, status],
    queryFn: () => fetchClassStudents(classId!, status),
    enabled: typeof classId === 'number' && classId > 0,
    ...options,
  });
}

export function useCreateClassMutation(
  options?: UseMutationOptions<Classroom, unknown, CreateClassRequest>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    ...options,
    mutationFn: createClass,
    onSuccess: async (...args) => {
      await queryClient.invalidateQueries({ queryKey: ['classes'] });
      await options?.onSuccess?.(...args);
    },
  });
}

export function useUpdateClassMutation(
  options?: UseMutationOptions<
    Classroom,
    unknown,
    { id: number; data: UpdateClassRequest }
  >,
) {
  const queryClient = useQueryClient();
  return useMutation({
    ...options,
    mutationFn: ({ id, data }) => updateClass(id, data),
    onSuccess: async (...args) => {
      await queryClient.invalidateQueries({ queryKey: ['classes'] });
      await queryClient.invalidateQueries({ queryKey: ['class', args[1].id] });
      await options?.onSuccess?.(...args);
    },
  });
}

export function useArchiveClassMutation(
  options?: UseMutationOptions<Classroom, unknown, number>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    ...options,
    mutationFn: (id: number) => archiveClass(id),
    onSuccess: async (...args) => {
      await queryClient.invalidateQueries({ queryKey: ['classes'] });
      await queryClient.invalidateQueries({ queryKey: ['class', args[1]] });
      await options?.onSuccess?.(...args);
    },
  });
}

export function useEnrollStudentMutation(
  options?: UseMutationOptions<
    Enrollment,
    unknown,
    { classId: number; req: EnrollStudentRequest }
  >,
) {
  const queryClient = useQueryClient();
  return useMutation({
    ...options,
    mutationFn: ({ classId, req }) => enrollStudent(classId, req),
    onSuccess: async (...args) => {
      await queryClient.invalidateQueries({ queryKey: ['classes'] });
      await queryClient.invalidateQueries({ queryKey: ['class', args[1].classId] });
      await queryClient.invalidateQueries({
        queryKey: ['class-students', args[1].classId],
      });
      await options?.onSuccess?.(...args);
    },
  });
}

export function useRemoveStudentMutation(
  options?: UseMutationOptions<
    void,
    unknown,
    { classId: number; studentId: number }
  >,
) {
  const queryClient = useQueryClient();
  return useMutation({
    ...options,
    mutationFn: ({ classId, studentId }) =>
      removeStudent(classId, studentId),
    onSuccess: async (...args) => {
      await queryClient.invalidateQueries({ queryKey: ['classes'] });
      await queryClient.invalidateQueries({ queryKey: ['class', args[1].classId] });
      await queryClient.invalidateQueries({
        queryKey: ['class-students', args[1].classId],
      });
      await options?.onSuccess?.(...args);
    },
  });
}

export function useCreateInvitationMutation(
  options?: UseMutationOptions<
    InvitationResponse,
    unknown,
    CreateInvitationRequest
  >,
) {
  return useMutation({
    mutationFn: createInvitation,
    ...options,
  });
}
