import React, { useState, useMemo } from 'react';
import {
  GraduationCap,
  X,
  CheckCircle,
  AlertCircle,
  Clock,
  Save,
} from 'lucide-react';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import { Badge } from '../../../components/ui/Badge';
import { useAssignmentScores, useBulkUpdateScores } from '../api';
import {
  ASSIGNMENT_TYPE_LABELS,
  SUBMISSION_STATUS_LABELS,
  type Assignment,
  type SubmissionStatus,
} from '../types';

interface GradeAssignmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  assignment: Assignment;
  onSuccess?: () => void;
}

interface StudentGradeOverride {
  score?: string;
  status?: SubmissionStatus;
  feedback?: string;
}

export const GradeAssignmentModal: React.FC<GradeAssignmentModalProps> = ({
  isOpen,
  onClose,
  assignment,
  onSuccess,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [overrides, setOverrides] = useState<Record<number, StudentGradeOverride>>({});

  const {
    data: scores = [],
    isLoading,
    isError,
    error,
  } = useAssignmentScores(isOpen ? assignment.id : undefined);

  const bulkMutation = useBulkUpdateScores(assignment.id, {
    onSuccess: () => {
      setSuccessMessage('Đã lưu bảng điểm và nhận xét thành công!');
      setErrorMessage(null);
      setOverrides({});
      onSuccess?.();
    },
    onError: (err) => {
      setErrorMessage(err.message || 'Không thể lưu bảng điểm. Vui lòng kiểm tra lại.');
      setSuccessMessage(null);
    },
  });

  // Tính toán dữ liệu hiển thị tức thì
  const items = useMemo(() => {
    return scores.map((item) => {
      const override = overrides[item.studentId] || {};
      const scoreStr =
        override.score !== undefined
          ? override.score
          : item.score !== null && item.score !== undefined
            ? String(item.score)
            : '';

      const status = override.status ?? item.status;
      const feedback =
        override.feedback !== undefined ? override.feedback : item.feedback ?? '';

      // Kiểm tra tính hợp lệ của điểm
      let isScoreInvalid = false;
      let scoreNum: number | null = null;
      if (scoreStr.trim() !== '') {
        const parsed = Number(scoreStr);
        if (Number.isNaN(parsed) || parsed < 0 || parsed > 10) {
          isScoreInvalid = true;
        } else {
          scoreNum = parsed;
        }
      }

      return {
        ...item,
        currentScoreStr: scoreStr,
        currentScoreNum: scoreNum,
        isScoreInvalid,
        currentStatus: status,
        currentFeedback: feedback,
      };
    });
  }, [scores, overrides]);

  // Kiểm tra có dòng nào bị lỗi điểm không
  const hasInvalidScore = useMemo(() => {
    return items.some((i) => i.isScoreInvalid);
  }, [items]);

  // Thống kê tóm tắt
  const stats = useMemo(() => {
    const total = items.length;
    const graded = items.filter((i) => i.currentScoreNum !== null).length;
    const sum = items.reduce(
      (acc, cur) => (cur.currentScoreNum !== null ? acc + cur.currentScoreNum : acc),
      0,
    );
    const avg = graded > 0 ? (sum / graded).toFixed(2) : null;
    return { total, graded, avg };
  }, [items]);

  if (!isOpen) return null;

  const handleScoreChange = (studentId: number, value: string) => {
    setOverrides((prev) => ({
      ...prev,
      [studentId]: {
        ...prev[studentId],
        score: value,
        // Tự động chuyển status sang GRADED nếu nhập điểm hợp lệ
        status:
          value.trim() !== '' && !Number.isNaN(Number(value))
            ? (prev[studentId]?.status ?? 'GRADED')
            : (prev[studentId]?.status ?? 'ASSIGNED'),
      },
    }));
  };

  const handleStatusChange = (studentId: number, status: SubmissionStatus) => {
    setOverrides((prev) => ({
      ...prev,
      [studentId]: {
        ...prev[studentId],
        status,
      },
    }));
  };

  const handleFeedbackChange = (studentId: number, feedback: string) => {
    setOverrides((prev) => ({
      ...prev,
      [studentId]: {
        ...prev[studentId],
        feedback,
      },
    }));
  };

  const handleSetAllStatus = (newStatus: SubmissionStatus) => {
    const newOverrides: Record<number, StudentGradeOverride> = { ...overrides };
    for (const item of items) {
      newOverrides[item.studentId] = {
        ...newOverrides[item.studentId],
        status: newStatus,
      };
    }
    setOverrides(newOverrides);
  };

  const handleSave = () => {
    if (hasInvalidScore) {
      setErrorMessage('Vui lòng sửa các điểm số không hợp lệ (thang điểm 10: từ 0 đến 10).');
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);

    const payload = items.map((item) => ({
      studentId: item.studentId,
      score: item.currentScoreNum,
      status: item.currentStatus,
      feedback: item.currentFeedback.trim() || undefined,
    }));

    bulkMutation.mutate({ scores: payload });
  };

  const handleClose = () => {
    setOverrides({});
    setErrorMessage(null);
    setSuccessMessage(null);
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-ink/60 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="relative w-full max-w-4xl bg-paper border-3 border-ink brut-box p-4 sm:p-6 space-y-5 max-h-[92vh] flex flex-col">
        {/* Modal Header */}
        <div className="flex items-start justify-between border-b-2 border-ink pb-4">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 bg-yellow border-2 border-ink brut-box flex items-center justify-center shrink-0">
              <GraduationCap className="w-6 h-6 text-ink" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-xl font-heading font-black uppercase tracking-wider text-ink">
                  {assignment.title}
                </h2>
                <Badge variant="blue">
                  {ASSIGNMENT_TYPE_LABELS[assignment.type]}
                </Badge>
              </div>
              <p className="text-xs font-mono font-bold text-ink/70 mt-0.5">
                Lớp: {assignment.className}
                {assignment.dueAt && (
                  <span className="ml-3 inline-flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" /> Hạn nộp:{' '}
                    {new Date(assignment.dueAt).toLocaleDateString('vi-VN')}
                  </span>
                )}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="p-1.5 text-ink/60 hover:text-ink hover:bg-cream border-2 border-transparent hover:border-ink transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Thông báo Alert */}
        {errorMessage && (
          <Alert variant="error" title="Lỗi cập nhật">
            {errorMessage}
          </Alert>
        )}
        {successMessage && (
          <Alert variant="info" title="Thành công">
            {successMessage}
          </Alert>
        )}

        {/* Thanh công cụ & Thống kê nhanh */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-cream p-3 border-2 border-ink brut-box">
          <div className="flex items-center gap-4 text-xs font-heading font-bold uppercase flex-wrap">
            <span>
              Sĩ số: <strong className="font-mono text-sm">{stats.total}</strong>
            </span>
            <span className="text-ink/30">|</span>
            <span>
              Đã chấm: <strong className="font-mono text-sm">{stats.graded}</strong> / {stats.total}
            </span>
            <span className="text-ink/30">|</span>
            <span>
              Điểm TB:{' '}
              <strong className="font-mono text-sm text-ink bg-yellow px-1.5 py-0.5 border border-ink">
                {stats.avg !== null ? `${stats.avg} / 10` : 'Chưa có'}
              </strong>
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => handleSetAllStatus('SUBMITTED')}
              className="text-xs font-heading font-bold uppercase px-2.5 py-1.5 bg-paper hover:bg-yellow border-2 border-ink transition-colors"
            >
              Tất cả đã nộp
            </button>
            <button
              type="button"
              onClick={() => handleSetAllStatus('GRADED')}
              className="text-xs font-heading font-bold uppercase px-2.5 py-1.5 bg-paper hover:bg-yellow border-2 border-ink transition-colors"
            >
              Tất cả đã chấm
            </button>
          </div>
        </div>

        {/* Body danh sách học sinh */}
        <div className="flex-1 overflow-y-auto border-2 border-ink brut-box bg-paper">
          {isLoading ? (
            <div className="p-8 text-center font-mono text-sm text-ink/70">
              Đang tải danh sách học sinh và điểm...
            </div>
          ) : isError ? (
            <div className="p-6">
              <Alert variant="error" title="Không thể tải bảng điểm">
                {error?.message || 'Lỗi khi tải bảng điểm.'}
              </Alert>
            </div>
          ) : items.length === 0 ? (
            <div className="p-8 text-center font-mono text-sm text-ink/70">
              Lớp học này hiện chưa có học sinh nào.
            </div>
          ) : (
            <div className="divide-y-2 border-ink">
              {/* Bảng Table Header (chỉ hiện trên màn hình md trở lên) */}
              <div className="hidden md:grid md:grid-cols-12 gap-3 p-3 bg-cream/60 font-heading font-black text-xs uppercase tracking-wider text-ink border-b-2 border-ink">
                <div className="col-span-4">Học sinh</div>
                <div className="col-span-2 text-center">Điểm số (0 - 10)</div>
                <div className="col-span-3">Trạng thái nộp</div>
                <div className="col-span-3">Nhận xét của gia sư</div>
              </div>

              {/* Bảng Table Rows */}
              {items.map((item) => (
                <div
                  key={item.studentId}
                  className="p-3 sm:p-4 grid grid-cols-1 md:grid-cols-12 gap-3 items-center hover:bg-yellow/5 transition-colors"
                >
                  {/* Cột 1: Thông tin học sinh */}
                  <div className="col-span-4 space-y-0.5">
                    <p className="font-heading font-bold text-sm text-ink">
                      {item.studentName}
                    </p>
                    <p className="font-mono text-xs text-ink/60 truncate">
                      {item.studentEmail}
                    </p>
                  </div>

                  {/* Cột 2: Điểm số */}
                  <div className="col-span-2 flex flex-col items-center">
                    <div className="relative w-full max-w-[110px]">
                      <input
                        type="number"
                        step="0.25"
                        min="0"
                        max="10"
                        placeholder="0 - 10"
                        value={item.currentScoreStr}
                        onChange={(e) => handleScoreChange(item.studentId, e.target.value)}
                        className={`w-full h-10 text-center font-mono font-bold text-sm bg-paper border-2 ${
                          item.isScoreInvalid
                            ? 'border-coral bg-coral/10 text-coral'
                            : 'border-ink focus:bg-yellow/15'
                        } brut-box focus:outline-none`}
                      />
                    </div>
                    {item.isScoreInvalid && (
                      <span className="text-[10px] font-mono text-coral font-bold mt-1">
                        Từ 0 đến 10
                      </span>
                    )}
                  </div>

                  {/* Cột 3: Trạng thái nộp */}
                  <div className="col-span-3">
                    <select
                      value={item.currentStatus}
                      onChange={(e) =>
                        handleStatusChange(item.studentId, e.target.value as SubmissionStatus)
                      }
                      className="w-full h-10 px-2 bg-paper border-2 border-ink brut-box font-mono text-xs text-ink focus:outline-none focus:bg-yellow/10"
                    >
                      {(Object.keys(SUBMISSION_STATUS_LABELS) as SubmissionStatus[]).map(
                        (status) => (
                          <option key={status} value={status}>
                            {SUBMISSION_STATUS_LABELS[status]}
                          </option>
                        ),
                      )}
                    </select>
                  </div>

                  {/* Cột 4: Nhận xét */}
                  <div className="col-span-3">
                    <input
                      type="text"
                      placeholder="Ghi nhận xét..."
                      value={item.currentFeedback}
                      onChange={(e) =>
                        handleFeedbackChange(item.studentId, e.target.value)
                      }
                      className="w-full h-10 px-2.5 bg-paper border-2 border-ink brut-box font-mono text-xs text-ink focus:outline-none focus:bg-yellow/10"
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3 border-t-2 border-ink/20">
          <div className="text-xs font-mono text-ink/70">
            {hasInvalidScore ? (
              <span className="text-coral font-bold inline-flex items-center gap-1">
                <AlertCircle className="w-4 h-4" /> Có ô điểm chưa hợp lệ (phải từ 0.00 đến 10.00).
              </span>
            ) : (
              <span className="text-ink/80 inline-flex items-center gap-1">
                <CheckCircle className="w-4 h-4 text-ink" /> Thang điểm 10 cố định (0 - 10), không hệ số theo D-30.
              </span>
            )}
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
            <Button
              type="button"
              variant="secondary"
              onClick={handleClose}
              disabled={bulkMutation.isPending}
            >
              Đóng
            </Button>
            <Button
              type="button"
              variant="primary"
              onClick={handleSave}
              isLoading={bulkMutation.isPending}
              disabled={hasInvalidScore || items.length === 0}
            >
              <Save className="w-4 h-4" /> Lưu bảng điểm
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
