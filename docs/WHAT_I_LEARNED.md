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

## Task: T1.2 - Đăng ký gia sư + Đăng nhập + JWT + BCrypt

**Concepts:**
- **Mã hóa mật khẩu bằng BCrypt (`PasswordEncoder`):**
  - BCrypt là thuật toán one-way hashing tích hợp salt ngẫu nhiên và cost factor (mặc định 10 vòng mã hóa).
  - Không bao giờ lưu mật khẩu dạng plain-text hoặc MD5/SHA-1; kết quả băm luôn bắt đầu bằng `$2a$` hoặc `$2b$`.
  - Kiểm tra mật khẩu bằng `passwordEncoder.matches(rawPassword, encodedPassword)` thay vì tự so sánh chuỗi băm.
- **Xác thực JWT (JSON Web Token) với JJWT 0.12.x:**
  - Token stateless: Access token chứa các claims định danh (`sub` = userId, `email`, `role`, `iat`, `exp`), được ký điện tử bằng HMAC-SHA256 với Secret Key 256-bit.
  - Server không cần truy vấn DB để xác thực danh tính từng request sau khi đã đăng nhập (stateless session).
- **Cơ chế Hashing Refresh Token an toàn:**
  - Refresh token là chuỗi ngẫu nhiên có thời hạn dài (7 ngày).
  - Để ngăn ngừa rủi ro rò rỉ cơ sở dữ liệu, **chỉ lưu chuỗi băm SHA-256** của refresh token vào bảng `refresh_tokens`. Khi client gửi refresh token lên để cấp lại access token, backend băm chuỗi đó và tìm bản ghi tương ứng (`findByTokenHash`).
- **Bảo mật phản hồi lỗi đăng nhập (OWASP Best Practice):**
  - Khi đăng nhập thất bại (dù email không tồn tại hay sai mật khẩu), luôn trả về mã lỗi chung `AUTH_INVALID_CREDENTIALS` (HTTP 401) kèm thông báo "Email hoặc mật khẩu không chính xác" để ngăn ngừa kỹ thuật tấn công dò quét người dùng (User Enumeration Attack).

**Architecture & Config:**
- **Spring Security 6 stateless filter chain:**
  - Cấu hình `SecurityFilterChain` vô hiệu hóa CSRF (`AbstractHttpConfigurer::disable`) vì API sử dụng token JWT không dùng session cookie, tránh rủi ro CSRF.
  - Đặt `SessionCreationPolicy.STATELESS` để Spring Security không tạo `HttpSession`.
  - Mở quyền truy cập `permitAll()` cho các endpoint xác thực công khai `/api/v1/auth/**`, Actuator `/actuator/**` và tài liệu Swagger OpenAPI `/v3/api-docs/**`.
- **Cấu trúc DTO bằng Java `record`:**
  - `RegisterTutorRequest`: Kiểm tra tính hợp lệ dữ liệu bằng Bean Validation (`@Email`, `@NotBlank`, `@Size(min=8)`).
  - `LoginRequest`: Đóng gói thông tin đăng nhập.
  - `UserResponse`: Trả về dữ liệu người dùng sạch, không bao gồm `passwordHash`.
  - `AuthResponse`: Đóng gói `accessToken`, `refreshToken` và `UserResponse`.

---

## Task: T1.3 - Refresh Token Rotation, Logout, Đổi Mật Khẩu & Sinh Link Reset Mật Khẩu

**Concepts:**
- **Refresh Token Rotation (Xoay vòng token) & Ngăn ngừa Tái sử dụng (Reuse Detection):**
  - Mỗi khi client gọi `/api/v1/auth/refresh`, refresh token hiện tại lập tức bị thu hồi (`revoked_at = Instant.now()`) và một cặp `accessToken` + `refreshToken` hoàn toàn mới được cấp phát.
  - Ngăn ngừa Reuse: Nếu một token đã bị thu hồi mà tiếp tục được gửi lên (dấu hiệu token bị đánh cắp hoặc rò rỉ), hệ thống từ chối ngay với HTTP 401 `AUTH_TOKEN_INVALID`.
