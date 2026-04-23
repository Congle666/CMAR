# So Sánh Với Bài Báo + Các Thay Đổi Đã Cải Tiến

**Mục đích:** Tài liệu tham khảo để viết báo cáo/luận văn về CMAR với cải tiến F1/Recall.

**Bài báo gốc:** Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* ICDM 2001.

**Setup:** 10-fold stratified CV, seed=42, minConf=0.5, χ²_thr=3.841, δ=4.

---

## 📋 Tóm Tắt Thay Đổi (For Thesis Writeup)

### Điểm chung — 4 variants

| # | Variant | Thay đổi so với CMAR gốc | Module bị sửa |
|:-:|---------|--------------------------|----------------|
| v1 | **Baseline** | Không — implement theo đúng bài báo | — |
| v2 | **H1: Weighted χ²** | Nhân thêm **trọng số nghịch tần suất** khi tính score classify | `CMARClassifierWeighted.java` |
| v3 | **H2: Class-specific minSup** | Ngưỡng minSup **riêng cho từng class** (sklearn balanced formula) | `FPGrowth.java` + `CrossValidator.java` |
| v4 | **H1+H2** | Kết hợp cả 2 hướng trên | Cả 2 file |

### Công thức thay đổi

**Bài báo gốc (baseline):**
```
score(c) = Σ [χ²(R)² / maxχ²(R)]                        (1)
minSup toàn cục = N × minSupPct                         (2)
```

**H1 — Weighted χ²:**
```
score(c) = weight(c) × Σ [χ²(R)² / maxχ²(R)]            (1')
weight(c) = N / (k × freq(c))         ← sklearn balanced
```

**H2 — Class-specific minSup:**
```
minSup(c) = max(2, round(minSupPct × freq(c)))          (2')
```
Thay vì ngưỡng chung, mỗi class có ngưỡng riêng tỉ lệ với tần suất.

---

## 📊 PHẦN 1 — So Sánh Với Bài Báo (20 datasets, Baseline vs Paper)

| # | Dataset | N | Cls | Paper Acc | Our Acc | ΔAcc | Our MacroF1 | Our WF1 |
|:-:|---------|:---:|:---:|:---------:|:-------:|:----:|:-----------:|:-------:|
| 1 | breast-w | 699 | 2 | 96.42 | 94.99 | -1.43 | 0.9444 | 0.9499 |
| 2 | cleve | 303 | 2 | 82.18 | 82.52 | +0.34 | 0.8238 | 0.8250 |
| 3 | crx | 690 | 2 | 85.36 | 86.26 | +0.90 | 0.8616 | 0.8627 |
| 4 | diabetes | 768 | 2 | 75.81 | 75.13 | -0.68 | 0.7330 | 0.7541 |
| 5 | german | 1000 | 2 | 73.40 | 74.20 | +0.80 | 0.6639 | 0.7287 |
| 6 | glass | 214 | 6 | 70.09 | 66.11 | -3.98 | 0.6113 | 0.6582 |
| 7 | heart | 270 | 2 | 82.59 | 84.44 | +1.85 | 0.8425 | 0.8444 |
| 8 | hepatitis | 155 | 2 | 80.65 | 81.81 | +1.16 | 0.7363 | 0.8232 |
| 9 | horse | 368 | 2 | 82.61 | 81.53 | -1.08 | 0.8065 | 0.8172 |
| 10 | iris | 150 | 3 | 94.00 | 95.33 | +1.33 | 0.9532 | 0.9532 |
| 11 | labor | 57 | 2 | 89.47 | 84.33 | -5.14 | 0.8389 | 0.8457 |
| 12 | led7 | 200 | 10 | 71.90 | 73.03 | +1.13 | 0.7119 | 0.7062 |
| 13 | lymph | 148 | 4 | 82.43 | 83.46 | +1.03 | **0.4235** ⚠️ | 0.8146 |
| 14 | mushroom | 8124 | 2 | 100.00 | 98.07 | -1.93 | 0.9806 | 0.9807 |
| 15 | sonar | 208 | 2 | 79.33 | 82.52 | +3.19 | 0.8263 | 0.8270 |
| 16 | tic-tac-toe | 958 | 2 | 99.27 | 97.29 | -1.98 | 0.9700 | 0.9729 |
| 17 | vehicle | 846 | 4 | 68.68 | 67.83 | -0.85 | 0.6477 | 0.6462 |
| 18 | waveform | 5000 | 3 | 80.17 | 83.90 | +3.73 | 0.8383 | 0.8384 |
| 19 | wine | 178 | 3 | 95.51 | 95.52 | +0.01 | 0.9559 | 0.9547 |
| 20 | zoo | 101 | 7 | 96.04 | 95.73 | -0.31 | 0.8972 | 0.9490 |
| | **TRUNG BÌNH** | | | **84.30** | **84.20** | **-0.10** | **0.8034** | **0.8384** |

