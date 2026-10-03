# ARCHITECTURE — TutorHub

## 1. Công nghệ
| Lớp | Lựa chọn | Trạng thái |
|---|---|---|
| Backend | Java 25 LTS, Spring Boot 3.x, Maven | Java 25 CONFIRMED; Spring Boot/Maven phiên bản ASSUMPTION |
| Web/API | Spring Web (REST, JSON) | ASSUMPTION |
| Bảo mật | Spring Security + JWT (access 15 phút, refresh 7 ngày), BCrypt | ASSUMPTION |
| Dữ liệu | Spring Data JPA (Hibernate), PostgreSQL 16, Flyway | PostgreSQL CONFIRMED; còn lại ASSUMPTION |
| Validation/Lỗi | Bean Validation, `@RestControllerAdvice`, ProblemDetail | ASSUMPTION |
| Tài liệu API | springdoc-openapi (Swagger UI) | ASSUMPTION |
| Xuất file | OpenPDF hoặc openhtmltopdf (PDF), Commons CSV | ASSUMPTION |
| Test backend | JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL) | ASSUMPTION |
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS | React/TS/Tailwind CONFIRMED |
| Frontend libs | React Router, TanStack Query, React Hook Form + Zod, Axios | ASSUMPTION |
| Test frontend | Vitest + React Testing Library (+ Playwright cho E2E sau MVP) | ASSUMPTION |
| Đóng gói | Docker, docker-compose (dev), Dockerfile riêng cho backend/frontend | Docker CONFIRMED |
| Deploy | Render hoặc Railway (backend + Postgres managed + frontend static) | CONFIRMED (chưa chọn 1) |
| CI | GitHub Actions: build + test | ASSUMPTION |

## 2. Kiến trúc tổng thể
```
[React SPA] --HTTPS/JSON--> [Spring Boot API] --JDBC--> [PostgreSQL]
                                  |
                          Security filter (JWT) -> Controller -> Service -> Repository
```
- Monolith module hóa theo tính năng (không dùng microservice).
- Phân lớp: `controller` (HTTP, DTO) → `service` (nghiệp vụ, transaction, kiểm tra quyền sở hữu) → `repository` (truy cập dữ liệu). Entity không trả trực tiếp ra API, luôn qua DTO (record).
- Quy tắc quyền sở hữu nằm ở **service** (ví dụ `ClassAccessPolicy`), không rải trong controller.
- **Vai trò ADMIN:** Được seed tự động khi deploy (từ biến môi trường `ADMIN_EMAIL`, `ADMIN_DEFAULT_PASSWORD`). ADMIN có toàn quyền (bỏ qua ownership filter), xem/quản lý mọi lớp học, tạo link reset mật khẩu cho bất kỳ user nào, và khi tạo lớp học có quyền chỉ định `tutor_id` (gia sư phụ trách).
- JWT filter xác minh chữ ký/expiry rồi nạp user ACTIVE từ DB; role lấy từ DB, không tin role gửi từ client. Refresh/reset token đều lưu SHA-256 hash và dùng pessimistic lock theo thứ tự user → token để chặn replay đồng thời.
- Đổi/đặt lại mật khẩu thu hồi toàn bộ refresh token. Access JWT hiện có là stateless nên còn hiệu lực tối đa đến khi hết TTL 15 phút.
- TUTOR chỉ tạo reset link cho học sinh đang ghi danh trong lớp ACTIVE của mình hoặc phụ huynh đã liên kết với học sinh đó; kiểm tra ownership ở service. ADMIN được bypass ownership theo D-27.

## 3. Cấu trúc repo
```
tutorhub/
├── AGENTS.md
├── README.md
├── docker-compose.yml
├── .env.example
├── docs/                     # các file tài liệu này
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/tutorhub/
│       │   ├── TutorHubApplication.java
│       │   ├── common/        # exception, ProblemDetail, base entity, time utils
│       │   ├── config/        # SecurityConfig, CorsConfig, OpenApiConfig
│       │   ├── auth/          # login, register, invitation, JWT
│       │   ├── user/
│       │   ├── classroom/     # class + enrollment
│       │   ├── schedule/      # recurring rule + session (conflict check)
│       │   ├── attendance/
│       │   ├── assignment/    # assignment + score
│       │   ├── tuition/       # đợt học phí (tích đã/chưa nộp) + buổi còn lại
│       │   └── report/        # progress report + export (PDF/CSV)
│       ├── main/resources/
│       │   ├── application.yml, application-dev.yml, application-prod.yml
│       │   └── db/migration/  # V1__init.sql, V2__...
│       └── test/java/com/tutorhub/...
└── frontend/
    ├── package.json, vite.config.ts, tailwind.config.ts, Dockerfile
    └── src/
        ├── api/               # axios client, hooks theo tính năng
        ├── components/        # UI dùng chung
        ├── features/          # auth, classes, schedule, attendance, assignments, tuition, reports, portal
        ├── routes/            # định tuyến + route guard theo role
        ├── types/
        └── main.tsx
```

