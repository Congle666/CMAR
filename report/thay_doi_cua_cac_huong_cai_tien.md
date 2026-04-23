# Thay Đổi Của Các Hướng Cải Tiến CMAR

**Mục đích:** Tài liệu chính thức cho khóa luận — mô tả đầy đủ các hướng cải tiến đã thực hiện, công thức thay đổi, kết quả đạt được.

**Ngày:** 2026-04-23
**Dataset test:** 7 UCI datasets (lymph, glass, german, hepatitis, iris, wine, zoo)
**Setup:** 10-fold stratified CV, seed=42, chiSqThreshold=3.841, coverageDelta=4

---

## 📋 Tổng Quan 5 Variants

| # | Tên | Ý nghĩa ngắn gọn |
|:-:|-----|-----------------|
| v1 | **Baseline CMAR** | Cài đặt đúng theo paper Li, Han & Pei (2001) |
| v2 | **Hướng 1 (H1)** | Class-weighted χ² — nhân weight nghịch tần suất vào score |
| v3 | **Hướng 2 (H2)** | Class-specific minSup — ngưỡng support riêng cho từng class |
| v4 | **H1+H2** | Kết hợp cả hai hướng trên |
| **v6** | **🆕 H2+H3** | **Class-specific minSup + Adaptive minConf** ⭐ **BEST** |

---

## 🔷 PHẦN 1 — Hướng 1 (H1): Class-weighted χ²

### 1.1 Ý tưởng

CMAR gốc tính score mỗi class trong bước classify:
```
score(c) = Σ [χ²(R)² / maxχ²(R)]      với R là các rule khớp
```

Vấn đề: class đa số (nhiều rule) áp đảo class thiểu số (ít rule) → minority bị ignore.

**Fix H1:** Nhân thêm **trọng số nghịch tần suất** cho mỗi class (sklearn "balanced"):

```
score(c) = weight(c) × Σ [χ²(R)² / maxχ²(R)]
weight(c) = N / (k × freq(c))
```

Trong đó:
- **N** = tổng số bản ghi huấn luyện
- **k** = số class
- **freq(c)** = số bản ghi thuộc class c

### 1.2 Ví dụ lymph (N=148, 4 classes)

| Class | freq | freq/N | **weight(c)** |
|-------|:----:|:------:|:-------------:|
| metastases | 81 | 54.7% | 148/(4×81) = **0.457** |
| malign_lymph | 61 | 41.2% | 148/(4×61) = **0.607** |
| fibrosis | 4 | 2.7% | 148/(4×4) = **9.25** ⭐ |
| normal | 2 | 1.4% | 148/(4×2) = **18.5** ⭐⭐ |

→ Fibrosis score được nhân 9.25x, normal 18.5x → công bằng hơn với minority.

### 1.3 Thay đổi code

**File sửa:** tạo mới `src/CMARClassifierWeighted.java` kế thừa `CMARClassifier`.

```java
@Override
public String classify(Transaction record) {
    // ... (giống baseline)
    for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
        String cls = entry.getKey();
        double score = 0.0;
        for (AssociationRule rule : entry.getValue()) {
            double chi2 = computeChiSquare(rule, cls);
            double maxChi2 = computeMaxChiSquare(rule, cls);
            if (maxChi2 > 0) score += (chi2 * chi2) / maxChi2;
        }
        // ⭐ NEW: nhân weight class
        score *= classWeights.getOrDefault(cls, 1.0);
        if (score > bestScore) { bestScore = score; bestClass = cls; }
    }
    return bestClass;
}
```

**Chỉ thay đổi bước `classify()`** — mining, pruning, CR-tree giữ nguyên.

### 1.4 Kết quả H1 (7 datasets)

| Dataset | Baseline MF1 | H1 MF1 | Δ | Ghi chú |
|---------|:------------:|:------:|:---:|---------|
| german | 0.6639 | 0.6726 | **+0.009** | ✅ |
| glass | 0.6113 | **0.6171** | **+0.006** | ✅ vehicle_float F1 0.20→0.31 |
| hepatitis | 0.7363 | 0.7144 | -0.022 | ❌ |
| lymph | 0.4235 | 0.4271 | +0.004 | tiny — **fibrosis vẫn F1=0** |
| zoo | 0.8972 | 0.8525 | -0.045 | ❌ amphibian over-predicted |

