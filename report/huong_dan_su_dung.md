# Hướng Dẫn Sử Dụng CMAR — Chi Tiết Từng File

---

## 1. Các File Chạy Chính (Entry Points)

Project có **3 file chạy chính** — bạn chỉ cần dùng 1 trong 3 tuỳ mục đích:

### 1.1 `Main.java` — Chạy 1 dataset, train/test split 80/20

**Khi nào dùng:** Muốn test nhanh 1 dataset, xem kết quả, xem cây FP-tree, xem luật.

```bash
# Build trước
javac -d out src/*.java

# Chạy cơ bản (dùng tham số mặc định trong code)
java -cp out Main

# Chạy với tham số từ dòng lệnh
java -cp out Main <file.csv> [minSupport] [minConfidence] [chiSqThreshold] [coverageDelta]
```

**Ví dụ cụ thể:**

```bash
# Dataset weather nhỏ, minSup=2 (để có đủ luật)
java -cp out Main data/weather.csv 2 0.5 3.841 4

# Dataset car (1728 records), minSup=50
java -cp out Main data/car.csv 50 0.5 3.841 4

# Dataset iris (150 records), minSup=2 (1% của 150)
java -cp out Main data/iris_disc.csv 2 0.5 3.841 4

# Dataset breast-w (699 records), minSup=7 (1% của 699)
java -cp out Main data/breast-w.csv 7 0.5 3.841 4
```

**Output:** Tạo 6 file trong `result/` và `report/`:
- `result/frequent_patterns.txt` — tất cả frequent patterns
- `result/association_rules.txt` — CARs trước pruning
- `result/predictions.txt` — dự đoán từng record test
- `result/evaluation.txt` — precision/recall/F1/accuracy
- `result/fpgrowth_result.txt` — báo cáo FP-Growth (cây, header table)
- `result/cmar_result.txt` — báo cáo CMAR đầy đủ (3 pruning, luật cuối)
- `report/fp_tree_report.md` — báo cáo tổng hợp Markdown

---

### 1.2 `Benchmark.java` — Chạy tất cả 20 datasets (10-fold CV)

**Khi nào dùng:** Muốn lặp lại Table 1 của paper, so sánh với CMAR/CBA/C4.5.

```bash
java -Xmx4g -cp out Benchmark
```

**Không có tham số dòng lệnh.** Tham số từng dataset được cấu hình sẵn trong mảng `DATASETS[]`:

```java
// Trong Benchmark.java dòng 15-36
{ "data/breast-w.csv", "breast-w", "0.01", "96.42", "96.28", "95.00" },
//   file CSV             tên      supPct   paper    CBA     C4.5
```

**Để thay đổi tham số:** Sửa trực tiếp trong `Benchmark.java`, build lại.

**Output:** In ra stdout — kết quả từng dataset + tổng kết cuối.

---

### 1.3 `BenchmarkOne.java` — Chạy 1 dataset với 10-fold CV

**Khi nào dùng:** Muốn test 10-fold CV cho 1 dataset cụ thể (chính xác hơn Main).

```bash
java -cp out BenchmarkOne <file> <name> <supPct> <paperCMAR> <paperCBA> <paperC45> [maxPatternLength]
```

**Ví dụ:**

```bash
# Test breast-w với supPct=1%, so với paper
java -cp out BenchmarkOne data/breast-w.csv breast-w 0.01 96.42 96.28 95.00

# Test glass với supPct=1%
java -cp out BenchmarkOne data/glass.csv glass 0.01 70.09 67.76 68.22

# Test sonar (60 thuộc tính) với maxPatternLength=6 để tránh bùng nổ
java -cp out BenchmarkOne data/sonar.csv sonar 0.05 79.33 76.92 73.56 6
```

---

## 2. Các File Tiện Ích (Preprocessors) — Khi Nào Chạy

**BẠN KHÔNG CẦN CHẠY CÁC FILE NÀY** nếu dùng `data/` có sẵn. Chúng chỉ cần chạy **1 lần** khi
cần tạo lại file CSV từ dữ liệu gốc UCI.

### 2.1 `FixHorse.java` — Sửa horse.csv

**Vấn đề:** UCI Horse Colic có 27 thuộc tính + 3 cột `lesion_type1/2/3` là mã số (11300, 2209...) không phải categorical — gây nhiễu.

**Làm gì:**
- Xoá 3 cột `lesion_type1/2/3`
- Thay `?` (missing) bằng mode (giá trị phổ biến nhất) của cột đó
- Xoá record có class = `?`

```bash
# Chỉ chạy khi cần tạo lại horse.csv từ bản gốc
java -cp out FixHorse
```

