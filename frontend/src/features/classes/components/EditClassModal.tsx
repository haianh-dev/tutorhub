import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Edit2, X, Users, UserCheck } from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { useUpdateClassMutation } from '../api';
import { extractClassErrorMessage } from '../errorMessages';
import {
  updateClassSchema,
  type UpdateClassFormValues,
} from '../schemas';
import type { Classroom } from '../../../types';

interface EditClassModalProps {
  isOpen: boolean;
  onClose: () => void;
  classroom: Classroom;
}

export const EditClassModal: React.FC<EditClassModalProps> = ({
  isOpen,
  onClose,
  classroom,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<UpdateClassFormValues>({
    resolver: zodResolver(updateClassSchema),
    defaultValues: {
      name: classroom.name,
      subject: classroom.subject,
      classType: classroom.classType,
      description: classroom.description ?? '',
    },
  });

  useEffect(() => {
    if (isOpen) {
      reset({
        name: classroom.name,
        subject: classroom.subject,
        classType: classroom.classType,
        description: classroom.description ?? '',
      });
      setErrorMessage(null);
    }
  }, [isOpen, classroom, reset]);

  const selectedType = watch('classType');
  const activeStudentCount = classroom.studentCount ?? 0;

  const updateMutation = useUpdateClassMutation({
    onSuccess: () => {
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
    updateMutation.mutate({
      id: classroom.id,
      data: {
        name: data.name,
        subject: data.subject,
        classType: data.classType,
        description: data.description?.trim() ? data.description.trim() : '',
      },
    });
  });

  const handleClose = () => {
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
            <div className="w-8 h-8 bg-paper border-2 border-ink flex items-center justify-center font-heading font-black text-sm">
              <Edit2 className="w-4 h-4" />
            </div>
            <h2 className="font-heading font-black text-xl uppercase tracking-wide">
              Chỉnh sửa thông tin lớp
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
            required
            {...register('name')}
            error={errors.name?.message}
          />

          <Input
            label="Môn học"
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
                  Lớp 1:1
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
            {activeStudentCount > 1 && selectedType === 'ONE_ON_ONE' && (
              <p className="mt-2 text-xs font-body font-medium text-coral bg-coral/10 p-2 border-2 border-coral">
                Lớp đang có {activeStudentCount} học sinh. Bạn không thể chuyển sang
                lớp 1:1 trừ khi giảm sĩ số xuống còn tối đa 1 học sinh.
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1.5">
              Mô tả / Ghi chú
            </label>
            <textarea
              rows={3}
              className="w-full border-3 border-ink bg-paper p-3 text-sm font-body text-ink placeholder:text-ink/40 focus:outline-none focus:ring-0 focus:bg-cream"
              {...register('description')}
            />
          </div>

          <div className="pt-2 flex justify-end gap-3 border-t-3 border-ink">
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
              loadingText="ĐANG LƯU…"
            >
              Lưu thay đổi
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