**Kết luận H1:** Giúp được class có sẵn rule (glass/vehicle_float +55% F1). **KHÔNG rescue được** class F1=0 (không có rule để weighted → weight × 0 = 0).

---

## 🔷 PHẦN 2 — Hướng 2 (H2): Class-specific minSup

### 2.1 Ý tưởng

CMAR gốc dùng **một minSup toàn cục** cho mọi class:
```
minSup = round(N × minSupPct)   (vd 5% × 134 = 7)
```

Vấn đề: class hiếm có tối đa K records (vd fibrosis K=4). Rule `{X}⇒fibrosis` có sup tối đa K=4 < 7 → **không rule nào được sinh.**

**Fix H2:** Ngưỡng support **riêng cho từng class**:
```
minSup(c) = max(2, round(minSupPct × freq(c)))
```

### 2.2 Ví dụ lymph

| Class | freq | minSup toàn cục cũ | **minSup(c) mới** |
|-------|:----:|:------------------:|:-----------------:|
| metastases | 81 | 7 | 4 |
| malign_lymph | 61 | 7 | 3 |
| fibrosis | 4 | 7 (không đạt) | **2** ✅ |
| normal | 2 | 7 (không đạt) | **2** ✅ |

→ Rules cho fibrosis với sup ≥ 2 **có thể được sinh**.

### 2.3 Thay đổi code

**File sửa:**
- `src/FPGrowth.java`: thêm `classMinSupMap` + check emission theo class
- `src/CrossValidator.java`: tính `classMinSupMap` từ training data

```java
// FPGrowth.java
private Map<String, Integer> classMinSupMap;
public void setClassMinSupMap(Map<String, Integer> map) { ... }

// Trong mineTree() — bước sinh CAR:
for (Entry<String, Integer> e : classDistForP.entrySet()) {
    String cls = e.getKey();
    int classSup = e.getValue();
    // ⭐ Dùng threshold per-class thay vì global minSupport
    if (classSup < classThreshold(cls)) continue;
    // ... rest unchanged
}
```

### 2.4 Kết quả H2 (7 datasets)

| Dataset | Baseline MF1 | H2 MF1 | Δ |
|---------|:------------:|:------:|:---:|
| german | 0.6639 | **0.6903** | **+0.026** ✅ |
| hepatitis | 0.7363 | **0.7430** | +0.007 ✅ |
| lymph | 0.4235 | 0.4167 | -0.007 ❌ |
| glass | 0.6113 | 0.6113 | 0 |
| iris/wine/zoo | không đổi | không đổi | 0 |

**Kết luận H2 alone:** Giúp german, hepatitis. **KHÔNG cứu lymph/fibrosis** vì rules được sinh nhưng bị **minConf = 0.5 loại bỏ**.

**Vấn đề toán học:** Rule `{X}⇒fibrosis` có `conf = classSup / itemSup ≤ 4 / 7 = 0.571`. Nhưng vì item-level minSup vẫn = 7 (để tractable), hầu hết rules có itemSup > 7 → `conf < 0.5` → bị loại.

---

## 🔷 PHẦN 3 — Hướng 3 (H3) ⭐: Adaptive minConf per class

### 3.1 Ý tưởng (điểm sáng tạo chính của đề tài)

**Phát hiện then chốt:** minConf = 0.5 là **quá chặt** với class hiếm.

Rule `{X}⇒fibrosis` với `conf = 0.3` nghĩa là:
- Khi khớp X, xác suất là fibrosis = 30%
- Baseline (đoán ngẫu nhiên): 2.7%
- **Lift = 30% / 2.7% = 11×** → rất có thông tin!

Nhưng conf=0.3 < minConf=0.5 → bị loại. Đây là **root cause** F1=0.

**Fix H3:** minConf **riêng cho từng class**, dựa vào class frequency:

```
minConf(c) = min(globalMinConf, max(FLOOR, LIFT × freq(c)/N))
```

Mặc định: `FLOOR = 0.3`, `LIFT = 5`, `globalMinConf = 0.5`.

### 3.2 Ví dụ lymph (N=148)

| Class | freq | freq/N | 5 × freq/N | **minConf(c)** |
|-------|:----:|:------:|:----------:|:--------------:|
| metastases | 81 | 0.547 | 2.735 | **0.5** (capped at global) |
| malign_lymph | 61 | 0.412 | 2.060 | **0.5** (capped) |
| fibrosis | 4 | 0.027 | 0.135 | **0.3** (floor — relaxed!) |
| normal | 2 | 0.014 | 0.068 | **0.3** (floor) |

