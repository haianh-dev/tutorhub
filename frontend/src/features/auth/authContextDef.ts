import { createContext } from 'react';
import type { AuthResponse, AuthTokens, Role, User } from '../../types';

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

export const AuthContext = createContext<AuthContextValue | null>(null);
