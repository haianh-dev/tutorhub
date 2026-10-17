import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Home, Shield } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { Alert } from '../components/ui/Alert';
import { useAuth } from '../features/auth/useAuth';

export const UnauthorizedPage: React.FC = () => {
  const { user, getHomeRoute } = useAuth();
  return (
    <div className="min-h-screen bg-cream text-ink font-body flex items-center justify-center p-6">
      <div className="w-full max-w-lg space-y-6">
        <div className="flex items-center justify-center gap-3 mb-2">
          <div className="w-14 h-14 border-3 border-ink bg-coral brut-box flex items-center justify-center text-ink">
            <Shield className="w-7 h-7" strokeWidth={2.5} />
          </div>
          <h1 className="font-heading font-black text-4xl uppercase tracking-wider">
            403
          </h1>
        </div>

        <Alert variant="error" title="Không đủ quyền">
          <p className="leading-relaxed">
            Tài khoản hiện tại ({user?.role ?? 'chưa đăng nhập'}) không có
            quyền truy cập trang này.
          </p>
          <p className="leading-relaxed mt-2 text-ink/85">
            Bạn hãy quay lại trang phù hợp với vai trò của mình.
          </p>
        </Alert>

        <div className="flex flex-wrap gap-3 justify-center">
          <Button
            variant="secondary"
            onClick={() => window.history.back()}
            className="justify-center"
          >
            <ArrowLeft className="w-4 h-4" />
            Quay lại
          </Button>
          <Link to={user ? getHomeRoute() : '/login'}>
            <Button variant="primary" className="justify-center">
              <Home className="w-4 h-4" />
              {user ? 'Về trang chủ' : 'Đăng nhập'}
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};

export const NotFoundPage: React.FC = () => (
  <div className="min-h-screen bg-cream text-ink font-body flex items-center justify-center p-6">
    <div className="w-full max-w-md text-center space-y-4">
      <h1 className="text-7xl font-heading font-black tracking-wider">404</h1>
      <p className="font-body text-ink/80 text-base">
        Trang bạn tìm kiếm không tồn tại.
      </p>
      <Link to="/">
        <Button variant="primary">
          <Home className="w-4 h-4" />
          Về trang chủ
        </Button>
      </Link>
    </div>
  </div>
);
