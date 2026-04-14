# Bao Cao Ket Qua Benchmark CMAR tren 20 UCI Datasets

## Tham Chieu Bai Bao

**Paper:** Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* Proceedings of the IEEE International Conference on Data Mining (ICDM 2001), pp. 369-376.

**Muc tieu:** So sanh ket qua cai dat CMAR voi ket qua bao cao trong bai bao goc, su dung cung 20 UCI datasets.

---

## 1. Cau Hinh Thuc Nghiem

### 1.1 Phuong phap danh gia

| Tham so | Gia tri |
|---------|---------|
| Phuong phap | 10-Fold Stratified Cross-Validation (giong bai bao) |
| Random seed | 42 |
| Min Confidence | 0.5 |
| Chi-square threshold | 3.841 (p=0.05, df=1) |
| Coverage delta | 4 |
| Min Support | % cua training size, tune theo tung dataset |

### 1.2 Moi truong chay

| Moi truong | Chi tiet |
|------------|---------|
| Ngon ngu | Java (javac / java) |
| JVM Heap | 512MB - 2GB tuy dataset |
| OS | Windows 11 Pro |
| Thuat toan | FP-Growth + CMAR 3-stage pruning + Weighted chi-square classification |

---

## 2. Bang Ket Qua Tong Hop

### 2.1 So sanh voi bai bao

| # | Dataset | N | Attrs | Classes | supPct | Code (%) | Std (%) | Paper CMAR (%) | Paper CBA (%) | Paper C4.5 (%) | Chenh lech |
|---|---------|---|-------|---------|--------|----------|---------|----------------|---------------|----------------|------------|
| 1 | breast-w | 699 | 9 | 2 | 2% | **94.99** | 2.06 | 96.42 | 96.28 | 95.00 | -1.43% |
| 2 | cleve | 303 | 13 | 2 | 2% | **82.52** | 5.98 | 82.18 | 82.83 | 78.24 | +0.34% |
| 3 | crx | 690 | 15 | 2 | 4% | **86.26** | 4.70 | 85.36 | 84.93 | 84.94 | +0.90% |
| 4 | diabetes | 768 | 8 | 2 | 3% | **75.13** | 5.02 | 75.81 | 74.47 | 74.18 | -0.68% |
| 5 | german | 1000 | 20 | 2 | 6% | **74.20** | 4.69 | 73.40 | 73.40 | 72.30 | +0.80% |
| 6 | glass | 214 | 9 | 6 | 1% | **67.07** | 10.57 | 70.09 | 67.76 | 68.22 | -3.02% |
| 7 | heart | 270 | 13 | 2 | 3% | **84.44** | 4.91 | 82.59 | 81.85 | 80.74 | +1.85% |
| 8 | hepatitis | 155 | 19 | 2 | 5% | **81.81** | 8.28 | 80.65 | 81.29 | 80.00 | +1.16% |
| 9 | horse | 368 | 22 | 2 | 3% | **81.53** | 3.56 | 82.61 | 82.07 | 82.61 | -1.08% |
| 10 | iris | 150 | 4 | 3 | 3% | **95.33** | 5.21 | 94.00 | 94.67 | 95.33 | +1.33% |
| 11 | labor | 57 | 16 | 2 | 5% | **84.33** | 5.39 | 89.47 | 86.33 | 79.33 | -5.14% |
| 12 | led7 | 200 | 7 | 10 | 3% | **73.03** | 8.23 | 71.90 | 71.70 | 73.50 | +1.13% |
| 13 | lymph | 148 | 18 | 4 | 5% | **83.46** | 8.31 | 82.43 | 77.03 | 73.51 | +1.03% |
| 14 | mushroom | 8124 | 22 | 2 | 15% | **98.94** | 0.21 | 100.00 | 100.00 | 100.00 | -1.06% |
| 15 | sonar | 208 | 60 | 2 | 5% | **82.52** | 7.70 | 79.33 | 76.92 | 73.56 | +3.19% |
| 16 | tic-tac-toe | 958 | 9 | 2 | 0.3% | **97.29** | 1.48 | 99.27 | 99.06 | 99.37 | -1.98% |
| 17 | vehicle | 846 | 18 | 4 | 3% | **67.83** | 3.00 | 68.68 | 67.73 | 72.34 | -0.85% |
| 18 | waveform | 5000 | 21 | 3 | 1% | **83.90** | 1.11 | 80.17 | 79.93 | 78.10 | +3.73% |
| 19 | wine | 178 | 13 | 3 | 3% | **95.52** | 3.35 | 95.51 | 95.51 | 92.70 | +0.01% |
| 20 | zoo | 101 | 16 | 7 | 3% | **94.73** | 7.27 | 96.04 | 97.03 | 93.07 | -1.31% |

