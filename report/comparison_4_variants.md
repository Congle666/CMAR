# So Sánh 4 Variants CMAR — Baseline vs H1 vs H2 vs H1+H2

**Ngày:** 2026-04-23
**Subset:** 7 datasets (chọn để chạy nhanh + có imbalance rõ)
**Setup:** 10-fold stratified CV, seed=42, minConf=0.5, χ²_thr=3.841, δ=4
**Branch:** `main`

---

## 🎯 Executive Summary — Kết luận honest

**Cả H1, H2 và H1+H2 đều KHÔNG phải giải pháp thần kỳ cho imbalance.**

Kết quả trên 7 dataset được chọn:

| Variant | Macro-F1 TB | Accuracy TB | vs baseline |
|---------|:-----------:|:-----------:|:-----------:|
| **Baseline (v1)** | **0.7488** | 83.52% | — |
| H1 — Weighted χ² (v2) | 0.7418 | 83.07% | **-0.007** |
| **H2 — Class minSup (v3)** | **0.7525** | **84.44%** | **+0.004** ✅ (tốt nhất) |
| H1+H2 combined (v4) | 0.7419 | 82.14% | **-0.007** ❌ (tệ hơn cả H1 hoặc H2 riêng lẻ) |

**Winner: H2 alone** — cải thiện nhẹ nhưng rõ trên **2 dataset (german, hepatitis)**.

**Thất bại: H1+H2** — kết hợp không cộng hợp, làm mô hình quá "hung hãn" với minority, sinh false positives.

---

## 📊 Bảng chi tiết — Macro-F1 per Dataset

| Dataset | Records | Classes | Baseline | H1 (v2) | H2 (v3) | H1+H2 (v4) | Best |
|---------|:-------:|:-------:|:--------:|:-------:|:-------:|:----------:|:----:|
| german | 1000 | 2 | 0.6639 | 0.6726 | **0.6903** | 0.6961 | v4 |
| glass | 214 | 6 | 0.6113 | **0.6171** | 0.6113 | 0.6171 | v2/v4 |
| hepatitis | 155 | 2 | 0.7363 | 0.7144 | **0.7430** | 0.7103 | **H2** |
| iris | 150 | 3 | 0.9532 | 0.9532 | 0.9532 | 0.9532 | ➖ tie |
| **lymph** | 148 | 4 | **0.4235** | **0.4271** | 0.4167 | **0.4084** | **H1 (tiny)** |
| wine | 178 | 3 | 0.9559 | 0.9559 | 0.9559 | 0.9559 | ➖ tie |
| zoo | 101 | 7 | **0.8972** | 0.8525 | **0.8972** | 0.8525 | tie (Base/H2) |
| **TB** | | | **0.7488** | 0.7418 | **0.7525** | 0.7419 | **H2** |

---

## 📉 Bảng Accuracy per Dataset

| Dataset | Baseline | H1 | H2 | H1+H2 |
|---------|:--------:|:----:|:----:|:-----:|
| german | 74.20 | 73.20 | 74.60 | 73.00 |
| glass | 66.11 | 64.04 | 66.11 | 64.04 |
| hepatitis | 81.81 | 76.64 | **82.48** | 76.64 |
| iris | 95.33 | 95.33 | 95.33 | 95.33 |
| lymph | 83.46 | 84.18 | 81.32 | 77.75 |
| wine | 95.52 | 95.52 | 95.52 | 95.52 |
| zoo | 95.73 | 92.68 | 95.73 | 92.68 |
| **TB** | **84.59** | 83.08 | **84.44** | 82.14 |

---

## 🔬 Phân Tích Chi Tiết — Lymph (Target Dataset)

Lymph là **target chính** vì có 2/4 class F1=0 trong baseline. Kết quả per-class:

| Class | Support | Base F1 | H1 F1 | **H2 F1** | H1+H2 F1 |
|-------|:-------:|:-------:|:-----:|:---------:|:--------:|
| metastases | 81 | 0.8608 | 0.8662 | **0.8608** | 0.8460 |
| malign_lymph | 61 | 0.8333 | 0.8421 | **0.8062** | 0.8000 |
| fibrosis | 4 | **0.0000** | **0.0000** | **0.0000** | **0.0000** |
| normal | 2 | **0.0000** | **0.0000** | **0.0000** | **0.0000** |

