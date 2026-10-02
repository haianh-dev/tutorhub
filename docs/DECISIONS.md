# DECISIONS — TutorHub

Định dạng: ID · Ngày · Trạng thái (`CONFIRMED` = người dùng đã chốt, `PROPOSED` = AI đề xuất chờ duyệt, `SUPERSEDED`) · Quyết định · Lý do · Phương án đã cân nhắc.
AI chỉ được coi `CONFIRMED` là ràng buộc. `PROPOSED` phải hỏi lại trước khi dựa vào.

| ID | Trạng thái | Quyết định | Lý do / Phương án khác |
|---|---|---|---|
| D-01 | CONFIRMED | Backend Java + Spring Boot | Mục tiêu học Java/Spring Boot |
| D-02 | CONFIRMED | Frontend React + TypeScript + Tailwind CSS (SPA tách riêng) | Người dùng chọn. Thymeleaf đã cân nhắc, không chọn |
| D-03 | SUPERSEDED | Ba vai trò TUTOR / STUDENT / PARENT, mỗi vai trò có tài khoản riêng | Bị thay thế bởi D-27 (bổ sung ADMIN) |
| D-04 | CONFIRMED | PostgreSQL chạy bằng Docker; deploy Render/Railway | Người dùng chọn. Chưa chọn cụ thể Render hay Railway |
| D-05 | CONFIRMED | Nhiều gia sư dùng chung, mỗi người chỉ truy cập lớp của mình | Từ ý tưởng ban đầu |
| D-06 | CONFIRMED | Chống ghi nhận trùng buổi học | Từ ý tưởng ban đầu |
| D-07 | PROPOSED | Java 21 LTS, Spring Boot 3.x, Maven | Ổn định, phổ biến, hỗ trợ tốt. Gradle không cần thiết cho MVP |
| D-08 | PROPOSED | Monolith theo module tính năng | Đơn giản, đủ cho MVP; microservice là thiết kế thừa |
| D-09 | PROPOSED | JWT (access 15 phút + refresh 7 ngày, refresh lưu băm ở DB) | Phù hợp SPA + API tách riêng. Session cookie là phương án thay thế (cần CSRF) |
| D-10 | PROPOSED | Chống trùng buổi bằng PostgreSQL exclusion constraint (`btree_gist` + `tstzrange`) | An toàn khi đồng thời, không phụ thuộc kiểm tra ở code. Chỉ kiểm tra trong service thì có race condition |
| D-11 | PROPOSED | Bảng `sessions` có cột `tutor_id` (denormalize) để đặt exclusion constraint | Constraint không thể tham chiếu qua bảng khác; đánh đổi: phải giữ `tutor_id` nhất quán với lớp |
| D-12 | CONFIRMED | Học sinh/phụ huynh vào bằng lời mời (invite link) do gia sư tạo | Tránh tài khoản giả, tự liên kết đúng lớp/con. Người dùng xác nhận 2026-09-30 |
| D-13 | CONFIRMED | Học phí tính theo số buổi học; `remaining` tính bằng truy vấn, không lưu cột | Người dùng xác nhận 2026-09-30. Cách tổ chức đợt xem D-22 |
| D-14 | CONFIRMED | Vắng có phép không trừ buổi; vắng không phép và đi muộn có trừ | Quy ước phổ biến. Người dùng xác nhận 2026-09-30 |
| D-15 | PROPOSED | Báo cáo lưu snapshot JSONB khi PUBLISHED | Báo cáo đã gửi phụ huynh không thay đổi khi sửa điểm sau đó |
| D-16 | PROPOSED | Flyway cho migration; test bằng Testcontainers PostgreSQL | Cần Postgres thật để test constraint; H2 không đủ |
| D-17 | PROPOSED | Lưu thời gian UTC (`timestamptz`), hiển thị `Asia/Ho_Chi_Minh` | Tránh lỗi múi giờ |
| D-18 | PROPOSED | Lỗi trả RFC 7807 kèm `code` ổn định | Frontend hiển thị thông báo tiếng Việt theo `code` |
| D-19 | CONFIRMED | Frontend: Vite, React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS v4 | Phổ biến, ít boilerplate. Người dùng duyệt 2026-09-30 |
| D-20 | PROPOSED | Xóa mềm/lưu trữ (archive/void) thay vì xóa cứng dữ liệu có liên quan | Giữ lịch sử điểm danh, điểm, đợt học phí |
| D-21 | CONFIRMED | Hệ thống không xử lý tiền: không lưu số tiền/đơn giá/hình thức thanh toán, không thanh toán online. Gia sư và phụ huynh trao đổi học phí qua Zalo/tin nhắn; web chỉ có ô tích đã nộp/chưa nộp | Người dùng xác nhận 2026-09-30. Giảm rủi ro pháp lý/bảo mật, đơn giản hóa schema |
| D-22 | CONFIRMED | Học phí chia thành đợt N buổi cho mỗi học sinh trong mỗi lớp (`tuition_cycles`); buổi đã học tính dồn vào đợt cũ trước; cảnh báo `CYCLE_LOW`, `CYCLE_DONE_UNPAID`, `NO_OPEN_CYCLE` | Cần để có "buổi đã học/còn lại" mà vẫn chỉ có tích đã/chưa nộp. Người dùng xác nhận 2026-09-30 |
| D-23 | CONFIRMED | Lớp có loại: `ONE_ON_ONE` (1:1) hoặc `GROUP` (nhóm từ 2 học sinh), gia sư tự chọn | Người dùng xác nhận 2026-09-30 |
| D-24 | CONFIRMED | Lớp 1:1 tối đa 1 học sinh đang học (kiểm tra trong service, có khóa dòng lớp); lớp nhóm dưới 2 học sinh chỉ cảnh báo, không chặn | Gia sư cần tạo lớp rồi thêm học sinh dần. Người dùng xác nhận 2026-09-30 |
| D-25 | CONFIRMED | Báo cáo tiến độ theo khoảng ngày do gia sư tự chọn | Người dùng xác nhận 2026-09-30 |
| D-26 | CONFIRMED | Định vị: công cụ hỗ trợ gia sư quản lý lịch và lớp; không phải sàn/nền tảng phân phối gia sư (không danh bạ, tìm kiếm, đánh giá, ghép gia sư–học sinh) | Người dùng xác nhận 2026-09-30 |
| D-27 | CONFIRMED | Bốn vai trò: ADMIN, TUTOR, STUDENT, PARENT; ADMIN có toàn quyền truy cập mọi tài nguyên | Người dùng yêu cầu (2026-09-30) cho quản trị hệ thống |
| D-28 | CONFIRMED | Học sinh (STUDENT) được xem báo cáo tiến độ đã công bố (PUBLISHED) của chính mình | Thống nhất giữa bảng phân quyền và Portal Dashboard (2026-09-30) |
| D-29 | CONFIRMED | Quên mật khẩu giải quyết bằng link đặt lại mật khẩu do gia sư/ADMIN tạo, gửi qua Zalo/tin nhắn (không tích hợp gửi email tự động/SMTP ở MVP) | MVP chưa cần hệ thống gửi email tự động (D-21, PROJECT_BRIEF §9). Gia sư quản lý lớp tạo link cho học sinh/phụ huynh của mình; ADMIN tạo cho mọi user (2026-09-30) |
| D-30 | CONFIRMED | Điểm số dùng thang điểm 10 cố định (0 đến 10), không dùng hệ số (trọng số) trong MVP; hỗ trợ 5 loại bài tập: HOMEWORK, QUIZ, EXAM, MOCK_TEST, OTHER | Thống nhất với PROJECT_BRIEF §9 mục 9-10 (2026-09-30). Đơn giản hóa nhập điểm và công thức điểm TB cho MVP |
| D-31 | CONFIRMED | Khởi tạo tài khoản ADMIN qua seed dữ liệu khi deploy (biến môi trường `ADMIN_EMAIL`, `ADMIN_DEFAULT_PASSWORD`); ADMIN tạo lớp học có quyền chỉ định `tutorId` | Đảm bảo hệ thống có tài khoản quản trị đầu tiên mà không mở public register cho ADMIN; giải quyết quyền sở hữu `classes.tutor_id` khi ADMIN tạo lớp (2026-09-30) |
| D-32 | CONFIRMED | Phong cách UI Neubrutalism: nền kem, 3 màu nhấn vàng/coral/xanh + đen/trắng, viền đen dày, bóng cứng không blur, góc vuông, Space Grotesk cho heading | Người dùng chọn (2026-10-02) |
| D-33 | PROPOSED | Font body dùng Space Mono thay DM Mono của prompt gốc; có thể đổi sang Be Vietnam Pro nếu đọc đoạn dài mỏi mắt | Đảm bảo hiển thị đúng dấu tiếng Việt |
| D-34 | PROPOSED | Viền 2px cho đường kẻ bên trong bảng/danh sách dày; khối nổi giữ 3px, vùng lớn 4px | Giữ tính dễ đọc của bảng điểm danh/điểm số |

## Câu hỏi còn mở
Toàn bộ các câu hỏi mở ban đầu đã được người dùng chốt đầy đủ tại `PROJECT_BRIEF.md §9` (ngày 2026-09-30). Mọi phát sinh mới trong quá trình triển khai phải tuân thủ quy trình hỏi người dùng trước khi quyết định.

## Cách ghi quyết định mới
Thêm dòng mới với ID tăng dần; nếu thay đổi quyết định cũ, đánh dấu dòng cũ `SUPERSEDED` và ghi ID thay thế. Không xóa dòng cũ.

