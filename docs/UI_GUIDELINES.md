# UI_GUIDELINES — TutorHub (Neubrutalism)

> Dành cho AI agent. Đọc hết file này trước khi viết hoặc sửa bất kỳ UI nào ở `frontend/`.
> Nguồn: prompt landing page Neubrutalism của người dùng, đã **chuyển thể sang ứng dụng quản lý** (dashboard, lịch, bảng, form). Các section dành riêng cho landing page (Hero, Marquee, Pricing, Testimonials, CTA banner, Footer pill) **không dùng** trong TutorHub.
> Mục đánh dấu **[ADAPTED]** là chỗ khác prompt gốc và có lý do ghi kèm. Mục **[PROPOSED]** cần người dùng duyệt.

---

## 0. Phạm vi và thứ tự ưu tiên

1. File này chỉ quyết định **giao diện** (màu, chữ, khoảng cách, component, bố cục, hành vi hover/focus). **Không** quyết định nghiệp vụ.
2. Khi mâu thuẫn, thứ tự ưu tiên: `AGENTS.md` / `AIWORKINGRULES.md` → `REQUIREMENTS.md` / `API_SPEC.md` / `DECISIONS.md` → file này.
3. Khi làm UI, **không được**: đổi logic, đổi API, đổi route/guard, thêm tính năng mới, thêm dependency khi chưa hỏi, sửa file không liên quan.
4. Ràng buộc sản phẩm luôn đúng, kể cả khi thiết kế "đẹp hơn" nếu vi phạm:
   - **Không có tiền**: không ô nhập/hiển thị số tiền, đơn giá, ký hiệu `₫`/`VND`. Học phí chỉ có đợt N buổi và trạng thái `ĐÃ NỘP` / `CHƯA NỘP`.
   - **Không phải sàn gia sư**: không trang danh bạ/tìm kiếm/đánh giá gia sư.
   - **Chỉ tiếng Việt**; không email; không mobile app riêng (web responsive).
   - Điểm thang 10, không hệ số.
   - Bốn vai trò: ADMIN, TUTOR, STUDENT, PARENT. Frontend chỉ ẩn/hiện theo vai trò để làm UX; **quyền thật do backend**.
5. Dữ liệu mẫu/placeholder luôn là **dữ liệu giả** (`@example.com`, tên bịa). Không dùng ảnh/tên học sinh thật.

---

## 1. Nguyên tắc cốt lõi

1. **Poster-like nhưng dùng được hằng ngày.** Khối vuông vức, viền đen dày, bóng cứng, màu pop-art. Nhưng đây là app thao tác nhiều: ưu tiên rõ ràng và tốc độ hơn trang trí.
2. **Nhấn mạnh có chủ đích.** Mỗi màn hình chỉ có **một** hành động chính (nút vàng). Không rải màu cho đẹp.
3. **Không bao giờ chỉ dùng màu để truyền thông tin.** Mọi trạng thái có chữ (và ký hiệu nếu cần) đi kèm màu.
4. **Mobile-first.** Phụ huynh xem chủ yếu bằng điện thoại. Thiết kế từ 375px lên, không cuộn ngang toàn trang.
5. **Nhất quán tuyệt đối:** mọi thành phần tương tác dùng cùng độ dày viền, cùng bóng, cùng góc vuông, cùng hiệu ứng hover.
6. **Dùng token, không dùng magic number.** Màu, bóng, cỡ chữ, khoảng cách đều lấy từ token ở §2–§4.
7. **Mọi màn hình có đủ 3 trạng thái:** loading, lỗi, rỗng (xem §6.14).

---

## 2. Design tokens

### 2.1 Màu (chỉ 6 màu, không thêm hex ngoài bảng này)

| Token Tailwind | Giá trị | Vai trò |
|---|---|---|
| `cream` | `#FFFBF0` | Nền trang |
| `paper` | `#FFFFFF` | Nền card/bảng/form |
| `ink` | `#000000` | Chữ, viền, bóng, nền khối tương phản |
| `yellow` | `#FFEB3B` | Hành động chính, highlight, mục đang chọn |
| `coral` | `#FF5252` | Nguy hiểm, lỗi, chưa nộp, vắng không phép, xung đột |
| `blue` | `#2196F3` | Thông tin, đã lên lịch, mục thông tin trung tính |

**Quy tắc chữ trên nền màu [quan trọng, theo độ tương phản]:**

| Nền | Chữ được dùng | Ghi chú |
|---|---|---|
| `cream`, `paper`, `yellow` | `ink` | tương phản rất cao |
| `coral`, `blue` | **`ink` only** | chữ trắng trên coral/blue chỉ đạt khoảng 3:1, **cấm** cho chữ nhỏ |
| `ink` | `paper` hoặc `cream` | tương phản 21:1 |

**Không** thêm màu thứ 7 (ví dụ xanh lá cho "thành công"). Nếu thấy cần, hỏi người dùng. Trạng thái thành công/tốt dùng chữ + ký hiệu `✓` trên nền `paper`/`blue`, xem bảng §9.

### 2.2 Khai báo trong Tailwind v4 (`frontend/src/index.css`)

Dự án dùng Tailwind v4 qua `@tailwindcss/vite` (không có `tailwind.config.js`). Khai báo token bằng `@theme`:

```css
@import "tailwindcss";

@theme {
  /* Colors */
  --color-cream: #FFFBF0;
  --color-paper: #FFFFFF;
  --color-ink: #000000;
  --color-yellow: #FFEB3B;
  --color-coral: #FF5252;
  --color-blue: #2196F3;

  /* Fonts (xem §3: DM Mono được thay bằng Space Mono) */
  --font-heading: "Space Grotesk", ui-sans-serif, system-ui, sans-serif;
  --font-body: "Space Mono", ui-monospace, "SFMono-Regular", Menlo, monospace;

  /* Hard shadows: KHÔNG blur */
  --shadow-hard-sm: 2px 2px 0 var(--color-ink);
  --shadow-hard: 4px 4px 0 var(--color-ink);
  --shadow-hard-lg: 8px 8px 0 var(--color-ink);
}

@layer base {
  html { background: var(--color-cream); color: var(--color-ink); font-family: var(--font-body); }
  h1, h2, h3, h4 { font-family: var(--font-heading); }
  :focus-visible { outline: 3px solid var(--color-ink); outline-offset: 3px; }
  .on-dark :focus-visible { outline-color: var(--color-paper); }
}

/* Thành phần tương tác nổi: viền + bóng cứng + hover dịch chuyển */
@utility brut-pop {
  border: 3px solid var(--color-ink);
  box-shadow: var(--shadow-hard);
  transition: transform 120ms ease, box-shadow 120ms ease, background-color 120ms ease;
  cursor: pointer;
  @media (hover: hover) {
    &:hover:not(:disabled):not([aria-disabled="true"]) {
      transform: translate(-4px, -4px);
      box-shadow: var(--shadow-hard-lg);
    }
  }
  &:active:not(:disabled):not([aria-disabled="true"]) {
    transform: translate(2px, 2px);
    box-shadow: var(--shadow-hard-sm);
  }
  &:disabled, &[aria-disabled="true"] { cursor: not-allowed; opacity: 0.5; box-shadow: none; }
}

/* Khối tĩnh (không hover): card, bảng, panel */
@utility brut-box {
  border: 3px solid var(--color-ink);
  box-shadow: var(--shadow-hard);
}

@media (prefers-reduced-motion: reduce) {
  .brut-pop { transition: none; }
  .brut-pop:hover, .brut-pop:active { transform: none; }
}
```

Ghi chú cho agent:
- Cú pháp `@utility` và `@theme` là của Tailwind v4. Nếu build lỗi, **dừng và báo**, không tự chuyển sang cấu hình v3.
- **Cấm** dùng `rounded-*`, `shadow-md/lg/xl`, `blur`, gradient mềm, `opacity` để làm "mờ đẹp". Có thể kiểm tra bằng `grep -R "rounded\|shadow-\(sm\|md\|lg\|xl\|2xl\)" frontend/src` (kết quả phải rỗng, trừ `shadow-hard*`).

---

## 3. Typography

### 3.1 Font [ADAPTED, cần chú ý]