- **Single-use Password Reset Token với Hashing SHA-256:**
  - Token đặt lại mật khẩu là chuỗi ngẫu nhiên có độ dài bảo mật cao (UUID/SecureRandom).
  - Tuyệt đối không lưu raw token vào DB để tránh rủi ro khi bị dump DB; chỉ lưu chuỗi băm **SHA-256** (`token_hash UNIQUE`). Khi client gửi token qua link, server băm SHA-256 rồi tìm kiếm bản ghi tương ứng.
  - Single-use: Trường `used_at` ghi lại thời điểm sử dụng. Nếu token đã dùng (`used_at IS NOT NULL`) hoặc đã hết hạn TTL (30 phút, `expires_at < Instant.now()`), hệ thống trả về mã lỗi HTTP 410 `PASSWORD_RESET_INVALID` (RFC 7807).
- **Pessimistic Locking (`SELECT ... FOR UPDATE`) Chống Race Condition:**
  - Khi có hai request đồng thời gửi cùng một refresh token hoặc reset token, có thể xảy ra race condition khiến cả hai đều vượt qua điều kiện kiểm tra (chưa dùng / chưa thu hồi).
  - Sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByTokenHashForUpdate`) để đặt Exclusive Lock dòng dữ liệu ở cấp độ DB trong suốt `@Transactional`, đảm bảo chỉ request đầu tiên thành công và request thứ hai bị từ chối.
- **Thu hồi phiên đăng nhập diện rộng (Revoke All Active Sessions):**
  - Khi người dùng đổi mật khẩu thành công (`PUT /api/v1/me/password`) hoặc hoàn tất đặt lại mật khẩu (`POST /api/v1/auth/reset-password`), hệ thống tự động thu hồi toàn bộ refresh token đang hoạt động của user đó (`revoked_at = Instant.now()`), buộc mọi thiết bị khác phải đăng nhập lại với mật khẩu mới.
- **Kiểm tra quyền sở hữu theo lớp học (Class Ownership Authorization):**
  - Theo thiết kế MVP không dùng email server (D-29), gia sư (TUTOR) chủ động sinh link đặt lại mật khẩu gửi cho học sinh/phụ huynh qua Zalo/tin nhắn.
  - Kiểm tra quyền sở hữu chặt chẽ: TUTOR chỉ được sinh link cho STUDENT đang ghi danh trong lớp của mình, hoặc PARENT có con ghi danh trong lớp của mình. Yêu cầu sinh link cho người ngoài lớp bị từ chối với HTTP 404 (để tránh rò rỉ sự tồn tại của user khác). ADMIN có quyền bypass để sinh link cho bất kỳ tài khoản nào.

**Architecture & Config:**
- **Schema Migrations (V3 & V4):**
  - `V3__create_password_reset_tokens.sql`: Bảng lưu token reset với các cột `token_hash`, `expires_at`, `used_at`, `user_id FK ON DELETE CASCADE`.
  - `V4__create_class_ownership_schema.sql`: Khởi tạo sớm các bảng cốt lõi `classes`, `class_enrollments`, `parent_students` (theo quyết định D-36) để hỗ trợ truy vấn kiểm tra quyền sở hữu TUTOR -> STUDENT/PARENT ngay từ tầng JPA/JDBC.
- **Phân tách Controller theo ngữ cảnh người dùng:**
  - `AuthController`: Quản lý các endpoint xác thực công khai hoặc luồng lifecycle token (`/refresh`, `/logout`, `/reset-password`).
  - `MeController`: Quản lý tài nguyên của chính người dùng hiện tại (`/api/v1/me`, `/api/v1/me/password`), định danh người dùng qua `UserPrincipal` trích xuất từ JWT SecurityContext, không tin cậy `userId` do client gửi lên.
  - `UserController`: Endpoint quản trị và phân quyền (`/api/v1/users/{id}/password-reset-link`), bảo vệ bằng `@PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")`.
- **Xử lý ngoại lệ bảo mật chuẩn RFC 7807 (`RestSecurityExceptionHandler`):**
  - Triển khai `AuthenticationEntryPoint` (401 Unauthorized) và `AccessDeniedHandler` (403 Forbidden) để định dạng lỗi bảo mật trả về cấu trúc JSON ProblemDetail thống nhất với toàn hệ thống, thay thế trang lỗi HTML mặc định của Spring Security.

