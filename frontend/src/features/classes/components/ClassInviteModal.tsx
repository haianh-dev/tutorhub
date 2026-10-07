import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Share2, Copy, Check, X, Link as LinkIcon, UserPlus } from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { useCreateInvitationMutation } from '../api';
import { extractClassErrorMessage } from '../errorMessages';
import {
  createClassInvitationSchema,
  type CreateClassInvitationFormValues,
} from '../schemas';
import type { Enrollment, InvitationResponse } from '../../../types';

interface ClassInviteModalProps {
  isOpen: boolean;
  onClose: () => void;
  classId: number;
  className: string;
  activeStudents?: Enrollment[];
}

export const ClassInviteModal: React.FC<ClassInviteModalProps> = ({
  isOpen,
  onClose,
  classId,
  className,
  activeStudents = [],
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [invitationResult, setInvitationResult] =
    useState<InvitationResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<CreateClassInvitationFormValues>({
    resolver: zodResolver(createClassInvitationSchema),
    defaultValues: {
      role: 'STUDENT',
      email: '',
      studentId: undefined,
    },
  });

  const selectedRole = watch('role');

  const inviteMutation = useCreateInvitationMutation({
    onSuccess: (data) => {
      setInvitationResult(data);
      setErrorMessage(null);
    },
    onError: (err) => {
      setErrorMessage(extractClassErrorMessage(err));
    },
  });

  if (!isOpen) return null;

  const onSubmit = handleSubmit((data) => {
    setErrorMessage(null);
    inviteMutation.mutate({
      role: data.role,
      classId,
      email: data.email?.trim() ? data.email.trim() : undefined,
      studentId:
        data.role === 'PARENT' && data.studentId ? Number(data.studentId) : undefined,
    });
  });

  const handleCopyLink = () => {
    if (!invitationResult?.link) return;
    navigator.clipboard.writeText(invitationResult.link).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    });
  };

  const handleResetAndClose = () => {
    reset();
    setInvitationResult(null);
    setErrorMessage(null);
    setCopied(false);
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
            <div className="w-8 h-8 bg-blue text-white border-2 border-ink flex items-center justify-center font-heading font-black text-sm">
              <Share2 className="w-4 h-4" />
            </div>
            <div>
              <h2 className="font-heading font-black text-xl uppercase tracking-wide">
                Mời tham gia lớp học
              </h2>
              <p className="font-body text-xs text-ink/70">
                Lớp: <strong className="font-heading text-ink">{className}</strong>
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleResetAndClose}
            className="p-1 hover:bg-cream border-2 border-transparent hover:border-ink transition-colors"
            title="Đóng"
          >
            <X className="w-5 h-5 text-ink" />
          </button>
        </div>

        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}

        {invitationResult ? (
          /* Đã tạo thành công link */
          <div className="space-y-5">
            <Alert variant="info" title="Tạo link mời thành công!">
              Gửi link này cho{' '}
              {invitationResult.role === 'STUDENT' ? 'học sinh' : 'phụ huynh'}{' '}
              qua Zalo hoặc tin nhắn SMS để họ đăng ký tài khoản và tự động vào
              lớp. Link có hạn 7 ngày.
            </Alert>

            <div>
              <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1.5">
                Link tham gia
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  readOnly
                  value={invitationResult.link}
                  className="w-full border-3 border-ink bg-cream p-2.5 text-xs font-mono text-ink select-all focus:outline-none"
                />
                <Button
                  type="button"
                  variant={copied ? 'secondary' : 'primary'}
                  onClick={handleCopyLink}
                  className="shrink-0"
                >
                  {copied ? (
                    <>
                      <Check className="w-4 h-4 text-green-700" />
                      Đã chép
                    </>
                  ) : (
                    <>
                      <Copy className="w-4 h-4" />
                      Sao chép
                    </>
                  )}
                </Button>
              </div>
            </div>

            <div className="p-3 border-2 border-ink bg-paper text-xs font-body text-ink/80 space-y-1">
              <div>
                • Vai trò nhận:{' '}
                <strong className="font-heading">
                  {invitationResult.role === 'STUDENT' ? 'Học sinh' : 'Phụ huynh'}
                </strong>
              </div>
              {invitationResult.email && (
                <div>
                  • Email gợi ý:{' '}
                  <span className="font-mono">{invitationResult.email}</span>
                </div>
              )}
              <div>
                • Hết hạn lúc:{' '}
                <span className="font-mono">
                  {new Date(invitationResult.expiresAt).toLocaleString('vi-VN')}
                </span>
              </div>
            </div>

            <div className="pt-2 flex justify-between gap-3 border-t-3 border-ink">
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  setInvitationResult(null);
                  reset();
                }}
              >
                Tạo lời mời khác
              </Button>
              <Button type="button" variant="primary" onClick={handleResetAndClose}>
                Hoàn tất
              </Button>
            </div>
          </div>
        ) : (
          /* Form tạo lời mời */
          <form onSubmit={onSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-2">
                Đối tượng nhận lời mời <span className="text-coral">*</span>
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setValue('role', 'STUDENT');
                    setValue('studentId', undefined);
                  }}
                  className={`p-3 border-3 border-ink text-left font-heading font-bold flex flex-col gap-1 transition-all ${
                    selectedRole === 'STUDENT'
                      ? 'bg-yellow brut-box translate-x-0.5 translate-y-0.5'
                      : 'bg-paper hover:bg-cream'
                  }`}
                >
                  <div className="flex items-center gap-1.5 text-xs uppercase">
                    <UserPlus className="w-4 h-4" />
                    Học sinh
                  </div>
                  <div className="font-body text-xs font-normal text-ink/80">
                    Sẽ được tự động ghi danh vào lớp sau khi hoàn tất.
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setValue('role', 'PARENT')}
                  className={`p-3 border-3 border-ink text-left font-heading font-bold flex flex-col gap-1 transition-all ${
                    selectedRole === 'PARENT'
                      ? 'bg-blue text-white brut-box translate-x-0.5 translate-y-0.5'
                      : 'bg-paper hover:bg-cream'
                  }`}
                >
                  <div className="flex items-center gap-1.5 text-xs uppercase">
                    <LinkIcon className="w-4 h-4" />
                    Phụ huynh
                  </div>
                  <div className="font-body text-xs font-normal opacity-90">
                    Cần liên kết với học sinh trong lớp.
                  </div>
                </button>
              </div>
            </div>

            {selectedRole === 'PARENT' && (
              <div>
                <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1.5">
                  Chọn học sinh con <span className="text-coral">*</span>
                </label>
                {activeStudents.length === 0 ? (
                  <div className="p-3 border-2 border-coral bg-cream text-xs font-body text-coral">
                    Lớp chưa có học sinh nào. Bạn cần ghi danh học sinh trước khi mời phụ huynh của học sinh đó.
                  </div>
                ) : (
                  <select
                    className="w-full border-3 border-ink bg-paper p-3 text-sm font-heading font-bold text-ink focus:outline-none focus:ring-0 focus:bg-cream"
                    {...register('studentId', { valueAsNumber: true })}
                  >
                    <option value="">-- Chọn học sinh trong lớp --</option>
                    {activeStudents.map((st) => (
                      <option key={st.studentId} value={st.studentId}>
                        {st.studentName} ({st.studentEmail}) - Mã #{st.studentId}
                      </option>
                    ))}
                  </select>
                )}
                {errors.studentId?.message && (
                  <p className="mt-1 text-xs font-body font-medium text-coral">
                    {errors.studentId.message}
                  </p>
                )}
              </div>
            )}

            <Input
              label="Email người nhận (tùy chọn)"
              placeholder="hocsinh@example.com (để gợi ý khi mở form)"
              {...register('email')}
              error={errors.email?.message}
            />

            <div className="pt-2 flex justify-end gap-3 border-t-3 border-ink">
              <Button
                type="button"
                variant="secondary"
                onClick={handleResetAndClose}
                disabled={inviteMutation.isPending}
              >
                Hủy
              </Button>
              <Button
                type="submit"
                variant="primary"
                isLoading={inviteMutation.isPending}
                loadingText="ĐANG SINH LINK…"
                disabled={selectedRole === 'PARENT' && activeStudents.length === 0}
              >
                <Share2 className="w-4 h-4" />
                Tạo link mời
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
