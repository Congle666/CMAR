# BÁO CÁO: THUẬT TOÁN CMAR TRÊN DATASET ZOO (UCI)

**Dataset:** Zoo — UCI Machine Learning Repository (101 records, 16 attributes, 7 classes)  
**Tham khảo:** Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* ICDM 2001.

---

## Mục Lục

1. [Tổng Quan Pipeline](#1-tổng-quan-pipeline)
2. [Dữ Liệu Đầu Vào](#2-dữ-liệu-đầu-vào)
3. [Tiền Xử Lý](#3-tiền-xử-lý)
4. [Cấu Hình Chạy](#4-cấu-hình-chạy)
5. [PHẦN 1: FP-Growth — Đào Mỏ Tập Phổ Biến](#5-phần-1-fp-growth--đào-mỏ-tập-phổ-biến)
6. [PHẦN 2: CMAR — Phân Lớp](#6-phần-2-cmar--phân-lớp)
7. [Đánh Giá Hiệu Suất](#7-đánh-giá-hiệu-suất)
8. [So Sánh Với Bài Báo](#8-so-sánh-với-bài-báo)

---

## 1. Tổng Quan Pipeline

| Bước | Mô tả | Kết quả |
|------|--------|---------|
| 0 | Load zoo.csv (101 records, cột 1 = tên con vật) | 101 bản ghi |
| 0b | Tiền xử lý: bỏ cột tên, thêm header | 101 bản ghi, 16 attributes + class |
| 1 | Split 80/20 | Train: 81, Test: 20 |
| 2 | Xây dựng FP-Tree + Header Table | Cây FP-tree |
| 3 | Mining đệ quy | **1,098,719 frequent patterns** |
| 4 | Trích xuất CARs | **525,376 candidate rules** |
| 5 | Pruning luật bị dominated | **1,387 luật** (loại 523,989) |
| 6 | Phân lớp bằng weighted χ² | Dự đoán 20 test records |
| 7 | Đánh giá kết quả | **Accuracy = 70.00%** |

---

## 2. Dữ Liệu Đầu Vào

**Zoo Dataset** — phân loại 101 con vật vào 7 loại dựa trên 16 thuộc tính.

### 2.1 Các thuộc tính

| # | Attribute | Kiểu | Giá trị | Mô tả |
|---|-----------|------|---------|-------|
| 1 | hair | Boolean | 0/1 | Có lông không |
| 2 | feathers | Boolean | 0/1 | Có lông vũ không |
| 3 | eggs | Boolean | 0/1 | Đẻ trứng không |
| 4 | milk | Boolean | 0/1 | Cho sữa không |
| 5 | airborne | Boolean | 0/1 | Bay được không |
| 6 | aquatic | Boolean | 0/1 | Sống dưới nước không |
| 7 | predator | Boolean | 0/1 | Là thú ăn thịt không |
| 8 | toothed | Boolean | 0/1 | Có răng không |
| 9 | backbone | Boolean | 0/1 | Có xương sống không |
| 10 | breathes | Boolean | 0/1 | Thở bằng phổi không |
| 11 | venomous | Boolean | 0/1 | Có nọc độc không |
| 12 | fins | Boolean | 0/1 | Có vây không |
| 13 | legs | Discrete | 0,2,4,5,6,8 | Số chân |
| 14 | tail | Boolean | 0/1 | Có đuôi không |
| 15 | domestic | Boolean | 0/1 | Nuôi trong nhà không |
| 16 | catsize | Boolean | 0/1 | Kích thước ≥ mèo không |

### 2.2 Phân bố 7 classes

| Class | Loại | Số lượng | Ví dụ |
|-------|------|----------|-------|
| 1 | Mammal (Thú có vú) | **41** | gấu, mèo, chó, voi |
| 2 | Bird (Chim) | **20** | đại bàng, vịt, chim cánh cụt |
| 3 | Reptile (Bò sát) | **5** | rắn, rùa biển |
| 4 | Fish (Cá) | **13** | cá chép, cá hồi |
| 5 | Amphibian (Lưỡng cư) | **4** | ếch, kỳ nhông |
| 6 | Insect (Côn trùng) | **8** | ong, bọ chét |
| 7 | Invertebrate (Không xương sống) | **10** | sứa, bạch tuộc |

> ⚠️ **Dữ liệu rất mất cân bằng:** Class 1 (Mammal) chiếm 41%, còn class 3 (Reptile) chỉ có 5 records và class 5 (Amphibian) chỉ có 4 records.

---

## 3. Tiền Xử Lý

| Bước | Mô tả |
|------|-------|
| 1 | Bỏ cột đầu tiên (tên con vật — không dùng cho phân lớp) |
| 2 | Thêm dòng header: `hair,feathers,eggs,...,class` |
| 3 | Class đã ở cột cuối → không cần di chuyển |
| 4 | Tất cả attributes đã là categorical (0/1 hoặc số chân) → không cần discretize |

File sau xử lý: `data/zoo_h.csv` (101 dòng data + 1 dòng header)

---

## 4. Cấu Hình Chạy

```
Dataset:        data/zoo_h.csv
Train/Test:     81 / 20  (80/20)
Min Support:    3  (≈ 3.7% trên 81 records)
Min Confidence: 0.5 (50%)
Random Seed:    42
```

---

## 5. PHẦN 1: FP-Growth — Đào Mỏ Tập Phổ Biến

### 5.1 Thống kê mining

| Metric | Giá trị |
|--------|---------|
| Items trong header table | ~30+ (16 attr × 2 values + 7 classes) |
| Frequent patterns | **1,098,719** |
| Candidate CARs | **525,376** |

> ⚠️ Số patterns rất lớn do 16 binary attributes tạo ra $2^{16} = 65,536$ tổ hợp tiềm năng. Đây là đặc trưng của dataset có nhiều binary attributes.

### 5.2 Top luật quan trọng (sau pruning)

| # | Luật | Sup | Conf | Ý nghĩa |
|---|------|-----|------|---------|
| 1 | milk=1 → **Mammal** | 0.457 | 1.000 | Có sữa → chắc chắn là thú |
| 2 | eggs=0, toothed=1 → **Mammal** | 0.457 | 1.000 | Không đẻ trứng + có răng → thú |
| 3 | venomous=0, eggs=0 → **Mammal** | 0.457 | 1.000 | Không độc + không trứng → thú |
| 24 | feathers=1 → **Bird** | 0.173 | 1.000 | Có lông vũ → chắc chắn là chim |
| 33 | breathes=0, toothed=1 → **Fish** | 0.148 | 1.000 | Không thở phổi + có răng → cá |
| 34 | breathes=0, backbone=1 → **Fish** | 0.148 | 1.000 | Không thở phổi + có xương sống → cá |

**Nhận xét:** Luật có ý nghĩa sinh học rõ ràng — `milk=1 → Mammal` (100%) là đúng theo định nghĩa.

---

## 6. PHẦN 2: CMAR — Phân Lớp

### 6.1 Kết Quả Pruning

```
Candidate rules (trước):  525,376
Pruned rules (sau):       1,387
Luật bị loại:             523,989  (99.74%)
```

> Pruning cực kỳ hiệu quả: loại **99.74%** luật thừa, giảm từ hơn nửa triệu xuống còn ~1,400 luật.

### 6.2 Kết Quả Dự Đoán (20 test records)

| # | Con vật (class) | Actual | Predicted | Kết quả |
|---|----------------|--------|-----------|---------|
| 1 | Bird | 2 | 2 | ✓ |
| 2 | Reptile | 3 | 4 | ✗ |
| 3 | Mammal | 1 | 1 | ✓ |
| 4 | Bird | 2 | 2 | ✓ |
| 5 | Fish | 4 | 4 | ✓ |
| 6 | Bird | 2 | 2 | ✓ |
| 7 | Mammal | 1 | 1 | ✓ |
| 8 | Bird | 2 | 2 | ✓ |
| 9 | Reptile | 3 | 4 | ✗ |
| 10 | Amphibian | 5 | 4 | ✗ |
| 11 | Bird | 2 | 2 | ✓ |
| 12 | Invertebrate | 7 | 7 | ✓ |
| 13 | Insect | 6 | 6 | ✓ |
| 14 | Amphibian | 5 | 4 | ✗ |
| 15 | Bird | 2 | 2 | ✓ |
| 16 | Mammal | 1 | 1 | ✓ |
| 17 | Reptile | 3 | 4 | ✗ |
| 18 | Invertebrate | 7 | 6 | ✗ |
| 19 | Mammal | 1 | 1 | ✓ |
| 20 | Invertebrate | 7 | 7 | ✓ |

**Đúng 14/20 — Sai 6/20**

---

## 7. Đánh Giá Hiệu Suất

### 7.1 Tổng Quan

| Metric | Giá trị |
|--------|---------|
| **Accuracy** | **14/20 = 70.00%** |
| **Macro-F1** | **0.5361** |

### 7.2 Chi Tiết Theo Class

| Class | Loại | TP | FP | FN | Precision | Recall | F1 |
|-------|------|----|----|----|-----------|--------|------|
| **1** | Mammal | 4 | 0 | 0 | **1.000** | **1.000** | **1.000** |
| **2** | Bird | 6 | 0 | 0 | **1.000** | **1.000** | **1.000** |
| **3** | Reptile | 0 | 0 | 3 | 0.000 | 0.000 | **0.000** |
| **4** | Fish | 1 | 5 | 0 | 0.167 | 1.000 | **0.286** |
| **5** | Amphibian | 0 | 0 | 2 | 0.000 | 0.000 | **0.000** |
| **6** | Insect | 1 | 1 | 0 | 0.500 | 1.000 | **0.667** |
| **7** | Invertebrate | 2 | 0 | 1 | 1.000 | 0.667 | **0.800** |

### 7.3 Phân Tích Lỗi

| Lỗi | Actual → Predicted | Nguyên nhân |
|-----|-------------------|-------------|
| 3 lần | Reptile → Fish | Bò sát và cá có nhiều đặc điểm chung (đẻ trứng, có xương sống, có răng). Chỉ 5 reptile trong 101 records → quá ít để tạo luật mạnh |
| 2 lần | Amphibian → Fish | Lưỡng cư cũng đẻ trứng, sống nước — chỉ 4 records trong toàn bộ dataset |
| 1 lần | Invertebrate → Insect | Cả hai đều không xương sống, đẻ trứng |

**Nguyên nhân chính:** Dataset cực kỳ **mất cân bằng** — Reptile (5 records) và Amphibian (4 records) quá ít để FP-Growth tạo luật đủ mạnh phân biệt với Fish.

---

## 8. So Sánh Với Bài Báo

### 8.1 Accuracy

| Phương pháp | Accuracy trên Zoo |
|-------------|------------------|
| **Bài báo — C4.5** | 93.2% |
| **Bài báo — CBA** | 96.8% |
| **Bài báo — CMAR** | **97.1%** |
| **Cài đặt — CMAR** | **70.00%** |

### 8.2 Phân Tích Chênh Lệch (70% vs 97.1%)

Chênh lệch **27.1%** — khá lớn. Nguyên nhân:

| Yếu tố | Bài báo | Cài đặt |
|---------|---------|---------|
| **Evaluation** | Có thể 10-fold cross-validation | Single 80/20 split |
| **Test size** | ~10 records mỗi fold | 20 records (1 sai = -5%) |
| **minSupport** | 1% (support threshold) | 3 (~3.7% — cao hơn 3.7×) |
| **Database coverage pruning** | Có | Chưa cài đặt |
| **Số minority records** | Chia đều qua folds | Có thể dồn hết vào test |

**Yếu tố quan trọng nhất:**
1. **Test size quá nhỏ (20)** — mỗi record sai = -5% accuracy. Sai 6 records = -30%.
2. **Single random split** — có thể minority class (Reptile, Amphibian) bị dồn hết vào test set mà training không đủ sample.
3. **Thiếu database coverage pruning** — với 7 classes mất cân bằng, pruning thêm giúp loại rule noise cho majority class.

> **Lưu ý:** với dataset chỉ 101 records và 7 classes, kết quả **rất nhạy** với cách chia train/test. Nếu đổi seed khác hoặc dùng cross-validation, accuracy có thể thay đổi đáng kể.

---

## Hướng Dẫn Chạy Lại

```bash
cd d:\CMAR
mkdir out 2>nul
javac -d out src\*.java
java -cp out Main data/zoo_h.csv 3 0.5
```

**Output files:**
- `result/fpgrowth_result.txt` — FP-Growth: header table + FP-tree + patterns
- `result/cmar_result.txt` — CMAR: 1,387 luật + predictions + evaluation
- `result/evaluation.txt` — Accuracy 70% + F1 metrics

---

*Dataset: UCI Zoo (101 records). Thuật toán: CMAR (Li, Han & Pei, ICDM 2001).*
