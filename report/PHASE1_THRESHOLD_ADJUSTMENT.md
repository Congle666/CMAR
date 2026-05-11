# 🔬 F1 Score & Recall Optimization - Hướng 1: Threshold Adjustment

**Ngày**: Tháng 5 2026 | **Phương pháp**: Threshold Adjustment (0.3, 0.4, 0.5) | **Scope**: 5 Imbalanced Datasets

---

## 📋 Executive Summary

### Mục Tiêu Hướng 1
- Điều chỉnh **classification threshold** từ 0.5 (baseline) → 0.3, 0.4 để **tăng Recall**
- **Trade-off**: Recall ↑, Precision ↓ (false positive tăng)
- **Kỳ vọng**: +3-8% Recall, +0-3% F1 trên 5 imbalanced datasets

### Các Loại Dataset Test
```
LYMPH (148 records, 4 classes) - WORST F1=0.42
  ├─ metastases:  81 (Recall: 0.84 - OK)
  ├─ malign_lymph: 61 (Recall: 0.90 - OK)  
  ├─ fibrosis:     4 (Recall: 0.00 - ❌ MISS hoàn toàn!)
  └─ normal:       2 (Recall: 0.00 - ❌ MISS hoàn toàn!)

GLASS (214 records, 6 classes) - BAD F1=0.61
  └─ vehicle_float: 17 (Recall: 0.18 - ❌ Very Low!)

VEHICLE (846 records, 4 classes) - MEDIUM F1=0.65
  ├─ opel: 212 (Recall: 0.30 - ❌ Low)
  └─ saab: 217 (Recall: 0.48 - ⚠️ Medium)

HEPATITIS (155 records, 2 classes) - MEDIUM F1=0.74
  └─ DIE (minority): 32 (Recall: 0.62 - ⚠️)

GERMAN (1000 records, 2 classes) - MEDIUM F1=0.66
  └─ bad (minority): 300 (Recall: 0.43 - ⚠️)
```

---

## 🔍 Cơ Chế Hướng 1: Threshold Adjustment

### Công Thức
```
Hiện tại (Baseline, T=0.5):
  Dự đoán class Positive nếu: confidence(rule) ≥ 0.5
  
  confidence(rule) = support(A ∩ B ∩ C) / support(A ∩ B)

Cải tiến (T=0.3 hoặc T=0.4):
  Dự đoán class Positive nếu: confidence(rule) ≥ 0.3 (or 0.4)
  
  → Rules "yếu" (confidence 0.3-0.5) cũng được dùng
  → Tự động tăng Recall
```

### Ví Dụ
```
Dataset LYMPH, class="fibrosis" (4 mẫu)

Baseline (T=0.5):
  Rule 1: items={X,Y} → fibrosis, confidence=0.4 ❌ REJECT (< 0.5)
  Rule 2: items={Z} → fibrosis, confidence=0.6 ✓ ACCEPT
  → Chỉ 1 rule chứa fibrosis → Recall = 25% (bắt 1/4)
  
With Threshold T=0.3:
  Rule 1: items={X,Y} → fibrosis, confidence=0.4 ✓ ACCEPT (>= 0.3)
  Rule 2: items={Z} → fibrosis, confidence=0.6 ✓ ACCEPT  
  → 2 rules cho fibrosis → Recall = 50-75% (bắt 2-3/4)
```

---

## 📊 Bảng 1: Chi Tiết Kết Quả Threshold Adjustment (5 Imbalanced)

| Dataset | Metric | Baseline (0.5) | T=0.4 | T=0.3 | Best | Δ |
|---------|--------|:--------------:|:----:|:----:|:----:|---:|
| **LYMPH** | Accuracy | 83.46% | ? | ? | ? | ? |
| | Macro-F1 | 0.4235 | ? | ? | ? | ? |
| | Recall (minor) | 0.00 | ? | ? | ? | ? |
| | Precision | 0.42 | ? | ? | ? | ? |
| **GLASS** | Accuracy | 66.11% | ? | ? | ? | ? |
| | Macro-F1 | 0.6113 | ? | ? | ? | ? |
| | Recall (minor) | 0.18 | ? | ? | ? | ? |
| | Precision | 0.63 | ? | ? | ? | ? |
| **VEHICLE** | Accuracy | 67.83% | ? | ? | ? | ? |
| | Macro-F1 | 0.6477 | ? | ? | ? | ? |
| | Recall (opel) | 0.30 | ? | ? | ? | ? |
| | Precision | 0.67 | ? | ? | ? | ? |
| **HEPATITIS** | Accuracy | 81.81% | ? | ? | ? | ? |
| | Macro-F1 | 0.7363 | ? | ? | ? | ? |
| | Recall (DIE) | 0.62 | ? | ? | ? | ? |
| | Precision | 0.73 | ? | ? | ? | ? |
| **GERMAN** | Accuracy | 74.20% | ? | ? | ? | ? |
| | Macro-F1 | 0.6639 | ? | ? | ? | ? |
| | Recall (bad) | 0.43 | ? | ? | ? | ? |
| | Precision | 0.68 | ? | ? | ? | ? |
| | | | | | | |
| **AVG (5 datasets)** | **Accuracy** | **74.68%** | **?** | **?** | **?** | **?** |
| | **Macro-F1** | **0.5765** | **?** | **?** | **?** | **?** |

---

## 📈 Bảng 2: Improvement Analysis (vs Baseline 0.5)

| Dataset | T=0.4 Δ Acc | T=0.4 Δ F1 | T=0.3 Δ Acc | T=0.3 Δ F1 | Better T |
|---------|:-----------:|:---------:|:-----------:|:---------:|:--------:|
| LYMPH | ? | ? | ? | ? | ? |
| GLASS | ? | ? | ? | ? | ? |
| VEHICLE | ? | ? | ? | ? | ? |
| HEPATITIS | ? | ? | ? | ? | ? |
| GERMAN | ? | ? | ? | ? | ? |
| **AVG** | **?** | **?** | **?** | **?** | **?** |

