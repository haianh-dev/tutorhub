import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import type {
  AcceptInvitationRequest,
  AuthResponse,
  AuthTokens,
  InvitationInfo,
  LoginRequest,
  RegisterResponse,
  RegisterTutorRequest,
  ResetPasswordRequest,
  User,
} from '../../types';

export async function login(req: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', req);
  return data;
}

export async function registerTutor(
  req: RegisterTutorRequest,
): Promise<RegisterResponse> {
  const { data } = await apiClient.post<RegisterResponse>(
    '/auth/register-tutor',
    req,
  );
  return data;
}

export async function refreshTokenRequest(
  refreshToken: string,
): Promise<AuthTokens> {
  const { data } = await apiClient.post<AuthTokens>('/auth/refresh', {
    refreshToken,
  });
  return data;
}

export async function logoutRequest(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken });
}

export async function getMe(): Promise<User> {
  const { data } = await apiClient.get<User>('/me');
  return data;
}

export async function getInvitation(token: string): Promise<InvitationInfo> {
  const { data } = await apiClient.get<InvitationInfo>(`/auth/invitations/${token}`);
  return data;
}

export async function acceptInvitation(
  req: AcceptInvitationRequest,
): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>(
    '/auth/accept-invitation',
    req,
  );
  return data;
}

export async function resetPassword(
  req: ResetPasswordRequest,
): Promise<void> {
  await apiClient.post('/auth/reset-password', req);
}

/* ============================== HOOKS ============================== */

export function useLoginMutation(
  options?: UseMutationOptions<AuthResponse, unknown, LoginRequest>,
) {
  return useMutation({
    mutationFn: login,
    ...options,
  });
}

export function useRegisterTutorMutation(
  options?: UseMutationOptions<RegisterResponse, unknown, RegisterTutorRequest>,
) {
  return useMutation({
    mutationFn: registerTutor,
    ...options,
  });
}

export function useInvitationQuery(
  token: string | undefined,
  options?: Omit<
    UseQueryOptions<InvitationInfo, unknown, InvitationInfo, [string, string]>,
    'queryKey' | 'queryFn' | 'enabled'
  >,
) {
  return useQuery({
    queryKey: ['invitation', token ?? ''],
    queryFn: () => getInvitation(token!),
    enabled: typeof token === 'string' && token.length > 0,
    retry: false,
    staleTime: 1000 * 60 * 5,
    ...options,
  });
}

export function useAcceptInvitationMutation(
  options?: UseMutationOptions<AuthResponse, unknown, AcceptInvitationRequest>,
) {
  return useMutation({
    mutationFn: acceptInvitation,
    ...options,
  });
}

export function useResetPasswordMutation(
  options?: UseMutationOptions<void, unknown, ResetPasswordRequest>,
) {
  return useMutation({
    mutationFn: resetPassword,
    ...options,
  });
}

export function useLogoutMutation(
  options?: UseMutationOptions<void, unknown, { refreshToken: string }>,
) {
  return useMutation({
    mutationFn: ({ refreshToken }) => logoutRequest(refreshToken),
    ...options,
  });
}

export function useMeQuery(
  options?: Omit<
    UseQueryOptions<User, unknown, User, [string]>,
    'queryKey' | 'queryFn'
  >,
) {
  return useQuery({
    queryKey: ['me'],
    queryFn: getMe,
    retry: false,
    staleTime: 1000 * 60,
    ...options,
  });
}

export function useInvalidateAuthQueries() {
  const queryClient = useQueryClient();
  return () =>
    queryClient.invalidateQueries({
      queryKey: ['me'],
    });
}
