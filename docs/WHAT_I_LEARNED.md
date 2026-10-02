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

---

## Task: T0.5 - Exception Handler Thống Nhất (ProblemDetail RFC 7807)

**Concepts:**
- **RFC 7807 (Problem Details for HTTP APIs):** Chuẩn quốc tế định nghĩa cấu trúc JSON thống nhất khi trả về lỗi HTTP (`type`, `title`, `status`, `detail`, `instance`). Spring Boot 3 tích hợp sẵn class `ProblemDetail`.
- **Mã lỗi ổn định (`ErrorCode`):** Thay vì để frontend dựa vào chuỗi message tiếng Anh (dễ thay đổi) để hiển thị thông báo, backend trả về mã `code` dạng enum (`SESSION_CONFLICT`, `VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`). Frontend chỉ cần switch-case theo `code` để hiển thị tiếng Việt.
- **`@RestControllerAdvice`:** Annotation đánh dấu class xử lý ngoại lệ tập trung (AOP - Aspect-Oriented Programming) cho mọi Controller trong ứng dụng.
- **Kế thừa `ResponseEntityExceptionHandler`:** Cho phép override lại cách Spring xử lý các lỗi HTTP chuẩn (như `MethodArgumentNotValidException` của Bean Validation) để format theo cấu trúc mong muốn mà không làm mất luồng chuẩn của Spring MVC.

**Architecture & Config:**
- **Kiến trúc Exception:**
  - `ErrorCode`: Enum định nghĩa mã lỗi + HTTP status mặc định + title tiếng Anh chuẩn.
  - `AppException`: RuntimeException cơ sở mang theo `ErrorCode`.
  - `ResourceNotFoundException` (404) và `DuplicateResourceException` (409): Subclass hỗ trợ ném lỗi nhanh gọn từ tầng Service.
  - `GlobalExceptionHandler`: Bắt `AppException`, Bean Validation (`MethodArgumentNotValidException`), lỗi DB constraint (`DataIntegrityViolationException`), và fallback `Exception` (500).
- **Bảo mật thông tin lỗi:** Toàn bộ exception không xác định (500) được ẩn stack trace và message kỹ thuật khỏi response, chỉ log chi tiết ở phía server nhằm tránh lộ thông tin nội bộ (Information Disclosure).
- `application.yml`: Bật `spring.mvc.problemdetails.enabled: true` để Spring tự động định dạng cả các lỗi do framework quản lý sang RFC 7807.

**Important decision & Debugging tips:**
- **Spring Security CSRF trong MockMvc Test:** Khi test endpoint `POST` với MockMvc, dù có dùng `@WithMockUser`, request vẫn có thể bị trả về `403 Forbidden` do Spring Security bật CSRF bảo vệ. Khắc phục bằng cách gắn `.with(csrf())` vào request builder.
- **Bean Validation trả về nhiều lỗi cho cùng 1 field:** Một giá trị input có thể cùng lúc vi phạm nhiều annotation (ví dụ chuỗi rỗng `""` vi phạm cả `@NotBlank` và `@Size(min=1)`), do đó danh sách `errors` cần được xử lý dạng mảng (array) chứ không giả định mỗi field chỉ có 1 lỗi duy nhất.

---

## Task: T0.6 - CI GitHub Actions (Build & Test Backend + Frontend)

**Concepts:**
- **CI (Continuous Integration):** Quy trình tự động hóa kiểm tra mã nguồn (build, typecheck, lint, test) ngay khi có code mới được đẩy lên (`push`) hoặc tạo yêu cầu gộp nhánh (`pull_request`). Giúp ngăn chặn code lỗi lọt vào nhánh chính (`main`).
- **GitHub Actions (Workflows & Jobs):** Hệ thống CI/CD tích hợp sẵn của GitHub. File workflow định dạng YAML đặt tại `.github/workflows/`. Các job độc lập (`backend`, `frontend`) mặc định được chạy song song (parallel) trên các máy ảo riêng biệt (`ubuntu-latest`), tối ưu thời gian phản hồi.
- **Dependency Caching:** Sử dụng cache của `actions/setup-java` (cache Maven `.m2`) và `actions/setup-node` (cache `~/.npm` theo `package-lock.json`) để tránh tải lại toàn bộ thư viện mỗi lần chạy CI, giảm đáng kể thời gian chạy pipeline.

**Architecture & Config:**
- **Workflow `.github/workflows/ci.yml`:**
  - `defaults.run.working-directory`: Thiết lập thư mục gốc cho từng job (`backend` hoặc `frontend`), giúp các step chạy lệnh nội bộ mà không cần lặp lại `cd backend` hay `cd frontend`.
  - Job `backend`: Chạy trên môi trường Linux có sẵn Docker daemon. Kiểm thử `mvn verify` (chạy cả unit test và SpringBoot integration test với Testcontainers PostgreSQL).
  - Job `frontend`: Chạy `npm ci`, kiểm tra lint bằng `oxlint` và build production bundle bằng `tsc -b && vite build`.
