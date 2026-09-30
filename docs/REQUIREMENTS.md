# REQUIREMENTS — TutorHub (MVP)

Quy ước: `FR-x` = yêu cầu chức năng, `NFR-x` = phi chức năng, `BR-x` = quy tắc nghiệp vụ. Mỗi FR có tiêu chí nghiệm thu (AC). Mục có **[ASSUMPTION]** cần người dùng duyệt.

## 1. Vai trò và phân quyền

| Hành động | ADMIN | TUTOR | STUDENT | PARENT |
|---|---|---|---|---|
| Tạo/sửa/xóa lớp | ✅ (tất cả) | ✅ (của mình) | ❌ | ❌ |
| Xem lớp | ✅ (tất cả) | Lớp mình dạy | Lớp mình học | Lớp của con |
| Quản lý buổi học, điểm danh | ✅ (tất cả) | ✅ (lớp mình) | ❌ | ❌ |
| Giao bài, nhập điểm | ✅ (tất cả) | ✅ (lớp mình) | ❌ | ❌ |
| Tích đã nộp/chưa nộp học phí, mở đợt học phí | ✅ (tất cả) | ✅ (lớp mình) | ❌ | ❌ |
| Xem điểm danh/điểm/bài tập | ✅ (tất cả) | Lớp mình | Của bản thân | Của con |
| Xem trạng thái học phí, buổi còn lại | ✅ (tất cả) | Lớp mình | Của bản thân | Của con |
| Tạo & công bố báo cáo | ✅ (tất cả) | ✅ | ❌ | ❌ |
| Xem báo cáo đã công bố | ✅ (tất cả) | ✅ | Của bản thân | Của con |
| Mời học sinh/phụ huynh | ✅ (tất cả) | ✅ | ❌ | ❌ |

**BR-1 (quyền sở hữu):** Mọi truy vấn dữ liệu lớp/buổi/điểm/học phí của TUTOR phải lọc theo `tutor_id` của người đăng nhập. Truy cập dữ liệu của gia sư khác trả `404` (không lộ sự tồn tại) hoặc `403`. Vai trò ADMIN có toàn quyền truy cập mọi tài nguyên và dữ liệu trên hệ thống.
**BR-2:** STUDENT chỉ đọc dữ liệu có `student_id` = chính mình (bao gồm cả báo cáo tiến độ đã công bố của bản thân). PARENT chỉ đọc dữ liệu của học sinh có trong `parent_students`.
**BR-3 (định vị):** Hệ thống là công cụ quản lý của gia sư, không phải sàn: không có danh bạ/tìm kiếm gia sư công khai; tài khoản STUDENT/PARENT chỉ tạo được qua lời mời của gia sư. Hệ thống **không lưu số tiền, đơn giá, hình thức thanh toán**; học phí chỉ có trạng thái đã nộp/chưa nộp.

## 2. Yêu cầu chức năng

### FR-1 Xác thực và tài khoản
- FR-1.1 Gia sư tự đăng ký bằng email + mật khẩu.
- FR-1.2 Học sinh/phụ huynh tạo tài khoản qua **lời mời** (token dùng một lần, hết hạn 7 ngày) do gia sư tạo. [CONFIRMED]
- FR-1.3 Đăng nhập trả access token (JWT ngắn hạn) + refresh token.
- FR-1.4 Đăng xuất, đổi mật khẩu. Cơ chế đặt lại mật khẩu: do gia sư tạo link reset mật khẩu cho học sinh/phụ huynh trong lớp của mình (hoặc ADMIN tạo cho mọi user) gửi qua Zalo/tin nhắn (không gửi email SMTP ở MVP); người nhận mở link đặt mật khẩu mới. [CONFIRMED: D-29]
- **AC:** email trùng → 409; mật khẩu < 8 ký tự → 400; sai mật khẩu → 401 (không nói rõ email hay mật khẩu sai); token lời mời / đặt lại mật khẩu hết hạn/đã dùng → 410/400; mật khẩu lưu bằng BCrypt.

### FR-2 Lớp học
- FR-2.1 Tạo lớp: tên, môn (Toán/Lý/khác), mô tả, **loại lớp** (`ONE_ON_ONE` = 1:1, hoặc `GROUP` = nhóm từ 2 học sinh), trạng thái (ACTIVE/ARCHIVED).
- FR-2.2 Sửa, lưu trữ (archive) lớp; xóa mềm, không xóa cứng khi đã có buổi học/điểm.
- FR-2.3 Thêm/bỏ học sinh khỏi lớp (enrollment); một học sinh không thể ghi danh 2 lần vào cùng lớp.
- FR-2.4 Danh sách lớp, tìm kiếm theo tên, lọc theo trạng thái, phân trang.
- FR-2.5 Lớp `ONE_ON_ONE` chỉ có tối đa 1 học sinh đang học. Lớp `GROUP` không giới hạn số học sinh; nếu đang có dưới 2 học sinh thì chỉ hiện cảnh báo (không chặn, vì gia sư cần tạo lớp trước rồi thêm học sinh dần). **[CONFIRMED: D-23, D-24]**
- FR-2.6 Đổi loại lớp: `ONE_ON_ONE` → `GROUP` luôn được; `GROUP` → `ONE_ON_ONE` chỉ khi có tối đa 1 học sinh đang học.
- **AC:** tên rỗng → 400; loại lớp thiếu/không hợp lệ → 400; gia sư B gọi API lớp của gia sư A → 404; ghi danh trùng → 409; thêm học sinh thứ 2 vào lớp 1:1 → 422 `ONE_ON_ONE_FULL`; đổi `GROUP` → `ONE_ON_ONE` khi có ≥ 2 học sinh đang học → 422.

