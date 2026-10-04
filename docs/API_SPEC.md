# API_SPEC — TutorHub v1 (DRAFT)

Base URL: `/api/v1` · JSON · Auth: `Authorization: Bearer <accessToken>` (trừ nhóm Auth).
Thời gian: ISO-8601 UTC (`2026-10-05T11:00:00Z`). Hệ thống không xử lý tiền (D-21). Phân trang: `?page=0&size=20&sort=field,desc` → `{ "content": [...], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`.
Lỗi: RFC 7807 (xem ARCHITECTURE.md §6). Mọi endpoint TUTOR đều tự lọc theo người đăng nhập (BR-1); vai trò ADMIN có toàn quyền truy cập/bỏ qua ownership filter.

Mã lỗi chung: 400 validation · 401 chưa đăng nhập · 403 sai vai trò · 404 không tồn tại/không thuộc quyền · 409 xung đột · 422 vi phạm nghiệp vụ.

## 1. Auth
| Method | Path | Role | Mô tả |
|---|---|---|---|
| POST | `/auth/register-tutor` | public | `{email, password, fullName, phone?}` → 201 `{user}` |
| POST | `/auth/login` | public | `{email, password}` → 200 `{accessToken, refreshToken, user}` |
| POST | `/auth/refresh` | public | `{refreshToken}` → 200 `{accessToken, refreshToken}`; rotate token cũ, token cũ không dùng lại được |
| POST | `/auth/logout` | any | `{refreshToken}` → 204; chỉ thu hồi token thuộc user hiện tại |
| POST | `/auth/accept-invitation` | public | `{token, password, fullName, phone?}` → 201, tự đăng nhập |
| GET | `/auth/invitations/{token}` | public | kiểm tra lời mời còn hiệu lực → `{role, email, className?}` |
| POST | `/auth/reset-password` | public | `{token, newPassword}` → 200; token dùng một lần, hết hạn/đã dùng → 410; thu hồi mọi refresh token của user |
| GET | `/me` | any | thông tin người dùng hiện tại |
| PUT | `/me/password` | any | `{currentPassword, newPassword}` → 204; mật khẩu hiện tại sai → 401; thu hồi mọi refresh token |

## 2. Lời mời và liên kết
| Method | Path | Role | Mô tả |
|---|---|---|---|
| POST | `/invitations` | TUTOR/ADMIN | `{role: STUDENT\|PARENT, email?, classId?, studentId?}` → 201 `{link, role, email?, className?, expiresAt}`; TTL 7 ngày; PARENT bắt buộc `studentId` (học sinh trong lớp của TUTOR); TUTOR chỉ mời trong lớp ACTIVE của mình; ADMIN không giới hạn |
| GET | `/auth/invitations/{token}` | public | kiểm tra lời mời → `{role, email?, className?, expiresAt}`; hết hạn/đã dùng → 410 |
| POST | `/auth/accept-invitation` | public | `{token, password, fullName, phone?}` → 201 `{accessToken, refreshToken, user}`; tạo tài khoản STUDENT/PARENT; STUDENT + classId → ghi danh; PARENT + studentId → liên kết parent_students; token dùng 1 lần |
| POST | `/users/{id}/password-reset-link` | TUTOR/ADMIN | tạo link đặt lại mật khẩu (TUTOR: chỉ STUDENT trong lớp đang dạy hoặc PARENT đã liên kết với học sinh trong lớp đó; ADMIN: mọi user) → 200 `{link, expiresAt}`; TTL mặc định 30 phút, token chỉ lưu hash, link gửi thủ công qua Zalo/tin nhắn |