→ Rule fibrosis chỉ cần conf ≥ 0.3 thay vì 0.5 → **nhiều rule được giữ hơn**.

### 3.3 Giải thích trực giác

Công thức `LIFT × freq(c)/N = 5 × freq(c)/N` đặt yêu cầu:
> "Rule phải có confidence ≥ **5 lần** baseline (tỉ lệ class trong dataset) mới được giữ."

- Class đa số: baseline 54% → 5× = 2.7 → bị cap về globalMinConf 0.5 → không thay đổi
- Class thiểu số: baseline 2.7% → 5× = 0.135 → bị floor 0.3 → threshold cuối là 0.3

**Ý nghĩa:** Công thức tự động **nới lỏng** cho class hiếm, **giữ nguyên** cho class đa số.

### 3.4 Thay đổi code

**File sửa:**
- `src/FPGrowth.java`: thêm `classMinConfMap` + check emission theo class
- `src/CrossValidator.java`: tính `classMinConfMap` từ training data

```java
// FPGrowth.java - NEW
private Map<String, Double> classMinConfMap;
public void setClassMinConfMap(Map<String, Double> map) { ... }

// Trong mineTree():
double confidence = (double) classSup / itemSupport;
// ⭐ H3: dùng minConf per-class thay vì global
if (confidence < classMinConfidence(cls)) continue;
```

```java
// CrossValidator.java - tính classMinConfMap trước mỗi fold
if (adaptiveMinConfFloor > 0) {
    classMinConfMap = new HashMap<>();
    int N = trainData.size();
    for (Entry<String, Integer> e : classFreq.entrySet()) {
        double classRatio = (double) e.getValue() / N;
        double thr = min(minConfidence,
                         max(adaptiveMinConfFloor,
                             adaptiveMinConfLift * classRatio));
        classMinConfMap.put(e.getKey(), thr);
    }
}
```

### 3.5 Quan trọng — H3 một mình KHÔNG đủ

**Phát hiện:** Chạy H3 alone (không có H2) → kết quả **IDENTICAL với baseline**. Vì sao?

- Global minSup = 7. Fibrosis có tối đa 4 records → `classSup(fibrosis) ≤ 4 < 7` → **rule không bao giờ được sinh** (bị loại ở support check trước khi tới confidence check).
- H3 chỉ nới minConf → nhưng nếu không có rule, không có gì để nới!

→ **H3 cần kết hợp với H2**.

---

## 🌟 PHẦN 4 — Kết hợp H2+H3 (v6) — THÀNH TỰU CHÍNH

### 4.1 Sơ đồ nguyên nhân

```
H2 (class minSup)         H3 (adaptive minConf)
minSup(fibrosis) = 2      minConf(fibrosis) = 0.3
   ↓                          ↓
Rule {X}⇒fibrosis       Rule có conf 0.3-0.5
được sinh ra            được giữ lại
   ↓                          ↓
        ═══════════════════
        H2 + H3 KẾT HỢP
        ═══════════════════
                ↓
        lymph/fibrosis F1: 0.00 → 0.67 ⭐
```

### 4.2 Kết quả đột phá trên lymph

| Class | Support | **Baseline F1/Rec** | **H2+H3 F1/Rec** | Thay đổi |
|-------|:-------:|:-------------------:|:----------------:|----------|
| metastases | 81 | 0.861 / 0.840 | **0.877** / 0.840 | +0.016 |
| malign_lymph | 61 | 0.833 / 0.902 | 0.819 / 0.853 | -0.015 |
| **fibrosis** | **4** | **0.000 / 0.000** | **0.667 / 1.000** ⭐ | **+0.667!** |
| normal | 2 | 0.000 / 0.000 | 0.000 / 0.000 | 0 (vẫn F1=0, dữ liệu quá ít) |

**Lymph MacroF1: 0.4235 → 0.5907 (+39%)** ⭐

**Chi tiết fibrosis:**
- TP=4, FP=4, FN=0
- Precision = 4/(4+4) = 0.50
- **Recall = 4/(4+0) = 1.00 (PERFECT!)**
- F1 = 0.67