### FR-3 Lịch dạy và buổi học
- FR-3.1 Tạo lịch lặp hàng tuần (thứ, giờ bắt đầu/kết thúc, ngày hiệu lực) → sinh các buổi học (session) cụ thể.
- FR-3.2 Tạo/sửa/hủy buổi học đơn lẻ; buổi có trạng thái SCHEDULED / COMPLETED / CANCELLED, chủ đề, ghi chú.
- FR-3.3 **Chống trùng:** không cho một gia sư có hai buổi (không bị hủy) chồng thời gian, kể cả tạo đồng thời.
- FR-3.4 Cảnh báo (không chặn) khi một học sinh bị chồng lịch giữa hai lớp. **[CONFIRMED]**
- FR-3.5 Xem lịch theo tuần/tháng (gia sư), lịch của tôi (học sinh), lịch của con (phụ huynh).
- **AC:** `end_at <= start_at` → 400; buổi chồng giờ với buổi khác của cùng gia sư → 409 kèm buổi xung đột; hai request tạo buổi trùng cùng lúc chỉ một thành công; buổi CANCELLED không gây xung đột; hai buổi liền kề (kết thúc 10:00, bắt đầu 10:00) **không** xung đột.

### FR-4 Điểm danh
- FR-4.1 Điểm danh từng học sinh theo buổi: PRESENT, LATE, ABSENT_EXCUSED, ABSENT_UNEXCUSED, kèm ghi chú.
- FR-4.2 Điểm danh hàng loạt (mặc định tất cả có mặt, chỉnh ngoại lệ) trong một transaction.
- FR-4.3 Sửa điểm danh sau khi lưu; ghi lại thời điểm sửa.
- **AC:** chỉ học sinh đã ghi danh vào lớp mới được điểm danh; mỗi (buổi, học sinh) chỉ có 1 bản ghi; bulk lỗi giữa chừng thì rollback toàn bộ; chỉ gia sư của lớp được điểm danh.

### FR-5 Bài tập và điểm số
- FR-5.1 Giao bài tập cho lớp: tiêu đề, mô tả, hạn nộp, loại (HOMEWORK/QUIZ/EXAM/MOCK_TEST/OTHER). Thang điểm 10 cố định (không dùng hệ số, không có max_score biến thiên). [CONFIRMED: D-30]
- FR-5.2 Tự tạo bản ghi điểm (chưa chấm) cho mọi học sinh trong lớp khi giao bài; học sinh ghi danh sau cũng được bổ sung.
- FR-5.3 Nhập/sửa điểm và nhận xét từng học sinh; nhập điểm hàng loạt.
- FR-5.4 Trạng thái từng bài của học sinh: ASSIGNED, SUBMITTED, GRADED, MISSING.
- **AC:** điểm < 0 hoặc > 10 → 400; học sinh thấy điểm của mình sau khi gia sư lưu; không thấy điểm của bạn.

### FR-6 Học phí theo số buổi (chỉ theo dõi, không xử lý tiền)
**Nguyên tắc [CONFIRMED]:** web không lưu số tiền, đơn giá, hình thức thanh toán. Gia sư và phụ huynh tự trao đổi học phí qua Zalo/tin nhắn. Web chỉ ghi **đợt học phí** (số buổi) và ô tích **đã nộp / chưa nộp**. Tiện ích tạo và sao chép tin nhắn nhắc học phí có sẵn để gia sư gửi Zalo.
- FR-6.1 Gia sư mở **đợt học phí** cho một học sinh trong một lớp: số buổi $N \ge 1$, ghi chú tùy chọn; mặc định trạng thái `UNPAID`. Mỗi học sinh trong lớp nhóm có các đợt riêng, độc lập với bạn cùng lớp. **[CONFIRMED: D-22]**
- FR-6.2 Tích `PAID` / bỏ tích về `UNPAID` cho từng đợt; lưu thời điểm tích; thao tác lặp lại không gây lỗi.
- FR-6.3 Tự tính buổi đã học và còn lại (truy vấn, không lưu cột dẫn xuất). Gọi $U$ là số buổi đã học của học sinh trong lớp, $N_k$ là số buổi của đợt thứ $k$ (xếp theo thời gian tạo, không tính đợt đã void):

$$\text{remaining} = \sum_k N_k - U$$

  Buổi đã học được tính dồn vào các đợt theo thứ tự (đợt cũ đầy trước):

