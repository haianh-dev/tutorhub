export type Role = 'ADMIN' | 'TUTOR' | 'STUDENT' | 'PARENT';

export type UserStatus = 'ACTIVE' | 'DISABLED';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  role: Role;
  status: UserStatus;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface AuthResponse extends AuthTokens {
  user: User;
}

export interface RegisterResponse {
  user: User;
}

export interface InvitationInfo {
  role: 'STUDENT' | 'PARENT';
  email?: string;
  className?: string;
  expiresAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterTutorRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface AcceptInvitationRequest {
  token: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export type ClassType = 'ONE_ON_ONE' | 'GROUP';
export type ClassStatus = 'ACTIVE' | 'ARCHIVED';

export type EnrollmentStatus = 'ACTIVE' | 'LEFT';

export interface Classroom {
  id: number;
  tutorId: number;
  name: string;
  subject: string;
  description?: string;
  classType: ClassType;
  status: ClassStatus;
  studentCount?: number;
  warnings?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface Enrollment {
  id: number;
  classId: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  studentPhone?: string;
  status: EnrollmentStatus;
  enrolledAt: string;
  leftAt?: string;
}

export interface CreateClassRequest {
  name: string;
  subject: string;
  classType: ClassType;
  description?: string;
  tutorId?: number;
}

export interface UpdateClassRequest {
  name?: string;
  subject?: string;
  classType?: ClassType;
  description?: string;
}

export interface EnrollStudentRequest {
  studentId: number;
}

export interface CreateInvitationRequest {
  role: 'STUDENT' | 'PARENT';
  email?: string;
  classId?: number;
  studentId?: number;
}

export interface InvitationResponse {
  link: string;
  role: 'STUDENT' | 'PARENT';
  email?: string;
  className?: string;
  expiresAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export type SessionStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface Session {
  id: number;
  classId: number;
  className?: string;
  tutorId: number;
  ruleId?: number;
  startAt: string;
  endAt: string;
  status: SessionStatus;
  topic?: string;
  note?: string;
  createdAt?: string;
  updatedAt?: string;
  warnings?: string[];
}

export interface CreateSessionRequest {
  startAt: string;
  endAt: string;
  topic?: string;
  note?: string;
}

export interface UpdateSessionRequest {
  startAt?: string;
  endAt?: string;
  topic?: string;
  note?: string;
}

export interface ScheduleRule {
  id: number;
  classId: number;
  dayOfWeek: number; // 1 (Mon) - 7 (Sun)
  startTime: string; // HH:mm:ss
  endTime: string;
  effectiveFrom: string; // YYYY-MM-DD
  effectiveTo?: string;
}

export interface CreateScheduleRuleRequest {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface GenerateSessionsRequest {
  from: string;
  to: string;
}

export interface SessionConflictItem {
  date: string;
  conflictingSessionId?: number;
  reason: string;
}

export interface GenerateSessionsResponse {
  created: number;
  conflicts: SessionConflictItem[];
  warnings?: string[];
}

export type AttendanceStatus = 'PRESENT' | 'LATE' | 'ABSENT_EXCUSED' | 'ABSENT_UNEXCUSED';

export type AssignmentType = 'HOMEWORK' | 'QUIZ' | 'EXAM' | 'MOCK_TEST' | 'OTHER';
export type AssignmentStatus = 'ASSIGNED' | 'SUBMITTED' | 'GRADED' | 'MISSING';

export interface Assignment {
  id: number;
  classId: number;
  title: string;
  description?: string;
  type: AssignmentType;
  dueAt?: string;
  createdAt: string;
}

export interface AssignmentScore {
  id: number;
  assignmentId: number;
  studentId: number;
  status: AssignmentStatus;
  score?: number; // Thang 10 (0.00 <= score <= 10.00), không dùng hệ số (D-30)
  feedback?: string;
  gradedAt?: string;
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  code?: string;
  timestamp?: string;
  conflictingSessionId?: number;
  errors?: ApiFieldError[];
}