### 2.2 `FixHorse2.java` — Sửa horse.csv (phiên bản khác)

**Khác FixHorse:** Giữ `?` nguyên (không impute) — `DatasetLoader` sẽ tự bỏ qua. Xoá cột có >40% missing.

```bash
java -cp out FixHorse2
```

### 2.3 `GermanPreprocessor.java` — Tạo german_disc.csv

**Vấn đề:** UCI German Credit gốc là file space-separated, không header, có 7 thuộc tính số (duration, amount, age...).

**Làm gì:**
- Đọc `german.csv` (gốc, space-separated)
- Thêm header
- Discretize 7 thuộc tính số thành categories (Short/Medium/Long, Young/Middle/Senior...)
- Ghi ra `german_disc.csv` (comma-separated)

```bash
java -cp out GermanPreprocessor data/german.csv data/german_disc.csv
```

### 2.4 `DiscretizeGlass.java` — Tạo glass.csv

**Vấn đề:** UCI Glass gốc (`glass.data`) có 9 thuộc tính số liên tục (RI, Na, Mg...).

**Làm gì:**
- Đọc `glass.data`
- Discretize bằng equal-frequency bins (VL/L/M/H/VH). Cột có nhiều zero thì tách bin "zero".
- Ghi ra `glass.csv`

```bash
java -cp out DiscretizeGlass
```

---

## 3. Cách Chọn minSupport Cho Chuẩn

Đây là **câu hỏi quan trọng nhất**. Paper nói dùng 1%, nhưng thực tế phải tinh chỉnh:

### 3.1 Quy tắc cơ bản

```
minSupport = trainSize × supPct

Ví dụ: 699 records × 80% train = 559 train × 1% = 5.59 → minSup = 6
```

### 3.2 Bảng chọn nhanh theo số lượng thuộc tính

| Số thuộc tính | minSup khuyến nghị | Lý do |
|---------------|---------------------|-------|
| 1 – 10 | **1%** (theo paper) | Ít thuộc tính → ít pattern → chạy nhanh |
| 11 – 15 | **1%** (theo paper) | Vẫn ok |
| 16 – 20 | **3 – 5%** | Bắt đầu bùng nổ combinatorial |
| 21 – 30 | **5%** | Cần cao hơn |
| 30+ (sonar 60) | **5 – 7%** | Pure Java không đủ nhanh cho 1% |

### 3.3 Dấu hiệu minSup quá cao (cần hạ xuống)

- **Quá ít luật** (dưới 5 luật sau pruning) → test accuracy thấp, mặc định class chiếm đa số
- **Accuracy = tỷ lệ default class** → classifier không học được gì
- **"Found 0 frequent patterns"** → minSup cao hơn số record → không có pattern nào

### 3.4 Dấu hiệu minSup quá thấp (cần tăng lên)

- **Java hết memory** (OutOfMemoryError) hoặc chạy rất lâu (>5 phút/fold)
- **Số patterns > 1 triệu** → FP-tree quá lớn
- **Số candidate rules > 500,000** → pruning chậm

### 3.5 Cách tinh chỉnh tốt nhất

```bash
# Bước 1: Bắt đầu với 1% (paper default)
java -cp out Main data/my_data.csv <1% của N> 0.5

# Bước 2: Nếu quá chậm / OOM → tăng lên 2%, 3%, 5%
java -cp out Main data/my_data.csv <3% của N> 0.5

# Bước 3: Nếu quá ít luật → hạ xuống 0.5% hoặc thấp hơn
java -cp out Main data/my_data.csv <0.5% của N> 0.5
```

**Ví dụ cụ thể:**

```bash
# Dataset 500 records, 8 thuộc tính → minSup = 500 × 1% = 5
java -cp out Main data/my_data.csv 5 0.5

# Dataset 200 records, 25 thuộc tính → minSup = 200 × 3% = 6
java -cp out Main data/my_data.csv 6 0.5

# Dataset 10000 records, 10 thuộc tính → minSup = 10000 × 1% = 100
java -cp out Main data/my_data.csv 100 0.5
```

---

## 4. Cách Bỏ Dữ Liệu Mới Vào

### 4.1 Yêu cầu file CSV

```csv
col1,col2,col3,...,class
val1,val2,val3,...,label_a
val1,val2,val3,...,label_b
```

**Quy tắc:**
- Dòng 1 = **header** (tên cột)
- **Cột cuối cùng** = class label (nhãn phân lớp)
- Các cột khác = thuộc tính
- Giá trị thiếu ghi `?` → DatasetLoader tự động bỏ qua
- **Phân tách bằng dấu phẩy `,`**
- **Không có khoảng trắng thừa** trước/sau dấu phẩy (strip tự động)