## 4. Mô hình dữ liệu (PostgreSQL)

Quy ước: khóa chính `BIGINT GENERATED ALWAYS AS IDENTITY`; thời gian `timestamptz` (UTC); `created_at`, `updated_at` ở mọi bảng; tên bảng số nhiều, snake_case.

V3 tạo `password_reset_tokens`. V4 đưa trước các bảng ownership tối thiểu (`classes`, `class_enrollments`, `parent_students`) để hoàn thành kiểm tra quyền của T1.3; T2.1/T2.2 sẽ bổ sung nghiệp vụ/API lớp học trên các bảng này.

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

users(
  id, email UNIQUE NOT NULL, password_hash NOT NULL, full_name NOT NULL, phone,
  role CHECK (role IN ('ADMIN','TUTOR','STUDENT','PARENT')), status CHECK (status IN ('ACTIVE','DISABLED')),
  created_at, updated_at)

invitations(
  id, token_hash UNIQUE NOT NULL, role CHECK (role IN ('STUDENT','PARENT')),
  invited_by BIGINT REFERENCES users, email,
  class_id BIGINT NULL REFERENCES classes,          -- ghi danh sẵn khi học sinh chấp nhận
  student_id BIGINT NULL REFERENCES users,          -- dùng khi mời PARENT liên kết với con
  expires_at NOT NULL, used_at NULL, created_at)

password_reset_tokens(                             -- link đặt lại mật khẩu do gia sư/ADMIN tạo (không dùng email SMTP ở MVP)
  id, user_id BIGINT NOT NULL REFERENCES users, token_hash UNIQUE NOT NULL,
  expires_at NOT NULL, used_at NULL, created_at)

parent_students(
  parent_id REFERENCES users, student_id REFERENCES users, created_at,
  PRIMARY KEY(parent_id, student_id), CHECK (parent_id <> student_id))

classes(
  id, tutor_id NOT NULL REFERENCES users, name NOT NULL, subject NOT NULL, description,
  class_type NOT NULL CHECK (class_type IN ('ONE_ON_ONE','GROUP')),
  status CHECK (status IN ('ACTIVE','ARCHIVED')), created_at, updated_at)

class_enrollments(
  id, class_id REFERENCES classes, student_id REFERENCES users,
  status CHECK (status IN ('ACTIVE','LEFT')), enrolled_at, left_at, created_at,
  UNIQUE (class_id, student_id))

schedule_rules(
  id, class_id REFERENCES classes, day_of_week SMALLINT CHECK (1..7),
  start_time TIME, end_time TIME CHECK (end_time > start_time),
  effective_from DATE, effective_to DATE NULL)

sessions(
  id, class_id REFERENCES classes, tutor_id NOT NULL REFERENCES users,  -- denormalized để đặt exclusion constraint
  rule_id NULL REFERENCES schedule_rules,
  start_at NOT NULL, end_at NOT NULL, CHECK (end_at > start_at),
  status CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED')), topic, note,
  EXCLUDE USING gist (tutor_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)
          WHERE (status <> 'CANCELLED'))

attendance(
  id, session_id REFERENCES sessions, student_id REFERENCES users,
  status CHECK (status IN ('PRESENT','LATE','ABSENT_EXCUSED','ABSENT_UNEXCUSED')), note,
  UNIQUE (session_id, student_id))

assignments(
  id, class_id REFERENCES classes, title NOT NULL, description,
  type CHECK (type IN ('HOMEWORK','QUIZ','EXAM','MOCK_TEST','OTHER')), due_at, created_at) -- Thang điểm 10 cố định, không dùng hệ số (D-30)

assignment_scores(
  id, assignment_id REFERENCES assignments, student_id REFERENCES users,
  status CHECK (status IN ('ASSIGNED','SUBMITTED','GRADED','MISSING')),
  score NUMERIC(4,2) NULL CHECK (score >= 0 AND score <= 10), feedback, graded_at NULL,
  UNIQUE (assignment_id, student_id))

