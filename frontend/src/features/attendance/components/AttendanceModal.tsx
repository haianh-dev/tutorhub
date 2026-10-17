import React, { useState, useMemo } from 'react';
import {
  X,
  UserCheck,
  CheckCheck,
  Clock,
  AlertCircle,
  Calendar,
  Save,
  Check,
  Users,
  MessageSquare,
} from 'lucide-react';
import { Button } from '../../../components/ui/Button';
import { Badge } from '../../../components/ui/Badge';
import { Alert } from '../../../components/ui/Alert';
import { useClassStudentsQuery } from '../../classes/api';
import { useSessionAttendance, useBulkUpdateAttendance } from '../api';
import type { AttendanceStatus, AttendanceItemRequest } from '../types';
import type { Session, Classroom } from '../../../types';

interface AttendanceModalProps {
  isOpen: boolean;
  onClose: () => void;
  session: Session | null;
  classroom?: Classroom | null;
  onSuccess?: () => void;
}

interface StudentAttendanceState {
  studentId: number;
  studentName: string;
  studentEmail: string;
  status: AttendanceStatus;
  note: string;
}

const STATUS_CONFIG: Record<
  AttendanceStatus,
  { label: string; activeClass: string; inactiveClass: string }
> = {
  PRESENT: {
    label: 'Có mặt',
    activeClass: 'bg-emerald-400 text-ink border-ink font-bold shadow-[2px_2px_0px_#000]',
    inactiveClass: 'bg-paper text-ink/70 border-ink/30 hover:border-ink hover:text-ink',
  },
  LATE: {
    label: 'Đi muộn',
    activeClass: 'bg-amber-300 text-ink border-ink font-bold shadow-[2px_2px_0px_#000]',
    inactiveClass: 'bg-paper text-ink/70 border-ink/30 hover:border-ink hover:text-ink',
  },
  ABSENT_EXCUSED: {
    label: 'Có phép',
    activeClass: 'bg-orange-300 text-ink border-ink font-bold shadow-[2px_2px_0px_#000]',
    inactiveClass: 'bg-paper text-ink/70 border-ink/30 hover:border-ink hover:text-ink',
  },
  ABSENT_UNEXCUSED: {
    label: 'Không phép',
    activeClass: 'bg-coral text-white border-ink font-bold shadow-[2px_2px_0px_#000]',
    inactiveClass: 'bg-paper text-ink/70 border-ink/30 hover:border-ink hover:text-ink',
  },
};

