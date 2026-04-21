# Phase 02 — Refactor ResultWriter to use EvalMetrics

## Context links

- Parent plan: [plan.md](./plan.md)
- Dependencies: Phase 01 (EvalMetrics.java must exist)

## Overview

- **Date:** 2026-04-21
- **Description:** Remove duplicated metric calculation logic in `ResultWriter.java` (currently in 2 places). Replace with calls to `EvalMetrics.compute()`. Add optional CSV output methods.
- **Priority:** Medium
- **Implementation status:** Planned
- **Review status:** Not reviewed

## Key Insights

- `ResultWriter.writeEvaluation()` and `writeCMARResult()` both re-implement TP/FP/FN/P/R/F1 logic (~15 lines each).
- After refactor, each will be ~3 lines calling `EvalMetrics.compute()`.
- Output format stays identical — byte-for-byte compatible with existing `result/evaluation.txt` to avoid regression noise.

## Requirements

- Behavior preserved: `java Main data/car.csv` must produce **identical** `result/evaluation.txt` as before.
- Add 2 NEW methods for CSV output (needed by Phase 03):
  - `writeMetricsCsv(List<EvalMetrics> perDataset, String filePath)` — 1 row per dataset
  - `writePerClassCsv(Map<String, EvalMetrics> perDataset, String filePath)` — 1 row per (dataset, class) pair
- Keep existing signatures of `writeEvaluation()` and `writeCMARResult()` unchanged (API-compatible).

## Architecture

```
ResultWriter.writeEvaluation(testData, predictions, path)
  └─ EvalMetrics m = EvalMetrics.compute(testData, predictions)
  └─ format m into existing plaintext format

ResultWriter.writeCMARResult(... testData, predictions ...)
  └─ same replacement in the duplicated block
```

New methods for Phase 03:

```
ResultWriter.writeMetricsCsv(Map<String, EvalMetrics> byDataset, path)
  → "dataset,records,classes,accuracy,accStd,macroF1,weightedF1" header + rows

ResultWriter.writePerClassCsv(Map<String, EvalMetrics> byDataset, path)
  → "dataset,class,support,tp,fp,fn,precision,recall,f1" header + rows
```

## Related code files

- EDIT: `src/ResultWriter.java`
  - lines 76–132 (`writeEvaluation`) — replace metric block with EvalMetrics call
  - lines 293–370 (`writeCMARResult`) — same pattern, inside the evaluation section
- NEW methods at end of class

## Implementation Steps

1. In `writeEvaluation()`:
   - Replace lines 83–106 (class collection + TP/FP/FN counting) with:
     ```java
     EvalMetrics metrics = EvalMetrics.compute(testData, predictions);
     ```
   - Replace lines 117–130 (per-class loop + macro-F1 calc) with iteration over `metrics.perClass.values()`.
   - Keep header, table format, and `Macro-F1:` line byte-identical.
2. In `writeCMARResult()`: same replacement in the evaluation block (~lines 343–370).
3. Add 2 new CSV writer methods at end of class:
   - `writeMetricsCsv(Map<String, EvalMetrics>, String)` — header + 1 row per dataset.
   - `writePerClassCsv(Map<String, EvalMetrics>, String)` — header + 1 row per class per dataset.
4. Compile + regression test: run `java Main data/car.csv`, diff new `evaluation.txt` against pre-refactor version — must match exactly.

## Todo list

- [ ] Refactor `writeEvaluation()` to use EvalMetrics
- [ ] Refactor `writeCMARResult()` to use EvalMetrics
- [ ] Add `writeMetricsCsv()` method
- [ ] Add `writePerClassCsv()` method
- [ ] Regression test: diff `result/evaluation.txt` before/after (must be identical)
- [ ] Regression test: diff `result/cmar_result.txt` before/after (must be identical)

## Success Criteria

- Compile clean.
- `diff result/evaluation.txt backup/evaluation.txt` → no differences (except optional timestamp).
- `diff result/cmar_result.txt backup/cmar_result.txt` → no differences.
- Line count in `ResultWriter.java` reduced (duplication removed).

## Risk Assessment

- **Risk:** Subtle format differences (trailing newlines, number formatting).
  - **Mitigation:** Backup existing output files before refactor, diff at end. Use `String.format` identical to original (`%.4f`, same field widths).
- **Risk:** Breaking other callers of `writeEvaluation()`.
  - **Mitigation:** Signature unchanged. No caller changes needed.

## Security Considerations

N/A.

## Next steps

→ Phase 03: Enhance `Benchmark.java` and `CrossValidator.java` to collect and emit F1 metrics using these new utilities.
