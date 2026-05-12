# SMOTE-N + Class Threshold Combo (v10)

**Created:** 2026-05-11 17:51
**Type:** Heavy artillery for imbalanced datasets

## Goal

Cải thiện F1/Recall trên cả 3 truly imbalanced datasets (lymph, hepatitis, **german**) bằng cách kết hợp 2 kỹ thuật chưa thử:

1. **SMOTE-N** (Synthetic Minority Oversampling cho dữ liệu categorical)
   - Tạo synthetic minority records → balance training data
   - Resolves root cause: thiếu minority records cho mining
2. **Class Score Boost** (Threshold Adjustment tại classify time)
   - Nhân score(minority) với inverse-frequency weight
   - Tăng Recall minority (đánh đổi với Precision)

## Hai Kỹ Thuật

### SMOTE-N Algorithm

```
For each minority class c with freq(c) < target:
    For each record r in class c:
        Find k=5 nearest neighbors of r in class c (Hamming distance)
        Generate synthetic record:
            For each attribute:
                value = MODE of [r[attr]] + [n[attr] for n in neighbors]
        Add to training set
    Repeat until freq(c) >= target
```

**target = min(majority_freq, freq(c) × 2)** — chọn min để tránh over-sample.

### Class Score Boost

```
adjusted_score(c) = score(c) × (N / (k × freq(c)))^alpha
```

`alpha` ∈ [0, 1]:
- 0: disabled (baseline)
- 0.5: moderate boost
- 1.0: full balanced boost

## Phases

| # | Phase | Status |
|:-:|------|:------:|
| 01 | SMOTE-N implementation | Planned |
| 02 | Class Score Boost in CMARClassifier | Planned |
| 03 | Wire to CrossValidator | Planned |
| 04 | BenchmarkSMOTEThreshold | Planned |
| 05 | Run + analyze | Planned |
| 06 | Thesis MD | Planned |

## Files

| Action | File |
|:------:|------|
| ADD | `src/SMOTE.java` |
| EDIT | `src/CMARClassifier.java` — setClassScoreBoost() + apply in classify() |
| EDIT | `src/CrossValidator.java` — add useSMOTE param |
| ADD | `src/BenchmarkSMOTEThreshold.java` |
| ADD | `result/v10_*_metrics.csv` |
| ADD | `report/v10_smote_threshold.md` |

## Success Criteria

- All 3 datasets cải thiện MacroF1 hoặc per-class F1 minority
- **German bad F1 ≥ 0.55** (baseline 0.50)
- **Lymph fibrosis F1 ≥ 0.50** (baseline 0.00, hiện tại best 0.67 v6)
- **Hepatitis DIE F1 ≥ 0.65** (baseline 0.59)

## Realistic Expected Results

| Dataset | Current Best | Target |
|---------|:------------:|:------:|
| lymph MacroF1 | 0.59 | **0.65+** |
| hepatitis MacroF1 | 0.78 | **0.82+** |
| german MacroF1 | 0.69 | **0.74+** |