## 3. Classes & Enrollments
| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/classes?status=&q=` | TUTOR/STUDENT/PARENT/ADMIN | danh sách lớp theo quyền (PARENT: thêm `?studentId=`; ADMIN: xem tất cả lớp) |
| POST | `/classes` | TUTOR/ADMIN | `{name, subject, classType: ONE_ON_ONE\|GROUP, description?, tutorId?}` → 201 (ADMIN bắt buộc truyền `tutorId`; TUTOR tự lấy từ token; cảnh báo nếu GROUP < 2 HS) |
| GET | `/classes/{id}` | theo quyền | chi tiết + số học sinh |
| PUT | `/classes/{id}` | TUTOR/ADMIN | cập nhật; đổi `GROUP` → `ONE_ON_ONE` khi có ≥ 2 học sinh đang học → 422 |
| POST | `/classes/{id}/archive` | TUTOR/ADMIN | lưu trữ lớp |
| GET | `/classes/{id}/students` | TUTOR/ADMIN | danh sách ghi danh |
| POST | `/classes/{id}/students` | TUTOR/ADMIN | `{studentId}` → 201; trùng → 409; lớp 1:1 đã có 1 học sinh đang học → 422 `ONE_ON_ONE_FULL` |
| DELETE | `/classes/{id}/students/{studentId}` | TUTOR/ADMIN | đặt enrollment `LEFT` |

## 4. Schedule & Sessions
| Method | Path | Role | Mô tả |
|---|---|---|---|
| POST | `/classes/{id}/schedule-rules` | TUTOR | `{dayOfWeek, startTime, endTime, effectiveFrom, effectiveTo?}` |
| POST | `/classes/{id}/schedule-rules/{ruleId}/generate` | TUTOR | `{from, to}` → tạo buổi; trả `{created: n, conflicts: [{date, conflictingSessionId}]}` (buổi không xung đột vẫn được tạo, xung đột được báo lại) |
| POST | `/classes/{id}/sessions` | TUTOR | `{startAt, endAt, topic?, note?}` → 201; trùng → 409 `SESSION_CONFLICT` |
| PUT | `/sessions/{id}` | TUTOR | sửa giờ/chủ đề/ghi chú; re-check trùng |
| POST | `/sessions/{id}/complete` | TUTOR | đánh dấu COMPLETED |
| POST | `/sessions/{id}/cancel` | TUTOR | đánh dấu CANCELLED |
| GET | `/sessions?from=&to=&classId=` | TUTOR/STUDENT/PARENT | lịch theo khoảng ngày (PARENT: `&studentId=`) |

## 5. Attendance
| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/sessions/{id}/attendance` | TUTOR | danh sách học sinh + trạng thái |
| PUT | `/sessions/{id}/attendance` | TUTOR | bulk `{records:[{studentId,status,note?}]}` trong 1 transaction |
| GET | `/students/{studentId}/attendance?classId=&from=&to=` | theo quyền | lịch sử điểm danh |

## 6. Assignments & Scores
| Method | Path | Role | Mô tả |
|---|---|---|---|
| POST | `/classes/{id}/assignments` | TUTOR | `{title, description?, type: HOMEWORK\|QUIZ\|EXAM\|MOCK_TEST\|OTHER, dueAt?}` → 201, tự tạo score rows (thang điểm 10 cố định, không dùng hệ số) |
| GET | `/classes/{id}/assignments` | theo quyền | danh sách |
| PUT | `/assignments/{id}` | TUTOR | sửa |
| DELETE | `/assignments/{id}` | TUTOR | chỉ khi chưa có điểm đã chấm, ngược lại 422 |
| GET | `/assignments/{id}/scores` | TUTOR | bảng điểm cả lớp |
| PUT | `/assignments/{id}/scores` | TUTOR | bulk `{scores:[{studentId, score?, status, feedback?}]}`; `score < 0` hoặc `score > 10` → 400 |
| GET | `/students/{studentId}/scores?classId=` | theo quyền | điểm của một học sinh (thang điểm 10) |

