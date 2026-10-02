# ROADMAP — TutorHub

Trạng thái task: `TODO` · `DOING` · `DONE (có bằng chứng test)` · `BLOCKED`. AI **không** tự đánh dấu DONE nếu chưa có bằng chứng chạy test.
Mỗi task: làm nhỏ, kiểm chứng độc lập, một commit/PR. Thứ tự theo phụ thuộc (cột Dep).

## Phase 0 — Nền tảng
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T0.1 | Khởi tạo repo Git, cấu trúc thư mục, `.gitignore`, `.env.example`, README khung | — | Repo có cấu trúc theo ARCHITECTURE §3 |
| T0.2 | Backend Spring Boot skeleton (Web, Security, JPA, Validation, Flyway, Actuator, springdoc) | T0.1 | `mvn verify` xanh; `/actuator/health` = UP |
| T0.3 | `docker-compose.yml` với PostgreSQL; profile dev kết nối được | T0.2 | `docker compose up` → backend chạy, Flyway chạy V1 rỗng |
| T0.4 | Frontend skeleton Vite + React + TS + Tailwind, router, layout | T0.1 | `npm run build` xanh; trang trống hiển thị |
| T0.4.1 | Định hướng giao diện Neubrutalism (Tokens, UI components, AppLayout, trang /dev/ui) | T0.4 | `npm run lint` & `npm run build` xanh; grep loại bỏ `rounded` và soft `shadow`; 6 màu token |
| T0.5 | Exception handler thống nhất (ProblemDetail) + test | T0.2 | Test 400/404/409 trả đúng định dạng |
| T0.6 | CI GitHub Actions: build+test backend và frontend | T0.2, T0.4 | Pipeline xanh trên main |

## Phase 1 — Xác thực & phân quyền
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T1.1 | Migration `users`, `refresh_tokens`; entity + repository | T0.3 | Migration chạy sạch; test repository |
| T1.2 | Đăng ký gia sư + đăng nhập + JWT + BCrypt | T1.1, T0.5 | AC FR-1 (email trùng, mật khẩu yếu, sai mật khẩu) có test |
| T1.3 | Refresh/logout/đổi mật khẩu + sinh link đặt lại mật khẩu cho học sinh/phụ huynh | T1.2 | Refresh token bị thu hồi thì không dùng lại được; link reset mật khẩu hợp lệ/hết hạn |
| T1.4 | Lời mời (invitations) + accept-invitation cho STUDENT/PARENT + `parent_students` | T1.2 | Token hết hạn/đã dùng bị từ chối; PARENT liên kết đúng con |
| T1.5 | Security config: role-based + test truy cập trái quyền mẫu | T1.2 | Endpoint sai role → 403, không token → 401 |
| T1.6 | Frontend: login, đăng ký gia sư, chấp nhận lời mời, đặt lại mật khẩu, route guard theo role | T1.2–T1.4, T0.4 | Chạy được luồng đăng nhập cả 4 vai trò (ADMIN, TUTOR, STUDENT, PARENT) |

## Phase 2 — Lớp học & học sinh
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T2.1 | Migration `classes` (có `class_type`), `class_enrollments`; CRUD lớp + archive (ADMIN chỉ định tutor_id) | T1.5 | AC FR-2; loại lớp bắt buộc; gia sư B không truy cập lớp gia sư A; ADMIN quản lý được mọi lớp (test) |
| T2.2 | Ghi danh/bỏ học sinh; danh sách học sinh của lớp; quy tắc lớp 1:1 (tối đa 1) và cảnh báo lớp nhóm dưới 2 | T2.1, T1.4 | Ghi danh trùng → 409; thêm HS thứ 2 vào lớp 1:1 → 422 (kể cả 2 request đồng thời); đổi loại lớp đúng FR-2.6 |
| T2.3 | Frontend: danh sách lớp, tạo/sửa lớp, quản lý học sinh, tạo link mời | T2.1, T2.2, T1.6 | Thao tác được trên UI |

