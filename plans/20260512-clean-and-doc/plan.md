# v12 — Aggressive Cleanup + H2/H3/SMOTE Documentation

**Created:** 2026-05-12
**Goal:** Aggressive code cleanup (mức 3) + write comprehensive MD explaining H2/H3/SMOTE.

## Rationale

v11 đã chứng minh: **final config chỉ cần H2 + H3 + Adaptive SMOTE**. Tất cả features còn lại (HM ranking, Spare rules, Class weighting, Stratified Top-K, Threshold adjust, IG weighting) **không nằm trong final config** → dead code → xoá.

## Phases

| # | Phase | Status |
|:-:|------|:------:|
| 01 | Delete 9 legacy benchmark files + 4 untracked | Pending |
| 02 | CMARClassifier — remove 6 unused features | Pending |
| 03 | Delete CMARClassifierWeighted + AttributeWeights | Pending |
| 04 | CrossValidator — consolidate overloads, remove useWCBAWeights | Pending |
| 05 | Delete legacy report MDs (~25 files) | Pending |
| 06 | Write `report/H2_H3_SMOTE_explained.md` | Pending |
| 07 | Compile + sanity benchmark | Pending |

## Files affected

### DELETE benchmark files (13 total)
Legacy: `BenchmarkAdaptiveConf`, `BenchmarkAll`, `BenchmarkClassSup`, `BenchmarkH2H3`, `BenchmarkHMTopK`, `BenchmarkOne`, `BenchmarkStratifiedGrid`, `BenchmarkWCBA`, `BenchmarkWeighted`.
Untracked: `BenchmarkImbalanced`, `BenchmarkPhase1`, `BenchmarkPhase2`, `BenchmarkTopK`, `ClassDistributionReport`.

### DELETE core (2)
`CMARClassifierWeighted.java`, `AttributeWeights.java`.

### EDIT (2)
`CMARClassifier.java` — remove 6 fields + setters + logic.
`CrossValidator.java` — remove `useWCBAWeights` param + AttributeWeights usage.

### EDIT (1)
`FPGrowth.java` — remove `setAttributeWeights` if unused.

### KEEP (final core)
`Benchmark.java` (baseline), `BenchmarkSMOTEFull.java` (v11 final), `SMOTE.java`, `CMARClassifier.java`, `CrossValidator.java`, `FPGrowth.java`, `CRTree.java`, `DatasetLoader.java`, `EvalMetrics.java`, `Transaction.java`, `AssociationRule.java`, `FPTree.java`, `FPNode.java`, `FrequentPattern.java`, `ResultWriter.java`, `Main.java`.

### DELETE report MDs
Keep only: `v10_smote_threshold.md`, `v11_smote_full_uci.md`, `bao_cao_thuat_toan_CMAR.md`, `baseline_f1_20datasets.md`.
Plus: NEW `H2_H3_SMOTE_explained.md`.

## Success Criteria

- Compile clean (zero errors)
- BenchmarkSMOTEFull still produces same final v11 numbers
- MD file đầy đủ về H2/H3/SMOTE với honest analysis
