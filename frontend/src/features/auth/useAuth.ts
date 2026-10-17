import { useContext } from 'react';
import { AuthContext, type AuthContextValue } from './authContextDef';

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth phải được sử dụng trong <AuthProvider>');
  }
  return ctx;
}

export type { AuthContextValue };
