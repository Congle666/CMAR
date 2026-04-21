# Benchmark F1 Infrastructure

**Created:** 2026-04-21 12:26
**Status:** Planned
**Type:** Infrastructure (no algorithm changes)
**Prerequisite for:** CMAR F1/Recall improvements (Hướng 1, 2, 3)

## Goal

Establish a **benchmark infrastructure** that captures **Macro-F1, Weighted-F1, per-class Precision/Recall/F1** for all 20 UCI datasets. This becomes the "ground truth before" snapshot — every future improvement will be measured against this baseline.

## Why this first?

Current `Benchmark.java` only logs **Accuracy**. The 3 planned improvements (class-weighted χ², class-specific minSup, F1-aware pruning) target **minority class performance** — which Accuracy **hides**.

Evidence from `result/evaluation.txt` on `car.csv`:
- Accuracy 75.14% looks OK
- BUT minority classes `good` (16 records) and `vgood` (15 records) have **F1 = 0.0** — completely ignored by model!
- Macro-F1 = **0.31** (terrible)

Without F1 metrics in benchmark, we can't prove improvements work.

## Scope

- Extract metric calculation into reusable class
- Add F1 + per-class metrics to `Benchmark.java`
- Emit CSV output for structured comparison later
- Generate `baseline_f1_20datasets.md` snapshot report
- **NO algorithm changes** — baseline CMAR stays identical

## Out of Scope

- The 3 improvements themselves (separate plans later)
- Adding new datasets
- Rewriting classifiers

## Phases

| # | Phase | File | Status |
|---|-------|------|--------|
| 01 | Create EvalMetrics utility class | [phase-01-eval-metrics.md](./phase-01-eval-metrics.md) | Planned |
| 02 | Refactor ResultWriter to use EvalMetrics | [phase-02-refactor-resultwriter.md](./phase-02-refactor-resultwriter.md) | Planned |
| 03 | Enhance Benchmark with F1 + CSV output | [phase-03-enhance-benchmark.md](./phase-03-enhance-benchmark.md) | Planned |
| 04 | Run baseline on 20 datasets | [phase-04-run-baseline.md](./phase-04-run-baseline.md) | Planned |
| 05 | Write baseline report | [phase-05-write-baseline-report.md](./phase-05-write-baseline-report.md) | Planned |

## Files Changed/Added/Deleted

| Action | File | Notes |
|:------:|------|-------|
| ADD | `src/EvalMetrics.java` | New — reusable metric computation |
| EDIT | `src/ResultWriter.java` | Replace inline metric logic with EvalMetrics calls |
| EDIT | `src/CrossValidator.java` | Return per-fold metrics (not just accuracy) |
| EDIT | `src/Benchmark.java` | Collect F1, write CSV output |
| ADD | `result/baseline_metrics.csv` | Generated output |
| ADD | `result/baseline_per_class.csv` | Generated output |
| ADD | `report/baseline_f1_20datasets.md` | Snapshot report |

**Nothing deleted.** Pure additive + refactor.

## Success Criteria

- `src/*.java` compiles with zero warnings
- `java Main data/car.csv` still gives 75.14% accuracy (unchanged)
- `java Benchmark` produces 2 CSV files with F1 data
- `baseline_f1_20datasets.md` shows Macro-F1 per dataset
- Git commit with clear message + tag `baseline-f1-v1`

## Estimated Effort

2–3 hours total.

## Next Plan (after this)

`plans/YYYYMMDD-HHmm-cmar-improvement-h1-weighted-chi2` — implement Hướng 1 using this infrastructure.