**Quan sát:**
- Chênh lệch accuracy trung bình: **-0.10%** → cài đặt baseline **rất sát bài báo**.
- 19/20 datasets trong khoảng ±5% so với paper.
- **Bài báo KHÔNG báo cáo F1/Recall** → ta bổ sung 2 chỉ số này làm đóng góp mới.

---

## 📊 PHẦN 2 — So Sánh 4 Variants (7 datasets subset)

### 2.1 Macro-F1 (chỉ số chính cho class imbalance)

| Dataset | Paper Acc | Baseline MF1 | H1 MF1 | **H2 MF1** | H1+H2 MF1 | Best |
|---------|:---------:|:------------:|:------:|:----------:|:---------:|:----:|
| german | 73.40 | 0.6639 | 0.6726 | **0.6903** | 0.6961 | **H1+H2** |
| glass | 70.09 | 0.6113 | 0.6171 | 0.6113 | 0.6171 | H1=H1+H2 |
| hepatitis | 80.65 | 0.7363 | 0.7144 | **0.7430** | 0.7103 | **H2** |
| iris | 94.00 | 0.9532 | 0.9532 | 0.9532 | 0.9532 | tie |
| lymph | 82.43 | 0.4235 | **0.4271** | 0.4167 | 0.4084 | **H1** |
| wine | 95.51 | 0.9559 | 0.9559 | 0.9559 | 0.9559 | tie |
| zoo | 96.04 | 0.8972 | 0.8525 | 0.8972 | 0.8525 | Base=H2 |
| **TB** | 84.59 | **0.7488** | 0.7418 | **0.7525** ⭐ | 0.7419 | **H2** |

### 2.2 Accuracy

| Dataset | Paper | Baseline | H1 | H2 | H1+H2 |
|---------|:-----:|:--------:|:----:|:----:|:-----:|
| german | 73.40 | 74.20 | 73.20 | 74.60 | 73.00 |
| glass | 70.09 | 66.11 | 64.04 | 66.11 | 64.04 |
| hepatitis | 80.65 | 81.81 | 76.64 | **82.48** | 76.64 |
| iris | 94.00 | 95.33 | 95.33 | 95.33 | 95.33 |
| lymph | 82.43 | 83.46 | 84.18 | 81.32 | 77.75 |
| wine | 95.51 | 95.52 | 95.52 | 95.52 | 95.52 |
| zoo | 96.04 | 95.73 | 92.68 | 95.73 | 92.68 |
| **TB** | 84.59 | 84.59 | 83.08 | **84.44** | 82.14 |

---

## 🔬 PHẦN 3 — Per-Class F1 + Recall (Key Imbalanced Datasets)

### 3.1 Lymph — Target chính (2/4 class F1=0 trong baseline)

| Class | Support | Ratio | Baseline (v1) | H1 (v2) | H2 (v3) | H1+H2 (v4) |
|-------|:-------:|:-----:|:-------------:|:-------:|:-------:|:----------:|
| | | | F1 / Rec | F1 / Rec | F1 / Rec | F1 / Rec |
| metastases | 81 | 54.7% | 0.861 / 0.840 | 0.866 / 0.840 | 0.861 / 0.840 | **0.837** / 0.790 |
| malign_lymph | 61 | 41.2% | 0.833 / 0.902 | 0.842 / 0.902 | **0.806** / 0.853 | 0.797 / 0.836 |
| **fibrosis** | **4** | **2.7%** | **0.000 / 0.000** | **0.000 / 0.000** | **0.000 / 0.000** | **0.000 / 0.000** |
| **normal** | **2** | **1.4%** | **0.000 / 0.000** | **0.000 / 0.000** | **0.000 / 0.000** | **0.000 / 0.000** |

**Finding:** **KHÔNG variant nào rescue được fibrosis/normal.** H2 có sinh rules (3 FP trên fibrosis, xem `v3_per_class.csv`) nhưng không đúng được 1 TP nào.

### 3.2 Glass — 6 classes imbalanced

