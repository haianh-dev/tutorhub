import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Plus, X, Users, UserCheck } from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { useCreateClassMutation } from '../api';
import { extractClassErrorMessage } from '../errorMessages';
import {
  createClassSchema,
  type CreateClassFormValues,
} from '../schemas';

interface CreateClassModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const CreateClassModal: React.FC<CreateClassModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<CreateClassFormValues>({
    resolver: zodResolver(createClassSchema),
    defaultValues: {
      name: '',
      subject: '',
      classType: 'ONE_ON_ONE',
      description: '',
    },
  });

  const selectedType = watch('classType');

  const createMutation = useCreateClassMutation({
    onSuccess: () => {
      reset();
      setErrorMessage(null);
      onSuccess?.();
      onClose();
    },
    onError: (err) => {
      setErrorMessage(extractClassErrorMessage(err));
    },
  });

  if (!isOpen) return null;

  const onSubmit = handleSubmit((data) => {
    setErrorMessage(null);
    createMutation.mutate({
      name: data.name,
      subject: data.subject,
      classType: data.classType,
      description: data.description?.trim() ? data.description.trim() : undefined,
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
      <div className="w-full max-w-lg border-3 border-ink bg-paper brut-box p-6 sm:p-8 space-y-6">
        <div className="flex items-center justify-between border-b-3 border-ink pb-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-yellow border-2 border-ink flex items-center justify-center font-heading font-black text-sm">
              +
            </div>
            <h2 className="font-heading font-black text-xl uppercase tracking-wide">
              Tạo lớp học mới
            </h2>
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

        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}

        <form onSubmit={onSubmit} className="space-y-4">
          <Input
            label="Tên lớp học"
            placeholder="Ví dụ: Toán 12 - Luyện thi ĐH A1"
            required
            {...register('name')}
            error={errors.name?.message}
          />

          <Input
            label="Môn học"
            placeholder="Ví dụ: Toán học, Tiếng Anh, Vật lý..."
            required
            {...register('subject')}
            error={errors.subject?.message}
          />

          <div>
            <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-2">
              Loại lớp học <span className="text-coral">*</span>
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setValue('classType', 'ONE_ON_ONE')}
                className={`p-3 border-3 border-ink text-left font-heading font-bold flex flex-col gap-1 transition-all ${
                  selectedType === 'ONE_ON_ONE'
                    ? 'bg-yellow brut-box translate-x-0.5 translate-y-0.5'
                    : 'bg-paper hover:bg-cream'
                }`}
              >
                <div className="flex items-center gap-1.5 text-xs uppercase">
                  <UserCheck className="w-4 h-4" />
                  Lớp 1:1 (Kèm riêng)
                </div>
                <div className="font-body text-xs text-ink/80 font-normal">
                  Tối đa 1 học sinh theo học.
                </div>
              </button>

              <button
                type="button"
                onClick={() => setValue('classType', 'GROUP')}
                className={`p-3 border-3 border-ink text-left font-heading font-bold flex flex-col gap-1 transition-all ${
                  selectedType === 'GROUP'
                    ? 'bg-blue text-white brut-box translate-x-0.5 translate-y-0.5'
                    : 'bg-paper hover:bg-cream'
                }`}
              >
                <div className="flex items-center gap-1.5 text-xs uppercase">
                  <Users className="w-4 h-4" />
                  Lớp Nhóm
                </div>
                <div className="font-body text-xs font-normal opacity-90">
                  Từ 2 học sinh trở lên.
                </div>
              </button>
            </div>
            {errors.classType?.message && (
              <p className="mt-1 text-xs font-body font-medium text-coral">
                {errors.classType.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1.5">
              Mô tả / Ghi chú (tùy chọn)
            </label>
            <textarea
              rows={3}
              placeholder="Ghi chú về mục tiêu, tài liệu hoặc thời gian học..."
              className="w-full border-3 border-ink bg-paper p-3 text-sm font-body text-ink placeholder:text-ink/40 focus:outline-none focus:ring-0 focus:bg-cream"
              {...register('description')}
            />
            {errors.description?.message && (
              <p className="mt-1 text-xs font-body font-medium text-coral">
                {errors.description.message}
              </p>
            )}
          </div>

          <div className="pt-2 flex justify-end gap-3 border-t-3 border-ink">
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
              loadingText="ĐANG TẠO LỚP…"
            >
              <Plus className="w-4 h-4" />
              Tạo lớp học
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
