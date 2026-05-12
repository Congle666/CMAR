# H2, H3 & SMOTE — Ba cải tiến cho CMAR trên Imbalanced Datasets

**Ngày:** 2026-05-12
**Phiên bản code:** v12 (sau aggressive cleanup)
**Đối tượng:** sinh viên/researcher đọc CMAR & WCBA paper, muốn hiểu rõ 3 cải tiến đã làm.

---

## Mục lục

1. [Vấn đề gốc — Imbalanced Dataset](#1-vấn-đề-gốc--imbalanced-dataset)
2. [H2 — Class-specific minSupport](#2-h2--class-specific-minsupport)
3. [H3 — Adaptive minConfidence](#3-h3--adaptive-minconfidence)
4. [SMOTE — Synthetic Minority Over-sampling](#4-smote--synthetic-minority-over-sampling)
5. [Áp dụng từ paper WCBA 2018](#5-áp-dụng-từ-paper-wcba-2018)
6. [So sánh tác dụng — Honest Analysis](#6-so-sánh-tác-dụng--honest-analysis)
7. [Khi nào dùng cái gì](#7-khi-nào-dùng-cái-gì)
8. [Mapping code → công thức](#8-mapping-code--công-thức)

---

## 1. Vấn đề gốc — Imbalanced Dataset

### 1.1 Bản chất

CMAR (Li, Han, Pei 2001) mining Class Association Rules dựa trên 3 ngưỡng:

| Ngưỡng | Ý nghĩa | Giá trị mặc định |
|--------|---------|:----------------:|
| `minSupport` | support tuyệt đối tối thiểu | `supPct × N` (~5%) |
| `minConfidence` | xác suất rule → class tối thiểu | `0.5` |
| `χ²` | ngưỡng tương quan thống kê | `3.841` (p=0.05) |

### 1.2 Tại sao gãy trên imbalanced data

Ví dụ **Lymph** (148 records, 4 classes):
```
metastases    81 records  (54.7%)  ← majority
malign_lymph  61 records  (41.2%)
fibrosis       4 records  ( 2.7%)  ← minority
normal         2 records  ( 1.4%)  ← minority
```

Với `minSupport = 5% × 148 = 7`:

- Rule `X → fibrosis`: support tối đa = **4** (số records fibrosis) → **không thể** đạt minSup = 7.
- Rule `X → normal`: support tối đa = **2** → **không thể** đạt minSup = 7.

→ Mining sinh **0 rules** cho 2 class này → classifier không có rule để predict → **F1(fibrosis) = F1(normal) = 0**.

**Đây là root cause** của imbalance problem trong CMAR.

---

## 2. H2 — Class-specific minSupport

### 2.1 Ý tưởng

Thay vì **một ngưỡng minSup toàn cục**, **mỗi class có ngưỡng riêng** tỷ lệ với size của class:

```
minSup(c) = max(2, ⌊supPct × freq(c)⌋)
```

### 2.2 Ví dụ Lymph (supPct = 5%)

| Class | freq(c) | Global minSup | **H2 minSup(c)** |
|-------|:-------:|:-------------:|:----------------:|
| metastases | 81 | 7 | 4 (= max(2, ⌊0.05×81⌋)) |
| malign_lymph | 61 | 7 | 3 (= max(2, ⌊0.05×61⌋)) |
| **fibrosis** | **4** | **7 ❌** | **2** ✅ |
| **normal** | **2** | **7 ❌** | **2** ✅ |

### 2.3 Trong code

File [src/CrossValidator.java](../src/CrossValidator.java):
```java
// H2: minSup(c) = supPct × freq(c)
Map<String, Integer> classMinSupMap = new HashMap<>();
for (Map.Entry<String, Integer> e : classFreq.entrySet()) {
    int thr = Math.max(2,
        (int) Math.round(classMinSupFraction * e.getValue()));
    classMinSupMap.put(e.getKey(), thr);
}
```

File [src/FPGrowth.java](../src/FPGrowth.java):
```java
private int classThreshold(String cls) {
    if (classMinSupMap == null) return minSupport;
    return classMinSupMap.getOrDefault(cls, minSupport);
}

// Trong rule emission:
if (classSup < classThreshold(cls)) continue;
```

### 2.4 ⚠️ H2 KHÔNG phải silver bullet — Honest

H2 chỉ là **điều kiện cần** để minority class có rule, **không phải điều kiện đủ**.

**Bằng chứng:** Lymph với H2+H3 (không SMOTE) → MacroF1 = **0.4181** (thậm chí tệ hơn baseline 0.4235).

Lý do: với 4 records fibrosis, rule mining có thể sinh được rules support ≥ 2, nhưng các rules đó **overfit** 2-3 records cụ thể → confidence không ổn định qua các fold CV.

**Đó là lý do tại sao cần SMOTE.**

---

## 3. H3 — Adaptive minConfidence

### 3.1 Ý tưởng

Tương tự H2, **mỗi class có ngưỡng confidence riêng**, thấp hơn cho minority:

```
minConf(c) = min(globalMinConf, max(floor, lift × freq(c) / N))
```

Với `floor = 0.3`, `lift = 5.0` (mặc định):

### 3.2 Ví dụ Lymph (globalMinConf = 0.5, N = 148)

| Class | freq(c) | P(c) = freq/N | 5 × P(c) | max(0.3, ...) | min(0.5, ...) | **minConf(c)** |
|-------|:-------:|:-------------:|:--------:|:-------------:|:-------------:|:--------------:|
| metastases | 81 | 0.547 | 2.74 | 2.74 | **0.5** | 0.5 |
| malign_lymph | 61 | 0.412 | 2.06 | 2.06 | **0.5** | 0.5 |
| **fibrosis** | **4** | **0.027** | **0.135** | **0.3** | **0.3** | **0.3** ✅ |
| **normal** | **2** | **0.014** | **0.070** | **0.3** | **0.3** | **0.3** ✅ |

→ Rule `X → fibrosis` chỉ cần confidence 30% (vẫn cao **gấp 11×** baseline ngẫu nhiên 2.7%).

### 3.3 Trong code

File [src/CrossValidator.java](../src/CrossValidator.java):
```java
// H3: minConf(c) = min(globalMinConf, max(floor, lift × freq(c)/N))
Map<String, Double> classMinConfMap = new HashMap<>();
int N = trainData.size();
for (Map.Entry<String, Integer> e : classFreq.entrySet()) {
    double classRatio = (double) e.getValue() / N;
    double thr = Math.min(minConfidence,
        Math.max(adaptiveMinConfFloor,
                 adaptiveMinConfLift * classRatio));
    classMinConfMap.put(e.getKey(), thr);
}
```

File [src/FPGrowth.java](../src/FPGrowth.java):
```java
private double classMinConfidence(String cls) {
    if (classMinConfMap == null) return minConfidence;
    return classMinConfMap.getOrDefault(cls, minConfidence);
}

// Trong rule emission:
if (confidence < classMinConfidence(cls)) continue;
```

### 3.4 Tại sao công thức như vậy

- **Baseline confidence** cho class c (random guess) = `P(c) = freq(c) / N`.
- Rule `X → c` chỉ "có ý nghĩa" nếu confidence cao **hơn baseline đáng kể**.
- `lift = 5` nghĩa là: rule phải cao hơn baseline **5 lần** mới được giữ.
- `floor = 0.3` đảm bảo dù minority cực nhỏ vẫn cần ít nhất 30% confidence (không nới quá rộng).
- `min(globalMinConf, ...)` đảm bảo majority class **không bị nới lỏng** (giữ 50%).

---

## 4. SMOTE — Synthetic Minority Over-sampling

### 4.1 Định nghĩa

**SMOTE** (Chawla, Bowyer, Hall, Kegelmeyer 2002 — JAIR vol 16): tạo records **nhân tạo** cho minority class bằng cách interpolate giữa records gốc và k-nearest neighbors.

### 4.2 SMOTE-N (variant cho Categorical data)

CMAR mining trên **categorical** (`attribute = value`) → SMOTE numeric không dùng được. Chawla et al. đề xuất **SMOTE-N**:

| Aspect | SMOTE gốc (numeric) | **SMOTE-N (categorical)** |
|--------|---------------------|---------------------------|
| Distance | Euclidean | **Hamming** (số attribute khác nhau) |
| Cách tạo value | Linear interpolation | **Mode voting** (giá trị xuất hiện nhiều nhất) |
| Phù hợp | Numeric data | **Categorical** ← CMAR dùng |

### 4.3 Pseudo-code SMOTE-N

```
function smote_n(data, k=5, target_ratio=1.0, seed):
    group records by class
    max_freq = max class size
    target = round(max_freq × target_ratio)

    for each class c có size < target:
        needed = target - size(c)
        for i in 1..needed:
            base = random record của class c
            neighbors = k-NN(base, class_c_records) via Hamming
            For each attribute a:
                pool = {base[a]} ∪ {n[a] for n in neighbors}
                mode_value = most frequent value in pool (random tie-break)
                synthetic[a] = mode_value
            new_record = Transaction(synthetic, class=c)
            data.append(new_record)
    return data
```

### 4.4 Ví dụ Lymph (fibrosis = 4 records → target = 81)

**Tạo synthetic #1:**
- base = fibrosis_r1
- 3 nearest neighbors (k_eff = min(5, 3) = 3): r2, r3, r4
- Cho mỗi attribute:
  - `lymphatics`: pool = {normal, arched, deformed, normal} → mode = `normal`
  - `block_lymph_c`: pool = {no, no, yes, no} → mode = `no`
  - ...
- Synthetic = `Transaction(lymphatics=normal, block_lymph_c=no, ..., class=fibrosis)`

Lặp 77 lần → fibrosis có 81 records (= majority size).

### 4.5 Adaptive SMOTE (v11+)

**Không** bật SMOTE blindly trên mọi dataset. Có **trigger heuristic**:

```
SMOTE_TRIGGER = 10  // ngưỡng minority size
SMOTE_RATIO   = 1.0 // target ratio = max_class_freq

For each fold của CV:
    min_freq = min frequency of any class in train fold
    if min_freq < SMOTE_TRIGGER:
        apply SMOTE-N(train_fold, k=5, target_ratio=1.0)
    else:
        skip SMOTE
```

→ Trên 20 UCI datasets, SMOTE **chỉ kích hoạt 3/20**: lymph, glass, zoo.
→ 17/20 datasets còn lại **không bị động** → zero regression.

### 4.6 Trong code

File [src/SMOTE.java](../src/SMOTE.java) — categorical SMOTE-N implementation:
```java
public static List<Transaction> apply(List<Transaction> data, int k,
                                        double targetRatio, long seed) {
    // group by class, find max_freq, target
    // for each minority class:
    //   k-NN via Hamming distance
    //   create synthetic via mode voting
    //   add to augmented set
    return augmented;
}
```

File [src/CrossValidator.java](../src/CrossValidator.java) — integration:
```java
if (smoteTargetRatio > 0) {
    trainData = SMOTE.apply(trainData, 5, smoteTargetRatio, seed + fold);
    minSupport = Math.max(2, (int) Math.round(trainData.size() * minSupportPct));
    // ... recompute classFreq for H2/H3 since data size changed
}
```

File [src/BenchmarkSMOTEFull.java](../src/BenchmarkSMOTEFull.java) — adaptive trigger:
```java
if (useSMOTE) {
    int minFreq = computeMinClassFreq(data);
    if (minFreq < SMOTE_TRIGGER) smoteRatio = SMOTE_RATIO;
}
```

---

## 5. Áp dụng từ paper WCBA 2018

Paper: *Weighted Classification Based on Association rules algorithm* (2018).

### 5.1 Ý tưởng WCBA gốc

WCBA đề xuất một số kỹ thuật để cải thiện CBA/CMAR trên imbalanced data:
1. **Harmonic Mean (HM)** thay vì χ² hoặc confidence cho rule ranking.
2. **Class weight** trong tính chi-square để boost minority.
3. **Strong + Spare rules**: 2-stage prediction (Strong rules first, Spare fallback).
4. **Attribute weights** (Information Gain) để weighted support.

### 5.2 Lịch sử áp dụng trong project (v6 → v11)

| Phiên bản | Ý tưởng từ WCBA | Kết quả |
|:---------:|-----------------|---------|
| v6 | Stratified Top-K (extension) | Cải tiến nhẹ |
| v7 | Class-weighted χ² + IG weights | Mixed results |
| v8 | HM ranking + Strong+Spare rules | Marginal |
| v9 | **H2 + H3** (class-specific thresholds — extension của "class weight") | Khá tốt cho moderate imbalance |
| v10 | SMOTE + Class Score Boost (Boost thừa khi đã SMOTE) | Lymph breakthrough |
| **v11** | **H2 + H3 + Adaptive SMOTE** (final) | **+18% MacroF1 trên lymph** |

### 5.3 Đã GIỮ lại từ WCBA trong code v11+ (sau cleanup)

| Ý tưởng WCBA | Trong code? | Lý do |
|--------------|:-----------:|-------|
| Class-specific thresholds (H2/H3) | ✅ giữ | **Backbone** của v11 |
| Harmonic Mean ranking | ❌ xoá | Không add giá trị khi đã có SMOTE |
| Strong + Spare rules | ❌ xoá | Cải tiến marginal, complexity cao |
| Class-weighted χ² | ❌ xoá | Bị SMOTE thay thế (SMOTE balance class trước mining) |
| Attribute weights (IG) | ❌ xoá | Cải tiến mơ hồ, chưa validate được |

→ **Tinh thần KISS**: chỉ giữ thứ thực sự đóng góp.

### 5.4 H2 + H3 là EXTENSION của WCBA

WCBA gốc weight **classification stage** (χ² lúc predict). H2 + H3 extend ý tưởng đó xuống **mining stage** (FPGrowth lúc sinh rules). Lợi ích:

- Mining stage: H2 + H3 đảm bảo minority class **có rules** để classify.
- Classification stage: dùng χ² gốc (đã đủ), không cần weight thêm.

---

## 6. So sánh tác dụng — Honest Analysis

### 6.1 Benchmark v11 trên 3 datasets imbalanced (Lymph / Hepatitis / German)

| Variant | Lymph | Hepatitis | German | AVG |
|---------|:-----:|:---------:|:------:|:---:|
| Baseline | 0.4235 | 0.7363 | 0.6639 | 0.6079 |
| Light (H2+H3) | 0.4181 ⚠️ | 0.7430 | 0.6903 | 0.6171 |
| Light + SMOTE | **0.7445** ⭐ | 0.7430 | 0.6903 | **0.7197** |

**Đọc bảng:**
- **Lymph**: Light alone **không cải thiện** (0.4235 → 0.4181). SMOTE mới fix được (→ 0.7445).
- **Hepatitis**: Light đủ tốt (DIE has 32 records, SMOTE không bật).
- **German**: Light đủ tốt (bad has 300 records, SMOTE không bật).

### 6.2 Đóng góp ròng của mỗi kỹ thuật

| Kỹ thuật | Lymph (extreme) | Moderate imbalance | Balanced |
|----------|:---------------:|:------------------:|:--------:|
| **H2 alone** | ❌ (overfitting với 4 records) | ✅ enables rules | ⚪ neutral |
| **H3 alone** | ❌ tương tự | ✅ enables rules | ⚪ neutral |
| **H2 + H3** | ⚠️ marginal (rules có nhưng không stable) | ✅ +0.026 (german) | ⚪ neutral |
| **SMOTE alone** | ✅✅ **breakthrough** (+0.32) | ⚠️ over-sample tạo nhiễu | ❌ unnecessary noise |
| **H2 + H3 + Adaptive SMOTE** | ✅✅ best | ✅ best | ⚪ zero regression |

### 6.3 Kết luận honest

> **H2 + H3 là điều kiện cần** (cho minority có rule).
> **SMOTE là điều kiện đủ** (cho minority có data đủ để rule stable).
> **Combo cả 2** là silver bullet, nhưng chỉ cho extreme imbalance.

---

## 7. Khi nào dùng cái gì

### 7.1 Decision tree theo `min_class_freq`

```
min_class_freq = min frequency of any class in dataset

┌─────────────────────────────────────────────────────────────┐
│ min_class_freq < 10  → Extreme imbalance                    │
│                      → BẬT H2 + H3 + SMOTE (Adaptive)       │
│                      → Ví dụ: lymph (fibrosis=4, normal=2)  │
├─────────────────────────────────────────────────────────────┤
│ 10 ≤ min_class_freq < 100 → Moderate imbalance              │
│                            → BẬT H2 + H3 (Light)            │
│                            → Ví dụ: hepatitis (DIE=32)      │
├─────────────────────────────────────────────────────────────┤
│ min_class_freq ≥ 100  → Balanced enough                     │
│                       → Có thể dùng Baseline hoặc Light     │
│                       → Ví dụ: breast-w, iris, mushroom     │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Final config khuyến nghị (default cho mọi dataset)

```java
CrossValidator.runWithMetrics(
    data, K=10,
    supPct, minConf=0.5, chi2=3.841, coverage=4,
    seed=42, maxPatternLength,
    CMARClassifier::new,
    /*H2*/ supPct,           // class-specific minSup
    /*H3 floor*/ 0.3,        // adaptive minConf sàn
    /*H3 lift*/  5.0,        // adaptive minConf khuếch đại
    /*SMOTE*/ adaptiveRatio  // 1.0 nếu min_freq<10, 0 nếu không
);
```

Đây chính là config của [BenchmarkSMOTEFull.java](../src/BenchmarkSMOTEFull.java).

---

## 8. Mapping code → công thức

### 8.1 H2 (Class-specific minSup)

| Công thức | Code |
|-----------|------|
| `minSup(c) = max(2, ⌊supPct × freq(c)⌋)` | `CrossValidator.java:118` |
| `classThreshold(cls) → classMinSupMap[cls]` | `FPGrowth.java:90` |
| Apply trong rule emission | `FPGrowth.java:231`: `if (classSup < classThreshold(cls)) continue;` |

### 8.2 H3 (Adaptive minConf)

| Công thức | Code |
|-----------|------|
| `minConf(c) = min(globalMinConf, max(floor, lift × freq(c)/N))` | `CrossValidator.java:135` |
| `classMinConfidence(cls) → classMinConfMap[cls]` | `FPGrowth.java:108` |
| Apply trong rule emission | `FPGrowth.java:234`: `if (confidence < classMinConfidence(cls)) continue;` |

### 8.3 SMOTE-N

| Step | Code |
|------|------|
| k-NN qua Hamming distance | `SMOTE.java:104` |
| Tạo synthetic via Mode voting | `SMOTE.java:165` |
| Integration trong CV fold loop | `CrossValidator.java:94` |
| Adaptive trigger | `BenchmarkSMOTEFull.java:165` |

---

## 9. Tham khảo

1. **Chawla, N. V., Bowyer, K. W., Hall, L. O., & Kegelmeyer, W. P.** (2002). *SMOTE: Synthetic Minority Over-sampling Technique*. JAIR 16, 321-357.
2. **Li, W., Han, J., & Pei, J.** (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules*. ICDM 2001, 369-376.
3. **WCBA** (2018). *Weighted Classification Based on Association Rules algorithm*.

## 10. Files liên quan trong project

| File | Vai trò |
|------|---------|
| [src/CrossValidator.java](../src/CrossValidator.java) | H2/H3/SMOTE integration |
| [src/FPGrowth.java](../src/FPGrowth.java) | classMinSupMap, classMinConfMap |
| [src/SMOTE.java](../src/SMOTE.java) | SMOTE-N implementation |
| [src/BenchmarkSMOTEFull.java](../src/BenchmarkSMOTEFull.java) | v11 final benchmark |
| [src/CMARClassifier.java](../src/CMARClassifier.java) | Core CMAR (clean, no extras) |
| [report/v11_smote_full_uci.md](v11_smote_full_uci.md) | Kết quả full 20 UCI |
| [report/v10_smote_threshold.md](v10_smote_threshold.md) | v10 SMOTE+Boost ablation |
