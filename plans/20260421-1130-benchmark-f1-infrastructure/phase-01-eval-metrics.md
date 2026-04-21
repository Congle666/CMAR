# Phase 01 — Create EvalMetrics Utility Class

## Context links

- Parent plan: [plan.md](./plan.md)
- Dependencies: none (foundation phase)

## Overview

- **Date:** 2026-04-21
- **Description:** Create a reusable `EvalMetrics.java` class with static methods to compute classification metrics from `(testData, predictions)` pairs. Includes per-class TP/FP/FN/Precision/Recall/F1 and aggregated Accuracy/Macro-F1/Weighted-F1.
- **Priority:** High (blocks phase 02)
- **Implementation status:** Planned
- **Review status:** Not reviewed

## Key Insights

- Current metric logic is **duplicated** in 2 places in `ResultWriter.java` (lines 117–130 and 343–357 per grep) — violates DRY.
- Extracting it into a utility class:
  - Removes duplication
  - Enables Benchmark.java to compute the same metrics per fold
  - Makes future improvements (weighted-F1, ROC) easier to add

## Requirements

- Pure static utility — no state, no side effects.
- Input: `List<Transaction> testData`, `List<String> predictions`.
- Output: an `EvalMetrics` POJO with nested `ClassMetrics` per class.
- Zero dependencies on CMARClassifier (evaluation is dataset-agnostic).

## Architecture

```java
public final class EvalMetrics {
    public double accuracy;
    public double macroF1;
    public double weightedF1;
    public Map<String, ClassMetrics> perClass;   // ordered by class appearance

    public static final class ClassMetrics {
        public String className;
        public int tp, fp, fn;
        public int support;          // tp + fn (actual count of this class in testData)
        public double precision;
        public double recall;
        public double f1;
    }

    /** Computes all metrics. Returns populated EvalMetrics. */
    public static EvalMetrics compute(List<Transaction> testData, List<String> predictions) { ... }

    /** Sum per-fold metrics into average (for CV reporting). Not used in Phase 01 but planned. */
    public static EvalMetrics average(List<EvalMetrics> folds) { ... }
}
```

## Related code files

- NEW: `src/EvalMetrics.java`
- Reference: `src/ResultWriter.java:76-132` (existing eval logic)

## Implementation Steps

1. Create `src/EvalMetrics.java` with the two inner classes above.
2. Implement `compute(testData, predictions)`:
   - Collect classes in order of first appearance (LinkedHashSet).
   - For each (actual, predicted) pair, increment TP/FP/FN buckets.
   - For each class: compute Precision/Recall/F1 using safe-division.
   - Compute:
     - `accuracy = total_tp / |testData|`
     - `macroF1 = mean(f1 per class)`
     - `weightedF1 = sum(f1 * support) / sum(support)`
3. Implement `average(folds)`:
   - Average accuracy, macroF1, weightedF1 across folds.
   - Per class: sum tp/fp/fn across folds, then recompute P/R/F1 on summed counts (micro-like). **This is called "micro over folds"** — documented in javadoc.
4. Add javadoc to every public symbol.
5. Compile check: `javac -d out src/*.java`.

## Todo list

- [ ] Create `src/EvalMetrics.java` skeleton
- [ ] Implement `compute()` method
- [ ] Implement `average()` method
- [ ] Add javadoc
- [ ] Verify compile

## Success Criteria

- File compiles with no warnings.
- `javac -d out src/*.java` exit code 0.
- No breaking change to existing code (since new file only).

## Risk Assessment

- **Risk:** Division by zero in edge cases (empty test set, class with 0 TP+FP).
  - **Mitigation:** Safe-division helpers returning 0.0 when denominator is 0.
- **Risk:** `average()` semantics unclear (macro-average of macros vs micro-over-folds).
  - **Mitigation:** Document clearly in javadoc. Use micro-over-folds (sum counts, then compute) for stability with small folds.

## Security Considerations

N/A — pure computation, no I/O, no user input.

## Next steps

→ Phase 02: Refactor `ResultWriter.java` to call `EvalMetrics.compute()` instead of inline logic.
