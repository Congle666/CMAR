# CMAR Hướng 1 — Class-weighted χ²

**Created:** 2026-04-21 13:00
**Status:** Planned
**Type:** Algorithm improvement (F1/Recall focus)
**Baseline ref:** Tag `baseline-f1-v1` (commit a79b40a)

## Goal

Improve CMAR's **Macro-F1** on class-imbalanced datasets by weighting the Weighted-χ² score with **inverse class frequency** — giving minority classes a fair chance against majority.

## Why

Baseline evidence (`report/baseline_f1_20datasets.md`):
- `lymph` — Acc 83% but Macro-F1 0.42 (2/4 classes F1=0)
- `glass`, `german`, `hepatitis`, `zoo`, `vehicle` — all show Acc >> Macro-F1 gap ≥ 0.03
- Model biased toward majority class

## Approach (minimal invasive)

**Formula change** — inside `classify()` only:

Original (baseline):
```
score(c) = Σ [χ²(R)² / maxχ²(R)]  for R ∈ R(t) with class c
```

Weighted (v2):
```
score(c) = weight(c) × Σ [χ²(R)² / maxχ²(R)]
weight(c) = N / (k × freq(c))          ← sklearn "balanced" formula
```

Nothing else changes — mining, pruning, CR-tree all identical. Only the **prediction step** is weighted.

## Expected Results

| Dataset | Current MacroF1 | Target MacroF1 |
|---------|:---------------:|:--------------:|
| lymph | 0.42 | **≥ 0.55** |
| glass | 0.61 | **≥ 0.70** |
| german | 0.66 | **≥ 0.75** |
| Overall avg | 0.80 | **≥ 0.83** |

**Trade-off accepted:** Accuracy may drop 1–3% on balanced datasets (iris, mushroom, wine) — sanity check they stay ≥ 90%.

## Scope

- Add `CMARClassifierWeighted.java` extending baseline classifier (override `classify()` only)
- Make baseline's private methods `protected` to allow subclass access
- Add factory pattern to `CrossValidator.runWithMetrics()` so benchmarks can plug classifier
- Add `BenchmarkWeighted.java` entry point
- Generate `result/v2_metrics.csv`, `result/v2_per_class.csv`
- Write `report/comparison_v2_vs_baseline.md` with delta analysis

## Out of Scope

- Changing mining / pruning (Hướng 2, 3)
- Hyperparameter tuning of weights
- Other weight formulas (could explore later)

## Phases

| # | Phase | File | Status |
|---|-------|------|--------|
| 01 | Make baseline methods protected | [phase-01-base-protected.md](./phase-01-base-protected.md) | Planned |
| 02 | Implement CMARClassifierWeighted | [phase-02-weighted-classifier.md](./phase-02-weighted-classifier.md) | Planned |
| 03 | Factory pattern in CrossValidator + BenchmarkWeighted | [phase-03-factory-benchmark.md](./phase-03-factory-benchmark.md) | Planned |
| 04 | Run benchmark + write comparison report | [phase-04-run-compare.md](./phase-04-run-compare.md) | Planned |

## Files Changed/Added

| Action | File | Notes |
|:------:|------|-------|
| EDIT | `src/CMARClassifier.java` | private → protected for 3 methods |
| ADD | `src/CMARClassifierWeighted.java` | new subclass, override classify() only |
| EDIT | `src/CrossValidator.java` | add classifier factory overload |
| ADD | `src/BenchmarkWeighted.java` | weighted-version benchmark entry |
| ADD | `result/v2_metrics.csv` | generated |
| ADD | `result/v2_per_class.csv` | generated |
| ADD | `result/v2_benchmark.log` | generated |
| ADD | `report/comparison_v2_vs_baseline.md` | delta report |

**Nothing deleted.** Purely additive + minimal visibility change.

## Success Criteria

- Compile clean.
- `java Main data/car.csv` baseline accuracy **unchanged** at 75.14% (no regression).
- `java -cp out MainWeighted data/car.csv` runs and produces predictions.
- `java -cp out BenchmarkWeighted` completes all 20 datasets.
- `v2_metrics.csv` has 20 rows.
- At least **3 of 5 target datasets** (lymph, glass, german, hepatitis, zoo) show Macro-F1 improvement.
- Overall avg Macro-F1 increases from 0.8034 to ≥ 0.83.
- Commit with tag `v2-weighted-chi2`.

## Estimated Effort

1–2 hours total.

## Next Plan (after this)

`plans/YYYYMMDD-HHmm-cmar-h2-class-specific-minsup` — implement Hướng 2.
