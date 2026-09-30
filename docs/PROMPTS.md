# PROMPTS — Bộ prompt dùng với AI cho TutorHub

Thay các phần trong `[...]`. Luôn dán/đính kèm `AGENTS.md` và các tài liệu liên quan trong `docs/`. Dùng theo thứ tự.

---
## P0 — Khởi động phiên (dán đầu mỗi cuộc trò chuyện mới)
```
Bạn là Senior Software Engineer đồng hành cùng tôi xây dựng TutorHub (web quản lý lớp học và gia sư).
Tôi đã biết Java cơ bản/OOP, đang học Spring Boot. Hãy giải thích quyết định kỹ thuật bằng tiếng Việt dễ hiểu, không chỉ đưa code.

Hãy đọc AGENTS.md, docs/DECISIONS.md và phần task hiện tại trong docs/ROADMAP.md, rồi tóm tắt lại trong 10 dòng:
- mục tiêu sản phẩm và phạm vi MVP,
- công nghệ đã chốt (CONFIRMED) và những gì còn là PROPOSED,
- trạng thái hiện tại và task tiếp theo.
Chưa viết code. Nêu mâu thuẫn hoặc thông tin thiếu nếu có.
```

## P1 — Rà soát và chốt tài liệu (dùng trước khi code)
```
Đọc toàn bộ docs/. Hãy đóng vai Product Analyst kiêm Architect phản biện:
1. Liệt kê mâu thuẫn giữa các tài liệu (schema vs API vs requirements).
2. Liệt kê giả định chưa xác nhận và rủi ro.
3. Đối chiếu với các quyết định đã chốt trong PROJECT_BRIEF §9.
4. Đề xuất chỉnh sửa cụ thể (diff) cho từng file.
Không tự đổi yêu cầu đã CONFIRMED. Không viết code. Hỏi tôi trước khi chốt.
```

## P2 — Thực hiện một task (dùng nhiều nhất)
```
Hãy thực hiện task [T3.2 — tên task] trong dự án hiện tại.

Trước khi code:
- Đọc AGENTS.md, các tài liệu liên quan (REQUIREMENTS FR-[x], ARCHITECTURE §[y], API_SPEC §[z]) và code hiện có.
- Nêu tiêu chí nghiệm thu, kế hoạch thay đổi, danh sách file sẽ tạo/sửa. Dừng lại chờ tôi duyệt nếu có thay đổi schema/API/dependency.

Trong khi làm:
- Chỉ làm phạm vi task này; tái sử dụng kiến trúc và quy ước hiện có.
- Viết test cho: hợp lệ, dữ liệu sai, trái quyền, và trường hợp biên trong TEST_PLAN.
- Không xóa/skip test. Không thêm dependency khi chưa hỏi.
- Giải thích các phần quan trọng và kiến thức mới bằng ví dụ ngắn.

Sau khi làm:
- Chạy `mvn verify` / `npm test` (nếu môi trường cho phép) và báo chính xác lệnh + kết quả; nếu không chạy được, nói rõ.
- Liệt kê file đã tạo/sửa và lý do; đối chiếu từng tiêu chí nghiệm thu.
- Nêu lỗi/rủi ro còn lại; cập nhật ROADMAP/API_SPEC/DECISIONS nếu cần.
Không tuyên bố hoàn thành nếu thiếu bằng chứng kiểm tra.
```

## P3 — Thiết kế trước khi code cho một tính năng phức tạp
```
Trước khi code [tính năng: chống trùng buổi học / báo cáo snapshot / học phí], hãy đề xuất thiết kế chi tiết:
luồng dữ liệu, thay đổi schema (migration), API, các trường hợp lỗi, cách test, phương án thay thế và đánh đổi.
Giải thích bằng ví dụ cụ thể. Không viết code; để tôi review trước.
```

## P4 — Review độc lập
```
Đóng vai reviewer độc lập cho các thay đổi trong `git diff [branch]`.
Đối chiếu với REQUIREMENTS, ARCHITECTURE, API_SPEC, TEST_PLAN. Kiểm tra: logic, trường hợp biên,
phân quyền (gia sư A đọc dữ liệu gia sư B? phụ huynh đọc con người khác?), race condition, transaction,
xử lý lỗi, N+1 query, lộ thông tin nhạy cảm, tính nhất quán, test còn thiếu.
Với mỗi vấn đề: mức độ, vị trí file, điều kiện xảy ra, cách sửa, test xác nhận.
Phân biệt "lỗi đã xác nhận" với "rủi ro cần kiểm tra thêm". Không sửa code; báo cáo trước.
```

## P5 — Debug
```
Tôi gặp lỗi: [mô tả]. Cách tái hiện: [các bước]. Log/stack trace: [dán, đã xóa secret/dữ liệu thật].
Hãy: (1) đưa ra giả thuyết theo thứ tự khả năng, (2) chỉ cách kiểm chứng từng giả thuyết,
(3) chỉ sau khi xác định nguyên nhân mới đề xuất sửa + test tái hiện lỗi. Giải thích vì sao lỗi xảy ra.
```

## P6 — Tổng kết phiên và bàn giao ngữ cảnh
```
Tổng kết trạng thái dự án sau phiên này dựa trên thay đổi thực tế trong repo:
- task đã xong (kèm bằng chứng test), task dang dở (phần đã làm/chưa làm),
- lỗi còn tồn tại và cách tái hiện, quyết định kỹ thuật mới, thay đổi API/schema,
- việc tiếp theo theo thứ tự phụ thuộc, lệnh cần chạy lại khi tiếp tục.
Cập nhật bảng "Nhật ký trạng thái" trong docs/ROADMAP.md và docs/DECISIONS.md.
Không đánh dấu xong dựa trên suy đoán. Không ghi bí mật. Cuối cùng viết bản tóm tắt <= 15 dòng
để tôi dán vào cuộc trò chuyện mới.
```

## P7 — Khởi tạo frontend từ API spec
```
Dựa trên docs/API_SPEC.md và REQUIREMENTS, tạo type TypeScript và hooks TanStack Query cho nhóm API [Classes].
Không sửa backend. Nếu phát hiện API_SPEC thiếu hoặc mơ hồ, liệt kê và hỏi tôi trước.
Màn hình cần có: loading, lỗi (theo `code`), rỗng, responsive 375px.
```
