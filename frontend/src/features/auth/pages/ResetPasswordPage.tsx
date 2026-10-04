import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Lock,
  KeyRound,
  AlertTriangle,
} from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import AuthCard from '../components/AuthCard';
import EyeToggle from '../components/EyeToggle';
import { useResetPasswordMutation } from '../api';
import { useAuth } from '../AuthContext';
import {
  extractAlertMessage,
  extractFieldErrors,
} from '../errorMessages';
import {
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from '../schemas';

export const ResetPasswordPage: React.FC = () => {
  const [params] = useSearchParams();
  const tokenFromUrl = params.get('token') ?? undefined;
  const navigate = useNavigate();
  const { isAuthenticated, getHomeRoute } = useAuth();
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [submitAlert, setSubmitAlert] = useState<{
    variant: 'info' | 'warning' | 'error';
    message: string;
  } | null>(null);

  // Nếu đã đăng nhập → không ở đây
  useEffect(() => {
    if (isAuthenticated) {
      navigate(getHomeRoute(), { replace: true });
    }
  }, [isAuthenticated, getHomeRoute, navigate]);

  const staticAlert = !tokenFromUrl
    ? {
        variant: 'error' as const,
        message:
          'Thiếu token đặt lại mật khẩu trong URL. Hãy đảm bảo bạn mở đúng link mà gia sư đã gửi.',
      }
    : null;

  const topAlert = submitAlert ?? staticAlert;

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      token: tokenFromUrl ?? '',
      newPassword: '',
      confirmPassword: '',
    },
    mode: 'onBlur',
  });

  useEffect(() => {
    if (tokenFromUrl) {
      reset((f) => ({ ...f, token: tokenFromUrl }));
    }
  }, [tokenFromUrl, reset]);

  const resetMutation = useResetPasswordMutation({
    onSuccess: () => {
      navigate('/login?reset=true', { replace: true });
    },
    onError: (err) => {
      setSubmitAlert({
        variant: 'error',
        message: extractAlertMessage(err),
      });
      const feMap = extractFieldErrors(err);
      if (feMap.newPassword) {
        setError('newPassword', { message: feMap.newPassword });
      }
      if (feMap.token) setError('token', { message: feMap.token });
    },
  });

  const onSubmit = handleSubmit((values) => {
    setSubmitAlert(null);
    resetMutation.mutate({
      token: values.token,
      newPassword: values.newPassword,
    });
  });

  const isDisabled = !tokenFromUrl || resetMutation.isSuccess;

  return (
    <AuthCard
      title="Đặt lại mật khẩu"
      subtitle="Nhập mật khẩu mới cho tài khoản của bạn."
      footer={
        <>
          Đã nhớ mật khẩu?{' '}
          <Link
            to="/login"
            className="font-heading font-bold uppercase text-ink underline decoration-2 underline-offset-4 hover:text-ink/70"
          >
            Đăng nhập
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4" noValidate>
        {topAlert && (
          <Alert variant={topAlert.variant}>
            <div className="flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{topAlert.message}</span>
            </div>
          </Alert>
        )}

        {tokenFromUrl && !resetMutation.isError && (
          <Alert variant="info">
            Liên kết đặt lại mật khẩu đã được xác thực. Hãy nhập mật khẩu mới
            bên dưới.
          </Alert>
        )}

        <div className="flex flex-col gap-1.5 relative">
          <Input
            label="Mật khẩu mới"
            type={showNew ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="Ít nhất 8 ký tự"
            required
            disabled={isDisabled}
            icon={<Lock className="w-4 h-4 shrink-0" />}
            rightAdornment={
              <EyeToggle
                shown={showNew}
                onToggle={() => setShowNew((v) => !v)}
              />
            }
            {...register('newPassword')}
            error={errors.newPassword?.message}
          />
        </div>

        <div className="flex flex-col gap-1.5 relative">
          <Input
            label="Xác nhận mật khẩu mới"
            type={showConfirm ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="Nhập lại mật khẩu mới"
            required
            disabled={isDisabled}
            icon={<KeyRound className="w-4 h-4 shrink-0" />}
            rightAdornment={
              <EyeToggle
                shown={showConfirm}
                onToggle={() => setShowConfirm((v) => !v)}
              />
            }
            {...register('confirmPassword')}
            error={errors.confirmPassword?.message}
          />
        </div>

        <Button
          type="submit"
          variant="primary"
          isLoading={resetMutation.isPending}
          loadingText="ĐANG ĐẶT MẬT KHẨU…"
          className="w-full justify-center"
          disabled={isDisabled}
        >
          <KeyRound className="w-4 h-4" />
          Đặt mật khẩu mới
        </Button>
      </form>
    </AuthCard>
  );
};

export default ResetPasswordPage;