**Important decision & Debugging tips:**
- **JUnit 5 `@Nested` Test Classes trong Maven Surefire:**
  - Khi tổ chức test case theo các class `@Nested` (ví dụ trong `AuthServiceTest`, `GlobalExceptionHandlerTest`), class cha có thể hiển thị `Tests run: 0` trên báo cáo root. Cần chạy qua JUnit Platform engine để đảm bảo toàn bộ nested tests đều được quét và thực thi.
- **Bảo vệ Endpoint Logout:**
  - Endpoint `POST /api/v1/auth/logout` yêu cầu người dùng phải xác thực (Bearer Token) và chỉ cho phép thu hồi refresh token thuộc về chính người dùng đó, ngăn chặn việc kẻ tấn công gửi bừa token hash của người khác để ép họ bị logout (Denial of Service).

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

---

## Task: T1.4 — Lời mời Học sinh / Phụ huynh (Invitations)

**Concepts:**
- **Invitation Token: Single-use + SHA-256 Hash:**
  - Giống với pattern reset password (T1.3), raw token được tạo bằng `SecureRandom` (32 bytes, Base64 URL-safe), **không bao giờ lưu raw token vào DB**. Chỉ lưu chuỗi băm **SHA-256** (`token_hash UNIQUE`). Khi người dùng gửi token qua link, backend băm lại và tra bảng `token_hash`.
  - **Single-use**: Trường `used_at` ghi thời điểm sử dụng. Token đã dùng (`used_at IS NOT NULL`) hoặc hết hạn (`expires_at < now`) → 410 `INVITATION_USED` / `INVITATION_EXPIRED`.
  - **Pessimistic lock** (`findByTokenHashForUpdate`) khi accept: ngăn hai request đồng thời cùng "chấp nhận" một invitation gây race condition.

- **Luồng nghiệp vụ theo role:**
  - `role=STUDENT`: tạo tài khoản STUDENT → nếu invitation có `class_id` → ghi danh vào lớp (`INSERT INTO class_enrollments ... ON CONFLICT DO UPDATE`).
  - `role=PARENT`: tạo tài khoản PARENT + liên kết phụ huynh-học sinh (`INSERT INTO parent_students ... ON CONFLICT DO NOTHING`).
  - Sau khi tạo tài khoản → **tự đăng nhập**: sinh access token + refresh token → trả về `AuthResponse` (không cần đăng nhập lại thủ công).

- **Ownership Check cho TUTOR:**
  - TUTOR chỉ được mời học sinh vào lớp **ACTIVE của chính họ** (`CHECK_TUTOR_OWNS_CLASS_SQL`).
  - Khi mời PARENT, học sinh trong `studentId` phải đang học trong ít nhất một lớp ACTIVE của TUTOR đó (`CHECK_STUDENT_IN_TUTOR_CLASS_SQL`).
  - Nếu không hợp lệ → 404 `RESOURCE_NOT_FOUND` (ẩn sự tồn tại, tránh information disclosure).
  - ADMIN không bị giới hạn (không qua ownership check).

- **Sử dụng `Long classId` thay vì `@ManyToOne` cho `classes`:**
  - Entity `Class` chưa tồn tại ở T1.4 (sẽ tạo ở T2.1). Dùng `Long classId` (raw FK column) trong entity `Invitation` để tránh circular dependency và giữ phạm vi task. Khi T2.1 tạo `Class` entity, có thể nâng lên `@ManyToOne` nếu cần.

**Architecture & Config:**
- **V5 Migration** (`V5__create_invitations.sql`): Bảng `invitations` với `token_hash UNIQUE`, `role CHECK (IN 'STUDENT','PARENT')`, `class_id FK REFERENCES classes ON DELETE SET NULL`, `student_id FK REFERENCES users ON DELETE SET NULL`, `expires_at`, `used_at`.
- **3 Endpoint mới:**
  - `POST /api/v1/invitations` — yêu cầu xác thực TUTOR/ADMIN (`InvitationController`), trả về link đầy đủ + expiresAt.
  - `GET /api/v1/auth/invitations/{token}` — public, xác minh token còn hợp lệ, trả role/email/className.
  - `POST /api/v1/auth/accept-invitation` — public, tạo tài khoản + ghi danh/liên kết + tự đăng nhập.
