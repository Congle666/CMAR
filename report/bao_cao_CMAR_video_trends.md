# BÁO CÁO HOÀN CHỈNH: THUẬT TOÁN CMAR
## Classification Based on Multiple Association Rules

**Dataset:** Video Trends (100 bản ghi)  
**Tham khảo:** Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* ICDM 2001.

---

## Mục Lục

1. [Tổng Quan Pipeline](#1-tổng-quan-pipeline)
2. [Dữ Liệu Đầu Vào](#2-dữ-liệu-đầu-vào)
3. [Cấu Hình Chạy](#3-cấu-hình-chạy)
4. [PHẦN 1: FP-Growth — Đào Mỏ Tập Phổ Biến](#4-phần-1-fp-growth--đào-mỏ-tập-phổ-biến)
   - 4.1 Thuật toán FP-Growth
   - 4.2 Bước 1: Đếm tần suất item
   - 4.3 Bước 2: Xây dựng cây FP-Tree
   - 4.4 Bước 3: Mining đệ quy
   - 4.5 Kết quả: Frequent Patterns
5. [PHẦN 2: CMAR — Phân Lớp](#5-phần-2-cmar--phân-lớp)
   - 5.1 Thuật toán CMAR
   - 5.2 Bước 1: Trích xuất CARs
   - 5.3 Bước 2: Pruning
   - 5.4 Bước 3: Phân lớp bằng Weighted χ²
   - 5.5 Kết quả dự đoán
6. [Đánh Giá Hiệu Suất](#6-đánh-giá-hiệu-suất)
7. [Hướng Dẫn Chạy Chương Trình](#7-hướng-dẫn-chạy-chương-trình)

---

## 1. Tổng Quan Pipeline

```
┌─────────────┐     ┌─────────────┐     ┌───────────────┐     ┌──────────────┐
│  Load CSV   │────▶│  FP-Growth  │────▶│  CAR Extract  │────▶│  CMAR Prune  │
│  Dataset    │     │  Mining     │     │  (Rules)      │     │  & Classify  │
└─────────────┘     └─────────────┘     └───────────────┘     └──────────────┘
     Step 1            Step 2-3             Step 4              Step 5-6-7
```

| Bước | Mô tả | Đầu ra |
|------|--------|--------|
| 1 | Load CSV → List\<Transaction\> | 100 bản ghi |
| 2 | Xây dựng cây FP-Tree | Cây FP-Tree + Header Table |
| 3 | Mining đệ quy trên FP-Tree | 290 frequent patterns |
| 4 | Trích xuất CARs (condset → class) | 131 candidate rules |
| 5 | Pruning luật bị dominated | 37 luật sau pruning |
| 6 | Phân lớp bằng weighted χ² | Dự đoán 20 test records |
| 7 | Đánh giá kết quả | **Accuracy = 95.00%** |

---

## 2. Dữ Liệu Đầu Vào

**File:** `data/video_trends.csv` — 100 bản ghi dự đoán xu hướng video

### Các thuộc tính:

| Cột | Thuộc tính | Các giá trị | Mô tả |
|-----|-----------|-------------|-------|
| 1 | category | Gaming, Education, Vlog, Comedy, Tech, Music | Thể loại video |
| 2 | duration | Short, Medium, Long | Độ dài video |
| 3 | quality | SD, HD, 4K | Chất lượng hình ảnh |
| 4 | subtitles | Yes, No | Có phụ đề không |
| 5 | time_of_day | Morning, Afternoon, Evening, Night | Thời điểm đăng |
| 6 | **trend** (class) | **Viral, Flop** | Nhãn phân lớp |

### Phân bố class:
- **Viral:** 68 bản ghi (68%)
- **Flop:** 32 bản ghi (32%)

### Ví dụ dữ liệu:
```
category,duration,quality,subtitles,time_of_day,trend
Gaming,Long,4K,Yes,Evening,Viral
Education,Medium,HD,Yes,Morning,Viral
Vlog,Long,SD,No,Afternoon,Flop
Comedy,Short,HD,Yes,Night,Viral
```

### Mã hóa item:
Mỗi cặp thuộc tính-giá trị thành 1 item. Ví dụ bản ghi đầu tiên:
```
Items: [category=Gaming, duration=Long, quality=4K, subtitles=Yes, time_of_day=Evening]
Class: class=Viral
```

---

## 3. Cấu Hình Chạy

| Tham số | Giá trị | Ý nghĩa |
|---------|---------|---------|
| Dataset | `data/video_trends_h.csv` | File dữ liệu đầu vào |
| Tổng bản ghi | 100 | |
| Train / Test | 80 / 20 | Tỷ lệ 80%-20% |
| **Min Support** | **5** | Item phải xuất hiện ≥ 5 lần trong 80 training records (6.25%) |
| **Min Confidence** | **0.5** | Luật phải có confidence ≥ 50% |
| Random Seed | 42 | Đảm bảo kết quả tái lập |

### Lệnh chạy:
```bash
cd d:\CMAR
javac -d out src\*.java
java -cp out Main data/video_trends_h.csv 5 0.5
```

---

## 4. PHẦN 1: FP-Growth — Đào Mỏ Tập Phổ Biến

### 4.1 Thuật Toán FP-Growth

FP-Growth (Frequent Pattern Growth) là thuật toán khai phá tập phổ biến **không cần sinh candidate** như Apriori. Thay vào đó, nó nén dữ liệu vào cấu trúc **FP-Tree** rồi mining trực tiếp trên cây.

**Ưu điểm so với Apriori:**
- Chỉ quét DB 2 lần (đếm tần suất + xây cây)
- Không sinh tập candidate → nhanh hơn nhiều lần
- Nén dữ liệu hiệu quả nhờ chia sẻ tiền tố chung

---

### 4.2 Bước 1: Đếm Tần Suất Item

Duyệt qua 80 giao dịch training, đếm số lần xuất hiện mỗi item. Loại bỏ item có tần suất < **minSupport = 5**.

**Header Table (Bảng Tần Suất):**

| # | Item | Tần suất | Tỷ lệ |
|---|------|----------|--------|
| 1 | class=Viral | 57 | 71.25% |
| 2 | subtitles=Yes | 51 | 63.75% |
| 3 | quality=HD | 36 | 45.00% |
| 4 | duration=Short | 30 | 37.50% |
| 5 | subtitles=No | 29 | 36.25% |
| 6 | quality=4K | 29 | 36.25% |
| 7 | duration=Long | 27 | 33.75% |
| 8 | class=Flop | 23 | 28.75% |
| 9 | duration=Medium | 23 | 28.75% |
| 10 | time_of_day=Morning | 22 | 27.50% |
| 11 | time_of_day=Afternoon | 20 | 25.00% |
| 12 | time_of_day=Night | 20 | 25.00% |
| 13 | time_of_day=Evening | 18 | 22.50% |
| 14 | category=Comedy | 16 | 20.00% |
| 15 | quality=SD | 15 | 18.75% |
| 16 | category=Education | 15 | 18.75% |
| 17 | category=Music | 14 | 17.50% |
| 18 | category=Tech | 13 | 16.25% |
| 19 | category=Vlog | 11 | 13.75% |
| 20 | category=Gaming | 11 | 13.75% |

> Tất cả 20 item đều có tần suất ≥ 5 nên không có item nào bị loại.

---

### 4.3 Bước 2: Xây Dựng Cây FP-Tree

**Cách xây dựng:**
1. Sắp xếp các item trong mỗi giao dịch theo **tần suất giảm dần** (theo bảng trên)
2. Chèn lần lượt từng giao dịch vào cây, bắt đầu từ nút gốc (root)
3. Nếu nút con cùng item **đã tồn tại** → tăng count
4. Nếu **chưa có** → tạo nút mới
5. Mỗi item trong Header Table có con trỏ (node-link) liên kết tất cả nút cùng item

**Ví dụ chèn giao dịch đầu tiên:**
```
Record: Gaming, Long, 4K, Yes, Evening → Viral
Sau sắp xếp theo tần suất: class=Viral(57) → subtitles=Yes(51) → quality=4K(29) → duration=Long(27) → time_of_day=Evening(18) → category=Gaming(11)

Root → class=Viral:1 → subtitles=Yes:1 → quality=4K:1 → duration=Long:1 → time_of_day=Evening:1 → category=Gaming:1
```

**Cấu trúc cây FP-Tree hoàn chỉnh (80 giao dịch):**

```
[root]
├── class=Viral:57
│   ├── subtitles=Yes:51
│   │   ├── quality=HD:24
│   │   │   ├── duration=Medium:7
│   │   │   │   ├── time_of_day=Evening:2
│   │   │   │   │   └── category=Comedy:2
│   │   │   │   ├── time_of_day=Morning:2
│   │   │   │   │   └── category=Education:2
│   │   │   │   ├── time_of_day=Afternoon:2
│   │   │   │   │   ├── category=Music:1
│   │   │   │   │   └── category=Tech:1
│   │   │   │   └── time_of_day=Night:1
│   │   │   │       └── category=Music:1
│   │   │   ├── duration=Long:9
│   │   │   │   ├── time_of_day=Evening:1
│   │   │   │   │   └── category=Comedy:1
│   │   │   │   ├── time_of_day=Night:3
│   │   │   │   │   ├── category=Gaming:1
│   │   │   │   │   ├── category=Comedy:1
│   │   │   │   │   └── category=Vlog:1
│   │   │   │   ├── time_of_day=Morning:4
│   │   │   │   │   └── category=Education:4
│   │   │   │   └── time_of_day=Afternoon:1
│   │   │   │       └── category=Tech:1
│   │   │   └── duration=Short:8
│   │   │       ├── time_of_day=Morning:3
│   │   │       │   ├── category=Tech:1
│   │   │       │   └── category=Education:2
│   │   │       ├── time_of_day=Night:3
│   │   │       │   └── category=Comedy:3
│   │   │       ├── time_of_day=Evening:1
│   │   │       │   └── category=Music:1
│   │   │       └── time_of_day=Afternoon:1
│   │   │           └── category=Music:1
│   │   ├── duration=Short:9
│   │   │   └── quality=4K:9
│   │   │       ├── time_of_day=Evening:6
│   │   │       │   └── category=Comedy:6
│   │   │       ├── time_of_day=Morning:1
│   │   │       │   └── category=Tech:1
│   │   │       └── time_of_day=Afternoon:2
│   │   │           └── category=Tech:2
│   │   └── quality=4K:18
│   │       ├── duration=Long:11
│   │       │   ├── time_of_day=Afternoon:3
│   │       │   │   ├── category=Music:1
│   │       │   │   └── category=Tech:2
│   │       │   ├── time_of_day=Evening:1
│   │       │   │   └── category=Gaming:1
│   │       │   ├── time_of_day=Morning:3
│   │       │   │   ├── category=Tech:1
│   │       │   │   └── category=Education:2
│   │       │   └── time_of_day=Night:4
│   │       │       ├── category=Music:3
│   │       │       └── category=Gaming:1
│   │       └── duration=Medium:7
│   │           ├── time_of_day=Night:1
│   │           │   └── category=Gaming:1
│   │           ├── time_of_day=Morning:2
│   │           │   └── category=Education:2
│   │           ├── time_of_day=Evening:2
│   │           │   └── category=Gaming:2
│   │           └── time_of_day=Afternoon:2
│   │               └── category=Tech:2
│   ├── quality=4K:2
│   │   └── subtitles=No:2
│   │       ├── duration=Medium:1
│   │       │   └── time_of_day=Evening:1
│   │       │       └── category=Gaming:1
│   │       └── duration=Long:1
│   │           └── time_of_day=Night:1
│   │               └── category=Gaming:1
│   └── quality=HD:4
│       ├── duration=Short:3
│       │   └── subtitles=No:3
│       │       └── time_of_day=Night:3
│       │           └── category=Comedy:3
│       └── subtitles=No:1
│           └── duration=Long:1
│               └── time_of_day=Evening:1
│                   └── category=Gaming:1
├── quality=HD:8
│   ├── duration=Short:4
│   │   └── subtitles=No:4
│   │       └── class=Flop:4
│   │           ├── time_of_day=Afternoon:2
│   │           │   ├── category=Gaming:1
│   │           │   └── category=Vlog:1
│   │           └── time_of_day=Morning:2
│   │               ├── category=Vlog:1
│   │               └── category=Education:1
│   └── subtitles=No:4
│       ├── duration=Long:1
│       │   └── class=Flop:1
│       │       └── time_of_day=Afternoon:1
│       │           └── category=Vlog:1
│       └── duration=Medium:3
│           └── class=Flop:3
│               ├── time_of_day=Morning:1
│               │   └── category=Tech:1
│               └── time_of_day=Afternoon:2
│                   ├── category=Tech:1
│                   └── category=Vlog:1
├── subtitles=No:9
│   ├── duration=Long:4
│   │   └── class=Flop:4
│   │       ├── time_of_day=Evening:1
│   │       │   └── quality=SD:1
│   │       │       └── category=Music:1
│   │       ├── time_of_day=Afternoon:1
│   │       │   └── quality=SD:1
│   │       │       └── category=Vlog:1
│   │       ├── time_of_day=Night:1
│   │       │   └── quality=SD:1
│   │       │       └── category=Vlog:1
│   │       └── time_of_day=Morning:1
│   │           └── quality=SD:1
│   │               └── category=Music:1
│   └── duration=Medium:5
│       └── class=Flop:5
│           ├── time_of_day=Afternoon:2
│           │   └── quality=SD:2
│           │       └── category=Vlog:2
│           ├── time_of_day=Night:2
│           │   └── quality=SD:2
│           │       ├── category=Music:1
│           │       └── category=Vlog:1
│           └── time_of_day=Morning:1
│               └── category=Education:1
│                   └── quality=SD:1
└── duration=Short:6
    └── subtitles=No:6
        └── class=Flop:6
            ├── time_of_day=Evening:2
            │   └── quality=SD:2
            │       └── category=Music:2
            ├── time_of_day=Morning:2
            │   ├── category=Education:1
            │   │   └── quality=SD:1
            │   └── quality=SD:1
            │       └── category=Gaming:1
            ├── time_of_day=Night:1
            │   └── quality=SD:1
            │       └── category=Music:1
            └── time_of_day=Afternoon:1
                └── quality=SD:1
                    └── category=Vlog:1
```

**Cách đọc cây:**
- Nhánh đầu tiên `class=Viral:57 → subtitles=Yes:51` nghĩa là: 51 trong 57 record Viral có subtitles=Yes
- Nhánh `class=Viral:57 → subtitles=Yes:51 → quality=HD:24` nghĩa là: 24 record vừa Viral + Yes + HD
- Các nhánh bên dưới `quality=HD:8` (không qua class=Viral) là các record Flop có HD

---

### 4.4 Bước 3: Mining Đệ Quy

Với mỗi item trong header table (theo thứ tự tần suất **tăng dần**: category=Gaming → category=Vlog → ... → class=Viral):

1. **Ghi nhận pattern** `{prefix ∪ item}` với support tương ứng
2. **Thu thập Conditional Pattern Base** — tất cả đường đi từ nút chứa item lên gốc
3. **Xây Conditional FP-Tree** từ các đường đi đó
4. **Đệ quy** trên conditional tree

**Ví dụ mining cho item `category=Gaming` (tần suất = 11):**

```
Conditional Pattern Base cho category=Gaming:
  Path 1: [class=Viral, subtitles=Yes, quality=HD, duration=Long, time_of_day=Night] : count=1
  Path 2: [class=Viral, subtitles=Yes, quality=4K, time_of_day=Evening] : count=1  
  Path 3: [class=Viral, subtitles=Yes, quality=4K, duration=Long, time_of_day=Night] : count=1
  ...
→ Xây Conditional FP-Tree → Đệ quy tìm các frequent patterns chứa category=Gaming
```

---

### 4.5 Kết Quả: Frequent Patterns

**Tổng: 290 frequent patterns** (support ≥ 5)

**Top 20 patterns theo support:**

| # | Pattern | Support | Tỷ lệ |
|---|---------|---------|--------|
| 1 | {class=Viral} | 57 | 71.25% |
| 2 | {subtitles=Yes} | 51 | 63.75% |
| 3 | {subtitles=Yes, class=Viral} | 51 | 63.75% |
| 4 | {quality=HD} | 36 | 45.00% |
| 5 | {duration=Short} | 30 | 37.50% |
| 6 | {subtitles=No} | 29 | 36.25% |
| 7 | {quality=4K} | 29 | 36.25% |
| 8 | {class=Viral, quality=4K} | 29 | 36.25% |
| 9 | {quality=HD, class=Viral} | 28 | 35.00% |
| 10 | {duration=Long} | 27 | 33.75% |

> **Nhận xét:** Pattern {subtitles=Yes, class=Viral} có support = 51 = support của {subtitles=Yes} → 100% video có phụ đề là Viral. Tương tự, {quality=4K, class=Viral} support = 29 = support {quality=4K} → 100% video 4K là Viral.

---

## 5. PHẦN 2: CMAR — Phân Lớp

### 5.1 Thuật Toán CMAR

CMAR (Classification based on Multiple Association Rules) cải tiến so với CBA bằng cách:
- Dùng **nhiều luật** cùng lúc thay vì chỉ 1 luật tốt nhất
- Kết hợp bằng **weighted chi-square** (χ²) theo nhóm class
- **Pruning** luật bị dominated để giảm số luật lưu trữ

---

### 5.2 Bước 1: Trích Xuất CARs (Class Association Rules)

Từ 290 frequent patterns, lọc ra các pattern chứa **đúng 1 item class** (`class=Viral` hoặc `class=Flop`).

**Công thức:**
- **Support(r)** = support(condset ∪ class) / tổng training records
- **Confidence(r)** = support(condset ∪ class) / support(condset)

Chỉ giữ luật có confidence ≥ 0.5

**Kết quả: 131 candidate rules**

**Top 10 luật trước pruning:**

| # | Luật | Support | Confidence |
|---|------|---------|------------|
| 1 | {subtitles=Yes} → Viral | 0.6375 | 1.0000 |
| 2 | {quality=4K} → Viral | 0.3625 | 1.0000 |
| 3 | {subtitles=Yes, quality=4K} → Viral | 0.3375 | 1.0000 |
| 4 | {subtitles=Yes, quality=HD} → Viral | 0.3000 | 1.0000 |
| 5 | {subtitles=Yes, duration=Long} → Viral | 0.2500 | 1.0000 |
| 6 | {subtitles=Yes, duration=Short} → Viral | 0.2125 | 1.0000 |
| 7 | {category=Comedy} → Viral | 0.2000 | 1.0000 |
| 8 | {quality=SD} → Flop | 0.1875 | 1.0000 |
| 9 | {quality=SD, subtitles=No} → Flop | 0.1875 | 1.0000 |
| 10 | {time_of_day=Morning, subtitles=Yes} → Viral | 0.1875 | 1.0000 |

---

### 5.3 Bước 2: Pruning (Tỉa Luật Bị Dominated)

**Nguyên tắc (Section 3.1 trong paper):**

Sắp xếp luật theo thứ tự ưu tiên:
1. **Confidence giảm dần**
2. Nếu bằng nhau → **support giảm dần**
3. Nếu vẫn bằng → **condset ngắn hơn** (luật đơn giản hơn ưu tiên)

Luật r₁ **bị loại** nếu tồn tại luật r₂ ưu tiên cao hơn sao cho:
- `condset(r₂) ⊆ condset(r₁)` (r₂ tổng quát hơn)
- `class(r₂) = class(r₁)` (cùng class)

**Ví dụ pruning:**

```
Luật #1: {subtitles=Yes} → Viral         [conf=1.0, sup=0.6375]  → GIỮ ✓
Luật #3: {subtitles=Yes, quality=4K} → Viral  [conf=1.0, sup=0.3375]  → LOẠI ✗
  Lý do: {subtitles=Yes} ⊆ {subtitles=Yes, quality=4K} và cùng class Viral
         → bị dominated bởi luật #1

Luật #8: {quality=SD} → Flop             [conf=1.0, sup=0.1875]  → GIỮ ✓
Luật #9: {quality=SD, subtitles=No} → Flop  [conf=1.0, sup=0.1875]  → LOẠI ✗
  Lý do: {quality=SD} ⊆ {quality=SD, subtitles=No} và cùng class Flop
         → bị dominated bởi luật #8
```

**Kết quả pruning:**
| | Số luật |
|--|---------|
| Trước pruning | 131 |
| **Sau pruning** | **37** |
| Bị loại | 94 (71.8%) |

---

### 5.4 Danh Sách 37 Luật Sau Pruning

| # | Luật (Condset → Class) | Support | Confidence |
|---|------------------------|---------|------------|
| 1 | {subtitles=Yes} → Viral | 0.6375 | 1.0000 |
| 2 | {quality=4K} → Viral | 0.3625 | 1.0000 |
| 3 | {category=Comedy} → Viral | 0.2000 | 1.0000 |
| 4 | {quality=SD} → Flop | 0.1875 | 1.0000 |
| 5 | {category=Vlog, subtitles=No} → Flop | 0.1250 | 1.0000 |
| 6 | {time_of_day=Night, quality=HD} → Viral | 0.1250 | 1.0000 |
| 7 | {subtitles=No, time_of_day=Afternoon} → Flop | 0.1125 | 1.0000 |
| 8 | {category=Vlog, time_of_day=Afternoon} → Flop | 0.0875 | 1.0000 |
| 9 | {time_of_day=Morning, subtitles=No} → Flop | 0.0875 | 1.0000 |
| 10 | {subtitles=No, category=Music} → Flop | 0.0750 | 1.0000 |
| 11 | {duration=Long, category=Education} → Viral | 0.0750 | 1.0000 |
| 12 | {category=Gaming, time_of_day=Evening} → Viral | 0.0625 | 1.0000 |
| 13 | {duration=Long, category=Gaming} → Viral | 0.0625 | 1.0000 |
| 14 | {duration=Medium, time_of_day=Evening} → Viral | 0.0625 | 1.0000 |
| 15 | {quality=HD, time_of_day=Evening} → Viral | 0.0625 | 1.0000 |
| 16 | {category=Vlog} → Flop | 0.1250 | 0.9091 |
| 17 | {duration=Long, quality=HD} → Viral | 0.1250 | 0.9091 |
| 18 | {time_of_day=Afternoon, category=Tech} → Viral | 0.1000 | 0.8889 |
| 19 | {quality=HD, category=Education} → Viral | 0.1000 | 0.8889 |
| 20 | {time_of_day=Night, duration=Long} → Viral | 0.1000 | 0.8889 |
| 21 | {subtitles=No, duration=Medium} → Flop | 0.1000 | 0.8889 |
| 22 | {time_of_day=Morning, duration=Long} → Viral | 0.0875 | 0.8750 |
| 23 | {time_of_day=Night, duration=Short} → Viral | 0.0750 | 0.8571 |
| 24 | {category=Tech} → Viral | 0.1375 | 0.8462 |
| 25 | {time_of_day=Evening} → Viral | 0.1875 | 0.8333 |
| 26 | {category=Gaming} → Viral | 0.1125 | 0.8182 |
| 27 | {duration=Long} → Viral | 0.2750 | 0.8148 |
| 28 | {time_of_day=Night} → Viral | 0.2000 | 0.8000 |
| 29 | {category=Education} → Viral | 0.1500 | 0.8000 |
| 30 | {subtitles=No} → Flop | 0.2875 | 0.7931 |
| 31 | {quality=HD} → Viral | 0.3500 | 0.7778 |
| 32 | {time_of_day=Morning} → Viral | 0.1875 | 0.6818 |
| 33 | {duration=Short} → Viral | 0.2500 | 0.6667 |
| 34 | {duration=Medium} → Viral | 0.1875 | 0.6522 |
| 35 | {category=Music} → Viral | 0.1000 | 0.5714 |
| 36 | {time_of_day=Afternoon, quality=HD} → Flop | 0.0625 | 0.5556 |
| 37 | {time_of_day=Afternoon} → Viral | 0.1375 | 0.5500 |

---

### 5.5 Bước 3: Phân Lớp Bằng Weighted Chi-Square (χ²)

**Quy trình phân lớp 1 record test (Section 3.2 trong paper):**

1. Thu thập tất cả luật match (condset ⊆ items của record)
2. Nếu **không có luật match** → trả về default class (Viral)
3. Nếu tất cả luật match **cùng 1 class** → trả về class đó
4. Nếu có luật match **nhiều class khác nhau**:
   - Nhóm luật theo class
   - Tính score cho mỗi nhóm: **score(G) = Σ conf(r) × χ²(r)**
   - Class có score cao nhất thắng

**Công thức Chi-Square cho 1 luật (condset A → class C):**

$$\chi^2 = \frac{n \times (ad - bc)^2}{(a+b)(c+d)(a+c)(b+d)}$$

Trong đó:
- n = tổng training records (80)
- a = support count(A ∩ C) — số record thỏa cả condset và class
- b = support count(A) - a — thỏa condset nhưng khác class
- c = freq(C) - a — đúng class nhưng không thỏa condset
- d = n - a - b - c — không thỏa cả hai

**Ví dụ: Phân lớp record `Gaming, Long, 4K, No, Night`**

Các luật match:
```
Luật #2:  {quality=4K} → Viral         conf=1.0      → nhóm Viral
Luật #13: {duration=Long, Gaming} → Viral  conf=1.0  → nhóm Viral
Luật #20: {Night, duration=Long} → Viral   conf=0.89 → nhóm Viral
Luật #26: {Gaming} → Viral              conf=0.82     → nhóm Viral
Luật #27: {duration=Long} → Viral       conf=0.81     → nhóm Viral
Luật #28: {Night} → Viral               conf=0.80     → nhóm Viral
Luật #30: {subtitles=No} → Flop         conf=0.79     → nhóm Flop
```

Tính score cho mỗi nhóm:
- score(Viral) = Σ conf(r) × χ²(r) cho 6 luật Viral
- score(Flop) = conf × χ² cho 1 luật Flop

→ Kết quả thực tế: chương trình dự đoán **Flop** (sai) vì luật `{subtitles=No} → Flop` có χ² rất cao. Đây là 1/20 trường hợp sai duy nhất.

---

### 5.6 Kết Quả Dự Đoán

**Default class:** Viral (vì class phổ biến nhất trong training: 57/80 = 71.25%)

| # | Record | Actual | Predicted | Kết quả |
|---|--------|--------|-----------|---------|
| 1 | Music, Medium, 4K, Yes, Evening | Viral | Viral | ✓ |
| 2 | Music, Short, HD, Yes, Night | Viral | Viral | ✓ |
| 3 | Vlog, Long, SD, No, Night | Flop | Flop | ✓ |
| 4 | Gaming, Long, 4K, Yes, Evening | Viral | Viral | ✓ |
| 5 | Tech, Long, HD, Yes, Morning | Viral | Viral | ✓ |
| 6 | Vlog, Long, SD, No, Afternoon | Flop | Flop | ✓ |
| 7 | Tech, Medium, HD, No, Morning | Flop | Flop | ✓ |
| 8 | **Gaming, Long, 4K, No, Night** | **Viral** | **Flop** | **✗** |
| 9 | Gaming, Long, HD, Yes, Evening | Viral | Viral | ✓ |
| 10 | Vlog, Long, SD, No, Afternoon | Flop | Flop | ✓ |
| 11 | Vlog, Short, HD, No, Afternoon | Flop | Flop | ✓ |
| 12 | Education, Long, HD, Yes, Morning | Viral | Viral | ✓ |
| 13 | Vlog, Short, HD, Yes, Afternoon | Viral | Viral | ✓ |
| 14 | Education, Medium, HD, Yes, Morning | Viral | Viral | ✓ |
| 15 | Comedy, Medium, SD, No, Evening | Flop | Flop | ✓ |
| 16 | Gaming, Long, 4K, Yes, Evening | Viral | Viral | ✓ |
| 17 | Vlog, Medium, SD, No, Night | Flop | Flop | ✓ |
| 18 | Tech, Short, SD, No, Morning | Flop | Flop | ✓ |
| 19 | Gaming, Medium, SD, No, Evening | Flop | Flop | ✓ |
| 20 | Gaming, Long, 4K, Yes, Night | Viral | Viral | ✓ |

> **Record #8** sai vì `subtitles=No` kéo weighted χ² về Flop mạnh hơn mặc dù `quality=4K` luôn là Viral. Đây là trường hợp biên khi 2 thuộc tính xung đột mạnh.

---

## 6. Đánh Giá Hiệu Suất

### Kết quả tổng quan:

| Metric | Giá trị |
|--------|---------|
| **Accuracy** | **19/20 = 95.00%** |
| **Macro-F1** | **0.9499** |
| Đúng | 19 |
| Sai | 1 |

### Chi tiết theo class:

| Class | TP | FP | FN | Precision | Recall | F1 |
|-------|----|----|-----|-----------|--------|------|
| **Viral** | 10 | 0 | 1 | **1.0000** | 0.9091 | 0.9524 |
| **Flop** | 9 | 1 | 0 | 0.9000 | **1.0000** | 0.9474 |

**Giải thích:**
- **Precision(Viral) = 1.0** → Khi dự đoán Viral, 100% là đúng
- **Recall(Viral) = 0.91** → Trong 11 video Viral thật, nhận diện đúng 10 (bỏ sót 1)
- **Recall(Flop) = 1.0** → Nhận diện đúng 100% video Flop
- **Precision(Flop) = 0.9** → 1 video Viral bị đoán nhầm thành Flop

### Tóm tắt pipeline:

```
100 records ──(80/20)──▶ 80 train, 20 test
                           │
                     FP-Growth (minSup=5)
                           │
                    290 frequent patterns
                           │
                    CAR extraction (minConf=0.5)
                           │
                     131 candidate rules
                           │
                    CMAR pruning
                           │
                      37 pruned rules
                           │
                    Weighted χ² classify
                           │
                    19/20 correct = 95%
```

---

## 7. Hướng Dẫn Chạy Chương Trình

### Biên dịch:
```bash
cd d:\CMAR
mkdir out 2>nul
javac -d out src\*.java
```

### Chạy:
```bash
java -cp out Main <đường_dẫn_csv> [minSupport] [minConfidence]
```

### Ví dụ:
```bash
# Video Trends (100 records)
java -cp out Main data/video_trends_h.csv 5 0.5

# Weather (14 records)
java -cp out Main data/weather.csv 2 0.5

# Car Evaluation (1728 records)
java -cp out Main data/car.csv 50 0.5

# Mushroom (8124 records)
java -cp out Main data/mushroom_full.csv 1500 0.5
```

### Chọn minSupport hợp lý:

| Kích thước dataset | minSupport gợi ý | Tỷ lệ |
|--------------------|-------------------|--------|
| < 50 records | 2-3 | ~5-10% |
| 50 - 200 records | 5-10 | ~5-10% |
| 200 - 2000 records | 20-100 | ~5-10% |
| > 2000 records | 100-2000 | 5-25% |

### Các file output:

| File | Nội dung |
|------|----------|
| `result/fpgrowth_result.txt` | **FP-Growth:** Header table + cây FP-Tree + tất cả frequent patterns |
| `result/cmar_result.txt` | **CMAR:** Pruning + luật sau pruning + dự đoán + evaluation |
| `result/frequent_patterns.txt` | Danh sách frequent patterns (raw) |
| `result/association_rules.txt` | Danh sách CARs trước pruning (raw) |
| `result/predictions.txt` | Dự đoán từng record (raw) |
| `result/evaluation.txt` | Metrics tổng hợp |
| `report/fp_tree_report.md` | Báo cáo Markdown kết hợp |

### Yêu cầu file CSV:
- Dòng đầu tiên là **header** (tên cột)
- Cột cuối cùng là **class label** (nhãn phân lớp)
- Giá trị thiếu (`?`) được bỏ qua tự động
