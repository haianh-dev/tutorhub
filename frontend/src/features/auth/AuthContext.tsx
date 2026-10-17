import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { logoutRequest } from './api';
import {
  emitLogout,
  getDefaultHomeRoute,
  logoutListeners,
  readStorageTokens,
  readStorageUser,
  tokenListeners,
  writeStorageTokens,
  writeStorageUser,
  type TokensCb,
  type VoidCb,
} from './authUtils';
import { AuthContext, type AuthContextValue } from './authContextDef';
import type { AuthResponse, AuthTokens, Role, User } from '../../types';

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
