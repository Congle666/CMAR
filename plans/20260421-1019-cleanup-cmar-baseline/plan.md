# CMAR Baseline Cleanup & Reorganization

**Created:** 2026-04-21
**Owner:** 22dh112022@gmail.com
**Type:** Pure restructuring (no algorithm changes)
**Status:** Planned

## Goal

Clean up the flat `src/` layout so the remaining code is the minimal canonical CMAR baseline — ready for future F1/Recall improvements. No logic changes.

## Scope

- Delete dead / one-shot preprocessing files that no longer serve the pipeline.
- Reorganize the remaining source into logical subfolders per **Option B** (subfolders, no `package` declarations).
- Keep plain `javac`; no Maven/Gradle.
- Verify before/after parity on the default dataset (`data/car.csv`) and on 3–5 benchmark datasets.

## Out of Scope

- F1/Recall improvements (separate plan after this one).
- Adding Java packages, build tooling, unit tests, or CI.
- Modifying core algorithm classes.

## Research Inputs

- `research/researcher-01-source-analysis.md` — file categorization + dependency graph.
- `research/researcher-02-structure-options.md` — evaluates 3 layouts; recommends Option B.

## Phases

| # | Phase | File | Status |
|---|-------|------|--------|
| 01 | Pre-flight & Safety Baseline | [phase-01-preflight-baseline.md](./phase-01-preflight-baseline.md) | Planned |
| 02 | Delete Dead & Preprocessing Files | [phase-02-delete-dead-files.md](./phase-02-delete-dead-files.md) | Planned |
| 03 | Reorganize into Subfolders (Option B) | [phase-03-reorganize-subfolders.md](./phase-03-reorganize-subfolders.md) | Planned |
| 04 | Verification & Docs Update | [phase-04-verify-and-docs.md](./phase-04-verify-and-docs.md) | Planned |

## Final Target Layout

```
src/
├── Main.java                # entry point stays at root (see Unresolved Q1)
├── core/                    # CMAR classifier + rule/record types
├── datastructure/           # CR-Tree, FP-Tree, nodes, pattern DTO
├── mining/                  # FPGrowth
├── io/                      # DatasetLoader, ResultWriter
└── benchmark/               # Benchmark, BenchmarkOne, CrossValidator
```

Deleted: `FixHorse.java`, `FixHorse2.java`, `DiscretizeGlass.java`, `GermanPreprocessor.java`, `RuleGenerator.java` (dead code), `data/discretize_glass.py`.

## Success Criteria (overall)

- `src/` contains 14 `.java` files across 5 subfolders + `Main.java` at root.
- `javac -d out $(find src -name "*.java")` compiles with zero warnings/errors.
- `java -cp out Main` on `data/car.csv` produces output byte-equivalent to pre-cleanup run (or only differs in timestamp lines).
- `benchmark_20_datasets.md` accuracy numbers on 3 sampled datasets unchanged.
- Clean `git log` — one commit per phase, reversible.

## Unresolved Questions (need user input before Phase 03)

1. **Main.java placement:** keep at `src/` root (proposed) or move to `src/app/`? Plan assumes root.
2. **`.gitignore`:** already excludes `out/`; no change needed — confirm no additional artifacts to ignore.
3. **Build helper:** add `build.sh` with the new multi-folder `javac` invocation? Plan adds it in Phase 04 as optional convenience; remove step if undesired.
