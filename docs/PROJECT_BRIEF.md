# PROJECT_BRIEF — TutorHub

> Trạng thái: DRAFT v0.3 (đã cập nhật theo quyết định ngày 2026-09-30). Mục đánh dấu **[CONFIRMED]** là người dùng đã chốt; **[ASSUMPTION]** là giả định cần xác nhận, AI không được tự coi là yêu cầu đã duyệt.

## 1. Tóm tắt

TutorHub là web quản lý lớp học và gia sư: giúp gia sư quản lý nhiều lớp, buổi học, điểm danh, bài tập, điểm số, học phí và gửi báo cáo tiến độ cho phụ huynh. **[CONFIRMED]**

## 1b. Định vị sản phẩm **[CONFIRMED]**

* TutorHub là **công cụ hỗ trợ gia sư quản lý lịch dạy và quản lý lớp**. Chỉ gia sư nào có nhu cầu đó mới dùng.

* **Không phải sàn/nền tảng phân phối gia sư**: không có danh bạ gia sư, không tìm kiếm/đánh giá gia sư, không ghép gia sư với học sinh.

* **Không xử lý tiền**: không thanh toán online, không lưu số tiền/đơn giá/hóa đơn. Gia sư và phụ huynh tự trao đổi học phí qua Zalo/tin nhắn ngoài hệ thống. Trên web chỉ có trạng thái **đã nộp / chưa nộp học phí**.

* Học sinh và phụ huynh là người được gia sư mời vào để xem lịch, điểm, buổi còn lại, báo cáo.

## 2. Vấn đề cần giải quyết

Gia sư dạy nhiều lớp/nhiều học sinh thường quản lý bằng Zalo, Excel, sổ tay nên:

* Khó theo dõi buổi đã học / còn lại và trạng thái học phí từng học sinh.
* Dễ xếp trùng lịch dạy.
* Mất thời gian tổng hợp điểm và viết báo cáo cho phụ huynh.
* Điểm danh, điểm số, bài tập nằm rải rác ở nhiều nơi.

## 3. Người dùng (4 vai trò, mỗi vai trò có tài khoản riêng) **[CONFIRMED]**

| Vai trò            | Nhu cầu chính                                                     |
| ------------------ | ----------------------------------------------------------------- |
| ADMIN (quản trị)   | Toàn quyền truy cập và quản lý mọi dữ liệu, lớp học, tài khoản   |
| TUTOR (gia sư)     | Quản lý lớp, lịch, điểm danh, bài tập, điểm, học phí, tạo báo cáo |
| STUDENT (học sinh) | Xem lịch học, bài tập, điểm, số buổi còn lại, báo cáo của mình    |
| PARENT (phụ huynh) | Xem tiến độ, điểm danh, học phí, báo cáo của con mình             |

## 4. Mục tiêu

* Một nơi duy nhất quản lý lớp, buổi học, điểm danh, bài tập, điểm, học phí. **[CONFIRMED]**
* Báo cáo tiến độ tạo nhanh, hiển thị cho phụ huynh và hỗ trợ xuất dữ liệu. **[CONFIRMED]**
* Nhiều gia sư dùng chung hệ thống; mỗi người chỉ truy cập lớp và dữ liệu thuộc quyền của mình. **[CONFIRMED]**
* Chống ghi nhận trùng buổi học và trùng lịch dạy. **[CONFIRMED]**
* Mục tiêu học tập: thiết kế DB quan hệ, phân quyền theo vai trò, transaction, truy vấn báo cáo, xuất dữ liệu. **[CONFIRMED]**

## 5. Công nghệ đã chốt

* Backend: Java + Spring Boot **[CONFIRMED]**
* Frontend: React + TypeScript + Tailwind CSS **[CONFIRMED]**
* Database: PostgreSQL, chạy bằng Docker **[CONFIRMED]**
* Deploy: Render hoặc Railway **[CONFIRMED]**
* Phiên bản cụ thể (Java 21, Spring Boot 3.x, Maven, Vite…): xem `DECISIONS.md` **[ASSUMPTION]**

## 6. Phạm vi MVP **[CONFIRMED]**

Trong MVP:

1. Đăng ký/đăng nhập, 4 vai trò (ADMIN, TUTOR, STUDENT, PARENT), phân quyền theo vai trò và theo quyền sở hữu lớp (ADMIN có toàn quyền truy cập).

2. Lớp học với hai loại:

   * **1:1**: tối đa 1 học sinh đang active.
   * **Nhóm**: từ 2 học sinh trở lên; hệ thống cho phép tạo lớp khi chưa đủ 2 học sinh nhưng hiển thị cảnh báo cho gia sư.

3. Danh sách học sinh và quản lý việc tham gia lớp.

4. Lịch dạy gồm:

   * Lịch lặp.
   * Buổi học cụ thể.
   * Chống trùng lịch của cùng một gia sư bằng ràng buộc ở tầng DB.