- **Heading: Space Grotesk** (700–900). Đã xác nhận có hỗ trợ tiếng Việt.
- **Body: Space Mono** (400, 700), thay cho DM Mono trong prompt gốc. **Lý do:** prompt gốc yêu cầu font phải hiển thị đúng dấu tiếng Việt; Space Mono có hỗ trợ tiếng Việt, còn DM Mono thì không xác nhận được là có (nên có nguy cơ vỡ dấu như "ệ", "ữ", "ặ"). Vẫn giữ cảm giác monospace của prompt gốc.
- **[PROPOSED]** Nếu đọc đoạn dài (nhận xét báo cáo, tin nhắn) thấy mỏi mắt vì monospace, đổi riêng `--font-body` sang **Be Vietnam Pro** (thiết kế cho tiếng Việt). Chỉ đổi khi người dùng duyệt.
- Nạp font trong `frontend/index.html` bằng thẻ `<link>` Google Fonts (không thêm npm dependency), nhớ có subset tiếng Việt:

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;700;900&family=Space+Mono:wght@400;700&display=swap&subset=vietnamese" rel="stylesheet">
```

- **Bài kiểm tra bắt buộc:** hiển thị chuỗi `Ệ ẳ ữ ặ Đ ơ ư — Lớp Toán 12A: Điểm danh, Học phí` ở cả heading và body; nếu có ký tự bị thay bằng font khác hoặc ô vuông thì font chưa nạp đúng.

### 3.2 Thang chữ (khai báo thành CSS variable hoặc class, không gõ số rời)

| Token | Giá trị | Weight | Dùng cho |
|---|---|---|---|
| `--fs-display` | `clamp(2.5rem, 7vw, 5rem)` | 900 | Chỉ trang đăng nhập/đăng ký và màn rỗng lớn |
| `--fs-h1` | `clamp(2rem, 5vw, 3rem)` | 700–900 | Tiêu đề trang |
| `--fs-h2` | `clamp(1.5rem, 3vw, 2rem)` | 700 | Tiêu đề khối/section |
| `--fs-h3` | `clamp(1.25rem, 2vw, 1.5rem)` | 700 | Tiêu đề card |
| `--fs-body` | `1rem` | 400 | Nội dung chính, form, bảng |
| `--fs-small` | `0.875rem` | 500 | Nhãn, ghi chú, badge |

(Thang display nhỏ hơn prompt gốc vì đây là app, không phải poster landing.)

### 3.3 Quy tắc chữ

- Heading, nút, nhãn badge: **IN HOA** (`uppercase`), đậm. Nội dung dữ liệu (tên người, ghi chú, nhận xét, ô bảng) viết **bình thường**, không in hoa.
- **Tiếng Việt có dấu chồng:** `line-height` heading in hoa **≥ 1.15**, heading thường ≥ 1.2, body 1.6. Đặt thấp hơn sẽ cắt dấu ở chữ như "Ệ", "Ặ".
- Số liệu (điểm, số buổi, giờ): dùng `font-variant-numeric: tabular-nums` để cột số thẳng hàng.
- Không dùng chữ nghiêng, `text-shadow`. Có thể dùng `-webkit-text-stroke` cho số lớn trang trí (vd số thứ tự bước) nhưng **không** dùng cho chữ cần đọc.
- Chữ dài phải wrap: `break-words`, và `min-w-0` trên phần tử flex/grid con chứa text.

---

## 4. Khoảng cách, lưới, bố cục

### 4.1 Lưới 8px (nghiêm ngặt)

- Tailwind mặc định 1 đơn vị = 4px. **Chỉ dùng số chẵn**: `1`=4px (chỉ dùng cho khe rất nhỏ), `2`=8, `4`=16, `6`=24, `8`=32, `12`=48, `16`=64, `24`=96.
- Ưu tiên **`gap` và `padding`**, hạn chế `margin` (tránh margin collapse). Không dùng `space-*` thay cho `gap`.
- Khoảng cách cố định:
  - Padding card: **24px** (`p-6`); chỉ giảm xuống 16px (`p-4`) khi 375px thật sự chật.
  - Khoảng giữa các card/khối: **24px** (`gap-6`). Card **không bao giờ dính nhau**.
  - Khoảng giữa các phần tử con trong card: **≥ 16px** (`gap-4`).
  - Padding trang (container): **16px mobile / 24px tablet / 48px desktop**; `max-width: 1280px`. Padding đặt ở container bên trong, **không** đặt trực tiếp lên thẻ `<section>`.
  - Khoảng giữa các khối lớn trong một trang: **48px** (`gap-12`) [ADAPTED: prompt gốc là 96px cho landing; app cần gọn hơn].

### 4.2 Viền [ADAPTED]

| Dùng cho | Độ dày |
|---|---|
| Card, nút, input, modal, khối nổi | **3px** `ink` |
| Đường phân vùng lớn (header, sidebar, thanh tab đáy) | **4px** `ink` |
| Đường kẻ **bên trong** bảng/danh sách dày (hàng, ô) | **2px** `ink` |

Lý do dòng cuối: bảng điểm danh/điểm số có nhiều hàng; viền 3–4px cho mọi ô sẽ làm bảng rất nặng và khó đọc. Phần ngoài của bảng vẫn 3px + bóng cứng.

### 4.3 Responsive

- Breakpoint: mobile `375`, tablet `md = 768`, desktop `lg = 1024`, rộng `1440`. Viết CSS mobile-first, mặc định 1 cột, mở cột bằng `md:`/`lg:`.
- **Không** ép lưới nhiều cột trên mobile. Card **không** có chiều cao cố định (dùng `min-h`), để nội dung dài tự giãn.
- Bảng rộng cuộn ngang **bên trong** khung riêng (`overflow-x-auto`), không làm cả trang cuộn ngang. Trên mobile ưu tiên chuyển hàng bảng thành **thẻ xếp dọc** nếu có > 3 cột.
- Phần tử cố định mép màn hình (header, thanh tab đáy) thêm `env(safe-area-inset-*)` để không bị tai thỏ/thanh hệ thống che.
- Không để overflow ngang ở 375px (kiểm tra thủ công).

### 4.4 Khung ứng dụng (AppLayout, đã có từ T0.4)

- **Desktop (≥ 1024px):** sidebar trái cố định, rộng 256px, nền `ink`, chữ `paper`, viền phải 4px; vùng nội dung nền `cream`.
- **Tablet/mobile:** sidebar thành drawer (đã có trong AppLayout), mở bằng nút menu 48×48 ở header.
- **Header:** cao 64px, nền `cream`, viền đáy 4px; trái: tên trang; phải: avatar chữ cái đầu + nút đăng xuất.
- Mục menu đang chọn: nền `yellow`, chữ `ink`, viền 3px, bóng cứng. Mục không chọn: chữ `paper`, hover nền `paper` chữ `ink`.
- Menu theo vai trò (chỉ liệt kê mục đã có trong ROADMAP, **không bịa thêm**):
  - TUTOR: Tổng quan · Lớp học · Lịch · Báo cáo
  - STUDENT / PARENT: Tổng quan · Lịch · Điểm · Học phí · Báo cáo (PARENT có bộ chọn con ở header)
  - ADMIN: dùng khung TUTOR cộng mục quản trị **chỉ khi đã có đặc tả** (hiện chưa có, xem §8.10)
- Trên mobile cho STUDENT/PARENT: thanh tab đáy cố định, tối đa 4–5 mục, mỗi mục cao ≥ 56px, viền trên 4px.

---

## 5. Hiệu ứng tương tác

- **Hover** (chỉ trên thiết bị có hover): bóng `4px → 8px` và `translate(-4px, -4px)`. Dùng `brut-pop`.
- **Active/pressed:** dịch `translate(2px, 2px)`, bóng 2px (cảm giác nhấn xuống).
- **Focus-visible:** outline 3px `ink`, offset 3px (trên nền tối dùng `paper`). **Không bao giờ** `outline: none` mà không thay thế.
- **Disabled:** `opacity: 0.5`, bỏ bóng, `cursor: not-allowed`, kèm `disabled` hoặc `aria-disabled`.
- **Thời gian chuyển:** 120ms, `ease`. Không animation dài, không bounce. Có `prefers-reduced-motion` thì tắt dịch chuyển.
- Card chỉ để **xem** (không bấm được) dùng `brut-box`, không có hover. Card bấm được (vd thẻ lớp học dẫn tới chi tiết) dùng `brut-pop` và là phần tử `<a>`/`<button>` thực sự.
- Mọi phần tử bấm được có `cursor: pointer` và hover state rõ ràng.

---

## 6. Component

Tất cả: góc **vuông** (`border-radius: 0`), viền 3px `ink`, font body trừ khi ghi khác. Đặt trong `frontend/src/components/ui/` (một file mỗi component, props gọn, có type).

### 6.1 Button
| Biến thể | Nền | Dùng khi |
|---|---|---|
| `primary` | `yellow` | Hành động chính duy nhất trong khu vực (Lưu, Tạo lớp) |
| `secondary` | `paper` | Hành động phụ (Hủy, Quay lại) |
| `info` | `blue` | Hành động trung tính nổi bật (Tạo link mời, Xem báo cáo) |
| `danger` | `coral` | Hành động phá hủy/không hoàn tác (Hủy buổi, Void đợt) |
- Cao tối thiểu **48px**, padding `16px 24px`, chữ in hoa đậm; nút CTA chính trên mobile portal 56px. Vùng chạm **≥ 44×44px**.
- Cỡ `sm` (40px) **chỉ** dùng trong bảng dày trên desktop.
- Nút chỉ icon phải có `aria-label` tiếng Việt.
- Trạng thái đang xử lý: giữ nguyên kích thước, đổi nhãn thành `ĐANG LƯU…`, `disabled`.

```tsx
// Ví dụ khung (không bắt buộc đúng từng ký tự, nhưng phải giữ token và quy tắc)
const variants = {
  primary: "bg-yellow", secondary: "bg-paper", info: "bg-blue", danger: "bg-coral",
} as const;

