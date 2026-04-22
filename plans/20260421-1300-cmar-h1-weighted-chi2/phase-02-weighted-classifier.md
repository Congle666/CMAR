# Phase 02 — Implement CMARClassifierWeighted

## Context links

- Parent: [plan.md](./plan.md)
- Dep: Phase 01 (protected methods)

## Overview

- **Date:** 2026-04-21
- **Description:** Create subclass that overrides `classify()` only. Apply inverse-frequency class weighting to the per-class score.
- **Priority:** High
- **Status:** Planned

## Key Insights

- Training (mining + 3-tier pruning) stays identical → inherit as-is.
- Class weight computed **once per classifier** after `train()` — cache in field.
- Weight formula: **sklearn "balanced"** — `w(c) = N / (k × freq(c))`.
  - N = total training records
  - k = number of classes
  - freq(c) = count of class c in training
- When all classes equally frequent: all weights = 1.0 → behaves **exactly like baseline** → safe fallback.

## Requirements

- Override `classify(Transaction)` and `train(rules, trainData)`.
- No change to public API — `predict(List)`, `evaluate(List)` inherit unchanged.
- Cache weights computed in `train()`.
- Unweighted branches (0 rules matched, all rules same class) behave identically to baseline.

## Architecture

```java
public class CMARClassifierWeighted extends CMARClassifier {

    /** Inverse-frequency weights, keyed by class label. Computed after train(). */
    private Map<String, Double> classWeights = new HashMap<>();

    @Override
    public void train(List<AssociationRule> rules, List<Transaction> trainData) {
        super.train(rules, trainData);
        computeClassWeights();
    }

    private void computeClassWeights() {
        Map<String, Integer> freq = getClassFreq();
        int N = getTotalTransactions();
        int k = freq.size();
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            classWeights.put(e.getKey(), (double) N / (k * e.getValue()));
        }
    }

    @Override
    public String classify(Transaction record) {
        List<AssociationRule> matching = getCRTree().findMatching(record);
        if (matching.isEmpty()) return getDefaultClass();

        Map<String, List<AssociationRule>> byClass = groupByClass(matching);
        if (byClass.size() == 1) return byClass.keySet().iterator().next();

        // Weighted score
        String bestClass = getDefaultClass();
        double bestScore = -1.0;
        for (Map.Entry<String, List<AssociationRule>> e : byClass.entrySet()) {
            String cls = e.getKey();
            double score = 0.0;
            for (AssociationRule r : e.getValue()) {
                double chi2 = computeChiSquare(r, cls);
                double maxChi2 = computeMaxChiSquare(r, cls);
                if (maxChi2 > 0) score += (chi2 * chi2) / maxChi2;
            }
            // Apply class weight
            score *= classWeights.getOrDefault(cls, 1.0);
            if (score > bestScore) { bestScore = score; bestClass = cls; }
        }
        return bestClass;
    }

    private Map<String, List<AssociationRule>> groupByClass(
            List<AssociationRule> matching) { ... }
}
```

## Related code files

- NEW: `src/CMARClassifierWeighted.java`
- Read-only ref: `src/CMARClassifier.java:194-227` (original classify)

## Implementation Steps

1. Create `src/CMARClassifierWeighted.java` with class extending `CMARClassifier`
2. Implement field `classWeights`, method `computeClassWeights()`
3. Override `train()` to call super + computeClassWeights
4. Override `classify()` with weighted scoring (copy + modify baseline logic)
5. Private helper `groupByClass()` for readability
6. Compile check
7. Smoke test:
   ```java
   // In a temporary test or Main replacement
   CMARClassifierWeighted c = new CMARClassifierWeighted();
   c.train(candidates, trainData);
   // Should produce different predictions than baseline on imbalanced data
   ```

## Todo list

- [ ] Create CMARClassifierWeighted.java file
- [ ] Implement classWeights field + computeClassWeights()
- [ ] Override train()
- [ ] Override classify() with weighted score
- [ ] Compile check
- [ ] Manual smoke test (compare output with baseline on car.csv)

## Success Criteria

- File compiles.
- On balanced dataset (iris), weighted result **close to baseline** (±1 record different).
- On imbalanced dataset (car, lymph), predictions **shift toward minority** class.

## Risk Assessment

- **Risk:** Over-weighting minority causes false positives (precision drops).
  - **Mitigation:** Expected trade-off. Compare F1 (balances P/R) not just Accuracy in benchmark.
- **Risk:** weight = 0 if class missing from training.
  - **Mitigation:** `getOrDefault(cls, 1.0)` — fallback to uniform weight.

## Security Considerations

N/A.

## Next steps

→ Phase 03: Add factory to CrossValidator + BenchmarkWeighted entry.
