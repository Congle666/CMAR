# BÁO CÁO TỔNG HỢP CMAR

**Cách thuật toán CMAR chạy — Tổng hợp đầy đủ, ít công thức, nhiều giải thích**

> File này tổng hợp toàn bộ nội dung đã thảo luận: cách thuật toán vận hành từng bước, cơ chế cắt tỉa, giải thích rõ các khái niệm then chốt như `defaultClass`, `minSup`, `classCount`, v.v.
>
> **Đối tượng đọc:** Người muốn hiểu CMAR "làm gì" và "chạy như thế nào" — không cần đi sâu chứng minh công thức.

---

## Mục Lục

1. [CMAR Để Làm Gì?](#1-cmar-để-làm-gì)
2. [Các Khái Niệm Cần Biết Trước](#2-các-khái-niệm-cần-biết-trước)
3. [Các Tham Số Đầu Vào](#3-các-tham-số-đầu-vào)
4. [Cách Thuật Toán Chạy — Toàn Cảnh](#4-cách-thuật-toán-chạy--toàn-cảnh)
5. [Bước 1: Đọc Dữ Liệu & Đếm Tần Số](#5-bước-1-đọc-dữ-liệu--đếm-tần-số)
6. [Bước 2: Xây Cây Nén Dữ Liệu (CR-tree)](#6-bước-2-xây-cây-nén-dữ-liệu-cr-tree)
7. [Bước 3: Đào Mỏ & Sinh Luật Ứng Viên](#7-bước-3-đào-mỏ--sinh-luật-ứng-viên)
8. [Bước 4: Cắt Tỉa 3 Tầng](#8-bước-4-cắt-tỉa-3-tầng)
9. [Bước 5: Lưu Luật Cuối Cùng vào CR-tree](#9-bước-5-lưu-luật-cuối-cùng-vào-cr-tree)
10. [Bước 6: Phân Lớp Bản Ghi Mới](#10-bước-6-phân-lớp-bản-ghi-mới)
11. [Minh Hoạ Thực Tế: Mushroom](#11-minh-hoạ-thực-tế-mushroom)
12. [Bảng Tra Cứu Thuật Ngữ Nhanh](#12-bảng-tra-cứu-thuật-ngữ-nhanh)
13. [Lời Khuyên Tinh Chỉnh Tham Số](#13-lời-khuyên-tinh-chỉnh-tham-số)
14. [Tổng Kết](#14-tổng-kết)

---

## 1. CMAR Để Làm Gì?

**CMAR** (Classification based on Multiple Association Rules) là một thuật toán **phân lớp** (classification) — tức là:
- Cho một tập bản ghi đã biết nhãn lớp (training set).
- Học ra một "mô hình" để khi có bản ghi mới, **dự đoán** nhãn lớp của nó.

**Điểm đặc biệt của CMAR:** Thay vì xây cây quyết định hay mạng nơ-ron, CMAR dùng **luật "nếu ... thì ..."** (ví dụ: "nếu có mùi foul → nấm độc").

**Ví dụ ứng dụng:**
- Y tế: "Triệu chứng → bệnh?"
- Ngân hàng: "Hồ sơ khách → duyệt thẻ tín dụng?"
- Sinh học: "Đặc điểm sinh vật → phân loài?"

**Ưu điểm nổi bật của CMAR:**
1. Dự đoán dựa trên **nhiều luật** cùng lúc, không phải 1 luật → ít sai lệch.
2. Luật tường minh → con người có thể **hiểu được** tại sao mô hình dự đoán như vậy.
3. Xử lý tốt khi dữ liệu có mối quan hệ phức tạp giữa nhiều thuộc tính.

---

## 2. Các Khái Niệm Cần Biết Trước

### 2.1 Bản ghi (Transaction / Record)

Một **bản ghi** là một hàng dữ liệu — gồm các **thuộc tính** (attributes) và một **nhãn lớp** (class).

Ví dụ bản ghi thời tiết:

| Outlook | Temperature | Humidity | Windy | Play |
|---------|-------------|----------|-------|:----:|
| sunny | hot | high | false | **no** |

4 cột đầu là thuộc tính. Cột `Play` là nhãn lớp.

### 2.2 Item

Mỗi ô trong bản ghi được viết lại thành **item** (cặp `thuộc_tính=giá_trị`):
- `outlook=sunny`
- `temperature=hot`
- `humidity=high`
- `windy=false`
- (class `play=no` — giữ riêng, không đưa vào items)

### 2.3 Pattern (Mẫu)

**Pattern** là một **tập hợp con** các item. Ví dụ:
- Pattern 1 item: `{outlook=sunny}`
- Pattern 2 items: `{outlook=sunny, humidity=high}`
- Pattern 3 items: `{outlook=sunny, humidity=high, windy=false}`

### 2.4 Luật kết hợp lớp (CAR)

Một **luật** có dạng:

> **Nếu bản ghi khớp pattern P → dự đoán lớp c**

Viết tắt: `P ⇒ c`

Ví dụ:
- `{outlook=sunny, humidity=high} ⇒ play=no`
- `{odor=foul} ⇒ poisonous`

**Luật có 2 thành phần:**
- **Vế trái (condset):** pattern P — điều kiện cần khớp.
- **Vế phải (class):** nhãn lớp c — kết luận.

### 2.5 Hai độ đo cơ bản của luật

Mỗi luật gắn với 2 con số:

**Support (sup):** "Có bao nhiêu bản ghi ủng hộ luật này?"
- Ví dụ: sup = 100 nghĩa là 100 bản ghi khớp cả P và có class c.
- Cao = luật phổ biến, đáng tin.

**Confidence (conf):** "Khi khớp vế trái, bao nhiêu % cũng khớp vế phải?"
- Ví dụ: conf = 95% nghĩa là trong 100 bản ghi khớp P, có 95 bản có class c.
- Cao = luật đúng, tin cậy.

**Ví dụ trực quan:**
> Luật `{odor=foul} ⇒ poisonous` có sup = 2160, conf = 100%.
>
> Nghĩa: "Có 2160 nấm mùi foul trong dataset, **tất cả** đều poisonous."

### 2.6 `defaultClass` — Khái niệm quan trọng

**`defaultClass`** là **nhãn lớp phổ biến nhất trong training set**.

**Cách xác định:** Đếm số lượng mỗi class → chọn class có count lớn nhất.

**Ví dụ:**
- Dataset Mushroom: 4208 edible, 3916 poisonous → **defaultClass = edible**.
- Dataset Zoo: mammal 41, bird 20, ... → **defaultClass = mammal**.

**`defaultClass` dùng khi nào?**

Khi phân lớp một bản ghi mới, có thể xảy ra trường hợp **không có luật nào khớp** (test record quá "lạ", không luật nào trong mô hình áp dụng được).

Thay vì không trả gì hoặc trả ngẫu nhiên, CMAR trả về `defaultClass`.

**Tại sao chiến lược này hợp lý?**
> Ẩn dụ: Khi đoán đội bóng thắng mà không biết thông tin gì → đoán đội "mạnh hơn xưa nay" là lựa chọn ít rủi ro nhất.

Tương tự: khi không biết gì → đoán class phổ biến nhất → đúng ít nhất với tỉ lệ = tần suất majority class.

**Ví dụ:**
- Dataset có 70% class A và 30% class B.
- Nếu đoán toàn A (defaultClass) → đúng 70% cho những record không có luật khớp.
- Tốt hơn đoán ngẫu nhiên (chỉ đúng 50%).

---

## 3. Các Tham Số Đầu Vào

CMAR cần bạn chỉ định 4 tham số chính trước khi chạy:

### 3.1 `minSup` — Ngưỡng Support Tối Thiểu

**Nghĩa:** Luật phải được **ít nhất `minSup` bản ghi ủng hộ** mới được giữ.

**Có 2 cách chỉ định:**
- **Tuyệt đối:** minSup = 50 (số bản ghi).
- **Tương đối:** minSup = 1% (tỉ lệ so với |T|).

**Ví dụ:** Dataset 1000 records, minSup = 1% → luật phải có ≥ 10 bản ghi ủng hộ.

**Trade-off:**
- `minSup` **thấp** → nhiều luật, bao gồm cả luật hiếm quý. Nhưng bùng nổ tổ hợp → chậm + tốn RAM.
- `minSup` **cao** → ít luật, chạy nhanh. Nhưng mất luật cho minority class.

**Thường dùng:** 1–5% cho dataset vừa, 10%+ cho dataset lớn.

### 3.2 `minConf` — Ngưỡng Confidence Tối Thiểu

**Nghĩa:** Luật phải đúng **ít nhất `minConf`%** mới được giữ.

**Giá trị thường dùng:** 50% (mặc định), 70% (chặt), 90% (rất chặt).

**Trade-off:**
- `minConf` thấp → nhiều luật yếu, dễ sai.
- `minConf` cao → ít luật nhưng chắc chắn; có thể không đủ luật cho một số class.

### 3.3 `χ²_threshold` — Ngưỡng Kiểm Định Thống Kê

**Nghĩa:** Luật phải có mối liên hệ đủ "thật" (không phải trùng hợp).

**Giá trị mặc định:** 3.841 (tương ứng 95% độ tin cậy thống kê).

**Điều chỉnh:**
- Giảm xuống 2.706 (90% tin cậy) → giữ thêm luật có ý nghĩa yếu hơn.
- Tăng lên 6.635 (99% tin cậy) → chỉ giữ luật cực mạnh.

### 3.4 `δ` (delta) — Ngưỡng Database Coverage

**Nghĩa:** Mỗi bản ghi training được "cover" tối đa `δ` lần trước khi loại khỏi quá trình pruning.

**Giá trị mặc định:** 4.

**Trade-off:**
- `δ = 1` (như CBA) → giữ ít luật.
- `δ = 4` (CMAR) → giữ nhiều luật hơn → phân lớp tốt hơn nhờ có "phiếu dự phòng".
- `δ = 10+` → rất nhiều luật, nhưng có luật yếu.

### 3.5 Tóm tắt tham số

| Tham số | Mặc định | Tác dụng chính |
|---------|:--------:|---------------|
| `minSup` | 1% | Ngưỡng phổ biến của luật |
| `minConf` | 50% | Ngưỡng tin cậy của luật |
| `χ²_threshold` | 3.841 | Ngưỡng ý nghĩa thống kê |
| `δ` | 4 | Số luật "dự phòng" cho mỗi record |

---

## 4. Cách Thuật Toán Chạy — Toàn Cảnh

**CMAR chạy qua 2 giai đoạn:**

```
┌───────────────────────────────────────────────────────────────┐
│  GIAI ĐOẠN I — HUẤN LUYỆN (TRAIN)   [chạy 1 lần]              │
│                                                               │
│  Input : Tập huấn luyện T + các tham số                       │
│  Output: Mô hình CMAR (CR-tree luật + defaultClass)           │
│                                                               │
│  Gồm 5 bước:                                                  │
│  ① Đọc dữ liệu & đếm tần số (+ xác định defaultClass)         │
│  ② Xây CR-tree khởi tạo (nén toàn bộ data)                    │
│  ③ Đào mỏ + sinh luật ứng viên (FP-Growth)                   │
│  ④ Cắt tỉa 3 tầng                                             │
│  ⑤ Lưu luật cuối vào CR-tree                                  │
└───────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────┐
│  GIAI ĐOẠN II — PHÂN LỚP (PREDICT)   [chạy mỗi lần dự đoán]   │
│                                                               │
│  Input : Bản ghi mới t                                        │
│  Output: Lớp dự đoán ĉ                                         │
│                                                               │
│  Gồm 1 bước chính:                                            │
│  ⑥ Truy vấn CR-tree + weighted χ² → chọn class                │
└───────────────────────────────────────────────────────────────┘
```

**Đặc điểm:**
- Giai đoạn I **tốn thời gian** nhất, nhưng chỉ chạy 1 lần.
- Giai đoạn II **rất nhanh** — chỉ vài mili-giây cho mỗi record.

---

## 5. Bước 1: Đọc Dữ Liệu & Đếm Tần Số

**Việc làm:**

1. Đọc file CSV → chuyển mỗi dòng thành một bản ghi dạng `(items, class)`.
2. **Đếm** mỗi item xuất hiện bao nhiêu lần.
3. **Đếm** mỗi class có bao nhiêu bản ghi.
4. **Xác định `defaultClass`:** class có count cao nhất.
5. **Lọc item hiếm:** loại item có count < minSup (vì chắc chắn không tạo được luật mạnh).
6. **Sắp các item còn lại** theo tần số giảm dần → gọi là **F-list**.

**Ví dụ trên Mushroom:**

Đếm item:
```
odor=n     : 3528 lần
odor=f     : 2160 lần
bruises=f  : 3936 lần
odor=a     :  400 lần
...
odor=m     :   36 lần  ← dưới minSup → LOẠI
```

Đếm class:
```
edible     : 4208
poisonous  : 3916
```

→ **defaultClass = edible** (4208 > 3916).

**F-list** (sắp giảm dần, giả định minSup = 1097):
```
[veil-color=w (6364), gill-attachment=f (6343), bruises=f (3936),
 odor=n (3528), ..., odor=f (2160), ...]
```

**Tại sao phải sắp?** Để ở bước 2, các item phổ biến nằm gần gốc cây → nhiều bản ghi chia sẻ được tiền tố → cây nén tốt hơn.

---

## 6. Bước 2: Xây Cây Nén Dữ Liệu (CR-tree)

**Mục đích:** Thay vì lưu thẳng toàn bộ training set (tốn bộ nhớ, truy vấn chậm), CMAR **nén** toàn bộ thành **một cây** — gọi là **CR-tree**.

### 6.1 Ý tưởng cây nén

Nhiều bản ghi có thể có **chung phần đầu**. Thay vì lưu lại cả phần đầu đó nhiều lần, ta **chia sẻ** bằng cách xây prefix tree (cây tiền tố):

**Ví dụ 3 bản ghi (đã sắp theo F-list):**
```
Bản ghi 1: [a, b, c]  → class X
Bản ghi 2: [a, b, d]  → class X
Bản ghi 3: [a, c]     → class Y
```

Nén thành cây:
```
root
 └── a:3 (X:2, Y:1)        ← cả 3 đi qua
      ├── b:2 (X:2)         ← 2 đi qua (bản ghi 1, 2)
      │    ├── c:1 (X:1)
      │    └── d:1 (X:1)
      └── c:1 (Y:1)          ← 1 đi qua (bản ghi 3)
```

Tổng: 6 node để lưu 3 bản ghi có 8 item.

### 6.2 Mỗi node của CR-tree lưu gì?

Mỗi node của CR-tree là một cấu trúc dữ liệu lưu:

| Trường | Ý nghĩa |
|--------|---------|
| **item** | Tên item (ví dụ `odor=f`) |
| **count** | Bao nhiêu bản ghi "đi qua" node này |
| **classCount** | **Phân bố class** của các bản ghi đi qua — dạng dict `{class: số lượng}` |
| **parent / children** | Liên kết cha-con trong cây |

**`classCount` là trường quan trọng nhất!** Chính nó biến FP-tree thông thường thành CR-tree đặc biệt cho classification.

**Ví dụ:** Node `odor=f` có:
- count = 2160 (2160 nấm có mùi foul)
- classCount = {poisonous: 2160, edible: 0}

Nhờ classCount, ta biết **ngay lập tức**: tất cả 2160 nấm mùi foul đều là poisonous → luật `{odor=f} ⇒ poisonous` có sup = 2160, conf = 100%.

### 6.3 Cách chèn một bản ghi vào cây

Với mỗi bản ghi training:
1. Trích các items có trong F-list (loại item hiếm).
2. Sắp theo F-list.
3. "Đi" từ root, đi theo path đã sắp:
   - Nếu node con với item đó đã tồn tại → đi xuống, tăng count.
   - Nếu chưa → tạo node mới, đi xuống.
   - Tại mỗi node, cập nhật `classCount[class(bản ghi)] += 1`.

Lặp cho mọi bản ghi → cây hoàn chỉnh.

### 6.4 "Header Table" — Chỉ mục của cây

Vì cùng một item có thể xuất hiện ở nhiều node (ở các nhánh khác nhau), cần có **Header Table** làm chỉ mục:

```
Header Table:
  odor=f → [node odor=f (1), node odor=f (2), node odor=f (3), ...]
  cap=x  → [node cap=x (1), node cap=x (2), ...]
  ...
```

Khi cần xử lý tất cả node của item `odor=f`, chỉ cần đi theo chain từ Header Table, không phải duyệt cả cây.

---

## 7. Bước 3: Đào Mỏ & Sinh Luật Ứng Viên

Đây là bước **phát hiện luật** từ cây.

### 7.1 Ý tưởng

CMAR duyệt từng item (từ hiếm đến phổ biến), với mỗi item:
1. Tìm tất cả "ngữ cảnh" mà item đó xuất hiện.
2. Cho từng ngữ cảnh, kiểm tra xem nó có dẫn tới một class nào đó với độ tin cậy đủ cao không.
3. Nếu có → sinh luật `ngữ cảnh ⇒ class`.

### 7.2 "Sinh luật trực tiếp" — Điểm đặc biệt CMAR

**Cách truyền thống (CBA, Apriori):**
- Pass 1: Tìm frequent patterns (chỉ items).
- Pass 2: Với mỗi pattern, quét lại dữ liệu để đếm class → tạo luật.

**Cách CMAR:**
- Chỉ 1 pass: khi tìm pattern, **luôn biết** phân bố class nhờ `classCount` đã lưu sẵn tại node → sinh luật ngay.

**Ví dụ:**

CMAR tìm được pattern `P = {odor=f}` với sup(P) = 2160.

Thu phân bố class của P (cộng classCount trên header chain):
- poisonous: 2160
- edible: 0

Sinh luật:
- `{odor=f} ⇒ poisonous`: sup = 2160, conf = 2160/2160 = 100%. ✓ Giữ (nếu vượt minSup, minConf).
- `{odor=f} ⇒ edible`: sup = 0. ✗ Loại.

### 7.3 Điều kiện sinh luật

Với mỗi pattern P và mỗi class c, sinh luật `P ⇒ c` nếu:
- sup của luật ≥ `minSup`
- conf của luật ≥ `minConf`

### 7.4 Kết quả bước 3

Đầu ra là **tập luật ứng viên** — có thể từ vài trăm đến vài chục nghìn luật.

Trên Mushroom (minSup=15%), con số này thường là **vài nghìn luật ứng viên**.

---

## 8. Bước 4: Cắt Tỉa 3 Tầng

**Vấn đề:** Tập ứng viên ở Bước 3 có **rất nhiều luật dư thừa** hoặc **yếu**. Cần lọc lại.

**Giải pháp CMAR:** Dùng **3 tầng cắt tỉa**, mỗi tầng loại một loại luật "xấu" khác nhau:

```
Luật ứng viên (~2000)
        │
        ▼
┌──────────────────────────┐
│  TẦNG 1: General Rule    │  Loại luật bị luật đơn giản lấn át
│  → còn ~300 luật         │
└──────────────────────────┘
        │
        ▼
┌──────────────────────────┐
│  TẦNG 2: Chi-square      │  Loại luật không có ý nghĩa thống kê
│  → còn ~200 luật         │
└──────────────────────────┘
        │
        ▼
┌──────────────────────────┐
│  TẦNG 3: Database Cover  │  Loại luật không còn đóng góp
│  → còn ~50-100 luật      │
└──────────────────────────┘
        │
        ▼
   Luật cuối cùng
```

### 8.1 Thứ tự ưu tiên luật (Rule Precedence)

Trước khi cắt tỉa, CMAR **sắp toàn bộ luật** theo mức độ ưu tiên:
1. **Confidence cao trước** (luật chắc chắn quan trọng nhất).
2. **Support cao trước** (khi cùng conf).
3. **Condset ngắn trước** (khi cùng conf và sup) — luật đơn giản hơn ưu tiên.

Sắp xong, các tầng pruning sẽ duyệt luật theo thứ tự này.

### 8.2 Tầng 1 — Loại Luật Dư Thừa (General Rule Pruning)

**Ý tưởng:** Nếu có 2 luật cùng class, mà luật A **đơn giản hơn** luật B (điều kiện ít hơn) và A **mạnh hơn hoặc bằng** B → **luật B dư thừa**, loại đi.

**Ẩn dụ:** Ví như có 2 quy tắc:
- A: "Có mùi foul → là nấm độc" (conf 100%)
- B: "Có mùi foul + màu trắng → là nấm độc" (conf 100%)

B thừa — A đã đủ kết luận, thêm "màu trắng" không giúp gì.

**Cách làm:**
- Duyệt luật từ "mạnh nhất" xuống.
- Với mỗi luật R:
  - Xem đã có luật R' nào trong danh sách giữ lại (cùng class) mà điều kiện R' **là tập con** điều kiện R không.
  - Nếu có → R **dư thừa** → loại.
  - Nếu không → thêm R vào danh sách giữ lại.

**Kết quả:** Thường loại 70–95% luật ứng viên.

**Ví dụ:**

Giả sử 3 luật cùng class X:

| # | Luật | Conf | Giữ hay loại? |
|:-:|------|:----:|:-------------:|
| 1 | `{a} ⇒ X` | 0.95 | **Giữ** |
| 2 | `{a, b} ⇒ X` | 0.90 | **Loại** (bị #1 lấn át) |
| 3 | `{a, b, c} ⇒ X` | 0.85 | **Loại** (bị #1 lấn át) |

Kết quả: Chỉ giữ luật #1.

### 8.3 Tầng 2 — Loại Luật Không Có Ý Nghĩa Thống Kê (χ²)

**Ý tưởng:** Kiểm tra từng luật còn lại xem mối liên hệ giữa vế trái và vế phải có phải **thật sự** không, hay chỉ là ngẫu nhiên.

**Tiêu chí:** Một luật được giữ nếu thoả **CẢ HAI**:
1. **Có ý nghĩa thống kê:** giá trị χ² đủ lớn (≥ ngưỡng, mặc định 3.841 tương ứng độ tin cậy 95%).
2. **Tương quan DƯƠNG:** vế trái làm **tăng** khả năng vế phải, không phải ngược lại.

**Ẩn dụ:** Tung đồng xu 10 lần được 7 mặt ngửa — có vẻ bị bias, nhưng 10 lần quá ít để kết luận. Cần tung 10,000 lần mới biết chắc.

Tương tự: luật conf 70% với sup = 5 nghe có vẻ OK, nhưng 5 bản ghi quá ít để khẳng định. Tầng 2 loại những luật "nghe có vẻ đúng nhưng dữ liệu không đủ để chứng minh".

**Ví dụ cảnh báo — "Paradox trà-cà phê":**

> Dataset 1000 khách: 900 mua cà phê, 200 mua trà, 150 mua cả hai.
>
> Luật: `trà ⇒ cà phê` có conf = 150/200 = 75%.
>
> Nghe ổn? **Không!** Xác suất nền của cà phê là 90%. Biết khách mua trà → xác suất cà phê còn 75% → **GIẢM đi** → luật tương quan **âm**.

Tầng 2 phát hiện và loại loại luật này.

**Kết quả:** Thường loại 50–80% luật còn lại sau Tầng 1.

### 8.4 Tầng 3 — Database Coverage

**Ý tưởng:** Sau Tầng 2 vẫn còn nhiều luật. Một số luật "trùng chức năng" — cùng phân lớp đúng một nhóm bản ghi.

**Câu hỏi:** Ta có cần **tất cả** không? Hay chỉ cần một số đủ để **cover** toàn bộ training set?

**Cách làm (Thuật toán 1 bài báo):**

1. Chuẩn bị:
   - Danh sách luật sắp theo precedence giảm dần.
   - Cho mỗi bản ghi training, một biến đếm `cover_count = 0`.
2. Duyệt luật từ tốt nhất:
   - Tìm mọi bản ghi training mà luật phân lớp **đúng** (bản ghi khớp vế trái **và** có class khớp vế phải).
   - Nếu có ít nhất 1 bản ghi như vậy → **chọn luật này**.
   - Với các bản ghi được cover: `cover_count += 1`.
   - Bản ghi nào đạt `cover_count ≥ δ` → **loại khỏi** T (đã đủ luật cover).
3. Dừng khi T rỗng hoặc hết luật.

**Tham số `δ` = 4 nghĩa là:** mỗi bản ghi training được "cover" tối đa 4 lần trước khi loại.

**Vì sao cần `δ > 1`?**
- CBA dùng `δ = 1` → giữ ít luật.
- CMAR dùng `δ = 4` → giữ nhiều luật hơn → khi classify record mới, có nhiều luật "dự phòng" để "hội ý" → chính xác hơn.

**Kết quả:** Thường loại 30–70% luật còn lại sau Tầng 2.

### 8.5 Tóm tắt 3 tầng

| Tầng | Tên | Loại luật nào? | % loại |
|:----:|-----|---------------|:------:|
| 1 | General Rule | Luật chi tiết dư thừa (bị luật đơn giản mạnh hơn lấn át) | 70–95% |
| 2 | Chi-square | Luật không đủ ý nghĩa thống kê / tương quan âm | 50–80% |
| 3 | Database Coverage | Luật không còn đóng góp vì data đã đủ cover | 30–70% |
| **Tổng** | | | **~99%** |

Từ vài nghìn luật ứng viên → vài chục/trăm luật tinh hoa.

---

## 9. Bước 5: Lưu Luật Cuối Cùng vào CR-tree

Sau Bước 4, ta có **tập luật tinh hoa** (vài chục đến vài trăm luật). CMAR lưu chúng vào một **CR-tree thứ hai** (CR-tree lưu trữ) để truy vấn nhanh khi phân lớp.

### 9.1 Cách lưu trữ

Mỗi luật gồm condset (vế trái) — được lưu như một **path trong cây**:
- Sắp các item trong condset theo tần số giảm dần.
- Đi từ root, đi theo path đó. Nếu chưa có node → tạo mới. Nếu có → đi xuống.
- Tại node cuối path → ghi luật vào.

### 9.2 Tại sao lại lưu thành cây thay vì danh sách?

**Lý do 1 — Tiết kiệm bộ nhớ:**
Nhiều luật có condset chung. Ví dụ:
- R1: `{a, b, c} ⇒ X`
- R2: `{a, b, d} ⇒ Y`

Cả hai chia sẻ `{a, b}` → lưu `{a, b}` **một lần** rồi rẽ nhánh.

**Lý do 2 — Truy vấn nhanh:**
Khi cần tìm "tất cả luật khớp với bản ghi test", dùng DFS **cắt tỉa**:
- Đi xuống cây, nhưng chỉ vào nhánh nào có item nằm trong bản ghi test.
- Nếu item không có trong test → bỏ qua toàn bộ nhánh đó.

Nhanh hơn nhiều so với duyệt toàn bộ danh sách luật.

**Kết quả:** Sau Bước 5, ta có **mô hình CMAR** gồm:
1. **CR-tree lưu trữ** (chứa các luật tinh hoa).
2. **`defaultClass`** (đã xác định ở Bước 1).

Mô hình này được lưu lại để dùng khi phân lớp.

---

## 10. Bước 6: Phân Lớp Bản Ghi Mới

Khi có bản ghi mới `t` cần dự đoán class, CMAR thực hiện 4 bước con:

### 10.1 Bước con 1 — Tìm các luật khớp với t

Dùng CR-tree lưu trữ, duyệt DFS cắt tỉa → thu **tập luật khớp** `R(t)` (tất cả luật có điều kiện ⊆ items của t).

### 10.2 Bước con 2 — Nếu không có luật nào khớp

**Trả về `defaultClass`.**

Đây chính là vai trò của `defaultClass`: phòng trường hợp test record quá "lạ", không luật nào áp dụng được.

### 10.3 Bước con 3 — Nếu tất cả luật khớp đồng nhất 1 class

**Trả về class đó.** Dễ.

Ví dụ: 5 luật khớp, đều dự đoán `edible` → trả về `edible`.

### 10.4 Bước con 4 — Nếu các luật khớp có class khác nhau

Đây là trường hợp khó — luật đang **mâu thuẫn**.

**Giải pháp: Weighted χ² score.**

**Ý tưởng:**
- Nhóm các luật theo class.
- Mỗi nhóm tính một **điểm tổng hợp** (score) — đo "sức mạnh" của nhóm.
- Chọn class có score **cao nhất**.

**Cách tính score:**

Mỗi luật R đóng góp vào score của nhóm một giá trị:

$$
\text{đóng góp của R} = \frac{\chi^2(R)^2}{\max\chi^2(R)}
$$

- Tử số bình phương χ² → nhấn mạnh luật mạnh (chia khoảng cách giữa luật mạnh và yếu).
- Mẫu số chia cho max χ² → **chuẩn hoá** luật về cùng thang đo [0, 1] → công bằng giữa luật sup lớn và sup nhỏ.

**Score của nhóm** = tổng đóng góp của mọi luật trong nhóm.

**Chọn class:** nhóm có score cao nhất.

### 10.5 Ví dụ minh hoạ

Bản ghi test khớp 4 luật:

| # | Luật | class | χ² | max χ² | Đóng góp = χ²²/maxχ² |
|:-:|------|:-----:|:---:|:------:|:---------------------:|
| R1 | … | A | 10 | 25 | 100/25 = 4.0 |
| R2 | … | A | 8 | 20 | 64/20 = 3.2 |
| R3 | … | B | 15 | 18 | 225/18 ≈ 12.5 |
| R4 | … | B | 6 | 12 | 36/12 = 3.0 |

**Nhóm theo class:**
- Nhóm A: R1, R2 → score = 4.0 + 3.2 = **7.2**
- Nhóm B: R3, R4 → score = 12.5 + 3.0 = **15.5**

**Kết quả:** Dự đoán class **B** (score cao hơn).

**Nhận xét:**
- Nếu đếm phiếu thô: 2 luật A vs 2 luật B → hoà.
- Weighted score: B thắng nhờ R3 rất mạnh (χ²² / maxχ² = 12.5).
- Đây là điểm mạnh của CMAR: **không đếm phiếu, mà đo sức mạnh**.

### 10.6 Các trường hợp đặc biệt

**Trường hợp 1 — Test record chứa item chưa từng thấy:**
- Item lạ đơn giản không khớp luật nào → không ảnh hưởng.
- Các luật không đề cập đến item đó vẫn xét bình thường.

**Trường hợp 2 — 0 luật khớp:**
- Trả `defaultClass`.

**Trường hợp 3 — Hoà (2 class có score bằng nhau):**
- Chiến lược thường dùng: chọn class có nhiều luật khớp, hoặc ưu tiên `defaultClass`.

---

## 11. Minh Hoạ Thực Tế: Mushroom

Áp dụng CMAR trên dataset **Mushroom** (UCI) — một benchmark kinh điển:

### 11.1 Dataset

| Thuộc tính | Giá trị |
|------------|---------|
| Số bản ghi | 8,124 |
| Số thuộc tính | 22 |
| Số class | 2 (edible / poisonous) |
| Phân bố class | 4208 edible (51.8%) / 3916 poisonous (48.2%) |

→ **defaultClass = edible**.

### 11.2 Đặc điểm nổi bật

Dataset Mushroom nổi tiếng có **"luật vàng"** — thuộc tính `odor` (mùi) gần như **quyết định** độc hay ăn được:

| odor | edible | poisonous | Luật |
|:----:|:------:|:---------:|------|
| almond (a) | 400 | 0 | `odor=a ⇒ edible` (100%) |
| anise (l) | 400 | 0 | `odor=l ⇒ edible` (100%) |
| none (n) | 3408 | 120 | `odor=n ⇒ edible` (96.6%) |
| foul (f) | 0 | 2160 | `odor=f ⇒ poisonous` (100%) |
| fishy (y) | 0 | 576 | `odor=y ⇒ poisonous` (100%) |
| ... | ... | ... | ... |

→ Chỉ cần biết `odor`, ta phân lớp đúng **~98.5%** toàn bộ dataset.

### 11.3 Chạy CMAR với `minSup = 15%`, `minConf = 0.5`, `χ²_thr = 3.841`, `δ = 4`

**Bước 1:**
- Đếm xong. `defaultClass = edible`.
- F-list có khoảng 30 items sau lọc.

**Bước 2:**
- Xây CR-tree với 6499 training records → cây khoảng vài nghìn node.

**Bước 3 (sinh luật):**
- FP-Growth sinh ~2000 luật ứng viên, trong đó có các luật mạnh như `odor=f ⇒ poisonous`.

**Bước 4 (cắt tỉa 3 tầng):**
- Tầng 1: ~2000 → ~300 (loại các luật kiểu `odor=f ∧ cap=x ⇒ poisonous` bị `odor=f ⇒ poisonous` lấn át).
- Tầng 2: ~300 → ~200 (loại luật χ² thấp hoặc tương quan âm).
- Tầng 3: ~200 → ~50-100 (coverage).

**Bước 5:** Lưu 50-100 luật vào CR-tree.

**Bước 6 (classify):**
- Phần lớn test records khớp 1-2 luật odor mạnh → phân lớp đúng ngay.
- Rất ít record cần weighted χ² vì hầu hết luật đều đồng nhất class.

### 11.4 Kết quả

| Phương pháp | Accuracy |
|-------------|:--------:|
| **CMAR (10-fold CV)** | **98.94%** |
| Bài báo CMAR | 100% |
| CBA | 100% |
| C4.5 | 100% |

Chênh lệch ~1% với bài báo (do minSup cao hơn bài báo).

---

## 12. Bảng Tra Cứu Thuật Ngữ Nhanh

Dưới đây là toàn bộ thuật ngữ quan trọng, giải thích ngắn gọn:

### 12.1 Dữ liệu & luật

| Thuật ngữ | Giải thích nhanh |
|-----------|------------------|
| **Transaction / Record / Bản ghi** | 1 hàng dữ liệu = tập items + class |
| **Item** | Cặp `thuộc_tính=giá_trị` (vd `odor=f`) |
| **Pattern / Itemset** | Tập con của items |
| **Condset** | Vế trái của luật (điều kiện) |
| **CAR / Class Association Rule** | Luật dạng `P ⇒ c`: "nếu khớp P thì class = c" |
| **Class label** | Nhãn lớp (vd `edible`, `poisonous`) |

### 12.2 Độ đo

| Thuật ngữ | Giải thích nhanh |
|-----------|------------------|
| **Support (sup)** | Số bản ghi ủng hộ luật |
| **Confidence (conf)** | Tỉ lệ luật đúng = sup(R) / sup(P) |
| **Chi-square (χ²)** | Đo ý nghĩa thống kê của mối liên hệ |
| **max χ²** | Cận trên lý thuyết của χ² (dùng chuẩn hoá) |
| **Weighted χ² score** | Điểm tổng hợp sức mạnh nhóm luật (dùng khi classify) |

### 12.3 Cấu trúc dữ liệu

| Thuật ngữ | Giải thích nhanh |
|-----------|------------------|
| **FP-tree** | Cây tiền tố nén dữ liệu (của Han 2000) |
| **CR-tree** | FP-tree mở rộng — có thêm `classCount` tại mỗi node |
| **Node** | Đỉnh của cây, lưu item + count + classCount + children |
| **`classCount`** | Dict tại mỗi node: `{class: số lượng}` — phân bố class đi qua node |
| **Header Table** | Chỉ mục: item → con trỏ đến các node chứa item |
| **F-list** | Danh sách item phổ biến, sắp giảm dần theo tần số |
| **Conditional Pattern Base** | "Bối cảnh xuất hiện" của 1 item — các prefix path từ root |
| **Conditional FP-tree** | Cây con cho không gian "patterns chứa X" |

### 12.4 Tham số

| Tham số | Giải thích nhanh | Mặc định |
|---------|------------------|:--------:|
| **minSup** | Ngưỡng support tối thiểu | 1% |
| **minConf** | Ngưỡng confidence tối thiểu | 50% |
| **χ²_threshold** | Ngưỡng χ² để luật có ý nghĩa | 3.841 |
| **δ (delta)** | Coverage threshold (số lần cover tối đa/record) | 4 |
| **`defaultClass`** | **Class phổ biến nhất — trả khi không luật nào khớp test record** | auto |

### 12.5 Các bước & khái niệm chính

| Thuật ngữ | Giải thích nhanh |
|-----------|------------------|
| **Rule Precedence** | Thứ tự ưu tiên: conf↓ → sup↓ → size↑ |
| **General Rule** | Luật đơn giản (ít điều kiện) hơn luật khác |
| **Redundant Rule** | Luật bị luật tổng quát + mạnh hơn lấn át → loại ở Tầng 1 |
| **Positive Correlation** | Tương quan dương: vế trái làm TĂNG khả năng vế phải |
| **Database Coverage** | Cơ chế Tầng 3: bản ghi bị cover δ lần thì loại |
| **Pruning 3 tầng** | 3 bước lọc luật: General → χ² → Coverage |
| **Weighted χ²** | Công thức tổng hợp sức mạnh luật khi classify |

---

## 13. Lời Khuyên Tinh Chỉnh Tham Số

### 13.1 Khi accuracy thấp

**Triệu chứng:** Bộ phân lớp đoán sai nhiều.

**Thử:**
- **Giảm `minSup`** → giữ thêm luật cho minority class.
- **Giảm `χ²_threshold`** (vd xuống 2.706) → giữ thêm luật có ý nghĩa yếu.
- **Tăng `δ`** (vd lên 6) → giữ thêm luật dự phòng.

### 13.2 Khi chạy quá chậm / hết RAM

**Triệu chứng:** Thuật toán chạy hàng giờ hoặc báo OutOfMemory.

**Thử:**
- **Tăng `minSup`** → ít pattern hơn → nhanh hơn.
- **Tăng `minConf`** → ít luật hơn.
- Giảm kích thước tập train.

### 13.3 Khi mô hình overfitting

**Triệu chứng:** Accuracy training cao (99%+) nhưng test thấp (70-80%).

**Thử:**
- **Tăng `minSup`** → loại luật quá "cụ thể".
- **Tăng `χ²_threshold`** (vd lên 6.635) → chỉ giữ luật rất mạnh.
- **Tăng `minConf`** (vd lên 0.7).

### 13.4 Khi class mất cân bằng

**Triệu chứng:** Majority class được dự đoán đúng, minority class luôn sai.

**Thử:**
- **Giảm `minSup`** (có thể đến 0.5%) → tạo luật cho minority.
- **Tăng `δ`** → giữ nhiều luật.
- Xem xét áp dụng oversampling minority class trước khi chạy CMAR.

### 13.5 Bảng khuyến nghị nhanh

| Đặc điểm dataset | minSup | minConf | δ | χ²_thr |
|------------------|:------:|:-------:|:-:|:------:|
| Nhỏ (<500), cân bằng | 2 (abs) | 0.5 | 2 | 2.706 |
| Trung bình, cân bằng | 1-2% | 0.5 | 4 | 3.841 |
| Lớn (>5000) | 1% | 0.5 | 4 | 3.841 |
| Imbalance | 0.5% | 0.5 | 6 | 2.706 |
| Nhiều thuộc tính (>30) | 3% | 0.6 | 4 | 3.841 |
| Nhiễu cao | 2% | 0.7 | 4 | 6.635 |

---

## 14. Tổng Kết

### 14.1 CMAR "chạy" như thế nào?

**Ngắn gọn:**

1. **Đọc data**, đếm tần số, tìm `defaultClass`, lọc item hiếm.
2. **Xây cây CR-tree** nén toàn bộ dữ liệu — mỗi node lưu `classCount` (phân bố class).
3. **Đào mỏ cây** bằng FP-Growth → phát hiện pattern + sinh luật **ngay lập tức** (không cần pass thứ hai nhờ `classCount`).
4. **Cắt tỉa 3 tầng:**
   - Tầng 1: loại luật dư thừa.
   - Tầng 2: loại luật không có ý nghĩa thống kê.
   - Tầng 3: loại luật không đóng góp thêm.
5. **Lưu** các luật sống sót vào CR-tree (để truy vấn nhanh).
6. **Phân lớp** bản ghi mới: tìm luật khớp, nếu đồng nhất class → trả class đó; nếu mâu thuẫn → weighted χ² chọn class mạnh nhất; nếu không có luật khớp → trả `defaultClass`.

### 14.2 Vai trò của `defaultClass` — Tóm gọn 3 câu

1. `defaultClass` là **class phổ biến nhất** trong training set, được tính ở Bước 1.
2. Nó **không tham gia** vào quá trình đào mỏ hay cắt tỉa — chỉ là "phương án dự phòng".
3. Được trả về khi phân lớp một bản ghi mà **không luật nào khớp** — chiến lược "đoán theo majority" luôn tốt hơn đoán ngẫu nhiên.

### 14.3 Cắt tỉa 3 tầng — Tóm gọn

- **Tầng 1 (General Rule):** "Luật đơn giản mạnh hơn đã thay thế được luật chi tiết → loại."
- **Tầng 2 (χ²):** "Mối liên hệ có đủ thật không, hay chỉ ngẫu nhiên? → loại luật không đủ."
- **Tầng 3 (Coverage):** "Các bản ghi training đã được luật mạnh cover đủ chưa? → loại luật dư."

### 14.4 Điểm mạnh then chốt của CMAR

1. **Dùng nhiều luật** khi phân lớp (qua weighted χ²) → ít sai hơn CBA (chỉ dùng 1 luật).
2. **Sinh luật trong lúc đào mỏ** (nhờ `classCount`) → nhanh gấp nhiều lần phương pháp 2-pass.
3. **CR-tree nén luật** → tiết kiệm 50–60% bộ nhớ.
4. **3 tầng pruning** → loại >99% luật dư thừa, giữ lại tinh hoa.

### 14.5 Khi nào nên / không nên dùng CMAR?

**Nên dùng khi:**
- Dữ liệu **categorical** (rời rạc) hoặc dễ discretize.
- Cần mô hình **tường minh, giải thích được**.
- Có nhiều **tương quan phức tạp** giữa các thuộc tính.
- Quy mô dataset **vừa** (dưới 100k records).

**Không nên dùng khi:**
- Thuộc tính liên tục không thể discretize hợp lý.
- Dataset quá lớn (triệu records) — có các thuật toán scale tốt hơn.
- Cần mô hình "hộp đen" chạy nhanh (dùng Random Forest, Neural Net).

### 14.6 Bản đồ tổng thể — Một hình

```
         ┌────────────────────────────────────┐
         │ TRAINING SET T                     │
         │ (vd: 6499 nấm có nhãn e/p)         │
         └─────────────┬──────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                    HUẤN LUYỆN (1 lần)                        │
│                                                              │
│  ① Đếm & lọc  →  defaultClass (vd "edible")                  │
│                  F-list (item phổ biến sắp giảm dần)         │
│                                                              │
│  ② CR-tree khởi tạo  (nén toàn bộ data, mỗi node có          │
│                       classCount)                            │
│                                                              │
│  ③ FP-Growth  →  sinh CAR trực tiếp  →  ~2000 ứng viên       │
│                                                              │
│  ④ Cắt tỉa 3 tầng:                                           │
│     Tầng 1 (General)  →  ~300                                │
│     Tầng 2 (χ²)       →  ~200                                │
│     Tầng 3 (Coverage) →  ~50-100                             │
│                                                              │
│  ⑤ CR-tree lưu trữ  ←  chèn các luật sống sót                │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │     MÔ HÌNH CMAR     │
                  │  (CR-tree + default) │
                  └──────────┬───────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────┐
│              PHÂN LỚP (mỗi record mới)                       │
│                                                              │
│  Record t → truy vấn CR-tree → R(t) = tập luật khớp          │
│                                                              │
│  R(t) = ∅ ?                 → trả defaultClass               │
│  R(t) đồng nhất 1 class ?   → trả class đó                   │
│  R(t) có nhiều class ?      → weighted χ² → argmax           │
└──────────────────────────────────────────────────────────────┘
```

---

## Lời Kết

CMAR **không phải** một thuật toán "ma thuật" — nó là kết quả của việc kết hợp khéo léo nhiều ý tưởng:

- **FP-tree** (nén dữ liệu) của Han 2000.
- **Association rule mining** (Apriori) của Agrawal 1994.
- **χ² test** (kiểm định thống kê) của thống kê cổ điển.
- **Database coverage** (cắt tỉa luật) của CBA.

Nhưng bằng cách thêm một trường nhỏ (`classCount`) vào node của cây, CMAR **biến đổi** tất cả những thành phần trên thành một thuật toán thống nhất, hiệu quả, và chính xác — có thể phân lớp từ dataset nhỏ (Zoo 101 records) đến lớn (Mushroom 8124 records) với accuracy cạnh tranh.

Hy vọng sau khi đọc file này, bạn đã nắm được:
- **Cách CMAR chạy** qua 6 bước lớn.
- **Mỗi tham số** (minSup, minConf, χ²_thr, δ) làm gì.
- **Vai trò `defaultClass`** — "phương án dự phòng" khi không có luật khớp.
- **Cơ chế 3 tầng cắt tỉa** — mỗi tầng lọc một loại luật "xấu" khác nhau.
- **Tại sao** dùng weighted χ² — công bằng giữa các luật dù quy mô khác nhau.

---

*File này tổng hợp các ý chính từ các báo cáo trước — tập trung vào **cách thuật toán vận hành** hơn là **chứng minh toán học**. Xem thêm các file bổ sung nếu cần chi tiết sâu hơn:*
- *`bao_cao_chi_tiet_CMAR.md`* — chi tiết công thức và ví dụ đầy đủ
- *`giai_thich_cong_thuc_CMAR.md`* — giải thích sâu từng công thức
- *`bao_cao_thuat_toan_CMAR.md`* — ánh xạ sang cài đặt Java
