# TutorHub

> Ứng dụng web quản lý lịch dạy, lớp học, điểm danh, bài tập, điểm số và theo dõi đợt học phí dành cho gia sư độc lập.

---

## 1. Giới thiệu & Định vị sản phẩm

* **TutorHub** là công cụ hỗ trợ gia sư quản lý công việc giảng dạy cá nhân.
* **Không phải sàn gia sư:** Không có tìm kiếm, đánh giá, ghép lớp công khai.
* **Không xử lý tiền:** Không lưu số tiền, đơn giá, hóa đơn hay thanh toán trực tuyến. Học phí chỉ theo dõi theo **số buổi của đợt** và trạng thái **đã nộp / chưa nộp**.

### Các vai trò (Roles)
* **ADMIN:** Quản trị toàn bộ hệ thống, có toàn quyền truy cập.
* **TUTOR (Gia sư):** Tạo & quản lý lớp, lịch dạy, điểm danh, bài tập, điểm số, đợt học phí và báo cáo tiến độ.
* **STUDENT (Học sinh):** Xem lịch học, bài tập, điểm số, số buổi còn lại và báo cáo của bản thân qua invite link.
* **PARENT (Phụ huynh):** Xem lịch học, điểm danh, điểm số, học phí và báo cáo tiến độ của con mình qua invite link.

---

## 2. Công nghệ sử dụng (Tech Stack)

* **Backend:** Java 25 LTS, Spring Boot 3.x (Spring Web, Spring Security + JWT, Spring Data JPA, Flyway, Testcontainers).
* **Frontend:** React 18, TypeScript, Vite, Tailwind CSS, TanStack Query, React Hook Form + Zod, Axios.
* **Database:** PostgreSQL 16 (chạy Docker, sử dụng `btree_gist` để xử lý exclusion constraint chống trùng lịch).
* **Kiến trúc:** Monolith module hóa theo tính năng (Feature-based packaging).

---

## 3. Cấu trúc thư mục

```text
tutorhub/
├── AGENTS.md                  # Hướng dẫn và quy ước làm việc cho AI
├── README.md                  # Tài liệu tổng quan dự án
├── .env.example               # Mẫu cấu hình biến môi trường
├── .gitignore                 # Cấu hình bỏ qua file trong Git
├── docs/                      # Tài liệu phân tích và thiết kế
│   ├── PROJECT_BRIEF.md       # Tổng quan mục tiêu, phạm vi MVP, quyết định chốt
│   ├── REQUIREMENTS.md        # Yêu cầu chức năng, phi chức năng, quy tắc nghiệp vụ
│   ├── ARCHITECTURE.md        # Thiết kế kiến trúc, schema DB, bảo mật
│   ├── API_SPEC.md            # Đặc tả chi tiết REST API
│   ├── ROADMAP.md             # Lộ trình từng phase và task
│   ├── TEST_PLAN.md           # Kế hoạch và kịch bản kiểm thử bắt buộc
│   ├── DECISIONS.md           # Nhật ký các quyết định kỹ thuật (ADR)
│   └── PROMPTS.md             # Bộ prompt tiêu chuẩn cho từng giai đoạn
├── backend/                   # Ứng dụng Spring Boot
│   └── src/
│       ├── main/java/com/tutorhub/
│       └── main/resources/
└── frontend/                  # Ứng dụng React + TypeScript + Vite
    └── src/
```

---

## 4. Hướng dẫn khởi chạy cục bộ (Local Development)

### Yêu cầu môi trường
* Java 25 LTS
* Node.js v20+ & npm
* Docker & Docker Compose (cho PostgreSQL và Testcontainers)

### Các bước khởi chạy (chuẩn bị)
```bash
# 1. Khởi động cơ sở dữ liệu PostgreSQL
docker compose up -d db

# 2. Chạy ứng dụng Backend (sau khi hoàn thành skeleton T0.2/T0.3)
cd backend
./mvnw spring-boot:run

# 3. Chạy ứng dụng Frontend (sau khi hoàn thành skeleton T0.4)
cd frontend
npm install
npm run dev
```

---

## 5. Tài liệu chi tiết
Vui lòng tham khảo thư mục [docs/](file:///E:/tutorhub/docs) để nắm rõ các yêu cầu và thiết kế kỹ thuật trước khi đóng góp mã nguồn.
