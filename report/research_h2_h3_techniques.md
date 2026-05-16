# Research Report: H2 & H3 Technique Analysis Against Literature

**Date**: 2026-05-13 | **Search Period**: Feb 2025 - May 2026 | **Focus**: MMSCBA vs H2 (minSup), Adaptive Confidence vs H3

---

## Executive Summary

**H2 Technique (Class-specific minSup)**: DIRECTLY MATCHES MMSCBA concept. Hu et al. (2016) propose Minimum Class Support (MCS) per class, defining `MRS(β) = min(MIS items, MCS(yβ))`. Your H2 formula `minSup(c) = max(2, ⌊supPct × freq(c)⌋)` is a concrete implementation of this principle—proportional to class frequency. **No novelty claimed needed; cite MMSCBA.**

**H3 Technique (Adaptive minConf)**: **NO DIRECT PAPER MATCH FOUND.** Literature extensively covers adaptive minSup (per-class/item support), cost-sensitive learning, and threshold tuning, but adaptive minConf remains largely unexplored. Your formula `minConf(c) = min(globalMinConf, max(floor, lift × P(c)))` appears novel. **Acknowledge as contribution; position as natural complement to MMSCBA.**

---

## Topic 1: MMSCBA (Hu et al. 2016) vs H2

### MMSCBA Core Technique