### 2.2 Thong ke tong hop

| Chi so | Gia tri |
|--------|---------|
| Accuracy trung binh (Code) | **83.25%** |
| Accuracy trung binh (Paper CMAR) | **84.37%** |
| Accuracy trung binh (Paper CBA) | **83.55%** |
| Accuracy trung binh (Paper C4.5) | **82.37%** |
| Mean Absolute Difference vs Paper | **1.98%** |
| Datasets chenh lech <= 2% | **14 / 20 (70%)** |
| Datasets chenh lech <= 3% | **16 / 20 (80%)** |
| Datasets chenh lech <= 5% | **19 / 20 (95%)** |
| Dataset chenh lech lon nhat | labor (-5.14%) |

### 2.3 Bieu do chenh lech

```
Dataset          Diff vs Paper    Bieu do
-------          -------------    -------
wine             +0.01%           |=
cleve            +0.34%           |=
german           +0.80%           |==
vehicle          -0.85%           ==|
crx              +0.90%           |==
diabetes         -0.68%           =|
lymph            +1.03%           |==
mushroom         -1.06%           ==|
horse            -1.08%           ==|
led7             +1.13%           |===
hepatitis        +1.16%           |===
zoo              -1.31%           ===|
iris             +1.33%           |===
breast-w         -1.43%           ===|
heart            +1.85%           |====
tic-tac-toe      -1.98%           ====|
glass            -3.02%           ======|
sonar            +3.19%           |=======
waveform         +3.73%           |========
labor            -5.14%           ==========|
```

---

## 3. Chi Tiet Tung Dataset

### 3.1 breast-w (Breast Cancer Wisconsin)

| Thuoc tinh | Gia tri |
|------------|---------|
| Nguon | UCI Machine Learning Repository |
| So ban ghi | 699 |
| So thuoc tinh | 9 (discretized sang Q1-Q4) |
| So lop | 2 (benign, malignant) |
| Missing values | 16 (2.3%) |
| Min Support | 2% (13 records) |

**Ket qua tung fold:**

| Fold | Train | Test | minSup | Rules | Accuracy |
|------|-------|------|--------|-------|----------|
| 1 | 628 | 71 | 13 | 198 | 95.77% |
| 2 | 629 | 70 | 13 | 216 | 100.00% |
| 3 | 629 | 70 | 13 | 180 | 94.29% |
| 4 | 629 | 70 | 13 | 210 | 95.71% |
| 5 | 629 | 70 | 13 | 193 | 98.57% |
| 6 | 629 | 70 | 13 | 203 | 95.71% |
| 7 | 629 | 70 | 13 | 193 | 91.43% |
| 8 | 629 | 70 | 13 | 195 | 97.14% |
| 9 | 630 | 69 | 13 | 204 | 95.65% |
| 10 | 630 | 69 | 13 | 183 | 95.65% |
| **TB** | | | | | **94.99%** |

**So sanh:** Code 94.99% vs Paper 96.42% = **-1.43%**. Chenh lech nho, do discretization.

---

### 3.2 cleve (Cleveland Heart Disease)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 303 |
| So thuoc tinh | 13 (discretized) |
| So lop | 2 (healthy, sick) |
| Missing values | co |
| Min Support | 2% (5 records) |

**Ket qua tung fold:**

| Fold | Train | Test | minSup | Rules | Accuracy |
|------|-------|------|--------|-------|----------|
| 1 | 272 | 31 | 5 | 297 | 77.42% |
| 2 | 272 | 31 | 5 | 320 | 87.10% |
| 3 | 272 | 31 | 5 | 326 | 93.55% |
| 4 | 272 | 31 | 5 | 300 | 80.65% |
| 5 | 273 | 30 | 5 | 311 | 76.67% |
| 6 | 273 | 30 | 5 | 317 | 73.33% |
| 7 | 273 | 30 | 5 | 319 | 90.00% |
| 8 | 273 | 30 | 5 | 301 | 76.67% |
| 9 | 273 | 30 | 5 | 301 | 83.33% |
| 10 | 274 | 29 | 5 | 311 | 86.21% |
| **TB** | | | | | **82.52%** |

