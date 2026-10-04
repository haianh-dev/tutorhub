import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Lock,
  User,
  Phone,
  GraduationCap,
  UserCheck,
  Users,
  AlertTriangle,
} from 'lucide-react';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { Badge } from '../../../components/ui/Badge';
import AuthCard from '../components/AuthCard';
import EyeToggle from '../components/EyeToggle';
import { useAcceptInvitationMutation, useInvitationQuery } from '../api';
import { useAuth } from '../AuthContext';
import {
  extractAlertMessage,
  extractFieldErrors,
} from '../errorMessages';
import {
  acceptInvitationSchema,
  type AcceptInvitationFormValues,
} from '../schemas';

function formatExpiresAt(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

export const AcceptInvitationPage: React.FC = () => {
  const [params] = useSearchParams();
  const tokenFromUrl = params.get('token') ?? undefined;
  const navigate = useNavigate();
  const { setAuth, getHomeRoute, isAuthenticated } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [submitAlert, setSubmitAlert] = useState<{
    variant: 'info' | 'warning' | 'error';
    message: string;
  } | null>(null);

  const invitationQuery = useInvitationQuery(tokenFromUrl);

  // Redirect nếu đã đăng nhập
  useEffect(() => {
    if (isAuthenticated) {
      navigate(getHomeRoute(), { replace: true });
    }
  }, [isAuthenticated, getHomeRoute, navigate]);

  // Alert tĩnh derive theo query params + invitation state (không dùng useEffect setState)
  const staticAlert = useMemo<
    { variant: 'info' | 'warning' | 'error'; message: string } | null
  >(() => {
    if (!tokenFromUrl) {
      return {
        variant: 'error',
        message:
          'Thiếu token lời mời trong URL. Hãy đảm bảo bạn mở đúng link mà gia sư đã gửi.',
      };
    }
    if (invitationQuery.isError) {
      return {
        variant: 'error',
        message: extractAlertMessage(invitationQuery.error),
      };
    }
    return null;
  }, [tokenFromUrl, invitationQuery.isError, invitationQuery.error]);

  const topAlert = submitAlert ?? staticAlert;

  const invite = invitationQuery.data;

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<AcceptInvitationFormValues>({
    resolver: zodResolver(acceptInvitationSchema) as Resolver<AcceptInvitationFormValues>,
    defaultValues: {
      token: tokenFromUrl ?? '',
      password: '',
      confirmPassword: '',
      fullName: '',
      phone: undefined,
    },
    mode: 'onBlur',
  });

  // Khi load token → set field token (nếu có thay đổi)
  useEffect(() => {
    if (tokenFromUrl) {
      reset((f) => ({ ...f, token: tokenFromUrl }));
    }
  }, [tokenFromUrl, reset]);

  const acceptMutation = useAcceptInvitationMutation({
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
      if (feMap.password) setError('password', { message: feMap.password });
      if (feMap.fullName) setError('fullName', { message: feMap.fullName });
      if (feMap.phone) setError('phone', { message: feMap.phone });
      if (feMap.token) setError('token', { message: feMap.token });
    },
  });

  const onSubmit = handleSubmit((values) => {
    setSubmitAlert(null);
    acceptMutation.mutate({
      token: values.token,
      password: values.password,
      fullName: values.fullName.trim(),
      phone: values.phone,
    });
  });

  const roleLabel = useMemo(() => {
    if (!invite) return null;
    return invite.role === 'STUDENT'
      ? { label: 'Học sinh', icon: <GraduationCap className="w-4 h-4" />, variant: 'blue' as const }
      : { label: 'Phụ huynh', icon: <Users className="w-4 h-4" />, variant: 'yellow' as const };
  }, [invite]);

  // Trạng thái loading / error invitation
  const isInvitationInvalid = !tokenFromUrl || invitationQuery.isError;
  const showForm = invitationQuery.isSuccess && !!invite;

  return (
    <AuthCard
      title="Chấp nhận lời mời"
      subtitle={
        invite
          ? 'Hoàn thiện thông tin tài khoản để tham gia TutorHub.'
          : 'Kiểm tra thông tin lời mời...'
      }
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

        {invitationQuery.isLoading && (
          <div className="border-3 border-ink bg-paper p-4 brut-box">
            <div className="font-heading font-bold uppercase text-xs text-ink/70 mb-2">
              Đang kiểm tra lời mời…
            </div>
            <div className="h-2 w-full bg-cream border-2 border-ink/30 overflow-hidden">
              <div className="h-full w-1/3 bg-yellow animate-pulse" />
            </div>
          </div>
        )}

        {showForm && roleLabel && (
          <div className="border-3 border-ink bg-blue brut-box p-4 text-ink space-y-3">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <Badge variant={roleLabel.variant} className="flex items-center gap-2">
                {roleLabel.icon}
                <span>Lời mời: {roleLabel.label}</span>
              </Badge>
            </div>

            <div className="space-y-1 font-body text-sm text-ink/90 leading-relaxed">
              {invite.className && (
                <div className="flex items-start gap-2">
                  <UserCheck className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>
                    Lớp học: <strong className="font-bold">{invite.className}</strong>
                  </span>
                </div>
              )}
              {invite.email && (
                <div className="flex items-start gap-2">
                  <span className="font-bold">Email gợi ý:</span>
                  <span>{invite.email}</span>
                </div>
              )}
              <div className="flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>Hết hạn lúc: {formatExpiresAt(invite.expiresAt)}</span>
              </div>
            </div>
          </div>
        )}

        <Input
          label="Họ và tên"
          type="text"
          autoComplete="name"
          placeholder="Nguyễn Văn A"
          required
          disabled={!showForm}
          icon={<User className="w-4 h-4 shrink-0" />}
          {...register('fullName')}
          error={errors.fullName?.message}
        />

        <Input
          label="Số điện thoại"
          type="tel"
          autoComplete="tel"
          placeholder="0909 xxx xxx (tùy chọn)"
          disabled={!showForm}
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
            disabled={!showForm}
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
            disabled={!showForm}
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
          isLoading={acceptMutation.isPending}
          loadingText="ĐANG TẠO TÀI KHOẢN…"
          className="w-full justify-center"
          disabled={isInvitationInvalid}
        >
          <UserCheck className="w-4 h-4" />
          Chấp nhận lời mời &amp; đăng nhập
        </Button>
      </form>
    </AuthCard>
  );
};

export default AcceptInvitationPage;
