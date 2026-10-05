import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Mail, Lock, LogIn } from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import AuthCard from '../components/AuthCard';
import EyeToggle from '../components/EyeToggle';
import { useAuth } from '../AuthContext';
import { useLoginMutation } from '../api';
import {
  extractAlertMessage,
  extractFieldErrors,
} from '../errorMessages';
import {
  loginSchema,
  type LoginFormValues,
} from '../schemas';

export const LoginPage: React.FC = () => {
  const { setAuth, getHomeRoute, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [showPassword, setShowPassword] = useState(false);
  const [submitAlert, setSubmitAlert] = useState<{
    variant: 'info' | 'warning' | 'error';
    message: string;
  } | null>(null);

  useEffect(() => {
    if (isAuthenticated) {
      navigate(getHomeRoute(), { replace: true });
    }
  }, [isAuthenticated, getHomeRoute, navigate]);

  const paramAlert = useMemo<
    { variant: 'info' | 'warning' | 'error'; message: string } | null
  >(() => {
    const registered = params.get('registered');
    const resetDone = params.get('reset');
    const invitedAccepted = params.get('accepted');
    if (registered === 'true') {
      return {
        variant: 'info',
        message:
          'Đăng ký tài khoản gia sư thành công! Hãy đăng nhập để bắt đầu.',
      };
    }
    if (resetDone === 'true') {
      return {
        variant: 'info',
        message:
          'Mật khẩu mới đã được đặt. Hãy đăng nhập bằng mật khẩu mới.',
      };
    }
    if (invitedAccepted === 'true') {
      return {
        variant: 'info',
        message:
          'Tài khoản đã được tạo. Hãy đăng nhập bằng mật khẩu vừa đặt.',
      };
    }
    return null;
  }, [params]);

  const topAlert = submitAlert ?? paramAlert;

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
    mode: 'onBlur',
  });

  const loginMutation = useLoginMutation({
    onSuccess: (auth) => {
      setAuth(auth);
      navigate(getHomeRoute(), { replace: true });
    },
    onError: (err) => {
      setSubmitAlert({
        variant: 'error',
        message: extractAlertMessage(err),
      });
      const feMap = extractFieldErrors(err);
      if (feMap.email) setError('email', { message: feMap.email });
      if (feMap.password) setError('password', { message: feMap.password });
    },
  });

  const onSubmit = handleSubmit((values) => {
    setSubmitAlert(null);
    loginMutation.mutate(values);
  });

  return (
    <AuthCard
      title="Đăng nhập"
      subtitle="Đăng nhập để truy cập TutorHub — quản lý lịch, lớp và tiến độ học sinh."
      footer={
        <>
          Chưa có tài khoản?{' '}
          <Link
            to="/register"
            className="font-heading font-bold uppercase text-ink underline decoration-2 underline-offset-4 hover:text-ink/70"
          >
            Đăng ký gia sư
          </Link>
          <div style={{ fontFamily: 'system-ui' }} className="mt-2 pt-2 border-t-2 border-ink/20 text-xs text-ink/70 leading-relaxed">
            Bạn là học sinh hoặc phụ huynh?<br />
            Sử dụng <strong>link lời mời</strong> do gia sư gửi qua Zalo/tin
            nhắn để tạo tài khoản.
          </div>
        </>
      }
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4" noValidate>
        {topAlert && (
          <Alert variant={topAlert.variant}>{topAlert.message}</Alert>
        )}

        <Input
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="gia.su@example.com"
          required
          icon={<Mail className="w-4 h-4 shrink-0" />}
          {...register('email')}
          error={errors.email?.message}
        />

        <Input
          label="Mật khẩu"
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          placeholder="• • • • • • • •"
          required
          icon={<Lock className="w-4 h-4 shrink-0" />}
          rightAdornment={
            <EyeToggle
              shown={showPassword}
              onToggle={() => setShowPassword((v) => !v)}
            />
          }
          {...register('password')}
          error={errors.password?.message}
        />

        <Button
          type="submit"
          variant="primary"
          isLoading={loginMutation.isPending}
          loadingText="ĐANG ĐĂNG NHẬP…"
          className="w-full justify-center"
        >
          <LogIn className="w-4 h-4" />
          Đăng nhập
        </Button>
      </form>
    </AuthCard>
  );
};

export default LoginPage;
