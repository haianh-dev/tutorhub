import React, { useMemo, useState } from 'react';
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
import { useAuth } from '../../features/auth/AuthContext';
import { Badge } from '../ui/Badge';

const navigation = [
  { name: 'Tổng quan', href: '/', icon: GraduationCap },
  { name: 'Lớp học', href: '/classes', icon: BookOpen },
  { name: 'Lịch dạy', href: '/schedule', icon: Calendar },
  { name: 'Điểm danh', href: '/attendance', icon: CheckCircle2 },
  { name: 'Bài tập & Điểm', href: '/assignments', icon: FileText },
  { name: 'Đợt học phí', href: '/tuition', icon: CreditCard },
  { name: 'Báo cáo tiến độ', href: '/reports', icon: BarChart3 },
];

function initialLetter(fullName: string): string {
  const trimmed = fullName.trim();
  if (!trimmed) return '?';
  const parts = trimmed.split(/\s+/);
  const last = parts[parts.length - 1] ?? trimmed;
  const ch = (last[0] ?? '?').toUpperCase();
  return /[A-Z0-9]/.test(ch) ? ch : '?';
}

export const AppLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();

  const currentNav = useMemo(
    () => navigation.find((item) => item.href === location.pathname),
    [location.pathname],
  );
  const pageTitle = currentNav ? currentNav.name : 'TutorHub';

  const userLetter = user ? initialLetter(user.fullName) : '?';
  const userName = user?.fullName ?? 'Đang tải...';
  const userRole = user?.role ?? '—';
  const userEmail = user?.email ?? '';

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="min-h-screen bg-cream text-ink flex flex-col lg:flex-row font-body">
      {/* Desktop Sidebar */}
      <aside className="hidden lg:flex lg:flex-col lg:w-64 lg:shrink-0 bg-ink text-paper border-r-4 border-ink min-h-screen">
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

        <div className="p-4 border-t-4 border-paper/20 bg-ink">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 border-3 border-paper bg-yellow text-ink font-heading font-black text-base flex items-center justify-center select-none shrink-0">
              {userLetter}
            </div>
            <div className="min-w-0 flex-1">
              <div className="font-heading font-bold text-xs uppercase text-paper truncate" title={userEmail}>
                {userName}
              </div>
              <div className="mt-1">
                <Badge variant="yellow" className="!border-paper !text-ink !bg-yellow">
                  {userRole}
                </Badge>
              </div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Column */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-cream border-b-4 border-ink sticky top-0 z-30 px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMobileMenuOpen((v) => !v)}
              className="lg:hidden w-11 h-11 border-3 border-ink bg-paper flex items-center justify-center text-ink cursor-pointer focus-visible:outline-3 focus-visible:outline-ink focus-visible:outline-offset-2"
              aria-label={mobileMenuOpen ? 'Đóng menu' : 'Mở menu'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>

            <h1 className="font-heading font-black text-lg sm:text-xl uppercase tracking-wide text-ink">
              {pageTitle}
            </h1>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 border-3 border-ink bg-yellow text-ink font-heading font-black text-base flex items-center justify-center select-none">
                {userLetter}
              </div>
              <div className="hidden sm:block text-left min-w-0">
                <div
                  className="font-heading font-bold text-xs uppercase text-ink truncate max-w-[200px]"
                  title={userEmail}
                >
                  {userName}
                </div>
                <div className="font-body text-[10px] text-ink/75 font-semibold uppercase">
                  {userRole}
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={handleLogout}
              className="w-10 h-10 border-3 border-ink bg-paper flex items-center justify-center text-ink hover:bg-coral cursor-pointer transition-colors"
              title="Đăng xuất"
              aria-label="Đăng xuất"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </header>

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

        <main className="flex-1 w-full max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-12 py-8">
          <Outlet />
        </main>

        <footer className="border-t-4 border-ink bg-paper py-4 px-4 sm:px-6 text-center text-xs font-body font-bold uppercase text-ink">
          TutorHub — Hệ thống quản lý lịch và lớp học cho gia sư
        </footer>
      </div>
    </div>
  );
};

export default AppLayout;
