# WHAT I LEARNED

*(Tài liệu này sẽ ghi chú lại những kiến thức, khái niệm và cấu trúc kỹ thuật quan trọng học được sau mỗi task)*

## Task: T0.2 - Backend Spring Boot skeleton

**Concepts:**
- Spring Boot starters: `web`, `security`, `data-jpa`, `validation`, `actuator`.
- Maven Wrapper (`mvnw`): Giúp build project không cần cài đặt Maven toàn cục.
- Flyway: Quản lý sự thay đổi (migration) của cơ sở dữ liệu qua các script `.sql`.
- Testcontainers: Tự động khởi tạo một database container (PostgreSQL) độc lập khi chạy test.

**Architecture & Config:**
- `application.yml`: Định nghĩa cổng chạy app (8080), kết nối DB (PostgreSQL), và disable Hibernate DDL auto (vì đã dùng Flyway).
- `@ServiceConnection`: Giúp Spring Boot Test tự động config chuỗi kết nối (URL, username, password) từ Testcontainer.

**Important decision:**
- Chốt dùng Lombok để giảm boilerplate code (getter, setter).
- Yêu cầu môi trường chạy test bắt buộc phải có Docker (để Testcontainers có thể bật Postgres).

---

## Task: T0.3 - Docker Compose + Dev Profile

**Concepts:**
- `docker-compose.yml`: File khai báo các service cần chạy (DB, pgAdmin...). Mỗi service là một container Docker độc lập.
- `healthcheck`: Cơ chế Docker tự kiểm tra container có thực sự sẵn sàng chưa (khác với chỉ "đang chạy"). Dùng `pg_isready` để kiểm tra PostgreSQL đã accept connections chưa.
- Docker `profiles`: Cho phép gom các service tùy chọn (như pgAdmin) vào một nhóm, chỉ khởi động khi cần (`--profile tools`).
- Spring Boot Profiles: `application-dev.yml` chỉ được load khi `spring.profiles.active=dev`. Giúp tách cấu hình dev (verbose, debug) khỏi prod (tắt log nhạy cảm).

**Architecture & Config:**
- `${VAR:-default}` trong docker-compose.yml: Đọc biến môi trường, dùng giá trị mặc định nếu không có — giúp chạy được ngay cả khi chưa có file `.env`.
- Thứ tự ưu tiên Spring Boot config: `application-dev.yml` **ghi đè** `application.yml` khi profile dev active.
- `/actuator/health`: Endpoint của Spring Boot Actuator kiểm tra toàn bộ health components (DB, disk, ping). Dùng để verify backend đã kết nối DB thành công.
- Flyway log `Successfully validated 1 migration` + `Current version: 1`: Xác nhận migration V1 đã chạy và schema đang ở đúng phiên bản.

**Important decision:**
- pgAdmin để vào profile `tools` (không khởi động mặc định) — giữ môi trường dev nhẹ.
- `.env` gitignored, `.env.example` committed — quy ước bảo mật chuẩn: không bao giờ commit secret thật.
- `DB_URL` dùng giá trị literal thay vì `${VAR}` lồng nhau — vì `.env` không hỗ trợ shell expansion như bash.

---

## Task: T0.4 - Frontend Skeleton (Vite + React + TS + Tailwind)

**Concepts:**
- **Vite:** Build tool và dev server siêu nhanh dựa trên ES Modules (ESM). Khác với Webpack bundle toàn bộ trước khi chạy, Vite chỉ biên dịch file khi trình duyệt yêu cầu (On-demand compilation).
- **TypeScript trong React (`react-ts`):** Ràng buộc kiểu dữ liệu tĩnh (`interfaces`, `types`), giảm thiểu lỗi runtime liên quan đến `undefined`, sai kiểu dữ liệu API.
- **Tailwind CSS v4:** Thế hệ mới nhất của Tailwind, tích hợp trực tiếp qua `@tailwindcss/vite` plugin, không cần cấu hình phức tạp `postcss.config.js` hay `tailwind.config.js`, kích hoạt chỉ bằng `@import "tailwindcss";` trong `index.css`.
- **SPA Routing (React Router v7 / v6 Data API):** Quản lý điều hướng phía client mà không cần reload lại toàn bộ trang web. Sử dụng `createBrowserRouter` và `RouterProvider`.
- **TanStack Query (React Query):** Quản lý server state, tự động cache dữ liệu, refetch khi focus/mất mạng, giúp code component không bị lẫn lộn giữa data fetching và UI state.

**Architecture & Config:**
- Cấu trúc thư mục theo ARCHITECTURE §3:
  - `src/api/`: cấu hình Axios client, token interceptor.
  - `src/components/`: UI components dùng chung (Layout, Navigation, v.v.).
  - `src/features/`: chia theo module nghiệp vụ (auth, classes, schedule, attendance, tuition, reports, portal).
  - `src/routes/`: cấu hình định tuyến và route guard sau này.
  - `src/types/`: các interface/type khớp với model hệ thống và RFC 7807 ProblemDetail.
- `vite.config.ts`: Cấu hình reverse proxy `/api` sang `http://localhost:8080`, tránh lỗi CORS trong môi trường dev.

**Important decision:**
- Sử dụng Tailwind CSS v4 với `@tailwindcss/vite` để tối ưu tốc độ build và tinh giản file cấu hình.
- Phân chia `AppLayout` với responsive drawer hỗ trợ tốt cả mobile (màn hình nhỏ từ 375px) theo quy ước frontend trong `AGENTS.md`.

