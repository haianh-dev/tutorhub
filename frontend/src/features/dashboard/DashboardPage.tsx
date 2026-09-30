import React from 'react';
import { BookOpen, Calendar, CheckCircle2, CreditCard, ArrowRight, ShieldCheck, Users } from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const stats = [
    { label: 'Lớp đang phụ trách', value: '0', sub: 'Chưa có lớp nào', icon: BookOpen, color: 'text-blue-600', bg: 'bg-blue-50' },
    { label: 'Buổi học tuần này', value: '0', sub: 'Lịch dạy trống', icon: Calendar, color: 'text-indigo-600', bg: 'bg-indigo-50' },
    { label: 'Cần điểm danh', value: '0', sub: 'Tất cả đã hoàn thành', icon: CheckCircle2, color: 'text-emerald-600', bg: 'bg-emerald-50' },
    { label: 'Đợt học phí chưa nộp', value: '0', sub: 'Cần nhắc qua Zalo', icon: CreditCard, color: 'text-amber-600', bg: 'bg-amber-50' },
  ];

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="rounded-2xl bg-gradient-to-r from-indigo-700 via-indigo-600 to-violet-600 p-6 sm:p-8 text-white shadow-xl shadow-indigo-100">
        <div className="max-w-3xl space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/15 text-xs font-semibold text-indigo-100 backdrop-blur-sm">
            <ShieldCheck className="w-3.5 h-3.5" />
            TutorHub Phase 0 — Khởi tạo nền tảng thành công
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Chào mừng bạn đến với TutorHub!
          </h1>
          <p className="text-indigo-100 text-sm sm:text-base leading-relaxed">
            Công cụ hỗ trợ gia sư quản lý lớp học, chống ghi nhận trùng lịch dạy, điểm danh nhanh và theo dõi đợt học phí minh bạch.
          </p>
          <div className="pt-2 flex flex-wrap gap-3">
            <Link
              to="/classes"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-indigo-700 font-semibold text-sm shadow hover:bg-indigo-50 transition-colors"
            >
              <Users className="w-4 h-4" />
              Bắt đầu tạo lớp học
              <ArrowRight className="w-4 h-4 ml-1" />
            </Link>
          </div>
        </div>
      </div>

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {stats.map((item) => {
          const Icon = item.icon;
          return (
            <div
              key={item.label}
              className="bg-white rounded-xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  {item.label}
                </span>
                <div className={`w-9 h-9 rounded-lg ${item.bg} ${item.color} flex items-center justify-center`}>
                  <Icon className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3 text-2xl font-bold text-slate-900">{item.value}</div>
              <div className="mt-1 text-xs text-slate-500">{item.sub}</div>
            </div>
          );
        })}
      </div>

      {/* Feature Progress Section */}
      <div className="bg-white rounded-xl border border-slate-200/80 p-6 shadow-sm">
        <h2 className="text-lg font-bold text-slate-900 mb-2">Tiến trình triển khai hệ thống</h2>
        <p className="text-sm text-slate-500 mb-5">
          TutorHub được xây dựng từng bước theo ROADMAP với kiểm thử nghiêm ngặt.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="p-4 rounded-lg bg-emerald-50 border border-emerald-100">
            <div className="text-xs font-bold text-emerald-800 uppercase tracking-wide">Phase 0 — Nền tảng</div>
            <div className="text-sm font-semibold text-emerald-900 mt-1">Backend + Database + Frontend Skeleton</div>
            <div className="text-xs text-emerald-700 mt-2 font-medium">✓ Đang hoàn tất T0.4</div>
          </div>

          <div className="p-4 rounded-lg bg-slate-50 border border-slate-200">
            <div className="text-xs font-bold text-slate-600 uppercase tracking-wide">Phase 1 — Xác thực</div>
            <div className="text-sm font-semibold text-slate-900 mt-1">Đăng nhập, JWT, 4 Vai trò, Lời mời</div>
            <div className="text-xs text-slate-500 mt-2">Sắp triển khai (T1.1 - T1.6)</div>
          </div>

          <div className="p-4 rounded-lg bg-slate-50 border border-slate-200">
            <div className="text-xs font-bold text-slate-600 uppercase tracking-wide">Phase 2 &amp; 3 — Lớp &amp; Lịch</div>
            <div className="text-sm font-semibold text-slate-900 mt-1">Quản lý lớp 1:1 / Nhóm, Chống trùng lịch</div>
            <div className="text-xs text-slate-500 mt-2">Quy tắc loại trừ DB exclusion</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
