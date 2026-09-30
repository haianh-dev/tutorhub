# AGENTS.md — Hướng dẫn cho AI làm việc trong repo TutorHub

## Dự án
TutorHub: web quản lý lớp học và gia sư (Java Spring Boot + React/TypeScript/Tailwind + PostgreSQL). Vai trò: TUTOR, STUDENT, PARENT.
**Định vị [CONFIRMED]:** công cụ hỗ trợ gia sư quản lý lịch và lớp; không phải sàn kết nối gia sư; không xử lý tiền (học phí chỉ có ô tích đã nộp/chưa nộp, trao đổi qua Zalo/tin nhắn ngoài hệ thống). Lớp có loại 1:1 hoặc nhóm (từ 2 học sinh). Báo cáo theo khoảng ngày tự chọn.
Đọc theo thứ tự: `docs/PROJECT_BRIEF.md` → `docs/REQUIREMENTS.md` → `docs/ARCHITECTURE.md` → `docs/API_SPEC.md` → `docs/ROADMAP.md` → `docs/TEST_PLAN.md` → `docs/DECISIONS.md`.
Chỉ đọc những tài liệu liên quan đến task hiện tại, nhưng luôn đọc `DECISIONS.md` và phần task trong `ROADMAP.md`.

## Người làm và cách giao tiếp
- Người dùng là sinh viên CNTT, biết Java cơ bản/OOP, đang học Spring Boot. Trả lời bằng tiếng Việt, giữ nguyên thuật ngữ kỹ thuật tiếng Anh.
- Mục tiêu kép: hoàn thiện sản phẩm **và** hiểu code. Với phần quan trọng, giải thích luồng dữ liệu, lý do chọn cách làm, các trường hợp lỗi, cách debug. Không chỉ đưa code.

## Quy trình cho mỗi task
1. Đọc tài liệu liên quan, code hiện có, xác định tiêu chí nghiệm thu (trong REQUIREMENTS/ROADMAP).
2. Nêu ngắn gọn kế hoạch: file sẽ tạo/sửa, ảnh hưởng. **Chờ người dùng duyệt** nếu task có thay đổi kiến trúc, schema, API, dependency.
3. Chỉ làm đúng phạm vi task. Không viết lại module không liên quan. Không thêm dependency khi chưa có lý do rõ ràng và chưa được duyệt.
4. Viết/cập nhật test (hợp lệ, dữ liệu sai, trái quyền).
5. Chạy kiểm tra, báo **chính xác** lệnh đã chạy và kết quả. Không khẳng định "xong" nếu chưa có bằng chứng.
6. Cập nhật tài liệu bị ảnh hưởng (API_SPEC, ARCHITECTURE, ROADMAP, DECISIONS).
7. Nếu phát hiện mâu thuẫn yêu cầu hoặc cần đổi thiết kế: **dừng và hỏi**.

## Lệnh chuẩn
```
docker compose up -d db                 # chạy PostgreSQL
cd backend  && mvn verify               # build + test backend (Testcontainers cần Docker)
cd backend  && mvn spring-boot:run      # chạy API (profile dev)
cd frontend && npm install && npm run dev
cd frontend && npm run lint && npm test && npm run build
```
(Cập nhật mục này khi lệnh thực tế khác.)

## Quy ước backend
- Package theo tính năng: `com.tutorhub.<feature>` với `controller`, `service`, `repository`, `dto`, `entity`.
- Controller mỏng: nhận DTO, gọi service, trả DTO. **Không** trả entity ra API. DTO dùng Java `record`.
- Nghiệp vụ và **kiểm tra quyền sở hữu** nằm ở service. Không tin `tutorId`/`studentId` từ client; lấy từ token.
- `@Transactional` ở service cho thao tác ghi nhiều bảng (điểm danh bulk, nhập điểm bulk, publish báo cáo).
- Validation bằng Bean Validation; lỗi qua `@RestControllerAdvice` trả RFC 7807 + `code` ổn định.
- Thay đổi schema **chỉ** bằng migration Flyway mới `V{n}__mo_ta.sql`. Không sửa migration đã áp dụng. Ràng buộc quan trọng đặt ở DB.
- Thời gian dùng `Instant`/`timestamptz`; không dùng `Date`. Tiền dùng `long`/`BigDecimal`, không dùng `double`.
- Chống trùng buổi học dựa vào exclusion constraint (D-10); không thay bằng kiểm tra ở code.
- Không dùng `SELECT *` trong truy vấn báo cáo; tránh N+1 (dùng fetch join/projection).
- Tên: lớp `PascalCase`, phương thức/biến `camelCase`, hằng `UPPER_SNAKE`, bảng/cột `snake_case`.

## Quy ước frontend
- TypeScript `strict`; không dùng `any` trừ khi có lý do ghi chú.
- Gọi API qua `src/api` (axios client + TanStack Query hooks); không `fetch` rải rác trong component.
- Form dùng React Hook Form + Zod; hiển thị lỗi tiếng Việt theo `code` từ backend.
- Route guard theo role chỉ để UX; **quyền thật do backend thực thi**.
- Tailwind utility; component dùng chung ở `components/`. Mọi màn có trạng thái loading / lỗi / rỗng. Responsive từ 375px.
- Type API khớp `docs/API_SPEC.md`.

## Bảo mật và dữ liệu
- Không commit secret, `.env`, dữ liệu thật. Cấu hình qua biến môi trường; cập nhật `.env.example` khi thêm biến.
- Không đưa API key, mật khẩu, token, dữ liệu học sinh thật vào prompt, code mẫu, tài liệu, log.
- Dữ liệu seed/test chỉ dùng dữ liệu giả (`@example.com`).
- Mật khẩu chỉ lưu BCrypt. Không log mật khẩu/token.

## Git
- Mỗi task một branch `feat/T3.2-session-conflict`; commit nhỏ, message rõ (`feat(schedule): reject overlapping sessions`).
- Kiểm tra `git diff` trước khi commit. Không `git push --force`, không xóa branch/lịch sử khi chưa được yêu cầu.

## Việc cần hỏi trước khi làm
Xóa file, thêm/đổi dependency, migration phá hủy dữ liệu, thay đổi API contract hoặc kiến trúc, lệnh deploy, bất cứ thứ gì nằm ngoài phạm vi task.

## Điều tuyệt đối không làm
- Không xóa/skip/làm yếu test chỉ để build xanh.
- Không biến `PROPOSED` trong DECISIONS thành yêu cầu đã duyệt.
- Không thêm tính năng ngoài phạm vi MVP chỉ vì thú vị về kỹ thuật.
- Không thêm bất kỳ trường hay tính năng nào liên quan đến tiền (số tiền, đơn giá, thanh toán online, hóa đơn, công nợ) hoặc sàn kết nối/tìm kiếm gia sư.
- Không đánh dấu task `DONE` khi chưa có bằng chứng chạy test.

## Trạng thái hiện tại
Xem bảng "Nhật ký trạng thái" cuối `docs/ROADMAP.md`. Cuối mỗi phiên, cập nhật bảng đó và `docs/DECISIONS.md`.