5. Điểm danh theo từng buổi:

   * Có mặt: tính là đã học.
   * Vắng không phép: trừ 1 buổi.
   * Vắng có phép: không trừ buổi.
   * MVP chưa xây hệ thống "buổi bù" riêng. Nếu cần dạy bù, gia sư có thể tạo một buổi học cụ thể mới.

6. Giao bài tập và nhập điểm.

7. Theo dõi buổi đã học / còn lại theo **đợt học phí**:

   * Mỗi đợt có số buổi `N` do gia sư nhập riêng cho từng học sinh trong từng lớp.
   * Các buổi đã học được phân bổ vào các đợt theo thứ tự cũ trước.
   * Web chỉ lưu trạng thái **đã nộp / chưa nộp**.
   * Không lưu số tiền, đơn giá, hóa đơn hoặc công nợ.

8. Điểm:

   * Sử dụng thang điểm 10.
   * Có thể phân loại điểm theo loại đánh giá như bài tập, kiểm tra, thi thử hoặc khác.
   * MVP chưa sử dụng hệ số điểm.

9. Báo cáo tiến độ cho phụ huynh theo **khoảng ngày do gia sư tự chọn**:

   * Xem trên web.
   * Hỗ trợ xuất PDF/CSV.

10. Cổng xem cho học sinh và phụ huynh:

    * Học sinh xem dữ liệu và báo cáo đã công bố của bản thân.
    * Phụ huynh xem dữ liệu của con được liên kết.
    * Không có quyền chỉnh sửa dữ liệu học tập.

11. Học sinh/phụ huynh tham gia hệ thống thông qua **invite link** do gia sư tạo.

12. Tiện ích tạo và **sao chép tin nhắn nhắc học phí** để gia sư tự gửi qua Zalo/tin nhắn bên ngoài hệ thống.

    * Không gửi trực tiếp tới Zalo.
    * Không xử lý hoặc lưu số tiền học phí.

13. MVP chỉ hỗ trợ **giao diện tiếng Việt**.

Ngoài MVP (chưa làm):

* Mọi tính năng liên quan đến tiền: thanh toán online, số tiền/đơn giá, hóa đơn, công nợ. Học phí trao đổi qua Zalo/tin nhắn ngoài hệ thống.
* Sàn kết nối gia sư: danh bạ/tìm kiếm/đánh giá gia sư, ghép gia sư–học sinh.
* Upload file bài làm, chấm bài trong ứng dụng.
* Video call / lớp học trực tuyến.
* Thông báo Zalo/SMS.
* Email notification trong MVP.
* App mobile native.
* Đa ngôn ngữ.
* Đa trung tâm/tổ chức.
* Hệ thống buổi bù riêng.
* Hệ thống hệ số điểm.
* Cấu hình chính sách điểm danh riêng cho từng lớp.

## 7. Ràng buộc

* Dự án cá nhân, một lập trình viên + AI; ưu tiên hoàn thành được, test được, deploy được.

* Người làm đã biết Java cơ bản/OOP, đang học Spring Boot: AI phải giải thích quyết định kỹ thuật, không chỉ đưa code.

* Deploy trên gói miễn phí/giá rẻ của Render/Railway, chú ý cold start và giới hạn DB.

* Dữ liệu cá nhân học sinh (tên, điểm, SĐT phụ huynh) là dữ liệu nhạy cảm: không commit dữ liệu thật vào repo.

* AI không được tự ý mở rộng scope hoặc thêm tính năng ngoài PROJECT_BRIEF nếu chưa được người dùng xác nhận.

* Khi có quyết định kỹ thuật quan trọng, AI phải ghi nhận vào `DECISIONS.md` thay vì âm thầm thay đổi kiến trúc.

## 8. Rủi ro và giả định

| #  | Giả định / rủi ro                                                                                                  | Cách xử lý                                                     |
| -- | ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------- |
| A1 | **[CONFIRMED]** Học phí tính theo số buổi; web chỉ lưu trạng thái đã nộp/chưa nộp                                  | Mỗi học sinh có các đợt học phí riêng                          |
| A2 | **[CONFIRMED]** Vắng không phép trừ 1 buổi; vắng có phép không trừ buổi                                            | MVP dùng chính sách cố định; có thể cấu hình ở phase sau       |
| A3 | **[CONFIRMED]** Học sinh/phụ huynh không tự đăng ký mà vào bằng **invite link** do gia sư tạo                      | Giảm rủi ro tài khoản giả và đảm bảo quyền truy cập            |
| A4 | **[CONFIRMED]** Một phụ huynh có thể có nhiều con; một học sinh có thể có nhiều phụ huynh                          | Bảng liên kết `parent_students`                                |
| A5 | **[CONFIRMED]** Học sinh có thể học nhiều lớp, nhiều gia sư                                                        | Bảng `class_enrollments`                                       |
| A6 | **[CONFIRMED]** Mỗi đợt học phí có số buổi `N` do gia sư nhập riêng cho từng học sinh trong từng lớp               | Buổi đã học được phân bổ vào đợt cũ trước                      |
| A7 | **[CONFIRMED]** Lớp 1:1 tối đa 1 học sinh active; lớp nhóm cần từ 2 học sinh để được coi là đủ điều kiện hoạt động | Cho phép tạo lớp nhóm dưới 2 học sinh nhưng hiển thị cảnh báo  |
| R1 | Trùng lịch khi hai request đồng thời                                                                               | PostgreSQL exclusion constraint + transaction + test đồng thời |
| R2 | Lộ dữ liệu giữa các gia sư                                                                                         | Kiểm tra ownership ở service layer + test truy cập trái quyền  |
| R3 | Logic phân bổ buổi học vào các đợt học phí có thể gây sai dữ liệu                                                  | Transaction + quy tắc phân bổ rõ ràng + integration test       |
| R4 | Deploy miễn phí có thể có cold start hoặc giới hạn tài nguyên                                                      | Thiết kế MVP nhẹ, theo dõi giới hạn Render/Railway             |

