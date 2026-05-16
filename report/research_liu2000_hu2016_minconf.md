# Research: Liu 2000 & Hu 2016 — Class-Specific Minimum Confidence Analysis

## Executive Summary

**Critical Finding:** Neither Liu et al. (2000) nor Hu et al. (2016/MMSCBA) propose class-specific minimum confidence thresholds (minconf_i per class).

- **Liu 2000**: Proposes class-specific minSup via formula `minsupp_i = minsupp_t × supp(c_i) / max(supp(C))`. **NO mention of per-class minConf.**
- **MMSCBA 2016**: Extends to multiple minimum supports (MIS per item, MCS per class). Confidence remains **uniformly global** across all rules.

## Liu et al. (2000) — "Improving an Association Rule Based Classifier"

**Paper Details:**
- Published: PKDD 2000, LNCS 1910, pp. 504–509
- Authors: B. Liu, Y. Ma, C.K. Wong
- Focus: Improve CBA (Classification Based on Association Rules) system

**What Liu 2000 Proposes:**
Liu 2000 introduces **class-specific minimum support** to address class imbalance:
```
minsupp_i = minsupp_t × supp(c_i) / max(supp(C))
```
Where:
- `minsupp_i` = min support for class i
- `minsupp_t` = total/baseline minimum support
- `supp(c_i)` = support (frequency) of class i
- `max(supp(C))` = support of majority class

**What Liu 2000 Does NOT Propose:**
- No per-class minimum confidence thresholds (minconf_i)
- No adaptive confidence by class
- No mention of confidence threshold adjustment for minority classes
- Confidence remains a **single global threshold** applied uniformly

**Search Evidence:**
- Search: "Liu" "Ma" "Wong" 2000 minSup minConf class-specific → No results discussing minConf per class
- Search: Liu 2000 "multiple minimum support" -confidence → Confirms support-only focus
- Secondary sources (SPMF, arulesCBA docs) cite Liu 2000 only for **minSup per class**, never minConf per class

## Hu et al. (2016) — MMSCBA: "Building an Associative Classifier with Multiple Minimum Supports"

**Paper Details:**
- Published: SpringerPlus, April 2016
- Authors: Hu, L.Y., Hu, Y.H., Tsai, C.F.
- Available Open Access: https://pmc.ncbi.nlm.nih.gov/articles/PMC4844591/

**What MMSCBA Proposes:**
MMSCBA extends CBA with **multiple minimum supports but NOT confidence**:
- MIS (Minimum Item Support): Different support thresholds per item
- MCS (Minimum Class Support): Different support thresholds per class
- MRS (Minimum Rule-Item Support): Combines MIS and MCS

**What MMSCBA Does NOT Propose:**
Direct quote from full text analysis:
> "The confidence of a frequent rule-item β is defined as: r_conf_D(β) = r_supp_D(β) / e_supp_D(β)"
> [Single universal formula, NOT adapted per class]

MMSCBA explicitly acknowledges:
- Four prediction methods (Maximum likelihood, Max χ², Laplace, Scoring) all use **global, uniform confidence**
- Post-rule-discovery methods calculate class-specific accuracy but **NOT during rule generation**
- Innovation is **support dimension only** — "addresses the rare item problem in CBA-based methods through multiple minimum supports"

**Search Evidence:**
- Direct text read: "users can specify different values of MIS for different items" and "a different minimum class support is assigned for each class label" — but confidence unchanged
- All MMSCBA discussions focus on MIS/MCS, never minconf_i
- MMSCBA paper explicitly states innovation addresses "rare item problem" via **support**, not confidence

## Ancestry of H3 (Per-Class minConf)

**H3 Conclusion: H3 appears to be ORIGINAL beyond these foundational works.**

Neither Liu 2000 nor MMSCBA 2016 propose:
- Per-class minimum confidence thresholds during rule generation
- Adaptive confidence scaling by class prevalence
- Any formula for minconf_i

**Gap in Literature:**
- Liu 2000 → class-specific minSup (solves rule discovery for rare classes)
- MMSCBA 2016 → extends to items + classes, still only minSup
- **H3 → class-specific minConf (solves rule quality/precision for rare classes)**

This represents an **orthogonal contribution** not previously explored in CBA literature.

## References

- [Liu, Ma, Wong 2000 on SpringerLink](https://link.springer.com/chapter/10.1007/3-540-45372-5_58)
- [Liu, Ma, Wong 2000 on Semantic Scholar](https://www.semanticscholar.org/paper/Improving-an-Association-Rule-Based-Classifier-Liu-Ma/0ce709a823996b78372e0c503192f83dac2d34e8)
- [MMSCBA 2016 on PMC (Open Access)](https://pmc.ncbi.nlm.nih.gov/articles/PMC4844591/)
- [MMSCBA 2016 on SpringerPlus](https://link.springer.com/article/10.1186/s40064-016-2153-1)
