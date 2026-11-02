export type AssignmentType =
  | 'HOMEWORK'
  | 'QUIZ'
  | 'EXAM'
  | 'MOCK_TEST'
  | 'OTHER';

export type SubmissionStatus =
  | 'ASSIGNED'
  | 'SUBMITTED'
  | 'GRADED'
  | 'MISSING';

export interface Assignment {
  id: number;
  classId: number;
  className: string;
  title: string;
  description?: string | null;
  type: AssignmentType;
  dueAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AssignmentScore {
  id: number;
  assignmentId: number;
  assignmentTitle: string;
  classId?: number | null;
  className?: string | null;
  studentId: number;
  studentName: string;
  studentEmail: string;
  score?: number | null;
  status: SubmissionStatus;
  feedback?: string | null;
  gradedAt?: string | null;
  updatedAt: string;
}

export interface CreateAssignmentRequest {
  title: string;
  description?: string | null;
  type: AssignmentType;
  dueAt?: string | null;
}

export interface UpdateAssignmentRequest {
  title: string;
  description?: string | null;
  type: AssignmentType;
  dueAt?: string | null;
}

export interface ScoreItemRequest {
  studentId: number;
  score?: number | null;
  status: SubmissionStatus;
  feedback?: string | null;
}

export interface BulkUpdateScoresRequest {
  scores: ScoreItemRequest[];
}

export const ASSIGNMENT_TYPE_LABELS: Record<AssignmentType, string> = {
  HOMEWORK: 'Bài tập về nhà',
  QUIZ: 'Kiểm tra 15p',
  EXAM: 'Thi giữa / cuối kỳ',
  MOCK_TEST: 'Đề thi thử',
  OTHER: 'Bài tập khác',
};

export const SUBMISSION_STATUS_LABELS: Record<SubmissionStatus, string> = {
  ASSIGNED: 'Đã giao bài',
  SUBMITTED: 'Đã nộp bài',
  GRADED: 'Đã chấm điểm',
  MISSING: 'Chưa nộp / Thiếu',
};