$$u_k = \min\!\Big(N_k,\ \max\!\big(0,\ U - \sum_{j<k} N_j\big)\Big)$$

  trong đó $u_k$ là số buổi đã dùng của đợt $k$. `U` = số buổi `COMPLETED` mà học sinh có điểm danh `PRESENT`, `LATE` hoặc `ABSENT_UNEXCUSED`. **[CONFIRMED: D-22]**
- FR-6.4 Cảnh báo hiển thị cho gia sư (để gia sư tự nhắn Zalo cho phụ huynh): `CYCLE_LOW` khi đợt hiện tại còn $N_k - u_k \le 2$ buổi; `CYCLE_DONE_UNPAID` khi đợt đã học đủ $N$ buổi mà vẫn `UNPAID`; `NO_OPEN_CYCLE` khi $U > \sum_k N_k$ (đã học vượt, chưa mở đợt mới).
- FR-6.5 Sửa số buổi/ghi chú của đợt; hủy đợt (void, không xóa cứng).
- FR-6.6 Học sinh/phụ huynh chỉ xem: đợt hiện tại, số buổi đã học/còn lại, trạng thái đã nộp/chưa nộp.
- **AC:** $N \le 0$ → 400; STUDENT/PARENT gọi API tích nộp → 403; số liệu đúng trong các ca: chưa học, vắng có phép, vắng không phép, đi muộn, buổi bị hủy, nhiều đợt, học vượt, đợt bị void.

### FR-7 Báo cáo tiến độ
- FR-7.1 Tạo báo cáo cho một học sinh trong một lớp theo **khoảng ngày do gia sư tự chọn** (từ ngày – đến ngày, không ép theo tháng/kỳ; chỉ tính buổi và điểm nằm trong khoảng): tỉ lệ đi học, danh sách điểm, điểm trung bình, buổi còn lại, trạng thái học phí (đã/chưa nộp), nhận xét của gia sư.
- FR-7.2 Trạng thái DRAFT → PUBLISHED; phụ huynh chỉ thấy PUBLISHED.
- FR-7.3 Báo cáo lưu **snapshot dữ liệu** tại thời điểm công bố (không đổi khi điểm sửa sau đó).
- FR-7.4 Xuất PDF (báo cáo) và CSV (điểm danh, điểm, trạng thái học phí).
- Công thức điểm trung bình (trung bình cộng thang điểm 10, không dùng hệ số):

$$\bar{s} = \frac{1}{m} \sum_{i=1}^m s_i$$

  trong đó $m$ là số đầu điểm đã chấm của học sinh trong khoảng thời gian báo cáo.
  Tỉ lệ đi học: $\text{attendance rate} = \dfrac{\text{PRESENT} + \text{LATE}}{\text{COMPLETED sessions}}$
- **AC:** không có điểm nào → hiển thị "chưa có dữ liệu" (không chia cho 0); phụ huynh không xem được báo cáo DRAFT hoặc của học sinh khác.

### FR-8 Cổng học sinh/phụ huynh (chỉ đọc)
- Trang tổng quan: buổi học sắp tới, bài tập sắp hạn, điểm gần đây, buổi còn lại và trạng thái học phí, báo cáo mới.
- Phụ huynh có bộ chọn con nếu có nhiều con.

## 3. Yêu cầu phi chức năng
- NFR-1 Bảo mật: BCrypt, JWT, CORS chỉ cho origin frontend, không log mật khẩu/token, secret qua biến môi trường.
- NFR-2 Validation: mọi input kiểm tra ở backend (Bean Validation); lỗi trả cùng một định dạng (RFC 7807 Problem Details).
- NFR-3 Hiệu năng: danh sách có phân trang; API danh sách < 500 ms với ~1.000 bản ghi (điều kiện thử nghiệm local).
- NFR-4 Dữ liệu: migration bằng Flyway; ràng buộc (FK, UNIQUE, CHECK, EXCLUDE) đặt ở DB, không chỉ ở code.
- NFR-5 Giao diện: responsive (dùng được trên điện thoại vì phụ huynh xem chủ yếu bằng điện thoại), tiếng Việt, trạng thái loading/lỗi/rỗng rõ ràng.
- NFR-6 Múi giờ: lưu UTC (`timestamptz`), hiển thị `Asia/Ho_Chi_Minh`.
- NFR-7 Khả năng bảo trì: có test tự động cho nghiệp vụ lõi, README chạy được bằng một lệnh `docker compose up`.

## 4. Ngoài phạm vi (không làm nếu chưa được duyệt)
Mọi tính năng liên quan đến tiền (thanh toán online, số tiền/đơn giá, hóa đơn, công nợ), sàn kết nối/tìm kiếm gia sư, upload file, chat, thông báo push/SMS/Zalo, đa tổ chức, app mobile, phân tích AI.

## 5. Định nghĩa "Hoàn thành" của một tính năng
1. Đáp ứng mọi AC trong tài liệu này.
2. Có test tự động (hợp lệ, dữ liệu sai, trái quyền).
3. `mvn verify` và `npm run build && npm test` đều xanh.
4. API_SPEC/ARCHITECTURE được cập nhật nếu thay đổi.
5. Đã kiểm tra thủ công trên giao diện cho các vai trò liên quan.