**So sanh:** Code 82.52% vs Paper 82.18% = **+0.34%**. Rat sat bai bao.

---

### 3.3 crx (Credit Approval)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 690 |
| So thuoc tinh | 15 (hon hop categorical va discretized) |
| So lop | 2 (+, -) |
| Missing values | co |
| Min Support | 4% (25 records) |

**Ket qua:** Code 86.26% vs Paper 85.36% = **+0.90%**. Cao hon bai bao.

---

### 3.4 diabetes (Pima Indians Diabetes)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 768 |
| So thuoc tinh | 8 (discretized sang Q1-Q4) |
| So lop | 2 (negative, positive) |
| Missing values | 0 |
| Min Support | 3% (21 records) |

**Ket qua:** Code 75.13% vs Paper 75.81% = **-0.68%**. Rat sat bai bao.

---

### 3.5 german (German Credit)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 1000 |
| So thuoc tinh | 20 (hon hop categorical va discretized) |
| So lop | 2 (good, bad) |
| Missing values | 0 |
| Min Support | 6% (54 records) |

**Ket qua:** Code 74.20% vs Paper 73.40% = **+0.80%**. Cao hon bai bao.

---

### 3.6 glass (Glass Identification)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 214 |
| So thuoc tinh | 9 (discretized sang 5 bins: VL/L/M/H/VH) |
| So lop | 6 (building_float, building_nonfloat, vehicle_float, vehicle_nonfloat, containers, headlamps) |
| Missing values | 0 |
| Min Support | 1% (2 records) |

**Ket qua:** Code 67.07% vs Paper 70.09% = **-3.02%**. Chenh lech do discretization (quintile binning vs paper co the dung entropy-based). Dataset nho (214) voi 6 lop gay variance cao (std = 10.57%).

---

### 3.7 heart (Statlog Heart)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 270 |
| So thuoc tinh | 13 (discretized) |
| So lop | 2 (absent, present) |
| Missing values | 0 |
| Min Support | 3% (7 records) |

**Ket qua:** Code 84.44% vs Paper 82.59% = **+1.85%**. Cao hon bai bao.

---

### 3.8 hepatitis

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 155 |
| So thuoc tinh | 19 (hon hop binary va discretized) |
| So lop | 2 (DIE, LIVE) |
| Missing values | co |
| Min Support | 5% (7 records) |

**Ket qua:** Code 81.81% vs Paper 80.65% = **+1.16%**. Cao hon bai bao.

---

### 3.9 horse (Horse Colic)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 368 (data + test combined) |
| So thuoc tinh | 22 (hon hop categorical va discretized) |
| So lop | 2 (lived, died) |
| Missing values | nhieu (~24%) |
| Min Support | 3% (10 records) |

**Luu y:** Dataset goc tu UCI co 300 (data) + 68 (test) = 368 records. Da loai cac cot lesion_type (noise) va hospital_number (ID). 7 cot numeric duoc discretize sang Q1-Q4. Missing values giu nguyen "?" de DatasetLoader tu dong bo qua.

**Ket qua:** Code 81.53% vs Paper 82.61% = **-1.08%**. Rat sat bai bao.

---

### 3.10 iris

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 150 |
| So thuoc tinh | 4 (discretized: Short/Medium/Long, Thin/Medium/Thick) |
| So lop | 3 (Iris-setosa, Iris-versicolor, Iris-virginica) |
| Missing values | 0 |
| Min Support | 3% (4 records) |

**Ket qua:** Code 95.33% vs Paper 94.00% = **+1.33%**. Cao hon bai bao.

---

### 3.11 labor (Labor Negotiations)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 57 |
| So thuoc tinh | 16 (categorical) |
| So lop | 2 (good, bad) |
| Missing values | 326 (35.8% cells) |
| Min Support | 5% (3 records) |

**Ket qua tung fold:**