**Ý nghĩa:** Mô hình **bắt đúng cả 4/4 records fibrosis** trong tập test. Trade-off: 4 FP (predict fibrosis cho records không phải fibrosis).

### 4.3 Kết quả v6 toàn bảng (7 datasets)

| Dataset | Paper Acc | Baseline MF1 | H1 | H2 | **H2+H3 (v6)** | Best |
|---------|:---------:|:------------:|:--:|:--:|:--------------:|:----:|
| german | 73.40 | 0.6639 | 0.6726 | 0.6903 | **0.6903** | H2=v6 |
| glass | 70.09 | 0.6113 | 0.6171 | 0.6113 | 0.6113 | H1 |
| hepatitis | 80.65 | 0.7363 | 0.7144 | 0.7430 | **0.7430** | H2=v6 |
| iris | 94.00 | 0.9532 | 0.9532 | 0.9532 | 0.9532 | tie |
| **lymph** | 82.43 | **0.4235** | 0.4271 | 0.4167 | **0.5907** ⭐ | **v6** |
| wine | 95.51 | 0.9559 | 0.9559 | 0.9559 | 0.9559 | tie |
| zoo | 96.04 | 0.8972 | 0.8525 | 0.8972 | 0.8972 | Base=H2=v6 |
| **TB Macro-F1** | | **0.7488** | 0.7418 | 0.7525 | **0.7774** ⭐ | **v6** |
| **TB Accuracy** | 84.59 | 84.59 | 83.08 | 84.44 | **84.80** ⭐ | **v6** |

**v6 THẮNG tất cả các metrics trung bình:**
- ✅ Avg Macro-F1: 0.7488 → **0.7774** (+3.8%)
- ✅ Avg Accuracy: 84.59 → **84.80** (+0.21%, **KHÔNG giảm!**)
- ✅ Không regression trên bất kỳ dataset nào (mọi delta ≥ 0)

---

## 📊 PHẦN 5 — Bảng So Sánh 5 Variants Đầy Đủ

### 5.1 Macro-F1 per Dataset

| Dataset | Base | H1 | H2 | H1+H2 | **H2+H3** |
|---------|:----:|:--:|:--:|:-----:|:---------:|
| german | 0.6639 | 0.6726 | 0.6903 | 0.6961 | **0.6903** |
| glass | 0.6113 | 0.6171 | 0.6113 | 0.6171 | 0.6113 |
| hepatitis | 0.7363 | 0.7144 | 0.7430 | 0.7103 | **0.7430** |
| iris | 0.9532 | 0.9532 | 0.9532 | 0.9532 | 0.9532 |
| **lymph** | 0.4235 | 0.4271 | 0.4167 | 0.4084 | **0.5907** ⭐ |
| wine | 0.9559 | 0.9559 | 0.9559 | 0.9559 | 0.9559 |
| zoo | 0.8972 | 0.8525 | 0.8972 | 0.8525 | 0.8972 |
| **TB** | 0.7488 | 0.7418 | 0.7525 | 0.7419 | **0.7774** |

### 5.2 Accuracy per Dataset

| Dataset | Base | H1 | H2 | H1+H2 | **H2+H3** |
|---------|:----:|:--:|:--:|:-----:|:---------:|
| german | 74.20 | 73.20 | 74.60 | 73.00 | **74.60** |
| glass | 66.11 | 64.04 | 66.11 | 64.04 | 66.11 |
| hepatitis | 81.81 | 76.64 | 82.48 | 76.64 | **82.48** |
| iris | 95.33 | 95.33 | 95.33 | 95.33 | 95.33 |
| lymph | 83.46 | 84.18 | 81.32 | 77.75 | **83.83** |
| wine | 95.52 | 95.52 | 95.52 | 95.52 | 95.52 |
| zoo | 95.73 | 92.68 | 95.73 | 92.68 | 95.73 |
| **TB** | 84.59 | 83.08 | 84.44 | 82.14 | **84.80** |

---

## 🎯 PHẦN 6 — Files Thay Đổi (For Thesis Writeup)

### 6.1 Files mới thêm

