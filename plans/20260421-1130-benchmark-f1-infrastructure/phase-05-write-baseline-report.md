# Phase 05 — Write Baseline F1 Report

## Context links

- Parent plan: [plan.md](./plan.md)
- Dependencies: Phase 04 (CSV files generated)

## Overview

- **Date:** 2026-04-21
- **Description:** Consume `baseline_metrics.csv` and `baseline_per_class.csv` to produce a human-readable markdown report `report/baseline_f1_20datasets.md`. Highlight datasets where Macro-F1 diverges significantly from Accuracy — these are candidates where Hướng 1/2/3 improvements will have biggest impact.
- **Priority:** Medium
- **Implementation status:** Planned
- **Review status:** Not reviewed

## Key Insights

- Accuracy vs Macro-F1 divergence is the **signal for class imbalance**.
- Example: `car.csv` has Accuracy 75% but Macro-F1 0.31 — minority classes `good`/`vgood` have F1 = 0.
- The report will rank datasets by imbalance severity → justifies why F1/Recall improvements are needed.

## Requirements

- Markdown report at `report/baseline_f1_20datasets.md`.
- Contains 4 main tables:
  1. **Summary table** — 20 datasets with Accuracy, Macro-F1, Weighted-F1, delta-vs-paper.
  2. **Imbalance severity ranking** — Top 5 datasets with largest (Accuracy - Macro-F1) gap.
  3. **Per-class F1 = 0 inventory** — classes that are completely missed by the model.
  4. **Paper comparison** — our Macro-F1 vs their Accuracy (no Macro-F1 in paper but we flag it).
- Narrative sections:
  - Executive summary (3–5 bullets)
  - Interpretation (why minority matters)
  - Candidate improvements (how Hướng 1/2/3 address these)

## Related code files

- Source data: `result/baseline_metrics.csv`, `result/baseline_per_class.csv`
- Target: `report/baseline_f1_20datasets.md`

## Implementation Steps

1. Parse `baseline_metrics.csv` into memory.
2. Write report with this structure:

```markdown
# Baseline F1 Snapshot — CMAR on 20 UCI Datasets

**Date:** 2026-04-21
**Commit:** <git rev>
**Seed:** 42, 10-fold stratified CV, minSup per dataset (see Benchmark.java)

## Executive Summary
- Tested X datasets, collected full Precision/Recall/F1 per class.
- Avg Accuracy: __ %, Avg Macro-F1: __, Avg Weighted-F1: __
- Y datasets have Accuracy >> Macro-F1 gap (severe imbalance).
- Z classes across all datasets have F1 = 0 (never predicted correctly).

## 1. Summary Table (all 20)
| Dataset | N | Cls | Acc | MacroF1 | WeightedF1 | Paper | Δ |
...

## 2. Imbalance Severity (top 5, Acc - MacroF1)
| Dataset | Acc | MacroF1 | Gap |
| car     | 0.75 | 0.31   | 0.44 |
...

## 3. Classes with F1 = 0 (inventory)
| Dataset | Class | Support | Reason |
| car     | good  | 16      | only 16 records, swamped by unacc (1210) |
...

## 4. Candidate Improvements
- Hướng 1 (class-weighted χ²) — directly addresses cases 2 & 3.
- Hướng 2 (class-specific minSup) — tackles the root (not enough rules for minority).
- Hướng 3 (F1-aware pruning) — complementary.
```

3. Add commentary tying numbers back to the 3 planned improvements.
4. Include a small "next steps" section at the end linking to the forthcoming improvement plans.

## Todo list

- [ ] Parse CSV and build summary tables manually (visual inspection OK — 20 rows)
- [ ] Identify F1=0 classes
- [ ] Compute Acc-MacroF1 gap, rank top 5
- [ ] Draft narrative sections
- [ ] Cross-link to plan.md and future improvement plans

## Success Criteria

- `report/baseline_f1_20datasets.md` exists, renders as valid markdown.
- All 20 datasets listed with accuracy + macroF1 + weightedF1.
- At least 5 datasets identified as imbalance candidates.
- At least 10 class-F1=0 cases listed (expected from car, glass, zoo).
- Cross-references to improvement plans.

## Risk Assessment

- **Risk:** CSV parsing errors (locale, decimal comma vs dot).
  - **Mitigation:** Ensure Benchmark writes with `Locale.US` (dot decimal separator).
- **Risk:** Report becomes too verbose.
  - **Mitigation:** Keep narrative under 200 lines; tables are the value.

## Security Considerations

N/A.

## Next steps

→ Wrap up infrastructure phase. Commit everything with tag `baseline-f1-v1`.
→ Next plan: `plans/YYYYMMDD-HHmm-cmar-improvement-h1-weighted-chi2` — implement Hướng 1.
