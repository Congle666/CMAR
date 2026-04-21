# CMAR Source Analysis & Cleanup Recommendations

## 1. File Categorization Table

| File | Lines | Category | Standalone | Core Referenced | Purpose |
|------|-------|----------|-----------|---|----------|
| AssociationRule.java | 74 | CORE | No | No | CAR data structure |
| CMARClassifier.java | 294 | CORE | No | Yes | CMAR 3-stage classifier |
| CRTree.java | 98 | CORE | No | No | CR-tree rule storage |
| DatasetLoader.java | 55 | CORE | No | Yes | CSV loader |
| FrequentPattern.java | 29 | CORE | No | No | Frequent itemset DTO |
| FPGrowth.java | 223 | CORE | No | Yes | Pattern mining engine |
| FPNode.java | 44 | CORE | No | No | CR-tree node impl |
| FPTree.java | 130 | CORE | No | No | CR-tree structure |
| Main.java | 163 | CORE | Yes | N/A | Entry point |
| RuleGenerator.java | 85 | CORE | No | No | CAR generation utility |
| ResultWriter.java | 501 | CORE | No | Yes | Result output (7 calls) |
| Transaction.java | 42 | CORE | No | No | Record/transaction DTO |
| **Benchmark.java** | 119 | BENCHMARK | Yes | No | 10-fold CV harness |
| **BenchmarkOne.java** | 35 | BENCHMARK | Yes | No | Single dataset CV |
| **CrossValidator.java** | 113 | BENCHMARK | No | No | K-fold CV impl |
| **DiscretizeGlass.java** | 220 | PREPROCESSING | Yes | No | Glass dataset processor |
| **FixHorse.java** | 87 | PREPROCESSING | Yes | No | Horse dataset cleaner v1 |
| **FixHorse2.java** | 82 | PREPROCESSING | Yes | No | Horse dataset cleaner v2 |
| **GermanPreprocessor.java** | 157 | PREPROCESSING | Yes | No | German dataset processor |
| ResultWriter.java | 501 | UTILITY | No | Yes | Output formatter |

**Summary:** 12 CORE + 1 UTILITY vs. 4 BENCHMARK + 3 PREPROCESSING (7 removable files).

---

## 2. Detailed Analysis: PREPROCESSING & BENCHMARK

### PREPROCESSING (3 files — datasets only)

**FixHorse.java** (87 lines)
- Purpose: Clean/fix horse.csv dataset
- Standalone with `main()` method
- No imports from core CMAR classes
- **Safe to delete?** YES. Dataset preprocessing tool, not part of algorithm.
- **Recommendation:** MOVE to `tools/preprocessing/` or DELETE

**FixHorse2.java** (82 lines)
- Purpose: Alternate horse.csv cleaner (v2)
- Standalone with `main()` method
- No imports from core CMAR classes
- **Safe to delete?** YES. Redundant preprocessing variant.
- **Recommendation:** DELETE (confirmed user intent)

**DiscretizeGlass.java** (220 lines)
- Purpose: Discretize UCI Glass dataset using equal-frequency binning
- Standalone with `main()` method
- No imports from core CMAR classes
- **Safe to delete?** YES. Dataset-specific preprocessing.
- **Recommendation:** MOVE to `tools/preprocessing/` or DELETE

**GermanPreprocessor.java** (157 lines)
- Purpose: German Credit dataset processor (missing value handling, feature encoding)
- Standalone with `main()` method
- No imports from core CMAR classes
- **Safe to delete?** YES. Dataset-specific preprocessing.
- **Recommendation:** MOVE to `tools/preprocessing/` or DELETE

### BENCHMARK (2 files — evaluation harnesses)

**Benchmark.java** (119 lines)
- Purpose: Runs 10-fold stratified CV on all UCI datasets (multiclass wrapper)
- Standalone with `main()` method
- Uses: DatasetLoader, CrossValidator, CMARClassifier, ResultWriter
- **Core algorithm required?** No. Evaluation harness.
- **Safe to delete?** YES, but consider keeping for CI/regression.
- **Recommendation:** MOVE to `tools/benchmark/` or keep for validation

**BenchmarkOne.java** (35 lines)
- Purpose: Single-dataset CV wrapper (10 folds)
- Standalone with `main()` method
- Uses: CrossValidator, CMARClassifier
- **Core algorithm required?** No. Evaluation harness.
- **Safe to delete?** YES, but consider keeping for quick testing.
- **Recommendation:** MOVE to `tools/benchmark/` or keep for validation

**CrossValidator.java** (113 lines)
- Purpose: K-Fold stratified CV implementation
- Used by Benchmark.java and BenchmarkOne.java (not in Main.java pipeline)
- No imports from core CMAR classes
- **Core algorithm required?** No. Evaluation utility.
- **Safe to delete?** YES. Benchmark-only dependency.
- **Recommendation:** MOVE to `tools/benchmark/` with Benchmark.java

---

## 3. Core Algorithm Pipeline (from Main.java trace)