- **`npm ci` vs `npm install` trong CI:** Luôn dùng `npm ci` trong môi trường tự động vì lệnh này dựa hoàn toàn vào `package-lock.json`, không bao giờ tự sửa lockfile và đảm bảo tính nhất quán tuyệt đối giữa môi trường dev và CI.

**Important decision & Debugging tips:**
- **Lỗi `Permission denied` với `mvnw` trên Linux runner:** Trên Windows, Git không tự đánh dấu thuộc tính thực thi (executable bit `+x`) cho file script Unix (`mvnw`). Khi đẩy lên repo và checkout trên Ubuntu, file chỉ có quyền `100644` khiến lệnh `./mvnw` thất bại. Cách khắc phục:
  1. Chạy `git update-index --chmod=+x backend/mvnw` trực tiếp trên máy dev để Git lưu cờ `100755` vào commit.
  2. Bổ sung step `run: chmod +x mvnw` trong workflow như một lớp bảo vệ dự phòng.

---

## Task: T1.1 - Migration Users, RefreshTokens, Entity & Repository

**Concepts:**
- **Flyway Versioned Migration (`V2`):** Quản lý tiến hóa schema cơ sở dữ liệu bằng các script SQL có số phiên bản tăng dần (`V{n}__description.sql`). Mỗi lần ứng dụng khởi động, Flyway tự động kiểm tra bảng `flyway_schema_history` và áp dụng các migration mới chưa chạy, đảm bảo đồng nhất tuyệt đối giữa local dev, test và production.
- **Chuẩn hóa Schema PostgreSQL theo ARCHITECTURE.md:**
  - `BIGINT GENERATED ALWAYS AS IDENTITY`: Tiêu chuẩn SQL hiện đại thay thế cho `SERIAL` cũ, tự động sinh khóa chính an toàn và ngăn ngừa việc vô tình chèn đè giá trị ID.
  - `TIMESTAMPTZ` (`timestamp with time zone`): Lưu trữ mốc thời gian chuẩn UTC, tránh rủi ro sai lệch thời gian giữa server và client ở các múi giờ khác nhau.
  - Check Constraints (`chk_users_role`, `chk_users_status`): Ràng buộc toàn vẹn dữ liệu ngay tại DB (`role IN ('ADMIN','TUTOR','STUDENT','PARENT')`), bảo vệ DB khỏi dữ liệu rác ngay cả khi truy vấn trực tiếp.
  - Foreign Key `ON DELETE CASCADE` (`refresh_tokens.user_id`): Tự động xóa sạch các refresh token khi tài khoản người dùng bị xóa.
- **JPA Entity Best Practices:**
  - `@Enumerated(EnumType.STRING)`: Bắt buộc lưu enum dưới dạng chuỗi tên (`"TUTOR"`, `"ADMIN"`), tuyệt đối không dùng `ORDINAL` vì nếu thêm mới hoặc đổi thứ tự enum trong code Java thì toàn bộ dữ liệu cũ trong DB sẽ bị sai lệch ý nghĩa.
  - `@PrePersist` & `@PreUpdate`: Tận dụng JPA Entity Lifecycle Callbacks để tự động điền `createdAt` và cập nhật `updatedAt = Instant.now()`.

**Architecture & Config:**
- **Cấu hình Lombok Annotation Processor trên Java 21:** Khi build bằng Maven trên JDK 21, nếu không khai báo `annotationProcessorPaths` cho `lombok` trong `maven-compiler-plugin`, trình biên dịch `javac` sẽ bỏ qua việc sinh mã cho `@Getter`, `@Setter`, `@Builder`, gây ra lỗi `cannot find symbol: method builder()`.
- **Kiểm thử `@DataJpaTest` với PostgreSQL thật:**
  - Bổ sung `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` để Spring không cố gắng thay thế PostgreSQL bằng H2 (do dự án quyết định dùng Postgres thật để kiểm tra toàn bộ constraint theo DECISIONS D-16).
  - Khi test chạy, Flyway tự động áp dụng cả `V1__init.sql` và `V2__create_users_and_refresh_tokens.sql`.
  - Kiểm thử đầy đủ các kịch bản: lưu/truy vấn entity, `existsByEmail`, `findByRole`, lọc token chưa thu hồi (`revokedAt IS NULL`), xóa token hết hạn, và kiểm tra ném `DataIntegrityViolationException` khi vi phạm Unique constraint.

---

## Task: T0.4.1 - Định hướng giao diện (UI Foundation Neubrutalism)

