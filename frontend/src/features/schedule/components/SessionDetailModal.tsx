import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, Calendar, Clock, CheckCircle2, Ban, Edit3, AlertCircle, AlertTriangle } from 'lucide-react';
import { Button } from '../../../components/ui/Button';
import { Badge } from '../../../components/ui/Badge';
import { Alert } from '../../../components/ui/Alert';
import { useUpdateSession, useCompleteSession, useCancelSession } from '../api';
import { extractScheduleError } from '../errorMessages';
import type { Session } from '../../../types';

interface SessionDetailModalProps {
  session: Session | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

interface EditFormValues {
  date: string;
  startTime: string;
  endTime: string;
  topic: string;
  note: string;
}

export const SessionDetailModal: React.FC<SessionDetailModalProps> = ({
  session,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [conflictingId, setConflictingId] = useState<number | undefined>();

  // Format dates/times
  const startDate = session ? new Date(session.startAt) : new Date();
  const endDate = session ? new Date(session.endAt) : new Date();

  const pad = (n: number) => n.toString().padStart(2, '0');
  const dateStr = `${startDate.getUTCFullYear()}-${pad(startDate.getUTCMonth() + 1)}-${pad(startDate.getUTCDate())}`;
  const startTimeStr = `${pad(startDate.getUTCHours())}:${pad(startDate.getUTCMinutes())}`;
  const endTimeStr = `${pad(endDate.getUTCHours())}:${pad(endDate.getUTCMinutes())}`;

  const {
    register,
    handleSubmit,
    reset,
  } = useForm<EditFormValues>({
    values: {
      date: dateStr,
      startTime: startTimeStr,
      endTime: endTimeStr,
      topic: session?.topic || '',
      note: session?.note || '',
    },
  });

  const updateMutation = useUpdateSession({
    onSuccess: (data) => {
      if (data.warnings && data.warnings.length > 0) {
        setWarnings(data.warnings);
      }
      setIsEditing(false);
      setErrorMessage(null);
      onSuccess?.();
    },
    onError: (err) => {
      const parsed = extractScheduleError(err);
      setErrorMessage(parsed.message);
      setConflictingId(parsed.conflictingSessionId);
    },
  });

  const completeMutation = useCompleteSession({
    onSuccess: () => {
      onSuccess?.();
      handleClose();
    },
    onError: (err) => {
      setErrorMessage(extractScheduleError(err).message);
    },
  });

  const cancelMutation = useCancelSession({
    onSuccess: () => {
      onSuccess?.();
      handleClose();
    },
    onError: (err) => {
      setErrorMessage(extractScheduleError(err).message);
    },
  });

  const handleClose = () => {
    setIsEditing(false);
    setErrorMessage(null);
    setWarnings([]);
    setConflictingId(undefined);
    reset();
    onClose();
  };

  const onSave = (values: EditFormValues) => {
    if (!session) return;
    setErrorMessage(null);
    setWarnings([]);

    const startIso = new Date(`${values.date}T${values.startTime}:00Z`).toISOString();
    const endIso = new Date(`${values.date}T${values.endTime}:00Z`).toISOString();

    updateMutation.mutate({
      id: session.id,
      req: {
        startAt: startIso,
        endAt: endIso,
        topic: values.topic.trim() || undefined,
        note: values.note.trim() || undefined,
      },
    });
  };

  if (!isOpen || !session) return null;

  const isCancelled = session.status === 'CANCELLED';
  const isCompleted = session.status === 'COMPLETED';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/60 backdrop-blur-xs font-body">
      <div className="w-full max-w-lg bg-paper border-3 border-ink brut-shadow-lg p-6 relative animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 mb-4 border-b-2 border-ink">
          <div className="flex items-center gap-2">
            <span className="font-heading font-black text-lg uppercase tracking-wider text-ink">
              Buổi học #{session.id}
            </span>
            <Badge
              variant={
                session.status === 'COMPLETED'
                  ? 'blue'
                  : session.status === 'CANCELLED'
                  ? 'paper'
                  : 'yellow'
              }
            >
              {session.status === 'COMPLETED'
                ? 'ĐÃ HOÀN THÀNH'
                : session.status === 'CANCELLED'
                ? 'ĐÃ HỦY'
                : 'SẮP DIỄN RA'}
            </Badge>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="p-1 border-2 border-ink bg-cream hover:bg-yellow transition-colors cursor-pointer"
          >
            <X className="w-5 h-5 text-ink" />
          </button>
        </div>

        {/* Lỗi trùng lịch (409) */}
        {errorMessage && (
          <div className="mb-4">
            <Alert variant="error">
              <div className="flex items-start gap-2">
                <AlertCircle className="w-5 h-5 shrink-0 mt-0.5 text-coral" />
                <div>
                  <p className="font-bold uppercase tracking-wide text-xs">Xung đột lịch!</p>
                  <p className="text-sm mt-0.5">{errorMessage}</p>
                  {conflictingId && (
                    <p className="text-xs mt-1 text-ink/80 font-mono">
                      Mã buổi học bị trùng: #{conflictingId}
                    </p>
                  )}
                </div>
              </div>
            </Alert>
          </div>
        )}

        {/* Warnings */}
        {warnings.length > 0 && (
          <div className="mb-4">
            <Alert variant="warning">
              <div className="flex items-start gap-2">
                <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5 text-ink" />
                <div>
                  <p className="font-bold uppercase tracking-wide text-xs">Cảnh báo trùng lịch học sinh:</p>
                  <ul className="text-xs list-disc list-inside mt-1 space-y-0.5">
                    {warnings.map((w, idx) => (
                      <li key={idx}>{w}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </Alert>
          </div>
        )}

        {/* Body */}
        {!isEditing ? (
          <div className="space-y-4">
            <div className="p-3 border-2 border-ink bg-cream/70">
              <p className="text-xs font-heading font-bold uppercase tracking-wider text-ink/70">
                Lớp học:
              </p>
              <p className="text-base font-black text-ink">
                {session.className || `Lớp #${session.classId}`}
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="p-3 border-2 border-ink bg-paper">
                <p className="text-xs font-heading font-bold uppercase tracking-wider text-ink/70 flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5" /> Ngày:
                </p>
                <p className="text-sm font-bold text-ink mt-0.5">{dateStr}</p>
              </div>

              <div className="p-3 border-2 border-ink bg-paper">
                <p className="text-xs font-heading font-bold uppercase tracking-wider text-ink/70 flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" /> Khung giờ:
                </p>
                <p className="text-sm font-bold text-ink mt-0.5">
                  {startTimeStr} - {endTimeStr} UTC
                </p>
              </div>
            </div>

            <div>
              <p className="text-xs font-heading font-bold uppercase tracking-wider text-ink/70 mb-1">
                Chủ đề:
              </p>
              <div className="p-3 border-2 border-ink bg-paper text-sm">
                {session.topic || <span className="italic text-ink/50">Chưa nhập chủ đề</span>}
              </div>
            </div>

            {session.note && (
              <div>
                <p className="text-xs font-heading font-bold uppercase tracking-wider text-ink/70 mb-1">
                  Ghi chú:
                </p>
                <div className="p-3 border-2 border-ink bg-paper text-sm text-ink/90 whitespace-pre-wrap">
                  {session.note}
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="pt-4 border-t-2 border-ink flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                {!isCancelled && !isCompleted && (
                  <>
                    <Button
                      type="button"
                      variant="primary"
                      onClick={() => completeMutation.mutate(session.id)}
                      disabled={completeMutation.isPending}
                    >
                      <CheckCircle2 className="w-4 h-4 mr-1 inline" />
                      Hoàn thành
                    </Button>
                    <Button
                      type="button"
                      variant="danger"
                      onClick={() => cancelMutation.mutate(session.id)}
                      disabled={cancelMutation.isPending}
                    >
                      <Ban className="w-4 h-4 mr-1 inline" />
                      Hủy buổi
                    </Button>
                  </>
                )}
              </div>

              <div className="flex items-center gap-2">
                {!isCancelled && (
                  <Button type="button" variant="secondary" onClick={() => setIsEditing(true)}>
                    <Edit3 className="w-4 h-4 mr-1 inline" /> Sửa
                  </Button>
                )}
                <Button type="button" variant="secondary" onClick={handleClose}>
                  Đóng
                </Button>
              </div>
            </div>
          </div>
        ) : (
          /* Form sửa giờ/chủ đề */
          <form onSubmit={handleSubmit(onSave)} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                  Ngày <span className="text-coral">*</span>
                </label>
                <input
                  type="date"
                  {...register('date', { required: true })}
                  className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper"
                />
              </div>

              <div>
                <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                  Bắt đầu <span className="text-coral">*</span>
                </label>
                <input
                  type="time"
                  {...register('startTime', { required: true })}
                  className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper"
                />
              </div>

              <div>
                <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                  Kết thúc <span className="text-coral">*</span>
                </label>
                <input
                  type="time"
                  {...register('endTime', { required: true })}
                  className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                Chủ đề
              </label>
              <input
                type="text"
                {...register('topic')}
                className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper"
              />
            </div>

            <div>
              <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                Ghi chú
              </label>
              <textarea
                rows={2}
                {...register('note')}
                className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper resize-none"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t-2 border-ink">
              <Button type="button" variant="secondary" onClick={() => setIsEditing(false)}>
                Hủy sửa
              </Button>
              <Button type="submit" variant="primary" disabled={updateMutation.isPending}>
                {updateMutation.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
