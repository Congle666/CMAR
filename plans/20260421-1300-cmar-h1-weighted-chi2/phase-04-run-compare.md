# Phase 04 — Run Weighted Benchmark + Comparison Report

## Context links

- Parent: [plan.md](./plan.md)
- Deps: Phase 03 (BenchmarkWeighted ready)

## Overview

- **Date:** 2026-04-21
- **Description:** Execute full `BenchmarkWeighted` on all 20 datasets. Generate comparison report showing baseline vs v2 delta.
- **Priority:** High
- **Status:** Planned

## Key Insights

- Runtime: ~same as baseline benchmark (5–15 min). Training unchanged; only classify() differs.
- `seed=42` same → fold splits identical → direct comparison valid.
- Save CSVs to `v2_*.csv` (baseline stays at `baseline_*.csv`).

## Requirements

- Generate: `result/v2_metrics.csv`, `result/v2_per_class.csv`, `result/v2_benchmark.log`
- Write `report/comparison_v2_vs_baseline.md` with:
  - Side-by-side table for all 20 datasets
  - Delta columns (ΔMacroF1, ΔAccuracy, ΔWeightedF1)
  - Highlighted winners/losers
  - Per-class delta for 5 target datasets (lymph, glass, german, vehicle, hepatitis)
  - Overall verdict

## Architecture

No code changes. Just run + analyze.

### Comparison report structure

```markdown
# Hướng 1 vs Baseline — Class-weighted χ² Results

## Summary
- Avg MacroF1: 0.80 → 0.XX (Δ +0.XX)
- Avg Accuracy: 84.20 → 84.XX (Δ -X.XX)
- Datasets improved: X/20
- Best improvement: lymph (+0.XX)
- Worst regression: (dataset) (-0.XX)

## Table — All 20 datasets
| Dataset | Baseline | Weighted | ΔAcc | ΔMacroF1 | ΔWF1 | Winner |
...

## Per-class Delta — Target Datasets
### lymph
| Class | Baseline F1 | Weighted F1 | ΔF1 |
| fibrosis | 0.00 | 0.XX | +0.XX |
...

## Verdict
- [ ] Target 1: lymph MacroF1 ≥ 0.55? YES/NO
- [ ] Target 2: glass MacroF1 ≥ 0.70? YES/NO
...
```

## Related code files

- Read-only: `result/baseline_metrics.csv`, `result/baseline_per_class.csv`, `result/v2_metrics.csv`, `result/v2_per_class.csv`
- Write: `report/comparison_v2_vs_baseline.md`

## Implementation Steps

1. Clean: `rm -rf out && mkdir out && javac -d out src/*.java`
2. Run benchmark with log:
   ```bash
   java -Xmx512m -cp out BenchmarkWeighted 2>&1 | tee result/v2_benchmark.log
   ```
3. Verify CSVs exist (21 rows metrics, ≥65 per_class).
4. Parse baseline + v2 CSVs, compute delta.
5. Draft report with tables.
6. Flag pass/fail for each success criterion.
7. Commit everything.

## Todo list

- [ ] Recompile cleanly
- [ ] Run BenchmarkWeighted, capture log
- [ ] Verify CSV outputs
- [ ] Compute deltas (baseline vs v2)
- [ ] Draft comparison report with all 4 sections
- [ ] Check success criteria: target MacroF1 improvements, no regression >3%
- [ ] Commit with tag v2-weighted-chi2

## Success Criteria

- All 20 datasets completed in v2 run.
- At least 3 of {lymph, glass, german, hepatitis, zoo} show ΔMacroF1 > 0.
- Overall Avg MacroF1 improvement ≥ 0.02 (0.80 → 0.82+).
- No dataset regresses ΔAccuracy > -5%.
- Report includes per-class delta for target datasets showing where F1 improved.

## Risk Assessment

- **Risk:** Weighting hurts precision on some datasets (false positives jump).
  - **Mitigation:** This is expected trade-off. Report honestly; don't hide losses.
- **Risk:** On already-balanced datasets, no change or slight regression.
  - **Mitigation:** Expected — weight formula naturally reduces to 1.0 when balanced. Any regression comes from float rounding.
- **Risk:** Target MacroF1 thresholds not met.
  - **Mitigation:** Report honestly. If fails, plan next step (maybe Hướng 2 gives what's missing). This is research, not guaranteed results.

## Security Considerations

N/A.

## Next steps

→ Commit with tag `v2-weighted-chi2`.
→ New plan: `plans/YYYYMMDD-HHmm-cmar-h2-class-specific-minsup`.