- **Security Config**: `/api/v1/auth/accept-invitation` và `/api/v1/auth/invitations/**` đã có trong `permitAll()` từ T1.3.
- **Link building**: Dùng `UriComponentsBuilder` để xây dựng link `{frontendBaseUrl}/accept-invitation?token={rawToken}` — có thể cấu hình qua `app.frontend.base-url` trong `application.yml`.

**Important decision & Debugging tips:**
- **@DataJpaTest + Real PostgreSQL: Data Leak từ @SpringBootTest:**
  - `@SpringBootTest` (dùng cho integration tests như `InvitationIntegrationTest`, `AuthIntegrationTest`) không rollback — mọi commit vào DB đều tồn tại sau khi test chạy xong.
  - `@DataJpaTest` bọc mỗi test trong `@Transactional` rollback, **nhưng** dữ liệu đã commit từ `@SpringBootTest` trước đó vẫn còn trong DB và không bị cuốn vào transaction rollback của `@DataJpaTest`.
  - **Giải pháp**: Thêm `@BeforeEach` vào `@DataJpaTest` test class, dùng `JdbcTemplate.update("DELETE FROM ...")` theo thứ tự FK đúng, sau đó `entityManager.flush(); entityManager.clear()` để xóa JPA first-level cache. **Không** dùng `userRepository.deleteAll()` vì JPA cache có thể không nhất quán với DB sau lệnh JDBC.
  - **Thứ tự xóa FK quan trọng**: `invitations` → `password_reset_tokens` → `refresh_tokens` → `parent_students` → `class_enrollments` → `classes` → `users`. Sai thứ tự sẽ bị PostgreSQL FK constraint rejection.

- **Tại sao không dùng email trong `AcceptInvitationRequest`:**
  - `users.email` là `NOT NULL` theo schema. Email người nhận lời mời được lấy từ `invitation.email` (điền khi TUTOR tạo lời mời). Nếu gia sư không điền email khi tạo lời mời → báo lỗi validation khi accept (yêu cầu gia sư tạo lại có email). Thiết kế này phù hợp với quy trình: gia sư biết email người được mời khi soạn lời mời.

---

## Task: T1.5 — Security Config: Role-Based Access Control (RBAC) & Test truy cập trái quyền mẫu

**Concepts:**
- **Kiến trúc bảo mật hai tầng (Two-Tier Security Architecture):**
  1. *Tầng URL Filter Chain (`SecurityFilterChain`)*:
     - Xử lý xác thực sơ bộ: Các public endpoint (`/api/v1/auth/login`, `/register-tutor`, `/refresh`, `/reset-password`, `/accept-invitation`, `/invitations/**`, `/actuator/**`) được đưa vào `permitAll()`.
     - Mọi endpoint còn lại được bảo vệ bằng `.anyRequest().authenticated()`.
     - Request không có token hoặc token không hợp lệ (sai signature, malformed, hết hạn, user bị vô hiệu hóa) sẽ bị chặn ngay tại filter chain bởi `AuthenticationEntryPoint` (`RestSecurityExceptionHandler.commence`), trả về HTTP `401 Unauthorized` kèm ProblemDetail RFC 7807 (`code: "AUTH_TOKEN_INVALID"`).
  2. *Tầng Method Security (`@EnableMethodSecurity`)*:
     - Phân quyền chi tiết theo vai trò dựa trên annotations `@PreAuthorize("hasRole('ADMIN')")` hoặc `@PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")`.
     - Chuẩn hóa tiền tố `ROLE_`: Spring Security ngầm định `hasRole('ADMIN')` kiểm tra authority `ROLE_ADMIN`. Class `UserPrincipal.authorities()` chuyển đổi enum `Role` sang `SimpleGrantedAuthority("ROLE_" + role.name())`.
     - Khi user có token hợp lệ nhưng vai trò không đủ quyền: Spring Security ném `AccessDeniedException`. Lỗi này được bắt bởi `GlobalExceptionHandler` (`@ExceptionHandler(AccessDeniedException.class)`) và `RestSecurityExceptionHandler.handle`, trả về HTTP `403 Forbidden` kèm ProblemDetail RFC 7807 (`code: "AUTH_ACCESS_DENIED"`).