---

## 🎯 Bảng 3: Trade-off Analysis (Precision vs Recall)

| Dataset | Threshold | Recall ↑ | Precision ↓ | F1 Impact | Status |
|---------|:---------:|:-------:|:---------:|:---------:|:------:|
| LYMPH | 0.4 | +0-5% | -5-10% | ? | TBD |
| | 0.3 | +5-15% | -10-20% | ? | TBD |
| GLASS | 0.4 | +10-20% | -10-15% | ? | TBD |
| | 0.3 | +20-35% | -15-25% | ? | TBD |
| VEHICLE | 0.4 | +15-25% | -10-15% | ? | TBD |
| | 0.3 | +25-40% | -15-20% | ? | TBD |
| HEPATITIS | 0.4 | +5-10% | -5-10% | ? | TBD |
| | 0.3 | +10-20% | -10-15% | ? | TBD |
| GERMAN | 0.4 | +10-20% | -10-15% | ? | TBD |
| | 0.3 | +20-30% | -15-20% | ? | TBD |

---

## 💡 Kỳ Vọng & Giả Thuyết

### Dự Báo Tích Cực
✓ **Threshold = 0.4** sẽ là "sweet spot":
- Recall tăng +3-15% trên minority classes
- Precision không giảm quá nhiều
- F1 tăng nhẹ +1-3%

✓ **Lymph & Glass** (worst baseline) sẽ cải tiến lớn nhất:
- Fibrosis & normal class trong lymph: recall 0% → +15-25%
- Vehicle_float trong glass: recall 18% → +20-40%

### Dự Báo Tiêu Cực  
❌ **Threshold = 0.3** quá mạnh:
- Precision sẽ giảm >20% (false positive tăng)
- F1 có thể giảm do precision collapse
- Chỉ tốt nếu business case yêu cầu recall tối đa

### Giả Thuyết Khoảng
```
Likely Outcomes:
- T=0.4: F1 +1-3% on average (Best)
- T=0.3: F1 -1-2% on average (Too aggressive)
- T=0.5: Baseline (no change)
```

---

## 🔧 Implementation Details

### Código Modified
**File**: `CMARClassifier.java`

```java
// Thêm field
private double classificationThreshold = 0.5;

// Thêm setter
public void setThreshold(double threshold) { 
    this.classificationThreshold = threshold; 
}

// Modify classify() method
public String classify(Transaction record) {
    List<AssociationRule> matching = crTree.findMatching(record);
    
    if (matching.isEmpty()) return defaultClass;
    
    // ← HƯỚNG 1: Threshold Adjustment
    matching = filterRulesByThreshold(matching, classificationThreshold);
    if (matching.isEmpty()) return defaultClass;
    
    // ... rest of classification logic
}

// New method: Filter Rules By Threshold
private List<AssociationRule> filterRulesByThreshold(
        List<AssociationRule> rules, double threshold) {
    if (threshold >= 1.0) return rules;
    
    List<AssociationRule> filtered = new ArrayList<>();
    for (AssociationRule rule : rules) {
        double confidence = (double) rule.getSupportCount() / 
                           Math.max(1, rule.getCondsetSupportCount());
        if (confidence >= threshold) {
            filtered.add(rule);
        }
    }
    return filtered.isEmpty() ? rules : filtered;
}
```

### Benchmark Class
**File**: `BenchmarkPhase1.java`
- Test: 5 imbalanced datasets
- Thresholds: 0.3, 0.4, 0.5 (baseline)
- CV: 10-fold stratified
- Metrics: Accuracy, Macro-F1, Weighted-F1
- Status: ✅ Running

---

## 📝 Benchmark Status

### Execution Timeline
```
Start:   [TIME_START]
Current: Running...
Datasets:
  ✓ LYMPH       - Fold 1/10 completed
  ⏳ GLASS       - Queued
  ⏳ VEHICLE     - Queued
  ⏳ HEPATITIS   - Queued
  ⏳ GERMAN      - Queued

Est. Total Time: 15-20 minutes
```

---

## 🎯 Next Steps

### After Phase 1 Complete:
1. ✅ Analyze results (Bảng 1-3 sẽ được fill)
2. 📊 Compare T=0.3 vs T=0.4 vs T=0.5 
3. 🚀 If T=0.4 shows +F1, proceed to **Phase 2: Class Weighting**
4. 📈 Otherwise, reconsider thresholds or move to different hướng

### Kiến Nghị Nếu Kết Quả Tốt (F1 +1-3%):
```
DECISION TREE:
├─ If F1 +2-3% && Precision OK
│  └─ Confirm T=0.4 & move to Phase 2 (Class Weighting)
│
├─ If F1 +0-1% && Trade-off acceptable
│  └─ Accept T=0.4 as viable alternative
│
└─ If F1 -1% || Precision collapsed (-20%)
   └─ Reject T=0.3/T=0.4, move directly to Phase 2 (weighting)
```

---

## 📚 Reference

### Công Thức Confidence
```
confidence(A → C) = support(A ∩ C) / support(A)
                  = Number of records where (A matches AND C is class) 
                    / Number of records where A matches
```

### Công Thức F1
```
Macro-F1 = (1/|Classes|) × Σ F1(class)
F1(class) = 2 × (precision × recall) / (precision + recall)
```

---

**Status**: ⏳ PHASE 1 IN PROGRESS  
**Updated**: May 11, 2026  
**Next Report**: When Phase 1 completes
