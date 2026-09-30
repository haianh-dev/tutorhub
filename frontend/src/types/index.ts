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
  updatedAt: string;
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

export interface ApiProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  code?: string;
  timestamp?: string;
  invalidParams?: Array<{
    field: string;
    message: string;
  }>;
}
