import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Mail,
  Lock,
  User,
  Phone,
  UserPlus,
} from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import AuthCard from '../components/AuthCard';
import EyeToggle from '../components/EyeToggle';
import { useRegisterTutorMutation } from '../api';
import { useAuth } from '../AuthContext';
import {
  extractAlertMessage,
  extractFieldErrors,
} from '../errorMessages';
import {
  registerTutorSchema,
  type RegisterTutorFormValues,
} from '../schemas';

export const RegisterTutorPage: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated, getHomeRoute } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [topAlert, setTopAlert] = useState<{
    variant: 'info' | 'warning' | 'error';
    message: string;
  } | null>(null);

  useEffect(() => {
    if (isAuthenticated) {
      navigate(getHomeRoute(), { replace: true });
    }
  }, [isAuthenticated, getHomeRoute, navigate]);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterTutorFormValues>({
    resolver: zodResolver(registerTutorSchema) as Resolver<RegisterTutorFormValues>,
    defaultValues: {
      email: '',
      password: '',
      confirmPassword: '',
      fullName: '',
      phone: undefined,
    },
    mode: 'onBlur',
  });

  const registerMutation = useRegisterTutorMutation({
    onSuccess: () => {
      navigate('/login?registered=true', { replace: true });
    },
    onError: (err) => {
      setTopAlert({
        variant: 'error',
        message: extractAlertMessage(err),
      });
      const feMap = extractFieldErrors(err);
      if (feMap.email) setError('email', { message: feMap.email });
      if (feMap.password) setError('password', { message: feMap.password });
      if (feMap.fullName) setError('fullName', { message: feMap.fullName });
      if (feMap.phone) setError('phone', { message: feMap.phone });
    },
  });

  const onSubmit = handleSubmit((values) => {
    setTopAlert(null);
    registerMutation.mutate({
      email: values.email.trim(),
      password: values.password,
      fullName: values.fullName.trim(),
      phone: values.phone,
    });
  });

  return (
    <AuthCard
      title="Đăng ký gia sư"
      subtitle="Tạo tài khoản gia sư để quản lý lớp học, lịch dạy và tiến độ học sinh."
      footer={
        <>
          Đã có tài khoản?{' '}
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
          label="Họ và tên"
          type="text"
          autoComplete="name"
          placeholder="Nguyễn Văn A"
          required
          icon={<User className="w-4 h-4 shrink-0" />}
          {...register('fullName')}
          error={errors.fullName?.message}
        />

        <Input
          label="Số điện thoại"
          type="tel"
          autoComplete="tel"
          placeholder="0909 xxx xxx (tùy chọn)"
          icon={<Phone className="w-4 h-4 shrink-0" />}
          {...register('phone')}
          error={errors.phone?.message}
        />

        <div className="flex flex-col gap-1.5 relative">
          <Input
            label="Mật khẩu"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="Ít nhất 8 ký tự"
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
        </div>

        <div className="flex flex-col gap-1.5 relative">
          <Input
            label="Xác nhận mật khẩu"
            type={showConfirm ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="Nhập lại mật khẩu"
            required
            icon={<Lock className="w-4 h-4 shrink-0" />}
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
          isLoading={registerMutation.isPending}
          loadingText="ĐANG TẠO TÀI KHOẢN…"
          className="w-full justify-center"
        >
          <UserPlus className="w-4 h-4" />
          Tạo tài khoản gia sư
        </Button>
      </form>
    </AuthCard>
  );
};

export default RegisterTutorPage;