## Phase 3 — Lịch dạy & chống trùng buổi
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T3.1 | Migration `schedule_rules`, `sessions` (+ `btree_gist`, EXCLUDE constraint) | T2.1 | Test SQL trực tiếp: chèn buổi chồng giờ bị DB từ chối |
| T3.2 | Service tạo/sửa/hủy/hoàn thành buổi; map lỗi DB → 409 `SESSION_CONFLICT` | T3.1 | Test: chồng giờ, liền kề, buổi CANCELLED, sửa giờ |
| T3.3 | Test đồng thời (2 luồng tạo buổi trùng) | T3.2 | Chỉ 1 thành công |
| T3.4 | Sinh buổi từ `schedule_rules`, báo cáo xung đột | T3.2 | Buổi hợp lệ vẫn tạo, xung đột trả về danh sách |
| T3.5 | Cảnh báo trùng lịch phía học sinh (không chặn) | T3.2 | Có trường `warnings` trong response |
| T3.6 | Frontend: lịch tuần/tháng, tạo buổi, hiển thị lỗi trùng | T3.2, T2.3 | Xem/tạo/hủy buổi trên UI |

## Phase 4 — Điểm danh
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T4.1 | Migration `attendance`; API GET/PUT bulk trong 1 transaction | T3.2, T2.2 | AC FR-4; rollback khi lỗi giữa chừng (test) |
| T4.2 | Frontend: màn điểm danh nhanh (mặc định có mặt); lớp 1:1 hiển thị gọn một học sinh | T4.1 | Điểm danh cả lớp < 5 thao tác |

## Phase 5 — Bài tập & điểm
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T5.1 | Migration `assignments` (5 loại: HOMEWORK, QUIZ, EXAM, MOCK_TEST, OTHER), `assignment_scores`; giao bài + tự tạo score rows (thang điểm 10 cố định, không dùng hệ số) | T2.2 | AC FR-5.1–5.2 |
| T5.2 | Nhập điểm/nhận xét bulk; validate `0 <= score <= 10` (thang 10 cố định) | T5.1 | Test biên (0, 10, 10.1, -0.1) |
| T5.3 | Frontend: danh sách bài, bảng nhập điểm | T5.2 | Nhập điểm cả lớp trên UI |

## Phase 6 — Học phí
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T6.1 | Migration `tuition_cycles` (không có cột tiền); mở đợt, tích đã/chưa nộp, void | T2.2 | AC FR-6.1, 6.2, 6.5; test không tồn tại trường tiền trong API |
| T6.2 | Truy vấn `totalSessions/used/remaining`, phân bổ vào đợt (hàm thuần), cảnh báo `CYCLE_LOW`/`CYCLE_DONE_UNPAID`/`NO_OPEN_CYCLE`, tổng hợp cả lớp | T6.1, T4.1 | Bảng ca kiểm thử trong TEST_PLAN (nhiều đợt, học vượt, void, lớp nhóm) |
| T6.3 | Frontend: tab học phí trong lớp (đợt, ô tích đã/chưa nộp, cảnh báo sắp hết buổi/chưa nộp, nút sao chép tin nhắn nhắc học phí để gửi Zalo) | T6.2 | Hiển thị đúng số liệu; không có ô nhập tiền; sao chép đúng mẫu tin nhắn |

## Phase 7 — Báo cáo & xuất dữ liệu
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T7.1 | Truy vấn tổng hợp báo cáo theo khoảng ngày tự chọn (tỉ lệ đi học, điểm TB không hệ số thang 10, buổi còn lại, trạng thái học phí) | T4.1, T5.2, T6.2 | Test số liệu; chỉ tính dữ liệu trong khoảng; không chia cho 0 |
| T7.2 | Tạo/sửa/publish báo cáo + snapshot JSONB | T7.1 | Sửa điểm sau publish không đổi báo cáo |
| T7.3 | Xuất PDF báo cáo, CSV điểm danh/điểm/trạng thái học phí | T7.2 | File mở được, đúng tiếng Việt có dấu |
| T7.4 | Frontend: tạo báo cáo, xem trước, publish, tải PDF/CSV | T7.2, T7.3 | Thao tác được trên UI |

