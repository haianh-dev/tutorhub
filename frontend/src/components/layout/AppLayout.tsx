import React, { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  CheckCircle2,
  FileText,
  CreditCard,
  BarChart3,
  Menu,
  X,
  GraduationCap,
  Bell,
  User as UserIcon,
} from 'lucide-react';

const navigation = [
  { name: 'Tổng quan', href: '/', icon: GraduationCap },
  { name: 'Lớp học', href: '/classes', icon: BookOpen },
  { name: 'Lịch dạy', href: '/schedule', icon: Calendar },
  { name: 'Điểm danh', href: '/attendance', icon: CheckCircle2 },
  { name: 'Bài tập & Điểm', href: '/assignments', icon: FileText },
  { name: 'Đợt học phí', href: '/tuition', icon: CreditCard },
  { name: 'Báo cáo tiến độ', href: '/reports', icon: BarChart3 },
];

export const AppLayout: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Top Navbar */}
      <header className="sticky top-0 z-40 bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            {/* Logo */}
            <div className="flex items-center gap-3">
              <Link to="/" className="flex items-center gap-2.5 group">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white shadow-md shadow-indigo-100 group-hover:scale-105 transition-transform">
                  <GraduationCap className="w-6 h-6" />
                </div>
                <div>
                  <span className="text-xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
                    TutorHub
                  </span>
                  <span className="hidden sm:inline-block ml-2 text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100">
                    Gia sư &amp; Lớp học
                  </span>
                </div>
              </Link>
            </div>

            {/* Desktop Navigation */}
            <nav className="hidden lg:flex items-center space-x-1">
              {navigation.map((item) => {
                const isActive = location.pathname === item.href;
                const Icon = item.icon;
                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    className={`flex items-center gap-2 px-3.5 py-2 text-sm font-medium rounded-lg transition-colors ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-700 font-semibold'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                    }`}
                  >
                    <Icon className={`w-4 h-4 ${isActive ? 'text-indigo-600' : 'text-slate-400'}`} />
                    {item.name}
                  </Link>
                );
              })}
            </nav>

            {/* Actions & Profile */}
            <div className="flex items-center gap-3">
              <button
                type="button"
                className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                title="Thông báo"
              >
                <Bell className="w-5 h-5" />
              </button>

              <div className="flex items-center gap-3 pl-2 border-l border-slate-200">
                <div className="w-9 h-9 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600">
                  <UserIcon className="w-5 h-5" />
                </div>
                <div className="hidden md:block text-left text-xs">
                  <div className="font-semibold text-slate-800">Gia sư Demo</div>
                  <div className="text-slate-500">TUTOR</div>
                </div>
              </div>

              {/* Mobile menu button */}
              <div className="lg:hidden flex items-center">
                <button
                  type="button"
                  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                  className="p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100"
                >
                  {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {mobileMenuOpen && (
          <div className="lg:hidden border-b border-slate-200 bg-white px-4 pt-2 pb-4 space-y-1">
            {navigation.map((item) => {
              const isActive = location.pathname === item.href;
              const Icon = item.icon;
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium ${
                    isActive
                      ? 'bg-indigo-50 text-indigo-700 font-semibold'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  {item.name}
                </Link>
              );
            })}
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 text-center text-xs text-slate-500">
          TutorHub — Hệ thống quản lý lịch và lớp học cho gia sư (Mục tiêu: Quản lý &amp; Giảng dạy hiệu quả)
        </div>
      </footer>
    </div>
  );
};

export default AppLayout;
