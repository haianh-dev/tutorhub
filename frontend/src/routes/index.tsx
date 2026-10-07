import React from 'react';
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  useLocation,
} from 'react-router-dom';
import {
  AuthProvider,
  getDefaultHomeRoute,
  useAuth,
} from '../features/auth/AuthContext';
import type { Role } from '../types';
import AppLayout from '../components/layout/AppLayout';
import DashboardPage from '../features/dashboard/DashboardPage';
import ClassesListPage from '../features/classes/ClassesListPage';
import ClassDetailPage from '../features/classes/ClassDetailPage';
import PlaceholderPage from '../components/PlaceholderPage';
import DevUiPage from '../features/dev/DevUiPage';
import LoginPage from '../features/auth/pages/LoginPage';
import RegisterTutorPage from '../features/auth/pages/RegisterTutorPage';
import AcceptInvitationPage from '../features/auth/pages/AcceptInvitationPage';
import ResetPasswordPage from '../features/auth/pages/ResetPasswordPage';
import { Alert } from '../components/ui/Alert';
import { Button } from '../components/ui/Button';
import { Link } from 'react-router-dom';
import { Shield, ArrowLeft, Home } from 'lucide-react';

/* ========================= ROUTE GUARDS ========================= */

/**
 * Chỉ khách (chưa đăng nhập) mới truy cập được (login, register, invitation, reset password).
 * Nếu đã đăng nhập → redirect về trang mặc định theo role.
 */
const GuestOnly: React.FC<{ children: React.ReactNode }> = ({ children }) => {
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
const RoleGuard: React.FC<{
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

const UnauthorizedPage: React.FC = () => {
  const { user, getHomeRoute } = useAuth();
  return (
    <div className="min-h-screen bg-cream text-ink font-body flex items-center justify-center p-6">
      <div className="w-full max-w-lg space-y-6">
        <div className="flex items-center justify-center gap-3 mb-2">
          <div className="w-14 h-14 border-3 border-ink bg-coral brut-box flex items-center justify-center text-ink">
            <Shield className="w-7 h-7" strokeWidth={2.5} />
          </div>
          <h1 className="font-heading font-black text-4xl uppercase tracking-wider">
            403
          </h1>
        </div>

        <Alert variant="error" title="Không đủ quyền">
          <p className="leading-relaxed">
            Tài khoản hiện tại ({user?.role ?? 'chưa đăng nhập'}) không có
            quyền truy cập trang này.
          </p>
          <p className="leading-relaxed mt-2 text-ink/85">
            Bạn hãy quay lại trang phù hợp với vai trò của mình.
          </p>
        </Alert>

        <div className="flex flex-wrap gap-3 justify-center">
          <Button
            variant="secondary"
            onClick={() => window.history.back()}
            className="justify-center"
          >
            <ArrowLeft className="w-4 h-4" />
            Quay lại
          </Button>
          <Link to={user ? getHomeRoute() : '/login'}>
            <Button variant="primary" className="justify-center">
              <Home className="w-4 h-4" />
              {user ? 'Về trang chủ' : 'Đăng nhập'}
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};

const NotFoundPage: React.FC = () => (
  <div className="min-h-screen bg-cream text-ink font-body flex items-center justify-center p-6">
    <div className="w-full max-w-md text-center space-y-4">
      <h1 className="text-7xl font-heading font-black tracking-wider">404</h1>
      <p className="font-body text-ink/80 text-base">
        Trang bạn tìm kiếm không tồn tại.
      </p>
      <Link to="/">
        <Button variant="primary">
          <Home className="w-4 h-4" />
          Về trang chủ
        </Button>
      </Link>
    </div>
  </div>
);

export const router = createBrowserRouter([
  {
    element: (
      <AuthProvider>
        <Outlet />
      </AuthProvider>
    ),
    children: [
      // ============ AUTH / GUEST ONLY ============
      {
        path: 'login',
        element: (
          <GuestOnly>
            <LoginPage />
          </GuestOnly>
        ),
      },
      {
        path: 'register',
        element: (
          <GuestOnly>
            <RegisterTutorPage />
          </GuestOnly>
        ),
      },
      {
        path: 'invitation/accept',
        element: (
          <GuestOnly>
            <AcceptInvitationPage />
          </GuestOnly>
        ),
      },
      {
        path: 'reset-password',
        element: (
          <GuestOnly>
            <ResetPasswordPage />
          </GuestOnly>
        ),
      },

      // ============ UNAUTHORIZED (public hiển thị) ============
      {
        path: 'unauthorized',
        element: <UnauthorizedPage />,
      },

      // ============ ADMIN / TUTOR ROUTES ============
      {
        path: '/',
        element: (
          <RoleGuard allowedRoles={['ADMIN', 'TUTOR']}>
            <AppLayout />
          </RoleGuard>
        ),
        children: [
          {
            index: true,
            element: <DashboardPage />,
          },
          {
            path: 'dev/ui',
            element: <DevUiPage />,
          },
          {
            path: 'classes',
            element: <ClassesListPage />,
          },
          {
            path: 'classes/:id',
            element: <ClassDetailPage />,
          },
          {
            path: 'schedule',
            element: (
              <PlaceholderPage
                title="Lịch dạy & Chống trùng buổi"
                description="Xem lịch dạy theo tuần/tháng, tạo buổi học tự động từ quy tắc tuần, bảo đảm chống trùng giờ bằng DB exclusion."
                phase="Phase 3 (T3.1 - T3.6)"
              />
            ),
          },
          {
            path: 'attendance',
            element: (
              <PlaceholderPage
                title="Điểm danh nhanh"
                description="Điểm danh học sinh cả lớp chỉ với vài thao tác, cập nhật trạng thái có mặt, muộn, có phép, không phép."
                phase="Phase 4 (T4.1 - T4.2)"
              />
            ),
          },
          {
            path: 'assignments',
            element: (
              <PlaceholderPage
                title="Bài tập & Bảng điểm"
                description="Giao bài tập về nhà, bài kiểm tra và nhập điểm trực tiếp trên hệ thống."
                phase="Phase 5 (T5.1 - T5.3)"
              />
            ),
          },
          {
            path: 'tuition',
            element: (
              <PlaceholderPage
                title="Quản lý đợt học phí (Theo số buổi)"
                description="Theo dõi đợt học phí N buổi, cảnh báo sắp hết buổi hoặc chưa đóng. Không lưu tiền trực tiếp."
                phase="Phase 6 (T6.1 - T6.3)"
              />
            ),
          },
          {
            path: 'reports',
            element: (
              <PlaceholderPage
                title="Báo cáo tiến độ học tập"
                description="Tổng hợp tình hình đi học, điểm số, buổi học còn lại theo khoảng thời gian tùy chọn và xuất PDF/CSV."
                phase="Phase 7 (T7.1 - T7.4)"
              />
            ),
          },
        ],
      },

      // ============ STUDENT / PARENT PORTAL (placeholder) ============
      {
        path: 'portal',
        element: (
          <RoleGuard allowedRoles={['STUDENT', 'PARENT']}>
            <PlaceholderPage
              title="Cổng học sinh & Phụ huynh"
              description="Xem lịch học sắp tới, bài tập, điểm, báo cáo tiến độ và trạng thái học phí của chính mình (hoặc con cái)."
              phase="Phase 8 (T8.1 - T8.2)"
            />
          </RoleGuard>
        ),
      },

      // ============ 404 ============
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
