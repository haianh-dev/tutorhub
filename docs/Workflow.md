## 1. Workflow AI
EXPLAIN
↓
Tạo User Entity
↓
IMPLEMENT
↓
Code
↓
VERIFY
↓
mvn test
↓
PASS
↓
Review
↓
Next Step
## 2. AI Review
Hãy VERIFY task vừa hoàn thành.
Không được sửa code ngay.
Kiểm tra:
Requirement đã được đáp ứng chưa?
Architecture có bị vi phạm không?
Code có phù hợp với coding convention hiện tại không?
Có lỗi logic nào không?
Có edge case nào chưa xử lý không?
Test nào đã chạy?
Kết quả test là gì?
Có file nào thay đổi ngoài phạm vi task không?
Có dependency hoặc configuration nào bị thay đổi không?
Task có thực sự đủ điều kiện đánh dấu DONE không?
Hãy báo cáo theo format:
STATUS: PASS / NEED FIX
IMPLEMENTED:
...
TESTED:
...
ISSUES:
...
FILES CHANGED:
...
REQUIREMENT CHECK:
...
NEXT TASK:
...
Nếu có vấn đề, hãy dừng lại và báo cáo. Không tự ý mở rộng phạm vi.
## 3. Update Memory
Code
 ↓
Test
 ↓
Review
 ↓
Update documentation
 ↓
Git commit
## 4. Quy trình git 
Ví dụ: 
Task T3
   ↓
AI phân tích
   ↓
AI implementation
   ↓
AI test
   ↓
AI review
   ↓
Bạn kiểm tra
   ↓
git diff
   ↓
Commit
   ↓
Task DONE
## 5. Quy trình làm việc
Ví dụ:
Bước 1
Đọc toàn bộ project và onboarding theo PROJECT_BRIEF.md, REQUIREMENTS.md, ROADMAP.md...
AI đọc.
↓
Bước 2
AI báo:
Tôi hiểu project như sau...
Bạn sửa nếu cần.
↓
Bước 3
Phân tích T1. Chưa code.
AI phân tích.
↓
Bước 4
OK, bắt đầu Step 1.
AI làm.
↓
Bước 5
Giải thích những thay đổi vừa thực hiện.
AI giải thích.
↓
Bước 6
Verify Step 1.
AI chạy test.
↓
Bước 7
Bạn kiểm tra.
↓
Bước 8
OK. Sang Step 2.
↓
Bước 9
Lặp lại.
↓
Cuối session
Tổng kết session và cập nhật project documentation.
AI cập nhật.
↓
Git commit.


