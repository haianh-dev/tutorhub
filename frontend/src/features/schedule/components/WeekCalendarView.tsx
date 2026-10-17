import React from 'react';
import { Clock, AlertTriangle } from 'lucide-react';
import { getTodayDateString } from '../dateUtils';
import type { Session } from '../../../types';

interface WeekCalendarViewProps {
  currentDate: Date; // Ngày mốc trong tuần
  sessions: Session[];
  onSelectSession: (session: Session) => void;
  onSelectDate: (date: string) => void;
}

const DAYS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật'];

export const WeekCalendarView: React.FC<WeekCalendarViewProps> = ({
  currentDate,
  sessions,
  onSelectSession,
  onSelectDate,
}) => {
  // Tính thứ 2 đầu tuần của currentDate (Monday = 1)
  const monday = new Date(currentDate);
  const day = monday.getUTCDay();
  const diff = monday.getUTCDate() - day + (day === 0 ? -6 : 1);
  monday.setUTCDate(diff);

  // Mảng 7 ngày trong tuần
  const weekDays = Array.from({ length: 7 }).map((_, i) => {
    const d = new Date(monday);
    d.setUTCDate(monday.getUTCDate() + i);
    const pad = (n: number) => n.toString().padStart(2, '0');
    const dateStr = `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`;
    return {
      dateStr,
      dayNumber: d.getUTCDate(),
      month: d.getUTCMonth() + 1,
      isToday: getTodayDateString() === dateStr,
    };
  });

  return (
    <div className="grid grid-cols-1 md:grid-cols-7 gap-3">
      {weekDays.map((dayItem, index) => {
        // Lọc các buổi học trong ngày này
        const daySessions = sessions.filter((s) => s.startAt.startsWith(dayItem.dateStr));

        return (
          <div
            key={dayItem.dateStr}
            className={`border-3 border-ink p-3 min-h-[180px] md:min-h-[360px] flex flex-col ${
              dayItem.isToday ? 'bg-yellow/15' : 'bg-paper'
            }`}
          >
            {/* Header cột ngày */}
            <div className="flex items-center justify-between pb-2 mb-2 border-b-2 border-ink">
              <div>
                <span className="text-xs font-heading font-black uppercase text-ink block">
                  {DAYS[index]}
                </span>
                <span className="text-lg font-black font-heading text-ink">
                  {dayItem.dayNumber}
                  <span className="text-xs font-normal text-ink/60">/{dayItem.month}</span>
                </span>
              </div>
              <button
                type="button"
                onClick={() => onSelectDate(dayItem.dateStr)}
                className="text-[10px] font-heading font-bold uppercase px-1.5 py-0.5 border border-ink bg-cream hover:bg-yellow cursor-pointer"
                title="Tạo buổi học vào ngày này"
              >
                + Thêm
              </button>
            </div>

            {/* Danh sách buổi trong ngày */}
            <div className="space-y-2 flex-1 overflow-y-auto">
              {daySessions.length === 0 ? (
                <div className="h-full flex items-center justify-center text-[11px] text-ink/30 italic">
                  Không có buổi
                </div>
              ) : (
                daySessions.map((session) => {
                  const startTime = session.startAt.substring(11, 16);
                  const endTime = session.endAt.substring(11, 16);

                  const isCancelled = session.status === 'CANCELLED';
                  const isCompleted = session.status === 'COMPLETED';

                  let cardBg = 'bg-yellow/30';
                  let borderCol = 'border-ink';
                  if (isCancelled) {
                    cardBg = 'bg-gray-200 opacity-60';
                  } else if (isCompleted) {
                    cardBg = 'bg-green-100';
                  }

                  return (
                    <div
                      key={session.id}
                      onClick={() => onSelectSession(session)}
                      className={`p-2 border-2 ${borderCol} ${cardBg} hover:translate-x-0.5 hover:-translate-y-0.5 transition-transform cursor-pointer text-left relative group`}
                    >
                      <div className="flex items-center justify-between text-[11px] font-bold text-ink">
                        <span className="flex items-center gap-1 font-mono">
                          <Clock className="w-3 h-3" /> {startTime} - {endTime}
                        </span>
                        {session.warnings && session.warnings.length > 0 && (
                          <span title="Có cảnh báo học sinh trùng lịch">
                            <AlertTriangle className="w-3.5 h-3.5 text-coral shrink-0" />
                          </span>
                        )}
                      </div>

                      <p
                        className={`text-xs font-black text-ink mt-1 truncate ${
                          isCancelled ? 'line-through' : ''
                        }`}
                      >
                        {session.className || `Lớp #${session.classId}`}
                      </p>

                      {session.topic && (
                        <p className="text-[11px] text-ink/80 truncate mt-0.5 italic">
                          {session.topic}
                        </p>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
