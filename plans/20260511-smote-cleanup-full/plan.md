# v11 — SMOTE Cleanup + Full UCI Benchmark

**Created:** 2026-05-11 18:30
**Goal:** Cleanup unused Boost code (KISS), validate SMOTE on full UCI suite (20 datasets)

## Rationale

v10 chứng minh:
- SMOTE alone = SMOTE + Boost trên 3 imbalanced datasets (lymph, hep, german)
- Boost redundant khi đã SMOTE
- → Remove Boost code để giảm complexity (KISS)

Mở rộng test: SMOTE adaptive trên 20 UCI datasets để có thesis-grade evidence.

## Phases

| # | Phase | Status |
|:-:|------|:------:|
| 01 | Remove Boost code (CMARClassifier) | Pending |
| 02 | BenchmarkSMOTEFull (20 datasets, adaptive SMOTE) | Pending |
| 03 | Run + analyze | Pending |
| 04 | Thesis MD v11_smote_full_uci.md | Pending |

## Design

### Adaptive SMOTE
SMOTE bật chỉ khi `min_class_freq < SMOTE_TRIGGER (=10)`. Tránh over-sample khi không cần.

### 3 Variants × 20 datasets
1. Baseline (no H2/H3, no SMOTE)
2. Light (H2 + H3 — class-specific minSup + adaptive minConf)
3. Light+SMOTE (H2 + H3 + adaptive SMOTE)

### Metrics
Accuracy, MacroF1, MacroRecall, per-class F1/Recall.

## Files

| Action | File |
|:------:|------|
| EDIT | `src/CMARClassifier.java` (remove boost field/setter/apply) |
| ADD | `src/BenchmarkSMOTEFull.java` |
| ADD | `result/v11_*_metrics.csv` + `*_per_class.csv` |
| ADD | `report/v11_smote_full_uci.md` |

## Success Criteria

- Compile clean, no boost references
- ≥15/20 datasets không bị regress vs Light
- Imbalanced datasets (lymph, glass, zoo, vehicle) cải thiện minority F1
