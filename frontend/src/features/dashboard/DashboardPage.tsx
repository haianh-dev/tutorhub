import React from 'react';
import { Users, ArrowRight, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { StatTileGroup } from '../../components/ui/StatTile';
import { Card } from '../../components/ui/Card';

export const DashboardPage: React.FC = () => {
  const stats = [
    { label: 'Lớp đang phụ trách', value: '0', subtext: 'Chưa có lớp nào', variant: 'paper' as const },
    { label: 'Buổi học tuần này', value: '0', subtext: 'Lịch dạy trống', variant: 'blue' as const },
    { label: 'Cần điểm danh', value: '0', subtext: 'Tất cả đã hoàn thành', variant: 'paper' as const },
    { label: 'Đợt học phí chưa nộp', value: '0', subtext: 'Cần nhắc qua Zalo', variant: 'yellow' as const },
  ];

  return (
    <div className="flex flex-col gap-8 text-ink">
      {/* Welcome Banner */}
      <div className="brut-box bg-yellow border-3 border-ink p-6 sm:p-8 text-ink flex flex-col gap-4">
        <div className="inline-flex items-center gap-2 border-2 border-ink bg-paper px-3 py-1 text-xs font-heading font-bold uppercase select-none w-fit">
          <ShieldCheck className="w-4 h-4" />
          TutorHub Phase 0 — Nền tảng Neubrutalism
        </div>
        <h2 className="text-2xl sm:text-3xl font-heading font-black tracking-tight uppercase">
          Chào mừng bạn đến với TutorHub!
        </h2>
        <p className="font-body text-sm sm:text-base leading-relaxed max-w-3xl">
          Công cụ hỗ trợ gia sư quản lý lớp học, chống ghi nhận trùng lịch dạy, điểm danh nhanh và theo dõi đợt học phí minh bạch.
        </p>
        <div className="pt-2 flex flex-wrap gap-4">
          <Link
            to="/classes"
            className="brut-pop inline-flex items-center gap-2 min-h-12 px-6 py-3 bg-paper border-3 border-ink text-ink font-heading font-bold text-xs sm:text-sm uppercase tracking-wider"
          >
            <Users className="w-4 h-4" />
            <span>Bắt đầu tạo lớp học</span>
            <ArrowRight className="w-4 h-4 ml-1" />
          </Link>
        </div>
      </div>

      {/* Quick Stats Grid using Neubrutalism StatTileGroup */}
      <div className="flex flex-col gap-2">
        <h3 className="font-heading font-bold text-sm uppercase tracking-wider">
          Chỉ số nhanh
        </h3>
        <StatTileGroup stats={stats} />
      </div>

      {/* Feature Progress Section */}
      <Card className="flex flex-col gap-4">
        <h3 className="font-heading font-black text-lg uppercase tracking-wide">
          Tiến trình triển khai hệ thống
        </h3>
        <p className="font-body text-sm text-ink/80">
          TutorHub được xây dựng từng bước theo ROADMAP với kiểm thử nghiêm ngặt.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
          <div className="border-3 border-ink bg-paper p-5 flex flex-col justify-between gap-3">
            <div className="text-xs font-heading font-bold uppercase text-ink">Phase 0 — Nền tảng</div>
            <div className="text-sm font-heading font-bold text-ink">Backend + Database + UI Neubrutalism</div>
            <div className="border-2 border-ink bg-blue px-2 py-1 text-xs font-heading font-bold uppercase w-fit text-ink">
              ✓ Hoàn tất T0.4.1
            </div>
          </div>

          <div className="border-3 border-ink bg-paper p-5 flex flex-col justify-between gap-3">
            <div className="text-xs font-heading font-bold uppercase text-ink">Phase 1 — Xác thực</div>
            <div className="text-sm font-heading font-bold text-ink">Đăng nhập, JWT, 4 Vai trò, Lời mời</div>
            <div className="border-2 border-ink bg-paper px-2 py-1 text-xs font-heading font-bold uppercase w-fit text-ink">
              Sắp tới (T1.2 - T1.6)
            </div>
          </div>

          <div className="border-3 border-ink bg-paper p-5 flex flex-col justify-between gap-3">
            <div className="text-xs font-heading font-bold uppercase text-ink">Phase 2 &amp; 3 — Lớp &amp; Lịch</div>
            <div className="text-sm font-heading font-bold text-ink">Quản lý lớp 1:1 / Nhóm, Chống trùng lịch</div>
            <div className="border-2 border-ink bg-paper px-2 py-1 text-xs font-heading font-bold uppercase w-fit text-ink">
              Quy tắc PostgreSQL Exclude
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default DashboardPage;
