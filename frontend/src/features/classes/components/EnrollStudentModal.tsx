import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { UserPlus, X } from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { useEnrollStudentMutation } from '../api';
import { extractClassErrorMessage } from '../errorMessages';
import {
  enrollStudentSchema,
  type EnrollStudentFormValues,
} from '../schemas';

interface EnrollStudentModalProps {
  isOpen: boolean;
  onClose: () => void;
  classId: number;
  className: string;
  isOneOnOne: boolean;
  hasActiveStudent: boolean;
}

export const EnrollStudentModal: React.FC<EnrollStudentModalProps> = ({
  isOpen,
  onClose,
  classId,
  className,
  isOneOnOne,
  hasActiveStudent,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EnrollStudentFormValues>({
    resolver: zodResolver(enrollStudentSchema),
    defaultValues: {
      studentId: undefined,
    },
  });

  const enrollMutation = useEnrollStudentMutation({
    onSuccess: () => {
      reset();
      setErrorMessage(null);
      onClose();
    },
    onError: (err) => {
      setErrorMessage(extractClassErrorMessage(err));
    },
  });

  if (!isOpen) return null;

  const onSubmit = handleSubmit((data) => {
    setErrorMessage(null);
    enrollMutation.mutate({
      classId,
      req: { studentId: data.studentId },
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
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/60"
    >
      <div className="w-full max-w-md border-3 border-ink bg-paper brut-box p-6 sm:p-8 space-y-6">
        <div className="flex items-center justify-between border-b-3 border-ink pb-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-yellow border-2 border-ink flex items-center justify-center font-heading font-black text-sm">
              <UserPlus className="w-4 h-4" />
            </div>
            <div>
              <h2 className="font-heading font-black text-xl uppercase tracking-wide">
                Ghi danh học sinh
              </h2>
              <p className="font-body text-xs text-ink/70">
                Lớp: <strong className="font-heading text-ink">{className}</strong>
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="p-1 hover:bg-cream border-2 border-transparent hover:border-ink transition-colors"
            title="Đóng"
          >
            <X className="w-5 h-5 text-ink" />
          </button>
        </div>

        {isOneOnOne && hasActiveStudent && (
          <Alert
            variant="warning"
            title="Lớp 1:1 đã đủ học sinh"
          >
            Lớp học 1:1 chỉ được có tối đa 1 học sinh theo học. Hãy xóa học sinh
            hiện tại trước khi thêm học sinh mới, hoặc đổi loại lớp sang lớp Nhóm.
          </Alert>
        )}

        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}

        <form onSubmit={onSubmit} className="space-y-4">
          <Input
            label="Mã ID học sinh (User ID)"
            type="number"
            placeholder="Ví dụ: 2"
            required
            helperText="Nhập ID tài khoản có vai trò STUDENT đã có trong hệ thống."
            {...register('studentId', { valueAsNumber: true })}
            error={errors.studentId?.message}
          />

          <div className="pt-2 flex justify-end gap-3 border-t-3 border-ink">
            <Button
              type="button"
              variant="secondary"
              onClick={handleClose}
              disabled={enrollMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={enrollMutation.isPending}
              loadingText="ĐANG GHI DANH…"
              disabled={isOneOnOne && hasActiveStudent}
            >
              <UserPlus className="w-4 h-4" />
              Ghi danh ngay
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
