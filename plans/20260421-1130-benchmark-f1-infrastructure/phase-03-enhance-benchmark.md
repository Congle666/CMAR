# Phase 03 — Enhance Benchmark with F1 + CSV output

## Context links

- Parent plan: [plan.md](./plan.md)
- Dependencies: Phase 01 (EvalMetrics), Phase 02 (CSV methods in ResultWriter)

## Overview

- **Date:** 2026-04-21
- **Description:** Modify `CrossValidator.run()` to collect per-fold `EvalMetrics` (not just accuracy). Modify `Benchmark.java` to aggregate metrics across datasets and emit 2 CSV files.
- **Priority:** High
- **Implementation status:** Planned
- **Review status:** Not reviewed

## Key Insights

- `CrossValidator.run()` currently returns `double[]` (per-fold accuracies). We need the full metric picture per fold.
- Backwards compatibility: some callers may rely on the current signature — provide **overload** instead of breaking change.
- CSV format is stable — once generated, can be compared with future runs (V2, V3, V4 from improvements) using pandas/Excel.

## Requirements

- `CrossValidator.run()` new overload: returns `List<EvalMetrics>` (one per fold).
- `Benchmark.java`:
  - Uses new overload.
  - Averages metrics per dataset using `EvalMetrics.average()`.
  - Writes `result/baseline_metrics.csv` and `result/baseline_per_class.csv`.
  - Console output adds Macro-F1 column next to Accuracy.

## Architecture

### CrossValidator changes

```java
// NEW overload — returns full metrics per fold
public static List<EvalMetrics> runWithMetrics(
    List<Transaction> data, int k,
    double minSupportPct, double minConfidence,
    double chiSqThreshold, int coverageDelta,
    long seed, int maxPatternLength)

// Existing methods preserved (call new one internally + return accuracy[])
public static double[] run(...) { ... derives accs[] from runWithMetrics() ... }
```

### Benchmark changes

```java
// Aggregate: Map<datasetName, EvalMetrics-averaged-over-folds>
Map<String, EvalMetrics> aggregated = new LinkedHashMap<>();

for each dataset:
    List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(...)
    EvalMetrics avg = EvalMetrics.average(foldMetrics);
    aggregated.put(name, avg);
    print row (include Macro-F1)

// At end:
ResultWriter.writeMetricsCsv(aggregated, "result/baseline_metrics.csv");
ResultWriter.writePerClassCsv(aggregated, "result/baseline_per_class.csv");
```

### CSV layout

`baseline_metrics.csv` (1 row per dataset, 20 rows total):
```csv
dataset,records,classes,minSupPct,accuracy,accStd,macroF1,weightedF1,paperCMAR,paperCBA,paperC45
iris,150,3,0.01,0.9533,0.0521,0.9530,0.9533,94.00,94.67,95.33
...
```

`baseline_per_class.csv` (1 row per (dataset, class) — ~80–100 rows total):
```csv
dataset,class,support,tp,fp,fn,precision,recall,f1
car,acc,384,117,68,267,0.632,0.305,0.411
car,unacc,1210,1208,270,2,0.817,0.998,0.898
car,good,69,0,0,69,0.000,0.000,0.000
car,vgood,65,0,0,65,0.000,0.000,0.000
zoo,mammal,41,41,0,0,1.000,1.000,1.000
...
```

## Related code files

- EDIT: `src/CrossValidator.java` — add `runWithMetrics()` overload
- EDIT: `src/Benchmark.java` — use new overload, collect metrics, emit CSV
- EDIT: `src/ResultWriter.java` — add 2 CSV writer methods (from Phase 02)

## Implementation Steps

1. In `CrossValidator.java`:
   - Add `runWithMetrics()` method that:
     - Trains classifier per fold (same as before)
     - Calls `classifier.predict(testData)` → gets `List<String> predictions`
     - Calls `EvalMetrics.compute(testData, predictions)` → gets fold's metrics
     - Returns `List<EvalMetrics>` (size = k)
   - Keep existing `run()` → delegates to `runWithMetrics()` and extracts `accuracy` values:
     ```java
     public static double[] run(...) {
         List<EvalMetrics> metrics = runWithMetrics(...);
         return metrics.stream().mapToDouble(m -> m.accuracy).toArray();
     }
     ```
2. In `Benchmark.java`:
   - Change result collection: aggregate `EvalMetrics` per dataset.
   - Add Macro-F1 column to console table:
     ```
     Dataset      N    supPct  Acc    MacroF1  Std    Paper  CBA    C4.5
     ```
   - After main loop: call `ResultWriter.writeMetricsCsv()` and `writePerClassCsv()`.
   - Update SUMMARY to include avg Macro-F1.
3. Compile + smoke test: run benchmark on just iris first.

## Todo list

- [ ] Add `CrossValidator.runWithMetrics()`
- [ ] Delegate existing `run()` to new method
- [ ] Update `Benchmark.java` to collect aggregated metrics
- [ ] Add Macro-F1 to console output
- [ ] Write CSV output at end of Benchmark
- [ ] Smoke test with 1 dataset (iris)

## Success Criteria

- Compile clean.
- `java -cp out Benchmark` runs without errors.
- 2 CSV files generated in `result/` with expected headers.
- Per-class F1 values match those produced by `java Main` on same data (sanity check via `car.csv`).

## Risk Assessment

- **Risk:** Changed `CrossValidator` signature breaks `BenchmarkOne.java`.
  - **Mitigation:** Keep original `run()` working via delegation. No breaking change.
- **Risk:** Console output width exceeds terminal.
  - **Mitigation:** Abbreviate column labels (`MF1` for Macro-F1), or use 2 lines per dataset.
- **Risk:** CSV output encoding issues on Windows.
  - **Mitigation:** Use `BufferedWriter` with explicit UTF-8 (no BOM) charset.

## Security Considerations

N/A.

## Next steps

→ Phase 04: Run the enhanced benchmark on all 20 datasets, collect results.