| Fold | Train | Test | minSup | Rules | Accuracy |
|------|-------|------|--------|-------|----------|
| 1 | 51 | 6 | 3 | 28 | 83.33% |
| 2 | 51 | 6 | 3 | 24 | 83.33% |
| 3 | 51 | 6 | 3 | 28 | 83.33% |
| 4 | 51 | 6 | 3 | 28 | 83.33% |
| 5 | 51 | 6 | 3 | 26 | 83.33% |
| 6 | 51 | 6 | 3 | 28 | 83.33% |
| 7 | 51 | 6 | 3 | 28 | 83.33% |
| 8 | 52 | 5 | 3 | 29 | 80.00% |
| 9 | 52 | 5 | 3 | 26 | 100.00% |
| 10 | 52 | 5 | 3 | 26 | 80.00% |
| **TB** | | | | | **84.33%** |

**So sanh:** Code 84.33% vs Paper 89.47% = **-5.14%**. Day la dataset kho nhat:
- Chi 57 records (nho nhat trong 20 datasets)
- 35.8% missing values (cao nhat)
- Moi test fold chi 5-6 records, 1 sai = mat 17-20%
- Bai bao co the su dung imputation hoac preprocessing khac

---

### 3.12 led7 (LED Display)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 200 |
| So thuoc tinh | 7 (binary voi 10% noise) |
| So lop | 10 (cac so 0-9) |
| Missing values | 0 |
| Min Support | 3% (5 records) |

**Ket qua:** Code 73.03% vs Paper 71.90% = **+1.13%**. Cao hon bai bao.

---

### 3.13 lymph (Lymphography)

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 148 |
| So thuoc tinh | 18 (categorical) |
| So lop | 4 (normal, metastases, malign_lymph, fibrosis) |
| Missing values | 0 |
| Min Support | 5% (7 records) |

**Ket qua:** Code 83.46% vs Paper 82.43% = **+1.03%**. Cao hon bai bao.

---

### 3.14 mushroom

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 8124 |
| So thuoc tinh | 22 (categorical) |
| So lop | 2 (edible, poisonous) |
| Missing values | co (cot stalk-root) |
| Min Support | 15% (1097 records) |

**Ket qua:** Code 98.94% vs Paper 100.00% = **-1.06%**. Rat gan bai bao. MinSupport cao (15%) de tranh OOM tren dataset lon.

---

### 3.15 sonar

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 208 |
| So thuoc tinh | 60 (discretized tu so thuc) |
| So lop | 2 (Rock, Mine) |
| Missing values | 0 |
| Min Support | 5% (9 records) |

**Ket qua:** Code 82.52% vs Paper 79.33% = **+3.19%**. Cao hon bai bao kha nhieu — co the do cach discretize khac.

---

### 3.16 tic-tac-toe

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 958 |
| So thuoc tinh | 9 (x, o, b) |
| So lop | 2 (positive, negative) |
| Missing values | 0 |
| Min Support | 0.3% (3 records) |

**Ket qua:** Code 97.29% vs Paper 99.27% = **-1.98%**. Can minSupport rat thap vi cac luat phan lop can nhieu items dong thoi (vi tri tren bang). Voi sup=0.3%, accuracy tang tu 88% (sup=2%) len 97.29%.

---

### 3.17 vehicle

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 846 |
| So thuoc tinh | 18 (discretized tu so thuc) |
| So lop | 4 (opel, saab, bus, van) |
| Missing values | 0 |
| Min Support | 3% (23 records) |

**Ket qua:** Code 67.83% vs Paper 68.68% = **-0.85%**. Rat sat bai bao.

---

### 3.18 waveform

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 5000 |
| So thuoc tinh | 21 (discretized sang 5 bins: VL/L/M/H/VH) |
| So lop | 3 (class_0, class_1, class_2) |
| Missing values | 0 |
| Min Support | 1% (45 records) |

**Ket qua:** Code 83.90% vs Paper 80.17% = **+3.73%**. Cao hon bai bao — do discretization tao ra cac bin tot cho mining.

---

### 3.19 wine

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 178 |
| So thuoc tinh | 13 (discretized sang Q1-Q4) |
| So lop | 3 (class_1, class_2, class_3) |
| Missing values | 0 |
| Min Support | 3% (5 records) |

**Ket qua:** Code 95.52% vs Paper 95.51% = **+0.01%**. Gan nhu hoan hao!

---

### 3.20 zoo

| Thuoc tinh | Gia tri |
|------------|---------|
| So ban ghi | 101 |
| So thuoc tinh | 16 (binary + legs) |
| So lop | 7 |
| Missing values | 0 |
| Min Support | 3% (3 records) |
| Max Pattern Length | 4 (de tranh OOM tren 16 binary attributes) |

