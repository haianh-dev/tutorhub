import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, Calendar, Clock, AlertTriangle, AlertCircle } from 'lucide-react';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { useCreateSession } from '../api';
import { extractScheduleError } from '../errorMessages';
import type { Classroom } from '../../../types';

interface CreateSessionModalProps {
  isOpen: boolean;
  onClose: () => void;
  classes: Classroom[];
  initialClassId?: number;
  initialDate?: string; // YYYY-MM-DD
  onSuccess?: () => void;
}

interface FormValues {
  classId: number;
  date: string;
  startTime: string; // HH:mm
  endTime: string; // HH:mm
  topic: string;
  note: string;
}

export const CreateSessionModal: React.FC<CreateSessionModalProps> = ({
  isOpen,
  onClose,
  classes,
  initialClassId,
  initialDate,
  onSuccess,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [conflictingId, setConflictingId] = useState<number | undefined>();

  const defaultDate = useMemo(() => initialDate || new Date().toISOString().split('T')[0], [initialDate]);
  const defaultClassId = initialClassId || (classes[0]?.id ?? 0);

  const {
    register,
    handleSubmit,
    reset,
  } = useForm<FormValues>({
    defaultValues: {
      classId: defaultClassId,
      date: defaultDate,
      startTime: '08:00',
      endTime: '09:30',
      topic: '',
      note: '',
    },
  });

  const createMutation = useCreateSession({
    onSuccess: (data) => {
      if (data.warnings && data.warnings.length > 0) {
        setWarnings(data.warnings);
      } else {
        handleClose();
        onSuccess?.();
      }
    },
    onError: (err) => {
      const parsed = extractScheduleError(err);
      setErrorMessage(parsed.message);
      setConflictingId(parsed.conflictingSessionId);
    },
  });

  const handleClose = () => {
    reset();
    setErrorMessage(null);
    setWarnings([]);
    setConflictingId(undefined);
    onClose();
  };

  const onSubmit = (values: FormValues) => {
    setErrorMessage(null);
    setWarnings([]);

    // Chuyển date và time thành ISO-8601 UTC
    const startIso = new Date(`${values.date}T${values.startTime}:00Z`).toISOString();
    const endIso = new Date(`${values.date}T${values.endTime}:00Z`).toISOString();

    createMutation.mutate({
      classId: Number(values.classId),
      req: {
        startAt: startIso,
        endAt: endIso,
        topic: values.topic.trim() || undefined,
        note: values.note.trim() || undefined,
      },
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/60 backdrop-blur-xs font-body">
      <div className="w-full max-w-lg bg-paper border-3 border-ink brut-shadow-lg p-6 relative animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 mb-4 border-b-2 border-ink">
          <div className="flex items-center gap-2">
            <Calendar className="w-6 h-6 text-ink" />
            <h2 className="font-heading font-black text-xl uppercase tracking-wider text-ink">
              Tạo buổi học mới
            </h2>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="p-1 border-2 border-ink bg-cream hover:bg-yellow transition-colors cursor-pointer"
          >
            <X className="w-5 h-5 text-ink" />
          </button>
        </div>

        {/* Lỗi trùng lịch gia sư (409 SESSION_CONFLICT) */}
        {errorMessage && (
          <div className="mb-4">
            <Alert variant="error">
              <div className="flex items-start gap-2">
                <AlertCircle className="w-5 h-5 shrink-0 mt-0.5 text-coral" />
                <div>
                  <p className="font-bold uppercase tracking-wide text-xs">Xung đột lịch dạy!</p>
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

        {/* Cảnh báo trùng lịch học sinh (FR-3.4 Warnings) */}
        {warnings.length > 0 && (
          <div className="mb-4">
            <Alert variant="warning">
              <div className="flex items-start gap-2">
                <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5 text-ink" />
                <div>
                  <p className="font-bold uppercase tracking-wide text-xs">Cảnh báo học sinh trùng lịch:</p>
                  <ul className="text-xs list-disc list-inside mt-1 space-y-0.5">
                    {warnings.map((w, idx) => (
                      <li key={idx}>{w}</li>
                    ))}
                  </ul>
                  <p className="text-xs mt-2 text-ink/80 italic">
                    (Buổi học vẫn đã được tạo thành công vào hệ thống. Bạn có thể đóng cửa sổ này.)
                  </p>
                </div>
              </div>
            </Alert>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
              Chọn lớp học <span className="text-coral">*</span>
            </label>
            <select
              {...register('classId', { required: true, valueAsNumber: true })}
              className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm font-bold focus:outline-none focus:bg-paper"
            >
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.subject})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                Ngày học <span className="text-coral">*</span>
              </label>
              <input
                type="date"
                {...register('date', { required: true })}
                className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper"
              />
            </div>

            <div>
              <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" /> Bắt đầu <span className="text-coral">*</span>
              </label>
              <input
                type="time"
                {...register('startTime', { required: true })}
                className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper"
              />
            </div>

            <div>
              <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" /> Kết thúc <span className="text-coral">*</span>
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
              Chủ đề buổi học
            </label>
            <input
              type="text"
              placeholder="VD: Hàm số bậc hai, Ôn tập giữa kỳ..."
              {...register('topic')}
              className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper"
            />
          </div>

          <div>
            <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
              Ghi chú thêm
            </label>
            <textarea
              rows={2}
              placeholder="Chuẩn bị bài tập trang 45..."
              {...register('note')}
              className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none focus:bg-paper resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t-2 border-ink">
            <Button type="button" variant="secondary" onClick={handleClose}>
              {warnings.length > 0 ? 'Đóng' : 'Hủy'}
            </Button>
            {warnings.length === 0 && (
              <Button type="submit" variant="primary" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Đang tạo...' : 'Tạo buổi học'}
              </Button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
};
