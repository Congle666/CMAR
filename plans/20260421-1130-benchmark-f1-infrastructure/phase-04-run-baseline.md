# Phase 04 — Run Baseline on 20 Datasets

## Context links

- Parent plan: [plan.md](./plan.md)
- Dependencies: Phase 03 (Benchmark emits CSV)

## Overview

- **Date:** 2026-04-21
- **Description:** Execute full `Benchmark` run over all 20 UCI datasets with 10-fold stratified CV. Capture output CSV and logs.
- **Priority:** High
- **Implementation status:** Planned
- **Review status:** Not reviewed

## Key Insights

- Full benchmark takes **5–15 minutes** total depending on dataset size (mushroom, waveform largest).
- Heap size: use `-Xmx512m` as per existing `huong_dan_chay.md`.
- `seed = 42` for reproducibility.
- Any crashes or OOM on specific datasets → log and continue (don't abort entire run).

## Requirements

- Generate both CSVs:
  - `result/baseline_metrics.csv`
  - `result/baseline_per_class.csv`
- Capture stdout to `result/baseline_benchmark.log`
- All 20 datasets should complete (if OOM on mushroom or similar — already known, raise minSupport per existing benchmark config).

## Architecture

No code changes — just execution.

## Related code files

- Read-only: `src/Benchmark.java` (entry point)
- Output: `result/baseline_metrics.csv`, `result/baseline_per_class.csv`, `result/baseline_benchmark.log`

## Implementation Steps

1. Clean `out/` and recompile fresh: `rm -rf out && mkdir out && javac -d out src/*.java`
2. Run benchmark with heap + redirect:
   ```bash
   java -Xmx512m -cp out Benchmark 2>&1 | tee result/baseline_benchmark.log
   ```
3. Verify both CSV files exist and are non-empty:
   ```bash
   wc -l result/baseline_metrics.csv result/baseline_per_class.csv
   head -3 result/baseline_metrics.csv
   ```
4. If any dataset failed/skipped, note in Phase 05 report.
5. Backup CSVs (git add, they'll be committed at end).

## Todo list

- [ ] Recompile cleanly
- [ ] Run `java -cp out Benchmark` and capture log
- [ ] Verify CSV files exist
- [ ] Sanity-check CSV contents (20 rows in metrics.csv, ~80+ rows in per_class.csv)
- [ ] Note any OOM/errors

## Success Criteria

- `baseline_metrics.csv` has 20 data rows + header
- `baseline_per_class.csv` has ≥60 data rows + header (20 datasets × avg 4 classes)
- No Java exceptions in log except the noted OOM cases (if any)
- Log file captures full console output

## Risk Assessment

- **Risk:** OutOfMemoryError on large datasets (mushroom, waveform) at low minSup.
  - **Mitigation:** Existing `DATASETS` array already tunes minSup per dataset. Keep as-is.
- **Risk:** Runtime takes >30 min on slow machines.
  - **Mitigation:** Accept — this is a one-shot baseline. Run in background with `nohup` if needed.
- **Risk:** Floating-point variation between runs.
  - **Mitigation:** `seed = 42` fixed. Results are deterministic for same seed.

## Security Considerations

N/A.

## Next steps

→ Phase 05: Analyze CSV data and write `baseline_f1_20datasets.md` markdown report.
