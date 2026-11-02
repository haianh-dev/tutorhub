import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Edit3, X, Calendar, FileText } from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { useUpdateAssignment } from '../api';
import {
  ASSIGNMENT_TYPE_LABELS,
  type Assignment,
  type AssignmentType,
} from '../types';

const editAssignmentSchema = z.object({
  title: z
    .string()
    .min(1, 'Tiêu đề bài tập không được để trống')
    .max(255, 'Tiêu đề không được vượt quá 255 ký tự'),
  type: z.enum(['HOMEWORK', 'QUIZ', 'EXAM', 'MOCK_TEST', 'OTHER']),
  dueAt: z.string().optional(),
  description: z.string().max(2000, 'Mô tả không được vượt quá 2000 ký tự').optional(),
});

type EditAssignmentFormValues = z.infer<typeof editAssignmentSchema>;

interface EditAssignmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  assignment: Assignment;
  onSuccess?: () => void;
}

export const EditAssignmentModal: React.FC<EditAssignmentModalProps> = ({
  isOpen,
  onClose,
  assignment,
  onSuccess,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Format dueAt for datetime-local input
  const initialDueAt = assignment.dueAt
    ? new Date(assignment.dueAt).toISOString().slice(0, 16)
    : '';

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditAssignmentFormValues>({
    resolver: zodResolver(editAssignmentSchema),
    defaultValues: {
      title: assignment.title,
      type: assignment.type,
      dueAt: initialDueAt,
      description: assignment.description || '',
    },
  });

  const updateMutation = useUpdateAssignment(assignment.classId, {
    onSuccess: () => {
      setErrorMessage(null);
      onSuccess?.();
      onClose();
    },
    onError: (err) => {
      setErrorMessage(err.message || 'Không thể cập nhật bài tập. Vui lòng thử lại.');
    },
  });

  if (!isOpen) return null;

  const onSubmit = handleSubmit((data) => {
    setErrorMessage(null);
    updateMutation.mutate({
      id: assignment.id,
      data: {
        title: data.title.trim(),
        type: data.type as AssignmentType,
        dueAt: data.dueAt ? new Date(data.dueAt).toISOString() : null,
        description: data.description?.trim() || null,
      },
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
              <Edit3 className="w-5 h-5 text-ink" />
            </div>
            <div>
              <h2 className="text-xl font-heading font-black uppercase tracking-wider text-ink">
                Sửa bài tập
              </h2>
              <p className="text-xs font-mono font-bold text-ink/70">
                Lớp: {assignment.className}
              </p>
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
          <Alert variant="error" title="Lỗi khi sửa bài tập">
            {errorMessage}
          </Alert>
        )}

        <form onSubmit={onSubmit} className="space-y-5">
          {/* Tiêu đề */}
          <Input
            label="Tiêu đề bài tập *"
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
              <Calendar className="w-4 h-4" /> Hạn nộp
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
              <FileText className="w-4 h-4" /> Mô tả bài tập
            </label>
            <textarea
              rows={3}
              {...register('description')}
              className="w-full p-3 bg-paper border-2 border-ink brut-box font-mono text-sm text-ink focus:outline-none focus:bg-yellow/10 resize-y"
            />
            {errors.description && (
              <p className="text-xs font-mono font-bold text-coral">
                {errors.description.message}
              </p>
            )}
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t-2 border-ink/20">
            <Button
              type="button"
              variant="secondary"
              onClick={handleClose}
              disabled={updateMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={updateMutation.isPending}
            >
              Lưu thay đổi
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