- **Đồng bộ xử lý lỗi bảo mật qua RFC 7807 (ProblemDetail):**
  - Tránh triệt để việc Spring Security trả về trang HTML 401/403 mặc định hoặc payload không nhất quán.
  - Bổ sung `@ExceptionHandler(AuthenticationException.class)` vào `GlobalExceptionHandler` để đảm bảo ngay cả khi lỗi xác thực ném ra ở tầng Web/Controller method, response vẫn luôn là RFC 7807 với `status: 401`, `code: "AUTH_TOKEN_INVALID"`, tiếng Việt detail.
  - Định dạng Content-Type luôn tương thích với `application/problem+json;charset=UTF-8`.

**Architecture & Test Coverage:**
- **Bộ kiểm thử mẫu `SecurityAccessControlIntegrationTest` (26 test cases):**
  - *Nhóm 1: Unauthenticated (9 tests)*: Kiểm tra không token trên 5 endpoints thực tế (`/me`, `/me/password`, `/auth/logout`, `/invitations`, `/users/{id}/password-reset-link`), header không có tiền tố Bearer, token JWT rác (malformed), token sai chữ ký (forged secret key), token hết hạn (expired timestamp), token của user có trạng thái `DISABLED`. Tất cả trả về 401 `AUTH_TOKEN_INVALID`.
  - *Nhóm 2: Forbidden Role (4 tests)*: `STUDENT` và `PARENT` gọi `POST /api/v1/invitations` hoặc `POST /api/v1/users/{id}/password-reset-link`. Tất cả trả về 403 `AUTH_ACCESS_DENIED`.
  - *Nhóm 3: Authorized Role (3 tests)*: `TUTOR` và `ADMIN` gọi endpoint quản lý → vượt qua security (201 Created); Cả 4 vai trò (`ADMIN`, `TUTOR`, `STUDENT`, `PARENT`) đều truy cập được endpoint chung `/api/v1/me` → 200 OK.
  - *Nhóm 4: Public Endpoints (5 tests)*: Endpoint công khai không bị chặn 401 khi không gửi token (trả về 400 validation nếu payload rỗng, hoặc 410, hoặc 200).
  - *Nhóm 5: Ma trận phân quyền 4 vai trò mẫu (5 tests)*: Sử dụng Test Controller nội bộ kiểm thử độc lập ma trận 4 roles x 4 mức phân quyền (`admin-only`, `tutor-only`, `student-only`, `parent-only`, `tutor-or-admin`), xác nhận cơ chế method security của Spring Security phân tách vai trò tuyệt đối chính xác.

**Important decision & Debugging tips:**
- **MockMvc Content-Type Header matching:**
  - `MockMvc.andExpect(header().string("Content-Type", equalTo("application/problem+json")))` có thể fail nếu response trả về có thêm `charset=UTF-8` (`application/problem+json;charset=UTF-8`).
  - **Giải pháp chuẩn:** Dùng `.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))` để kiểm tra MIME type một cách an toàn và tương thích.
- **Trạng thái tài khoản người dùng (`UserStatus`):**
  - Enum `UserStatus` có hai giá trị: `ACTIVE` và `DISABLED` (không phải `INACTIVE`). Trong `JwtAuthenticationFilter`, kiểm tra `user.getStatus() == UserStatus.ACTIVE` để ngăn chặn token của user bị vô hiệu hóa truy cập hệ thống.

---

## Task: T2.2 — Ghi danh / Bỏ học sinh & Quy tắc lớp 1:1, Lớp nhóm

