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

