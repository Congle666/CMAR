# Phase 03 — Reorganize into Subfolders (Option B)

## Context

- **Parent plan:** [../plan.md](./plan.md)
- **Previous phase:** [phase-02-delete-dead-files.md](./phase-02-delete-dead-files.md) (must be complete)
- **Next phase:** [phase-04-verify-and-docs.md](./phase-04-verify-and-docs.md)
- **Research:** `research/researcher-02-structure-options.md` Option B

## Overview

- **Date:** 2026-04-21
- **Description:** Move the 14 remaining `.java` files into logical subfolders under `src/`. **No `package` declarations** are added; no source code is edited.
- **Priority:** High
- **Implementation Status:** Planned
- **Review Status:** Pending

## Key Insights

- Because there are **no `package` statements**, `javac` treats all files as the default package regardless of folder. Moving files has zero semantic impact; only the compile command's file list changes.
- Bash on Windows (Git Bash) supports `**` glob when `shopt -s globstar` is enabled OR via `find`. Using `find` is the safest cross-shell option.
- `Main.java` stays at `src/` root so `java -cp out Main` remains the entry command (matches user habit; see Unresolved Q1 in plan.md).
- `ResultWriter.java` + `DatasetLoader.java` are I/O concerns (both do file read/write). Group them under `io/` rather than `benchmark/` — cleaner semantics. This deviates slightly from researcher-02's suggested grouping but is more accurate.

## Requirements

1. Move 13 files (everything except `Main.java`) into 5 subfolders.
2. Do not edit any file contents.
3. Compile command updated to enumerate all folders (or use `find`).
4. `java -cp out Main` still succeeds.

## Architecture

### Before/After File Mapping

| Before (flat) | After (subfolder) | Rationale |
|---|---|---|
| `src/Main.java` | `src/Main.java` | Entry point stays at root |
| `src/CMARClassifier.java` | `src/core/CMARClassifier.java` | Core algorithm |
| `src/AssociationRule.java` | `src/core/AssociationRule.java` | CAR data type used across core |
| `src/Transaction.java` | `src/core/Transaction.java` | Record type used across core |
| `src/FPGrowth.java` | `src/mining/FPGrowth.java` | Mining engine |
| `src/FrequentPattern.java` | `src/datastructure/FrequentPattern.java` | Pattern DTO |
| `src/FPTree.java` | `src/datastructure/FPTree.java` | FP-tree structure |
| `src/FPNode.java` | `src/datastructure/FPNode.java` | FP-tree node |
| `src/CRTree.java` | `src/datastructure/CRTree.java` | CR-tree storage |
| `src/DatasetLoader.java` | `src/io/DatasetLoader.java` | CSV loading |
| `src/ResultWriter.java` | `src/io/ResultWriter.java` | Output writing |
| `src/Benchmark.java` | `src/benchmark/Benchmark.java` | 10-fold CV harness |
| `src/BenchmarkOne.java` | `src/benchmark/BenchmarkOne.java` | Single-dataset CV |
| `src/CrossValidator.java` | `src/benchmark/CrossValidator.java` | K-fold impl |

### Final Layout

```
src/
├── Main.java
├── core/
│   ├── AssociationRule.java
│   ├── CMARClassifier.java
│   └── Transaction.java
├── datastructure/
│   ├── CRTree.java
│   ├── FPNode.java
│   ├── FPTree.java
│   └── FrequentPattern.java
├── mining/
│   └── FPGrowth.java
├── io/
│   ├── DatasetLoader.java
│   └── ResultWriter.java
└── benchmark/
    ├── Benchmark.java
    ├── BenchmarkOne.java
    └── CrossValidator.java
```

## Related Code Files

All 14 files listed in the mapping table above. No file content changes.

## Implementation Steps

1. **Create target subfolders.** From `d:/CMAR/`:
   ```bash
   mkdir -p src/core src/datastructure src/mining src/io src/benchmark
   ```

2. **Move files with `git mv` (preserves history).**
   ```bash
   # core/
   git mv src/AssociationRule.java    src/core/
   git mv src/CMARClassifier.java     src/core/
   git mv src/Transaction.java        src/core/

   # datastructure/
   git mv src/CRTree.java             src/datastructure/
   git mv src/FPNode.java             src/datastructure/
   git mv src/FPTree.java             src/datastructure/
   git mv src/FrequentPattern.java    src/datastructure/

   # mining/
   git mv src/FPGrowth.java           src/mining/

   # io/
   git mv src/DatasetLoader.java      src/io/
   git mv src/ResultWriter.java       src/io/

   # benchmark/
   git mv src/Benchmark.java          src/benchmark/
   git mv src/BenchmarkOne.java       src/benchmark/
   git mv src/CrossValidator.java     src/benchmark/
   ```

3. **Compile with the new layout.** Use `find` for portability:
   ```bash
   rm -rf out
   javac -d out $(find src -name "*.java")
   ```
   Expected: zero errors, zero warnings. If warnings appear about missing package, confirm NONE of the files had stray `package` lines (Phase 02 research confirmed they don't).

4. **Run Main and verify parity.**
   ```bash
   java -cp out Main
   (cd result && sha256sum *.txt) > /tmp/checksums.phase3.txt
   diff plans/20260421-1019-cleanup-cmar-baseline/baseline/checksums.txt /tmp/checksums.phase3.txt
   ```
   Expected: identical (or timestamp-only diffs).

5. **Run Benchmark to double-check parity on multiple datasets.**
   ```bash
   java -cp out Benchmark 2>&1 | tee /tmp/benchmark.phase3.log
   ```
   Spot-check `car`, `iris`, `zoo` rows against Phase-01 numbers.

6. **Commit the reorganization.**
   ```bash
   git add src
   git commit -m "refactor(cleanup): reorganize src/ into core|datastructure|mining|io|benchmark subfolders (Option B, no packages)"
   ```

## Todo

- [ ] Subfolders `core/`, `datastructure/`, `mining/`, `io/`, `benchmark/` created
- [ ] All 13 files moved via `git mv`
- [ ] `Main.java` remains at `src/` root
- [ ] No file's content edited (git diff shows only renames)
- [ ] `javac -d out $(find src -name "*.java")` succeeds
- [ ] `java -cp out Main` succeeds
- [ ] `result/` checksums match baseline
- [ ] Benchmark spot-check on 3 datasets matches baseline numbers
- [ ] Single commit created

## Success Criteria

- `find src -name "*.java" | wc -l` returns **14**.
- `ls src/*.java` returns exactly **`src/Main.java`**.
- `git log --diff-filter=R -1 --stat` shows 13 rename entries, zero content changes.
- Compile + run produce identical output to baseline.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Hidden `package` statement in a file breaks build | Very Low | High | `grep -n '^package' src/**/*.java` before compile — must be empty |
| `find` on Git Bash returns CRLF or backslash paths | Low | Medium | Verify `$(find src -name "*.java")` expands to space-separated forward-slash paths; use `xargs` if needed |
| IDE (IntelliJ) complains about "file outside of package" | Medium | None | Cosmetic warning only; plain javac ignores it |
| Users on Windows CMD (not bash) can't use `find` | Low | Low | Phase 04 adds `build.sh` or explicit folder enumeration |

## Security Considerations

N/A — file moves only.

## Rollback

Single atomic commit → revert with:
```bash
git revert --no-edit HEAD
```
Or hard reset:
```bash
git reset --hard HEAD~1
```

## Next Steps

Proceed to [phase-04-verify-and-docs.md](./phase-04-verify-and-docs.md).