**Main.java invocation chain:**
```
Main.main() [L44-162]
  ├─ DatasetLoader.load() [L69]
  ├─ FPGrowth(minSupport) + .mine() [L95-96]
  ├─ FPGrowth.getPatterns() [L97]
  ├─ ResultWriter.writeFrequentPatterns() [L101]
  ├─ ResultWriter.writeRules() [L104]
  ├─ ResultWriter.writeFPTreeReport() [L111-114]
  ├─ ResultWriter.writeFPGrowthResult() [L118-120]
  ├─ CMARClassifier() + .train() [L129-132]
  ├─ CMARClassifier.predict() [L139]
  ├─ ResultWriter.writePredictions() [L141]
  ├─ CMARClassifier.evaluate() [L148]
  ├─ ResultWriter.writeEvaluation() [L151]
  └─ ResultWriter.writeCMARResult() [L155-158]

FPGrowth dependencies:
  ├─ FPTree(minSupport) [line 165, 297 in FPGrowth]
  │  ├─ FPNode (root creation) [line 95 in FPTree]
  │  └─ CRTree (optional structure) [line 39 in FPGrowth]
  ├─ FrequentPattern (pattern results) [line 222 in FPGrowth]
  ├─ AssociationRule (CAR results) [line 140 in FPGrowth]
  └─ RuleGenerator (via AssociationRule creation) [possible]

CMARClassifier dependencies:
  ├─ AssociationRule (input rules) [line 74 in CMARClassifier]
  └─ Transaction (for training/eval) [throughout]

ResultWriter dependencies:
  ├─ FrequentPattern
  ├─ AssociationRule
  ├─ CMARClassifier
  ├─ Transaction
  └─ FPTree (for tree report)
```

**Files NOT in Main.java chain:**
- Benchmark.java, BenchmarkOne.java, CrossValidator.java
- DiscretizeGlass.java, FixHorse.java, FixHorse2.java, GermanPreprocessor.java

---

## 4. Dependency Graph — Internal Classes Only

```
CORE ALGORITHM:
  Main.java
    → DatasetLoader.java
    → FPGrowth.java
      → FPTree.java
        → FPNode.java
      → CRTree.java
      → FrequentPattern.java
      → AssociationRule.java
      → RuleGenerator.java (optional call)
    → CMARClassifier.java
      → AssociationRule.java
      → Transaction.java
    → ResultWriter.java
      → FrequentPattern.java
      → AssociationRule.java
      → FPTree.java
      → CMARClassifier.java
    → Transaction.java (data structure)

EVALUATION:
  Benchmark.java
    → CrossValidator.java
    → CMARClassifier.java
    → DatasetLoader.java
    → ResultWriter.java
  BenchmarkOne.java
    → CrossValidator.java
    → CMARClassifier.java

PREPROCESSING (NO INTERNAL DEPS):
  FixHorse.java (standalone)
  FixHorse2.java (standalone)
  DiscretizeGlass.java (standalone)
  GermanPreprocessor.java (standalone)
```

---

## 5. Unused Imports Analysis

All internal imports are valid—no unused inter-class references found. Standard library imports (java.util.*, java.io.*) are appropriate.

---

## 6. Data Folder Audit

**`data/discretize_glass.py`** exists in data/ folder
- **Issue:** Python script mixed with Java project data
- **Status:** Artifact—should be deleted or moved to `tools/preprocessing/`
- No Java code references it

---

## 7. Recommendations Summary

### SAFE TO DELETE (no impact on core CMAR):
| File | Reason |
|------|--------|
| **FixHorse.java** | Dataset preprocessing only; user confirmed |
| **FixHorse2.java** | Dataset preprocessing only; user confirmed |
| **DiscretizeGlass.java** | Dataset preprocessing only |
| **GermanPreprocessor.java** | Dataset preprocessing only |
| **data/discretize_glass.py** | Python artifact, unrelated |

### CONDITIONAL (consider keeping for testing):
| File | Reason | Action |
|------|--------|--------|
| **Benchmark.java** | 10-fold CV harness; useful for regression testing | MOVE to `tools/benchmark/` OR keep in src/ |
| **BenchmarkOne.java** | Single-dataset CV wrapper; lightweight | MOVE to `tools/benchmark/` OR delete |
| **CrossValidator.java** | K-fold implementation; only used by benchmarks | MOVE to `tools/benchmark/` with Benchmark* |

### KEEP (core algorithm):
All 12 CORE files + ResultWriter.java (7 calls from Main.java)

---

## 8. Post-Cleanup Verification Steps

After deletion/moves, ensure:
1. `javac src/*.java` compiles cleanly
2. `java Main data/car.csv` runs without errors
3. Check `result/` and `report/` folders for expected output files
4. No "class not found" errors at runtime

---

## Unresolved Questions

1. **Should Benchmark.java remain in src/?** Currently only used for regression testing, not core pipeline. Recommend moving to `tools/benchmark/` but keep if regression testing is part of CI/CD.

2. **Is there a `data/horse.csv` or `data/glass.csv` in the repo?** If yes, FixHorse* and DiscretizeGlass* were likely one-time data prep scripts—safe to archive.

3. **Does RuleGenerator.java get called?** Scanned FPGrowth.java but didn't find explicit call—confirm if it's used or dead code.

---

**Report generated:** 2026-04-21 | **Analyzed:** 19 Java files + 1 Python artifact