**Concepts:**
- **Phong cách Neubrutalism trong ứng dụng quản lý:**
  - Kết hợp sự táo bạo của phong cách Brutalism (khối hộp sắc cạnh, góc vuông `border-radius: 0`, viền đen dày `3px/4px border-ink`, bóng đổ cứng không làm mờ `box-shadow: 4px 4px 0 #000`) với màu sắc Pop-Art có kiểm soát.
  - Khác biệt với landing page hào nhoáng: Trong ứng dụng quản trị (Dashboard, Lịch, Form, Bảng điểm), ưu tiên tính công thái học (ergonomics), độ tương phản cao, thao tác nhanh và cấu trúc lưới cố định, không dùng các chi tiết thừa thãi như marquee trôi chữ hay hiệu ứng kéo thả hoạt họa gây xao nhãng.
- **Design Tokens & Theme trong Tailwind CSS v4:**
  - Tailwind v4 sử dụng `@theme` block trực tiếp trong `index.css` để định nghĩa custom token: biến màu (`--color-cream`, `--color-paper`, `--color-ink`, `--color-yellow`, `--color-coral`, `--color-blue`), fonts (`--font-heading`, `--font-body`), và shadows cứng (`--shadow-hard-*`).
  - Sử dụng `@utility` để tạo các class tái sử dụng chuẩn hóa: `brut-pop` (dành cho phần tử tương tác có hover/active offset và bóng dịch chuyển) và `brut-box` (dành cho khối tĩnh như Card, Container).
- **Typography & Font Tiếng Việt:**
  - Heading: **Space Grotesk** (độ đậm 700 - 900, `line-height >= 1.15` để không cắt dấu tiếng Việt).
  - Body: **Space Mono** (chữ đơn cách monospace mang phong thái kỹ thuật, rõ ràng, hỗ trợ đầy đủ ký tự tiếng Việt có dấu như `Ệ ẳ ữ ặ Đ ơ ư`).
  - Hỗ trợ `font-variant-numeric: tabular-nums` cho các bảng số liệu, điểm số và giờ học.
- **Accessible Interactions & Micro-states:**
  - Vùng chạm (touch target) tối thiểu 44×44px cho mọi button/input.
  - Vòng viền bàn phím (`:focus-visible`) với `outline: 3px solid var(--color-ink); outline-offset: 3px;` giúp người dùng điều hướng bàn phím trực quan.
  - Trạng thái `prefers-reduced-motion` tự động vô hiệu hóa chuyển động `translate` khi hover.

**Architecture & Components:**
- Xây dựng thư viện component nền tảng tại `src/components/ui/`:
  - `Button`: Hỗ trợ 4 biến thể ngữ nghĩa (`primary`, `secondary`, `info`, `danger`), trạng thái loading/disabled, font heading in hoa.
  - `Card`: Khối chứa nội dung tĩnh với viền 3px, nền `paper` và padding 24px theo hệ lưới 8px.
  - `Badge`: Nhãn trạng thái nhỏ viền 2px, in hoa, màu theo quy ước ngữ nghĩa trạng thái.
  - `Alert`: Khối thông báo/cảnh báo nổi bật với khối ký hiệu 40×40 riêng biệt (`!`, `✕`, `i`).
  - `Input`: Ô nhập liệu chuẩn với nhãn in hoa bên trên, viền 3px và khối thông báo lỗi chuyên biệt màu `coral`.
  - `Tabs`: Bộ chuyển tab dạng segmented button với tab đang kích hoạt mang màu nhấn `yellow`.
  - `StatTileGroup`: Dải hiển thị chỉ số lớn với font số hiển thị 900 và nhãn ngắn gọn.
  - `EmptyState`: Khung viền đứt nét 3px cho các màn chưa có dữ liệu.
- Chuyển đổi khung `AppLayout` sang phong cách Neubrutalism chuẩn:
  - Desktop: Sidebar cố định 256px màu `ink`, chữ `paper`, viền phải 4px `ink`.
  - Mobile: Header dính cố định viền 4px kèm ngăn menu drawer trượt xuống khi bấm hamburger button.

---

## 🛠️ Tips & Debugging Thường Gặp

### Lỗi: `Web server failed to start. Port 8080 was already in use.`
- **Nguyên nhân:** Có tiến trình khác (hoặc backend Spring Boot trước đó chạy ngầm / daemon) đang chiếm giữ cổng `8080`, khiến Tomcat không thể bind cổng khi khởi động từ IDE hoặc terminal.
- **Cách xử lý trên Windows (PowerShell):**
  1. Tìm PID (Process ID) của tiến trình đang chiếm port:
     ```powershell
     Get-NetTCPConnection -LocalPort 8080
     # hoặc:
     netstat -ano | findstr :8080
     ```
  2. Tắt tiến trình đó theo PID:
     ```powershell
     Stop-Process -Id <PID> -Force
     # hoặc dùng lệnh Command Prompt:
     taskkill /PID <PID> /F
     ```

