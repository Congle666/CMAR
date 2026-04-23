# Phase 02 — CrossValidator overload + BenchmarkClassSup

## Context links

- Parent: [plan.md](./plan.md)
- Dep: Phase 01

## Overview

- **Date:** 2026-04-21
- **Description:** Add `CrossValidator` overload that pre-computes class-specific minSup from training data and sets it on `FPGrowth`. Add `BenchmarkClassSup.java` entry point.
- **Priority:** High
- **Status:** Planned

## Key Insights

- Class frequencies only known at runtime per fold (training set changes).
- Compute `classMinSup(c) = max(2, round(fraction × freq(c)))` inside each fold.
- `fraction` param = `minSupportPct` reuses user's existing config.
- BenchmarkClassSup = near-copy of Benchmark with one extra param.

## Requirements

- CrossValidator overload: `runWithMetrics(..., double classMinSupFraction, Supplier<CMARClassifier> factory)`.
- When `classMinSupFraction <= 0`: behave like existing overload (no per-class logic).
- Output CSV paths: `v3_metrics.csv`, `v3_per_class.csv`.
- Global `minSup` for item pruning = unchanged (preserves tractability).

## Architecture

### CrossValidator new overload

```java
public static List<EvalMetrics> runWithMetrics(
        List<Transaction> data, int k,
        double minSupportPct, double minConfidence,
        double chiSqThreshold, int coverageDelta,
        long seed, int maxPatternLength,
        Supplier<CMARClassifier> classifierFactory,
        double classMinSupFraction) {
    // ... same splits as before ...
    for each fold:
        FPGrowth fp = new FPGrowth(globalMinSup);
        if (classMinSupFraction > 0) {
            Map<String, Integer> classCounts = countClasses(trainData);
            Map<String, Integer> classMinSup = new HashMap<>();
            for (String c : classCounts.keySet()) {
                int threshold = Math.max(2,
                    (int) Math.round(classMinSupFraction * classCounts.get(c)));
                classMinSup.put(c, threshold);
            }
            fp.setClassMinSupMap(classMinSup);
        }
        // ... rest unchanged
}
```

### BenchmarkClassSup.java

Copy of Benchmark with:
- Title: "CMAR Benchmark — Class-specific minSup (Hướng 2)"
- Call: `CrossValidator.runWithMetrics(..., CMARClassifier::new, minSupPct)`
  — reuses user's minSupPct as the per-class fraction
- CSV paths: `v3_metrics.csv`, `v3_per_class.csv`

## Related code files

- EDIT: `src/CrossValidator.java`
- NEW: `src/BenchmarkClassSup.java`

## Implementation Steps

1. In `CrossValidator.java`:
   - Add overload with `double classMinSupFraction` param.
   - Existing overloads delegate with `classMinSupFraction = 0` (no-op).
2. Implement class-count + per-class threshold computation inside fold loop.
3. Create `src/BenchmarkClassSup.java`:
   - Near-copy of Benchmark.java
   - Pass `supPct` as `classMinSupFraction`
   - Output `v3_*.csv`
4. Compile.
5. Smoke test on lymph — verify classMinSup was set (log it).

## Todo list

- [ ] Add CrossValidator overload
- [ ] Delegate existing methods with fraction=0
- [ ] Pre-compute classMinSup per fold
- [ ] Call FPGrowth.setClassMinSupMap
- [ ] Create BenchmarkClassSup.java
- [ ] Compile check
- [ ] Smoke test on lymph

## Success Criteria

- Compile clean
- BenchmarkClassSup runs on lymph and emits CARs for fibrosis/normal (check via print or log)
- No regression on Benchmark (baseline unchanged)

## Risk Assessment

- **Risk:** Per-fold classMinSup compute adds overhead.
  - **Mitigation:** Negligible (one Map iteration per fold).
- **Risk:** BenchmarkClassSup mining explodes on large datasets.
  - **Mitigation:** Global minSup unchanged. Only emission check differs. Should be same order-of-magnitude runtime as baseline.

## Security Considerations

N/A.

## Next steps

→ Phase 03: Full run + comparison.
