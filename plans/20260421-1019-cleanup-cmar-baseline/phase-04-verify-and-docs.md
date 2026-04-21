# Phase 04 — Verification & Docs Update

## Context

- **Parent plan:** [../plan.md](./plan.md)
- **Previous phase:** [phase-03-reorganize-subfolders.md](./phase-03-reorganize-subfolders.md) (must be complete)
- **Next phase:** End of this cleanup plan. The next body of work — F1/Recall improvements — will be a **separate plan** authored after this one is merged.

## Overview

- **Date:** 2026-04-21
- **Description:** Full end-to-end validation of the cleaned baseline, plus documentation refresh. Produces a clean "ready for improvement" state.
- **Priority:** Medium-High (the code already works after Phase 03; this phase is about documentation and confidence)
- **Implementation Status:** Planned
- **Review Status:** Pending

## Key Insights

- `README.md` is currently near-empty (6 bytes); populating it with build/run commands is a quick win.
- Vietnamese user guides `report/huong_dan_chay.md` and `report/huong_dan_su_dung.md` likely reference `src/*.java` — must update to new subfolder layout.
- A one-line `build.sh` saves future contributors from memorizing the new compile command (optional — see Unresolved Q3 in plan.md).

## Requirements

1. Full regression run on `data/car.csv` (default) + 3 extra datasets (iris, zoo, mushroom_full).
2. Accuracy numbers for car/iris/zoo match Phase-01 baseline within 0.0 tolerance (same seed, deterministic).
3. README.md populated with updated build + run instructions reflecting Option B layout.
4. `huong_dan_chay.md` and `huong_dan_su_dung.md` updated to reference new paths.
5. Optional: `build.sh` and `run.sh` helpers added (approve with user first).

## Architecture

No code changes. Documentation + optional shell helpers only.

```
d:/CMAR/
├── README.md          # rewritten with new commands
├── build.sh           # (optional) wraps: javac -d out $(find src -name "*.java")
├── run.sh             # (optional) wraps: java -cp out Main "$@"
└── report/
    ├── huong_dan_chay.md      # updated
    └── huong_dan_su_dung.md   # updated
```

## Related Code Files

- `d:/CMAR/README.md`
- `d:/CMAR/report/huong_dan_chay.md`
- `d:/CMAR/report/huong_dan_su_dung.md`
- `d:/CMAR/report/so_sanh_bai_bao_va_cai_dat.md` (scan for src/ path refs)

## Implementation Steps

1. **Full regression — default dataset.**
   ```bash
   rm -rf out
   javac -d out $(find src -name "*.java")
   java -cp out Main 2>&1 | tee /tmp/phase4.main.log
   (cd result && sha256sum *.txt) > /tmp/checksums.phase4.txt
   diff plans/20260421-1019-cleanup-cmar-baseline/baseline/checksums.txt /tmp/checksums.phase4.txt
   ```
   Expected: identical, or timestamp-only diff.

2. **Regression on extra datasets.** For each of `iris.csv`, `zoo.csv`, `mushroom_full.csv`, run Main if it accepts a path arg, otherwise run Benchmark and extract per-dataset rows.
   ```bash
   java -cp out Benchmark 2>&1 | tee /tmp/phase4.benchmark.log
   ```
   Compare the `car`, `iris`, `zoo`, `mushroom` rows against `baseline/benchmark.before.log`.

3. **Rewrite `README.md`.** Replace current near-empty content with:
   - Project name + one-line description.
   - Layout diagram (copy from plan.md).
   - Build command: `javac -d out $(find src -name "*.java")`.
   - Run command: `java -cp out Main`.
   - Benchmark command: `java -cp out Benchmark`.
   - Pointer to the paper PDF in repo root.

4. **Update `report/huong_dan_chay.md` + `huong_dan_su_dung.md`.**
   Search both files for old flat-path references and replace with subfolder paths:
   ```bash
   grep -n "src/" report/huong_dan_chay.md report/huong_dan_su_dung.md
   ```
   Update any `src/FooBar.java` references to `src/<subfolder>/FooBar.java` (use mapping from phase-03). Also update compile commands if they appear.

5. **Optional — add `build.sh` and `run.sh`.** Only if user approves (see Unresolved Q3):
   ```bash
   # build.sh
   #!/usr/bin/env bash
   set -e
   rm -rf out
   javac -d out $(find src -name "*.java")
   ```
   ```bash
   # run.sh
   #!/usr/bin/env bash
   exec java -cp out Main "$@"
   ```
   Make executable: `chmod +x build.sh run.sh`.

6. **Final verification — delete `out/`, rebuild from scratch, run.**
   ```bash
   rm -rf out result/*.txt
   ./build.sh   # or manual javac
   ./run.sh     # or manual java
   ls result/   # confirm all expected outputs regenerated
   ```

7. **Commit docs.**
   ```bash
   git add README.md report/huong_dan_chay.md report/huong_dan_su_dung.md
   # include build.sh run.sh only if added
   git commit -m "docs(cleanup): update README + Vietnamese guides for new src/ layout"
   ```

8. **Tag the clean baseline.**
   ```bash
   git tag -a cmar-baseline-clean-2026-04-21 -m "Clean CMAR baseline — ready for F1/Recall improvements"
   ```

## Todo

- [ ] Regression on `data/car.csv` matches baseline checksums
- [ ] Benchmark regression on iris/zoo/mushroom_full matches baseline accuracy
- [ ] `README.md` rewritten with new build/run commands
- [ ] `report/huong_dan_chay.md` updated
- [ ] `report/huong_dan_su_dung.md` updated
- [ ] `report/so_sanh_bai_bao_va_cai_dat.md` scanned for stale path refs
- [ ] (Optional) `build.sh` + `run.sh` added and executable
- [ ] Full clean rebuild succeeds from scratch
- [ ] Docs commit created
- [ ] Tag `cmar-baseline-clean-2026-04-21` created

## Success Criteria

- `java -cp out Main` produces outputs identical to Phase-01 baseline.
- Benchmark per-dataset accuracy for car, iris, zoo, mushroom_full matches baseline exactly (deterministic algorithm + fixed seeds).
- New contributor can clone, read `README.md`, and run `./build.sh && ./run.sh` (or equivalent two-liner) without asking questions.
- Clean-baseline tag is in `git tag --list`.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Benchmark numbers differ due to nondeterminism (e.g., `HashMap` iteration) | Low | Medium | Check Main/Benchmark code for `Math.random()` or `HashMap` ordering; if present, acceptable to tolerate 1e-6 diff and record note in README |
| Vietnamese markdown files have broken formatting after path edits | Low | Low | Preview in editor after edit; restore from git if needed |
| `find` output ordering differs between runs → compile-time constant inlining varies | Very Low | Negligible | Java compilation is order-independent for the default package |

## Security Considerations

N/A — documentation and build-script additions only; no secrets, no network calls.

## Rollback

- Docs commit can be reverted atomically: `git revert --no-edit HEAD`.
- Tag removal (if needed): `git tag -d cmar-baseline-clean-2026-04-21`.
- Full plan rollback: `git reset --hard pre-cleanup-2026-04-21`.

## Next Steps

This closes the cleanup plan. The follow-up plan for **F1/Recall improvements on the CMAR baseline** will start from tag `cmar-baseline-clean-2026-04-21` and should be authored in a new dated folder under `plans/`.
