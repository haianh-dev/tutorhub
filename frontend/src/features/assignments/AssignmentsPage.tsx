import React, { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  BookOpen,
  Plus,
  Clock,
  Edit3,
  Trash2,
  GraduationCap,
} from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Alert } from '../../components/ui/Alert';
import { EmptyState } from '../../components/ui/EmptyState';
import { StatTileGroup } from '../../components/ui/StatTile';
import { useQuery } from '@tanstack/react-query';
import { fetchClasses } from '../classes/api';
import {
  useClassAssignments,
  useDeleteAssignment,
} from './api';
import {
  ASSIGNMENT_TYPE_LABELS,
  type Assignment,
} from './types';
import { CreateAssignmentModal } from './components/CreateAssignmentModal';
import { EditAssignmentModal } from './components/EditAssignmentModal';
import { GradeAssignmentModal } from './components/GradeAssignmentModal';

export const AssignmentsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlClassId = searchParams.get('classId') ? Number(searchParams.get('classId')) : null;

  const [internalClassId, setInternalClassId] = useState<number | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingAssignment, setEditingAssignment] = useState<Assignment | null>(null);
  const [gradingAssignment, setGradingAssignment] = useState<Assignment | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Tải danh sách lớp học của gia sư
  const {
    data: classesPage,
    isLoading: isClassesLoading,
    isError: isClassesError,
  } = useQuery({
    queryKey: ['classes', 'allActive'],
    queryFn: () => fetchClasses({ size: 100 }),
  });

  const classes = useMemo(() => classesPage?.content || [], [classesPage]);

  // Derived selectedClassId: urlClassId > internalClassId > lớp đầu tiên
  const selectedClassId = useMemo(() => {
    if (urlClassId && classes.some((c) => c.id === urlClassId)) {
      return urlClassId;
    }
    if (internalClassId && classes.some((c) => c.id === internalClassId)) {
      return internalClassId;
    }
    return classes.length > 0 ? classes[0].id : null;
  }, [urlClassId, internalClassId, classes]);

  const handleClassChange = (newClassId: number) => {
    setInternalClassId(newClassId);
    setSearchParams({ classId: String(newClassId) });
  };

  const selectedClass = useMemo(() => {
    return classes.find((c) => c.id === selectedClassId) || null;
  }, [classes, selectedClassId]);

  // Tải bài tập của lớp đã chọn
  const {
    data: assignments = [],
    isLoading: isAssignmentsLoading,
    isError: isAssignmentsError,
    error: assignmentsError,
  } = useClassAssignments(selectedClassId || undefined);

  // Mutation xóa bài tập
  const deleteMutation = useDeleteAssignment(selectedClassId || 0, {
    onSuccess: () => {
      setActionError(null);
    },
    onError: (err) => {
      setActionError(
        err.message || 'Không thể xóa bài tập. Bài tập có thể đã có điểm được chấm.',
      );
    },
  });

  const handleDelete = (assignment: Assignment) => {
    if (
      window.confirm(
        `Bạn có chắc chắn muốn xóa bài tập "${assignment.title}"? Thao tác này không thể hoàn tác.`,
      )
    ) {
      deleteMutation.mutate(assignment.id);
    }
  };

  // Thống kê nhanh các loại bài tập
  const stats = useMemo(() => {
    const total = assignments.length;
    const homework = assignments.filter((a) => a.type === 'HOMEWORK').length;
    const quizOrExam = assignments.filter(
      (a) => a.type === 'QUIZ' || a.type === 'EXAM' || a.type === 'MOCK_TEST',
    ).length;
    return { total, homework, quizOrExam };
  }, [assignments]);

  return (
    <div className="flex flex-col gap-8 text-ink">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b-3 border-ink pb-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <BookOpen className="w-8 h-8 text-ink" />
            <h1 className="text-2xl sm:text-3xl font-heading font-black uppercase tracking-wider text-ink">
              Bài tập &amp; Bảng điểm
            </h1>
          </div>
          <p className="font-body text-xs sm:text-sm text-ink/80">
            Quản lý bài tập, bài kiểm tra và nhập điểm học sinh trên thang điểm 10 cố định (D-30).
          </p>
        </div>

        {selectedClass && (
          <Button
            variant="primary"
            onClick={() => setIsCreateOpen(true)}
            className="shrink-0"
          >
            <Plus className="w-4 h-4" />
            Giao bài tập mới
          </Button>
        )}
      </div>

      {actionError && (
        <Alert variant="error" title="Thao tác thất bại">
          {actionError}
        </Alert>
      )}

      {/* Lựa chọn lớp học & Thống kê */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
        {/* Bộ chọn lớp */}
        <Card className="lg:col-span-1 space-y-4">
          <label className="block text-xs font-heading font-black uppercase tracking-wider text-ink">
            Chọn lớp học
          </label>

          {isClassesLoading ? (
            <div className="font-mono text-xs text-ink/60">Đang tải danh sách lớp...</div>
          ) : isClassesError ? (
            <div className="font-mono text-xs text-coral">Lỗi tải danh sách lớp</div>
          ) : classes.length === 0 ? (
            <div className="font-mono text-xs text-ink/70">
              Bạn chưa phụ trách lớp học nào.
            </div>
          ) : (
            <select
              value={selectedClassId || ''}
              onChange={(e) => handleClassChange(Number(e.target.value))}
              className="w-full h-11 px-3 bg-paper border-2 border-ink brut-box font-mono text-sm text-ink focus:outline-none focus:bg-yellow/10"
            >
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.subject})
                </option>
              ))}
            </select>
          )}

          {selectedClass && (
            <div className="p-3 bg-cream border-2 border-ink text-xs font-mono space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-ink/60">Môn học:</span>
                <span className="font-bold">{selectedClass.subject}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-ink/60">Sĩ số:</span>
                <span className="font-bold">{selectedClass.studentCount ?? 0} học sinh</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-ink/60">Loại lớp:</span>
                <Badge variant={selectedClass.classType === 'ONE_ON_ONE' ? 'yellow' : 'blue'}>
                  {selectedClass.classType === 'ONE_ON_ONE' ? '1:1' : 'Nhóm'}
                </Badge>
              </div>
            </div>
          )}
        </Card>

        {/* Thống kê thẻ số liệu */}
        <div className="lg:col-span-3">
          <StatTileGroup
            stats={[
              {
                label: 'Tổng số bài đã giao',
                value: stats.total,
                variant: 'paper',
                subtext: 'Tất cả bài tập & kiểm tra',
              },
              {
                label: 'Bài tập về nhà',
                value: stats.homework,
                variant: 'yellow',
                subtext: 'BTVN rèn luyện',
              },
              {
                label: 'Bài kiểm tra & Thi',
                value: stats.quizOrExam,
                variant: 'blue',
                subtext: 'Đánh giá năng lực',
              },
            ]}
          />
        </div>
      </div>

      {/* Danh sách bài tập của lớp */}
      <div className="space-y-4">
        <h2 className="text-lg font-heading font-black uppercase tracking-wider text-ink border-b-2 border-ink pb-2 flex items-center gap-2">
          <span>Danh sách bài tập ({assignments.length})</span>
          {selectedClass && (
            <span className="text-xs font-mono font-bold text-ink/60 lowercase">
              — {selectedClass.name}
            </span>
          )}
        </h2>

        {isAssignmentsLoading ? (
          <div className="p-12 text-center border-2 border-ink brut-box bg-paper font-mono text-sm text-ink/70">
            Đang tải danh sách bài tập...
          </div>
        ) : isAssignmentsError ? (
          <Alert variant="error" title="Không thể tải danh sách bài tập">
            {assignmentsError?.message || 'Lỗi khi tải bài tập.'}
          </Alert>
        ) : !selectedClassId ? (
          <EmptyState
            title="Chưa chọn lớp học"
            description="Vui lòng chọn một lớp học ở trên để quản lý bài tập và bảng điểm."
          />
        ) : assignments.length === 0 ? (
          <EmptyState
            title="Lớp chưa có bài tập nào"
            description="Hãy bắt đầu giao bài tập về nhà hoặc bài kiểm tra đầu tiên cho học sinh."
            actionText="Giao bài tập mới"
            onAction={() => setIsCreateOpen(true)}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {assignments.map((assignment) => (
              <Card
                key={assignment.id}
                className="flex flex-col justify-between space-y-4 hover:translate-x-0.5 hover:translate-y-0.5 transition-transform"
              >
                <div className="space-y-3">
                  {/* Badge hàng trên */}
                  <div className="flex items-center justify-between gap-2 flex-wrap">
                    <Badge variant={assignment.type === 'HOMEWORK' ? 'yellow' : 'blue'}>
                      {ASSIGNMENT_TYPE_LABELS[assignment.type]}
                    </Badge>

                    {assignment.dueAt ? (
                      <span className="text-xs font-mono font-bold text-ink/70 inline-flex items-center gap-1 bg-cream px-2 py-0.5 border border-ink">
                        <Clock className="w-3.5 h-3.5" />
                        Hạn: {new Date(assignment.dueAt).toLocaleDateString('vi-VN')}
                      </span>
                    ) : (
                      <span className="text-xs font-mono text-ink/50">Không hạn nộp</span>
                    )}
                  </div>

                  {/* Tiêu đề & Mô tả */}
                  <div className="space-y-1.5">
                    <h3 className="font-heading font-black text-base text-ink uppercase tracking-wide">
                      {assignment.title}
                    </h3>
                    {assignment.description && (
                      <p className="font-body text-xs text-ink/80 leading-relaxed line-clamp-2">
                        {assignment.description}
                      </p>
                    )}
                  </div>
                </div>

                {/* Hàng nút tác vụ dưới */}
                <div className="pt-3 border-t-2 border-ink/15 flex items-center justify-between gap-2">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => setGradingAssignment(assignment)}
                    className="flex-1 justify-center"
                  >
                    <GraduationCap className="w-4 h-4" />
                    Nhập điểm cả lớp
                  </Button>

                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      title="Sửa bài tập"
                      onClick={() => setEditingAssignment(assignment)}
                      className="p-2 bg-paper hover:bg-yellow border-2 border-ink transition-colors"
                    >
                      <Edit3 className="w-4 h-4 text-ink" />
                    </button>
                    <button
                      type="button"
                      title="Xóa bài tập"
                      onClick={() => handleDelete(assignment)}
                      disabled={deleteMutation.isPending}
                      className="p-2 bg-paper hover:bg-coral border-2 border-ink transition-colors"
                    >
                      <Trash2 className="w-4 h-4 text-ink" />
                    </button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Modal Tạo bài tập */}
      {selectedClassId && (
        <CreateAssignmentModal
          isOpen={isCreateOpen}
          onClose={() => setIsCreateOpen(false)}
          classId={selectedClassId}
          className={selectedClass?.name}
        />
      )}

      {/* Modal Sửa bài tập */}
      {editingAssignment && (
        <EditAssignmentModal
          isOpen={Boolean(editingAssignment)}
          onClose={() => setEditingAssignment(null)}
          assignment={editingAssignment}
        />
      )}

      {/* Modal Bảng nhập điểm cả lớp */}
      {gradingAssignment && (
        <GradeAssignmentModal
          isOpen={Boolean(gradingAssignment)}
          onClose={() => setGradingAssignment(null)}
          assignment={gradingAssignment}
        />
      )}
    </div>
  );
};

export default AssignmentsPage;
