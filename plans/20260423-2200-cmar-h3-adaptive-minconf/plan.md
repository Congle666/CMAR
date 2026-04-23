# Hướng 3 (Option C) — Adaptive minConf Per Class

**Created:** 2026-04-23 22:00
**Type:** Algorithm improvement (target: rescue extreme minority F1=0)
**Refs:** Baseline `baseline-f1-v1`, v2 `v2-weighted-chi2`, v3 `v3-class-minsup`

## Goal

Solve the hard-stop in H1/H2: **lymph/fibrosis F1 = 0** because NO rule can pass `minConf = 0.5` due to math:
- Fibrosis has max 4 records in training
- Item-level `minSup = 7` filters items unique to fibrosis
- Shared items → `conf = (<=4) / (>=7) < 0.571` — never passes 0.5

**Fix:** Make `minConf` **per-class** based on class frequency. Minority classes get lower threshold. Majority stays at global.

## Formula

```
minConf(c) = min(globalMinConf, max(FLOOR, LIFT × freq(c)/N))
```

Defaults: `FLOOR = 0.3`, `LIFT = 5`, `globalMinConf = 0.5`.

### Example (lymph N=148)

| Class | freq | freq/N | 5 × freq/N | **minConf(c)** |
|-------|:----:|:------:|:----------:|:--------------:|
| metastases | 81 | 0.547 | 2.735 | **0.500** (capped at global) |
| malign_lymph | 61 | 0.412 | 2.060 | **0.500** (capped) |
| fibrosis | 4 | 0.027 | 0.135 | **0.300** (floor) |
| normal | 2 | 0.014 | 0.068 | **0.300** (floor) |

→ Rule `{X}⇒fibrosis` with `conf=0.43` now **PASSES** (0.43 > 0.30). Previously rejected at global 0.5.

### Why this makes sense

Rule's **lift** = `conf / (freq(c)/N)`. A rule with `conf=0.3` for fibrosis = lift 11x (strongly informative). Baseline `minConf=0.5` implicitly required lift >18x for fibrosis — overkill.

## Scope

- Modify `FPGrowth.java` — add `classMinConfMap` field + emission uses per-class threshold
- Modify `CrossValidator.java` — compute classMinConfMap from trainData
- Add `BenchmarkAdaptiveConf.java` — benchmark entry
- Run on 7 target datasets
- Write thesis-ready comparison MD

## Phases

| # | Phase | File | Status |
|---|-------|------|--------|
| 01 | FPGrowth classMinConfMap | [phase-01-fpgrowth-minconf.md](./phase-01-fpgrowth-minconf.md) | Planned |
| 02 | CV overload + BenchmarkAdaptiveConf | [phase-02-cv-benchmark.md](./phase-02-cv-benchmark.md) | Planned |
| 03 | Run + validate | [phase-03-run-validate.md](./phase-03-run-validate.md) | Planned |
| 04 | Thesis-ready comparison MD | [phase-04-thesis-md.md](./phase-04-thesis-md.md) | Planned |

## Success Criteria

- Compile clean
- **lymph/fibrosis F1 > 0.10** (rescue from F1=0)
- Overall Macro-F1 ≥ baseline (no regression)
- Thesis MD explains H1, H2, H3 clearly + concrete improvement numbers

## Files

| Action | File |
|:------:|------|
| EDIT | `src/FPGrowth.java` |
| EDIT | `src/CrossValidator.java` |
| ADD | `src/BenchmarkAdaptiveConf.java` |
| ADD | `result/v5_metrics.csv` + `v5_per_class.csv` |
| ADD | `report/huong_3_adaptive_conf.md` (thesis writeup) |

## Estimated Effort: 2 hours
