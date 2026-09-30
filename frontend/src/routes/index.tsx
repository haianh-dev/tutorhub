import { createBrowserRouter } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import DashboardPage from '../features/dashboard/DashboardPage';
import PlaceholderPage from '../components/PlaceholderPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: 'classes',
        element: (
          <PlaceholderPage
            title="Quản lý Lớp học & Học sinh"
            description="Tạo và quản lý lớp học 1:1 hoặc nhóm, ghi danh học sinh và gửi lời mời tham gia lớp."
            phase="Phase 2 (T2.1 - T2.3)"
          />
        ),
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
      {
        path: '*',
        element: (
          <div className="text-center py-16">
            <h1 className="text-4xl font-bold text-slate-800">404</h1>
            <p className="text-slate-500 mt-2">Trang bạn tìm kiếm không tồn tại.</p>
          </div>
        ),
      },
    ],
  },
]);