### 4.2 Dữ liệu danh mục (categorical) — Dùng ngay

```csv
outlook,temperature,humidity,windy,play
sunny,hot,high,false,no
overcast,hot,high,false,yes
rainy,mild,high,false,yes
```

→ Tự động tạo items: `outlook=sunny`, `temperature=hot`, v.v.

### 4.3 Dữ liệu số (numerical) — Phải discretize trước

**CMAR chỉ làm việc với dữ liệu categorical.** Nếu có số liên tục phải chuyển:

**Cách 1: Tự làm trong Excel/Python**

```python
# Ví dụ: chia tuổi thành 3 nhóm
import pandas as pd
df = pd.read_csv("data.csv")
df["age"] = pd.cut(df["age"], bins=3, labels=["Young","Middle","Old"])
df.to_csv("data_disc.csv", index=False)
```

**Cách 2: Viết Java tương tự GermanPreprocessor.java**

Xem `GermanPreprocessor.java` làm ví dụ — nó đọc file gốc, chia số thành bins, ghi ra CSV mới.

### 4.4 Ví dụ toàn bộ quy trình

```bash
# 1. Đặt file CSV đúng format vào data/
cp /path/to/my_dataset.csv data/my_dataset.csv

# 2. Xem số dòng để tính minSup
wc -l data/my_dataset.csv
# → 501 (500 records + 1 header)
# minSup = 500 × 1% = 5

# 3. Build
javac -d out src/*.java

# 4. Chạy thử nhanh
java -cp out Main data/my_dataset.csv 5 0.5 3.841 4

# 5. Xem kết quả
cat result/cmar_result.txt

# 6. Nếu ok → chạy 10-fold CV chính xác hơn
java -cp out BenchmarkOne data/my_dataset.csv my_dataset 0.01 0 0 0
# (0 0 0 = không có paper để so)
```

---

## 5. Giải Thích Ý Nghĩa 5 Tham Số

| Tham số | Mặc định | Ý nghĩa | Tăng lên thì... | Hạ xuống thì... |
|---------|----------|---------|-----------------|-----------------|
| `minSupport` | 1% của N | Lượng giao dịch tối thiểu để 1 pattern được coi là "phổ biến" | Ít pattern hơn, chạy nhanh, nhưng bỏ sót luật hiếm | Nhiều pattern, chạy chậm, nhưng bắt được luật hiếm |
| `minConfidence` | 0.5 | Độ chính xác tối thiểu của luật (P(class\|condset)) | Luật chính xác hơn nhưng ít hơn | Nhiều luật nhưng có thể sai |
| `chiSqThreshold` | 3.841 | Ngưỡng ý nghĩa thống kê (p=0.05, df=1) | Lọc nghiêm hơn, giữ luật có tương quan mạnh | Cho nhiều luật qua, kể cả tương quan yếu |
| `coverageDelta` | 4 | Số luật tối đa cover 1 record trước khi xoá nó khỏi training | Bỏ luật sớm hơn → ít luật cuối | Giữ nhiều luật, có thể có luật thừa |

**Lời khuyên:**
- `minConfidence = 0.5` và `chiSqThreshold = 3.841` và `coverageDelta = 4` **giữ nguyên** (đúng paper).
- Chỉ tinh chỉnh **`minSupport`** theo bảng ở mục 3.2.

---

## 6. Cấu Trúc File Trong Project

