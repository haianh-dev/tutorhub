import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, Sparkles, AlertTriangle, CheckCircle, Trash2 } from 'lucide-react';
import { Button } from '../../../components/ui/Button';
import { Alert } from '../../../components/ui/Alert';
import {
  useScheduleRules,
  useCreateScheduleRule,
  useDeleteScheduleRule,
  useGenerateSessions,
} from '../api';
import { extractScheduleError } from '../errorMessages';
import type { Classroom, GenerateSessionsResponse } from '../../../types';

interface GenerateSessionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  classes: Classroom[];
  initialClassId?: number;
  onSuccess?: () => void;
}

const DAYS_OF_WEEK = [
  { value: 1, label: 'Thứ Hai' },
  { value: 2, label: 'Thứ Ba' },
  { value: 3, label: 'Thứ Tư' },
  { value: 4, label: 'Thứ Năm' },
  { value: 5, label: 'Thứ Sáu' },
  { value: 6, label: 'Thứ Bảy' },
  { value: 7, label: 'Chủ Nhật' },
];

export const GenerateSessionsModal: React.FC<GenerateSessionsModalProps> = ({
  isOpen,
  onClose,
  classes,
  initialClassId,
  onSuccess,
}) => {
  const [selectedClassId, setSelectedClassId] = useState<number>(
    initialClassId || classes[0]?.id || 0,
  );
  const [activeTab, setActiveTab] = useState<'generate' | 'rules'>('generate');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [result, setResult] = useState<GenerateSessionsResponse | null>(null);

  // Queries
  const { data: rules = [], isLoading: isLoadingRules } = useScheduleRules(selectedClassId);

  // Form tạo Rule mới
  const {
    register: registerRule,
    handleSubmit: handleSubmitRule,
    reset: resetRule,
  } = useForm({
    defaultValues: {
      dayOfWeek: 1,
      startTime: '08:00',
      endTime: '09:30',
      effectiveFrom: new Date().toISOString().split('T')[0],
      effectiveTo: '',
    },
  });

  // Form sinh lịch
  const {
    register: registerGen,
    handleSubmit: handleSubmitGen,
  } = useForm({
    defaultValues: {
      ruleId: 0,
      from: new Date().toISOString().split('T')[0],
      to: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30 ngày sau
    },
  });

  const createRuleMutation = useCreateScheduleRule({
    onSuccess: () => {
      resetRule();
      setErrorMessage(null);
    },
    onError: (err) => setErrorMessage(extractScheduleError(err).message),
  });

  const deleteRuleMutation = useDeleteScheduleRule({
    onError: (err) => setErrorMessage(extractScheduleError(err).message),
  });

  const generateMutation = useGenerateSessions({
    onSuccess: (res) => {
      setResult(res);
      setErrorMessage(null);
      onSuccess?.();
    },
    onError: (err) => setErrorMessage(extractScheduleError(err).message),
  });

  if (!isOpen) return null;

  const onAddRule = (values: {
    dayOfWeek: number;
    startTime: string;
    endTime: string;
    effectiveFrom: string;
    effectiveTo: string;
  }) => {
    setErrorMessage(null);
    createRuleMutation.mutate({
      classId: selectedClassId,
      req: {
        dayOfWeek: Number(values.dayOfWeek),
        startTime: `${values.startTime}:00`,
        endTime: `${values.endTime}:00`,
        effectiveFrom: values.effectiveFrom,
        effectiveTo: values.effectiveTo || undefined,
      },
    });
  };

  const onGenerate = (values: { ruleId: number; from: string; to: string }) => {
    if (!values.ruleId) {
      setErrorMessage('Vui lòng chọn quy tắc lịch để sinh buổi.');
      return;
    }
    setErrorMessage(null);
    setResult(null);

    generateMutation.mutate({
      classId: selectedClassId,
      ruleId: Number(values.ruleId),
      req: {
        from: values.from,
        to: values.to,
      },
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/60 backdrop-blur-xs font-body">
      <div className="w-full max-w-2xl bg-paper border-3 border-ink brut-shadow-lg p-6 relative max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 mb-4 border-b-2 border-ink">
          <div className="flex items-center gap-2">
            <Sparkles className="w-6 h-6 text-yellow" />
            <h2 className="font-heading font-black text-xl uppercase tracking-wider text-ink">
              Quy tắc lịch lặp & Sinh buổi tự động
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 border-2 border-ink bg-cream hover:bg-yellow transition-colors cursor-pointer"
          >
            <X className="w-5 h-5 text-ink" />
          </button>
        </div>

        {/* Chọn lớp học */}
        <div className="mb-4">
          <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
            Chọn lớp học:
          </label>
          <select
            value={selectedClassId}
            onChange={(e) => {
              setSelectedClassId(Number(e.target.value));
              setResult(null);
              setErrorMessage(null);
            }}
            className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm font-bold focus:outline-none focus:bg-paper"
          >
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.subject})
              </option>
            ))}
          </select>
        </div>

        {/* Tab switch */}
        <div className="flex border-b-2 border-ink mb-4 gap-2">
          <button
            type="button"
            onClick={() => setActiveTab('generate')}
            className={`px-4 py-2 text-xs font-heading font-bold uppercase border-2 border-b-0 border-ink transition-colors cursor-pointer ${
              activeTab === 'generate' ? 'bg-yellow text-ink -mb-[2px] pb-[10px]' : 'bg-cream text-ink/70'
            }`}
          >
            Sinh buổi học hàng loạt
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('rules')}
            className={`px-4 py-2 text-xs font-heading font-bold uppercase border-2 border-b-0 border-ink transition-colors cursor-pointer ${
              activeTab === 'rules' ? 'bg-yellow text-ink -mb-[2px] pb-[10px]' : 'bg-cream text-ink/70'
            }`}
          >
            Quản lý quy tắc ({rules.length})
          </button>
        </div>

        {errorMessage && (
          <div className="mb-4">
            <Alert variant="error">{errorMessage}</Alert>
          </div>
        )}

        {/* TAB 1: SINH BUỔI */}
        {activeTab === 'generate' && (
          <div className="space-y-4">
            {rules.length === 0 ? (
              <div className="p-6 border-2 border-dashed border-ink bg-cream/50 text-center">
                <p className="text-sm font-bold text-ink">Lớp học này chưa có quy tắc lịch lặp nào.</p>
                <p className="text-xs text-ink/70 mt-1">
                  Vui lòng chuyển sang tab "Quản lý quy tắc" để thêm lịch dạy cố định trước.
                </p>
                <Button
                  type="button"
                  variant="primary"
                  className="mt-3"
                  onClick={() => setActiveTab('rules')}
                >
                  Thêm quy tắc ngay
                </Button>
              </div>
            ) : (
              <form onSubmit={handleSubmitGen(onGenerate)} className="space-y-4">
                <div>
                  <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                    Chọn quy tắc lặp:
                  </label>
                  <select
                    {...registerGen('ruleId', { required: true, valueAsNumber: true })}
                    className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm font-bold focus:outline-none"
                  >
                    <option value={0}>-- Chọn quy tắc lịch --</option>
                    {rules.map((r) => {
                      const dayLabel = DAYS_OF_WEEK.find((d) => d.value === r.dayOfWeek)?.label;
                      return (
                        <option key={r.id} value={r.id}>
                          {dayLabel}: {r.startTime.slice(0, 5)} - {r.endTime.slice(0, 5)} (từ {r.effectiveFrom})
                        </option>
                      );
                    })}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                      Từ ngày:
                    </label>
                    <input
                      type="date"
                      {...registerGen('from', { required: true })}
                      className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                      Đến ngày:
                    </label>
                    <input
                      type="date"
                      {...registerGen('to', { required: true })}
                      className="w-full px-3 py-2 border-3 border-ink bg-cream text-ink text-sm focus:outline-none"
                    />
                  </div>
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  disabled={generateMutation.isPending}
                  className="w-full justify-center"
                >
                  {generateMutation.isPending ? 'Đang phân tích & sinh buổi...' : 'Bắt đầu sinh buổi học'}
                </Button>
              </form>
            )}

            {/* Kết quả sau khi sinh */}
            {result && (
              <div className="mt-4 p-4 border-2 border-ink bg-cream/70 space-y-3">
                <div className="flex items-center gap-2">
                  <CheckCircle className="w-5 h-5 text-green-600" />
                  <p className="font-bold text-sm text-ink">
                    Đã tạo thành công <span className="text-base text-green-700">{result.created}</span> buổi học!
                  </p>
                </div>

                {result.conflicts.length > 0 && (
                  <div className="p-3 border-2 border-ink bg-coral/10">
                    <p className="text-xs font-heading font-bold uppercase tracking-wider text-coral flex items-center gap-1">
                      <AlertTriangle className="w-4 h-4" /> Bỏ qua {result.conflicts.length} buổi bị xung đột:
                    </p>
                    <ul className="text-xs list-disc list-inside mt-1 space-y-1 text-ink">
                      {result.conflicts.map((c, i) => (
                        <li key={i}>
                          <span className="font-mono font-bold">{c.date}</span>: {c.reason}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {result.warnings && result.warnings.length > 0 && (
                  <div className="p-3 border-2 border-ink bg-yellow/20">
                    <p className="text-xs font-heading font-bold uppercase tracking-wider text-ink flex items-center gap-1">
                      <AlertTriangle className="w-4 h-4" /> Cảnh báo trùng lịch học sinh:
                    </p>
                    <ul className="text-xs list-disc list-inside mt-1 space-y-1 text-ink">
                      {result.warnings.map((w, i) => (
                        <li key={i}>{w}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* TAB 2: QUẢN LÝ QUY TẮC */}
        {activeTab === 'rules' && (
          <div className="space-y-6">
            {/* Form thêm quy tắc */}
            <form onSubmit={handleSubmitRule(onAddRule)} className="p-4 border-2 border-ink bg-cream space-y-3">
              <p className="text-xs font-heading font-black uppercase tracking-wider text-ink">
                + Thêm quy tắc lịch mới
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                    Thứ:
                  </label>
                  <select
                    {...registerRule('dayOfWeek', { valueAsNumber: true })}
                    className="w-full px-2 py-1.5 border-2 border-ink bg-paper text-xs font-bold"
                  >
                    {DAYS_OF_WEEK.map((d) => (
                      <option key={d.value} value={d.value}>
                        {d.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                    Giờ bắt đầu:
                  </label>
                  <input
                    type="time"
                    {...registerRule('startTime')}
                    className="w-full px-2 py-1.5 border-2 border-ink bg-paper text-xs"
                  />
                </div>

                <div>
                  <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                    Giờ kết thúc:
                  </label>
                  <input
                    type="time"
                    {...registerRule('endTime')}
                    className="w-full px-2 py-1.5 border-2 border-ink bg-paper text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                    Hiệu lực từ ngày:
                  </label>
                  <input
                    type="date"
                    {...registerRule('effectiveFrom')}
                    className="w-full px-2 py-1.5 border-2 border-ink bg-paper text-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-heading font-bold uppercase tracking-wider text-ink mb-1">
                    Đến ngày (tùy chọn):
                  </label>
                  <input
                    type="date"
                    {...registerRule('effectiveTo')}
                    className="w-full px-2 py-1.5 border-2 border-ink bg-paper text-xs"
                  />
                </div>
              </div>

              <Button
                type="submit"
                variant="primary"
                disabled={createRuleMutation.isPending}
                className="w-full justify-center text-xs"
              >
                {createRuleMutation.isPending ? 'Đang lưu...' : 'Lưu quy tắc'}
              </Button>
            </form>

            {/* Danh sách quy tắc hiện tại */}
            <div className="space-y-2">
              <p className="text-xs font-heading font-bold uppercase tracking-wider text-ink">
                Danh sách quy tắc đã có:
              </p>
              {isLoadingRules ? (
                <p className="text-xs text-ink/60 italic">Đang tải...</p>
              ) : rules.length === 0 ? (
                <p className="text-xs text-ink/60 italic">Chưa có quy tắc nào.</p>
              ) : (
                rules.map((r) => {
                  const dayLabel = DAYS_OF_WEEK.find((d) => d.value === r.dayOfWeek)?.label;
                  return (
                    <div
                      key={r.id}
                      className="flex items-center justify-between p-3 border-2 border-ink bg-paper"
                    >
                      <div>
                        <p className="font-bold text-sm text-ink">
                          {dayLabel}: {r.startTime.slice(0, 5)} - {r.endTime.slice(0, 5)}
                        </p>
                        <p className="text-xs text-ink/70">
                          Hiệu lực: {r.effectiveFrom} {r.effectiveTo ? `đến ${r.effectiveTo}` : '(vô thời hạn)'}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() =>
                          deleteRuleMutation.mutate({ classId: selectedClassId, ruleId: r.id })
                        }
                        className="p-1.5 border-2 border-ink bg-coral/20 hover:bg-coral text-ink transition-colors cursor-pointer"
                        title="Xóa quy tắc"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
