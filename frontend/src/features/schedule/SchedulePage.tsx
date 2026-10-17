import React, { useState } from 'react';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Plus,
  Sparkles,
  Filter,
} from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { WeekCalendarView } from './components/WeekCalendarView';
import { MonthCalendarView } from './components/MonthCalendarView';
import { CreateSessionModal } from './components/CreateSessionModal';
import { SessionDetailModal } from './components/SessionDetailModal';
import { GenerateSessionsModal } from './components/GenerateSessionsModal';
import { useSessions } from './api';
import { useClassesQuery } from '../classes/api';
import type { Session } from '../../types';

export const SchedulePage: React.FC = () => {
  const [viewMode, setViewMode] = useState<'week' | 'month'>('week');
  const [currentDate, setCurrentDate] = useState<Date>(() => new Date());
  const [selectedClassId, setSelectedClassId] = useState<number | undefined>(undefined);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isGenerateOpen, setIsGenerateOpen] = useState(false);
  const [selectedSession, setSelectedSession] = useState<Session | null>(null);
  const [selectedDateForCreate, setSelectedDateForCreate] = useState<string | undefined>(undefined);

  // Tính khoảng ngày để gọi API theo viewMode
  const getQueryRange = () => {
    if (viewMode === 'week') {
      const monday = new Date(currentDate);
      const day = monday.getUTCDay();
      const diff = monday.getUTCDate() - day + (day === 0 ? -6 : 1);
      monday.setUTCDate(diff);

      const sunday = new Date(monday);
      sunday.setUTCDate(monday.getUTCDate() + 6);

      const pad = (n: number) => n.toString().padStart(2, '0');
      const fromStr = `${monday.getUTCFullYear()}-${pad(monday.getUTCMonth() + 1)}-${pad(monday.getUTCDate())}T00:00:00Z`;
      const toStr = `${sunday.getUTCFullYear()}-${pad(sunday.getUTCMonth() + 1)}-${pad(sunday.getUTCDate())}T23:59:59Z`;
      return { from: fromStr, to: toStr };
    } else {
      const year = currentDate.getUTCFullYear();
      const month = currentDate.getUTCMonth();
      const last = new Date(Date.UTC(year, month + 1, 0));

      const pad = (n: number) => n.toString().padStart(2, '0');
      const fromStr = `${year}-${pad(month + 1)}-01T00:00:00Z`;
      const toStr = `${year}-${pad(month + 1)}-${pad(last.getUTCDate())}T23:59:59Z`;
      return { from: fromStr, to: toStr };
    }
  };

  const { from, to } = getQueryRange();

  // Queries
  const { data: classesData } = useClassesQuery({ status: 'ACTIVE' });
  const classes = classesData?.content || [];

  const {
    data: sessions = [],
    isLoading,
    refetch,
  } = useSessions({
    from,
    to,
    classId: selectedClassId,
  });

  // Điều hướng thời gian
  const handlePrev = () => {
    const next = new Date(currentDate);
    if (viewMode === 'week') {
      next.setUTCDate(next.getUTCDate() - 7);
    } else {
      next.setUTCMonth(next.getUTCMonth() - 1);
    }
    setCurrentDate(next);
  };

  const handleNext = () => {
    const next = new Date(currentDate);
    if (viewMode === 'week') {
      next.setUTCDate(next.getUTCDate() + 7);
    } else {
      next.setUTCMonth(next.getUTCMonth() + 1);
    }
    setCurrentDate(next);
  };

  const handleToday = () => {
    setCurrentDate(new Date());
  };

  // Mở modal tạo buổi vào ngày cụ thể
  const handleSelectDateForCreate = (date: string) => {
    setSelectedDateForCreate(date);
    setIsCreateOpen(true);
  };

  // Mở modal chi tiết buổi học
  const handleSelectSession = (session: Session) => {
    setSelectedSession(session);
    setIsDetailOpen(true);
  };

  // Tiêu đề khoảng thời gian hiển thị
  const formatHeaderTitle = () => {
    const pad = (n: number) => n.toString().padStart(2, '0');
    if (viewMode === 'week') {
      const monday = new Date(currentDate);
      const day = monday.getUTCDay();
      const diff = monday.getUTCDate() - day + (day === 0 ? -6 : 1);
      monday.setUTCDate(diff);

      const sunday = new Date(monday);
      sunday.setUTCDate(monday.getUTCDate() + 6);

      return `Tuần: ${pad(monday.getUTCDate())}/${pad(monday.getUTCMonth() + 1)} — ${pad(sunday.getUTCDate())}/${pad(sunday.getUTCMonth() + 1)}/${sunday.getUTCFullYear()}`;
    } else {
      return `Tháng ${currentDate.getUTCMonth() + 1} / ${currentDate.getUTCFullYear()}`;
    }
  };

  return (
    <div className="space-y-6 font-body">
      {/* 1. Header & Actions Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-3 border-ink bg-paper p-4 brut-shadow">
        <div>
          <h1 className="font-heading font-black text-2xl uppercase tracking-wider text-ink flex items-center gap-2">
            <CalendarIcon className="w-7 h-7 text-ink" /> Lịch dạy & Buổi học
          </h1>
          <p className="text-xs text-ink/70 mt-0.5">
            Quản lý lịch học tuần/tháng, tạo buổi học đơn lẻ hoặc sinh tự động từ quy tắc tuần.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="secondary"
            onClick={() => setIsGenerateOpen(true)}
            className="text-xs"
          >
            <Sparkles className="w-4 h-4 mr-1 text-yellow fill-yellow" /> Sinh lịch lặp
          </Button>

          <Button
            type="button"
            variant="primary"
            onClick={() => {
              setSelectedDateForCreate(undefined);
              setIsCreateOpen(true);
            }}
            className="text-xs"
          >
            <Plus className="w-4 h-4 mr-1" /> Tạo buổi học
          </Button>
        </div>
      </div>

      {/* 2. Control Bar (Bộ lọc lớp, nút chuyển tuần/tháng, view switch) */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3 border-3 border-ink bg-cream">
        {/* Điều hướng Next/Prev/Today */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handlePrev}
            className="p-1.5 border-2 border-ink bg-paper hover:bg-yellow transition-colors cursor-pointer"
            title="Trước"
          >
            <ChevronLeft className="w-5 h-5 text-ink" />
          </button>

          <button
            type="button"
            onClick={handleToday}
            className="px-3 py-1 text-xs font-heading font-black uppercase border-2 border-ink bg-paper hover:bg-yellow transition-colors cursor-pointer"
          >
            Hôm nay
          </button>

          <button
            type="button"
            onClick={handleNext}
            className="p-1.5 border-2 border-ink bg-paper hover:bg-yellow transition-colors cursor-pointer"
            title="Sau"
          >
            <ChevronRight className="w-5 h-5 text-ink" />
          </button>

          <span className="font-heading font-black text-sm uppercase text-ink ml-2">
            {formatHeaderTitle()}
          </span>
        </div>

        {/* Lọc theo lớp & Chuyển chế độ xem */}
        <div className="flex items-center gap-2">
          {/* Lọc lớp */}
          <div className="flex items-center gap-1 bg-paper border-2 border-ink px-2 py-1 text-xs">
            <Filter className="w-3.5 h-3.5 text-ink" />
            <select
              value={selectedClassId ?? ''}
              onChange={(e) =>
                setSelectedClassId(e.target.value ? Number(e.target.value) : undefined)
              }
              className="bg-transparent font-bold text-ink focus:outline-none cursor-pointer"
            >
              <option value="">Tất cả các lớp</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {/* Toggle Tuần / Tháng */}
          <div className="flex border-2 border-ink bg-paper">
            <button
              type="button"
              onClick={() => setViewMode('week')}
              className={`px-3 py-1 text-xs font-heading font-bold uppercase transition-colors cursor-pointer ${
                viewMode === 'week' ? 'bg-yellow text-ink font-black' : 'text-ink/70 hover:bg-cream'
              }`}
            >
              Tuần
            </button>
            <button
              type="button"
              onClick={() => setViewMode('month')}
              className={`px-3 py-1 text-xs font-heading font-bold uppercase border-l-2 border-ink transition-colors cursor-pointer ${
                viewMode === 'month' ? 'bg-yellow text-ink font-black' : 'text-ink/70 hover:bg-cream'
              }`}
            >
              Tháng
            </button>
          </div>
        </div>
      </div>

      {/* 3. Lịch Content */}
      {isLoading ? (
        <div className="p-12 text-center border-3 border-ink bg-paper">
          <p className="font-heading font-bold text-sm uppercase text-ink animate-pulse">
            Đang tải dữ liệu lịch học...
          </p>
        </div>
      ) : viewMode === 'week' ? (
        <WeekCalendarView
          currentDate={currentDate}
          sessions={sessions}
          onSelectSession={handleSelectSession}
          onSelectDate={handleSelectDateForCreate}
        />
      ) : (
        <MonthCalendarView
          currentDate={currentDate}
          sessions={sessions}
          onSelectSession={handleSelectSession}
          onSelectDate={handleSelectDateForCreate}
        />
      )}

      {/* Modals */}
      <CreateSessionModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        classes={classes}
        initialDate={selectedDateForCreate}
        initialClassId={selectedClassId}
        onSuccess={() => refetch()}
      />

      <SessionDetailModal
        session={selectedSession}
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        onSuccess={() => refetch()}
      />

      <GenerateSessionsModal
        isOpen={isGenerateOpen}
        onClose={() => setIsGenerateOpen(false)}
        classes={classes}
        initialClassId={selectedClassId}
        onSuccess={() => refetch()}
      />
    </div>
  );
};

export default SchedulePage;