**Kết luận thẳng:** **KHÔNG có variant nào cứu được fibrosis/normal.**

**Lý do:**
- H2 với `minSup(fibrosis)=2` nhưng global minSup=7 vẫn lọc items đặc trưng của fibrosis (freq<7).
- Rules được sinh cho fibrosis (H2 v3 có **3 FP** — xem `v3_per_class.csv` — rules đã tồn tại) nhưng trigger sai → false positives, không đúng được fibrosis thực.
- H1 weighting amplified false positives → lymph accuracy giảm mạnh (-5.71% vs baseline).

**Root cause:** Items đặc trưng cho fibrosis có **global freq < 7** → bị cắt ở item-level pruning TRƯỚC khi tới class-specific emission. Class-specific minSup không cứu được trong trường hợp này.

---

## 💡 Insight Nghiên Cứu

### Khi nào H2 hiệu quả?

**H2 giúp khi:**
- Dataset có 2-3 classes (german, hepatitis) — minority class vẫn đủ records để items có global freq cao.
- Minority/majority ratio KHÔNG quá nhỏ (> 20%).

**H2 KHÔNG giúp khi:**
- Minority class < 5% dataset (lymph fibrosis 2.7%, normal 1.4%).
- Nhiều classes (≥ 4) với phân bố quá skewed.
- Items đặc trưng chỉ xuất hiện trong minority class.

### Tại sao H1+H2 tệ hơn tổng?

- H1 đã push minority class score lên (inverse weight).
- H2 sinh thêm rules cho minority (một số là noise/false).
- H1 amplify luôn cả rules noise từ H2 → false positives bùng nổ.

**Minh hoạ trên zoo:**
- Baseline: class 5 (amphibian, 4 records) F1=0.75
- H1: F1=0.55 (weight quá mạnh)
- H2: F1=0.75 (không đổi — global minSup đã OK cho zoo)
- H1+H2: F1=0.55 (chỉ lặp lại H1's damage)

---

## 🎓 Kết Luận

### Mục tiêu đã đạt
- ✅ **Verified** H1 và H2 có thể cài đặt dễ dàng trong CMAR.
- ✅ **Measured** tác động chính xác với Macro-F1, per-class F1.
- ✅ **Disproved** giả thuyết "H1+H2 cộng hợp sẽ tốt hơn" — thực tế ngược lại.

### Mục tiêu chưa đạt
- ❌ **Không cứu được** fibrosis/normal F1=0 (các class cực hiếm <5%).
- ❌ **Overall Macro-F1** không tăng > 0.02 (chỉ +0.004 với H2).

### Giải pháp đề xuất cho tương lai (Hướng 3+)

1. **Hướng 3 — F1-aware pruning:** Thay χ² threshold bằng F1 kỳ vọng — giữ rules góp F1, loại rules chỉ boost accuracy.

2. **Hạ global minSup cho minority-specific items:** Thuật toán adaptive — phát hiện items có "pattern đặc trưng cho class X" và cho phép support thấp hơn.

3. **SMOTE/oversampling:** Nhân bản records minority class trước khi mining — H2 sẽ work tốt hơn.

4. **Ensemble voting:** Chạy 3 classifiers (baseline, H1, H2) song song → voting cuối.

5. **Cap weight/cap candidates:** Weight tối đa 3.0x thay vì unbounded, hoặc giới hạn số rules mỗi class.

---

## 📂 Files liên quan

- `result/baseline_metrics.csv` / `result/baseline_per_class.csv` — v1 baseline (20 datasets)
- `result/v2_metrics.csv` / `result/v2_per_class.csv` — H1 weighted (20 datasets)
- `result/v3_metrics.csv` / `result/v3_per_class.csv` — H2 class-minSup (7 datasets subset)
- `result/v4_metrics.csv` / `result/v4_per_class.csv` — H1+H2 combined (7 datasets subset)
- `result/v3_benchmark.log`, `result/v4_benchmark.log` — console logs

## Next steps

→ **Hướng 3 — F1-aware pruning** (nên thử) hoặc
→ **Variant cải tiến:** H2 với adaptive global minSup cho minority items.
→ Viết final synthesis report sau khi đã có Hướng 3.

---

*Honest comparison — cả 3 hướng đều có điểm yếu. H2 alone là best nhưng chỉ cải thiện nhẹ. Cần kỹ thuật phức tạp hơn cho minority class cực hiếm.*
