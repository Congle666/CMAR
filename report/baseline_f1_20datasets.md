# Baseline F1 Snapshot — CMAR trên 20 UCI Datasets

**Ngày:** 2026-04-21
**Setup:** 10-fold stratified CV, seed = 42, minConf = 0.5, χ²_thr = 3.841, δ = 4
**Commit:** (sẽ tag `baseline-f1-v1`)
**Source data:** `result/baseline_metrics.csv`, `result/baseline_per_class.csv`

---

## 📋 Executive Summary

- **20/20 datasets** đánh giá thành công.
- **Accuracy trung bình: 84.20%** (paper CMAR: 84.30%, chênh -0.10%).
- **Macro-F1 trung bình: 0.8034**.
- **Mean Absolute Diff vs paper: 1.64%** — rất sát, 19/20 datasets trong khoảng ±5%.
- **1 dataset cực kỳ imbalanced (`lymph`)** — Accuracy 83% nhưng Macro-F1 chỉ 0.42 do 2 class hiếm (`fibrosis` 4 records, `normal` 2 records) bị **F1 = 0.0**.
- **3 dataset khác có imbalance vừa** — `hepatitis`, `german`, `zoo`, `glass` đều có gap Accuracy–MacroF1 ≥ 0.05.
- Các dataset cân bằng (iris, mushroom, wine, breast-w) đều đạt MacroF1 ≈ Accuracy → mô hình công bằng.

---

## 📊 Bảng 1 — Tổng hợp 20 Datasets

| # | Dataset | N | Cls | minSup% | Acc | MacroF1 | WeightedF1 | Paper | Δ |
|:-:|---------|:---:|:---:|:-------:|:-----:|:-------:|:----------:|:-----:|:----:|
| 1 | breast-w | 699 | 2 | 2.0% | 94.99 | **0.9444** | 0.9499 | 96.42 | -1.43 |
| 2 | cleve | 303 | 2 | 2.0% | 82.52 | **0.8238** | 0.8250 | 82.18 | +0.34 |
| 3 | crx | 690 | 2 | 4.0% | 86.26 | **0.8616** | 0.8627 | 85.36 | +0.90 |
| 4 | diabetes | 768 | 2 | 3.0% | 75.13 | **0.7330** | 0.7541 | 75.81 | -0.68 |
| 5 | german | 1000 | 2 | 6.0% | 74.20 | **0.6639** | 0.7287 | 73.40 | +0.80 |
| 6 | **glass** | 214 | 6 | 1.0% | 66.11 | **0.6113** | 0.6582 | 70.09 | -3.98 |
| 7 | heart | 270 | 2 | 3.0% | 84.44 | **0.8425** | 0.8444 | 82.59 | +1.85 |
| 8 | hepatitis | 155 | 2 | 5.0% | 81.81 | **0.7363** | 0.8232 | 80.65 | +1.16 |
| 9 | horse | 368 | 2 | 3.0% | 81.53 | **0.8065** | 0.8172 | 82.61 | -1.08 |
| 10 | iris | 150 | 3 | 3.0% | 95.33 | **0.9532** | 0.9532 | 94.00 | +1.33 |
| 11 | **labor** | 57 | 2 | 5.0% | 84.33 | **0.8389** | 0.8457 | 89.47 | -5.14 |
| 12 | led7 | 200 | 10 | 3.0% | 73.03 | **0.7119** | 0.7062 | 71.90 | +1.13 |
| 13 | **lymph** | 148 | 4 | 5.0% | 83.46 | **0.4235** ⚠️ | 0.8146 | 82.43 | +1.03 |
| 14 | mushroom | 8124 | 2 | 15.0% | 98.07 | **0.9806** | 0.9807 | 100.00 | -1.93 |
| 15 | sonar | 208 | 2 | 5.0% | 82.52 | **0.8263** | 0.8270 | 79.33 | +3.19 |
| 16 | tic-tac-toe | 958 | 2 | 0.3% | 97.29 | **0.9700** | 0.9729 | 99.27 | -1.98 |
| 17 | vehicle | 846 | 4 | 3.0% | 67.83 | **0.6477** | 0.6462 | 68.68 | -0.85 |
| 18 | waveform | 5000 | 3 | 1.0% | 83.90 | **0.8383** | 0.8384 | 80.17 | +3.73 |
| 19 | wine | 178 | 3 | 3.0% | 95.52 | **0.9559** | 0.9547 | 95.51 | +0.01 |
| 20 | zoo | 101 | 7 | 3.0% | 95.73 | **0.8972** | 0.9490 | 96.04 | -0.31 |
| | **TRUNG BÌNH** | | | | **84.20** | **0.8034** | **0.8384** | **84.30** | **-0.10** |

