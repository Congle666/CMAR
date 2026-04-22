# Phase 01 — Make CMARClassifier Internals Protected

## Context links

- Parent: [plan.md](./plan.md)
- Baseline: commit a79b40a, tag `baseline-f1-v1`

## Overview

- **Date:** 2026-04-21
- **Description:** Change `private` → `protected` for 3 methods in `CMARClassifier.java` so `CMARClassifierWeighted` can reuse them without duplication.
- **Priority:** High (blocks phase 02)
- **Status:** Planned

## Key Insights

- Subclass needs access to:
  - `computeChiSquare(rule, cls)` — for weighted score calculation
  - `computeMaxChiSquare(rule, cls)` — same
- Public getters for `classFreq`, `totalTransactions`, `defaultClass`, `getCRTree()` already exist → no change needed there.
- Access changes are behavior-neutral (no existing code tries to access private methods cross-class).

## Requirements

- No API breakage.
- No logic change.
- Change is confined to access modifiers only.

## Architecture

```diff
- private double computeChiSquare(AssociationRule rule, String cls)
+ protected double computeChiSquare(AssociationRule rule, String cls)

- private double computeMaxChiSquare(AssociationRule rule, String cls)
+ protected double computeMaxChiSquare(AssociationRule rule, String cls)
```

Optional third:
```diff
- (classify method stays public — already)
```

`classify()` is already public. Good.

## Related code files

- EDIT: `src/CMARClassifier.java:233` (computeChiSquare)
- EDIT: `src/CMARClassifier.java:245` (computeMaxChiSquare)

## Implementation Steps

1. Open `src/CMARClassifier.java`
2. Replace `private double computeChiSquare` → `protected double computeChiSquare`
3. Replace `private double computeMaxChiSquare` → `protected double computeMaxChiSquare`
4. Compile: `javac -d out src/*.java` → must succeed
5. Smoke test: `java -cp out Main data/car.csv 50 0.5` → accuracy must be 75.14% (unchanged)

## Todo list

- [ ] Change `computeChiSquare` to protected
- [ ] Change `computeMaxChiSquare` to protected
- [ ] Compile check
- [ ] Run baseline to verify no regression

## Success Criteria

- `javac -d out src/*.java` exit 0
- Main on car.csv → 75.14% (byte-identical to baseline)

## Risk Assessment

- **Risk:** Low — pure visibility change.
- **Mitigation:** Run smoke test.

## Security Considerations

N/A.

## Next steps

→ Phase 02: create `CMARClassifierWeighted` subclass.
