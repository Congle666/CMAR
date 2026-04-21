# CMAR Project Reorganization: Structure Options Research

## Current State Summary

- **19 flat .java files** in `src/`: mixed core algorithm (CMARClassifier, RuleGenerator, FPGrowth), benchmark/utility (Benchmark, CrossValidator, ResultWriter), and dataset-specific preprocessing (DiscretizeGlass, FixHorse, FixHorse2, GermanPreprocessor)
- **No package declarations** in any file; all imports are java.util.* standard library only
- **Build command:** `javac -d out src/*.java` — simple glob, no dependency management

### Files by Category
| Core CMAR Algorithm | FP-Tree Mining | Benchmark/IO | Dataset-Specific Preprocessing |
|---|---|---|---|
| CMARClassifier, RuleGenerator, AssociationRule, CRTree, Transaction | FPGrowth, FPNode, FPTree, FrequentPattern | Main, Benchmark, BenchmarkOne, CrossValidator, ResultWriter, DatasetLoader | DiscretizeGlass, GermanPreprocessor, FixHorse, FixHorse2 |

---

## Option A: Flat + Naming Convention (Keep src/, Rename Files)

**Approach:** Keep all files in `src/`, prefix preprocessing files with `PreprocessDatasetName_`.

```
src/
├── CMARClassifier.java
├── RuleGenerator.java
├── ...
├── PreprocessDiscretizeGlass.java  (renamed from DiscretizeGlass.java)
├── PreprocessGerman.java            (renamed from GermanPreprocessor.java)
├── PreprocessFixHorse.java          (renamed from FixHorse.java)
├── PreprocessFixHorse2.java         (renamed from FixHorse2.java)
```

| Aspect | Rating | Notes |
|--------|--------|-------|
| **Disruption** | Very Low | Only 4 file renames; no code changes; no `package` statements |
| **Clarity** | Low | Prefix convention helps but files still mixed; unclear what's "active" preprocessing |
| **Compile** | Unchanged | `javac -d out src/*.java` |
| **Run** | Unchanged | `java -cp out Main` |

**Pros:** Minimal disruption, no build system change.  
**Cons:** Still visually cluttered; hard to scan which are core algorithm files; doesn't scale if more datasets added.

---

## Option B: Subfolder Without Package Declarations

**Approach:** Organize into logical subfolders (`src/core`, `src/preprocess`, `src/benchmark`, `src/datastructure`), but do NOT add `package` statements.

```
src/
├── core/
│   ├── CMARClassifier.java
│   ├── RuleGenerator.java
│   ├── AssociationRule.java
│   └── Transaction.java
├── datastructure/
│   ├── CRTree.java
│   ├── FPTree.java
│   ├── FPNode.java
│   └── FrequentPattern.java
├── mining/
│   ├── FPGrowth.java
├── benchmark/
│   ├── Main.java
│   ├── Benchmark.java
│   ├── BenchmarkOne.java
│   ├── CrossValidator.java
│   ├── ResultWriter.java
│   └── DatasetLoader.java
└── preprocess/
    ├── DiscretizeGlass.java
    ├── GermanPreprocessor.java
    ├── FixHorse.java
    └── FixHorse2.java
```

| Aspect | Rating | Notes |
|--------|--------|-------|
| **Disruption** | Low-Medium | Move 19 files to subfolders; **no code changes** (no import changes needed) |
| **Clarity** | High | Clear visual separation; obvious which is core vs preprocessing |
| **Compile** | Needs minor tweak | `javac -d out src/**/*.java` or `javac -d out src/*/*.java` (bash glob) |
| **Run** | Unchanged | `java -cp out Main` |

**Pros:** Clear separation without code modifications; still uses plain javac; scales well.  
**Cons:** Requires glob expansion in compile command; may confuse IDEs expecting packages.

---

## Option C: Proper Java Packages (Full Migration)

**Approach:** Add `package` declarations and organize into proper packages.

```
src/
├── cmar/
│   ├── core/
│   │   ├── CMARClassifier.java    → package cmar.core;
│   │   ├── RuleGenerator.java
│   │   ├── AssociationRule.java
│   │   └── Transaction.java
│   ├── datastructure/
│   │   ├── CRTree.java            → package cmar.datastructure;
│   │   ├── FPTree.java
│   │   ├── FPNode.java
│   │   └── FrequentPattern.java
│   ├── mining/
│   │   └── FPGrowth.java           → package cmar.mining;
│   ├── benchmark/
│   │   ├── Main.java              → package cmar.benchmark;
│   │   ├── Benchmark.java
│   │   ├── BenchmarkOne.java
│   │   ├── CrossValidator.java
│   │   ├── ResultWriter.java
│   │   └── DatasetLoader.java
│   └── preprocess/
│       ├── DiscretizeGlass.java   → package cmar.preprocess;
│       ├── GermanPreprocessor.java
│       ├── FixHorse.java
│       └── FixHorse2.java
```

| Aspect | Rating | Notes |
|--------|--------|-------|
| **Disruption** | **High** | Add `package` statement to all 19 files; update class references in cross-package calls |
| **Clarity** | Highest | Fully qualified names; clear module boundaries |
| **Compile** | Unchanged | `javac -d out src/**/*.java` (same as Option B) |
| **Run** | Changed | `java -cp out cmar.benchmark.Main` |

**Pros:** Standard Java practice; IDE-friendly; enables future library distribution.  
**Cons:** 19 files modified; Main class reference changes; only beneficial if library is reused elsewhere (not stated).

---

## Comparison Table

| Criterion | Option A | Option B | Option C |
|-----------|----------|----------|----------|
| **Files Modified** | 4 (rename) | 0 (move only) | 19 (add package + refs) |
| **Build Complexity** | Lowest | Low | Low |
| **Clarity of Intent** | Low | High | Highest |
| **IDE Support** | ✓ (no issues) | ~ (folder vs package confusion) | ✓✓ (standard Java) |
| **Future Scalability** | Poor | Good | Excellent |
| **Runnable w/ javac** | Yes | Yes | Yes |
| **No Build Tool Migration** | ✓ | ✓ | ✓ |

---

## Recommendation: **Option B** (Subfolder Without Packages)

**Justification:**
1. **Sweet spot:** Clear separation of concerns (high clarity) with minimal code disruption (no package statements needed).
2. **Practical:** Researcher/student projects rarely benefit from package namespacing; CMAR is not a library being distributed.
3. **Maintainable:** Moving files requires no edits; any future developer can understand structure at a glance.
4. **Scalable:** Adding more datasets/algorithms is clean (just add folders).
5. **Build simplicity:** One-line command change; still plain javac.

**Delete as planned:**
- `src/preprocess/FixHorse.java` — Horse dataset already processed
- `src/preprocess/FixHorse2.java`

---

## Post-Cleanup Build & Run Commands

**Compile:**
```bash
javac -d out src/core/*.java src/datastructure/*.java src/mining/*.java src/benchmark/*.java src/preprocess/*.java
```

Or with wildcard (bash/zsh):
```bash
javac -d out src/**/*.java
```

**Run:**
```bash
java -cp out Main
```

**Run specific benchmark:**
```bash
java -cp out Benchmark
```

---

## Unresolved Questions

1. **Glob expansion:** Will this project ever be compiled on Windows CMD (no **glob support)? If yes, Option B requires `.bat` wrapper or explicit file listing.
2. **IDE usage:** Will developers use IDE (Eclipse, IntelliJ) or plain editor? IDEs may warn about missing package declarations even though code works.
3. **Future library distribution:** Is there any plan to distribute CMAR as a JAR library? If yes, Option C becomes more valuable.