**Paper**: "Building an associative classifier with multiple minimum supports" | SpringerPlus 5(1), 2016
**DOI**: 10.1186/s40064-016-2153-1
**Full Text Access**: [PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC4844591/), [SpringerPlus](https://springerplus.springeropen.com/articles/10.1186/s40064-016-2153-1)

#### Key Formula

Each item has a **Minimum Item Support (MIS)**. Classes have **Minimum Class Support (MCS)**. For rule-items:

```
MRS(β) = min(MIS(i₁), MIS(i₂), ..., MIS(iₘ), MCS(yβ))
```

Where β = rule antecedent + class label.

#### Support Assignment Method

- Per-item: `MIS(iₚ) = σ × f(iₚ)` (σ ∈ [0,1], f = item frequency)
- Per-class: User-specified; paper gives no explicit formula but shows class-level tuning is central

**Problem Solved**: Rare item problem—frequent items get higher thresholds; rare items get lower ones.

### H2 vs MMSCBA Comparison

| Aspect | MMSCBA | H2 |
|--------|--------|--|-
| **Support Type** | Per-item + per-class | Per-class only |
| **Class Formula** | User-specified (implicit) | `minSup(c) = max(2, ⌊supPct × freq(c)⌋)` |
| **Frequency Basis** | Yes (σ tuning parameter) | Yes (supPct parameter) |
| **Floor Constraint** | Not explicit | Explicit `max(2, ...)` |
| **Status** | Published 2016 | Your implementation |

**Conclusion**: H2 is a **simplified, frequency-proportional instance** of MMSCBA's MCS concept. Differences are implementation details, not conceptual divergence.

---

## Topic 2: Adaptive Minimum Confidence

### Literature Findings

#### 1. Adaptive Support (Well-Established)
- [Threshold Tuning for Improved Classification Association Rule Mining](https://www.researchgate.net/publication/220894612_Threshold_Tuning_for_Improved_Classification_Association_Rule_Mining)
- [Minimum threshold determination based on dataset characteristics](https://link.springer.com/article/10.1186/s40537-021-00538-3)
- **Finding**: Automatic threshold determination for **minSup** per dataset is established; per-class minSup extensions exist (MMSCBA, others).

#### 2. Cost-Sensitive Rules (Related but Different)
- [A Cost-Sensitive Based Approach for Improving Associative Classification on Imbalanced Datasets](https://link.springer.com/chapter/10.1007/978-3-319-08979-9_3)
- [Associative Classifiers Algorithms for Imbalanced Data: Systematic Review](https://link.springer.com/chapter/10.1007/978-3-032-12882-9_19)
- **Finding**: Cost matrices adjust **ranking/weighting** of rules post-mining, not confidence threshold during mining.

#### 3. Class-Specific Confidence (Literature Gap)
Searched terms:
- "class-specific minimum confidence"
- "per-class confidence threshold"
- "adaptive minConf association rules"
- "confidence threshold per-class imbalanced"

**Result**: No papers found proposing adaptive minConf per class during rule mining. Literature states:

> "Minimum confidences do not affect classification much because classifiers tend to use high confidence rules." — mlxtend/association_rules documentation

Confidence is treated as **uniform/global** across classes in standard literature.

#### 4. What Papers DO Cover

**For Imbalanced Data**:
- Adaptive minSup (per class/item) ✓ — MMSCBA, others
- Cost-sensitive ranking (post-mining) ✓ — SSCR, others
- Class-weighted sampling ✓ — SMOTE, imbalanced resampling
- **Adaptive minConf** ✗ — Not found in literature

---

## H3 Novelty Assessment

### Formula Breakdown

```
minConf(c) = min(globalMinConf, max(floor, lift × P(c)))
where:
  P(c) = freq(c) / N
  lift = 5 (example)
  floor = 0.3
```

**Logic**: Minority classes (low P(c)) get higher confidence floors; majority classes retain global minConf. Lift parameter calibrates sensitivity to class rarity.

### Is H3 Novel?

**Evidence H3 is Original**:
1. No papers found with per-class minConf formula
2. Literature treats confidence as **dataset-wide constant**
3. Imbalance handling focuses on minSup, not minConf
4. Cost-sensitive approaches work post-mining, not during rule generation

**Closest Related Work** (but not equivalent):
- **MMSCBA**: Adaptive minSup (not minConf)
- **SSCR** (cost-sensitive CBA): Weights rules by cost after mining (not threshold-based)
- **CBA/CMAR**: Fixed minConf across classes

**Conclusion**: H3 appears to be **your original contribution**. No prior art directly proposes adaptive minConf per class.

---

## Recommendations

### For Thesis Writing

1. **H2 (Class-specific minSup)**:
   - Cite Hu et al. (2016) MMSCBA
   - Frame H2 as: "Following MMSCBA's principle of class-specific support thresholds, we propose a frequency-proportional formulation..."
   - No novelty claim needed; proper attribution sufficient

2. **H3 (Adaptive minConf)**:
   - Acknowledge no direct literature precedent
   - Position as: "To complement class-specific support (H2), we introduce adaptive class-specific confidence thresholds, a novel extension addressing minority class under-representation in rule mining"
   - Compare to MMSCBA and cost-sensitive methods to show orthogonal contribution
   - Empirically validate H3's impact (which your v11/v12 experiments do)

3. **Combined H2+H3**:
   - Present as integrated framework extending MMSCBA
   - H2 adapts support threshold (existing idea, your implementation)
   - H3 adapts confidence threshold (novel idea, your contribution)
   - Together: comprehensive two-stage threshold adaptation for imbalanced classification

### Experimental Validation

Your v11/v12 results (Lymph F1: 0.42→0.74, 20 UCI datasets) provide strong empirical evidence of efficacy. This is crucial since H3 lacks prior art—empirical validation becomes the proof-point.

---

## Unresolved Questions

1. Why does literature ignore adaptive minConf despite widespread adaptive minSup research? Possible reasons:
   - Confidence rarely bottlenecks performance vs. support
   - Post-mining cost-sensitive weighting considered sufficient
   - Computational complexity concerns not documented

2. Could H3 be a special case of cost-sensitive learning in disguise? Possibly, but your formulation is rule-mining-time adaptation, not post-hoc weighting.

3. Do existing papers on "threshold determination" indirectly cover minConf? Full paper access needed (paywalls encountered).

---

## Sources Cited

- [MMSCBA Full Text (PMC)](https://pmc.ncbi.nlm.nih.gov/articles/PMC4844591/)
- [MMSCBA (SpringerPlus)](https://springerplus.springeropen.com/articles/10.1186/s40064-016-2153-1)
- [Cost-Sensitive CBA for Imbalanced Data](https://link.springer.com/chapter/10.1007/978-3-319-08979-9_3)
- [Associative Classifiers Imbalance Review](https://link.springer.com/chapter/10.1007/978-3-032-12882-9_19)
- [Threshold Tuning for CAR Mining](https://www.researchgate.net/publication/220894612_Threshold_Tuning_for_Improved_Classification_Association_Rule_Mining)
- [mlxtend Association Rules](https://rasbt.github.io/mlxtend/user_guide/frequent_patterns/association_rules/)
- [Wikipedia: Association Rule Learning](https://en.wikipedia.org/wiki/Association_rule_learning)
