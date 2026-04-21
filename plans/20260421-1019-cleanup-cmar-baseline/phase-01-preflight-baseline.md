# Phase 01 — Pre-flight & Safety Baseline

## Context

- **Parent plan:** [../plan.md](./plan.md)
- **Dependencies:** none (first phase)
- **Research:** `research/researcher-01-source-analysis.md`
- **Next phase:** [phase-02-delete-dead-files.md](./phase-02-delete-dead-files.md)

## Overview

- **Date:** 2026-04-21
- **Description:** Capture the working state before any changes so every later phase can be verified or rolled back.
- **Priority:** High (blocks all other phases)
- **Implementation Status:** Planned
- **Review Status:** Pending

## Key Insights

- Repo has no build system and no tests — the only regression signal is the output files under `result/` and `report/benchmark_20_datasets.md`.
- Main.java reads `data/car.csv` by default (confirmed from research-01, line 69 of Main).
- `.gitignore` already excludes `out/`, `*.class`, `*.log` — no pollution concerns.

## Requirements

1. Git working tree clean (or intentionally dirty — record state).
2. Safety tag + backup branch exists on `main`.
3. "Before" output artifacts are stored under `plans/20260421-1019-cleanup-cmar-baseline/baseline/`.
4. Benchmark numbers for 3 sample datasets (car, iris, zoo) recorded.

## Architecture

No code changes. Artifact capture only. Baseline folder layout:

```
plans/20260421-1019-cleanup-cmar-baseline/
└── baseline/
    ├── result.before/          # copy of result/*.txt after Main run
    ├── benchmark.before.md     # copy of report/benchmark_20_datasets.md
    ├── checksums.txt           # sha256 of every result/*.txt
    └── run.log                 # stdout of Main + Benchmark runs
```

## Related Code Files

- `d:/CMAR/src/Main.java` — entry point, reads `data/car.csv`.
- `d:/CMAR/src/Benchmark.java` — 10-fold CV harness.
- `d:/CMAR/result/` — output to snapshot.
- `d:/CMAR/report/benchmark_20_datasets.md` — accuracy table to compare later.

## Implementation Steps

1. **Confirm clean working tree.** From `d:/CMAR/`:
   ```bash
   git status
   ```
   Expected: only untracked `report/bao_cao_*.md` files (already listed in opening git status). Do not commit them as part of this plan.

2. **Create safety tag + backup branch.**
   ```bash
   git tag pre-cleanup-2026-04-21
   git branch backup/pre-cleanup-2026-04-21
   ```

3. **Create baseline snapshot folder.**
   ```bash
   mkdir -p plans/20260421-1019-cleanup-cmar-baseline/baseline
   ```

4. **Compile current code.**
   ```bash
   rm -rf out && javac -d out src/*.java 2>&1 | tee plans/20260421-1019-cleanup-cmar-baseline/baseline/compile.before.log
   ```
   Expect zero errors.

5. **Run Main on default dataset and capture stdout.**
   ```bash
   java -cp out Main 2>&1 | tee plans/20260421-1019-cleanup-cmar-baseline/baseline/run.log
   ```

6. **Snapshot result/ folder + checksum.**
   ```bash
   cp -r result plans/20260421-1019-cleanup-cmar-baseline/baseline/result.before
   (cd result && sha256sum *.txt) > plans/20260421-1019-cleanup-cmar-baseline/baseline/checksums.txt
   ```

7. **Snapshot current benchmark report.**
   ```bash
   cp report/benchmark_20_datasets.md plans/20260421-1019-cleanup-cmar-baseline/baseline/benchmark.before.md
   ```

8. **Spot-run Benchmark on 3 datasets** (optional but strongly recommended if Benchmark.java accepts a dataset arg; otherwise run the full benchmark once and note car/iris/zoo rows). Save output:
   ```bash
   java -cp out Benchmark 2>&1 | tee plans/20260421-1019-cleanup-cmar-baseline/baseline/benchmark.before.log
   ```
   Extract accuracy for `car`, `iris`, `zoo` rows and write them to `baseline/accuracy.before.txt` (manual copy if needed).

9. **Commit baseline snapshot.**
   ```bash
   git add plans/20260421-1019-cleanup-cmar-baseline/baseline
   git commit -m "chore(cleanup): snapshot baseline outputs before cleanup"
   ```

## Todo

- [ ] `git status` clean (or recorded)
- [ ] Tag `pre-cleanup-2026-04-21` created
- [ ] Backup branch `backup/pre-cleanup-2026-04-21` created
- [ ] `baseline/` folder created
- [ ] `compile.before.log` captured (no errors)
- [ ] `run.log` captured from `Main`
- [ ] `result.before/` copy made + `checksums.txt` written
- [ ] `benchmark.before.md` copied
- [ ] `benchmark.before.log` captured (optional)
- [ ] Baseline snapshot committed

## Success Criteria

- Tag `pre-cleanup-2026-04-21` visible via `git tag --list`.
- `baseline/checksums.txt` lists sha256 of every file in `result/`.
- Running `java -cp out Main` completes without exception.
- Baseline commit exists on `main`.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Main run fails on current code | Low | High | Abort plan; investigate pre-existing bug first |
| Benchmark run takes too long | Medium | Low | Skip full bench; record only car/iris/zoo manually |
| Untracked `report/bao_cao_*.md` files get mixed in | Low | Low | Do not `git add -A`; add specific paths only |

## Security Considerations

N/A — local-only restructuring task, no credentials or external services touched.

## Rollback

Nothing to roll back (no files modified). If the baseline commit is unwanted:
```bash
git reset --soft HEAD~1
```

## Next Steps

Proceed to [phase-02-delete-dead-files.md](./phase-02-delete-dead-files.md).
