import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import {
  forceClearAuth,
  refreshTokensStandalone,
} from '../features/auth/AuthContext';
import type { ApiProblemDetail } from '../types';

export interface RetryableConfig extends InternalAxiosRequestConfig {
  __isRetry?: boolean;
}

export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

const AUTH_PUBLIC_URLS = new Set([
  '/auth/login',
  '/auth/refresh',
  '/auth/register-tutor',
  '/auth/accept-invitation',
  '/auth/reset-password',
]);

function isPublicAuthUrl(url: string | undefined): boolean {
  if (!url) return false;
  const withoutBase = url.startsWith('/api/v1') ? url.slice('/api/v1'.length) : url;
  for (const p of AUTH_PUBLIC_URLS) {
    if (withoutBase.startsWith(p)) return true;
  }
  return false;
}

// Attach latest access token từ localStorage trên mỗi request.
// Luôn đọc lại storage để nhận token mới sau khi refresh thành công.
apiClient.interceptors.request.use((config) => {
  try {
    const token = localStorage.getItem('tutorhub_access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  } catch {
    /* ignore localStorage lỗi */
  }
  return config;
});

// Xử lý lỗi: 401 tự động refresh access token + retry request gốc (một lần duy nhất).
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiProblemDetail>) => {
    const status = error.response?.status;
    const config = error.config as RetryableConfig | undefined;

    // Trường hợp 401 → refresh token + retry 1 lần
    if (status === 401 && config && !config.__isRetry) {
      const url = config.url;

      // Endpoint public auth hoặc refresh endpoint bị 401 là lỗi hard, không retry.
      if (isPublicAuthUrl(url)) {
        return Promise.reject(error);
      }

      try {
        const fresh = await refreshTokensStandalone();
        config.__isRetry = true;
        if (config.headers) {
          config.headers.Authorization = `Bearer ${fresh.accessToken}`;
        }
        return apiClient(config as AxiosRequestConfig);
      } catch {
        // Refresh thất bại → xóa auth, UI sẽ redirect qua route guard hoặc check me.
        forceClearAuth();
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  },
);

export default apiClient;
