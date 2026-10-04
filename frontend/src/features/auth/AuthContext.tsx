import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useNavigate } from 'react-router-dom';
import {
  logoutRequest,
  refreshTokenRequest,
} from './api';
import type { AuthResponse, AuthTokens, Role, User } from '../../types';

const LS_ACCESS = 'tutorhub_access_token';
const LS_REFRESH = 'tutorhub_refresh_token';
const LS_USER = 'tutorhub_user';

function readStorageTokens(): AuthTokens | null {
  try {
    const access = localStorage.getItem(LS_ACCESS);
    const refresh = localStorage.getItem(LS_REFRESH);
    if (!access || !refresh) return null;
    return { accessToken: access, refreshToken: refresh };
  } catch {
    return null;
  }
}

function readStorageUser(): User | null {
  try {
    const raw = localStorage.getItem(LS_USER);
    if (!raw) return null;
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

function writeStorageTokens(t: AuthTokens) {
  try {
    localStorage.setItem(LS_ACCESS, t.accessToken);
    localStorage.setItem(LS_REFRESH, t.refreshToken);
  } catch {
    /* ignore */
  }
}

function writeStorageUser(u: User) {
  try {
    localStorage.setItem(LS_USER, JSON.stringify(u));
  } catch {
    /* ignore */
  }
}

function clearStorageAuth() {
  try {
    localStorage.removeItem(LS_ACCESS);
    localStorage.removeItem(LS_REFRESH);
    localStorage.removeItem(LS_USER);
  } catch {
    /* ignore */
  }
}

/* -------- Singleton callbacks: pub/sub cho interceptor gọi ngoài React ------- */
type TokensCb = (t: AuthTokens) => void;
type VoidCb = () => void;
const tokenListeners = new Set<TokensCb>();
const logoutListeners = new Set<VoidCb>();

function emitTokens(t: AuthTokens) {
  writeStorageTokens(t);
  tokenListeners.forEach((fn) => {
    try {
      fn(t);
    } catch {
      /* ignore */
    }
  });
}

function emitLogout() {
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

/* ---------------- Context shape ---------------- */

export interface AuthContextValue {
  user: User | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  isInitialized: boolean;
  hasRole: (allowedRoles: readonly Role[]) => boolean;
  setAuth: (auth: AuthResponse) => void;
  updateUser: (user: User) => void;
  logout: () => Promise<void>;
  getHomeRoute: () => string;
}

const AuthContext = createContext<AuthContextValue | null>(null);

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

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [tokens, setTokensState] = useState<AuthTokens | null>(() =>
    readStorageTokens(),
  );
  const [user, setUserState] = useState<User | null>(() => readStorageUser());
  const isInitialized = true;
  const navigate = useNavigate();

  // Register singleton listeners
  useEffect(() => {
    const onTokens: TokensCb = (fresh) => setTokensState(fresh);
    const onLogout: VoidCb = () => {
      setTokensState(null);
      setUserState(null);
    };
    tokenListeners.add(onTokens);
    logoutListeners.add(onLogout);
    return () => {
      tokenListeners.delete(onTokens);
      logoutListeners.delete(onLogout);
    };
  }, []);

  const setAuth = useCallback((auth: AuthResponse) => {
    const nextTokens: AuthTokens = {
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
    };
    writeStorageTokens(nextTokens);
    writeStorageUser(auth.user);
    setTokensState(nextTokens);
    setUserState(auth.user);
  }, []);

  const updateUser = useCallback((next: User) => {
    writeStorageUser(next);
    setUserState(next);
  }, []);

  const logout = useCallback(async () => {
    const current = readStorageTokens();
    try {
      if (current?.refreshToken) {
        await logoutRequest(current.refreshToken);
      }
    } catch {
      // Logout API lỗi → vẫn clear auth client-side
    }
    emitLogout();
    navigate('/login', { replace: true });
  }, [navigate]);

  const hasRole = useCallback(
    (allowedRoles: readonly Role[]) => {
      if (!user) return false;
      return allowedRoles.includes(user.role);
    },
    [user],
  );

  const getHomeRoute = useCallback(() => {
    if (!user) return '/login';
    return getDefaultHomeRoute(user.role);
  }, [user]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      tokens,
      isAuthenticated: Boolean(user && tokens?.accessToken),
      isInitialized,
      hasRole,
      setAuth,
      updateUser,
      logout,
      getHomeRoute,
    }),
    [user, tokens, isInitialized, hasRole, setAuth, updateUser, logout, getHomeRoute],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth phải được sử dụng trong <AuthProvider>');
  }
  return ctx;
}
