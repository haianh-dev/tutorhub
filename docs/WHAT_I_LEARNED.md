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
