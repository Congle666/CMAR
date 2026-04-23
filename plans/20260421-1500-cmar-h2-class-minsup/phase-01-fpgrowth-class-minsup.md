# Phase 01 — FPGrowth per-class minSup support

## Context links

- Parent: [plan.md](./plan.md)

## Overview

- **Date:** 2026-04-21
- **Description:** Add optional `Map<String, Integer> classMinSupMap` to `FPGrowth` so rule emission can use per-class thresholds instead of the global `minSupport`.
- **Priority:** High (blocks Phase 02)
- **Status:** Planned

## Key Insights

- `FPGrowth.mine()` currently emits CAR when `classSup >= minSupport` (single global).
- Goal: emit CAR when `classSup >= classMinSupMap.getOrDefault(cls, minSupport)`.
- Global `minSupport` still controls **item-level pruning** (tree build time) — unchanged.
- When `classMinSupMap` is null → exact baseline behavior (backward compat).

## Requirements

- Optional setter `setClassMinSupMap(Map<String, Integer>)`.
- Default behavior (no setter call) must match current baseline output byte-for-byte.
- Thread-safety: not required — single-threaded mining.

## Architecture

```java
public class FPGrowth {
    private final int minSupport;
    private Map<String, Integer> classMinSupMap;  // NEW — optional

    public void setClassMinSupMap(Map<String, Integer> map) {
        this.classMinSupMap = map;
    }

    private int classThreshold(String cls) {
        if (classMinSupMap == null) return minSupport;
        return classMinSupMap.getOrDefault(cls, minSupport);
    }

    // In mine()'s inner emission loop (existing ~line 149):
    //   if (classSup < minSupport) continue;
    // Change to:
    //   if (classSup < classThreshold(cls)) continue;
}
```

## Related code files

- EDIT: `src/FPGrowth.java:149` (emission check inside `mineTree()`)

## Implementation Steps

1. Add field `private Map<String, Integer> classMinSupMap;`
2. Add setter + private helper `classThreshold(cls)`
3. Replace the check `if (classSup < minSupport) continue;` → `if (classSup < classThreshold(cls)) continue;`
4. Compile + run `Main` on car.csv → must match baseline (75.14%)
5. Compile + run `Benchmark` on iris → must match baseline numbers

## Todo list

- [ ] Add classMinSupMap field + setter
- [ ] Add classThreshold() helper
- [ ] Modify emission check in mineTree()
- [ ] Compile check
- [ ] Regression test (Main, Benchmark subset)

## Success Criteria

- Compile clean
- No regression: baseline output unchanged
- `FPGrowth` without setter call behaves identically to before

## Risk Assessment

- **Risk:** Forgetting to handle null map → NPE.
  - **Mitigation:** Helper method with null check.
- **Risk:** Emission check in wrong place.
  - **Mitigation:** Regression tests.

## Security Considerations

N/A.

## Next steps

→ Phase 02: CrossValidator overload + BenchmarkClassSup.
