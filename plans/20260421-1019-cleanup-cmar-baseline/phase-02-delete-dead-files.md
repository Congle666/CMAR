# Phase 02 — Delete Dead & Preprocessing Files

## Context

- **Parent plan:** [../plan.md](./plan.md)
- **Previous phase:** [phase-01-preflight-baseline.md](./phase-01-preflight-baseline.md) (must be complete)
- **Next phase:** [phase-03-reorganize-subfolders.md](./phase-03-reorganize-subfolders.md)
- **Research:** `research/researcher-01-source-analysis.md` sections 2, 6, 7

## Overview

- **Date:** 2026-04-21
- **Description:** Remove files that no longer serve the CMAR baseline: dataset-specific preprocessing scripts (already applied or replaced by runtime handling), dead code, and a stray Python artifact.
- **Priority:** High
- **Implementation Status:** Planned
- **Review Status:** Pending

## Key Insights

- `RuleGenerator.java` is **dead code** — declared but never invoked. `FPGrowth.mine()` emits `AssociationRule` directly (grep confirms no caller).
- `DiscretizeGlass.java` + `GermanPreprocessor.java` already produced `glass.csv` / `german_disc.csv` — rerunning is unneeded.
- `FixHorse.java`, `FixHorse2.java` — user explicitly asked to delete; horse.csv still has `?` tokens but that is handled at load-time (or not used in current baseline).
- `data/discretize_glass.py` — Python file inside Java project's data folder; unrelated to runtime.
- After deletion, source count drops from 20 → 15 files.

## Requirements

1. Delete 5 `.java` files + 1 `.py` file in **a single commit per deletion round** so each can be reverted atomically.
2. After each deletion, `javac -d out src/*.java` must still succeed.
3. After all deletions, `java -cp out Main` must produce output matching Phase-01 checksums (except for timestamp lines if any).

## Architecture

Net file removal only. No moves, no edits. Target `src/` after this phase:

```
src/  (15 files, still flat)
├── AssociationRule.java
├── Benchmark.java
├── BenchmarkOne.java
├── CMARClassifier.java
├── CRTree.java
├── CrossValidator.java
├── DatasetLoader.java
├── FPGrowth.java
├── FPNode.java
├── FPTree.java
├── FrequentPattern.java
├── Main.java
├── ResultWriter.java
└── Transaction.java
```

(14 files; `Main.java` counted.)

## Related Code Files

Files being deleted:

| File | Path | Reason |
|---|---|---|
| FixHorse.java | `d:/CMAR/src/FixHorse.java` | Dataset preprocessing, user-confirmed delete |
| FixHorse2.java | `d:/CMAR/src/FixHorse2.java` | Redundant v2 of above |
| DiscretizeGlass.java | `d:/CMAR/src/DiscretizeGlass.java` | Preprocessing already applied to `glass.csv` |
| GermanPreprocessor.java | `d:/CMAR/src/GermanPreprocessor.java` | Preprocessing already applied to `german_disc.csv` |
| RuleGenerator.java | `d:/CMAR/src/RuleGenerator.java` | Dead code — never called |
| discretize_glass.py | `d:/CMAR/data/discretize_glass.py` | Python file misplaced in Java data folder |

Untouched: all remaining `.java` files and every CSV under `data/`.

## Implementation Steps

1. **Sanity re-check RuleGenerator is unreferenced.**
   ```bash
   grep -n "RuleGenerator" src/*.java
   ```
   Expected: only the `class RuleGenerator` definition line. If any other line matches, STOP — do not delete; escalate.

2. **Round A — delete the two horse fixers.**
   ```bash
   git rm src/FixHorse.java src/FixHorse2.java
   rm -rf out && javac -d out src/*.java
   java -cp out Main > /tmp/roundA.log 2>&1 && echo OK
   git commit -m "chore(cleanup): remove FixHorse{,2}.java dataset preprocessing scripts"
   ```

3. **Round B — delete glass + german preprocessors.**
   ```bash
   git rm src/DiscretizeGlass.java src/GermanPreprocessor.java
   rm -rf out && javac -d out src/*.java
   java -cp out Main > /tmp/roundB.log 2>&1 && echo OK
   git commit -m "chore(cleanup): remove dataset-specific preprocessors (already applied)"
   ```

4. **Round C — delete RuleGenerator (dead code).**
   ```bash
   git rm src/RuleGenerator.java
   rm -rf out && javac -d out src/*.java
   java -cp out Main > /tmp/roundC.log 2>&1 && echo OK
   git commit -m "chore(cleanup): remove RuleGenerator.java (dead code, never called)"
   ```

5. **Round D — delete misplaced Python file.**
   ```bash
   git rm data/discretize_glass.py
   git commit -m "chore(cleanup): remove stray data/discretize_glass.py"
   ```

6. **Parity check vs Phase-01 baseline.**
   ```bash
   rm -rf out && javac -d out src/*.java && java -cp out Main
   (cd result && sha256sum *.txt) > /tmp/checksums.after.txt
   diff plans/20260421-1019-cleanup-cmar-baseline/baseline/checksums.txt /tmp/checksums.after.txt
   ```
   Expected: zero diff, OR only diffs on files containing timestamps. Investigate any unexpected diff before proceeding.

## Todo

- [ ] Confirmed `RuleGenerator` has no callers
- [ ] Round A committed (FixHorse deletions)
- [ ] Round B committed (Glass/German deletions)
- [ ] Round C committed (RuleGenerator deletion)
- [ ] Round D committed (Python artifact deletion)
- [ ] `javac -d out src/*.java` succeeds after each round
- [ ] `java -cp out Main` succeeds after each round
- [ ] Post-deletion `result/` checksums match baseline (modulo timestamps)

## Success Criteria

- `ls src/*.java | wc -l` returns **14**.
- No `git grep -l "FixHorse\|DiscretizeGlass\|GermanPreprocessor\|RuleGenerator"` matches remain.
- `data/discretize_glass.py` absent.
- `java -cp out Main` exits 0 and produces `result/*.txt` equivalent to baseline.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Hidden reflective call to RuleGenerator via `Class.forName` | Very Low | High | Step 1 grep; also grep for `"RuleGenerator"` as string literal |
| `horse.csv` loader fails on `?` tokens after FixHorse removal | Low | Medium | Baseline run in Phase 01 already exercised the loader; if `horse.csv` is never in default pipeline this is irrelevant |
| Result files have embedded timestamps causing spurious diff | Medium | Low | Diff manually; whitelist timestamp lines |

## Security Considerations

N/A — pure deletion of local files under version control.

## Rollback

Each round is its own commit. To revert:
```bash
# revert last N cleanup commits (N = 1..4)
git revert --no-edit HEAD~N..HEAD
```
Or hard reset back to the pre-cleanup tag:
```bash
git reset --hard pre-cleanup-2026-04-21
```

## Next Steps

Proceed to [phase-03-reorganize-subfolders.md](./phase-03-reorganize-subfolders.md).