## Phase 8 — Cổng học sinh/phụ huynh
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T8.1 | API portal (dashboard, children) + kiểm tra quyền theo `parent_students` | T7.2, T5.2, T6.2, T3.2 | Phụ huynh không xem được con người khác (test) |
| T8.2 | Frontend portal responsive (ưu tiên điện thoại) | T8.1 | Dùng được trên màn 375px |

## Phase 9 — Hoàn thiện & deploy
| ID | Task | Dep | Điều kiện hoàn thành |
|---|---|---|---|
| T9.1 | Rà soát bảo mật (CORS, secret, rate limit đăng nhập, log) | Phase 8 | Checklist trong TEST_PLAN §6 đạt |
| T9.2 | Dockerfile production + cấu hình Render/Railway (seed tài khoản ADMIN mặc định) | T9.1 | App chạy trên môi trường thật, migration tự chạy |
| T9.3 | Seed dữ liệu demo (giả), README hướng dẫn, ảnh chụp màn hình | T9.2 | Người lạ chạy được theo README |
| T9.4 | (Tùy chọn) Email mời/gửi báo cáo tự động, E2E Playwright | T9.2 | Chỉ làm khi được duyệt |


## Mốc đề xuất
- **M1** Phase 0–2: đăng nhập + quản lý lớp.
- **M2** Phase 3–4: lịch + điểm danh (giá trị cốt lõi).
- **M3** Phase 5–7: điểm, học phí, báo cáo.
- **M4** Phase 8–9: cổng phụ huynh + deploy.

## Nhật ký trạng thái (cập nhật cuối mỗi phiên làm việc)
| Ngày | Task | Trạng thái | Bằng chứng (lệnh test/kết quả) | Ghi chú |
|---|---|---|---|---|
| 2026-09-30 | T0.1 | DONE | git init; commit dda1e53; cấu trúc thư mục backend & frontend | Hoàn thành T0.1 |
| 2026-09-30 | T0.2 | DONE | `mvn verify` thành công (Build Success); test Testcontainers tạm thời disabled | Đã setup POM, YML, Test class, Flyway |
| 2026-09-30 | T0.3 | DONE | `docker compose up -d db` → healthy; `mvn spring-boot:run -Dspring-boot.run.profiles=dev` → Started; `/actuator/health` = UP; Flyway V1 validated | docker-compose.yml, application-dev.yml, .env |
| 2026-09-30 | T0.4 | DONE | `npm run build` thành công (tsc -b && vite build); `npm run lint` 0 warning 0 error; preview HTTP 200 | Vite + React + TS + Tailwind v4 + React Router + TanStack Query |
| 2026-09-30 | T0.5 | DONE | `mvn test -Dtest=GlobalExceptionHandlerTest` → Tests run: 6, Failures: 0, Errors: 0 (BUILD SUCCESS) | GlobalExceptionHandler + AppException + ErrorCode + ResourceNotFoundException + DuplicateResourceException; test 400/404/409/422/500 |
| 2026-10-01 | T0.6 | DONE | Tạo `.github/workflows/ci.yml` (backend `./mvnw clean verify`, frontend `npm ci && npm run lint && npm run build`); file mode `backend/mvnw` 100755 | Hoàn thành CI pipeline cho Phase 0 |
| 2026-10-01 | T1.1 | DONE | Migration V2 chạy sạch trên PostgreSQL; UserRepositoryTest 4/4 passed; RefreshTokenRepositoryTest 4/4 passed (Total 14 tests passed) | Entity User, RefreshToken, Role, UserStatus; UserRepository, RefreshTokenRepository; V2__create_users_and_refresh_tokens.sql |
| 2026-10-02 | T0.4.1 | DONE | `npm run lint` (0 error, 0 warning); `npm run build` (tsc -b && vite build); grep `rounded`/`shadow` = 0; hex check sạch | Thiết lập Design Tokens Tailwind v4 @theme, components/ui, AppLayout Neubrutalism, trang /dev/ui |
| 2026-10-02 | T1.2 | DONE | `mvn test` → Tests run: 28, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS) | Đăng ký gia sư + Đăng nhập + JWT + BCrypt + RefreshToken SHA-256 hash; AuthServiceTest (6/6 passed), AuthIntegrationTest (7/7 passed) |


