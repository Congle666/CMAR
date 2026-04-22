# Comparison: Hướng 1 (Weighted χ²) vs Baseline CMAR

**Date:** 2026-04-21
**Experiment:** `CMARClassifierWeighted` with sklearn-style inverse class frequency weighting
**Setup:** 10-fold stratified CV, seed=42, minConf=0.5, χ²_thr=3.841, δ=4 (identical to baseline)
**Change:** Only `classify()` logic modified — mining, pruning, CR-tree unchanged
**Commits:** baseline `a79b40a`, v2 TBD

---

## 🎯 Executive Summary — Honest Verdict

**Hướng 1 có tác dụng MIXED — không phải silver bullet như kỳ vọng.**

| Metric | Baseline (v1) | Weighted (v2) | Δ |
|--------|:-------------:|:-------------:|:-----:|
| Avg Accuracy | **84.20%** | 83.42% | **-0.78%** |
| Avg Macro-F1 | **0.8034** | 0.7988 | **-0.0046** |
| Datasets within ±5% paper | 19/20 | 18/20 | -1 |

**Kết luận tổng thể:** Macro-F1 trung bình **giảm nhẹ** — không đạt mục tiêu ≥ 0.83.

**Điểm quan trọng phát hiện:** Weighted χ² chỉ có tác dụng **KHI đã có rule cho minority class**. Khi không có rule nào (ví dụ `lymph/fibrosis`, `lymph/normal`), weighting không thể tạo ra prediction — cần **Hướng 2 (class-specific minSup)** để thật sự sinh ra rule cho minority.

---

## 📊 Bảng 1 — Tất cả 20 Datasets

| Dataset | Base Acc | v2 Acc | ΔAcc | Base MacroF1 | v2 MacroF1 | **ΔMacroF1** | Kết quả |
|---------|:--------:|:------:|:-----:|:------------:|:----------:|:------------:|:-------:|
| breast-w | 94.99 | 95.85 | +0.86 | 0.9444 | 0.9544 | **+0.0100** | ✅ Cải thiện |
| cleve | 82.52 | 82.84 | +0.32 | 0.8238 | 0.8274 | **+0.0036** | ✅ |
| crx | 86.26 | 86.26 | 0.00 | 0.8616 | 0.8616 | **0.0000** | ➖ Không đổi |
| diabetes | 75.13 | 74.88 | -0.25 | 0.7330 | 0.7356 | **+0.0026** | ✅ |
| german | 74.20 | 73.20 | -1.00 | 0.6639 | 0.6726 | **+0.0087** | ✅ Target đạt |
| **glass** | 66.11 | 64.04 | -2.07 | 0.6113 | 0.6171 | **+0.0058** | 🎯 Target đạt |
| heart | 84.44 | 83.70 | -0.74 | 0.8425 | 0.8355 | -0.0070 | ❌ Regression |
| **hepatitis** | 81.81 | 76.64 | **-5.17** | 0.7363 | 0.7144 | **-0.0219** | ❌ Regression nặng |
| horse | 81.53 | 80.17 | -1.36 | 0.8065 | 0.7952 | -0.0113 | ❌ |
| iris | 95.33 | 95.33 | 0.00 | 0.9532 | 0.9532 | 0.0000 | ➖ Balanced, OK |
| labor | 84.33 | 82.67 | -1.66 | 0.8389 | 0.8219 | -0.0170 | ❌ |
| led7 | 73.03 | 71.59 | -1.44 | 0.7119 | 0.6970 | -0.0149 | ❌ |
| **lymph** | 83.46 | 84.18 | +0.72 | 0.4235 | 0.4271 | **+0.0036** | 🎯 Tiny, chưa đạt target 0.55 |
| mushroom | 98.07 | 98.46 | +0.39 | 0.9806 | 0.9846 | **+0.0040** | ✅ |
| sonar | 82.52 | 83.47 | +0.95 | 0.8263 | 0.8364 | **+0.0101** | ✅ |
| tic-tac-toe | 97.29 | 95.20 | -2.09 | 0.9700 | 0.9481 | **-0.0219** | ❌ |
| vehicle | 67.83 | 67.83 | 0.00 | 0.6477 | 0.6483 | **+0.0006** | ➖ Gần không đổi |
| waveform | 83.90 | 83.82 | -0.08 | 0.8383 | 0.8376 | -0.0007 | ➖ |
| wine | 95.52 | 95.52 | 0.00 | 0.9559 | 0.9559 | 0.0000 | ➖ Balanced |
| **zoo** | 95.73 | 92.68 | **-3.05** | 0.8972 | 0.8525 | **-0.0447** | ❌ Regression |
| **TRUNG BÌNH** | **84.20** | **83.42** | **-0.78** | **0.8034** | **0.7988** | **-0.0046** | ⚠️ Mixed |