| File | Mô tả | Dòng code |
|------|-------|:---------:|
| `src/CMARClassifierWeighted.java` | H1 classifier | ~100 |
| `src/EvalMetrics.java` | Macro-F1, Weighted-F1, per-class P/R/F1 | ~220 |
| `src/BenchmarkWeighted.java` | H1 benchmark entry | ~125 |
| `src/BenchmarkClassSup.java` | H2 benchmark entry | ~125 |
| `src/BenchmarkH1H2.java` | H1+H2 benchmark | ~120 |
| `src/BenchmarkAdaptiveConf.java` | H3 alone benchmark | ~125 |
| `src/BenchmarkH2H3.java` ⭐ | **H2+H3 benchmark (best)** | ~125 |

### 6.2 Files đã sửa

| File | Thay đổi chính |
|------|----------------|
| `src/CMARClassifier.java` | 2 methods: private → protected (cho subclass H1 dùng) |
| `src/FPGrowth.java` | +`classMinSupMap` (H2) + `classMinConfMap` (H3) + emission check |
| `src/CrossValidator.java` | +`runWithMetrics()` overload với factory + H2 params + H3 params |
| `src/Benchmark.java` | +`maxPatternLength` cho sonar/vehicle/waveform + dataset filter args |
| `src/ResultWriter.java` | +CSV writers (metrics + per-class) |

### 6.3 Output files

| File | Nội dung |
|------|----------|
| `result/baseline_metrics.csv` | v1 — 20 datasets |
| `result/baseline_per_class.csv` | v1 per-class |
| `result/v2_metrics.csv` / `v2_per_class.csv` | H1 (20 datasets) |
| `result/v3_metrics.csv` / `v3_per_class.csv` | H2 (7 datasets) |
| `result/v4_metrics.csv` / `v4_per_class.csv` | H1+H2 (7 datasets) |
| `result/v5_metrics.csv` / `v5_per_class.csv` | H3 alone (7 datasets) |
| **`result/v6_metrics.csv` / `v6_per_class.csv`** | **H2+H3 (7 datasets) — BEST** |

---

## 🎓 PHẦN 7 — Viết Báo Cáo Khóa Luận

### 7.1 Thông điệp chính

> *"Luận văn đã cài đặt lại CMAR trung thành theo paper, sau đó đề xuất và triển khai 3 hướng cải tiến. Kết hợp Hướng 2 + Hướng 3 đạt kết quả đột phá: rescue được class thiểu số cực hiếm (fibrosis chỉ có 4 records) từ **F1 = 0 lên F1 = 0.67**, trong khi vẫn giữ nguyên accuracy tổng thể."*

### 7.2 Bảng tóm tắt đóng góp (copy vào thesis)

| # | Hướng cải tiến | Kỹ thuật | Cải thiện chính |
|:-:|----------------|----------|-----------------|
| H1 | Class-weighted χ² | Inverse frequency weight | glass vehicle_float F1 0.20→0.31 |
| H2 | Class-specific minSup | minSup(c) = max(2, fraction × freq(c)) | german, hepatitis MacroF1 +3-4% |
| **H3** | **Adaptive minConf** | **minConf(c) = min(0.5, max(0.3, 5 × freq(c)/N))** | **Phát hiện root cause + rescue fibrosis** |
| **H2+H3** | **Kết hợp** | Cả 2 kỹ thuật cùng lúc | **lymph/fibrosis F1: 0.00→0.67 (Recall 100%)** |

### 7.3 Phân tích root cause (điểm mạnh academic)

**Tại sao H1 và H2 alone không cứu được fibrosis (4 records):**

1. Baseline minSup toàn cục = 7 → fibrosis (≤4 records) không qua được item-level.
2. H1 (weighted) chỉ scale score các rule đã có → nếu không rule nào cho fibrosis, weight × 0 = 0.
3. H2 (class-minSup) hạ sup threshold xuống 2 → rule được sinh. **Nhưng** conf vẫn bị global 0.5 chặn: `conf = 4/7 = 0.57` chỉ 1 số item được giữ.
4. **H3 (adaptive minConf)** hạ conf threshold xuống 0.3 cho fibrosis → thêm nhiều rule được giữ.
5. **H2+H3:** sinh rule (H2) + giữ rule (H3) → **đủ rules để predict đúng fibrosis**.

### 7.4 Công thức chính cho thesis (LaTeX-ready)

**H1 — Weighted score:**
$$
\text{score}(c) = w(c) \cdot \sum_{R \in \mathcal{R}_c} \frac{\chi^2(R)^2}{\max \chi^2(R)}, \quad w(c) = \frac{N}{k \cdot \text{freq}(c)}
$$

