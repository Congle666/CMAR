# Hướng Dẫn Chạy & Giải Thích Thuật Toán CMAR

> **Dữ liệu sử dụng:** `data/car.csv` — UCI Car Evaluation Dataset (1728 bản ghi, 6 thuộc tính)

---

## Mục Lục

1. [Cấu trúc dự án](#1-cấu-trúc-dự-án)
2. [Chuẩn bị dữ liệu car.csv](#2-chuẩn-bị-dữ-liệu-carcsv)
3. [Cách biên dịch và chạy](#3-cách-biên-dịch-và-chạy)
4. [Cấu hình tham số](#4-cấu-hình-tham-số)
5. [Giải thích thuật toán FP-Tree & FP-Growth](#5-giải-thích-thuật-toán-fp-tree--fp-growth)
6. [Sinh luật kết hợp lớp (CAR)](#6-sinh-luật-kết-hợp-lớp-car)
7. [Thuật toán CMAR – Pruning & Phân lớp](#7-thuật-toán-cmar--pruning--phân-lớp)
8. [Kết quả chạy trên car.csv](#8-kết-quả-chạy-trên-carcsv)

---

## 1. Cấu Trúc Dự Án

```
CMAR/
├── claude.md                  ← Tài liệu theo dõi tiến trình
├── data/
│   ├── weather.csv            ← Dataset mẫu nhỏ (14 bản ghi)
│   └── car.csv                ← UCI Car Evaluation (1728 bản ghi)
├── result/
│   ├── frequent_patterns.txt  ← Kết quả Step 3: tập phổ biến
│   ├── association_rules.txt  ← Kết quả Step 4: luật kết hợp
│   ├── predictions.txt        ← Kết quả Step 6: dự đoán
│   └── evaluation.txt         ← Kết quả Step 7: đánh giá
├── report/
│   └── huong_dan_chay.md      ← File này
└── src/
    ├── Main.java              ← Điểm khởi chạy
    ├── DatasetLoader.java     ← Đọc CSV → List<Transaction>
    ├── Transaction.java       ← Bản ghi dữ liệu
    ├── FrequentPattern.java   ← Tập phổ biến
    ├── FPNode.java            ← Nút FP-Tree
    ├── FPTree.java            ← Cấu trúc FP-Tree
    ├── FPGrowth.java          ← Thuật toán khai thác FP-Growth
    ├── AssociationRule.java   ← Luật kết hợp lớp (CAR)
    ├── RuleGenerator.java     ← Sinh CAR từ tập phổ biến
    ├── CMARClassifier.java    ← Thuật toán CMAR
    └── ResultWriter.java      ← Ghi kết quả ra file
```

---

## 2. Chuẩn Bị Dữ Liệu car.csv

### Mô tả dataset

| Thuộc tính | Giá trị có thể |
|------------|----------------|
| `buying`   | vhigh, high, med, low |
| `maint`    | vhigh, high, med, low |
| `doors`    | 2, 3, 4, 5more |
| `persons`  | 2, 4, more |
| `lug_boot` | small, med, big |
| `safety`   | low, med, high |
| **class**  | **unacc, acc, good, vgood** |

**Phân phối nhãn lớp:**
- `unacc` — 1210 bản ghi (70%)
- `acc` — 384 bản ghi (22%)
- `good` — 69 bản ghi (4%)
- `vgood` — 65 bản ghi (4%)

### Thêm dòng tiêu đề (header) vào car.csv

File UCI gốc không có dòng tiêu đề. `DatasetLoader` đọc dòng đầu làm tên cột,
vì vậy cần thêm header trước khi chạy.

**Cách thực hiện (chạy một lần duy nhất):**

```powershell
# PowerShell
$h = 'buying,maint,doors,persons,lug_boot,safety,class'
$c = Get-Content 'data\car.csv'
if ($c[0] -notmatch '^buying') {
    @($h) + $c | Set-Content 'data\car.csv'
    Write-Host "Header added."
}
```

Hoặc đơn giản hơn, mở file và thêm dòng đầu:
```
buying,maint,doors,persons,lug_boot,safety,class
vhigh,vhigh,2,2,small,low,unacc
...
```

---

## 3. Cách Biên Dịch và Chạy

### Yêu cầu

- **Java JDK** ≥ 8 (kiểm tra: `java -version`)

### Các lệnh

```bash
# 1. Di chuyển vào thư mục dự án
cd d:\CMAR

# 2. Tạo thư mục output
mkdir out

# 3. Biên dịch tất cả file Java
javac -d out src\*.java

# 4. Chạy chương trình
java -cp out Main
```

### Output mong đợi trên màn hình

```
=== CMAR Classification Algorithm ===

[1] Loading dataset: data/car.csv
    Loaded 1728 records.

[2] Splitting data (train=80%, test=19%)
    Train: 1382  |  Test: 346

[3] Mining frequent patterns (minSupport=50)...
    Found 462 frequent patterns.
    Saved -> result/frequent_patterns.txt

[4] Generating CARs (minConfidence=0.5)...
    Generated 182 candidate rules.
    Saved -> result/association_rules.txt

[5] Training CMAR classifier...
    Rules after CMAR pruning: 35 (from 182 candidates)
    Default class: "unacc"

[6] Classifying 346 test records...
    Saved -> result/predictions.txt

[7] Evaluating...
    Test Accuracy: 0.7110 (71.10%)
    Saved -> result/evaluation.txt

=== Done ===
```

---

## 4. Cấu Hình Tham Số

Tất cả tham số được đặt trong `src/Main.java`:

```java
static final String DATASET_PATH   = "data/car.csv";   // file dữ liệu
static final double TRAIN_RATIO    = 0.8;               // 80% train / 20% test
static final int    MIN_SUPPORT    = 50;                // ngưỡng support tối thiểu
static final double MIN_CONFIDENCE = 0.5;               // ngưỡng confidence tối thiểu
static final long   RANDOM_SEED    = 42;                // hạt ngẫu nhiên (tái hiện kết quả)
```

### Hướng dẫn chọn ngưỡng

| Dataset | Số bản ghi | MIN_SUPPORT đề xuất | Ghi chú |
|---------|-----------|---------------------|---------|
| weather.csv | 14 | 2 | Dataset cực nhỏ |
| car.csv | 1728 | 50–100 | ≈ 3–6% tổng bản ghi |
| Lớn > 10k | 10000+ | 100–500 | Tăng để tránh bùng nổ luật |

- **MIN_SUPPORT quá nhỏ** → quá nhiều tập phổ biến → chậm, nhiều luật nhiễu
- **MIN_SUPPORT quá lớn** → quá ít luật → độ chính xác thấp
- **MIN_CONFIDENCE < 0.5** → nhiều luật yếu → phân lớp kém

---

## 5. Giải Thích Thuật Toán FP-Tree & FP-Growth

### 5.1 Mã hóa dữ liệu

Mỗi bản ghi CSV được chuyển thành một **danh sách item** theo dạng `"tênCột=giáTrị"`:

```
buying=vhigh, maint=vhigh, doors=2, persons=2, lug_boot=small, safety=low, class=unacc
```

Class label cũng được thêm vào cây như một item `class=...`, cho phép FP-Growth
khai thác quan hệ giữa thuộc tính và nhãn lớp trong **một lần duyệt duy nhất**.

---

### 5.2 Xây dựng FP-Tree

**FP-Tree (Frequent Pattern Tree)** là cấu trúc cây nén thông tin các giao dịch phổ biến.

#### Ví dụ minh hoạ (5 giao dịch, minSupport = 2)

| TID | Items |
|-----|-------|
| T1  | safety=high, persons=4, lug_boot=big, class=good |
| T2  | safety=high, persons=more, lug_boot=med, class=acc |
| T3  | safety=high, persons=4, lug_boot=med, class=acc |
| T4  | safety=med, persons=4, lug_boot=big, class=acc |
| T5  | safety=high, persons=4, lug_boot=big, class=good |

**Bước 1 — Đếm tần số toàn cục:**
```
safety=high  : 4   ✔ (≥2)
persons=4    : 4   ✔
lug_boot=big : 3   ✔
lug_boot=med : 2   ✔
class=good   : 2   ✔
class=acc    : 3   ✔
persons=more : 1   ✗ (loại)
safety=med   : 1   ✗ (loại)
```

**Bước 2 — Sắp xếp từng giao dịch theo tần số GIẢM DẦN:**

| TID | Path sau sắp xếp |
|-----|-----------------|
| T1  | safety=high → class=acc(3) → persons=4 → lug_boot=big |
| T2  | safety=high → class=acc → persons=4 → lug_boot=med |
| T3  | safety=high → class=acc → persons=4 → lug_boot=big |
| T4  | class=acc → persons=4 → lug_boot=big |
| T5  | safety=high → class=good → persons=4 → lug_boot=big |

**Bước 3 — Chèn từng path vào cây:**

```
root
 └── safety=high : 4
      ├── class=acc : 3   ◄── node link
      │    └── persons=4 : 3
      │         ├── lug_boot=big : 2
      │         └── lug_boot=med : 1
      └── class=good : 1
           └── persons=4 : 1
                └── lug_boot=big : 1
 └── class=acc : 1   ◄── node link (nối tiếp node trên)
      └── persons=4 : 1
           └── lug_boot=big : 1
```

**Header Table** (bảng liên kết nút):
```
Item           | freq | first node → next node → ...
---------------|------|-----------------------------
safety=high    |  4   | node(safety=high:4)
class=acc      |  3+1 | node(class=acc:3) → node(class=acc:1)
class=good     |  1   | node(class=good:1)
persons=4      |  3+1 | node(persons=4:3) → node(persons=4:1) → ...
lug_boot=big   |  2+1 | ...
lug_boot=med   |  1   | ...
```

---

### 5.3 Thuật toán FP-Growth (khai thác đệ quy)

FP-Growth khai thác tập phổ biến **không cần sinh ứng viên**. Nó hoạt động theo
nguyên tắc **chia để trị**:

```
FP-Growth(tree, prefix):
  for each item (theo thứ tự tần số TĂNG DẦN từ header table):
    1. pattern = prefix ∪ {item},  support = freq(item)
    2. Ghi nhận (pattern, support) là tập phổ biến
    3. Thu thập tất cả prefix-path đến từng nút của item (conditional pattern base)
    4. Đếm tần số item trong conditional pattern base
    5. Lọc bỏ item dưới ngưỡng → xây conditional FP-Tree
    6. Đệ quy: FP-Growth(condTree, pattern)
```

#### Ví dụ: khai thác với item `lug_boot=big`

**Conditional pattern base của `lug_boot=big`:**
```
Path từ T1+T3:  safety=high → class=acc → persons=4   (count=2)
Path từ T4:     class=acc → persons=4                  (count=1)
Path từ T5:     safety=high → class=good → persons=4   (count=1)
```

**Conditional FP-Tree** (chỉ giữ items có freq ≥ 2):
```
root
 └── persons=4 : 4   (2+1+1)
      └── class=acc : 3
           └── safety=high : 2
```

Từ cây này, các tập phổ biến được khai thác thêm:
- `{lug_boot=big, persons=4}` — sup=4
- `{lug_boot=big, persons=4, class=acc}` — sup=3
- v.v...

---

## 6. Sinh Luật Kết Hợp Lớp (CAR)

Từ tập phổ biến `P`, một **Class Association Rule (CAR)** được sinh khi:
- `P` chứa **đúng 1** item dạng `class=...`
- `P \ {class item}` là **condset** (điều kiện)
- Confidence ≥ minConfidence

**Công thức:**

$$\text{support}(P) = \frac{|\{t \in D \mid P \subseteq t\}|}{|D|}$$

$$\text{confidence}(A \Rightarrow C) = \frac{\text{support}(A \cup C)}{\text{support}(A)}$$

**Ví dụ luật từ car.csv:**
```
{safety=high, persons=4} => acc  [sup=0.18, conf=0.72]
{lug_boot=big, safety=high} => acc  [sup=0.12, conf=0.80]
```

**Thứ tự ưu tiên luật (Rule Precedence):**
1. Confidence cao hơn → ưu tiên hơn
2. Nếu bằng nhau → Support cao hơn → ưu tiên hơn
3. Nếu vẫn bằng → condset nhỏ hơn (đơn giản hơn) → ưu tiên hơn

---

## 7. Thuật Toán CMAR – Pruning & Phân Lớp

### 7.1 Pruning (Loại bỏ luật lặp)

CMAR loại bỏ luật "bị thống trị" để tránh dư thừa:

> Luật **r1 bị loại** nếu tồn tại luật **r2 có thứ tự ưu tiên cao hơn** sao cho:
> - `condset(r2) ⊆ condset(r1)`  (r2 tổng quát hơn)
> - `class(r2) = class(r1)`       (dự đoán cùng nhãn)

**Ví dụ:**
```
r2: {safety=high} => acc  [conf=0.75]   → ưu tiên cao hơn, tổng quát hơn
r1: {safety=high, persons=4} => acc  [conf=0.72]   → BỊ LOẠI
```

Kết quả: 182 luật ứng viên → **35 luật sau pruning** (trên car.csv).

---

### 7.2 Phân Lớp (Classification)

Để phân lớp một bản ghi test:

**Bước 1:** Thu thập tất cả luật khớp với bản ghi  
*(luật khớp khi `condset ⊆ items của bản ghi`)*

**Bước 2:** Nếu **không có luật nào khớp** → trả về **lớp mặc định**  
*(lớp xuất hiện nhiều nhất trong tập huấn luyện — thường là `unacc`)*

**Bước 3:** Nếu tất cả luật khớp **cùng nhãn lớp** → trả về nhãn đó

**Bước 4:** Nếu nhiều lớp → tính **Weighted Chi-Square** cho mỗi nhóm:

$$\text{score}(G) = \sum_{r \in G} \text{conf}(r) \times \chi^2(r)$$

$$\chi^2(r) = \frac{n \cdot (ad - bc)^2}{(a+b)(c+d)(a+c)(b+d)}$$

Trong đó với luật $A \Rightarrow C$:

| Ký hiệu | Ý nghĩa |
|---------|---------|
| $n$ | Tổng số bản ghi huấn luyện |
| $a$ | `supportCount(r)` — số bản ghi chứa cả A và C |
| $b$ | `condsetSupportCount − a` — chứa A nhưng không C |
| $c$ | `classFreq[C] − a` — chứa C nhưng không A |
| $d$ | `n − a − b − c` — không chứa cả A lẫn C |

**Nhãn lớp được chọn** là nhóm có `score` cao nhất.

---

### 7.3 Ví dụ phân lớp thực tế

**Bản ghi test:** `buying=low, maint=low, doors=4, persons=more, lug_boot=big, safety=high`

**Luật khớp:**
```
r1: {safety=high, persons=more} => acc  [conf=0.85, χ²=12.3]
r2: {lug_boot=big, safety=high} => acc  [conf=0.80, χ²=9.1]
r3: {buying=low} => acc              [conf=0.70, χ²=7.5]
r4: {safety=high} => unacc           [conf=0.60, χ²=4.2]
```

**Tính score:**
```
score(acc)   = 0.85×12.3 + 0.80×9.1 + 0.70×7.5 = 10.455 + 7.28 + 5.25 = 22.985
score(unacc) = 0.60×4.2  = 2.52
```

**→ Dự đoán: `acc`** ✔

---

## 8. Kết Quả Chạy Trên car.csv

| Thông số | Giá trị |
|----------|---------|
| Tổng bản ghi | 1728 |
| Tập huấn luyện | 1382 (80%) |
| Tập kiểm tra | 346 (20%) |
| MIN_SUPPORT | 50 |
| MIN_CONFIDENCE | 0.5 |
| Tập phổ biến tìm được | 462 |
| Luật CAR ứng viên | 182 |
| Luật sau CMAR pruning | 35 |
| Lớp mặc định | unacc |
| **Độ chính xác** | **71.10%** |

### Xem chi tiết kết quả

```bash
# Xem 20 luật đầu
type result\association_rules.txt

# Xem dự đoán
type result\predictions.txt

# Xem độ chính xác theo từng lớp
type result\evaluation.txt
```

### Cải thiện độ chính xác

Thử điều chỉnh trong `src/Main.java`:
```java
// Giảm MIN_SUPPORT để có nhiều luật hơn
static final int    MIN_SUPPORT    = 30;

// Tăng MIN_CONFIDENCE để lọc luật mạnh hơn
static final double MIN_CONFIDENCE = 0.6;
```

Sau khi chỉnh tham số, biên dịch lại và chạy:
```bash
javac -d out src\*.java && java -cp out Main
```

---

## Sơ Đồ Pipeline Tổng Quan

```
car.csv
   │
   ▼
DatasetLoader.load()
   │  "buying=vhigh" "maint=vhigh" ... "class=unacc"
   ▼
FPGrowth.mine()           ← Xây FP-Tree + đệ quy khai thác
   │  462 tập phổ biến
   ▼
RuleGenerator.generate()  ← Lọc các pattern có "class=..." → CAR
   │  182 luật ứng viên
   ▼
CMARClassifier.train()    ← Pruning luật thống trị
   │  35 luật sau pruning
   ▼
CMARClassifier.predict()  ← Weighted Chi-Square classification
   │  346 dự đoán
   ▼
ResultWriter              ← Ghi ra result/*.txt
```