**Concepts:**
- **Pessimistic Locking chống Race Condition trong nghiệp vụ giới hạn lớp 1:1:**
  - Lớp `ONE_ON_ONE` chỉ cho phép tối đa 1 học sinh đang học (`ACTIVE`). Nếu chỉ kiểm tra bằng `countByClazzIdAndStatus` thông thường trong code mà không khóa dòng DB, hai request đồng thời có thể cùng đọc sĩ số = 0 và cùng thêm học sinh thành công (Race Condition vi phạm quy tắc 1:1).
  - **Giải pháp:** Sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên phương thức truy vấn `ClassRepository.findByIdForUpdate(classId)` (`SELECT ... FOR UPDATE`). Transaction đầu tiên khóa bản ghi lớp, transaction thứ hai bắt buộc phải đợi cho đến khi transaction đầu commit. Khi transaction thứ hai đọc, sĩ số đã là 1 và lập tức ném lỗi HTTP 422 `ONE_ON_ONE_FULL`.
  - Kiểm thử đồng thời (Concurrency Test) bằng `ExecutorService` + `CountDownLatch` xác nhận: 2 luồng thêm 2 học sinh khác nhau vào lớp 1:1 trống đồng thời chỉ có đúng 1 luồng thành công (201), luồng còn lại nhận lỗi 422.

- **Vòng đời Ghi danh (Enrollment Lifecycle) & Unique Constraint:**
  - Bảng `class_enrollments` có ràng buộc duy nhất `UNIQUE (class_id, student_id)`.
  - Khi học sinh rời lớp (`DELETE /classes/{id}/students/{studentId}`): Không xóa cứng bản ghi mà đặt `status = LEFT`, `leftAt = Instant.now()`.
  - Khi học sinh tái ghi danh: Nếu tạo mới `INSERT` sẽ vi phạm unique constraint. Do đó, kiểm tra bản ghi cũ: nếu đang `LEFT` thì tái kích hoạt cập nhật `status = ACTIVE`, `enrolledAt = now`, `leftAt = null`. Nếu đang `ACTIVE` thì ném lỗi 409 `ENROLLMENT_DUPLICATE`.

- **Cảnh báo lớp nhóm (GROUP) dưới 2 học sinh:**
  - Lớp `GROUP` không giới hạn số lượng học sinh tối đa, và theo D-24, gia sư có thể tạo lớp trước rồi thêm học sinh dần nên không chặn khi < 2 học sinh.
  - Trường `warnings` trong `ClassResponse` tự động bổ sung thông báo `"Lớp nhóm hiện có ít hơn 2 học sinh"` khi `classType == GROUP` và `studentCount < 2`, và trở thành mảng rỗng khi đã đủ từ 2 học sinh trở lên.

---

## Task: T2.3 — Giao diện Lớp học, Quản lý học sinh & Link mời (Frontend)

**Concepts & UI/UX Patterns:**
- **Neubrutalism Layout trong quản lý Lớp học:**
  - Sử dụng hệ thống thẻ nổi (pop cards) với viền dày 3px, nền giấy (`bg-paper`), bóng khối sắc nét và badge màu ngữ nghĩa phân biệt rõ loại lớp: Vàng (`bg-yellow`) cho lớp 1:1 và Xanh (`bg-blue`) cho lớp nhóm.
  - Hiển thị trực quan chỉ số nhanh qua `StatTileGroup` (Tổng số lớp, Lớp 1:1, Lớp nhóm, Tổng học sinh đang theo học).
  - Tích hợp bộ lọc trạng thái lớp (`ACTIVE`, `ARCHIVED`, `ALL`) và ô tìm kiếm tức thì theo tên lớp / môn học với cơ chế debounce 300ms.
- **Quy tắc công thái học khi chuyển loại lớp (Form & Business Rule UX):**
  - Trong Modal tạo và sửa lớp, người dùng có thể linh hoạt chuyển đổi giữa lớp 1:1 và lớp nhóm.
  - Khi lớp đang có từ 2 học sinh trở lên, giao diện hiển thị cảnh báo chặn trực tiếp nếu người dùng cố gắng chọn chuyển về lớp 1:1, ngăn chặn request không hợp lệ trước khi gửi lên API (khớp mã lỗi backend `CLASS_TYPE_CHANGE_INVALID`).
