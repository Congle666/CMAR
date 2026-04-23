# CMAR Hướng 2 — Class-specific minSup

**Created:** 2026-04-21 15:00
**Status:** Planned
**Type:** Algorithm improvement (F1/Recall for minority classes)
**Baseline ref:** Tag `baseline-f1-v1` + `v2-weighted-chi2`

## Goal

Enable CMAR to generate rules for **extremely rare minority classes** by using **per-class minSup** instead of a single global threshold. Solves the honest finding from Hướng 1: *"weighted classification can't rescue classes with ZERO rules"*.

## Why

Baseline + H1 finding (`report/comparison_v2_vs_baseline.md`):
- `lymph/fibrosis` (4 records): F1 = 0.00 in baseline AND v2 (weighting doesn't help).
- `lymph/normal` (2 records): F1 = 0.00 in both.
- Root cause: with global minSup = 7 on 148-record dataset, no rule can have sup ≥ 7 for a 4-record class.

## Approach

Replace global `minSup` with **per-class threshold**:

```
minSup(c) = max(2, round(minSupPct × freq(c)))
```

**Example on lymph (minSupPct=5%):**
| Class | freq | Old minSup | New minSup |
|-------|:----:|:----------:|:----------:|
| metastases | 81 | 7 | 4 |
| malign_lymph | 61 | 7 | 3 |
| fibrosis | 4 | 7 | **2** ✓ |
| normal | 2 | 7 | **2** ✓ |

Mining: global minSup for item-level pruning kept (tractability). Rule emission: per-class threshold used instead of global.

## Expected Results

| Dataset | Target improvement |
|---------|-------------------|
| **lymph/fibrosis** | F1 0.00 → **≥ 0.30** |
| **lymph/normal** | F1 0.00 → **≥ 0.20** |
| lymph MacroF1 | 0.42 → **≥ 0.55** |
| zoo class 3, 5 | Slight improvement |
| Overall Avg MacroF1 | 0.80 → **≥ 0.83** |

## Scope

- Modify `FPGrowth.java` — support optional `Map<String, Integer> classMinSupMap`
- Modify `CrossValidator.java` — add overload that computes class-specific thresholds from trainData
- Add `BenchmarkClassSup.java` — benchmark entry using per-class minSup (with baseline classifier)
- (Optional) `BenchmarkH1H2.java` — combined weighted classify + per-class mining
- Generate `result/v3_metrics.csv`, `result/v3_per_class.csv`
- Write `report/comparison_v3_vs_baseline.md`

## Out of Scope

- Changing CR-tree structure
- Changing pruning logic (Hướng 3)
- Adaptive per-fold tuning

## Phases

| # | Phase | File | Status |
|---|-------|------|--------|
| 01 | Modify FPGrowth for per-class minSup | [phase-01-fpgrowth-class-minsup.md](./phase-01-fpgrowth-class-minsup.md) | Planned |
| 02 | CrossValidator overload + BenchmarkClassSup | [phase-02-cv-and-benchmark.md](./phase-02-cv-and-benchmark.md) | Planned |
| 03 | Run benchmark + comparison report | [phase-03-run-compare.md](./phase-03-run-compare.md) | Planned |
| 04 | (Optional) Combined H1+H2 | [phase-04-combined-h1h2.md](./phase-04-combined-h1h2.md) | Planned |

## Files Changed/Added

| Action | File | Notes |
|:------:|------|-------|
| EDIT | `src/FPGrowth.java` | add classMinSup map + setter, modify emission check |
| EDIT | `src/CrossValidator.java` | add overload with classMinSupFraction |
| ADD | `src/BenchmarkClassSup.java` | benchmark using class-specific minSup |
| ADD | `src/BenchmarkH1H2.java` | (optional) combined |
| ADD | `result/v3_metrics.csv` | generated |
| ADD | `result/v3_per_class.csv` | generated |
| ADD | `report/comparison_v3_vs_baseline.md` | delta report |

## Success Criteria

- Compile clean
- `java Main data/car.csv` baseline unchanged at 75.14% (no regression)
- `BenchmarkClassSup` completes all 20 datasets
- `lymph/fibrosis` F1 > 0.0 (even small improvement proves concept)
- Overall Avg MacroF1 ≥ baseline's 0.8034 (not required to meet 0.85 solo)
- Git tag `v3-class-minsup`

## Estimated Effort

2 hours.

## Next Plan

`plans/YYYYMMDD-HHmm-cmar-h3-f1-pruning` — F1-aware pruning (Hướng 3).