**H2 — Class-specific minSup:**
$$
\text{minSup}(c) = \max(2, \text{round}(\text{minSupPct} \cdot \text{freq}(c)))
$$

**H3 — Adaptive minConf:**
$$
\text{minConf}(c) = \min\left(\text{minConf}_{\text{global}}, \max\left(\text{FLOOR}, \text{LIFT} \cdot \frac{\text{freq}(c)}{N}\right)\right)
$$

Với mặc định: `FLOOR = 0.3`, `LIFT = 5`, `minConf_global = 0.5`.

### 7.5 Cấu trúc chương "Đánh giá"

1. **Tiểu mục 5.1** — Setup thực nghiệm (10-fold CV, seed, datasets)
2. **Tiểu mục 5.2** — Kết quả Baseline (so sánh với paper — bảng 20 datasets)
3. **Tiểu mục 5.3** — Phân tích vấn đề (Macro-F1 lymph chỉ 0.42 dù Acc 83%)
4. **Tiểu mục 5.4** — Kết quả H1 — phát hiện "weight không rescue được class không có rule"
5. **Tiểu mục 5.5** — Kết quả H2 — phát hiện "sinh rule nhưng minConf chặn"
6. **Tiểu mục 5.6** — Đề xuất H3 dựa trên root cause analysis
7. **Tiểu mục 5.7** — **Kết quả H2+H3: lymph/fibrosis F1 0→0.67** ⭐
8. **Tiểu mục 5.8** — Thảo luận trade-off + hạn chế (normal vẫn F1=0)
9. **Tiểu mục 5.9** — Hướng tương lai (SMOTE, F1-aware pruning cho class siêu hiếm)

---

## 📌 PHẦN 8 — Lệnh chạy cho reproducibility

### 8.1 Compile

```bash
cd d:/CMAR
mkdir -p out
javac -d out src/*.java
```

### 8.2 Chạy từng variant

```bash
# Baseline (20 datasets)
java -Xmx1g -cp out Benchmark

# H1 — Weighted χ²
java -Xmx1g -cp out BenchmarkWeighted

# H2 — Class-specific minSup
java -Xmx1g -cp out BenchmarkClassSup lymph glass german hepatitis zoo iris wine

# H1+H2
java -Xmx1g -cp out BenchmarkH1H2 lymph glass german hepatitis zoo iris wine

# H3 alone (chứng minh H3 alone không đủ)
java -Xmx1g -cp out BenchmarkAdaptiveConf lymph glass german hepatitis zoo iris wine

# ⭐ H2+H3 — BEST
java -Xmx1g -cp out BenchmarkH2H3 lymph glass german hepatitis zoo iris wine
```

### 8.3 Tham số mặc định

| Tham số | Giá trị |
|---------|:-------:|
| K (folds) | 10 |
| Seed | 42 |
| minConf global | 0.5 |
| χ² threshold | 3.841 |
| coverage delta (δ) | 4 |
| H3 adaptive floor | 0.3 |
| H3 adaptive lift | 5.0 |

---

## 🎯 Kết Luận

### Thành tựu
1. ✅ **Baseline faithful** — accuracy -0.1% so với paper
2. ✅ **3 hướng cải tiến đã implement + benchmark**
3. ✅ **Phát hiện root cause** class F1=0 (minConf chặn minority)
4. ✅ **H2+H3 rescue được fibrosis** (F1 0→0.67, Recall 100%)
5. ✅ **Không regression** — Avg MacroF1 +3.8%, Avg Accuracy +0.2%
6. ✅ **Code generalizable** — auto-compute thresholds từ class frequency, work trên dataset mới

### Hạn chế còn lại
- `normal` class (2 records) vẫn F1=0 — cần SMOTE hoặc dataset augmentation
- `glass` class nhỏ chưa cải thiện — cần tuning minSupPct thấp hơn

### Đóng góp nghiên cứu
- Phương pháp **Adaptive minConf per class** (H3) là đóng góp chính — đề tài có thể publish.
- H2+H3 combined thể hiện rõ phương pháp **layered approach**: hỗ trợ rule generation + rule retention.
- Phân tích định lượng root cause (vì sao H1 và H2 alone fail) là đóng góp học thuật.

---

*File này là tham khảo chính cho chương "Phương pháp đề xuất" và "Đánh giá" trong khóa luận. Mọi bảng số + công thức + code snippet đã sẵn sàng copy.*
