import React, { useEffect, useMemo, useState } from 'react';
import {
  Plus,
  Search,
  BookOpen,
  Filter,
} from 'lucide-react';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { EmptyState } from '../../components/ui/EmptyState';
import { StatTileGroup } from '../../components/ui/StatTile';
import {
  useArchiveClassMutation,
  useClassesQuery,
} from './api';
import { ClassCard } from './components/ClassCard';
import { CreateClassModal } from './components/CreateClassModal';
import { extractClassErrorMessage } from './errorMessages';
import type { Classroom, ClassStatus } from '../../types';

export const ClassesListPage: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<ClassStatus | 'ALL'>('ACTIVE');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm.trim());
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  const queryParams = useMemo(() => {
    return {
      status: selectedStatus === 'ALL' ? undefined : selectedStatus,
      q: debouncedSearch.length > 0 ? debouncedSearch : undefined,
      size: 50,
    };
  }, [selectedStatus, debouncedSearch]);

  const {
    data: classesPage,
    isLoading,
    isError,
    error,
    refetch,
  } = useClassesQuery(queryParams);

  const archiveMutation = useArchiveClassMutation({
    onSuccess: () => {
      setActionError(null);
    },
    onError: (err) => {
      setActionError(extractClassErrorMessage(err));
    },
  });

  const classes = useMemo(
    () => classesPage?.content ?? [],
    [classesPage?.content],
  );

  // Tính số liệu thống kê
  const stats = useMemo(() => {
    const total = classes.length;
    const oneOnOne = classes.filter((c: Classroom) => c.classType === 'ONE_ON_ONE').length;
    const group = classes.filter((c: Classroom) => c.classType === 'GROUP').length;
    const totalStudents = classes.reduce(
      (sum: number, c: Classroom) => sum + (c.studentCount ?? 0),
      0,
    );

    return [
      {
        label: 'Tổng số lớp',
        value: total.toString(),
        subtext: 'Đang hiển thị',
        variant: 'paper' as const,
      },
      {
        label: 'Lớp 1:1',
        value: oneOnOne.toString(),
        subtext: 'Kèm riêng',
        variant: 'yellow' as const,
      },
      {
        label: 'Lớp Nhóm',
        value: group.toString(),
        subtext: 'Từ 2 HS trở lên',
        variant: 'blue' as const,
      },
      {
        label: 'Tổng học sinh',
        value: totalStudents.toString(),
        subtext: 'Đang theo học',
        variant: 'paper' as const,
      },
    ];
  }, [classes]);

  return (
    <div className="flex flex-col gap-8 text-ink">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b-3 border-ink pb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="border-2 border-ink bg-yellow px-2 py-0.5 text-xs font-heading font-black uppercase">
              Phase 2 (T2.1 - T2.3)
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-heading font-black tracking-tight uppercase">
            Quản lý Lớp học &amp; Học sinh
          </h1>
          <p className="font-body text-sm text-ink/80 mt-1 max-w-2xl">
            Tạo và theo dõi các lớp học 1:1 hoặc nhóm, ghi danh học sinh, quản lý
            sĩ số và gửi liên kết mời học sinh/phụ huynh.
          </p>
        </div>

        <div className="shrink-0">
          <Button
            variant="primary"
            onClick={() => setIsCreateOpen(true)}
            className="w-full sm:w-auto justify-center"
          >
            <Plus className="w-4 h-4" />
            Tạo lớp học mới
          </Button>
        </div>
      </div>

      {actionError && (
        <Alert variant="error" title="Thao tác thất bại">
          {actionError}
        </Alert>
      )}

      {/* Quick Stats Grid */}
      <div className="flex flex-col gap-2">
        <h3 className="font-heading font-bold text-xs uppercase tracking-wider text-ink/70">
          Tổng quan lớp học
        </h3>
        <StatTileGroup stats={stats} />
      </div>

      {/* Filter & Search Bar */}
      <div className="border-3 border-ink bg-paper brut-box p-4 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        {/* Search Input */}
        <div className="w-full md:max-w-md">
          <Input
            placeholder="Tìm theo tên lớp, môn học..."
            value={searchTerm}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setSearchTerm(e.target.value)
            }
            icon={<Search className="w-4 h-4" />}
          />
        </div>

        {/* Status Filters */}
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-heading font-bold uppercase text-ink/70 flex items-center gap-1">
            <Filter className="w-3.5 h-3.5" />
            Trạng thái:
          </span>

          <button
            type="button"
            onClick={() => setSelectedStatus('ACTIVE')}
            className={`px-3 py-1.5 border-2 border-ink text-xs font-heading font-bold uppercase transition-transform ${
              selectedStatus === 'ACTIVE'
                ? 'bg-yellow brut-box -translate-y-0.5'
                : 'bg-paper hover:bg-cream'
            }`}
          >
            Đang hoạt động
          </button>

          <button
            type="button"
            onClick={() => setSelectedStatus('ARCHIVED')}
            className={`px-3 py-1.5 border-2 border-ink text-xs font-heading font-bold uppercase transition-transform ${
              selectedStatus === 'ARCHIVED'
                ? 'bg-coral text-ink brut-box -translate-y-0.5'
                : 'bg-paper hover:bg-cream'
            }`}
          >
            Đã lưu trữ
          </button>

          <button
            type="button"
            onClick={() => setSelectedStatus('ALL')}
            className={`px-3 py-1.5 border-2 border-ink text-xs font-heading font-bold uppercase transition-transform ${
              selectedStatus === 'ALL'
                ? 'bg-blue text-white brut-box -translate-y-0.5'
                : 'bg-paper hover:bg-cream'
            }`}
          >
            Tất cả
          </button>
        </div>
      </div>

      {/* Classes Grid / Loading / Empty */}
      {isLoading ? (
        <div className="border-3 border-ink bg-paper brut-box p-12 text-center">
          <p className="font-heading font-bold text-sm uppercase animate-pulse">
            Đang tải danh sách lớp học…
          </p>
        </div>
      ) : isError ? (
        <Alert variant="error" title="Không thể tải danh sách lớp">
          {extractClassErrorMessage(error)}
          <div className="mt-3">
            <Button variant="secondary" size="sm" onClick={() => refetch()}>
              Thử lại
            </Button>
          </div>
        </Alert>
      ) : classes.length === 0 ? (
        <EmptyState
          icon={<BookOpen className="w-10 h-10" />}
          title={
            debouncedSearch || selectedStatus !== 'ACTIVE'
              ? 'Không tìm thấy lớp học phù hợp'
              : 'Bạn chưa tạo lớp học nào'
          }
          description={
            debouncedSearch
              ? `Không có kết quả khớp với từ khóa "${debouncedSearch}". Hãy thử tìm kiếm khác.`
              : 'Bắt đầu bằng cách tạo lớp 1:1 hoặc lớp nhóm để quản lý học sinh và lịch dạy.'
          }
          actionText="Tạo lớp học đầu tiên"
          onAction={() => setIsCreateOpen(true)}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {classes.map((cls: Classroom) => (
            <ClassCard
              key={cls.id}
              classroom={cls}
              onArchive={(id: number) => archiveMutation.mutate(id)}
              isArchiving={archiveMutation.isPending}
            />
          ))}
        </div>
      )}

      {/* Modal tạo lớp mới */}
      <CreateClassModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
      />
    </div>
  );
};

export default ClassesListPage;