tuition_cycles(                      -- đợt học phí; KHÔNG có cột tiền (D-21)
  id, enrollment_id REFERENCES class_enrollments,
  sessions_total INT NOT NULL CHECK (sessions_total > 0),
  status CHECK (status IN ('UNPAID','PAID')) DEFAULT 'UNPAID', paid_marked_at NULL,
  note, voided_at NULL, created_at, updated_at)

progress_reports(
  id, class_id, student_id, period_start DATE, period_end DATE CHECK (period_end >= period_start),
  tutor_comment, status CHECK (status IN ('DRAFT','PUBLISHED')),
  snapshot JSONB NULL,               -- dữ liệu chốt khi PUBLISHED
  created_by REFERENCES users, published_at NULL, created_at)
```

Index gợi ý: `sessions(class_id, start_at)`, `attendance(student_id)`, `assignment_scores(student_id)`, `class_enrollments(student_id)`, `classes(tutor_id)`.

**Chống trùng buổi học:** exclusion constraint ở trên là chốt chặn cuối cùng (an toàn khi đồng thời). Service bắt `DataIntegrityViolationException` và trả 409 kèm thông tin buổi xung đột (truy vấn lại). Service kiểm tra trước để trả thông báo thân thiện, nhưng **không** dựa vào kiểm tra trước làm cơ chế duy nhất.

**Loại lớp:** `ONE_ON_ONE` tối đa 1 enrollment `ACTIVE`; `GROUP` không giới hạn (dưới 2 học sinh chỉ trả `warnings`). Quy tắc 1:1 kiểm tra trong service, trong cùng transaction với việc ghi danh và khóa dòng lớp (`SELECT ... FOR UPDATE` / `PESSIMISTIC_WRITE`) để hai request thêm học sinh đồng thời không cùng lọt qua.

**Buổi còn lại và đợt học phí (truy vấn, không lưu cột dẫn xuất):**
```sql
total_sessions = SUM(sessions_total) FROM tuition_cycles WHERE enrollment_id=? AND voided_at IS NULL
used           = COUNT(*) FROM attendance a JOIN sessions s ON s.id=a.session_id
                 WHERE s.class_id=? AND a.student_id=? AND s.status='COMPLETED'
                   AND a.status IN ('PRESENT','LATE','ABSENT_UNEXCUSED')
remaining      = total_sessions - used
```
Phân bổ `used` vào từng đợt (đợt tạo trước đầy trước) làm ở service (hàm thuần, dễ unit test), theo công thức $u_k = \min\big(N_k,\ \max(0,\ U - \sum_{j<k} N_j)\big)$. Cảnh báo: `CYCLE_LOW`, `CYCLE_DONE_UNPAID`, `NO_OPEN_CYCLE`.

## 5. Xác thực và phân quyền
- Đăng nhập → access JWT (claims: `sub`=userId, `role`). Refresh token lưu băm ở DB (bảng `refresh_tokens`, thêm ở migration của phase auth) để thu hồi được.
- `SecurityFilterChain`: `/api/v1/auth/**` public; còn lại yêu cầu xác thực; `@PreAuthorize` theo role ở mức endpoint.
- Quyền sở hữu (lớp của mình, con của mình) kiểm tra ở service; không tin `tutorId`/`studentId` do client gửi, luôn lấy từ token. Riêng `ROLE_ADMIN` có toàn quyền truy cập/bỏ qua bộ lọc ownership.
- Frontend chỉ ẩn/hiện UI theo role (UX); **backend là nơi thực thi quyền**.

## 6. Xử lý lỗi
Một định dạng lỗi duy nhất (RFC 7807):
```json
{ "type": "about:blank", "title": "Schedule conflict", "status": 409,
  "detail": "Buổi học trùng với lớp Toán 12A (18:00–19:30).",
  "code": "SESSION_CONFLICT", "errors": [{"field":"startAt","message":"..."}] }
```
Mã `code` ổn định để frontend hiển thị thông báo tiếng Việt. Không lộ stack trace ra ngoài.

## 7. Logging và cấu hình
- Log dạng dòng, có `requestId`; không log mật khẩu, token, dữ liệu học sinh đầy đủ.
- Profile: `dev` (docker compose), `test` (Testcontainers), `prod`.
- Biến môi trường: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`, `CORS_ALLOWED_ORIGINS`, `APP_BASE_URL`. Không commit `.env`.

## 8. Deploy (Render/Railway)
- Backend: Docker image từ `backend/Dockerfile`; Postgres managed; Flyway tự chạy migration khi khởi động.
- Frontend: build tĩnh (`npm run build`) host static, biến `VITE_API_BASE_URL`.
- Health check: `/actuator/health`. Lưu ý gói miễn phí có cold start.