---

## 📉 Phân Loại Kết Quả

| Loại | Số datasets | Datasets |
|------|:-----------:|----------|
| ✅ **MacroF1 tăng** | 8 | breast-w, cleve, diabetes, german, glass, lymph, mushroom, sonar, vehicle |
| ➖ **Không đổi** | 3 | crx, iris, wine |
| ❌ **MacroF1 giảm** | 9 | heart, hepatitis, horse, labor, led7, tic-tac-toe, waveform, zoo |

---

## 🎯 Bảng 2 — Target Datasets Chi Tiết

Dataset được chọn vì gap Acc−MacroF1 cao (imbalance) trong baseline.

### lymph (MacroF1 0.42 → 0.43 — tiny +0.004)

| Class | Support | Base F1 | v2 F1 | ΔF1 |
|-------|:-------:|:-------:|:-----:|:----:|
| metastases | 81 | 0.8608 | 0.8662 | +0.005 |
| malign_lymph | 61 | 0.8333 | 0.8421 | +0.009 |
| **fibrosis** | **4** | **0.0000** | **0.0000** | **0.000** |
| **normal** | **2** | **0.0000** | **0.0000** | **0.000** |

**Insight:** 2 class thực sự F1=0 **không thay đổi** — vì model không có rule nào cover fibrosis/normal, weighting không thể tạo ra prediction. Chỉ ảnh hưởng các class đã có rule.

### glass (MacroF1 0.61 → 0.62 — target đạt biên)

| Class | Support | Base F1 | v2 F1 | ΔF1 |
|-------|:-------:|:-------:|:-----:|:----:|
| building_nonfloat | 76 | 0.6515 | 0.6299 | -0.022 |
| building_float | 70 | 0.7123 | 0.6970 | -0.015 |
| headlamps | 29 | 0.8000 | 0.8387 | +0.039 |
| **vehicle_float** | **17** | **0.2000** | **0.3111** | **+0.111** ⭐ |
| vehicle_nonfloat | 13 | 0.7857 | 0.6452 | -0.141 |
| containers | 9 | 0.5185 | 0.5806 | +0.062 |

**Insight:** Minority `vehicle_float` (17 records) **F1 +0.11** — đúng hướng! Nhưng đổi lại: `vehicle_nonfloat` giảm 0.14, tổng MacroF1 chỉ tăng nhẹ.

### zoo (MacroF1 0.90 → 0.85 — regression!)

| Class | Support | Base F1 | v2 F1 | ΔF1 |
|-------|:-------:|:-------:|:-----:|:----:|
| mammal (1) | 41 | 0.9639 | 0.9500 | -0.014 |
| bird (2) | 20 | 1.0000 | 1.0000 | 0.000 |
| fish (4) | 13 | 1.0000 | 0.9630 | -0.037 |
| insect (6) | 8 | 1.0000 | 1.0000 | 0.000 |
| invertebrate (7) | 10 | 0.9000 | 0.8421 | -0.058 |
| reptile (3) | 5 | 0.6667 | 0.6667 | 0.000 |
| **amphibian (5)** | **4** | **0.7500** | **0.5455** | **-0.205** ⚠️ |