## 7. Tuition (đợt học phí — chỉ tích đã/chưa nộp, KHÔNG có trường tiền)
| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/classes/{id}/students/{studentId}/tuition` | theo quyền | `{totalSessions, used, remaining, currentCycleId, cycles:[{id, sessionsTotal, usedInCycle, status: PAID\|UNPAID, paidMarkedAt, note}], alerts:[...]}` |
| POST | `/classes/{id}/students/{studentId}/tuition-cycles` | TUTOR | `{sessionsTotal, note?}` → 201, trạng thái `UNPAID`; `sessionsTotal <= 0` → 400 |
| PUT | `/tuition-cycles/{id}` | TUTOR | `{sessionsTotal?, note?}` |
| POST | `/tuition-cycles/{id}/mark-paid` | TUTOR | tích **đã nộp** (lặp lại không lỗi) |
| POST | `/tuition-cycles/{id}/mark-unpaid` | TUTOR | bỏ tích |
| POST | `/tuition-cycles/{id}/void` | TUTOR | hủy đợt (soft) |
| GET | `/classes/{id}/tuition-summary` | TUTOR | bảng tổng hợp cả lớp; mỗi học sinh có `alerts` |

Mã cảnh báo (`alerts[].code`): `CYCLE_LOW` (đợt hiện tại còn ≤ 2 buổi), `CYCLE_DONE_UNPAID` (đã học đủ N buổi mà chưa nộp), `NO_OPEN_CYCLE` (học vượt tổng số buổi các đợt, chưa mở đợt mới). Gia sư tự nhắn Zalo cho phụ huynh; hệ thống không gửi tin.

## 8. Reports
| Method | Path | Role | Mô tả |
|---|---|---|---|
| POST | `/reports` | TUTOR | `{classId, studentId, periodStart, periodEnd, tutorComment?}` → 201 DRAFT (kèm số liệu tính sẵn: tỉ lệ đi học, điểm TB không hệ số thang 10, buổi còn lại). Khoảng ngày tự chọn; `periodEnd < periodStart` → 400 |
| GET | `/reports/{id}` | theo quyền | PARENT chỉ thấy PUBLISHED của con |
| PUT | `/reports/{id}` | TUTOR | sửa khi DRAFT |
| POST | `/reports/{id}/publish` | TUTOR | chốt snapshot, chuyển PUBLISHED |
| GET | `/reports?studentId=&classId=` | theo quyền | danh sách |
| GET | `/reports/{id}/export.pdf` | theo quyền | PDF |
| GET | `/classes/{id}/export/attendance.csv` | TUTOR | CSV điểm danh |
| GET | `/classes/{id}/export/scores.csv` | TUTOR | CSV điểm |
| GET | `/classes/{id}/export/tuition.csv` | TUTOR | CSV đợt học phí (đã/chưa nộp) |

## 9. Portal (học sinh/phụ huynh)
| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/portal/dashboard?studentId=` | STUDENT/PARENT | buổi sắp tới, bài sắp hạn, điểm gần đây, buổi còn lại, báo cáo mới (STUDENT bỏ qua `studentId`) |
| GET | `/portal/children` | PARENT | danh sách con |

## 10. Ví dụ
`POST /api/v1/classes/12/sessions`
```json
{ "startAt": "2026-10-05T11:00:00Z", "endAt": "2026-10-05T12:30:00Z", "topic": "Hàm số mũ" }
```
→ `201`
```json
{ "id": 301, "classId": 12, "startAt": "2026-10-05T11:00:00Z", "endAt": "2026-10-05T12:30:00Z", "status": "SCHEDULED", "topic": "Hàm số mũ" }
```
→ `409`
```json
{ "title": "Schedule conflict", "status": 409, "code": "SESSION_CONFLICT",
  "detail": "Trùng với buổi #288 (Lớp Lý 11B, 12:00–13:00 UTC).", "conflictingSessionId": 288 }
```

> Khi API thay đổi, cập nhật file này **trước** khi sửa code; cập nhật type ở `frontend/src/types`.
