import React from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../../components/ui/Card';
import { GraduationCap } from 'lucide-react';

export interface AuthCardProps {
  title: string;
  subtitle?: string;
  footer?: React.ReactNode;
  children: React.ReactNode;
}

export const AuthCard: React.FC<AuthCardProps> = ({
  title,
  subtitle,
  footer,
  children,
}) => {
  return (
    <div className="min-h-screen w-full bg-cream text-ink flex items-center justify-center px-4 py-10 font-body">
      <div className="w-full max-w-md flex flex-col items-stretch gap-6">
        {/* Brand */}
        <Link
          to="/login"
          className="flex items-center justify-center gap-3 select-none group"
        >
          <div className="w-12 h-12 border-3 border-ink bg-yellow text-ink font-heading font-black text-2xl flex items-center justify-center brut-box shrink-0">
            <GraduationCap className="w-7 h-7" strokeWidth={2.5} />
          </div>
          <div className="text-left">
            <div className="font-heading font-black text-2xl uppercase tracking-widest text-ink block leading-tight">
              TutorHub
            </div>
            <div style={{ fontFamily: '"Quicksand", sans-serif' }} className="text-[11px] uppercase font-bold tracking-widest text-ink/70 block">
              Quản lý lịch & Lớp học
            </div>
          </div>
        </Link>

        {/* Card */}
        <Card variant="paper" className="w-full">
          <div className="space-y-1">
            <h2 className="font-heading font-black text-2xl uppercase tracking-wide text-ink">
              {title}
            </h2>
            {subtitle && (
              <p style={{ fontFamily: 'system-ui' }} className="text-sm text-ink/75 leading-relaxed">
                {subtitle}
              </p>
            )}
          </div>

          <div className="pt-2">{children}</div>
        </Card>

        {/* Footer link */}
        {footer && (
          <div className="border-3 border-ink bg-paper p-4 text-center brut-box">
            <div style={{ fontFamily: 'system-ui' }} className="text-sm text-ink/85 leading-relaxed">
              {footer}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AuthCard;