export const AttendanceModal: React.FC<AttendanceModalProps> = ({
  isOpen,
  onClose,
  session,
  classroom,
  onSuccess,
}) => {
  const [overrides, setOverrides] = useState<Record<number, { status?: AttendanceStatus; note?: string }>>({});
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const sessionId = session?.id;
  const classId = session?.classId;

  // Lấy danh sách học sinh đang học trong lớp
  const {
    data: enrollments = [],
    isLoading: isLoadingStudents,
    error: studentsError,
  } = useClassStudentsQuery(classId, 'ACTIVE');

  // Lấy dữ liệu điểm danh hiện tại của buổi học
  const {
    data: existingAttendance = [],
    isLoading: isLoadingAttendance,
    error: attendanceError,
  } = useSessionAttendance(sessionId);

  // Mutation cập nhật điểm danh hàng loạt
  const bulkMutation = useBulkUpdateAttendance(sessionId ?? 0, {
    onSuccess: () => {
      setSuccessMessage('Lưu điểm danh thành công!');
      setTimeout(() => {
        setSuccessMessage(null);
        setOverrides({});
        onSuccess?.();
        onClose();
      }, 700);
    },
    onError: (err: unknown) => {
      const errObj = err as { response?: { data?: { detail?: string; message?: string } } };
      const detail =
        errObj.response?.data?.detail ||
        errObj.response?.data?.message ||
        'Có lỗi xảy ra khi lưu điểm danh. Vui lòng thử lại!';
      setErrorMessage(detail);
    },
  });

  // Derived state: kết hợp enrollments, existingAttendance và overrides
  const items: StudentAttendanceState[] = useMemo(() => {
    if (!enrollments) return [];
    const attendanceMap = new Map(
      existingAttendance.map((rec) => [
        rec.studentId,
        { status: rec.status, note: rec.note || '' },
      ]),
    );

    return enrollments.map((enr) => {
      const existing = attendanceMap.get(enr.studentId);
      const override = overrides[enr.studentId];
      return {
        studentId: enr.studentId,
        studentName: enr.studentName || `Học sinh #${enr.studentId}`,
        studentEmail: enr.studentEmail || '',
        // Mặc định là PRESENT (có mặt) theo AC FR-4 nếu chưa từng được điểm danh
        status: override?.status ?? existing?.status ?? 'PRESENT',
        note: override?.note !== undefined ? override.note : (existing?.note ?? ''),
      };
    });
  }, [enrollments, existingAttendance, overrides]);

  // Cập nhật trạng thái cho 1 học sinh
  const handleStatusChange = (studentId: number, nextStatus: AttendanceStatus) => {
    setOverrides((prev) => ({
      ...prev,
      [studentId]: { ...prev[studentId], status: nextStatus },
    }));
  };

  // Cập nhật ghi chú cho 1 học sinh
  const handleNoteChange = (studentId: number, note: string) => {
    setOverrides((prev) => ({
      ...prev,
      [studentId]: { ...prev[studentId], note },
    }));
  };

  // Nút thao tác nhanh: Tất cả có mặt
  const handleSetAllPresent = () => {
    const nextOverrides: Record<number, { status: AttendanceStatus }> = {};
    enrollments.forEach((enr) => {
      nextOverrides[enr.studentId] = { status: 'PRESENT' };
    });
    setOverrides(nextOverrides);
  };

  // Submit bulk
  const handleSave = () => {
    if (!session || items.length === 0) return;
    setErrorMessage(null);

    const records: AttendanceItemRequest[] = items.map((item) => ({
      studentId: item.studentId,
      status: item.status,
      note: item.note.trim() ? item.note.trim() : undefined,
    }));

    bulkMutation.mutate({ records });
  };

  // Thống kê nhanh
  const stats = useMemo(() => {
    const total = items.length;
    const present = items.filter((i) => i.status === 'PRESENT').length;
    const late = items.filter((i) => i.status === 'LATE').length;
    const excused = items.filter((i) => i.status === 'ABSENT_EXCUSED').length;
    const unexcused = items.filter((i) => i.status === 'ABSENT_UNEXCUSED').length;
    return { total, present, late, excused, unexcused };
  }, [items]);

  if (!isOpen || !session) return null;

  const isOneOnOne = classroom?.classType === 'ONE_ON_ONE' || items.length === 1;
  const isLoading = isLoadingStudents || isLoadingAttendance;
  const error = studentsError || attendanceError;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-ink/60 backdrop-blur-xs overflow-y-auto"
    >
      <div className="w-full max-w-3xl border-3 border-ink bg-paper brut-box p-5 sm:p-7 space-y-5 max-h-[90vh] flex flex-col my-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b-3 border-ink pb-4 shrink-0">
          <div className="space-y-1">
            <div className="flex items-center gap-2 flex-wrap">
              <div className="w-8 h-8 bg-neon-green border-2 border-ink flex items-center justify-center font-heading font-black text-ink shadow-[2px_2px_0px_#000]">
                <UserCheck className="w-5 h-5" />
              </div>
              <h2 className="font-heading font-black text-xl sm:text-2xl uppercase tracking-wide text-ink">
                Điểm danh buổi học
              </h2>
              {classroom?.classType && (
                <Badge variant={classroom.classType === 'ONE_ON_ONE' ? 'blue' : 'yellow'}>
                  {classroom.classType === 'ONE_ON_ONE' ? 'Lớp 1:1' : 'Lớp nhóm'}
                </Badge>
              )}
            </div>

            <div className="flex items-center gap-3 text-xs sm:text-sm text-ink/80 flex-wrap pt-1 font-mono">
              <span className="flex items-center gap-1 font-bold text-ink">
                <Users className="w-3.5 h-3.5" />
                {session.className}
              </span>
              <span>•</span>
              <span className="flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5" />
                {session.startAt.slice(0, 10)}
              </span>
              <span>•</span>
              <span className="flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                {session.startAt.slice(11, 16)} - {session.endAt.slice(11, 16)}
              </span>
              {session.topic && (
                <>
                  <span>•</span>
                  <span className="text-ink font-semibold">Chủ đề: {session.topic}</span>
                </>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 hover:bg-cream border-2 border-transparent hover:border-ink transition-colors self-start shrink-0"
            title="Đóng"
          >
            <X className="w-6 h-6 text-ink" />
          </button>
        </div>

        {/* Thông báo Alert */}
        {errorMessage && (
          <Alert variant="error" title="Lỗi điểm danh">
            {errorMessage}
          </Alert>
        )}
        {successMessage && (
          <Alert variant="info" title="Thành công">
            {successMessage}
          </Alert>
        )}

        {/* Thanh công cụ và thống kê */}
        {!isLoading && !error && items.length > 0 && (
          <div className="flex items-center justify-between gap-3 flex-wrap bg-cream border-2 border-ink p-3 brut-box shrink-0">
            {/* Quick action: Tất cả có mặt */}
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleSetAllPresent}
                className="px-3 py-1.5 bg-paper border-2 border-ink text-xs font-heading font-bold uppercase tracking-wider hover:bg-emerald-300 transition-colors flex items-center gap-1.5 shadow-[2px_2px_0px_#000] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none"
              >
                <CheckCheck className="w-4 h-4 text-emerald-700" />
                Tất cả có mặt
              </button>
              <span className="text-xs text-ink/70 font-mono">
                (Mặc định tất cả học sinh đã được đánh dấu Có mặt)
              </span>
            </div>

            {/* Thống kê thẻ nhỏ */}
            <div className="flex items-center gap-1.5 text-xs font-mono font-bold">
              <span className="px-2 py-0.5 bg-emerald-400 border border-ink">
                Có mặt: {stats.present}
              </span>
              {stats.late > 0 && (
                <span className="px-2 py-0.5 bg-amber-300 border border-ink">
                  Muộn: {stats.late}
                </span>
              )}
              {stats.excused > 0 && (
                <span className="px-2 py-0.5 bg-orange-300 border border-ink">
                  Có phép: {stats.excused}
                </span>
              )}
              {stats.unexcused > 0 && (
                <span className="px-2 py-0.5 bg-coral text-white border border-ink">
                  Không phép: {stats.unexcused}
                </span>
              )}
            </div>
          </div>
        )}

        {/* Body content */}
        <div className="overflow-y-auto space-y-4 pr-1 grow">
          {isLoading ? (
            <div className="py-12 text-center font-heading uppercase text-xs tracking-widest text-ink/60">
              Đang tải danh sách học sinh…
            </div>
          ) : error ? (
            <div className="py-8 text-center text-coral font-body text-sm">
              Không thể tải danh sách học sinh. Vui lòng thử lại!
            </div>
          ) : items.length === 0 ? (
            <div className="py-8 text-center bg-cream border-2 border-dashed border-ink p-6 space-y-2">
              <AlertCircle className="w-8 h-8 text-ink/60 mx-auto" />
              <p className="font-heading font-bold text-sm uppercase">
                Chưa có học sinh nào trong lớp
              </p>
              <p className="text-xs text-ink/70">
                Hãy ghi danh học sinh vào lớp học trước khi thực hiện điểm danh buổi học này.
              </p>
            </div>
          ) : isOneOnOne ? (
            /* ================= GIAO DIỆN LỚP 1:1 (TỐI GIẢN & NHANH) ================= */
            <div className="bg-cream border-3 border-ink p-5 sm:p-6 brut-box space-y-5">
              <div className="flex items-center justify-between border-b-2 border-ink pb-3">
                <div>
                  <h3 className="font-heading font-black text-lg text-ink">
                    {items[0].studentName}
                  </h3>
                  <p className="text-xs text-ink/70 font-mono">{items[0].studentEmail}</p>
                </div>
                <Badge variant={items[0].status === 'PRESENT' ? 'yellow' : 'coral'}>
                  {STATUS_CONFIG[items[0].status].label}
                </Badge>
              </div>

              {/* 4 nút trạng thái to bản */}
              <div className="space-y-2">
                <label className="text-xs font-heading font-bold uppercase tracking-wider block text-ink">
                  Trạng thái tham gia
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                  {(Object.keys(STATUS_CONFIG) as AttendanceStatus[]).map((statusKey) => {
                    const cfg = STATUS_CONFIG[statusKey];
                    const isSelected = items[0].status === statusKey;
                    return (
                      <button
                        key={statusKey}
                        type="button"
                        onClick={() => handleStatusChange(items[0].studentId, statusKey)}
                        className={`py-3 px-3 border-2 font-heading text-xs uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 ${
                          isSelected ? cfg.activeClass : cfg.inactiveClass
                        }`}
                      >
                        {isSelected && <Check className="w-4 h-4 stroke-[3]" />}
                        {cfg.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Ghi chú */}
              <div className="space-y-1.5">
                <label className="text-xs font-heading font-bold uppercase tracking-wider block text-ink flex items-center gap-1.5">
                  <MessageSquare className="w-3.5 h-3.5" />
                  Ghi chú cho học sinh (tùy chọn)
                </label>
                <input
                  type="text"
                  placeholder="Ví dụ: Đi muộn 15 phút vì kẹt xe, chuẩn bị bài tốt..."
                  value={items[0].note}
                  onChange={(e) => handleNoteChange(items[0].studentId, e.target.value)}
                  className="w-full px-3 py-2 bg-paper border-2 border-ink text-sm font-body focus:outline-hidden focus:ring-2 focus:ring-ink"
                />
              </div>
            </div>
          ) : (
            /* ================= GIAO DIỆN LỚP NHÓM (DANH SÁCH BẢNG) ================= */
            <div className="space-y-3">
              {items.map((item, index) => (
                <div
                  key={item.studentId}
                  className="bg-cream border-2 border-ink p-3.5 brut-box flex flex-col md:flex-row md:items-center justify-between gap-3 hover:bg-cream/90 transition-colors"
                >
                  {/* Học sinh info */}
                  <div className="min-w-[180px] space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 bg-paper border border-ink text-[10px] font-bold flex items-center justify-center shrink-0">
                        {index + 1}
                      </span>
                      <span className="font-heading font-bold text-sm text-ink truncate">
                        {item.studentName}
                      </span>
                    </div>
                    {item.studentEmail && (
                      <p className="text-[11px] text-ink/70 font-mono truncate pl-7">
                        {item.studentEmail}
                      </p>
                    )}
                  </div>

                  {/* 4 Nút trạng thái */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5 shrink-0">
                    {(Object.keys(STATUS_CONFIG) as AttendanceStatus[]).map((statusKey) => {
                      const cfg = STATUS_CONFIG[statusKey];
                      const isSelected = item.status === statusKey;
                      return (
                        <button
                          key={statusKey}
                          type="button"
                          onClick={() => handleStatusChange(item.studentId, statusKey)}
                          className={`py-1.5 px-2.5 border-2 text-[11px] font-heading uppercase tracking-wide transition-all flex items-center justify-center gap-1 ${
                            isSelected ? cfg.activeClass : cfg.inactiveClass
                          }`}
                        >
                          {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                          {cfg.label}
                        </button>
                      );
                    })}
                  </div>

                  {/* Ô ghi chú */}
                  <div className="md:w-56 shrink-0">
                    <input
                      type="text"
                      placeholder="Ghi chú..."
                      value={item.note}
                      onChange={(e) => handleNoteChange(item.studentId, e.target.value)}
                      className="w-full px-2.5 py-1 bg-paper border border-ink text-xs font-body focus:outline-hidden focus:border-ink focus:ring-1 focus:ring-ink"
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t-3 border-ink shrink-0">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={bulkMutation.isPending}
          >
            Hủy
          </Button>

          <Button
            type="button"
            variant="primary"
            onClick={handleSave}
            disabled={bulkMutation.isPending || items.length === 0}
            className="flex items-center gap-2"
          >
            <Save className="w-4 h-4" />
            {bulkMutation.isPending ? 'Đang lưu…' : 'Lưu điểm danh'}
          </Button>
        </div>
      </div>
    </div>
  );
};