- **Liên kết mời học sinh & Phụ huynh (Invitation Link Workflow):**
  - Tích hợp Modal sinh lời mời (`POST /api/v1/invitations`) gắn với `classId`:
    - Học sinh (`STUDENT`): Khi nhận lời mời và hoàn tất đăng ký sẽ tự động được ghi danh vào lớp.
    - Phụ huynh (`PARENT`): Buộc phải chọn học sinh con có mặt trong lớp để thiết lập quan hệ liên kết giám sát.
  - Link lời mời được hiển thị rõ ràng cùng nút sao chép vào Clipboard (One-click copy) kèm thông báo trực quan "Đã chép" và ghi chú thời hạn 7 ngày.
- **Xử lý danh sách học sinh & Vòng đời ghi danh (Student Enrollment Tab):**
  - Trang chi tiết lớp (`/classes/:id`) phân loại học sinh theo 3 tab: Đang học (`ACTIVE`), Đã nghỉ (`LEFT`), và Tất cả.
  - Hỗ trợ ghi danh trực tiếp theo ID học sinh (kèm kiểm tra lớp 1:1 nếu đã đủ 1 học sinh thì vô hiệu hóa nút ghi danh) và nút "Cho nghỉ" để chuyển trạng thái sang `LEFT` mà không xóa dữ liệu lịch sử.
- **Ánh xạ lỗi tiếng Việt nhất quán qua RFC 7807:**
  - Mọi lỗi từ API (`ENROLLMENT_DUPLICATE`, `ONE_ON_ONE_FULL`, `CLASS_TYPE_CHANGE_INVALID`, `CLASS_NOT_FOUND`) được bắt và hiển thị thông báo tiếng Việt thân thiện, rõ nghĩa theo đúng `errorMessages.ts`.

---

## Task: T3.1 — Migration `schedule_rules`, `sessions` & PostgreSQL EXCLUDE Constraint

**Concepts & Database Design:**
- **Giải pháp chống trùng lịch dạy bằng PostgreSQL Exclusion Constraint (`EXCLUDE USING gist`):**
  - **Vấn đề Race Condition:** Nếu chỉ kiểm tra trùng buổi trong Spring Boot service bằng query `SELECT COUNT(*) WHERE tutor_id = ? AND start_at < ? AND end_at > ?`, khi hai request được gửi đồng thời (concurrency), cả hai transaction có thể cùng đọc ra kết quả `count = 0` và cùng chèn thành công bản ghi mới, dẫn đến tình trạng một gia sư bị xếp 2 buổi học trùng giờ nhau.
  - **Bản chất của Exclusion Constraint:** Đây là sự tổng quát hóa của Unique Constraint trong PostgreSQL. Thay vì chỉ kiểm tra sự bằng nhau (`=`), Exclusion Constraint cho phép kiểm tra quan hệ giữa các dòng bằng bất kỳ toán tử index nào (ở đây là toán tử chồng lấn dải thời gian `&&`).
  - **Extension `btree_gist`:** PostgreSQL mặc định chỉ hỗ trợ kiểu dữ liệu hình học hoặc dải (range) trong GIST index. Để kết hợp cột số nguyên thông thường (`tutor_id BIGINT`) với toán tử `=`, bắt buộc phải bật extension `CREATE EXTENSION IF NOT EXISTS btree_gist;`.
  - **Dải thời gian nửa mở (`tstzrange(start_at, end_at, '[)')`):**
    - Ký hiệu `'[)'` quy định: mốc `start_at` thuộc dải (inclusive `[`), còn `end_at` không thuộc dải (exclusive `)`).
    - Điều này giải quyết bài toán nghiệp vụ kinh điển: Buổi 1 kết thúc lúc `10:00:00` và Buổi 2 bắt đầu lúc `10:00:00` là hai buổi **liền kề**, toán tử chồng lấn `&&` trả về `false`, không bị coi là xung đột.
  - **Mệnh đề điều kiện `WHERE (status <> 'CANCELLED')`:**
    - Giúp hệ thống hỗ trợ hủy buổi học (`CANCELLED`). Khi một buổi học đã bị hủy, exclusion constraint tự động bỏ qua bản ghi đó, cho phép gia sư tạo lại buổi dạy mới vào đúng khung giờ đó mà không vi phạm ràng buộc.
