import {
  refreshTokenRequest,
} from './api';
import type { AuthTokens, Role, User } from '../../types';

export const LS_ACCESS = 'tutorhub_access_token';
export const LS_REFRESH = 'tutorhub_refresh_token';
export const LS_USER = 'tutorhub_user';

export function readStorageTokens(): AuthTokens | null {
  try {
    const access = localStorage.getItem(LS_ACCESS);
    const refresh = localStorage.getItem(LS_REFRESH);
    if (!access || !refresh) return null;
    return { accessToken: access, refreshToken: refresh };
  } catch {
    return null;
  }
}

export function readStorageUser(): User | null {
  try {
    const raw = localStorage.getItem(LS_USER);
    if (!raw) return null;
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

export function writeStorageTokens(t: AuthTokens) {
  try {
    localStorage.setItem(LS_ACCESS, t.accessToken);
    localStorage.setItem(LS_REFRESH, t.refreshToken);
  } catch {
    /* ignore */
  }
}

export function writeStorageUser(u: User) {
  try {
    localStorage.setItem(LS_USER, JSON.stringify(u));
  } catch {
    /* ignore */
  }
}

export function clearStorageAuth() {
  try {
    localStorage.removeItem(LS_ACCESS);
    localStorage.removeItem(LS_REFRESH);
    localStorage.removeItem(LS_USER);
  } catch {
    /* ignore */
  }
}

/* -------- Singleton callbacks: pub/sub cho interceptor gọi ngoài React ------- */
export type TokensCb = (t: AuthTokens) => void;
export type VoidCb = () => void;
export const tokenListeners = new Set<TokensCb>();
export const logoutListeners = new Set<VoidCb>();

export function emitTokens(t: AuthTokens) {
  writeStorageTokens(t);
  tokenListeners.forEach((fn) => {
    try {
      fn(t);
    } catch {
      /* ignore */
    }
  });
}

export function emitLogout() {
  clearStorageAuth();
  logoutListeners.forEach((fn) => {
    try {
      fn();
    } catch {
      /* ignore */
    }
  });
}

let refreshPromise: Promise<AuthTokens> | null = null;

/**
 * Hàm standalone dùng bên ngoài React (axios 401 interceptor).
 * Đảm bảo chỉ 1 request refresh chạy đồng thời (dedupe).
 * Nếu refresh lỗi → emitLogout → clear auth toàn cục.
 */
export async function refreshTokensStandalone(): Promise<AuthTokens> {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const current = readStorageTokens();
      if (!current?.refreshToken) {
        throw new Error('NO_REFRESH_TOKEN');
      }
      const fresh = await refreshTokenRequest(current.refreshToken);
      emitTokens(fresh);
      return fresh;
    } catch (err) {
      emitLogout();
      throw err;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

/** Clear auth khi interceptor nhận 401 sau khi đã refresh thất bại hoặc 403 hard. */
export function forceClearAuth() {
  emitLogout();
}

export function getDefaultHomeRoute(role: Role): string {
  switch (role) {
    case 'ADMIN':
    case 'TUTOR':
      return '/';
    case 'STUDENT':
    case 'PARENT':
      return '/portal';
    default:
      return '/login';
  }
}
