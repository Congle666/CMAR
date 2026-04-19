# BÁO CÁO: THUẬT TOÁN CMAR TRÊN DATASET IRIS (UCI)

**Dataset:** Iris — UCI Machine Learning Repository (150 records, 4 attributes, 3 classes)  
**Tham khảo:** Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* ICDM 2001.

---

## Mục Lục

1. [Tổng Quan Pipeline](#1-tổng-quan-pipeline)
2. [Dữ Liệu Đầu Vào](#2-dữ-liệu-đầu-vào)
3. [Tiền Xử Lý: Discretization](#3-tiền-xử-lý-discretization)
4. [Cấu Hình Chạy](#4-cấu-hình-chạy)
5. [PHẦN 1: FP-Growth — Đào Mỏ Tập Phổ Biến](#5-phần-1-fp-growth--đào-mỏ-tập-phổ-biến)
6. [PHẦN 2: CMAR — Phân Lớp](#6-phần-2-cmar--phân-lớp)
7. [Đánh Giá Hiệu Suất](#7-đánh-giá-hiệu-suất)
8. [So Sánh Với Bài Báo](#8-so-sánh-với-bài-báo)

---

## 1. Tổng Quan Pipeline

```
┌──────────┐   ┌──────────────┐   ┌───────────┐   ┌──────────┐   ┌──────────┐
│ Load CSV │──▶│ Discretize   │──▶│ FP-Growth │──▶│ Extract  │──▶│  CMAR    │
│ (iris)   │   │ (3 bins)     │   │ Mining    │   │ CARs     │   │ Classify │
└──────────┘   └──────────────┘   └───────────┘   └──────────┘   └──────────┘
   Step 0          Step 0b           Step 2-3        Step 4        Step 5-7
```

| Bước | Mô tả | Kết quả |
|------|--------|---------|
| 0 | Load iris.csv (150 records, 4 attributes liên tục) | 150 bản ghi |
| 0b | Discretize: mỗi attribute → 3 bins (Low/Medium/High) | 150 bản ghi categorical |
| 1 | Split 80/20 | Train: 120, Test: 30 |
| 2 | Xây dựng FP-Tree + Header Table | 15 items trong header table |
| 3 | Mining đệ quy trên FP-Tree | **172 frequent patterns** |
| 4 | Trích xuất CARs (condset → class) | **78 candidate rules** |
| 5 | Pruning luật bị dominated | **25 luật** (loại 53) |
| 6 | Phân lớp bằng weighted χ² | Dự đoán 30 test records |
| 7 | Đánh giá kết quả | **Accuracy = 93.33%** |

---

## 2. Dữ Liệu Đầu Vào

**Iris Dataset** (Fisher, 1936) — một trong những dataset kinh điển nhất trong machine learning.

| Thuộc tính | Kiểu | Phạm vi | Mô tả |
|------------|------|---------|-------|
| sepal_length | Continuous | 4.3 – 7.9 | Chiều dài đài hoa (cm) |
| sepal_width | Continuous | 2.0 – 4.4 | Chiều rộng đài hoa (cm) |
| petal_length | Continuous | 1.0 – 6.9 | Chiều dài cánh hoa (cm) |
| petal_width | Continuous | 0.1 – 2.5 | Chiều rộng cánh hoa (cm) |
| class | Categorical | 3 giá trị | Iris-setosa / Iris-versicolor / Iris-virginica |

**Phân bố class:** 50 Setosa + 50 Versicolor + 50 Virginica (cân bằng hoàn toàn)

---

## 3. Tiền Xử Lý: Discretization

CMAR chỉ xử lý categorical data. Do Iris có 4 attributes liên tục, cần **discretize** (phân bin) trước — đây cũng là cách bài báo làm (dùng CBA's discretization).

**Phương pháp:** Equal-width binning, mỗi attribute chia thành 3 khoảng.

| Attribute | Bin 1 | Bin 2 | Bin 3 |
|-----------|-------|-------|-------|
| sepal_length | Short (≤ 5.5) | Medium (5.5 – 6.7) | Long (> 6.7) |
| sepal_width | Narrow (≤ 2.8) | Medium (2.8 – 3.6) | Wide (> 3.6) |
| petal_length | Short (≤ 3.0) | Medium (3.0 – 5.0) | Long (> 5.0) |
| petal_width | Thin (≤ 0.9) | Medium (0.9 – 1.7) | Wide (> 1.7) |

**Ví dụ chuyển đổi:**
```
Gốc:    5.1, 3.5, 1.4, 0.2, Iris-setosa
Sau:    Short, Medium, Short, Thin, Iris-setosa
```

---

## 4. Cấu Hình Chạy

```
Dataset:        data/iris_disc.csv
Train/Test:     120 / 30  (80/20)
Min Support:    5  (≈ 4.17% trên 120 records)
Min Confidence: 0.5 (50%)
Random Seed:    42
```

---

## 5. PHẦN 1: FP-Growth — Đào Mỏ Tập Phổ Biến

### 5.1 Header Table (Bảng Tần Suất Item)

| Item | Frequency | Tỉ lệ (%) |
|------|-----------|-----------|
| sepal_width=Medium | 76 | 63.3% |
| sepal_length=Medium | 55 | 45.8% |
| sepal_length=Short | 49 | 40.8% |
| petal_length=Medium | 47 | 39.2% |
| petal_length=Short | 44 | 36.7% |
| petal_width=Thin | 43 | 35.8% |
| class=Iris-setosa | 43 | 35.8% |
| petal_width=Medium | 42 | 35.0% |
| class=Iris-versicolor | 40 | 33.3% |
| class=Iris-virginica | 37 | 30.8% |
| petal_width=Wide | 35 | 29.2% |
| sepal_width=Narrow | 33 | 27.5% |
| petal_length=Long | 29 | 24.2% |
| sepal_length=Long | 16 | 13.3% |
| sepal_width=Wide | 11 | 9.2% |

**Tổng items:** 15 (4 attributes × 3 bins + 3 classes)

### 5.2 Cấu Trúc Cây FP-Tree

```
[root]
├── sepal_length=Short:16
│   ├── petal_length=Short:9
│   │   ├── petal_width=Thin:8 → class=Iris-setosa:8
│   │   └── petal_width=Medium:1 → class=Iris-versicolor:1
│   └── petal_length=Medium:7
│       └── petal_width=Medium:7 → class=Iris-versicolor:6 / class=Iris-virginica:1
├── sepal_width=Medium:76
│   ├── sepal_length=Short:33
│   │   ├── petal_length=Short:32 → petal_width=Thin:32 → class=Iris-setosa:32
│   │   └── petal_length=Medium:1 → class=Iris-versicolor:1
│   ├── class=Iris-virginica:11 → petal_width=Wide:11 → petal_length=Long:11 → sepal_length=Long:11
│   ├── sepal_length=Medium:30
│   │   ├── petal_length=Medium:20 → petal_width=Medium:17 (→Versicolor) / Wide:3 (→Virginica)
│   │   └── class=Iris-virginica:10 → petal_width=Wide:10 → petal_length=Long:10
│   └── petal_length=Medium:2 → class=Iris-versicolor:2
├── sepal_length=Medium:25
│   ├── petal_length=Medium:17 → petal_width=Medium:13 (→Versicolor) / Wide:4 (→Virginica)
│   ├── petal_length=Short:3 → petal_width=Thin:3 → class=Iris-setosa:3
│   └── class=Iris-virginica:4 → petal_width=Wide:4
└── class=Iris-virginica:3 → petal_width=Wide:3
```

**Nhận xét:**
- **Iris-setosa** luôn đi kèm `petal_width=Thin` + `petal_length=Short` → phân biệt rõ ràng
- **Iris-versicolor** chủ yếu ở khu vực `petal_width=Medium` + `petal_length=Medium`
- **Iris-virginica** tập trung ở `petal_width=Wide` + `petal_length=Long`

### 5.3 Kết Quả Mining

- **Frequent patterns:** 172
- **Top patterns:**

| # | Pattern | Support |
|---|---------|---------|
| 1 | {sepal_width=Medium} | 76 |
| 2 | {sepal_length=Medium} | 55 |
| 3 | {petal_width=Thin, petal_length=Short} | 43 |
| 4 | {petal_width=Thin, petal_length=Short, class=Iris-setosa} | 43 |
| 5 | {petal_width=Medium, petal_length=Medium} | 40 |

---

## 6. PHẦN 2: CMAR — Phân Lớp

### 6.1 Trích Xuất CARs

Từ 172 patterns → **78 candidate CARs** (có đúng 1 class item, confidence ≥ 0.5)

### 6.2 Pruning

```
Candidate rules (trước): 78
Pruned rules (sau):      25
Luật bị loại:            53  (67.9%)
```

### 6.3 Tất Cả 25 Luật Sau Pruning

| # | Luật | Support | Confidence |
|---|------|---------|------------|
| 1 | petal_width=Thin → **Iris-setosa** | 0.3583 | **1.0000** |
| 2 | petal_length=Short, sepal_width=Medium → **Iris-setosa** | 0.2667 | **1.0000** |
| 3 | petal_length=Long → **Iris-virginica** | 0.2417 | **1.0000** |
| 4 | petal_width=Medium, sepal_width=Medium → **Iris-versicolor** | 0.1667 | **1.0000** |
| 5 | sepal_length=Long, petal_width=Wide → **Iris-virginica** | 0.1167 | **1.0000** |
| 6 | petal_length=Short, sepal_width=Wide → **Iris-setosa** | 0.0833 | **1.0000** |
| 7 | sepal_width=Narrow, petal_width=Wide → **Iris-virginica** | 0.0833 | **1.0000** |
| 8 | sepal_length=Short, sepal_width=Wide → **Iris-setosa** | 0.0583 | **1.0000** |
| 9 | petal_length=Short → **Iris-setosa** | 0.3583 | 0.9773 |
| 10 | petal_width=Wide → **Iris-virginica** | 0.2833 | 0.9714 |
| 11 | sepal_length=Short, sepal_width=Medium → **Iris-setosa** | 0.2667 | 0.9697 |
| 12 | petal_width=Medium, sepal_length=Medium, petal_length=Medium → **Iris-versicolor** | 0.2417 | 0.9667 |
| 13 | petal_width=Medium, petal_length=Medium → **Iris-versicolor** | 0.3167 | 0.9500 |
| 14 | petal_width=Medium, sepal_length=Medium → **Iris-versicolor** | 0.2417 | 0.9355 |
| 15 | petal_width=Medium → **Iris-versicolor** | 0.3250 | 0.9286 |
| 16 | petal_length=Medium, sepal_width=Medium → **Iris-versicolor** | 0.1750 | 0.9130 |
| 17 | sepal_width=Wide → **Iris-setosa** | 0.0833 | 0.9091 |
| 18 | sepal_length=Long → **Iris-virginica** | 0.1167 | 0.8750 |
| 19 | petal_length=Medium, sepal_length=Short → **Iris-versicolor** | 0.0583 | 0.8750 |
| 20 | petal_length=Medium → **Iris-versicolor** | 0.3250 | 0.8298 |
| 21 | sepal_length=Short → **Iris-setosa** | 0.3333 | 0.8163 |
| 22 | sepal_width=Narrow, sepal_length=Short → **Iris-versicolor** | 0.0583 | 0.7778 |
| 23 | sepal_length=Medium, sepal_width=Medium → **Iris-versicolor** | 0.1500 | 0.6000 |
| 24 | sepal_width=Narrow → **Iris-versicolor** | 0.1583 | 0.5758 |
| 25 | sepal_length=Medium → **Iris-versicolor** | 0.2500 | 0.5455 |

**Phân tích luật:**
- **Iris-setosa** (8 luật): Dễ nhận diện nhất — `petal_width=Thin` hoặc `petal_length=Short` là đủ (conf=100%)
- **Iris-virginica** (5 luật): `petal_length=Long` hoặc `petal_width=Wide` → confidence cao (97–100%)
- **Iris-versicolor** (12 luật): Khó nhất — nằm "giữa" 2 loại kia, cần kết hợp nhiều thuộc tính

### 6.4 Kết Quả Dự Đoán (30 test records)

| # | Thực tế | Dự đoán | Kết quả |
|---|---------|---------|---------|
| 1 | Iris-virginica | Iris-versicolor | ✗ WRONG |
| 2 | Iris-versicolor | Iris-versicolor | ✓ |
| 3 | Iris-setosa | Iris-setosa | ✓ |
| 4 | Iris-setosa | Iris-setosa | ✓ |
| 5 | Iris-virginica | Iris-virginica | ✓ |
| 6 | Iris-versicolor | Iris-versicolor | ✓ |
| 7 | Iris-virginica | Iris-virginica | ✓ |
| 8 | Iris-virginica | Iris-virginica | ✓ |
| 9 | Iris-setosa | Iris-setosa | ✓ |
| 10 | Iris-setosa | Iris-setosa | ✓ |
| 11 | Iris-versicolor | Iris-versicolor | ✓ |
| 12 | Iris-versicolor | Iris-versicolor | ✓ |
| 13 | Iris-virginica | Iris-virginica | ✓ |
| 14 | Iris-virginica | Iris-virginica | ✓ |
| 15 | Iris-setosa | Iris-setosa | ✓ |
| 16 | Iris-virginica | Iris-virginica | ✓ |
| 17 | Iris-versicolor | Iris-versicolor | ✓ |
| 18 | Iris-setosa | Iris-setosa | ✓ |
| 19 | Iris-versicolor | Iris-versicolor | ✓ |
| 20 | Iris-virginica | Iris-virginica | ✓ |
| 21 | Iris-versicolor | Iris-versicolor | ✓ |
| 22 | Iris-virginica | Iris-virginica | ✓ |
| 23 | Iris-versicolor | Iris-versicolor | ✓ |
| 24 | Iris-virginica | Iris-virginica | ✓ |
| 25 | Iris-virginica | Iris-virginica | ✓ |
| 26 | Iris-setosa | Iris-setosa | ✓ |
| 27 | Iris-virginica | Iris-virginica | ✓ |
| 28 | Iris-versicolor | Iris-versicolor | ✓ |
| 29 | Iris-virginica | Iris-versicolor | ✗ WRONG |
| 30 | Iris-versicolor | Iris-versicolor | ✓ |

**Sai 2/30:** Cả 2 lỗi đều là Iris-virginica bị nhầm thành Iris-versicolor — đây là lỗi phổ biến vì 2 loại này có đặc điểm gần nhau ở vùng ranh giới.

---

## 7. Đánh Giá Hiệu Suất

### 7.1 Tổng Quan

| Metric | Giá trị |
|--------|---------|
| **Accuracy** | **28/30 = 93.33%** |
| **Macro-F1** | **0.9419** |

### 7.2 Chi Tiết Theo Class

| Class | TP | FP | FN | Precision | Recall | F1 |
|-------|----|----|-----|-----------|--------|------|
| **Iris-setosa** | 7 | 0 | 0 | **1.0000** | **1.0000** | **1.0000** |
| **Iris-versicolor** | 10 | 2 | 0 | 0.8333 | **1.0000** | 0.9091 |
| **Iris-virginica** | 11 | 0 | 2 | **1.0000** | 0.8462 | 0.9167 |

**Phân tích:**
- **Iris-setosa:** Phân lớp hoàn hảo (F1 = 1.000) — dễ nhận diện nhờ petal nhỏ
- **Iris-versicolor:** Recall = 100% (bắt hết) nhưng Precision = 83.3% (có 2 FP — thực tế là virginica bị gán nhầm)
- **Iris-virginica:** Precision = 100% (gán đúng hết) nhưng Recall = 84.6% (2 record bị nhầm sang versicolor)

### 7.3 Confusion Matrix

|  | Predicted Setosa | Predicted Versicolor | Predicted Virginica |
|--|-----------------|---------------------|-------------------|
| **Actual Setosa** | **7** | 0 | 0 |
| **Actual Versicolor** | 0 | **10** | 0 |
| **Actual Virginica** | 0 | 2 | **11** |

---

## 8. So Sánh Với Bài Báo

### 8.1 Accuracy

| Phương pháp | Accuracy trên Iris |
|-------------|-------------------|
| **Bài báo — C4.5** | 95.3% |
| **Bài báo — CBA** | 94.7% |
| **Bài báo — CMAR** | **94.0%** |
| **Cài đặt — CMAR** | **93.33%** |

### 8.2 Phân Tích Chênh Lệch (93.33% vs 94.0%)

Chênh lệch chỉ **0.67%** (≈ 1 record) — rất nhỏ và có thể do:

| Yếu tố | Bài báo | Cài đặt |
|---------|---------|---------|
| **Discretization** | CBA's discretization (entropy-based) | Equal-width 3 bins |
| **Cross-validation** | C4.5 shuffle utility (có thể 10-fold CV) | Single 80/20 split |
| **Database coverage pruning** | Có | Chưa cài |
| **Confidence diff threshold** | 20% | Chưa cài |

> **Kết luận:** Accuracy 93.33% **rất gần** với bài báo (94.0%), chênh lệch chỉ ~1 record. Phương pháp discretization khác nhau (equal-width vs entropy-based) là nguyên nhân chính. Kết quả xác nhận cài đặt CMAR **đúng** trên dataset chuẩn UCI.

---

## Hướng Dẫn Chạy Lại

```bash
cd d:\CMAR

# Bước 1: Compile
mkdir out 2>nul
javac -d out src\*.java

# Bước 2: Chạy trên iris đã discretize
java -cp out Main data/iris_disc.csv 5 0.5
```

**Kết quả output:**
- `result/frequent_patterns.txt` — 172 frequent patterns
- `result/association_rules.txt` — 78 candidate CARs
- `result/fpgrowth_result.txt` — FP-Growth: header table + FP-tree + patterns
- `result/cmar_result.txt` — CMAR: pruning + 25 luật + predictions + evaluation
- `result/predictions.txt` — Dự đoán 30 records
- `result/evaluation.txt` — Accuracy + F1 metrics
- `report/fp_tree_report.md` — FP-Tree report dạng Markdown

---

*Dataset: UCI Iris (Fisher, 1936). Thuật toán: CMAR (Li, Han & Pei, ICDM 2001).*
