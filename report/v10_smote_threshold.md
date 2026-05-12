# CMAR v10 — SMOTE-N + Class Score Boost cho Imbalanced Datasets

**Ngày:** 2026-05-11
**Phiên bản:** v10 (Heavy artillery cho lymph / hepatitis / german)
**Cơ sở:** WCBA-Light (H2 class-specific minSup + H3 adaptive minConf, baseline v9)
**Tech mới:** SMOTE-N (oversampling categorical), Class Score Boost (alpha=0.5)

---

## 1. Động lực

Sau v6–v9 (Stratified Top-K, IG weighting, WCBA Strong+Spare, class-weighted χ²), lymph **fibrosis F1 vẫn = 0** ở baseline, hepatitis DIE F1 ~0.59, german bad F1 ~0.50.

**Root cause:** Minority classes có quá ít transactions → mining không sinh được CAR có support đủ cao → classifier trống rules cho minority.

Hai kỹ thuật v10:

1. **SMOTE-N** (Chawla et al. 2002, categorical version): tạo synthetic minority records → balance training distribution **trước khi mining**.
2. **Class Score Boost** ở classify time: `score(c) × (N/(k·freq(c)))^α` — nâng nhẹ score cho minority khi vote.

---

## 2. Thiết kế

### 2.1 SMOTE-N (categorical)
```
For each class c có freq(c) < target = maxFreq × 1.0:
  while freq(c) < target:
    base = random record của class c
    neighbors = k=5 nearest neighbors (Hamming distance)
    synthetic = with each attribute lấy MODE(base, neighbors)
    add synthetic to training
```
- Hamming distance trên `attr=value` items.
- Mode + random tie-break giữ tính ngẫu nhiên hạn chế.
- Edge case: class < 2 records → duplicate đơn giản.

### 2.2 Class Score Boost (alpha)
Tại `CMARClassifier.classify()`:
```java
weight = (N / (k_classes × freq(c)))^alpha
score(c) *= weight
```
α=0.5: moderate. Class minority được nâng score, không thay đổi tập rule mining.

### 2.3 Variants benchmark
| # | Variant | H2/H3 | SMOTE | Boost |
|---|---------|:-----:|:-----:|:-----:|
| 1 | Baseline | - | - | - |
| 2 | WCBA-Light | ✓ | - | - |
| 3 | +SMOTE | ✓ | ✓ | - |
| 4 | +Boost | ✓ | - | ✓ |
| 5 | +BOTH FINAL | ✓ | ✓ | ✓ |

---

## 3. Kết quả MacroF1

| Dataset | Baseline | Light | +SMOTE | +Boost | +BOTH | Best Δ |
|---------|:--------:|:-----:|:------:|:------:|:-----:|:------:|
| **lymph** | 0.4235 | 0.4181 | **0.7445** | 0.4097 | 0.7445 | SMOTE **+0.3209** |
| hepatitis | 0.7363 | **0.7430** | 0.7430 | 0.7337 | 0.7430 | Light +0.0067 |
| german | 0.6639 | **0.6903** | 0.6716 | 0.6663 | 0.6716 | Light +0.0264 |
| **AVG** | 0.6079 | 0.6171 | **0.7197** | 0.6033 | **0.7197** | +0.1118 |

### 3.1 Per-class (highlights)

**LYMPH — breakthrough!**
| Class | sup | Baseline F1 | +SMOTE F1 | Δ |
|-------|:---:|:-----------:|:---------:|:-:|
| metastases | 81 | 0.861 | 0.859 | -0.002 |
| malign_lymph | 61 | 0.833 | 0.819 | -0.014 |
| **fibrosis** | **4** | **0.000** | **0.500** | **+0.500** |
| **normal** | **2** | **0.000** | **0.800** | **+0.800** |

**HEPATITIS**: DIE F1 0.588 → 0.597 (gain marginal).
**GERMAN**:
- bad **Recall** 0.433 → 0.677 (+0.244 với SMOTE)
- nhưng bad **F1** 0.502 → 0.575 (+0.073) — precision giảm.
- good F1 0.826 → 0.768 (-0.058) — đánh đổi.

---