## 9. Quyết định đã chốt (2026-09-30)

1. Học phí tính theo số buổi học; web chỉ có trạng thái đã nộp/chưa nộp; trao đổi học phí ngoài web.
2. Mỗi đợt học phí có số buổi `N` do gia sư nhập riêng cho từng học sinh trong từng lớp.
3. Lớp do gia sư chọn: **1:1 hoặc nhóm**.
4. Lớp 1:1 tối đa 1 học sinh active.
5. Lớp nhóm cần từ 2 học sinh trở lên; dưới 2 học sinh hệ thống chỉ cảnh báo.
6. Vắng có phép không trừ buổi.
7. Vắng không phép trừ 1 buổi.
8. MVP chưa có hệ thống buổi bù riêng.
9. Điểm sử dụng thang 10.
10. MVP chưa sử dụng hệ số điểm.
11. Báo cáo tiến độ theo khoảng ngày do gia sư tự chọn.
12. MVP chỉ hỗ trợ tiếng Việt.
13. MVP chưa cần email.
14. Có tiện ích tạo và sao chép tin nhắn nhắc học phí để gia sư tự gửi qua Zalo/tin nhắn.
15. TutorHub là công cụ quản lý cho gia sư, không xử lý tiền và không phải sàn gia sư.
16. Học sinh/phụ huynh tham gia thông qua invite link do gia sư tạo.
17. Không mở rộng scope nếu chưa có xác nhận của người dùng.
18. Thêm vai trò ADMIN có toàn quyền truy cập và quản lý mọi tài nguyên trên hệ thống.
19. Học sinh (STUDENT) được xem báo cáo tiến độ đã công bố (PUBLISHED) của chính mình.

## 10. Nguyên tắc làm việc với AI **[CONFIRMED]**

Khi AI hỗ trợ phát triển TutorHub:

1. **Không tự suy diễn yêu cầu mới.** Nếu PROJECT_BRIEF chưa quyết định một vấn đề ảnh hưởng đến nghiệp vụ hoặc kiến trúc, AI phải hỏi trước khi triển khai.

2. **Giải thích trước khi code.** Với mỗi phần quan trọng, AI cần giải thích:

   * Vì sao chọn cách này.
   * Nó giải quyết vấn đề gì.
   * Có trade-off gì.
   * Sau này có thể mở rộng như thế nào.

3. **Ưu tiên kiến trúc đơn giản nhưng đúng.** Không over-engineering cho một dự án cá nhân.

4. **Database phải đảm bảo tính toàn vẹn dữ liệu.** Những quy tắc quan trọng không chỉ kiểm tra ở frontend/backend mà cần cân nhắc constraint hoặc transaction ở DB.

5. **Phân quyền phải được kiểm tra ở backend.** Frontend chỉ dùng để ẩn/hiện UI, không được coi frontend là lớp bảo mật.

6. **Mọi thay đổi kiến trúc hoặc quyết định kỹ thuật quan trọng phải được ghi vào `DECISIONS.md`.**

7. **Mỗi phase phải có tiêu chí hoàn thành rõ ràng** và được test trước khi chuyển sang phase tiếp theo.

8. **Không dùng dữ liệu cá nhân thật** trong source code, database seed, screenshot, test hoặc repository công khai.

9. Khi có nhiều phương án kỹ thuật, AI cần trình bày các phương án và lý do chọn một phương án trước khi triển khai.

10. **Mục tiêu cuối cùng của dự án không chỉ là chạy được**, mà người phát triển phải hiểu được cách hệ thống hoạt động, đặc biệt về:

    * REST API.
    * Spring Boot.
    * Authentication & Authorization.
    * PostgreSQL và thiết kế DB quan hệ.
    * Transaction.
    * Data validation.
    * Testing.
    * React/TypeScript.
    * Deployment.
    * Bảo mật và ownership-based access control.
