import React, { useMemo, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Users,
  UserCheck,
  Share2,
  Edit2,
  Archive,
  UserPlus,
  UserMinus,
  Mail,
  Phone,
  AlertTriangle,
  BookOpen,
} from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { EmptyState } from '../../components/ui/EmptyState';
import {
  useArchiveClassMutation,
  useClassByIdQuery,
  useClassStudentsQuery,
  useRemoveStudentMutation,
} from './api';
import { EditClassModal } from './components/EditClassModal';
import { ClassInviteModal } from './components/ClassInviteModal';
import { EnrollStudentModal } from './components/EnrollStudentModal';
import { extractClassErrorMessage } from './errorMessages';
import type { Enrollment, EnrollmentStatus } from '../../types';

export const ClassDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const classId = id ? parseInt(id, 10) : undefined;

  const [studentStatusTab, setStudentStatusTab] =
    useState<EnrollmentStatus | 'ALL'>('ACTIVE');
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [isEnrollOpen, setIsEnrollOpen] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const {
    data: classroom,
    isLoading: isClassLoading,
    isError: isClassError,
    error: classError,
  } = useClassByIdQuery(classId);

  const {
    data: students = [],
    isLoading: isStudentsLoading,
    isError: isStudentsError,
    error: studentsError,
  } = useClassStudentsQuery(classId);

  const removeMutation = useRemoveStudentMutation({
    onSuccess: () => {
      setActionError(null);
    },
    onError: (err) => {
      setActionError(extractClassErrorMessage(err));
    },
  });

  const archiveMutation = useArchiveClassMutation({
    onSuccess: () => {
      setActionError(null);
    },
    onError: (err) => {
      setActionError(extractClassErrorMessage(err));
    },
  });

  const activeStudents = useMemo(
    () => students.filter((s: Enrollment) => s.status === 'ACTIVE'),
    [students],
  );

  const leftStudents = useMemo(
    () => students.filter((s: Enrollment) => s.status === 'LEFT'),
    [students],
  );

  const displayedStudents = useMemo(() => {
    if (studentStatusTab === 'ACTIVE') return activeStudents;
    if (studentStatusTab === 'LEFT') return leftStudents;
    return students;
  }, [studentStatusTab, activeStudents, leftStudents, students]);

  if (isClassLoading) {
    return (
      <div className="border-3 border-ink bg-paper brut-box p-12 text-center text-ink">
        <p className="font-heading font-bold text-sm uppercase animate-pulse">
          Đang tải chi tiết lớp học…
        </p>
      </div>
    );
  }

  if (isClassError || !classroom) {
    return (
      <div className="space-y-4">
        <Link
          to="/classes"
          className="inline-flex items-center gap-1.5 text-xs font-heading font-bold uppercase text-ink underline hover:text-ink/70"
        >
          <ArrowLeft className="w-4 h-4" /> Quay lại danh sách lớp
        </Link>
        <Alert variant="error" title="Không thể tải lớp học">
          {extractClassErrorMessage(classError)}
        </Alert>
      </div>
    );
  }

  const isOneOnOne = classroom.classType === 'ONE_ON_ONE';
  const isArchived = classroom.status === 'ARCHIVED';
  const hasWarnings = classroom.warnings && classroom.warnings.length > 0;

  return (
    <div className="flex flex-col gap-8 text-ink">
      {/* Navigation & Header */}
      <div className="space-y-3">
        <Link
          to="/classes"
          className="inline-flex items-center gap-1.5 text-xs font-heading font-bold uppercase text-ink hover:underline decoration-2 underline-offset-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Quay lại danh sách lớp học
        </Link>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b-3 border-ink pb-6">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <Badge
                variant={isOneOnOne ? 'yellow' : 'blue'}
                icon={
                  isOneOnOne ? (
                    <UserCheck className="w-3.5 h-3.5" />
                  ) : (
                    <Users className="w-3.5 h-3.5 text-white" />
                  )
                }
              >
                <span className={isOneOnOne ? 'text-ink' : 'text-white'}>
                  {isOneOnOne ? 'Lớp 1:1' : 'Lớp nhóm'}
                </span>
              </Badge>

              {isArchived ? (
                <Badge variant="coral">Đã lưu trữ</Badge>
              ) : (
                <Badge variant="paper">Đang hoạt động</Badge>
              )}

              <span className="font-mono text-xs font-bold text-ink/60">
                Mã lớp #{classroom.id}
              </span>
            </div>

            <h1 className="text-2xl sm:text-3xl font-heading font-black tracking-tight uppercase">
              {classroom.name}
            </h1>

            <div className="flex items-center gap-2 text-sm font-heading font-bold uppercase text-ink/80">
              <BookOpen className="w-4 h-4" />
              <span>Môn: {classroom.subject}</span>
            </div>
          </div>

          {!isArchived && (
            <div className="flex flex-wrap items-center gap-2">
              <Button
                variant="secondary"
                onClick={() => setIsInviteOpen(true)}
              >
                <Share2 className="w-4 h-4" />
                Mời học sinh / PH
              </Button>

              <Button
                variant="secondary"
                onClick={() => setIsEditOpen(true)}
              >
                <Edit2 className="w-4 h-4" />
                Sửa lớp
              </Button>

              <Button
                variant="secondary"
                onClick={() => {
                  if (
                    window.confirm(
                      `Bạn có chắc chắn muốn lưu trữ lớp "${classroom.name}"? Lớp sẽ không thể chỉnh sửa sau khi lưu trữ.`,
                    )
                  ) {
                    archiveMutation.mutate(classroom.id);
                  }
                }}
                isLoading={archiveMutation.isPending}
              >
                <Archive className="w-4 h-4 text-coral" />
                Lưu trữ
              </Button>
            </div>
          )}
        </div>
      </div>

      {actionError && (
        <Alert variant="error" title="Thao tác thất bại">
          {actionError}
        </Alert>
      )}

      {/* Cảnh báo sĩ số nếu có */}
      {hasWarnings && (
        <div className="p-4 bg-yellow border-3 border-ink brut-box flex items-center gap-3">
          <AlertTriangle className="w-6 h-6 shrink-0 text-ink" />
          <div className="text-sm font-heading font-bold uppercase">
            {classroom.warnings![0]}
          </div>
        </div>
      )}

      {/* Thông tin lớp & Thống kê */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2 space-y-4">
          <h3 className="font-heading font-black text-base uppercase tracking-wider border-b-2 border-ink pb-2">
            Mô tả &amp; Mục tiêu lớp học
          </h3>
          <p className="font-body text-sm text-ink/90 leading-relaxed whitespace-pre-wrap">
            {classroom.description || 'Chưa có mô tả chi tiết cho lớp học này.'}
          </p>

          <div className="pt-4 grid grid-cols-2 gap-4 text-xs font-heading font-bold border-t-2 border-ink/20">
            <div>
              <span className="text-ink/60 uppercase block">Ngày tạo:</span>
              <span className="font-mono text-sm">
                {new Date(classroom.createdAt).toLocaleDateString('vi-VN')}
              </span>
            </div>
            <div>
              <span className="text-ink/60 uppercase block">Cập nhật lần cuối:</span>
              <span className="font-mono text-sm">
                {new Date(classroom.updatedAt).toLocaleDateString('vi-VN')}
              </span>
            </div>
          </div>
        </Card>

        {/* Thống kê sĩ số */}
        <Card className="space-y-4 flex flex-col justify-between">
          <div className="space-y-3">
            <h3 className="font-heading font-black text-base uppercase tracking-wider border-b-2 border-ink pb-2">
              Sĩ số hiện tại
            </h3>
            <div className="text-4xl font-heading font-black">
              {activeStudents.length}
              <span className="text-base font-normal text-ink/70 ml-2">
                {isOneOnOne ? '/ 1 tối đa' : 'học sinh'}
              </span>
            </div>
            <p className="font-body text-xs text-ink/80">
              {isOneOnOne
                ? 'Lớp 1:1 chỉ cho phép 1 học sinh đang học đồng thời.'
                : 'Lớp nhóm nên có từ 2 học sinh để duy trì hiệu quả học tập.'}
            </p>
          </div>

          {!isArchived && (
            <div className="pt-2">
              <Button
                variant="primary"
                onClick={() => setIsEnrollOpen(true)}
                className="w-full justify-center"
                disabled={isOneOnOne && activeStudents.length >= 1}
              >
                <UserPlus className="w-4 h-4" />
                Ghi danh trực tiếp
              </Button>
            </div>
          )}
        </Card>
      </div>

      {/* Danh sách học sinh */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b-3 border-ink pb-3">
          <div className="flex items-center gap-3">
            <h2 className="font-heading font-black text-xl uppercase tracking-wider">
              Danh sách học sinh
            </h2>
            <span className="border-2 border-ink px-2 py-0.5 bg-paper text-xs font-mono font-bold">
              {students.length} bản ghi
            </span>
          </div>

          {/* Tab filter trạng thái học sinh */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setStudentStatusTab('ACTIVE')}
              className={`px-3 py-1 border-2 border-ink text-xs font-heading font-bold uppercase ${
                studentStatusTab === 'ACTIVE'
                  ? 'bg-yellow brut-box'
                  : 'bg-paper hover:bg-cream'
              }`}
            >
              Đang học ({activeStudents.length})
            </button>
            <button
              type="button"
              onClick={() => setStudentStatusTab('LEFT')}
              className={`px-3 py-1 border-2 border-ink text-xs font-heading font-bold uppercase ${
                studentStatusTab === 'LEFT'
                  ? 'bg-coral text-ink brut-box'
                  : 'bg-paper hover:bg-cream'
              }`}
            >
              Đã nghỉ ({leftStudents.length})
            </button>
            <button
              type="button"
              onClick={() => setStudentStatusTab('ALL')}
              className={`px-3 py-1 border-2 border-ink text-xs font-heading font-bold uppercase ${
                studentStatusTab === 'ALL'
                  ? 'bg-blue text-white brut-box'
                  : 'bg-paper hover:bg-cream'
              }`}
            >
              Tất cả ({students.length})
            </button>
          </div>
        </div>

        {/* Bảng học sinh */}
        {isStudentsLoading ? (
          <div className="border-3 border-ink bg-paper brut-box p-8 text-center">
            <p className="font-heading font-bold text-xs uppercase animate-pulse">
              Đang tải danh sách học sinh…
            </p>
          </div>
        ) : isStudentsError ? (
          <Alert variant="error">
            {extractClassErrorMessage(studentsError)}
          </Alert>
        ) : displayedStudents.length === 0 ? (
          <EmptyState
            icon={<Users className="w-8 h-8" />}
            title={
              studentStatusTab === 'LEFT'
                ? 'Không có học sinh nào đã nghỉ'
                : 'Chưa có học sinh nào trong lớp'
            }
            description={
              studentStatusTab === 'LEFT'
                ? 'Chưa có học sinh nào rời khỏi lớp này.'
                : 'Bạn có thể ghi danh trực tiếp bằng ID học sinh hoặc tạo link mời gửi qua Zalo.'
            }
            actionText={!isArchived && studentStatusTab !== 'LEFT' ? 'Ghi danh ngay' : undefined}
            onAction={!isArchived && studentStatusTab !== 'LEFT' ? () => setIsEnrollOpen(true) : undefined}
          />
        ) : (
          <div className="border-3 border-ink bg-paper brut-box overflow-x-auto">
            <table className="w-full text-left text-xs font-body text-ink">
              <thead className="bg-cream border-b-3 border-ink font-heading font-bold uppercase tracking-wider">
                <tr>
                  <th className="p-3">Học sinh</th>
                  <th className="p-3">Thông tin liên hệ</th>
                  <th className="p-3">Trạng thái</th>
                  <th className="p-3">Ngày ghi danh</th>
                  {!isArchived && <th className="p-3 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y-2 divide-ink">
                {displayedStudents.map((st: Enrollment) => (
                  <tr key={st.id} className="hover:bg-cream/60 transition-colors">
                    <td className="p-3">
                      <div className="font-heading font-bold text-sm">
                        {st.studentName}
                      </div>
                      <div className="text-ink/60 font-mono text-[11px]">
                        ID: #{st.studentId}
                      </div>
                    </td>

                    <td className="p-3 space-y-1">
                      <div className="flex items-center gap-1.5 text-ink/80">
                        <Mail className="w-3.5 h-3.5 shrink-0" />
                        <span className="font-mono">{st.studentEmail}</span>
                      </div>
                      {st.studentPhone && (
                        <div className="flex items-center gap-1.5 text-ink/80">
                          <Phone className="w-3.5 h-3.5 shrink-0" />
                          <span className="font-mono">{st.studentPhone}</span>
                        </div>
                      )}
                    </td>

                    <td className="p-3">
                      {st.status === 'ACTIVE' ? (
                        <Badge variant="yellow">Đang học</Badge>
                      ) : (
                        <Badge variant="coral">Đã nghỉ</Badge>
                      )}
                    </td>

                    <td className="p-3 font-mono">
                      <div>
                        {new Date(st.enrolledAt).toLocaleDateString('vi-VN')}
                      </div>
                      {st.leftAt && (
                        <div className="text-[10px] text-coral">
                          Nghỉ:{' '}
                          {new Date(st.leftAt).toLocaleDateString('vi-VN')}
                        </div>
                      )}
                    </td>

                    {!isArchived && (
                      <td className="p-3 text-right">
                        {st.status === 'ACTIVE' && (
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => {
                              if (
                                window.confirm(
                                  `Bạn có chắc muốn cho học sinh "${st.studentName}" rời lớp? Học sinh sẽ chuyển sang trạng thái "Đã nghỉ".`,
                                )
                              ) {
                                removeMutation.mutate({
                                  classId: classroom.id,
                                  studentId: st.studentId,
                                });
                              }
                            }}
                            isLoading={removeMutation.isPending}
                            className="text-coral hover:bg-coral hover:text-ink"
                          >
                            <UserMinus className="w-3.5 h-3.5" />
                            Cho nghỉ
                          </Button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modals */}
      <EditClassModal
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        classroom={classroom}
      />

      <ClassInviteModal
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
        classId={classroom.id}
        className={classroom.name}
        activeStudents={activeStudents}
      />

      <EnrollStudentModal
        isOpen={isEnrollOpen}
        onClose={() => setIsEnrollOpen(false)}
        classId={classroom.id}
        className={classroom.name}
        isOneOnOne={isOneOnOne}
        hasActiveStudent={activeStudents.length >= 1}
      />
    </div>
  );
};

export default ClassDetailPage;