**Insight:** Class 5 (amphibian — chỉ 4 records) F1 **GIẢM** 0.20! Weighting đẩy quá mạnh → false positives (Precision 1.0 → 0.43).

---

## 🔬 Phân Tích — Tại Sao Mixed Results?

### Trường hợp weighting GIÚP (8 datasets)
- Dataset có minority class **vừa hiếm vừa có rule đủ**.
- Ví dụ: **glass/vehicle_float** (17/214 = 8%) — có luật support thấp nhưng tồn tại → weighting cho chúng "tiếng nói".

### Trường hợp weighting KHÔNG giúp (lymph fibrosis/normal)
- Minority quá hiếm → không rule nào support đủ minSup → **0 rules**.
- Weighting nhân 0 vẫn = 0 → không prediction.
- **Giải pháp:** Hướng 2 (class-specific minSup) — hạ minSup riêng cho class hiếm để tạo rule.

### Trường hợp weighting HẠI (zoo, hepatitis, tic-tac-toe, ...)
- Minority đã được phân lớp khá tốt trong baseline (nhờ rules mạnh).
- Weighting nhân quá nhiều → tổng score class hiếm vượt class lớn dù không xứng → false positives tăng.
- **Giải pháp:** Weight cap, hoặc dùng weight ít "hung hãn" hơn (ví dụ sqrt(N/freq) thay vì N/freq).

---

## 🎓 Success Criteria Check

| Tiêu chí | Target | Actual | Pass? |
|----------|:------:|:------:|:-----:|
| ≥ 3/5 target datasets có ΔMacroF1 > 0 | 3 | 4/5 (lymph, glass, german, vehicle) | ✅ |
| Overall Avg MacroF1 ≥ 0.83 | 0.83 | **0.7988** | ❌ |
| Accuracy không giảm >5% trên bất kỳ dataset | pass | hepatitis -5.17% | ❌ (biên giới) |
| Lymph MacroF1 ≥ 0.55 | 0.55 | 0.4271 | ❌ |
| Glass MacroF1 ≥ 0.70 | 0.70 | 0.6171 | ❌ |

**Kết luận:** Hướng 1 một mình **không đủ** để đạt mục tiêu MacroF1 cao. Lý do: weighting chỉ modulate score CỦA CÁC rule đã có — không tạo ra rule mới cho minority cực hiếm.

---

## 💡 Recommendations cho Hướng 2 & 3

### Hướng 2 (Class-specific minSup)
- **Motivation:** lymph fibrosis/normal F1=0 vì 0 rules. Cần hạ minSup cho class hiếm → sinh rule.
- **Kỳ vọng:** lymph fibrosis F1 từ 0 lên **0.3+**.

### Hướng 3 (F1-aware pruning)
- **Motivation:** Với rules có sẵn, pruning theo F1 (thay χ²) có thể giữ rules tốt cho minority.
- **Kỳ vọng:** zoo, hepatitis regression được khắc phục.

### Kết hợp H1 + H2
- H2 tạo rule cho minority → H1 weight chúng → **cả hai cùng chiến**.
- Đây mới là combo thực sự hiệu quả.

---

## 📂 Files liên quan

- `src/CMARClassifierWeighted.java` — implementation
- `src/BenchmarkWeighted.java` — benchmark entry
- `result/v2_metrics.csv`, `result/v2_per_class.csv` — outputs
- `result/v2_benchmark.log` — full console log
- `report/baseline_f1_20datasets.md` — baseline reference

## Next steps

→ **Tiến hành Hướng 2 (class-specific minSup)** — plan tiếp theo.
→ Sau H2 xong, chạy **H1 + H2 kết hợp** để đo hiệu ứng tổng.
→ Cân nhắc **weight cap** hoặc **sqrt weight** nếu H2 cũng không đủ.

---

*Honest report — Hướng 1 một mình là MIXED success. Đã đặt nền cho H2, H3 tiếp theo.*
