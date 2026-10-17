import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../features/auth/useAuth';
import { getDefaultHomeRoute } from '../features/auth/authUtils';
import type { Role } from '../types';

/**
 * Chỉ khách (chưa đăng nhập) mới truy cập được (login, register, invitation, reset password).
 * Nếu đã đăng nhập → redirect về trang mặc định theo role.
 */
export const GuestOnly: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, user, isInitialized } = useAuth();
  const location = useLocation();

  if (!isInitialized) {
    return (
      <div className="min-h-screen bg-cream flex items-center justify-center font-body">
        <div className="border-3 border-ink bg-paper brut-box p-6 text-ink font-heading font-bold uppercase text-xs">
          Đang tải…
        </div>
      </div>
    );
  }

  if (isAuthenticated && user) {
    const from = (location.state as { from?: string } | null)?.from;
    const to = from ?? getDefaultHomeRoute(user.role);
    return <Navigate to={to} replace />;
  }

  return <>{children}</>;
};

/**
 * Yêu cầu đăng nhập, và (tùy chọn) phải có role trong danh sách `allowedRoles`.
 * - Chưa đăng nhập → redirect /login?returnUrl=...
 * - Đăng nhập rồi nhưng sai role → redirect /unauthorized
 */
export const RoleGuard: React.FC<{
  allowedRoles: readonly Role[];
  children: React.ReactNode;
}> = ({ allowedRoles, children }) => {
  const { isAuthenticated, user, isInitialized, hasRole } = useAuth();
  const location = useLocation();

  if (!isInitialized) {
    return (
      <div className="min-h-screen bg-cream flex items-center justify-center font-body">
        <div className="border-3 border-ink bg-paper brut-box p-6 text-ink font-heading font-bold uppercase text-xs">
          Đang tải…
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    const returnUrl = encodeURIComponent(
      location.pathname + location.search,
    );
    return (
      <Navigate
        to={`/login?returnUrl=${returnUrl}`}
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  if (!hasRole(allowedRoles)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};