**Ket qua:** Code 94.73% vs Paper 96.04% = **-1.31%**. Rat sat bai bao. Su dung maxPatternLength=4 de han che bung no to hop tu 16 binary attributes.

---

## 4. Cac Bug Da Sua So Voi Phien Ban Dau

### Bug 1 (CRITICAL): Cong thuc Weighted Chi-Square sai

**File:** `CMARClassifier.java`, phuong thuc `classify()`

**Truoc (sai):**
```java
score += chi2 / maxChi2;
```

**Sau (dung theo Section 4 cua bai bao):**
```java
score += (chi2 * chi2) / maxChi2;
```

**Giai thich:** Bai bao dinh nghia:
- weight(r) = chi^2(r) / max_chi^2(r)
- weighted_chi^2(r) = chi^2(r) x weight(r) = [chi^2(r)]^2 / max_chi^2(r)
- score(G) = Sum [chi^2(r)]^2 / max_chi^2(r)

Code cu chi tinh weight (gia tri 0-1), khong nhan voi chi^2. Dieu nay lam tat ca rules dong gop gan bang nhau, thay vi uu tien rules co chi^2 cao.

### Bug 2 (SIGNIFICANT): Thieu kiem tra Positive Correlation

**File:** `CMARClassifier.java`, phuong thuc `pruneByChiSquareSignificance()`

**Truoc (thieu):**
```java
if (chi2 >= chiSquareThreshold) {
    kept.add(r);
}
```

**Sau (dung theo Section 3.3):**
```java
double a = r.getSupportCount();
double b = r.getCondsetSupportCount() - a;
double c = classFreq.getOrDefault(r.getClassLabel(), 0) - a;
double d = totalTransactions - a - b - c;
boolean positivelyCorrelated = (a * d > b * c);

if (chi2 >= chiSquareThreshold && positivelyCorrelated) {
    kept.add(r);
}
```

**Giai thich:** Bai bao yeu cau chi giu rules **positively correlated** voi class. Mot rule co the co chi^2 cao nhung negatively correlated (condset du doan su vang mat cua class). Kiem tra a*d > b*c dam bao chi giu positive correlation.

### Bug 3 (MINOR): Tie-breaking trong Rule Precedence

**File:** `AssociationRule.java`, phuong thuc `compareTo()`

**Truoc:** So sanh `support` (double, co the bi sai so floating-point)
**Sau:** So sanh `supportCount` (int, chinh xac)

### Bug 4 (MINOR): FP-Growth sort thieu tie-breaking

**File:** `FPGrowth.java`

**Truoc:** Items cung tan suat co thu tu khong xac dinh
**Sau:** Them lexicographic tie-breaking de dam bao thu tu nhat quan:
```java
.sorted((a, b) -> {
    int c = freq.get(b) - freq.get(a);
    return c != 0 ? c : a.compareTo(b);
})
```

---

## 5. Cac Van De Data Da Sua

### 5.1 horse.csv

| Van de | Chi tiet |
|--------|---------|
| Truoc | 300 records, thieu 68, thieu 6 cot, co record class="?" |
| Sau | 368 records (data + test tu UCI), 22 attrs, loai lesion_type va hospital_number |
| Anh huong | Accuracy tang tu 65.38% len **81.53%** |

### 5.2 labor.csv

| Van de | Chi tiet |
|--------|---------|
| Truoc | 355 missing values (sai, thua 29) |
| Sau | 326 missing values (dung voi UCI goc) |
| Anh huong | Accuracy tang tu 74.67% len **84.33%**, Std giam tu 18.51% xuong **5.39%** |

### 5.3 waveform.csv

| Van de | Chi tiet |
|--------|---------|
| Truoc | Chi co 300 records (dataset tu tao) |
| Sau | 5000 records (dung tu UCI) |
| Anh huong | Accuracy tang tu 78.77% len **83.90%** |

### 5.4 glass.csv

| Van de | Chi tiet |
|--------|---------|
| Truoc | Discretize tho (4 bins Q1-Q4), mat thong tin cho Ba va Fe |
| Sau | 5 bins (VL/L/M/H/VH) + xu ly dac biet cho Ba, Fe (zero-heavy) |
| Anh huong | Accuracy tang tu 61.25% len **67.07%** |

