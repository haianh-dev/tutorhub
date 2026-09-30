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
| POST | `/auth/refresh` | public | `{refreshToken}` → 200 `{accessToken, refreshToken}` |
| POST | `/auth/logout` | any | thu hồi refresh token |
| POST | `/auth/accept-invitation` | public | `{token, password, fullName, phone?}` → 201, tự đăng nhập |
| GET | `/auth/invitations/{token}` | public | kiểm tra lời mời còn hiệu lực → `{role, email, className?}` |
| GET | `/me` | any | thông tin người dùng hiện tại |
| PUT | `/me/password` | any | `{currentPassword, newPassword}` |

## 2. Lời mời và liên kết
| Method | Path | Role | Mô tả |
|---|---|---|---|
| POST | `/invitations` | TUTOR | `{role: STUDENT|PARENT, email?, classId? , studentId?}` → `{link, expiresAt}`. PARENT bắt buộc `studentId` là học sinh trong lớp của gia sư |

## 3. Classes & Enrollments
| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/classes?status=&q=` | TUTOR/STUDENT/PARENT | danh sách lớp theo quyền (PARENT: thêm `?studentId=`) |
| POST | `/classes` | TUTOR | `{name, subject, classType: ONE_ON_ONE\|GROUP, description?}` → 201 (kèm `warnings` nếu GROUP dưới 2 học sinh) |
| GET | `/classes/{id}` | theo quyền | chi tiết + số học sinh |
| PUT | `/classes/{id}` | TUTOR | cập nhật; đổi `GROUP` → `ONE_ON_ONE` khi có ≥ 2 học sinh đang học → 422 |
| POST | `/classes/{id}/archive` | TUTOR | lưu trữ lớp |
| GET | `/classes/{id}/students` | TUTOR | danh sách ghi danh |
| POST | `/classes/{id}/students` | TUTOR | `{studentId}` → 201; trùng → 409; lớp 1:1 đã có 1 học sinh đang học → 422 `ONE_ON_ONE_FULL` |
| DELETE | `/classes/{id}/students/{studentId}` | TUTOR | đặt enrollment `LEFT` |

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
| POST | `/classes/{id}/assignments` | TUTOR | `{title, description?, type, dueAt?, maxScore, weight?}` → 201, tự tạo score rows |
| GET | `/classes/{id}/assignments` | theo quyền | danh sách |
| PUT | `/assignments/{id}` | TUTOR | sửa |
| DELETE | `/assignments/{id}` | TUTOR | chỉ khi chưa có điểm đã chấm, ngược lại 422 |
| GET | `/assignments/{id}/scores` | TUTOR | bảng điểm cả lớp |
| PUT | `/assignments/{id}/scores` | TUTOR | bulk `{scores:[{studentId, score?, status, feedback?}]}`; `score > maxScore` → 400 |
| GET | `/students/{studentId}/scores?classId=` | theo quyền | điểm của một học sinh |

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
| POST | `/reports` | TUTOR | `{classId, studentId, periodStart, periodEnd, tutorComment?}` → 201 DRAFT (kèm số liệu tính sẵn để xem trước). Khoảng ngày tự chọn; `periodEnd < periodStart` → 400 |
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