```
CMAR-main/
├── data/                          # ← Dữ liệu CSV
│   ├── weather.csv                    # Dataset nhỏ (14 records) — để test
│   ├── car.csv                        # 1728 records, 6 thuộc tính
│   ├── breast-w.csv                   # 699 records, 9 thuộc tính
│   ├── iris_disc.csv                  # 150 records, 4 thuộc tính (đã discretize)
│   ├── glass.csv                      # 214 records, 9 thuộc tính (đã discretize)
│   ├── german_disc.csv                # 1000 records, 20 thuộc tính (đã discretize)
│   ├── mushroom_full.csv              # 8124 records, 22 thuộc tính
│   └── ...                            # 20 datasets UCI tổng cộng
│
├── src/                           # ← Source code
│   ├── Main.java                      # CHẠY 1 — train/test 80/20
│   ├── Benchmark.java                 # CHẠY 2 — 20 datasets × 10-fold
│   ├── BenchmarkOne.java              # CHẠY 3 — 1 dataset × 10-fold
│   │
│   ├── DatasetLoader.java             # Đọc CSV → List<Transaction>
│   ├── Transaction.java               # 1 record = items + class label
│   │
│   ├── FPNode.java                    # Node cây CR-tree (count + classCount)
│   ├── FPTree.java                    # Cây CR-tree
│   ├── FPGrowth.java                  # Mining: phát CARs trực tiếp
│   ├── FrequentPattern.java           # Pattern (cho báo cáo)
│   │
│   ├── AssociationRule.java           # Luật P → class (support, confidence)
│   ├── CRTree.java                    # Cây lưu luật, tìm kiếm nhanh
│   ├── CMARClassifier.java            # 3 pruning + weighted chi-square
│   ├── CrossValidator.java            # Stratified 10-fold CV
│   │
│   ├── RuleGenerator.java             # (Cũ, không dùng nữa)
│   ├── ResultWriter.java              # Xuất kết quả ra file
│   │
│   ├── GermanPreprocessor.java        # Tiền xử lý german.csv → german_disc.csv
│   ├── DiscretizeGlass.java           # Tiền xử lý glass.data → glass.csv
│   ├── FixHorse.java                  # Sửa horse.csv (impute missing)
│   └── FixHorse2.java                 # Sửa horse.csv (xoá cột nhiều missing)
│
├── result/                        # ← Kết quả (tự động tạo khi chạy)
│   ├── frequent_patterns.txt
│   ├── association_rules.txt
│   ├── predictions.txt
│   ├── evaluation.txt
│   ├── fpgrowth_result.txt
│   └── cmar_result.txt
│
├── report/                        # ← Báo cáo
│   ├── fp_tree_report.md              # Báo cáo FP-tree Markdown
│   ├── benchmark_results.md           # Kết quả 20 datasets
│   └── huong_dan_su_dung.md           # File này
│
└── out/                           # ← Class files (tự động tạo khi build)
```

---

## 7. Quick Start — Từ Đầu Đến Cuối

```bash
# ======= BƯỚC 1: Build =======
cd CMAR-main
mkdir -p out
javac -d out src/*.java

# ======= BƯỚC 2: Test nhanh với dataset nhỏ =======
java -cp out Main data/weather.csv 2 0.5 3.841 4
# → Xem result/cmar_result.txt

# ======= BƯỚC 3: Chạy dataset thực =======
java -cp out Main data/breast-w.csv 7 0.5 3.841 4
# → Accuracy khoảng 95%

# ======= BƯỚC 4: Muốn chính xác hơn → 10-fold CV =======
java -cp out BenchmarkOne data/breast-w.csv breast-w 0.01 96.42 96.28 95.00
# → Accuracy khoảng 94.13% (so paper 96.42%)

# ======= BƯỚC 5: Chạy toàn bộ 20 datasets =======
java -Xmx4g -cp out Benchmark
# → Mất khoảng 20-30 phút
```

---

## 8. Câu Hỏi Thường Gặp (FAQ)

### Q: Main.java và BenchmarkOne.java khác gì?

| | Main.java | BenchmarkOne.java |
|---|-----------|-------------------|
| **Chia dữ liệu** | 80% train / 20% test | **10-fold CV** (chính xác hơn) |
| **Tham số** | minSup tuyệt đối (ví dụ: 5) | minSup theo % (ví dụ: 0.01 = 1%) |
| **Output** | File chi tiết trong result/ | 1 dòng kết quả stdout |
| **Khi nào dùng** | Xem cây, xem luật, debug | Đánh giá chính xác accuracy |

### Q: Tại sao có FixHorse.java và FixHorse2.java?

- `FixHorse.java` — cách 1: thay `?` bằng mode (giá trị phổ biến nhất)
- `FixHorse2.java` — cách 2: giữ `?` nguyên, chỉ xoá cột có quá nhiều missing (>40%)

Cả 2 đều đã được chạy sẵn → `data/horse.csv` hiện tại là kết quả. **Không cần chạy lại.**

### Q: minSupport nên là tuyệt đối hay phần trăm?

- `Main.java` nhận **tuyệt đối** (ví dụ: `5` tức là 5 transactions)
- `BenchmarkOne.java` và `Benchmark.java` nhận **phần trăm** (ví dụ: `0.01` = 1%)
- Quy đổi: `minSup_tuyệt_đối = trainSize × phần_trăm`

### Q: Sao dataset của tôi chạy OOM?

```bash
# Tăng Java heap:
java -Xmx4g -cp out Main data/big_data.csv 50 0.5
#         ^^^^ tăng lên 4GB, 8GB nếu cần

# Hoặc tăng minSup:
java -cp out Main data/big_data.csv 100 0.5   # từ 50 lên 100
```

### Q: Dataset của tôi có dấu tiếng Việt (UTF-8)?

Thêm `-Dfile.encoding=UTF-8`:

```bash
java -Dfile.encoding=UTF-8 -cp out Main data/vn_data.csv 5 0.5
```
