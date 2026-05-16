# CMAR Class-Specific Minimum Support Claim Verification

## VERDICT: CLAIM IS HALLUCINATED (NOT IN ORIGINAL CMAR 2001 PAPER)

---

## Question 1-3: Does CMAR Paper Contain Class-Specific MinSup?
**ANSWER: NO**

The original CMAR paper by Li, Han, & Pei (2001) uses a **SINGLE global minimum support threshold** for all items, classes, and itemsets.

**Evidence:**
- "Most CBA-based methods (Li et al. 2001) adopt a **single threshold of minimum support for all items**"
- CMAR does NOT employ per-class support thresholds in the original 2001 publication

---

## Question 4: Which Algorithm/Paper Has This Formula?
**ANSWER: Liu et al. (2000) - NOT CMAR**

The formula: `minsupp_c = minsupp_t × supp(c) / max(supp(C))`

**Actually from:**
- **Liu B., Ma Y., Wong C.K. (2000)** "Improving an Association Rule Based Classifier"
- Published in PKDD 2000 (Springer LNCS vol 1910)
- This is a CBA extension, NOT the original CMAR algorithm

---

## Question 5: Confirm CMAR Uses Global MinSup?
**CONFIRMED: YES**

CMAR (Li et al. 2001) uses:
- **Single minimum support threshold across entire dataset**
- **Single minimum confidence threshold**
- Class distribution maintained per frequent itemset (not per-class thresholds)

CMAR's innovation was NOT class-specific support, but rather:
1. Using FP-Growth for efficiency
2. Maintaining class distribution in FP-tree
3. Chi-square significance testing
4. CR-tree storage structure
5. Multiple rules weighted for classification

---

## Citation Correction
**DO NOT cite the formula as CMAR**

If discussing class-specific minimum support for handling class imbalance:
- Cite: Liu B., Ma Y., Wong C.K. (2000) "Improving an Association Rule Based Classifier"
- Cite as: CBA extension with multiple minimum supports (NOT original CMAR)

Later extensions (e.g., MMSCBA) built on this, but CMAR 2001 is NOT the source.

---

## Conclusion for Thesis Defense
**The previous researcher hallucinated this claim.** CMAR's actual contribution is elegant design (FP-tree + CR-tree + chi-square) not per-class support. Use Liu (2000) if discussing class-specific thresholds.