- **Denormalization `tutor_id` trong bảng `sessions` (Quyết định D-11):**
  - Trong mô hình chuẩn, `sessions` chỉ cần khóa ngoại trỏ tới `class_id` (vì lớp đã có `tutor_id`). Tuy nhiên, ràng buộc EXCLUDE của PostgreSQL chỉ có thể áp dụng trên các cột của **chính bảng đó**, không thể join sang bảng `classes`.
  - Do đó, denormalize cột `tutor_id` vào `sessions` là sự đánh đổi cần thiết để trao toàn bộ trách nhiệm đảm bảo toàn vẹn dữ liệu cho tầng Database engine, bảo đảm an toàn 100% trước mọi race condition.

---

## Task: T3.2 — Service Quản lý Buổi học & Xử lý Trùng lịch (409 SESSION_CONFLICT)

**Concepts & Implementation Details:**
- **Quy trình 2 lớp phát hiện và ánh xạ lỗi trùng buổi:**
  - *Lớp 1 (Ứng dụng/Service):* Trước khi ghi xuống DB, service chủ động truy vấn `findOverlappingSessions` trong dải thời gian `[startAt, endAt)` (loại trừ các buổi `CANCELLED` và chính buổi đang sửa). Nếu tìm thấy buổi trùng, service ngay lập tức ném `SessionConflictException` chứa `conflictingSessionId` và message định dạng chuẩn: `"Trùng với buổi #ID (Lớp TênLớp, HH:mm–HH:mm UTC)."`.
  - *Lớp 2 (Cơ sở dữ liệu):* Nếu xảy ra race condition giữa 2 luồng đồng thời vượt qua lớp 1, PostgreSQL Exclusion Constraint (`exclude_tutor_overlapping_sessions`) sẽ chặn đứng dòng thứ hai và ném `DataIntegrityViolationException`. Khối `catch` trong service (và `GlobalExceptionHandler`) sẽ bắt lại và chuyển thành ProblemDetail 409 `SESSION_CONFLICT`.
- **Ánh xạ ProblemDetail RFC 7807 với trường mở rộng tùy biến:**
  - Theo chuẩn RFC 7807, Spring `ProblemDetail` hỗ trợ hàm `setProperty("key", value)` để bổ sung các metadata nghiệp vụ mở rộng.
  - Khi gặp `SessionConflictException`, `GlobalExceptionHandler` tự động gán `problem.setProperty("conflictingSessionId", conflictEx.getConflictingSessionId())`. Nhờ đó frontend có thể trích xuất chính xác ID buổi bị trùng để hiển thị liên kết trực tiếp tới buổi đó cho gia sư.
- **Xử lý Dynamic Query với JPA Specification & Khắc phục lỗi PostgreSQL Parameter Type ($n):**
  - Trong PostgreSQL JDBC driver, việc viết native/HQL query dạng `(? IS NULL OR column >= ?)` với các tham số kiểu `Instant` (`timestamptz`) có thể gây ra lỗi:
    `ERROR: could not determine data type of parameter $4` khi tham số truyền vào là `null` (do PostgreSQL không thể suy luận kiểu của `null` trong biểu thức so sánh).
  - **Giải pháp tối ưu:** Sử dụng `JpaSpecificationExecutor<SessionEntity>` kết hợp với Criteria API `Specification<SessionEntity>`. Chỉ thêm `Predicate` vào danh sách khi tham số thực sự khác `null` (ví dụ `if (from != null) predicates.add(cb.greaterThanOrEqualTo(...))`). Câu lệnh SQL sinh ra luôn gọn gàng, đúng kiểu dữ liệu và tận dụng tối đa chỉ mục của PostgreSQL.
- **Bảo mật và Phân quyền xem Lịch học:**
  - `TUTOR`: Chỉ xem được các buổi do chính mình phụ trách (`tutor_id = actorId`).
  - `STUDENT`: Chỉ xem được các buổi thuộc những lớp mà học sinh đang có ghi danh hoạt động (`status = 'ACTIVE'`).
  - `PARENT`: Chỉ xem được các buổi của lớp mà con mình (đã xác thực qua bảng `parent_students`) đang theo học.
  - `ADMIN`: Có quyền xem lịch của mọi lớp/mọi gia sư mà không bị giới hạn ownership.