| Class | Support | Baseline F1/Rec | H1 F1/Rec | H2 F1/Rec | H1+H2 F1/Rec |
|-------|:-------:|:---------------:|:---------:|:---------:|:------------:|
| building_nonfloat | 76 | 0.652 / 0.566 | 0.630 / 0.526 | 0.652 / 0.566 | 0.630 / 0.526 |
| building_float | 70 | 0.712 / 0.743 | 0.697 / 0.657 | 0.712 / 0.743 | 0.697 / 0.657 |
| **vehicle_float** | 17 | **0.200 / 0.177** | **0.311 / 0.412** ⭐ | 0.200 / 0.177 | **0.311 / 0.412** |
| headlamps | 29 | 0.800 / 0.897 | 0.839 / 0.897 | 0.800 / 0.897 | 0.839 / 0.897 |
| vehicle_nonfloat | 13 | 0.786 / 0.846 | 0.645 / 0.769 | 0.786 / 0.846 | 0.645 / 0.769 |
| containers | 9 | 0.519 / 0.778 | 0.581 / 1.000 | 0.519 / 0.778 | 0.581 / 1.000 |

**Finding:** H1 cải thiện `vehicle_float` F1 từ 0.20 → **0.31** (+55%!). Recall từ 18% → **41%**. Đánh đổi: các class khác bị giảm nhẹ.

### 3.3 German — 2 classes imbalanced (minority 'bad')

| Class | Support | Baseline F1/Rec | H1 F1/Rec | **H2 F1/Rec** | H1+H2 F1/Rec |
|-------|:-------:|:---------------:|:---------:|:-------------:|:------------:|
| good (70%) | 700 | 0.826 / 0.874 | 0.818 / 0.860 | **0.822** / 0.836 | 0.798 / 0.760 |
| **bad** (30%) | 300 | **0.502 / 0.433** | 0.527 / 0.480 | **0.559 / 0.537** ⭐ | **0.595** / 0.660 |

**Finding:** H2 cải thiện minority `bad` **Recall từ 43% → 54%** (+26%!). H1+H2 thậm chí **F1 0.50 → 0.60** nhưng Accuracy giảm.

### 3.4 Hepatitis — 2 classes imbalanced (minority 'DIE')

| Class | Support | Baseline F1/Rec | H1 F1/Rec | **H2 F1/Rec** | H1+H2 F1/Rec |
|-------|:-------:|:---------------:|:---------:|:-------------:|:------------:|
| LIVE (79%) | 123 | 0.884 / 0.870 | 0.840 / 0.817 | **0.889** / 0.878 | 0.839 / 0.764 |
| **DIE** (21%) | 32 | **0.588 / 0.625** | 0.470 / 0.469 | **0.597** / 0.625 | 0.581 / **0.781** ⭐ |

**Finding:** H1+H2 tăng Recall của minority DIE lên **78%** (baseline chỉ 62.5%) — tốt cho bài toán y tế!

---

## 📝 PHẦN 4 — Tóm Tắt "Thay Đổi Được Gì" (For Thesis)

### 4.1 Cải thiện rõ rệt

| Dataset | Metric | Baseline → Best variant | Δ |
|---------|--------|-------------------------|:---:|
| **glass/vehicle_float** | F1 | 0.20 → **0.31** (H1) | **+55%** |
| **glass/vehicle_float** | Recall | 17.6% → **41.2%** (H1) | **+134%** |
| **german/bad** | F1 | 0.50 → **0.60** (H1+H2) | **+19%** |
| **german/bad** | Recall | 43.3% → **66.0%** (H1+H2) | **+52%** |
| **hepatitis/DIE** | Recall | 62.5% → **78.1%** (H1+H2) | **+25%** |
| **lymph/metastases** | F1 | 0.861 → 0.866 (H1) | +0.6% |

### 4.2 Không cải thiện được

| Dataset | Class | Lý do |
|---------|-------|-------|
| lymph | fibrosis (4 records, 2.7%) | Items đặc trưng bị lọc ở item-level pruning (freq < global minSup=7). Class-specific threshold 2 không giúp vì rules không được mine từ đầu. |
| lymph | normal (2 records, 1.4%) | Tương tự. |

### 4.3 Regression (chỗ xấu đi)

| Dataset | Metric | Baseline → Worst | Δ |
|---------|--------|------------------|:---:|
| zoo | MacroF1 | 0.897 → 0.852 (H1) | -5% |
| hepatitis | Accuracy | 81.81 → 76.64 (H1+H2) | -6.3% |
| glass | Accuracy | 66.11 → 64.04 (H1+H2) | -3.1% |