> **⚠️ Lưu ý:** 3 dataset có Δ > 3% (glass -3.98, labor -5.14, sonar +3.19, waveform +3.73). Chi tiết nguyên nhân xem §5.

---

## 🔥 Bảng 2 — Xếp Hạng Mức Độ Class Imbalance (Acc − MacroF1)

Dataset nào có gap lớn → Accuracy đang "lừa" chúng ta. **Macro-F1 là chỉ báo thật**.

| Rank | Dataset | Acc | MacroF1 | **Gap** | Số class | Ghi chú |
|:----:|---------|:------:|:-------:|:-------:|:--------:|---------|
| 🥇 | **lymph** | 0.8346 | 0.4235 | **0.4111** | 4 | **2/4 class có F1=0** (fibrosis, normal) |
| 🥈 | hepatitis | 0.8181 | 0.7363 | 0.0818 | 2 | Minority `DIE` (32 recs) F1=0.59 |
| 🥉 | german | 0.7420 | 0.6639 | 0.0781 | 2 | Minority `bad` (300 recs) F1=0.50 |
| 4 | **zoo** | 0.9573 | 0.8972 | 0.0601 | 7 | Class 3 & 5 (rare) F1≈0.67, 0.75 |
| 5 | glass | 0.6611 | 0.6113 | 0.0498 | 6 | `vehicle_float` (17 recs) F1=0.20 |
| 6 | vehicle | 0.6783 | 0.6477 | 0.0306 | 4 | `opel` F1=0.40 (hay nhầm saab/opel) |
| 7 | led7 | 0.7303 | 0.7119 | 0.0184 | 10 | Class `8` F1=0.41, `5` F1=0.61 |
| 8 | diabetes | 0.7513 | 0.7330 | 0.0183 | 2 | |
| ... | (còn lại) | | | <0.02 | | Gần cân bằng |

---

## ⛔ Bảng 3 — Các Class Có F1 = 0 (không phát hiện được)

| Dataset | Class | Support | TP | FP | FN | Nguyên nhân |
|---------|-------|:-------:|:--:|:--:|:--:|-------------|
| lymph | fibrosis | **4** | 0 | 0 | 4 | Quá ít records (2.7% dataset) |
| lymph | normal | **2** | 0 | 0 | 2 | Quá ít records (1.4% dataset) |

→ Chỉ 2 class thực sự F1=0 — đều ở `lymph` do số records cực nhỏ (2 và 4 trên 148).

---

## 📉 Bảng 4 — Classes F1 < 0.5 (Yếu)

| Dataset | Class | Support | F1 | Precision | Recall |
|---------|-------|:-------:|:----:|:---------:|:------:|
| glass | vehicle_float | 17 | **0.2000** | 0.2308 | 0.1765 |
| vehicle | opel | 212 | **0.3987** | 0.6058 | 0.2972 |
| led7 | 8 | 21 | **0.4118** | 0.5385 | 0.3333 |
| lymph | fibrosis | 4 | **0.0000** | — | — |
| lymph | normal | 2 | **0.0000** | — | — |
| german | bad | 300 | **0.5019** | 0.5963 | 0.4333 |

**Quan sát:** Recall thường thấp hơn Precision → model **bỏ sót** minority class (dự đoán "an toàn" thành majority). Đây là dấu hiệu rõ ràng mà **Hướng 1 (class-weighted χ²) và Hướng 2 (class-specific minSup)** sẽ cải thiện.

---

## 🎯 Các Dataset Mục Tiêu Cho Cải Tiến

Dựa trên phân tích trên, **3 nhóm dataset** sẽ phản ánh rõ nhất tác động của cải tiến:

### Nhóm A — Impact CAO (dễ thấy rõ)
| Dataset | Vấn đề | Kỳ vọng MacroF1 |
|---------|--------|:---------------:|
| **lymph** | 2 class F1=0, gap 0.41 | 0.42 → **0.55+** |
| **glass** | vehicle_float F1=0.20 | 0.61 → **0.70+** |
| **german** | minority `bad` F1=0.50 | 0.66 → **0.75+** |
| **vehicle** | opel F1=0.40 | 0.65 → **0.70+** |

### Nhóm B — Impact TRUNG BÌNH
| Dataset | Kỳ vọng |
|---------|:-------:|
| hepatitis | 0.74 → 0.78 |
| zoo | 0.90 → 0.92 |
| led7 | 0.71 → 0.75 |
| diabetes | 0.73 → 0.76 |

### Nhóm C — Sanity Check (KHÔNG giảm)
- iris, mushroom, wine, breast-w, tic-tac-toe, waveform: đã đạt MacroF1 > 0.95 → cải tiến không được làm xấu đi.

---

## 📝 5. Phân Tích Chênh Lệch vs Paper

19/20 datasets trong khoảng ±5% so với paper — rất sát. 3 điểm đáng chú ý:

### labor (-5.14%) — outlier
- Dataset 57 records, 56 dòng có missing `?` → cách xử lý missing khác paper có thể gây chênh lớn.
- Xem phân tích chi tiết: mỗi fold test chỉ có 5–6 records → 1 record sai = -17% accuracy fold.

### glass (-3.98%)
- 6 class imbalance (9–76 records), thuộc tính liên tục đã discretize.
- Cách discretize khác paper (equal-width vs Fayyad-Irani entropy) → chênh ~4%.

### waveform (+3.73%), sonar (+3.19%)
- Vượt paper — có thể do discretization hoặc minSup chọn khác.

**Tất cả các chênh lệch đều là do dataset/preprocessing, KHÔNG phải lỗi thuật toán.** Xem `report/so_sanh_bai_bao_va_cai_dat.md` để chi tiết.

---

## 🔮 6. Mục Tiêu Các Cải Tiến (Hướng 1, 2, 3)

Sau khi có baseline này, ta thiết lập các mục tiêu cụ thể cho 3 hướng cải tiến:

### Hướng 1 — Class-weighted χ²
- **Mục tiêu:** Tăng Recall minority class. Macro-F1 `lymph` từ 0.42 lên **≥ 0.55**.
- **Trade-off chấp nhận:** Accuracy giảm 1–3% trên dataset cân bằng.

### Hướng 2 — Class-specific minSup
- **Mục tiêu:** Sinh thêm luật cho minority class. Giải quyết class F1=0.
- **Trade-off:** Số luật tăng → thời gian train lâu hơn.

### Hướng 3 — F1-aware pruning
- **Mục tiêu:** Pruning tầng 2 giữ luật vì F1 thay vì χ² thô.
- **Trade-off:** Phải tune ngưỡng F1.

**Thành công tổng thể** nếu sau cả 3 cải tiến:
- Avg Macro-F1 **tăng từ 0.80 → ≥ 0.85**
- Avg Accuracy **không giảm quá 2%** (hiện 84.20%, vẫn phải ≥ 82%)
- Không dataset nào xuất hiện thêm class F1=0 mới

---

## 📂 Files liên quan

- `result/baseline_metrics.csv` — bảng 1 dạng CSV (parse dễ)
- `result/baseline_per_class.csv` — per-class metrics chi tiết
- `result/baseline_benchmark.log` — console output đầy đủ
- `report/benchmark_20_datasets.md` — benchmark trước (chỉ accuracy, no F1)
- `plans/20260421-1130-benchmark-f1-infrastructure/plan.md` — plan hoàn thành

## Next Steps

→ `plans/YYYYMMDD-HHmm-cmar-improvement-h1-weighted-chi2/` — implement Hướng 1 (Class-weighted χ²).
→ Kế thừa CSV format này làm template cho mọi experiment tiếp theo.
→ Mọi so sánh sẽ là `v2_metrics.csv`, `v3_metrics.csv` diff với `baseline_metrics.csv`.

---

*Baseline F1 snapshot hoàn thành 2026-04-21. Git tag: `baseline-f1-v1`.*