## 4. Phân tích

### 4.1 Tại sao Lymph nổ?
- fibrosis chỉ 4 records, normal chỉ 2 records.
- Baseline mining: minority không đủ support → 0 CAR cho 2 class này → F1 = 0.
- SMOTE balance lên ~80 records mỗi class → mining sinh ra rules → F1 ≈ 0.5–0.8.

### 4.2 Tại sao Hepatitis & German chỉ cải thiện nhẹ?
- **Hepatitis**: DIE đã có 32 records (>20) — đủ cho mining, SMOTE không add nhiều thông tin mới chỉ duplicate patterns đã có.
- **German**: imbalance chỉ 2.3x (700 vs 300) — SMOTE balance lên 1:1 over-sample → nhiều synthetic "bad" patterns quá similar → loss generalization. Recall ↑ nhưng F1 không vượt Light (H2/H3 đã handle good enough).

### 4.3 Boost alone bị Boost không phát huy?
- Boost only (V4) tệ hơn Light (V2): tăng score minority mà không có rules → predict sai class → F1 giảm.
- Boost + SMOTE = SMOTE alone: khi SMOTE đã balance class, weight `N/(k·freq)` ≈ 1 cho mọi class → boost effectively no-op.

→ **Khi đã SMOTE, Boost redundant.**

### 4.4 So sánh Goal vs Actual

| Mục tiêu | Target | Actual | OK? |
|----------|:------:|:------:|:---:|
| lymph MacroF1 | 0.65+ | **0.7445** | ✅ vượt |
| hepatitis MacroF1 | 0.82+ | 0.7430 | ❌ thiếu |
| german MacroF1 | 0.74+ | 0.6903 | ❌ thiếu |
| lymph fibrosis F1 ≥ 0.50 | ✓ | 0.500 | ✅ |
| hepatitis DIE F1 ≥ 0.65 | - | 0.597 | ❌ |
| german bad F1 ≥ 0.55 | - | 0.575 | ✅ vượt |

---

## 5. Kết luận

**SMOTE-N là single best technique cho extreme minority** (fibrosis 4 / normal 2 records lymph): chuyển từ F1=0 thành F1=0.5–0.8.

**WCBA-Light vẫn winner cho moderate imbalance**: hepatitis & german không lợi từ SMOTE (over-sample tạo nhiễu).

**Recommended config cho thesis writeup:**
- Lymph (extreme imbalance, classes < 10 records): **SMOTE on**.
- Hepatitis/German (moderate imbalance): **Light (H2+H3) only**.
- **Adaptive rule:** SMOTE chỉ bật khi `min_class_freq < 10`.

**Class Score Boost (α=0.5):** không nên dùng độc lập, không cần khi SMOTE on → có thể bỏ.

---

## 6. Files thay đổi

| Action | File |
|:------:|------|
| ADD | `src/SMOTE.java` (SMOTE-N categorical, k-NN Hamming + mode) |
| EDIT | `src/CMARClassifier.java` — `setClassScoreBoost(alpha)` + apply trong `classify()` |
| EDIT | `src/CrossValidator.java` — overload với `smoteTargetRatio` |
| ADD | `src/BenchmarkSMOTEThreshold.java` — 5 variants × 3 datasets |
| ADD | `result/v10_{baseline,light,smote,boost,final}_metrics.csv` |
| ADD | `result/v10_benchmark.log` |
| ADD | `report/v10_smote_threshold.md` (this file) |

---

## 7. Next steps gợi ý

1. **Adaptive SMOTE**: chỉ enable khi `min_class_freq < threshold` (e.g., 10).
2. **SMOTE ratio tuning**: 0.5 / 0.75 / 1.0 để giảm noise cho moderate-imbalance datasets.
3. **Borderline-SMOTE / ADASYN**: chỉ over-sample minority records gần decision boundary → ít noise hơn.
4. **Combine với cost-sensitive learning**: weight rules by class cost thay vì balance data.
5. **Final thesis comparison table**: tổng hợp v6 (TopK), v7 (IG), v8 (Strong+Spare), v9 (H2+H3), v10 (SMOTE) → chọn best per dataset.