### 4.4 Trade-off chính

- **H1 (Weighted):** Tăng Recall minority, giảm Precision (nhiều FP). Đổi accuracy ↓ lấy F1 minority ↑.
- **H2 (Class minSup):** Chỉ helps khi minority có **≥ 10% dataset** + items phổ biến (shared). Với minority < 5% → vô dụng.
- **H1+H2 combined:** Over-aggressive — weight amplify noise từ thêm rules của H2 → recall minority tăng mạnh nhưng precision tụt sâu.

---

## 🎯 PHẦN 5 — Tóm Gọn Cho Phần "Đóng Góp Của Đề Tài"

### Đóng góp thực tế

1. **Implement CMAR faithful theo paper** (baseline) — accuracy -0.10% so với paper (rất sát).
2. **Đo lường F1 + Recall** (paper không có) — phát hiện vấn đề class imbalance nặng (lymph MacroF1 0.42 dù accuracy 83%).
3. **2 cải tiến thuật toán:** H1 (weighted χ²) + H2 (class-specific minSup).
4. **Benchmark đầy đủ 4 variants** trên 7-20 UCI datasets → có số liệu cụ thể.
5. **Phát hiện giới hạn:** cả 3 cải tiến **KHÔNG rescue được class cực hiếm (<5%)** — cần kỹ thuật phức tạp hơn (F1-aware pruning, SMOTE).

### Thành công cụ thể (viết cho người chấm)

- **Glass dataset:** Recall lớp `vehicle_float` (17 records) từ **17.6% → 41.2%** (H1).
- **German dataset:** Recall lớp minority `bad` từ **43.3% → 66.0%** (H1+H2), F1 tăng 19%.
- **Hepatitis dataset:** Recall lớp minority `DIE` (y tế) từ **62.5% → 78.1%** (H1+H2) — quan trọng vì bỏ sót bệnh nhân tử vong nguy hiểm.

### Hạn chế cần thừa nhận

- **Class cực hiếm (<5% dataset)** vẫn F1=0 — do item-level pruning loại items đặc trưng.
- **Accuracy tổng giảm 0.1-2.5%** khi áp dụng cải tiến — trade-off.
- **H1+H2 combined tệ hơn từng cái riêng** — giả thuyết "cộng hợp" sai. Finding quan trọng cho research.

---

## 📂 Files Tham Khảo

| File | Nội dung |
|------|----------|
| `result/baseline_metrics.csv` | 20 datasets baseline |
| `result/baseline_per_class.csv` | Per-class P/R/F1 baseline |
| `result/v2_metrics.csv` + v2_per_class | H1 (20 datasets) |
| `result/v3_metrics.csv` + v3_per_class | H2 (7 datasets subset) |
| `result/v4_metrics.csv` + v4_per_class | H1+H2 (7 datasets) |
| `report/comparison_4_variants.md` | So sánh 4 variants chi tiết |
| `report/baseline_f1_20datasets.md` | Snapshot baseline + phân tích imbalance |
| `report/comparison_v2_vs_baseline.md` | H1 alone deep dive |

## 💡 Gợi Ý Viết Báo Cáo

### Cấu trúc chương "Cải tiến đề xuất"

1. **Vấn đề đặt ra:** CMAR paper chỉ tối ưu Accuracy → class imbalance bị che khuất (lymph case study).
2. **Hướng 1 — Class-weighted χ²:** motivation + công thức + code change + kết quả.
3. **Hướng 2 — Class-specific minSup:** motivation + công thức + code change + kết quả.
4. **Hướng kết hợp H1+H2:** giả thuyết → kết quả ngược → bài học.
5. **Đánh giá:** bảng so sánh + phân tích per-class + thảo luận trade-off.
6. **Hạn chế:** tại sao lymph fibrosis vẫn F1=0 + đề xuất hướng tương lai.

### Cấu trúc chương "Thực nghiệm"

- Table 5.1: So sánh Accuracy với paper (20 datasets)
- Table 5.2: So sánh Macro-F1 giữa 4 variants (7 datasets)
- Table 5.3: Per-class F1/Recall cho lymph, glass, german, hepatitis
- Biểu đồ (gợi ý): bar chart F1 minority class theo 4 variants

---

*File này là tham khảo đầy đủ cho phần "Đóng góp" và "Thực nghiệm" trong báo cáo/luận văn. Có thể copy trực tiếp các bảng vào báo cáo.*
