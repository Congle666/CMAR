# Phase 03 — Classifier Factory + BenchmarkWeighted

## Context links

- Parent: [plan.md](./plan.md)
- Deps: Phase 01, 02

## Overview

- **Date:** 2026-04-21
- **Description:** Refactor `CrossValidator.runWithMetrics()` to accept a classifier factory. Add `BenchmarkWeighted.java` that runs same 20 datasets using `CMARClassifierWeighted`.
- **Priority:** High
- **Status:** Planned

## Key Insights

- `CrossValidator` currently hardcodes `new CMARClassifier()` — can't swap implementations.
- Solution: accept `Supplier<CMARClassifier>` factory parameter.
- Keep backwards compat: existing overloads default to `CMARClassifier::new`.
- `BenchmarkWeighted` is near-copy of `Benchmark` with factory passed + different output CSV paths.

## Requirements

- Non-breaking change: existing `CrossValidator.runWithMetrics(...)` signatures preserved; internally delegate to new factory overload.
- `CMARClassifier::new` as default factory.
- CSV outputs must distinguish: `v2_metrics.csv` vs baseline `baseline_metrics.csv`.

## Architecture

### CrossValidator overload

```java
import java.util.function.Supplier;

public static List<EvalMetrics> runWithMetrics(
        List<Transaction> data, int k,
        double minSupportPct, double minConfidence,
        double chiSqThreshold, int coverageDelta,
        long seed, int maxPatternLength,
        Supplier<CMARClassifier> classifierFactory) {
    // ... same logic, but use factory.get() instead of new CMARClassifier()
}

// Existing overloads delegate with CMARClassifier::new
public static List<EvalMetrics> runWithMetrics(
        List<Transaction> data, int k, double minSupportPct,
        double minConfidence, double chiSqThreshold, int coverageDelta,
        long seed, int maxPatternLength) {
    return runWithMetrics(data, k, minSupportPct, minConfidence,
        chiSqThreshold, coverageDelta, seed, maxPatternLength,
        CMARClassifier::new);
}
```

### BenchmarkWeighted.java

Near-copy of `Benchmark.java` with:
- Title: "CMAR Benchmark — Weighted χ² (Hướng 1)"
- Call: `CrossValidator.runWithMetrics(..., CMARClassifierWeighted::new)`
- CSV output paths:
  - `result/v2_metrics.csv`
  - `result/v2_per_class.csv`

## Related code files

- EDIT: `src/CrossValidator.java` — add factory overload
- NEW: `src/BenchmarkWeighted.java` — copy Benchmark with factory swap

## Implementation Steps

1. In `CrossValidator.java`:
   - Import `java.util.function.Supplier`.
   - Add new overload with `Supplier<CMARClassifier> classifierFactory`.
   - Implement: replace `new CMARClassifier()` → `classifierFactory.get()`.
   - Update old overloads to delegate with `CMARClassifier::new`.
2. Create `src/BenchmarkWeighted.java`:
   - Copy `Benchmark.java`.
   - Rename class → `BenchmarkWeighted`.
   - Change output path constants to `v2_*`.
   - Update title in console output.
   - Replace call to `runWithMetrics` with factory version: `..., CMARClassifierWeighted::new`.
3. Compile check.
4. Smoke test on 1 dataset first:
   - Temporary comment-out most of DATASETS array (keep just iris)
   - Run BenchmarkWeighted
   - Verify CSV output correctness
   - Uncomment datasets

## Todo list

- [ ] Add Supplier factory overload to CrossValidator
- [ ] Default old overloads to CMARClassifier::new
- [ ] Create BenchmarkWeighted.java
- [ ] Change output paths to v2_*
- [ ] Compile check
- [ ] Smoke test on iris only
- [ ] (Leave full 20-dataset run to phase 04)

## Success Criteria

- Compile clean.
- `java -cp out Benchmark` still works (backwards compat).
- `java -cp out BenchmarkWeighted` produces CSV files on smoke-test dataset.
- iris weighted result comparable to baseline (±1 record) — confirms no break on balanced data.

## Risk Assessment

- **Risk:** Method overload ambiguity.
  - **Mitigation:** Explicitly typed parameter lists — Java resolves correctly.
- **Risk:** Default classifier pinned to baseline if caller forgets to pass factory.
  - **Mitigation:** That's the intended behavior — backward compat.

## Security Considerations

N/A.

## Next steps

→ Phase 04: Full benchmark + comparison report.
