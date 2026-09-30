# TEST_PLAN — TutorHub

## 1. Chiến lược
| Tầng | Công cụ | Phạm vi |
|---|---|---|
| Unit (backend) | JUnit 5, Mockito | Logic thuần: tính buổi còn lại, điểm TB có hệ số, tỉ lệ đi học, policy quyền |
| Integration (backend) | Spring Boot Test + Testcontainers PostgreSQL | Repository, migration, transaction, exclusion constraint, security |
| API | MockMvc / RestAssured | Status code, định dạng lỗi, phân quyền theo role |
| Frontend | Vitest + React Testing Library | Form validation, route guard, hiển thị lỗi 409, trạng thái rỗng/loading |
| E2E (sau MVP) | Playwright | Luồng gia sư: đăng nhập → tạo lớp → tạo buổi → điểm danh |

Lệnh chuẩn: `cd backend && mvn verify` · `cd frontend && npm run lint && npm test && npm run build`.
Test DB dùng PostgreSQL thật qua Testcontainers (không dùng H2, vì cần `btree_gist` và exclusion constraint).

## 2. Test case bắt buộc theo tính năng

### Auth
- Đăng ký hợp lệ → 201; email trùng → 409; mật khẩu < 8 → 400; email sai định dạng → 400.
- Login đúng/sai → 200/401; thông báo lỗi không tiết lộ email tồn tại.
- Token hết hạn → 401; refresh token bị thu hồi → không dùng lại được.
- Lời mời: hợp lệ; hết hạn; đã dùng; PARENT mời cho học sinh không thuộc lớp của gia sư → 403/422.
- Mật khẩu không xuất hiện trong response và log.

### Phân quyền (chạy cho MỌI nhóm endpoint)
- Không token → 401. Sai role → 403.
- TUTOR B truy cập lớp/buổi/điểm/học phí/báo cáo của TUTOR A → 404.
- STUDENT đọc dữ liệu của học sinh khác → 404/403.
- PARENT đọc dữ liệu của học sinh không phải con → 404/403; PARENT xem báo cáo DRAFT → không thấy.
- Client gửi `tutorId`/`studentId` giả trong body → bị bỏ qua hoặc từ chối.

### Lớp học
- Tạo/sửa/archive hợp lệ; tên rỗng → 400; thiếu/sai `classType` → 400; ghi danh trùng → 409; bỏ học sinh rồi ghi danh lại hoạt động đúng.
- Lớp `ONE_ON_ONE`: thêm học sinh thứ 2 → 422 `ONE_ON_ONE_FULL`; học sinh 1 rời lớp (`LEFT`) rồi thêm học sinh mới → thành công; **hai request thêm học sinh đồng thời** vào lớp 1:1 trống → đúng 1 thành công.
- Lớp `GROUP`: thêm nhiều học sinh thành công; dưới 2 học sinh vẫn tạo được và trả `warnings`.
- Đổi loại: `ONE_ON_ONE` → `GROUP` luôn được; `GROUP` → `ONE_ON_ONE` với ≥ 2 học sinh đang học → 422, với ≤ 1 → thành công.

### Buổi học & chống trùng (quan trọng nhất)
- Chồng giờ hoàn toàn / một phần / bao trùm → 409.
- Liền kề (10:00 kết thúc, 10:00 bắt đầu) → thành công.
- Buổi CANCELLED không gây xung đột; hủy buổi rồi tạo lại khung giờ đó → thành công.
- Sửa giờ buổi sang khung đã có buổi khác → 409.
- Hai gia sư khác nhau cùng khung giờ → thành công.
- **Đồng thời:** 2 luồng tạo buổi trùng cùng lúc → đúng 1 thành công, 1 nhận 409.
- `end <= start` → 400. Xử lý đúng múi giờ (nhập theo Asia/Ho_Chi_Minh, lưu UTC).
- Sinh buổi từ lịch lặp: tạo các buổi hợp lệ, báo cáo danh sách buổi xung đột.

### Điểm danh
- Bulk hợp lệ; học sinh chưa ghi danh → 422; lặp (buổi, học sinh) → cập nhật, không tạo bản ghi thứ hai; lỗi giữa chừng → rollback toàn bộ.

### Bài tập & điểm
- Giao bài tạo score row cho mọi học sinh đang ghi danh; học sinh ghi danh sau được bổ sung.
- Điểm biên: $0$, $\text{max}$, $\text{max}+0.01$ (từ chối), âm (từ chối).
- Xóa bài đã có điểm → 422.

### Học phí (đợt theo số buổi, chỉ tích đã/chưa nộp)
Công thức: $\text{remaining}=\sum_k N_k - U$ và $u_k=\min\!\big(N_k,\ \max(0,\ U-\sum_{j<k}N_j)\big)$. Unit test hàm phân bổ là hàm thuần, không cần DB.

