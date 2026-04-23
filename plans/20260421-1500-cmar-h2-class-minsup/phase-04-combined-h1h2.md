# Phase 04 — Combined H1+H2 Benchmark (Optional)

## Context links

- Parent: [plan.md](./plan.md)
- Deps: Phase 03

## Overview

- **Date:** 2026-04-21
- **Description:** Run combined Hướng 1 (weighted classify) + Hướng 2 (per-class minSup) benchmark. Per-class mining GENERATES rules for minority; weighted classify boosts minority SCORES.
- **Priority:** Medium (optional but valuable for final report)
- **Status:** Planned

## Key Insights

- H1 alone: weighted score but no rules for fibrosis → F1 still 0.
- H2 alone: rules exist for fibrosis but majority-biased score → still low F1.
- **H1+H2 together** should compound: rules exist AND score is weighted. Expected best result.

## Requirements

- New entry `BenchmarkH1H2.java`:
  - Factory: `CMARClassifierWeighted::new` (H1)
  - `classMinSupFraction = minSupPct` (H2)
  - Output `v4_metrics.csv`, `v4_per_class.csv`
- Report `report/comparison_v4_combined.md` comparing:
  - Baseline (v1)
  - H1 alone (v2)
  - H2 alone (v3)
  - H1+H2 (v4)
- 4-way table per dataset showing MacroF1.

## Architecture

Trivial — compose existing factory + CV overload.

```java
public class BenchmarkH1H2 {
    public static void main(String[] args) throws Exception {
        // Same loop as BenchmarkClassSup but with CMARClassifierWeighted::new
        ...
        CrossValidator.runWithMetrics(
            data, K, supPct, minConfidence, chiSqThreshold, coverageDelta,
            seed, maxPatLen,
            CMARClassifierWeighted::new,    // H1
            supPct);                         // H2
        ...
    }
}
```

## Related code files

- NEW: `src/BenchmarkH1H2.java`
- NEW: `result/v4_metrics.csv`, `result/v4_per_class.csv`
- NEW: `report/comparison_v4_combined.md`

## Implementation Steps

1. Create BenchmarkH1H2.java (copy BenchmarkClassSup, switch factory).
2. Recompile.
3. Run: `java -Xmx512m -cp out BenchmarkH1H2 2>&1 | tee result/v4_benchmark.log`
4. Write 4-way comparison report.
5. Declare final verdict based on cumulative improvement.

## Todo list

- [ ] Create BenchmarkH1H2
- [ ] Run full benchmark
- [ ] Write comparison report with 4 variants
- [ ] Document final improvement number

## Success Criteria

- All 20 datasets complete
- `lymph/fibrosis` F1 > 0 in v4
- Overall MacroF1 best among all 4 variants
- Report clearly shows compound effect

## Risk Assessment

- **Risk:** Over-aggressive weighting + too many minority rules → false-positive explosion.
  - **Mitigation:** Honest reporting. May need future weight capping.

## Security Considerations

N/A.

## Next steps

→ Plan: `plans/YYYYMMDD-HHmm-cmar-h3-f1-pruning` (Hướng 3).
→ Final synthesis report across all improvements.
