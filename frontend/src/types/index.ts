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

export interface Classroom {
  id: number;
  tutorId: number;
  name: string;
  subject: string;
  description?: string;
  classType: ClassType;
  status: ClassStatus;
  studentCount?: number;
  createdAt: string;
  updatedAt: string;
}

export type SessionStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface Session {
  id: number;
  classId: number;
  tutorId: number;
  startAt: string;
  endAt: string;
  status: SessionStatus;
  topic?: string;
  note?: string;
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
  errors?: ApiFieldError[];
}