| Tình huống | Các đợt $N_k$ | $U$ | remaining | $u_k$ / cảnh báo |
|---|---|---|---|---|
| Mở đợt, chưa học | 10 | 0 | 10 | $u_1=0$ |
| Học 4 buổi có mặt | 10 | 4 | 6 | $u_1=4$ |
| Vắng có phép 1 buổi | 10 | 4 | 6 | không đổi |
| Vắng không phép 1 buổi | 10 | 5 | 5 | $u_1=5$ |
| Đi muộn 1 buổi | 10 | 6 | 4 | tính là đã học |
| Buổi CANCELLED | 10 | không tính | — | — |
| Còn 2 buổi | 10 | 8 | 2 | `CYCLE_LOW` |
| Học đủ, chưa nộp | 10 (UNPAID) | 10 | 0 | `CYCLE_DONE_UNPAID` |
| Học đủ, đã nộp | 10 (PAID) | 10 | 0 | không có `CYCLE_DONE_UNPAID` |
| Hai đợt | 10 + 10 | 13 | 7 | $u_1=10,\ u_2=3$; đợt hiện tại là đợt 2 |
| Học vượt | 10 | 11 | −1 | `NO_OPEN_CYCLE` |
| Đợt bị void | 10 (void) + 8 | 3 | 5 | đợt void không tính |

- $N \le 0$ → 400. Tích đã nộp/bỏ tích lặp lại không lỗi; lưu `paid_marked_at`.
- Lớp nhóm: đợt và số buổi còn lại của học sinh A không ảnh hưởng học sinh B.
- Phân quyền: STUDENT/PARENT gọi mark-paid/mark-unpaid/tạo đợt → 403; PARENT chỉ xem học phí của con.
- Không có trường số tiền/đơn giá/hình thức thanh toán ở request, response, schema (test kiểm tra schema JSON của API và cột DB).

### Báo cáo
- Khoảng ngày tự chọn: `periodEnd < periodStart` → 400; khoảng 1 ngày; khoảng vắt qua tháng/năm; chỉ tính buổi và điểm nằm trong khoảng.
- Không có buổi/điểm → "chưa có dữ liệu", không lỗi chia cho 0.
- Điểm TB có hệ số: ví dụ $s=(8,\,6)$, $w=(1,\,2)$ → $\bar{s}=\dfrac{8\cdot 1+6\cdot 2}{1+2}=\dfrac{20}{3}\approx 6.67$.
- Publish → snapshot cố định; sửa điểm sau đó không làm đổi báo cáo đã publish.
- PDF tiếng Việt có dấu hiển thị đúng; CSV mở được bằng Excel (UTF-8 BOM).

### Frontend
- Route guard: STUDENT không vào được trang của TUTOR (và ngược lại).
- Hiển thị thông báo tiếng Việt cho từng `code` lỗi (đặc biệt `SESSION_CONFLICT`).
- Có trạng thái loading/lỗi/rỗng; responsive tại 375px.

## 3. Migration
- Chạy toàn bộ migration trên DB rỗng thành công.
- Chèn dữ liệu vi phạm CHECK/UNIQUE/FK/EXCLUDE bị DB từ chối (test SQL).
- Không sửa migration đã áp dụng; thay đổi mới = file `V{n}` mới.

## 4. Dữ liệu test
Chỉ dùng dữ liệu giả (tên, email `@example.com`, SĐT giả). Không dùng dữ liệu học sinh thật.

## 5. Điều kiện bắt buộc trước khi merge
1. `mvn verify` và `npm run lint && npm test && npm run build` xanh.
2. Test mới cho mọi hành vi mới: hợp lệ, dữ liệu sai, trái quyền.
3. Không skip/xóa test để build xanh.
4. Tài liệu (API_SPEC/ARCHITECTURE/ROADMAP) đã đồng bộ.

## 6. Checklist trước khi deploy
- [ ] Không có secret trong repo/lịch sử Git; `JWT_SECRET` dài, ngẫu nhiên, đặt qua biến môi trường.
- [ ] CORS chỉ cho origin frontend production.
- [ ] Giới hạn số lần đăng nhập sai (rate limit) hoặc khóa tạm.
- [ ] Không lộ stack trace/SQL trong response lỗi.
- [ ] Swagger UI tắt hoặc bảo vệ ở production.
- [ ] Backup/khôi phục DB đã thử một lần.
- [ ] Smoke test sau deploy: đăng nhập 3 vai trò, tạo buổi trùng nhận 409, tải PDF báo cáo.
