# Phase 03 — Run BenchmarkClassSup + Comparison Report

## Context links

- Parent: [plan.md](./plan.md)
- Deps: Phase 01, 02

## Overview

- **Date:** 2026-04-21
- **Description:** Run full 10-fold CV benchmark using class-specific minSup on all 20 UCI datasets. Generate comparison report vs baseline.
- **Priority:** High
- **Status:** Planned

## Key Insights

- Same datasets, seed, K as baseline → direct comparison valid.
- Focus on `lymph` per-class F1 (fibrosis, normal) as primary success signal.

## Requirements

- Run: `java -Xmx512m -cp out BenchmarkClassSup` → `result/v3_*.csv`
- Report with:
  - Summary stats (Avg Acc, Macro-F1 delta)
  - Per-class F1 change on lymph, glass, zoo
  - Rule count delta (H2 may generate more rules — measure cost)
  - Verdict

## Architecture

No code change.

## Related code files

- Output: `result/v3_metrics.csv`, `result/v3_per_class.csv`, `result/v3_benchmark.log`
- Target: `report/comparison_v3_vs_baseline.md`

## Implementation Steps

1. Recompile fresh: `rm -rf out && mkdir out && javac -d out src/*.java`
2. Run: `java -Xmx512m -cp out BenchmarkClassSup 2>&1 | tee result/v3_benchmark.log`
3. Verify CSV outputs (20 rows, ~65 per-class rows)
4. Parse + compute delta vs baseline
5. Write comparison report with same structure as `comparison_v2_vs_baseline.md`

## Todo list

- [ ] Clean compile
- [ ] Run BenchmarkClassSup with log
- [ ] Verify CSVs
- [ ] Extract lymph fibrosis/normal F1 — primary signal
- [ ] Write comparison report
- [ ] Honest verdict

## Success Criteria

- All 20 datasets complete
- `lymph/fibrosis` F1 > 0 (even F1 > 0.10 validates the approach)
- No regression > 5% accuracy on any dataset vs baseline
- Report includes honest mixed-results analysis

## Risk Assessment

- **Risk:** Mining time increases significantly.
  - **Mitigation:** Only emission check changes; mining structure same. Expect similar runtime.
- **Risk:** Too many low-quality rules for minority → noise.
  - **Mitigation:** 3-tier pruning (unchanged) filters noise. χ² test should catch weak rules.

## Security Considerations

N/A.

## Next steps

→ Phase 04 (optional): Combined H1+H2 benchmark.
→ Commit with tag `v3-class-minsup`.