export function Button({ variant = "secondary", className = "", ...props }: ButtonProps) {
  return (
    <button
      className={`brut-pop min-h-12 px-6 py-4 font-heading text-sm font-bold uppercase text-ink ${variants[variant]} ${className}`}
      {...props}
    />
  );
}
```

### 6.2 Form: Input, Select, Textarea, Checkbox, Radio, Toggle
- Nền `paper`, viền 3px, cao ≥ 48px, padding ngang 16px (`px-4`), dọc 12px (`py-3`). Đây là ngoại lệ duy nhất ngoài bội của 8px (để ô cao đúng 48px); mọi chỗ khác theo lưới 8px.
- Label **luôn hiện** phía trên (không dùng placeholder thay label), chữ `--fs-small` in hoa đậm. Trường bắt buộc có `*`.
- Focus: dùng outline focus chuẩn (§5), viền vẫn 3px; không đổi sang màu nền mới.
- Lỗi: viền vẫn `ink`, thêm khối thông báo nền `coral` chữ `ink` ngay dưới ô, kèm tiền tố `LỖI:`; liên kết bằng `aria-describedby` và `aria-invalid`.
- Ô nhập điểm: nhận số từ 0 đến 10, tối đa 2 chữ số thập phân (khớp schema), báo lỗi tại chỗ khi ngoài khoảng.
- Checkbox/radio/toggle tự vẽ bằng ô vuông 24×24 viền 3px; chọn = nền `ink` + dấu `✓` màu `paper`. Toggle là hai ô chữ `CHƯA NỘP | ĐÃ NỘP` (segmented), không phải công tắc tròn.
- Input date/time dùng định dạng `vi-VN` khi hiển thị (xem §10).

### 6.3 Card
- Nền `paper` (hoặc một màu pop-art nếu là khối nhấn), `brut-box`, padding 24px, bên trong `flex flex-col gap-4`.
- Tiêu đề card (`h3`) cách nội dung ≥ 16px. Không nhét quá 1 nhấn màu mỗi card.

### 6.4 Badge / Tag
- Ô chữ nhật nhỏ: viền 2px `ink`, padding `4px 8px`, `--fs-small` in hoa đậm. Màu theo bảng ngữ nghĩa §9.
- Luôn có chữ. Không dùng chấm màu trơ.

### 6.5 Alert / Banner (cảnh báo, lỗi, thông tin)
- Khối ngang, viền 3px, bóng cứng, trái có ký hiệu 40×40 (`!`, `✕`, `i`) trong ô vuông viền 3px.
- `warning` nền `yellow`, `error` nền `coral`, `info` nền `blue`. Chữ luôn `ink`.
- Lỗi từ API hiển thị theo `code` (bảng thông báo ở §10), **không** hiển thị nguyên văn `detail` kỹ thuật.

### 6.6 Table / danh sách dày
- Khung ngoài `brut-box`, hàng tiêu đề nền `ink` chữ `paper` (in hoa nhỏ), đường kẻ hàng 2px, hàng hover nền `yellow`.
- Padding ô 16px dọc và ngang (`p-4`); cột số căn phải, `tabular-nums`.
- Có thể tiêu đề cột cố định (sticky) khi cuộn dọc trong khung.
- < 768px: chuyển thành thẻ xếp dọc (mỗi hàng là một card, nhãn cột in hoa nhỏ phía trên giá trị).

### 6.7 Tabs
- Dãy ô vuông liền nhau, viền 3px; tab đang chọn nền `yellow`, các tab còn lại nền `paper`; hover dùng hiệu ứng `brut-pop`.
- Dùng `role="tablist"` / `role="tab"` / `aria-selected`; phím mũi tên chuyển tab.

### 6.8 Modal / Drawer
- Lớp phủ phía sau modal dùng nền `cream` **đặc** (không dùng độ trong suốt/opacity vì bị cấm). Khối modal là `brut-box` viền 4px.
- Khung modal: tiêu đề `h2` in hoa, nút đóng `✕` 48×48 góc phải, hành động chính ở cuối (`primary` bên phải, `secondary` bên trái). Bẫy focus trong modal, đóng bằng `Esc`, trả focus về nút mở.
- Mobile: modal full-width, sát đáy (bottom sheet) với viền trên 4px.

### 6.9 Toast
- Góc dưới phải (mobile: dưới cùng giữa), `brut-box`, tự tắt sau 5s (lỗi ở lại tới khi đóng). `role="status"` (lỗi: `role="alert"`).

### 6.10 Stat tile (ô số liệu, lấy cảm hứng từ section Stats của landing)
- Dải ô vuông liền nhau, mỗi ô: số rất lớn (`--fs-h1`, 900) + nhãn nhỏ bên dưới. Ô đan xen `paper` / `blue` / `paper` / `yellow` (coral chỉ dùng khi là cảnh báo thật).
- Ngăn cách bằng viền 3px; cả dải có bóng cứng. Mobile: lưới 2×2, giữ viền.

### 6.11 Avatar
- **Ô vuông** 40×40 viền 3px, hiển thị **chữ cái đầu** của tên, nền theo thứ tự xoay vòng `yellow`/`blue`/`coral`. Không dùng ảnh người thật.

### 6.12 Progress theo buổi (đợt học phí)
- Một đợt N buổi hiển thị **N ô vuông** liền nhau (nền `ink` = đã học, `paper` = còn lại), viền 2px. Nếu N > 20 chuyển thành một thanh phẳng 24px cao và ghi `x/N BUỔI`.
- Luôn có chữ `ĐÃ HỌC x / N BUỔI` bên cạnh.

### 6.13 Khối lịch (buổi học)
- Mỗi buổi là một ô: giờ (`07:00–08:30`), tên lớp, trạng thái. Nền theo trạng thái (§9).
- Buổi `CANCELLED`: nền `paper`, chữ gạch ngang, nhãn `ĐÃ HỦY`.

### 6.14 Trạng thái loading / lỗi / rỗng (bắt buộc mọi màn có dữ liệu)
- **Loading:** khung skeleton là các khối `paper` viền 3px (không shimmer mềm); thêm chữ `ĐANG TẢI…`, `aria-busy="true"`.
- **Lỗi:** Alert `error` + nút `THỬ LẠI`.
- **Rỗng:** khung viền **3px nét đứt** (`border-dashed`), tiêu đề in hoa, một câu hướng dẫn và nút hành động chính (vd `TẠO LỚP ĐẦU TIÊN`). Với STUDENT/PARENT (không có quyền tạo) chỉ có câu giải thích, không có nút.

---

## 7. Biểu tượng

- **Không thêm thư viện icon** khi chưa được duyệt (ARCHITECTURE không có thư viện icon). Dùng ký tự Unicode (`✓ ✕ ! → ←`) hoặc SVG inline nét dày 3px, góc vuông (`stroke-linecap="square"`).
- **[PROPOSED]** nếu cần nhiều icon, đề xuất `lucide-react` (nét dày, hợp style) và xin duyệt trước.
- Icon trang trí có `aria-hidden="true"`; icon mang nghĩa có `aria-label`.

---

## 8. Màn hình

Mỗi mục dưới đây ánh xạ với task ROADMAP. **Chỉ dựng UI cho task đang làm**, dùng dữ liệu thật từ API hoặc mock rõ ràng; không dựng sẵn màn chưa tới lượt.

### 8.1 Đăng nhập, đăng ký gia sư, nhận lời mời (T1.6)
- Desktop: hai cột, trái là khối `yellow` viền 4px với `--fs-display` tên sản phẩm `TUTORHUB` và một dòng mô tả; phải là form trong card. Mobile: một cột, khối `yellow` thu gọn ở trên.
- Trang nhận lời mời hiển thị rõ **vai trò được mời** (HỌC SINH/PHỤ HUYNH) và tên lớp (nếu có) trong một Alert `info`.
- Không có tùy chọn "đăng ký" cho học sinh/phụ huynh (chỉ qua link mời). Không có "Quên mật khẩu" trừ khi người dùng đã chốt cơ chế (hiện chưa có email).

### 8.2 Tổng quan gia sư
- Dải Stat tile: số lớp đang dạy · buổi hôm nay · cảnh báo học phí · học sinh đang học.
- Dưới: danh sách "Buổi sắp tới" (card) và "Cần chú ý" (Alert cảnh báo học phí `CYCLE_LOW`/`CYCLE_DONE_UNPAID`/`NO_OPEN_CYCLE`).

### 8.3 Lớp học (T2.3)
- Danh sách lớp: lưới thẻ 1 cột (mobile) / 2 cột (md) / 3 cột (lg). Mỗi thẻ: tên lớp (h3), badge môn, badge loại lớp, số học sinh đang học; bấm được (`brut-pop`).
- Badge loại lớp: `1:1` nền `yellow`, `NHÓM` nền `blue`. Lớp nhóm dưới 2 học sinh có badge `ÍT HỌC SINH` nền `yellow` kèm Alert `warning` ở trang chi tiết (`GROUP_TOO_SMALL`), **không chặn** thao tác.
- Có ô tìm kiếm, lọc `ĐANG DẠY / ĐÃ LƯU TRỮ`, phân trang.
- Chi tiết lớp dùng **Tabs**: Học sinh · Lịch · Điểm danh · Bài tập · Học phí (tab chỉ hiện khi tính năng đã làm).
- Form tạo/sửa lớp: tên, môn, mô tả, **loại lớp** (hai ô chọn lớn `1:1` / `NHÓM`). Đổi loại lớp bị từ chối (422) phải hiển thị thông báo rõ.
- Mời học sinh/phụ huynh: nút `TẠO LINK MỜI` mở modal hiển thị link kèm nút `SAO CHÉP`, hạn dùng, và dòng nhắc "Gửi link này qua Zalo/tin nhắn".

### 8.4 Lịch (T3.6)
- **Desktop (≥ 1024px):** lưới tuần 7 cột, cột giờ bên trái, buổi là khối vuông. **Tablet:** lưới tuần cuộn ngang trong khung riêng. **Mobile:** dạng **agenda** (danh sách theo ngày), không dựng lưới 7 cột.
- Điều hướng: `← TUẦN TRƯỚC | HÔM NAY | TUẦN SAU →` (nút 48px).
- Tạo buổi trùng lịch (409 `SESSION_CONFLICT`): giữ form mở, hiển thị Alert `error` nêu buổi xung đột (giờ + tên lớp), **không** xóa dữ liệu đã nhập.
- Cảnh báo học sinh chồng lịch (không chặn): Alert `warning`, vẫn cho lưu.

### 8.5 Điểm danh (T4.2)
- Mỗi học sinh một hàng: avatar + tên; 4 lựa chọn dạng segmented: `CÓ MẶT` · `MUỘN` · `VẮNG CÓ PHÉP` · `VẮNG KHÔNG PHÉP`. Mặc định `CÓ MẶT`. Mobile: 2×2 trong mỗi hàng.
- Thanh hành động **cố định đáy** (có safe-area): `TẤT CẢ CÓ MẶT` (secondary) và `LƯU ĐIỂM DANH` (primary). Báo "Chưa lưu thay đổi" khi có thay đổi.
- Lớp 1:1: hiển thị gọn một học sinh, không cần phần "tất cả có mặt".
- Điểm danh cả lớp trong **< 5 thao tác** (điều kiện hoàn thành của T4.2).

### 8.6 Bài tập và điểm (T5.3)
- Danh sách bài (card/ bảng): tiêu đề, loại (badge), hạn nộp, tiến độ chấm.
- Bảng nhập điểm: hàng = học sinh, ô nhập số 0–10; lỗi báo tại ô; nút `LƯU ĐIỂM` cố định đáy. Điểm và nhận xét của một học sinh khác **không** được lộ ra ở màn STUDENT.

### 8.7 Học phí (T6.3, T6.4)
- Mỗi học sinh/lớp: danh sách **đợt** (card). Mỗi đợt: `ĐỢT k`, progress theo buổi (§6.12), ghi chú, trạng thái.
- Gia sư: toggle phân đoạn `CHƯA NỘP | ĐÃ NỘP`, nút `SỬA SỐ BUỔI`, `HỦY ĐỢT` (danger, có hộp xác nhận), `MỞ ĐỢT MỚI` (primary; form chỉ có **số buổi N** và ghi chú).
- Học sinh/phụ huynh: chỉ xem badge `ĐÃ NỘP` / `CHƯA NỘP`, không có nút thay đổi.
- Cảnh báo (cho gia sư): `CYCLE_LOW` → badge/Alert `warning` "Đợt hiện tại sắp hết buổi"; `CYCLE_DONE_UNPAID` → `error` "Đã học đủ buổi nhưng chưa nộp"; `NO_OPEN_CYCLE` → `error` "Đã học vượt số buổi, chưa mở đợt mới".
- **Nhắc học phí (T6.4):** nút `TẠO TIN NHẮN NHẮC` mở modal gồm ô văn bản soạn sẵn **chỉnh sửa được** và nút `SAO CHÉP`. Văn bản **không chứa số tiền**; có dòng nhỏ "Hệ thống không tự gửi tin nhắn. Hãy dán vào Zalo." Sau khi sao chép hiện Toast `ĐÃ SAO CHÉP`.
- **Tuyệt đối không** có ô nhập hay chỗ hiển thị tiền.

### 8.8 Báo cáo tiến độ (T7.4)
- Chọn học sinh + lớp + **khoảng ngày** (hai ô ngày, hợp lệ khi đến ngày ≥ từ ngày), nút `XEM TRƯỚC`.
- Xem trước dạng "tờ giấy": khối `paper` viền 4px, tiêu đề, các số liệu trong dải Stat tile (tỉ lệ đi học, điểm trung bình, buổi còn lại, trạng thái học phí), danh sách điểm, ô nhận xét của gia sư.
- Trạng thái báo cáo: `NHÁP` (badge `paper`) / `ĐÃ CÔNG BỐ` (badge `blue`). Gia sư: `CÔNG BỐ` (primary), `TẢI PDF`, `TẢI CSV`.
- Không có dữ liệu trong khoảng ngày → hiển thị "CHƯA CÓ DỮ LIỆU" thay vì số 0 hoặc NaN.
- Học sinh/phụ huynh chỉ thấy báo cáo `ĐÃ CÔNG BỐ`.

### 8.9 Cổng học sinh/phụ huynh (T8.2)
- **Thiết kế cho 375px trước.** Một cột, thẻ lớn, chữ ≥ 16px, nút ≥ 48px.
- Thứ tự: (PARENT) bộ chọn con ở đầu trang → buổi học sắp tới → bài sắp hạn → điểm gần đây → buổi còn lại + trạng thái học phí → báo cáo mới.
- Thanh tab đáy cố định (§4.4). Không có nút chỉnh sửa dữ liệu học tập.

### 8.10 ADMIN
- **Chưa có đặc tả màn hình ADMIN trong tài liệu.** Agent **không được tự thiết kế tính năng quản trị**. Chỉ dùng khung ứng dụng và các component ở trên khi một task cụ thể mô tả màn ADMIN. Nếu gặp, hỏi người dùng.

---

## 9. Ánh xạ trạng thái → giao diện (luôn có chữ)

| Đối tượng | Giá trị | Nhãn (tiếng Việt) | Nền badge | Ký hiệu |
|---|---|---|---|---|
| Điểm danh | `PRESENT` | CÓ MẶT | `paper` | ✓ |
| | `LATE` | MUỘN | `yellow` | ! |
| | `ABSENT_EXCUSED` | VẮNG CÓ PHÉP | `blue` | — |
| | `ABSENT_UNEXCUSED` | VẮNG KHÔNG PHÉP | `coral` | ✕ |
| Buổi học | `SCHEDULED` | ĐÃ LÊN LỊCH | `blue` | |
| | `COMPLETED` | ĐÃ DẠY | `paper` | ✓ |
| | `CANCELLED` | ĐÃ HỦY | `paper` (gạch ngang) | ✕ |
| Loại lớp | `ONE_ON_ONE` | 1:1 | `yellow` | |
| | `GROUP` | NHÓM | `blue` | |
| Trạng thái lớp | `ACTIVE` / `ARCHIVED` | ĐANG DẠY / ĐÃ LƯU TRỮ | `paper` / `paper` (nét đứt) | |
| Học phí | `PAID` | ĐÃ NỘP | `blue` | ✓ |
| | `UNPAID` | CHƯA NỘP | `coral` | ! |
| Cảnh báo học phí | `CYCLE_LOW` | SẮP HẾT BUỔI | `yellow` | ! |
| | `CYCLE_DONE_UNPAID` | HỌC ĐỦ, CHƯA NỘP | `coral` | ! |
| | `NO_OPEN_CYCLE` | CHƯA MỞ ĐỢT MỚI | `coral` | ! |
| Báo cáo | `DRAFT` | NHÁP | `paper` | |
| | `PUBLISHED` | ĐÃ CÔNG BỐ | `blue` | ✓ |
| Bài của học sinh | `ASSIGNED` / `SUBMITTED` / `GRADED` / `MISSING` | ĐÃ GIAO / ĐÃ NỘP BÀI / ĐÃ CHẤM / THIẾU | `paper` / `blue` / `paper` (✓) / `coral` | |

Đừng dùng cách viết `PAID`/`UNPAID` thô cho người dùng cuối; luôn dùng nhãn tiếng Việt ở cột 3.

---

## 10. Nội dung chữ (microcopy)

- Toàn bộ chữ giao diện bằng **tiếng Việt**, xưng "bạn", ngắn, trực tiếp. Nút dùng động từ: `LƯU`, `TẠO LỚP`, `HỦY BUỔI`.
- Ngày/giờ hiển thị theo `vi-VN`, múi giờ `Asia/Ho_Chi_Minh` (backend lưu UTC, **chuyển đổi ở frontend** khi hiển thị): ví dụ `Thứ Hai, 05/10/2026 · 18:00–19:30`.
- Thông báo lỗi theo `code` của API (không hiện chi tiết kỹ thuật):

| `code` / status | Thông báo |
|---|---|
| `SESSION_CONFLICT` (409) | Buổi này trùng giờ với một buổi khác của bạn. Hãy chọn giờ khác. (kèm giờ + lớp xung đột) |
| `ONE_ON_ONE_FULL` (422) | Lớp 1:1 chỉ có tối đa 1 học sinh đang học. |
| `GROUP_TOO_SMALL` (cảnh báo) | Lớp nhóm nên có từ 2 học sinh. Bạn vẫn có thể tiếp tục sử dụng lớp. |
| 400 validation | Hiện lỗi ngay dưới từng ô theo `errors[].field`. |
| 401 | Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại. |
| 403 | Bạn không có quyền thực hiện thao tác này. |
| 404 | Không tìm thấy nội dung này. |
| 5xx / mạng | Có lỗi xảy ra. Vui lòng thử lại sau. |

- Hộp xác nhận cho thao tác khó hoàn tác (hủy buổi, hủy đợt, bỏ học sinh khỏi lớp): nêu rõ hậu quả, nút `danger` ghi động từ cụ thể, không dùng "OK".

---

## 11. Truy cập (accessibility)

- Dùng HTML ngữ nghĩa: `header`, `nav`, `main`, `section`, `button`, `a`, `table`. Không dùng `div` có `onClick` thay cho `button`.
- Mọi ô form có `label` gắn đúng; lỗi gắn bằng `aria-describedby`.
- Vùng chạm ≥ 44×44px. Thứ tự Tab hợp lý; điều hướng bằng bàn phím được toàn bộ (menu, tab, modal, segmented).
- Tương phản: tuân thủ §2.1 (chữ trên coral/blue luôn `ink`).
- Trạng thái chỉ-màu bị cấm: mọi trạng thái có chữ (§9).
- Tôn trọng `prefers-reduced-motion`.
- Ngôn ngữ trang: `<html lang="vi">`.

---

## 12. Nên và không nên

**Nên**
- Dùng `brut-pop` cho thứ bấm được, `brut-box` cho khối tĩnh.
- Dùng đúng 6 màu token; dùng `yellow` cho một hành động chính mỗi vùng.
- Để nội dung tự giãn chiều cao; test ở 375px.
- Viết chữ tiếng Việt thật, có dấu, đúng ngữ cảnh gia sư/lớp học.

**Không**
- Không `rounded-*`, bóng mềm, blur, gradient mềm, glassmorphism, opacity để làm mờ.
- Không thêm màu ngoài bảng; không hard-code hex ngoài `@theme`.
- Không dùng chữ trắng trên `coral`/`blue`.
- Không copy cấu trúc landing page (hero, marquee, pricing, testimonial, footer pill, CTA banner) vào app.
- Không ảnh người thật, không dữ liệu học sinh thật.
- Không đổi logic/API/route khi đang làm UI. Không thêm dependency khi chưa hỏi.

---

## 13. Kế hoạch cho T0.4.1

**Phạm vi:** chỉ nền tảng giao diện, chưa dựng màn nghiệp vụ.

1. Cập nhật `frontend/index.html`: `lang="vi"`, link Google Fonts (§3.1).
2. Cập nhật `frontend/src/index.css`: `@theme`, `@layer base`, `@utility brut-pop`, `brut-box` (§2.2).
3. Tạo `src/components/ui/`: `Button`, `Card`, `Badge`, `Alert`, `Input` (kèm label + lỗi), `Tabs`, `StatTile`, `EmptyState`. Chưa cần `Modal`/`Table`/`Toast` nếu chưa có màn dùng tới (làm ở task cần).
4. Áp dụng vào `AppLayout` hiện có theo §4.4 (sidebar, header, drawer mobile), **giữ nguyên route và logic**.
5. Dựng một trang kiểm tra **tạm** `/dev/ui` (chỉ ở môi trường dev, không đưa vào menu) liệt kê các component và trạng thái hover/disabled/lỗi để kiểm tra bằng mắt; xóa hoặc chặn ở production.
6. Ghi vào `DECISIONS.md` hai quyết định đề xuất (xem cuối file) và ghi kiến thức mới vào `WHAT_I_LEARNED.md`.

**Điều kiện hoàn thành (DoD):**
- [ ] `npm run lint` và `npm run build` xanh, không warning mới (nếu có test thì `npm test` xanh).
- [ ] `grep -R "rounded\|shadow-\(sm\|md\|lg\|xl\|2xl\)" frontend/src` không còn kết quả ngoài `shadow-hard*`.
- [ ] Không còn hex màu nằm ngoài `@theme` trong `src/`.
- [ ] Chuỗi `Ệ ẳ ữ ặ Đ ơ ư` hiển thị đúng ở heading và body.
- [ ] Ở 375px không cuộn ngang; drawer menu hoạt động; mọi nút ≥ 44px.
- [ ] Phím Tab thấy rõ focus trên mọi phần tử tương tác.
- [ ] Không file ngoài phạm vi UI bị sửa; không dependency mới.
- [ ] Báo cáo theo mẫu VERIFY trong `Workflow.md` (STATUS / IMPLEMENTED / TESTED / ISSUES / FILES CHANGED).

**Áp dụng cho các task sau:** mỗi task frontend (T1.6, T2.3, T3.6, T4.2, T5.3, T6.3/6.4, T7.4, T8.2) phải đọc file này, dùng lại component từ `components/ui/`, và thêm component mới vào đó thay vì viết style rời trong feature.

---

## 14. Tự kiểm tra trước khi báo xong (agent)

1. Có dùng đúng token, không magic number, không màu ngoài bảng?
2. Mọi thứ bấm được có hover, focus-visible, disabled, cursor đúng?
3. Mọi màn có loading / lỗi / rỗng?
4. Có chỗ nào chỉ dùng màu để truyền thông tin?
5. Có hiển thị hoặc nhập tiền ở đâu không? (phải là **không**)
6. Có thêm tính năng/route/dependency ngoài task không? (phải là **không**)
7. Ở 375px còn cuộn ngang hoặc chữ bị cắt dấu tiếng Việt không?
8. Chữ trên nền `coral`/`blue` có phải `ink` không?

---

## Phụ lục: quyết định đề xuất ghi vào `DECISIONS.md`

| ID | Trạng thái | Quyết định | Lý do |
|---|---|---|---|
| D-29 | CONFIRMED | Phong cách UI Neubrutalism: nền kem, 3 màu nhấn vàng/coral/xanh + đen/trắng, viền đen dày, bóng cứng không blur, góc vuông, Space Grotesk cho heading | Người dùng chọn |
| D-30 | PROPOSED | Font body dùng **Space Mono** thay **DM Mono** của prompt gốc; có thể đổi sang Be Vietnam Pro nếu đọc đoạn dài mỏi mắt | Đảm bảo hiển thị đúng dấu tiếng Việt |
| D-31 | PROPOSED | Viền 2px cho đường kẻ bên trong bảng/danh sách dày; khối nổi giữ 3px, vùng lớn 4px | Giữ tính dễ đọc của bảng điểm danh/điểm số |
