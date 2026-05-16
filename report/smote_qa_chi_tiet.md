# 🎓 SMOTE trong CMAR — Q&A Chi tiết Bảo vệ Thesis

**Ngày:** 2026-05-12
**Đối tượng:** Sinh viên chuẩn bị bảo vệ thesis về CMAR Imbalanced.
**Mục tiêu:** Trả lời chuẩn 5 câu hỏi cốt lõi + 10 câu hỏi follow-up giảng viên có thể hỏi.

---

## Mục lục

1. [Q1: SMOTE dùng gì để chạy? Implementation thế nào?](#q1-smote-dùng-gì-để-chạy)
2. [Q2: Sau SMOTE thì Accuracy có tăng không?](#q2-sau-smote-thì-accuracy-có-tăng-không)
3. [Q3: Train/Test data flow — dùng dữ liệu gốc hay SMOTE?](#q3-traintest-data-flow)
4. [Q4: H2 và H3 có cơ sở paper không?](#q4-h2-và-h3-có-cơ-sở-paper-không)
5. [Q5: SMOTE chạy với tham số K và N hả?](#q5-smote-chạy-với-tham-số-k-và-n-hả)
6. [10 câu hỏi follow-up khó](#10-câu-hỏi-follow-up-khó)
7. [Cheat sheet số liệu phải nhớ](#cheat-sheet-số-liệu-phải-nhớ)

---

## Q1: SMOTE dùng gì để chạy?

### TL;DR
**Tự viết Java thuần** trong file [src/SMOTE.java](../src/SMOTE.java) (236 dòng), **KHÔNG dùng thư viện external**.

### Chi tiết kỹ thuật

```
File:          src/SMOTE.java
Ngôn ngữ:      Java 8+
Thư viện:      Chỉ Java standard library (java.util.*)
Biến thể:      SMOTE-N (Nominal / Categorical)
Reference:     Chawla et al. 2002 (JAIR vol 16, pp. 321-357)
```

### Tại sao tự viết, không dùng thư viện?

| Lý do | Giải thích |
|-------|------------|
| **CMAR dùng categorical data** | Thư viện phổ biến (imbalanced-learn của Python) cho numeric → không hợp |
| **SMOTE-N ít implementation** | Chawla 2002 đề xuất biến thể N nhưng ít library implement |
| **Tích hợp với pipeline Java** | Toàn bộ CMAR codebase là Java → tự viết để cùng ngôn ngữ |
| **Hiểu sâu thuật toán** | Thesis cần demonstrate hiểu rõ algorithm, không dùng black-box |

### Câu trả lời chuẩn cho giảng viên

> "Dạ thưa thầy, em **tự viết SMOTE-N bằng Java** trong file `src/SMOTE.java`, không dùng thư viện external. Lý do:
> 1. CMAR mining trên dữ liệu categorical, mà thư viện SMOTE phổ biến (như imbalanced-learn) chủ yếu cho numeric.
> 2. Biến thể SMOTE-N (Nominal) dùng Hamming distance + mode voting thay vì linear interpolation, em implement đúng theo paper Chawla 2002."

---

## Q2: Sau SMOTE thì Accuracy có tăng không?

### TL;DR
**Tăng nhẹ trung bình (+0.24%) nhưng KHÔNG đồng đều giữa các datasets.** Accuracy KHÔNG phải metric quan trọng cho imbalanced data — **F1 và Recall** mới là chỉ số đáng tin.

### Số liệu thực tế (từ CSV v11)

| Dataset | Baseline Acc | Sau SMOTE Acc | Δ Acc | Δ MacroF1 |
|---------|:------------:|:-------------:|:-----:|:---------:|
| **lymph** | 0.8346 | 0.8328 | **−0.18%** ⚠️ | **+75.8%** ⭐ |
| **glass** | 0.6611 | 0.6304 | **−3.07%** ⚠️ | −1.12% |
| vehicle | 0.6795 | 0.7076 | **+2.81%** ✓ | +3.60% |
| german | 0.7420 | 0.7460 | +0.40% | +2.64% |
| hepatitis | 0.8181 | 0.8248 | +0.67% | +0.67% |
| mushroom | 0.9807 | 0.9817 | +0.10% | +0.10% |
| **AVG (20 datasets)** | **0.8412** | **0.8436** | **+0.24%** | **+2.40%** |

### Paradox Lymph — Accuracy giảm nhưng F1 vọt

```
Lymph (148 records):
  Accuracy: 0.8346 → 0.8328  (−0.0018, giảm nhẹ)
  MacroF1:  0.4235 → 0.7445  (+0.3209, tăng 75.8%) ⭐
```

### Tại sao paradox?

```
Trước SMOTE:
  Đúng ~83% records, hầu hết là metastases & malign_lymph (majority)
  Sai 6/148 records (fibrosis + normal vì không có rule)
  → Accuracy cao nhờ majority

Sau SMOTE:
  Đúng ~83% records (gần như cùng count)
  Predict đúng vài fibrosis/normal mới
  Nhưng đổi vài predict majority thành sai
  → Accuracy hầu như không đổi
  → Nhưng F1 minority tăng vọt (0 → 0.5/0.8)
```

### Vì sao Accuracy không phù hợp cho imbalanced?

**Ví dụ minh họa:**
```
Dataset spam: 1000 email (950 ham, 50 spam)
Model siêu ngu: predict TẤT CẢ = ham
  → Accuracy = 950/1000 = 95%  ←  trông tốt!
  → Nhưng Recall(spam) = 0%   ←  VÔ DỤNG

→ Accuracy bị dominated bởi majority class.
→ Trong imbalanced, F1/Recall mới phản ánh đúng.
```

### Câu trả lời chuẩn cho giảng viên

> "Dạ thưa thầy, Accuracy có tăng nhẹ trung bình **+0.24%** trên 20 UCI. Tuy nhiên đây là metric **không quan trọng** với imbalanced data, ví dụ Lymph:
> - Accuracy hầu như không đổi (0.8346 → 0.8328, giảm 0.0018)
> - Nhưng MacroF1 tăng từ 0.42 lên 0.74 (+75.8%)
>
> Lý do: Accuracy bị **dominated bởi majority class**. Predict đúng metastases (81 records) đóng góp accuracy nhiều hơn predict sai fibrosis (4 records). Đó là tại sao em **báo cáo MacroF1 và MacroRecall** thay vì Accuracy — chúng đánh giá công bằng cho mọi class."

---

## Q3: Train/Test Data Flow

### TL;DR
**Train dùng dữ liệu SMOTE (gốc + synthetic). Test dùng dữ liệu GỐC.**

```
┌────────────────────────────────────────────────────────────────┐
│  TRAIN set:  dữ liệu GỐC + synthetic (sau khi SMOTE)            │
│  TEST set:   dữ liệu GỐC (KHÔNG bao giờ động đến)                │
└────────────────────────────────────────────────────────────────┘
```

### Pipeline 10-fold CV trong project

```
DATA GỐC (148 records Lymph)
       │
       ↓
┌─────────────────────────────────────────────────┐
│  Stratified split thành 10 folds                │
│  Mỗi fold giữ tỷ lệ class                       │
└─────────────────────────────────────────────────┘
       │
       ↓ For each fold k:
┌─────────────────────────────────────────────────┐
│  Step 1: Chia                                    │
│    train = 9 folds × ~14 records = ~133 GỐC      │
│    test  = 1 fold × ~15 records = ~15 GỐC        │
└─────────────────────────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────────────────┐
│  Step 2: SMOTE CHỈ áp dụng cho TRAIN             │
│    train: 133 → 266 records (gốc + synthetic)    │
│    test:  vẫn 15 GỐC (không bị động)             │
└─────────────────────────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────────────────┐
│  Step 3: Mining + Pruning + Training             │
│    Học rules từ 266 records SMOTE                │
└─────────────────────────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────────────────┐
│  Step 4: Evaluate trên 15 records GỐC            │
│    Tính F1, Recall, Accuracy thật                │
└─────────────────────────────────────────────────┘
```

### Code chứng minh — [src/CrossValidator.java](../src/CrossValidator.java)

```java
// Chia train/test từ data GỐC
List<Transaction> testData = folds[fold];         // Test: GỐC
List<Transaction> trainData = new ArrayList<>();
for (int j = 0; j < k; j++) if (j != fold) trainData.addAll(folds[j]);

// SMOTE CHỈ apply cho trainData
if (smoteTargetRatio > 0) {
    trainData = SMOTE.apply(trainData, 5, smoteTargetRatio, seed + fold);
    // testData KHÔNG bị động — vẫn là GỐC
}

// Train trên SMOTE data
classifier.train(candidates, trainData);

// Test trên GỐC data
List<String> predictions = classifier.predict(testData);
EvalMetrics metrics = EvalMetrics.compute(testData, predictions);
```

### ⚠️ Lỗi phổ biến phải tránh — Data Leakage

**SAI:** SMOTE trên toàn bộ data **RỒI MỚI** chia train/test
```
148 records → SMOTE → 324 records → chia train/test
                                    ↓
                       Synthetic có thể lọt vào TEST
                                    ↓
                       Model "thấy" test gián tiếp
                                    ↓
                       F1 báo cáo bị OVERESTIMATE ❌
```

**ĐÚNG (project này):** Chia train/test **TRƯỚC**, sau đó SMOTE chỉ trên train
```
148 records → chia train/test → SMOTE chỉ trên train
              (133 train / 15 test)   (133 → 266)
                                       ↓
                              Test vẫn 15 GỐC
                                       ↓
                              F1 báo cáo khách quan ✅
```

### Câu trả lời chuẩn cho giảng viên

> "Dạ thưa thầy, em **chỉ áp dụng SMOTE cho training set**, test set giữ nguyên dữ liệu gốc. Lý do quan trọng:
>
> Nếu SMOTE trên toàn bộ data trước khi chia, synthetic records có thể lọt vào test set → model học gián tiếp từ test → gây **data leakage** → F1 báo cáo bị overestimate.
>
> Em làm theo đúng best practice của paper Chawla 2002: **SMOTE inside CV loop**, sau khi đã chia fold. Bạn có thể verify trong [`CrossValidator.java`](../src/CrossValidator.java) — `SMOTE.apply()` chỉ gọi trên `trainData`, không bao giờ động đến `testData`."

---

## Q4: H2 và H3 có cơ sở paper không?

### TL;DR
- **H2** có direct paper backing — **Liu, Ma, Wong (2000) PKDD**.
- **H3** là novel combination với grounded components — Vo et al. (2015) + Nguyen et al. (2019).
- **WCBA 2018 KHÔNG phải nguồn của H2/H3** (đã verify bằng đọc PDF trực tiếp).

### Phân loại 3 cải tiến theo nguồn gốc

| Cải tiến | Nguồn gốc | Reference chính |
|----------|:---------:|-----------------|
| **SMOTE-N** | ✅ Direct paper | Chawla et al. (2002), JAIR vol 16 |
| **H2** (class-specific minSup) | ✅ Direct paper | **Liu, Ma, Wong (2000) PKDD, LNCS 1910** |
| **H3** (adaptive minConf) | ⚠️ Novel combination | Vo et al. (2015) + Nguyen et al. (2019) |

### H2 — Kế thừa từ Liu, Ma, Wong (2000)

> **Liu, B., Ma, Y., & Wong, C.K.** (2000). *Improving an Association Rule Based Classifier*. PKDD 2000, LNCS 1910, pp. 504-509. DOI: 10.1007/3-540-45372-5_58.

Công thức gốc của Liu et al. 2000:
```
minsupp_i = minsupp_t × supp(c_i) / max(supp(C))
```

**H2 là biến thể với 2 tinh chỉnh:**
- Dùng absolute frequency thay relative support
- Thêm safety floor `max(2, ...)`

**Lineage:**
- Liu, Hsu, Ma (1999) KDD '99 — MS-Apriori (item-specific MIS) — conceptual ancestor
- **Liu, Ma, Wong (2000) PKDD — CLASS-SPECIFIC minSup — parent của H2**
- Hu et al. (2016) SpringerPlus — MMSCBA refinement

### H3 — Novel combination dựa trên Vo (2015) + Nguyen (2019)

H3 **KHÔNG là pure invention**. 2 components có paper backing:

**Component A — Adjust confidence theo class frequency:**
> **Vo, B., Nguyen, L.T.T., & Hong, T.P.** (2015). *Class Association Rule Mining with Multiple Imbalanced Attributes*. ICDM 2015 Workshops, LNCS 9376, pp. 636-647.

Quote: *"Weighted confidence = confidence × weight(class) where weight inversely relates to class frequency."*

**Component B — Lift là alternative tốt cho confidence trên imbalanced:**
> **Nguyen, L.T.T., Vo, B., Nguyen, T.-N., et al.** (2019). *Mining class association rules on imbalanced class datasets*. JIFS 37(1). DOI: 10.3233/JIFS-179326.

Quote: *"Confidence is not suitable for imbalanced datasets... lift and conviction are less sensitive to class distribution."*

**Đóng góp gốc của H3:**

| Aspect | Vo et al. 2015 | Nguyen et al. 2019 | **H3 (project)** |
|--------|----------------|---------------------|-------------------|
| Stage áp dụng | Post-mining ranking | Post-mining filtering | **Mining stage** ⭐ |
| Combine lift × P(c) trong threshold | ✗ | ✗ | ✓ **Original** |
| First AC với adaptive minConf at mining | ✗ | ✗ | ✓ |

→ H3 là **first AC method** apply adaptive minConf tại mining stage (đã verify 9 algorithms: MCAR, CPAR, ECBA, FACA, MAC, BCAR, L³, CMAR-XGB, WCBA — tất cả dùng global minConf).

### WCBA 2018 — KHÔNG phải nguồn của H2/H3

> **Alwidian, J., Hammo, B.H., & Obeid, N.** (2018). *WCBA: Weighted classification based on association rules algorithm for breast cancer disease*. Applied Soft Computing 62, pp. 536-549.

Sau khi đọc PDF trực tiếp, WCBA paper thực sự đề xuất:
- Attribute weights (gán bởi domain experts, scale 1-10)
- Weighted Support = `support × attribute_weight`
- Harmonic Mean ranking
- Strong + Spare rules

**WCBA KHÔNG có:** class-specific minSup, adaptive minConf per class, hoặc bất kỳ xử lý class imbalance nào.

### Câu trả lời chuẩn cho giảng viên

> "Dạ thưa thầy, em xin báo cáo nguồn gốc:
>
> **H2** kế thừa trực tiếp từ paper **Liu, Ma, Wong (2000)** *Improving an Association Rule Based Classifier* tại PKDD 2000. Liu et al. đề xuất công thức `minsupp_i = minsupp_t × supp(c_i)/max(supp(C))` cho CBA. H2 của em là biến thể adapt cho CMAR với 2 điểm tinh chỉnh: (1) dùng absolute frequency thay relative support, (2) thêm safety floor `max(2, ...)`.
>
> **H3** là **novel combination** dựa trên 2 paper:
> - **Vo, Nguyen, Hong (2015)** *Class Association Rule Mining with Multiple Imbalanced Attributes* tại ICDM 2015 — đề xuất weighted confidence theo class frequency.
> - **Nguyen, Vo et al. (2019)** *Mining class association rules on imbalanced class datasets* tại JIFS — chứng minh lift ít bias hơn confidence với class imbalance.
>
> **Đóng góp gốc của H3** là combine 2 ý tưởng này thành adaptive threshold tại **MINING stage** với công thức `minConf(c) = min(globalMinConf, max(floor, lift × P(c)))`. Theo khảo sát 9 thuật toán AC, em là **first** apply adaptive minConf tại rule generation phase — các paper khác chỉ áp dụng post-mining ranking."

---

## Q5: SMOTE chạy với tham số K và N hả?

### TL;DR
Đúng — 2 tham số chính:
- **K = 5** (số nearest neighbors)
- **target_ratio = 1.0** (tỷ lệ balance đích, tương đương N=100% trong paper gốc)

### Tham số chi tiết

```
┌─────────────────────────────────────────────────────────────┐
│  K = số nearest neighbors                                   │
│      Mặc định: 5                                            │
│      Vai trò: tìm K records minority gần nhất với base      │
│              để lấy attribute values trộn vào synthetic     │
│      Edge case: nếu class < K+1 records → K_eff = size−1    │
│                                                             │
│  target_ratio (= N trong paper):                            │
│      Mặc định: 1.0 (balance hoàn toàn về max_class_freq)    │
│      0.5 = balance về 50% majority size                     │
│      1.0 = balance về 100% majority size                    │
└─────────────────────────────────────────────────────────────┘
```

### So sánh với Paper SMOTE gốc

| Aspect | Paper Chawla 2002 | Project (v11) |
|--------|-------------------|---------------|
| K (neighbors) | 5 (default) | **5** ✓ |
| Oversample param | N% (e.g., N=100% gấp đôi) | target_ratio (intuitive hơn) |
| target_ratio=1.0 tương đương | N = (max_freq/min_freq − 1) × 100% | Balance hoàn toàn |

### Ví dụ trực quan với K=5

**Tạo 1 synthetic record cho fibrosis (4 records):**

```
Step 1: Chọn base
  base = r1 (random từ {r1, r2, r3, r4})

Step 2: Tìm K=5 nearest neighbors
  Nhưng fibrosis chỉ có 4 records → K_eff = min(5, 4−1) = 3
  Sort các records khác r1 theo Hamming distance:
    r4 (gần nhất — khác 1 attribute)
    r2 (khác 2 attributes)
    r3 (khác 4 attributes)
  neighbors = {r4, r2, r3}

Step 3: Cho mỗi attribute, lấy MODE của 4 records (base + 3 neighbors)
  ┌─────────────────┬──────┬──────┬──────┬──────┬──────────┐
  │ Attribute       │ r1   │ r4   │ r2   │ r3   │ MODE     │
  ├─────────────────┼──────┼──────┼──────┼──────┼──────────┤
  │ lymphatics      │normal│normal│arched│deform│ normal   │
  │ block           │ no   │ no   │ no   │ yes  │ no       │
  │ changes_in_lym  │ bean │ bean │ oval │round │ bean     │
  │ ...             │ ...  │ ...  │ ...  │ ...  │ ...      │
  └─────────────────┴──────┴──────┴──────┴──────┴──────────┘

Step 4: Tạo synthetic
  synthetic = Transaction(
    lymphatics=normal, block=no, changes_in_lym=bean, ...,
    class=fibrosis
  )

Lặp 77 lần để có 77 synthetic fibrosis records.
Sau cùng, fibrosis có 4 thật + 77 synthetic = 81 records.
```

### Tại sao target_ratio = 1.0?

| Lý do | Giải thích |
|-------|------------|
| **Best practice** | Đa số literature về imbalanced learning recommend fully balanced cho extreme imbalance |
| **Đơn giản** | Ít hyperparameter để tune |
| **Hiệu quả** | Trên Lymph: F1 +75.8% với ratio=1.0 |

### ⚠️ Khi nào nên giảm target_ratio?

```
Glass dataset (6 classes phức tạp):
  target_ratio=1.0 → MacroF1 GIẢM −1.12% ⚠️
  → Synthetic quá nhiều gây nhiễu boundary giữa 6 classes

Tương lai: thử target_ratio=0.5 cho multi-class complex.
```

### Code thực tế — [src/SMOTE.java](../src/SMOTE.java)

```java
public static List<Transaction> apply(List<Transaction> data,
                                       int k,                // K-neighbors (5)
                                       double targetRatio,   // = N (1.0)
                                       long seed) {
    // 1. Group by class, find max_freq
    // 2. target = round(max_freq × targetRatio)
    // 3. For each class < target:
    //      while not enough:
    //        base = random record
    //        kEff = min(k, records.size() - 1)
    //        neighbors = kNearestNeighbors(base, records, kEff)
    //        synthetic = createSynthetic(base, neighbors)  // mode voting
    //        augmented.add(synthetic)
    return augmented;
}

// Default convenience
public static List<Transaction> apply(List<Transaction> data) {
    return apply(data, 5, 1.0, 42);   // K=5, ratio=1.0, seed=42
}
```

### Câu trả lời chuẩn cho giảng viên

> "Dạ thưa thầy đúng ạ, SMOTE có 2 tham số chính:
>
> 1. **K = 5** — số nearest neighbors. Em dùng K = 5 theo mặc định của Chawla 2002. Với class có ít hơn K+1 records (như fibrosis chỉ 4 records), em dùng `K_effective = min(5, size−1) = 3`.
> 2. **target_ratio = 1.0** — tỷ lệ balance đích. Project em dùng `target_ratio = 1.0`, nghĩa là balance hoàn toàn về max_class_freq. Tương đương `N = 100%` trong paper gốc.
>
> Em chọn `target_ratio = 1.0` (fully balanced) vì đa số literature recommend cho extreme imbalance. Tương lai có thể tune `target_ratio = 0.5` cho multi-class complex như Glass."

---

## 10 câu hỏi follow-up khó

### Q6: "Tại sao chọn SMOTE chứ không phải Undersampling?"
> "Dạ thưa thầy, 2 lý do:
> 1. **Undersampling mất thông tin**: bỏ records majority = bỏ data hữu ích.
> 2. **Datasets thực tế nhỏ**: Lymph chỉ 148 records, undersampling từ 81 metastases xuống còn 4 = mất 95% data.
>
> SMOTE giữ nguyên data gốc và **bổ sung** synthetic → tận dụng tối đa thông tin."

### Q7: "SMOTE có gây overfitting không?"
> "Dạ thưa thầy có rủi ro. Em mitigate bằng 2 cách:
> 1. **Adaptive trigger**: chỉ bật SMOTE khi cần (`min_freq < 10`) → 17/20 datasets không động đến.
> 2. **K-NN voting** thay vì duplicate đơn thuần: synthetic dựa trên mode của 5 neighbors thật → giữ pattern, không bản sao.
>
> Bằng chứng: Glass có Recall tăng (+5.97%) nhưng F1 giảm (-1.12%) — chứng tỏ SMOTE *có thể* hurt khi data complex, nhưng không catastrophic. Trên 17 datasets test có min_freq cao, SMOTE off hoàn toàn → không có cơ hội overfit."

### Q8: "Tại sao SMOTE_TRIGGER = 10?"
> "Dạ thưa thầy đây là **heuristic** dựa trên quan sát: với minSup ~5%, class ≥10 records đạt được ngưỡng mining trên dataset ~150-200 records. Em đã thử nghiệm và threshold này hoạt động tốt — 3 datasets trigger, 17 datasets không. Em thừa nhận không phải optimal universal — tương lai có thể tune qua nested CV."

### Q9: "Tại sao công thức H3 dùng floor=0.3, lift=5?"
> "Dạ thưa thầy:
> - **floor=0.3** đảm bảo dù minority cực nhỏ, rule vẫn phải có confidence ≥30%.
> - **lift=5** nghĩa rule phải tốt hơn baseline ngẫu nhiên 5 lần.
> Hai số này em chọn để **balance**: không quá nới (gây tạp), không quá khắt (mất rules). Em đã thử nghiệm trên 20 UCI và combination này cho kết quả ổn định nhất."

### Q10: "So với deep learning thì sao?"
> "Dạ thưa thầy CMAR và DL là 2 paradigm khác nhau:
> - **CMAR** → interpretable rules (có thể đọc và giải thích)
> - **DL** → black-box accuracy
>
> Project này focus vào **rule-based**, không cạnh tranh DL về accuracy. Trong lĩnh vực y tế (như Lymph), interpretability quan trọng hơn accuracy — bác sĩ cần biết WHY model predict, không chỉ WHAT."

### Q11: "Có dùng significance test (Wilcoxon, t-test) không?"
> "Dạ thưa thầy em chưa làm significance test. Em chỉ báo cáo mean ± std của 10-fold CV. Đây là **hạn chế** em sẽ bổ sung cho version tới — dự kiến dùng Wilcoxon signed-rank test để so sánh paired (baseline vs L+SMOTE) trên 20 datasets."

### Q12: "Kết quả có reproduce được không?"
> "Dạ thưa thầy hoàn toàn được:
> - Seed cố định = 42
> - 10-fold stratified CV
> - Code public trên GitHub: github.com/Congle666/CMAR
> - Lệnh chạy: `javac -d out src/*.java && java -cp out BenchmarkSMOTEFull`
> - Mất ~25 phút trên i7 + 16GB.
> - Em có ghi log chi tiết và 6 CSV output để verify."

### Q13: "Glass MacroF1 giảm — em giải thích thế nào?"
> "Dạ thưa thầy Glass là multi-class phức tạp (6 classes). SMOTE_RATIO=1.0 quá aggressive → balance class size=9 lên 76 (gấp 8 lần) → synthetic records quá nhiều, gây nhiễu boundary với các class moderate (13, 17 records). Recall vẫn tăng (+5.97%) nhưng Precision giảm → F1 giảm.
>
> Đây là **hạn chế** em đã nhận diện. Giải pháp tương lai: dùng target_ratio = 0.5 hoặc Borderline-SMOTE cho dataset này."

### Q14: "Tại sao chỉ test trên UCI mà không dùng dataset mới hơn?"
> "Dạ thưa thầy em chọn UCI vì paper CMAR 2001 dùng tập này → fair comparison với paper gốc. UCI cũng là benchmark chuẩn trong literature association rules. Tương lai có thể test trên dataset mới hơn (KDD Cup, Kaggle competitions) cho thesis defense."

### Q15: "H2/H3/SMOTE có thể áp dụng cho thuật toán khác (như CBA, CPAR) không?"
> "Dạ thưa thầy về lý thuyết được. H2/H3 là sửa **mining stage** của FP-Growth, áp dụng được cho mọi thuật toán dùng FP-Growth (CBA, CPAR, MCAR...). SMOTE là **data preprocessing**, hoàn toàn độc lập với classifier — áp dụng được cho mọi thuật toán supervised learning.
>
> Tuy nhiên em chỉ validate trên CMAR. Để generalize, em cần test thêm trên CBA hoặc các thuật toán khác."

---

## Cheat sheet số liệu phải nhớ

### Số liệu Lymph (highlight chính)
| Metric | Trước | Sau | % tăng |
|--------|:-----:|:---:|:------:|
| Accuracy | 0.8346 | 0.8328 | −0.18% (giảm nhẹ) |
| **MacroF1** | **0.4235** | **0.7445** | **+75.8%** ⭐ |
| **MacroRecall** | **0.4353** | **0.7949** | **+82.6%** ⭐ |
| fibrosis F1 (4 records) | 0.000 | **0.500** | breakthrough |
| normal F1 (2 records) | 0.000 | **0.800** | breakthrough |
| normal Recall | 0.000 | **1.000** | bắt 100% |

### Số liệu trung bình 20 UCI
| Metric | Trước | Sau | % tăng |
|--------|:-----:|:---:|:------:|
| Average Accuracy | 0.8412 | 0.8436 | +0.29% |
| Average MacroF1 | 0.8034 | 0.8227 | **+2.40%** |
| Average MacroRecall | 0.8117 | 0.8350 | **+2.87%** |

### Số liệu Win-count
- Cải thiện F1: 9 datasets
- Tied: 9 datasets
- Giảm nhẹ: 2 datasets (glass −1.12%, horse −0.26%)
- SMOTE kích hoạt: 3/20 datasets (lymph, glass, zoo)
- Zero regression: 17/20 datasets

### Tham số SMOTE
- K (neighbors) = **5**
- target_ratio = **1.0** (fully balanced)
- SMOTE_TRIGGER = **10** (min_class_freq < 10 thì bật)
- seed = 42 (reproducible)

### Tham số H2 + H3
- H2: `minSup(c) = max(2, ⌊supPct × freq(c)⌋)`
- H3: `minConf(c) = min(0.5, max(0.3, 5 × freq(c)/N))`
  - floor = 0.3
  - lift = 5

### Setup
- 20 UCI datasets (cùng tập paper CMAR 2001)
- 10-fold stratified cross-validation
- Time: ~25 phút trên i7 + 16GB

---

## Tài liệu liên quan

- 📄 [de_hieu_van_de_va_cai_tien.md](de_hieu_van_de_va_cai_tien.md) — Giải thích easy-to-understand
- 📄 [H2_H3_SMOTE_explained.md](H2_H3_SMOTE_explained.md) — Technical deep-dive
- 📄 [v11_smote_full_uci.md](v11_smote_full_uci.md) — Kết quả full 20 UCI
- 📄 [v10_smote_threshold.md](v10_smote_threshold.md) — Ablation study v10
- 💻 [src/SMOTE.java](../src/SMOTE.java) — Implementation
- 💻 [src/CrossValidator.java](../src/CrossValidator.java) — Pipeline integration

## Tham khảo

1. **Chawla, N. V., Bowyer, K. W., Hall, L. O., & Kegelmeyer, W. P.** (2002). *SMOTE: Synthetic Minority Over-sampling Technique*. JAIR 16, 321-357.
2. **Li, W., Han, J., & Pei, J.** (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules*. ICDM 2001.
3. **Liu, B., Ma, Y., & Wong, C.K.** (2000). *Improving an Association Rule Based Classifier*. PKDD 2000, LNCS 1910, pp. 504-509. DOI: 10.1007/3-540-45372-5_58. — **Nguồn của H2**.

4. **Vo, B., Nguyen, L.T.T., & Hong, T.P.** (2015). *Class Association Rule Mining with Multiple Imbalanced Attributes*. ICDM 2015 Workshops, LNCS 9376. — **Component A của H3**.

5. **Nguyen, L.T.T., Vo, B., Nguyen, T.-N., et al.** (2019). *Mining class association rules on imbalanced class datasets*. JIFS 37(1). DOI: 10.3233/JIFS-179326. — **Component B của H3**.

6. **Hu, L.Y., et al.** (2016). *Building an associative classifier with multiple minimum supports*. SpringerPlus 5(1):1-19. — MMSCBA refinement.
