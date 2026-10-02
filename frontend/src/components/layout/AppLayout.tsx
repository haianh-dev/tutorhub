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
  LogOut,
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

  // Find page title according to current route
  const currentNav = navigation.find((item) => item.href === location.pathname);
  const pageTitle = currentNav ? currentNav.name : 'TutorHub';

  return (
    <div className="min-h-screen bg-cream text-ink flex flex-col lg:flex-row font-body">
      {/* Desktop Sidebar (lg >= 1024px) */}
      <aside className="hidden lg:flex lg:flex-col lg:w-64 lg:shrink-0 bg-ink text-paper border-r-4 border-ink min-h-screen">
        {/* Brand Header in Sidebar */}
        <div className="h-16 px-6 border-b-4 border-paper/20 flex items-center gap-3 bg-ink select-none">
          <div className="w-9 h-9 border-3 border-ink bg-yellow text-ink flex items-center justify-center font-heading font-black text-lg">
            T
          </div>
          <div>
            <span className="font-heading font-black text-lg tracking-wider uppercase text-paper block">
              TutorHub
            </span>
            <span className="font-heading text-[10px] uppercase font-bold tracking-widest text-paper/70 block">
              Quản lý gia sư
            </span>
          </div>
        </div>

        {/* Sidebar Nav Links */}
        <nav className="flex-1 p-4 flex flex-col gap-2">
          {navigation.map((item) => {
            const isActive = location.pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.name}
                to={item.href}
                className={`flex items-center gap-3 px-4 py-3 font-heading text-xs font-bold uppercase transition-colors border-3 ${
                  isActive
                    ? 'bg-yellow text-ink border-ink brut-box'
                    : 'bg-ink text-paper border-transparent hover:bg-paper hover:text-ink hover:border-paper'
                }`}
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span className="truncate">{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* User Info & Role at Bottom of Sidebar */}
        <div className="p-4 border-t-4 border-paper/20 bg-ink">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 border-3 border-paper bg-yellow text-ink font-heading font-bold text-base flex items-center justify-center select-none shrink-0">
              G
            </div>
            <div className="min-w-0 flex-1">
              <div className="font-heading font-bold text-xs uppercase text-paper truncate">
                Gia sư Demo
              </div>
              <div className="font-body text-[11px] text-paper/70 font-semibold uppercase">
                TUTOR
              </div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Column */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Header */}
        <header className="h-16 bg-cream border-b-4 border-ink sticky top-0 z-30 px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          <div className="flex items-center gap-3">
            {/* Mobile Hamburger Button */}
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="lg:hidden w-11 h-11 border-3 border-ink bg-paper flex items-center justify-center text-ink cursor-pointer focus-visible:outline-3 focus-visible:outline-ink focus-visible:outline-offset-2"
              aria-label={mobileMenuOpen ? 'Đóng menu' : 'Mở menu'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>

            {/* Current Page Title */}
            <h1 className="font-heading font-black text-lg sm:text-xl uppercase tracking-wide text-ink">
              {pageTitle}
            </h1>
          </div>

          {/* Right Header: Profile badge and Logout */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 border-3 border-ink bg-yellow text-ink font-heading font-bold text-base flex items-center justify-center select-none">
                G
              </div>
              <div className="hidden sm:block text-left">
                <div className="font-heading font-bold text-xs uppercase text-ink">
                  Gia sư Demo
                </div>
                <div className="font-body text-[10px] text-ink/75 font-semibold">
                  TUTOR
                </div>
              </div>
            </div>

            <button
              type="button"
              className="w-10 h-10 border-3 border-ink bg-paper flex items-center justify-center text-ink hover:bg-coral cursor-pointer transition-colors"
              title="Đăng xuất"
              aria-label="Đăng xuất"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </header>

        {/* Mobile Navigation Drawer */}
        {mobileMenuOpen && (
          <div className="lg:hidden border-b-4 border-ink bg-paper px-4 py-4 space-y-2">
            {navigation.map((item) => {
              const isActive = location.pathname === item.href;
              const Icon = item.icon;
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center gap-3 px-4 py-3 font-heading text-xs font-bold uppercase border-3 ${
                    isActive
                      ? 'bg-yellow text-ink border-ink brut-box'
                      : 'bg-paper text-ink border-ink hover:bg-cream'
                  }`}
                >
                  <Icon className="w-5 h-5 shrink-0" />
                  <span>{item.name}</span>
                </Link>
              );
            })}
          </div>
        )}

        {/* Main Workspace Content Area */}
        <main className="flex-1 w-full max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-12 py-8">
          <Outlet />
        </main>

        {/* App Footer */}
        <footer className="border-t-4 border-ink bg-paper py-4 px-4 sm:px-6 text-center text-xs font-body font-bold uppercase text-ink">
          TutorHub — Hệ thống quản lý lịch và lớp học cho gia sư
        </footer>
      </div>
    </div>
  );
};

export default AppLayout;