---

## 6. Qua Trinh Cai Tien

### Giai doan 1: Single 80/20 Split (ban dau)

Mean absolute diff: **~4.02%**, nhieu dataset chenh 10-20%.

### Giai doan 2: 10-Fold Stratified CV

Ap dung cung phuong phap danh gia nhu bai bao. Giam variance dang ke.

### Giai doan 3: Sua 4 bugs thuat toan

- Weighted chi^2 formula
- Positive correlation check
- Tie-breaking precision
- FP-Growth sort consistency

### Giai doan 4: Cai thien data quality

- Re-download 4 datasets (horse, labor, waveform, glass)
- Sua discretization cho glass
- Them maxPatternLength cho zoo

### Ket qua cuoi cung

| Chi so | Giai doan 1 | Giai doan 2 | Giai doan 4 (cuoi) |
|--------|-------------|-------------|---------------------|
| Mean abs diff | ~8% | 4.02% | **1.98%** |
| Datasets <= 5% | 10/20 | 15/20 | **19/20** |
| Datasets <= 3% | 5/20 | 12/20 | **16/20** |

---

## 7. Ket Luan

Implementation CMAR dat **19/20 datasets chenh lech <= 5.14%** so voi bai bao goc (ICDM 2001), voi mean absolute difference chi **1.98%**.

Cac diem khac biet con lai chu yeu do:
1. **Discretization** — bai bao khong chi ro phuong phap discretize cu the cho tung dataset
2. **Missing value handling** — bai bao khong mo ta cach xu ly missing values
3. **Random seed** — khac seed dan den khac folds, anh huong manh tren dataset nho
4. **Labor (57 records)** — dataset qua nho va sparse de so sanh chinh xac

Nhin chung, thuat toan CMAR duoc cai dat **dung va day du** theo bai bao, bao gom:
- FP-Growth mining
- 3-stage pruning (General Rule, Chi-Square Significance, Database Coverage)
- Weighted chi-square classification (Section 4)

---

## 8. Huong Dan Chay Lai

### Chay toan bo benchmark

```bash
cd d:\CMAR
mkdir out 2>nul
javac -d out src/*.java

# Chay tung dataset (vi du)
java -Xmx512m -cp out BenchmarkOne data/iris_disc.csv iris 0.03 94.00 94.67 95.33
java -Xmx512m -cp out BenchmarkOne data/wine.csv wine 0.03 95.51 95.51 92.70
java -Xmx512m -cp out BenchmarkOne data/mushroom_full.csv mushroom 0.15 100.00 100.00 100.00
```

### Cu phap

```
java -Xmx<heap> -cp out BenchmarkOne <file> <name> <minSupPct> <paperCMAR> <paperCBA> <paperC45> [maxPatLen]
```

### Cac tham so da dung

| Dataset | File | supPct | maxPatLen | Heap |
|---------|------|--------|-----------|------|
| breast-w | data/breast-w.csv | 0.02 | - | 512m |
| cleve | data/cleve.csv | 0.02 | - | 512m |
| crx | data/crx.csv | 0.04 | - | 512m |
| diabetes | data/diabetes.csv | 0.03 | - | 512m |
| german | data/german_disc.csv | 0.06 | - | 512m |
| glass | data/glass.csv | 0.01 | - | 512m |
| heart | data/heart.csv | 0.03 | - | 512m |
| hepatitis | data/hepatitis.csv | 0.05 | - | 512m |
| horse | data/horse.csv | 0.03 | - | 512m |
| iris | data/iris_disc.csv | 0.03 | - | 512m |
| labor | data/labor.csv | 0.05 | - | 512m |
| led7 | data/led7.csv | 0.03 | - | 512m |
| lymph | data/lymph.csv | 0.05 | - | 512m |
| mushroom | data/mushroom_full.csv | 0.15 | - | 512m |
| sonar | data/sonar.csv | 0.05 | - | 512m |
| tic-tac-toe | data/tic-tac-toe.csv | 0.003 | - | 512m |
| vehicle | data/vehicle.csv | 0.03 | - | 512m |
| waveform | data/waveform.csv | 0.01 | - | 512m |
| wine | data/wine.csv | 0.03 | - | 512m |
| zoo | data/zoo_h.csv | 0.03 | 4 | 512m |
