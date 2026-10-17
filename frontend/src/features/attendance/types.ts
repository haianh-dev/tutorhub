export type AttendanceStatus =
  | 'PRESENT'
  | 'LATE'
  | 'ABSENT_EXCUSED'
  | 'ABSENT_UNEXCUSED';

export interface AttendanceRecord {
  id: number;
  sessionId: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  status: AttendanceStatus;
  note?: string | null;
  updatedAt: string;
}

export interface AttendanceItemRequest {
  studentId: number;
  status: AttendanceStatus;
  note?: string;
}

export interface BulkAttendanceRequest {
  records: AttendanceItemRequest[];
}

export interface StudentAttendanceSummary {
  studentId: number;
  studentName: string;
  totalSessions: number;
  presentCount: number;
  lateCount: number;
  absentExcusedCount: number;
  absentUnexcusedCount: number;
  records: AttendanceRecord[];
}
