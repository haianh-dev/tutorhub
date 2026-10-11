import React from 'react';
import type { Session } from '../../../types';

interface MonthCalendarViewProps {
  currentDate: Date; // Tháng mốc
  sessions: Session[];
  onSelectSession: (session: Session) => void;
  onSelectDate: (date: string) => void;
}

const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

export const MonthCalendarView: React.FC<MonthCalendarViewProps> = ({
  currentDate,
  sessions,
  onSelectSession,
  onSelectDate,
}) => {
  const year = currentDate.getUTCFullYear();
  const month = currentDate.getUTCMonth(); // 0-indexed

  // Ngày đầu tiên của tháng
  const firstDay = new Date(Date.UTC(year, month, 1));
  // Số ngày trong tháng
  const daysInMonth = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();

  // Thứ trong tuần của ngày 1 (Monday=1, ..., Sunday=0 -> chuyển sang 1-7)
  let startDayOfWeek = firstDay.getUTCDay();
  startDayOfWeek = startDayOfWeek === 0 ? 7 : startDayOfWeek;

  // Tổng số ô trong lưới (bắt đầu từ thứ 2)
  const paddingDays = startDayOfWeek - 1;
  const pad = (n: number) => n.toString().padStart(2, '0');

  return (
    <div className="border-3 border-ink bg-paper">
      {/* Header thứ */}
      <div className="grid grid-cols-7 border-b-2 border-ink bg-cream text-center font-heading font-black text-xs uppercase py-2">
        {WEEKDAYS.map((day) => (
          <div key={day} className="text-ink">
            {day}
          </div>
        ))}
      </div>

      {/* Grid ngày */}
      <div className="grid grid-cols-7">
        {/* Padding ô trống đầu tháng */}
        {Array.from({ length: paddingDays }).map((_, i) => (
          <div
            key={`pad-${i}`}
            className="border-b border-r border-ink/20 min-h-[90px] md:min-h-[110px] bg-ink/5 p-1"
          />
        ))}

        {/* Các ngày trong tháng */}
        {Array.from({ length: daysInMonth }).map((_, i) => {
          const dayNum = i + 1;
          const dateStr = `${year}-${pad(month + 1)}-${pad(dayNum)}`;
          const isToday = new Date().toISOString().split('T')[0] === dateStr;

          // Buổi học trong ngày
          const daySessions = sessions.filter((s) => s.startAt.startsWith(dateStr));

          return (
            <div
              key={dateStr}
              className={`border-b border-r border-ink p-1.5 min-h-[90px] md:min-h-[110px] flex flex-col transition-colors ${
                isToday ? 'bg-yellow/15' : 'bg-paper hover:bg-cream/40'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span
                  className={`text-xs font-heading font-bold px-1 ${
                    isToday ? 'bg-yellow border border-ink text-ink font-black' : 'text-ink/80'
                  }`}
                >
                  {dayNum}
                </span>
                <button
                  type="button"
                  onClick={() => onSelectDate(dateStr)}
                  className="text-[9px] font-mono text-ink/50 hover:text-ink hover:font-bold cursor-pointer"
                  title="Thêm buổi học"
                >
                  +
                </button>
              </div>

              {/* Danh sách buổi thu gọn */}
              <div className="space-y-1 flex-1 overflow-y-auto">
                {daySessions.slice(0, 3).map((session) => {
                  const startTime = session.startAt.substring(11, 16);
                  const isCancelled = session.status === 'CANCELLED';
                  const isCompleted = session.status === 'COMPLETED';

                  let badgeBg = 'bg-yellow/40 border-ink';
                  if (isCancelled) badgeBg = 'bg-gray-200 opacity-50 line-through';
                  if (isCompleted) badgeBg = 'bg-green-200 border-green-800';

                  return (
                    <div
                      key={session.id}
                      onClick={() => onSelectSession(session)}
                      className={`px-1 py-0.5 border text-[10px] font-bold truncate cursor-pointer hover:scale-102 transition-transform flex items-center justify-between ${badgeBg}`}
                      title={`${session.className || 'Lớp học'}: ${startTime}`}
                    >
                      <span className="truncate">{session.className || `Lớp #${session.classId}`}</span>
                      <span className="font-mono text-[9px] ml-1 shrink-0">{startTime}</span>
                    </div>
                  );
                })}

                {daySessions.length > 3 && (
                  <p className="text-[10px] font-bold text-ink/60 text-center">
                    +{daySessions.length - 3} buổi khác
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
