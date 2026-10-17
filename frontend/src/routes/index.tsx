import {
  createBrowserRouter,
  Outlet,
} from 'react-router-dom';
import { AuthProvider } from '../features/auth/AuthContext';
import AppLayout from '../components/layout/AppLayout';
import DashboardPage from '../features/dashboard/DashboardPage';
import ClassesListPage from '../features/classes/ClassesListPage';
import ClassDetailPage from '../features/classes/ClassDetailPage';
import SchedulePage from '../features/schedule/SchedulePage';
import PlaceholderPage from '../components/PlaceholderPage';
import DevUiPage from '../features/dev/DevUiPage';
import LoginPage from '../features/auth/pages/LoginPage';
import RegisterTutorPage from '../features/auth/pages/RegisterTutorPage';
import AcceptInvitationPage from '../features/auth/pages/AcceptInvitationPage';
import ResetPasswordPage from '../features/auth/pages/ResetPasswordPage';
import { GuestOnly, RoleGuard } from './RouteGuards';
import { UnauthorizedPage, NotFoundPage } from './ErrorPages';

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
            element: <SchedulePage />,
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
