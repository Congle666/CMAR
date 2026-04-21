# CMAR — Classification based on Multiple Association Rules

Java implementation of **CMAR** (Li, Han & Pei — ICDM 2001). Plain `javac` build, no Maven/Gradle.

## Project Structure

```
src/                          # 14 Java files, flat layout (default package)
├── Main.java                 # Entry point
│
├── CMARClassifier.java       # Core: 3-stage pruning + weighted χ² classify
├── AssociationRule.java      # Core: CAR data structure
├── Transaction.java          # Core: record + class label
│
├── FPGrowth.java             # Mining: FP-Growth with class distribution
├── FPTree.java               # Data struct: CR-tree for mining
├── FPNode.java               # Data struct: tree node (with classCount)
├── CRTree.java               # Data struct: CR-tree for rule storage
├── FrequentPattern.java      # DTO: pattern + support
│
├── DatasetLoader.java        # I/O: CSV loader
├── ResultWriter.java         # I/O: output formatters
│
├── Benchmark.java            # Benchmark: 10-fold CV on all datasets
├── BenchmarkOne.java         # Benchmark: single-dataset CV
└── CrossValidator.java       # Benchmark: K-fold CV implementation

data/      # UCI datasets (CSV)
report/    # Reports (Vietnamese)
result/    # Runtime output files
```

## Build

```bash
# Bash (Git Bash on Windows) or PowerShell
mkdir -p out
javac -d out src/*.java
```

## Run

```bash
# Default dataset (data/car.csv)
java -cp out Main

# Custom dataset + params
java -cp out Main data/zoo_h.csv 3 0.5

# Full signature
java -cp out Main <dataset> <minSup> <minConf> <chiSqThreshold> <coverageDelta>
```

**Params (defaults shown):**
- `minSup` = 50 (absolute count)
- `minConf` = 0.5
- `chiSqThreshold` = 3.841 (α = 0.05, df = 1)
- `coverageDelta` = 4

## Benchmark

10-fold stratified CV on all 20 UCI datasets:

```bash
java -Xmx512m -cp out Benchmark
```

Single dataset:

```bash
java -Xmx512m -cp out BenchmarkOne data/iris_disc.csv iris 0.03 94.00 94.67 95.33
# args: <file> <name> <minSupPct> <paperCMAR> <paperCBA> <paperC45>
```

## Output

After running `Main`:
- `result/frequent_patterns.txt` — mined patterns
- `result/association_rules.txt` — candidate CARs
- `result/cmar_result.txt` — final rules + predictions + accuracy
- `result/evaluation.txt` — per-class precision/recall/F1
- `result/predictions.txt` — per-record predictions
- `report/fp_tree_report.md` — FP-tree structure report

## Documentation

See `report/` for Vietnamese reports:
- `bao_cao_tong_hop_CMAR.md` — overview (narrative style)
- `bao_cao_chi_tiet_CMAR.md` — full algorithm details
- `bao_cao_thuat_toan_CMAR.md` — code mapping
- `giai_thich_cong_thuc_CMAR.md` — formula explanations
- `benchmark_20_datasets.md` — benchmark results vs paper

## Reference

Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* ICDM 2001, pp. 369–376.
