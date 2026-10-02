import React, { useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Alert } from '../../components/ui/Alert';
import { Input } from '../../components/ui/Input';
import { Tabs } from '../../components/ui/Tabs';
import { StatTileGroup } from '../../components/ui/StatTile';
import { EmptyState } from '../../components/ui/EmptyState';

export const DevUiPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('tab-1');
  const [inputValue, setInputValue] = useState('');

  const demoStats = [
    { label: 'Số lớp đang dạy', value: '4', subtext: '2 lớp 1:1, 2 nhóm', variant: 'paper' as const },
    { label: 'Buổi hôm nay', value: '2', subtext: 'Ca tối 18:00 - 21:00', variant: 'blue' as const },
    { label: 'Cảnh báo học phí', value: '1', subtext: 'Đợt 10 buổi sắp hết', variant: 'yellow' as const },
    { label: 'Học sinh đang học', value: '12', subtext: 'Tổng số học sinh', variant: 'paper' as const },
  ];

  const testString = 'Ệ ẳ ữ ặ Đ ơ ư — Lớp Toán 12A: Điểm danh, Học phí';

  return (
    <div className="flex flex-col gap-12 text-ink max-w-5xl mx-auto pb-16">
      {/* Page Header */}
      <div className="border-b-4 border-ink pb-6">
        <div className="inline-block border-2 border-ink bg-yellow px-3 py-1 font-heading text-xs font-bold uppercase mb-2">
          Môi trường Dev / Kiểm tra trực quan
        </div>
        <h1 className="font-heading font-black text-3xl sm:text-4xl uppercase tracking-tight">
          Neubrutalism UI Showcase
        </h1>
        <p className="font-body text-sm text-ink/80 mt-2">
          Trang kiểm tra các component và quy tắc thiết kế theo UI_GUIDELINES.md.
        </p>
      </div>

      {/* 1. Typography & Vietnamese Accents Check */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          1. Kiểm tra Tiếng Việt (Space Grotesk &amp; Space Mono)
        </h2>
        <Card className="gap-4">
          <div>
            <span className="font-heading text-xs uppercase font-bold text-ink/60 block mb-1">
              Heading (Space Grotesk 900 / 700):
            </span>
            <div className="font-heading font-black text-2xl uppercase text-ink">
              {testString}
            </div>
            <div className="font-heading font-bold text-lg uppercase text-ink mt-1">
              {testString}
            </div>
          </div>

          <div className="border-t-2 border-ink/20 pt-4">
            <span className="font-heading text-xs uppercase font-bold text-ink/60 block mb-1">
              Body (Space Mono 400 / 700):
            </span>
            <div className="font-body text-base text-ink">
              {testString}
            </div>
            <div className="font-body font-bold text-base text-ink mt-1">
              {testString} (In đậm)
            </div>
          </div>
        </Card>
      </section>

      {/* 2. Color Palette */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          2. Bảng 6 màu Design Tokens (Không dùng màu ngoài bảng)
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
          <div className="border-3 border-ink bg-cream p-4 text-center font-heading font-bold text-xs uppercase text-ink">
            Cream<br /><span className="text-[10px] font-mono">#FFFBF0</span>
          </div>
          <div className="border-3 border-ink bg-paper p-4 text-center font-heading font-bold text-xs uppercase text-ink">
            Paper<br /><span className="text-[10px] font-mono">#FFFFFF</span>
          </div>
          <div className="border-3 border-ink bg-ink p-4 text-center font-heading font-bold text-xs uppercase text-paper">
            Ink<br /><span className="text-[10px] font-mono">#000000</span>
          </div>
          <div className="border-3 border-ink bg-yellow p-4 text-center font-heading font-bold text-xs uppercase text-ink">
            Yellow<br /><span className="text-[10px] font-mono">#FFEB3B</span>
          </div>
          <div className="border-3 border-ink bg-coral p-4 text-center font-heading font-bold text-xs uppercase text-ink">
            Coral<br /><span className="text-[10px] font-mono">#FF5252</span>
          </div>
          <div className="border-3 border-ink bg-blue p-4 text-center font-heading font-bold text-xs uppercase text-ink">
            Blue<br /><span className="text-[10px] font-mono">#2196F3</span>
          </div>
        </div>
      </section>

      {/* 3. Buttons */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          3. Nút bấm (Buttons) — brut-pop hover, active &amp; disabled
        </h2>
        <div className="flex flex-wrap items-center gap-4">
          <Button variant="primary">LƯU THAY ĐỔI</Button>
          <Button variant="secondary">HỦY THAO TÁC</Button>
          <Button variant="info">TẠO LINK MỜI</Button>
          <Button variant="danger">HỦY BUỔI HỌC</Button>
          <Button variant="primary" disabled>NÚT BỊ VÔ HIỆU</Button>
          <Button variant="primary" isLoading loadingText="ĐANG LƯU…">NÚT LOADING</Button>
        </div>
      </section>

      {/* 4. Badges */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          4. Nhãn trạng thái (Badges)
        </h2>
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant="paper" icon="✓">CÓ MẶT</Badge>
          <Badge variant="yellow" icon="!">MUỘN</Badge>
          <Badge variant="blue" icon="—">VẮNG CÓ PHÉP</Badge>
          <Badge variant="coral" icon="✕">VẮNG KHÔNG PHÉP</Badge>
          <Badge variant="yellow">LỚP 1:1</Badge>
          <Badge variant="blue">LỚP NHÓM</Badge>
          <Badge variant="blue" icon="✓">ĐÃ NỘP HỌC PHÍ</Badge>
          <Badge variant="coral" icon="!">CHƯA NỘP HỌC PHÍ</Badge>
        </div>
      </section>

      {/* 5. Alerts */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          5. Khối thông báo (Alerts)
        </h2>
        <div className="flex flex-col gap-4">
          <Alert variant="info" title="Thông tin lớp học">
            Học sinh tham gia lớp bằng liên kết mời do gia sư chia sẻ qua Zalo.
          </Alert>
          <Alert variant="warning" title="Cảnh báo lớp nhóm">
            Lớp nhóm hiện tại chỉ có 1 học sinh. Nên có từ 2 học sinh trở lên để đạt hiệu quả giảng dạy tốt nhất.
          </Alert>
          <Alert variant="error" title="Xung đột lịch học">
            Buổi học này trùng giờ với lớp &quot;Toán 12A (Thứ Hai 18:00 - 19:30)&quot;. Vui lòng chọn khung giờ khác.
          </Alert>
        </div>
      </section>

      {/* 6. Form Inputs */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          6. Ô nhập liệu (Input Form)
        </h2>
        <Card className="gap-6">
          <Input
            label="Tên lớp học"
            required
            placeholder="Ví dụ: Ôn thi Đại học Toán 12"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            helperText="Nhập tên lớp rõ ràng giúp học sinh và phụ huynh dễ nhận biết."
          />

          <Input
            label="Ô có thông báo lỗi"
            required
            defaultValue="Giờ học không hợp lệ"
            error="Thời gian kết thúc phải sau thời gian bắt đầu ít nhất 30 phút."
          />

          <Input
            label="Điểm bài tập (Thang điểm 10)"
            type="number"
            min="0"
            max="10"
            step="0.25"
            placeholder="Nhập từ 0 đến 10"
            helperText="Điểm số thang 10, tối đa 2 chữ số thập phân, không tính hệ số."
          />
        </Card>
      </section>

      {/* 7. Tabs */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          7. Phân đoạn điều hướng (Tabs)
        </h2>
        <Tabs
          tabs={[
            { id: 'tab-1', label: 'Học sinh', badge: 12 },
            { id: 'tab-2', label: 'Lịch dạy', badge: 3 },
            { id: 'tab-3', label: 'Điểm danh' },
            { id: 'tab-4', label: 'Đợt học phí', badge: '1 SẮP HẾT' },
          ]}
          activeTab={activeTab}
          onChange={setActiveTab}
        />
        <div className="border-3 border-ink bg-paper p-4 font-body text-sm">
          Đang chọn Tab ID: <strong>{activeTab}</strong>
        </div>
      </section>

      {/* 8. Stat Tiles */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          8. Dải chỉ số (Stat Tile Group)
        </h2>
        <StatTileGroup stats={demoStats} />
      </section>

      {/* 9. Empty State */}
      <section className="flex flex-col gap-4">
        <h2 className="font-heading font-black text-xl uppercase tracking-wide border-b-2 border-ink pb-2">
          9. Khung trạng thái rỗng (Empty State)
        </h2>
        <EmptyState
          title="Chưa có buổi học nào trong tuần"
          description="Hãy tạo lịch học định kỳ hoặc thêm buổi học mới để bắt đầu theo dõi tiến độ giảng dạy."
          actionText="TẠO BUỔI HỌC MỚI"
          onAction={() => alert('Thao tác tạo buổi học mới')}
        />
      </section>
    </div>
  );
};

export default DevUiPage;
