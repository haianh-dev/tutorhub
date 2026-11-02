import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { BookOpen, X, Calendar, FileText } from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { useCreateAssignment } from '../api';
import { ASSIGNMENT_TYPE_LABELS, type AssignmentType } from '../types';

const createAssignmentSchema = z.object({
  title: z
    .string()
    .min(1, 'Tiêu đề bài tập không được để trống')
    .max(255, 'Tiêu đề không được vượt quá 255 ký tự'),
  type: z.enum(['HOMEWORK', 'QUIZ', 'EXAM', 'MOCK_TEST', 'OTHER']),
  dueAt: z.string().optional(),
  description: z.string().max(2000, 'Mô tả không được vượt quá 2000 ký tự').optional(),
});

type CreateAssignmentFormValues = z.infer<typeof createAssignmentSchema>;

interface CreateAssignmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  classId: number;
  className?: string;
  onSuccess?: () => void;
}

export const CreateAssignmentModal: React.FC<CreateAssignmentModalProps> = ({
  isOpen,
  onClose,
  classId,
  className,
  onSuccess,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateAssignmentFormValues>({
    resolver: zodResolver(createAssignmentSchema),
    defaultValues: {
      title: '',
      type: 'HOMEWORK',
      dueAt: '',
      description: '',
    },
  });

  const createMutation = useCreateAssignment(classId, {
    onSuccess: () => {
      reset();
      setErrorMessage(null);
      onSuccess?.();
      onClose();
    },
    onError: (err) => {
      setErrorMessage(err.message || 'Không thể tạo bài tập. Vui lòng thử lại.');
    },
  });

  if (!isOpen) return null;

  const onSubmit = handleSubmit((data) => {
    setErrorMessage(null);
    createMutation.mutate({
      title: data.title.trim(),
      type: data.type as AssignmentType,
      dueAt: data.dueAt ? new Date(data.dueAt).toISOString() : null,
      description: data.description?.trim() || null,
    });
  });

  const handleClose = () => {
    reset();
    setErrorMessage(null);
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/60 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="relative w-full max-w-lg bg-paper border-3 border-ink brut-box p-6 space-y-6 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-ink pb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-yellow border-2 border-ink brut-box flex items-center justify-center">
              <BookOpen className="w-5 h-5 text-ink" />
            </div>
            <div>
              <h2 className="text-xl font-heading font-black uppercase tracking-wider text-ink">
                Tạo bài tập mới
              </h2>
              {className && (
                <p className="text-xs font-mono font-bold text-ink/70">
                  Lớp: {className}
                </p>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="p-1 text-ink/60 hover:text-ink hover:bg-cream border-2 border-transparent hover:border-ink transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {errorMessage && (
          <Alert variant="error" title="Lỗi khi tạo bài tập">
            {errorMessage}
          </Alert>
        )}

        <form onSubmit={onSubmit} className="space-y-5">
          {/* Tiêu đề */}
          <Input
            label="Tiêu đề bài tập *"
            placeholder="Ví dụ: Bài tập phương trình bậc hai, Kiểm tra 15p..."
            {...register('title')}
            error={errors.title?.message}
          />

          {/* Loại bài tập */}
          <div className="space-y-1.5">
            <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink">
              Loại bài tập *
            </label>
            <select
              {...register('type')}
              className="w-full h-11 px-3 bg-paper border-2 border-ink brut-box font-mono text-sm text-ink focus:outline-none focus:bg-yellow/10"
            >
              {(Object.keys(ASSIGNMENT_TYPE_LABELS) as AssignmentType[]).map((type) => (
                <option key={type} value={type}>
                  {ASSIGNMENT_TYPE_LABELS[type]}
                </option>
              ))}
            </select>
          </div>

          {/* Hạn nộp */}
          <div className="space-y-1.5">
            <label className="flex items-center gap-1.5 text-xs font-heading font-bold uppercase tracking-wider text-ink">
              <Calendar className="w-4 h-4" /> Hạn nộp (tùy chọn)
            </label>
            <input
              type="datetime-local"
              {...register('dueAt')}
              className="w-full h-11 px-3 bg-paper border-2 border-ink brut-box font-mono text-sm text-ink focus:outline-none focus:bg-yellow/10"
            />
          </div>

          {/* Mô tả */}
          <div className="space-y-1.5">
            <label className="flex items-center gap-1.5 text-xs font-heading font-bold uppercase tracking-wider text-ink">
              <FileText className="w-4 h-4" /> Hướng dẫn / Mô tả bài tập
            </label>
            <textarea
              rows={3}
              placeholder="Yêu cầu làm các bài tập từ trang 25 đến 27 sách bài tập..."
              {...register('description')}
              className="w-full p-3 bg-paper border-2 border-ink brut-box font-mono text-sm text-ink focus:outline-none focus:bg-yellow/10 resize-y"
            />
            {errors.description && (
              <p className="text-xs font-mono font-bold text-coral">
                {errors.description.message}
              </p>
            )}
          </div>

          {/* Thông báo tự động sinh bảng điểm */}
          <div className="p-3 bg-yellow/20 border-2 border-ink/40 text-xs font-mono text-ink/80 leading-relaxed">
            💡 Hệ thống sẽ tự động tạo danh sách điểm số (thang điểm 10 cố định) cho toàn bộ học sinh đang học trong lớp.
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t-2 border-ink/20">
            <Button
              type="button"
              variant="secondary"
              onClick={handleClose}
              disabled={createMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={createMutation.isPending}
            >
              Giao bài tập
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
