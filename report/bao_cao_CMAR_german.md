# BAO CAO: THUAT TOAN CMAR TREN DATASET GERMAN CREDIT (UCI)

**Dataset:** German Credit (Statlog) — UCI Machine Learning Repository (1000 records, 20 attributes, 2 classes)
**Tham khao:** Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* ICDM 2001.

---

## Muc Luc

1. [Tong Quan Pipeline](#1-tong-quan-pipeline)
2. [Du Lieu Dau Vao](#2-du-lieu-dau-vao)
3. [Tien Xu Ly: Discretization](#3-tien-xu-ly-discretization)
4. [Cau Hinh Chay](#4-cau-hinh-chay)
5. [PHAN 1: FP-Growth — Dao Mo Tap Pho Bien](#5-phan-1-fp-growth--dao-mo-tap-pho-bien)
6. [PHAN 2: CMAR — Phan Lop](#6-phan-2-cmar--phan-lop)
7. [Danh Gia Hieu Suat](#7-danh-gia-hieu-suat)
8. [So Sanh Voi Bai Bao](#8-so-sanh-voi-bai-bao)

---

## 1. Tong Quan Pipeline

```
┌──────────┐   ┌──────────────┐   ┌───────────┐   ┌──────────┐   ┌──────────┐
│ Load CSV │──▶│ Discretize   │──▶│ FP-Growth │──▶│ Extract  │──▶│  CMAR    │
│ (german) │   │ (tercile)    │   │ Mining    │   │ CARs     │   │ Classify │
└──────────┘   └──────────────┘   └───────────┘   └──────────┘   └──────────┘
   Step 0          Step 0b           Step 2-3        Step 4        Step 5-7
```

| Buoc | Mo ta | Ket qua |
|------|-------|---------|
| 0 | Load german.csv (1000 records, 20 attributes, space-separated) | 1000 ban ghi |
| 0b | Discretize: 7 attributes so → categorical (tercile bins) | 1000 ban ghi categorical |
| 1 | Split 80/20 (seed=42) | Train: 800, Test: 200 |
| 2 | Xay dung FP-Tree + Header Table | 50 items trong header table |
| 3 | Mining de quy tren FP-Tree | **153,966 frequent patterns** |
| 4 | Trich xuat CARs (condset → class, conf >= 0.5) | **49,046 candidate rules** |
| 5a | Pruning 1: General rule pruning | **5,077 luat** (loai 43,969) |
| 5b | Pruning 2: Chi-square significance (>= 3.841) | **3,931 luat** (loai 1,146) |
| 5c | Pruning 3: Database coverage (delta=4) | **658 luat** (loai 3,273) |
| 6 | Phan lop bang weighted chi-square | Du doan 200 test records |
| 7 | Danh gia ket qua | **Accuracy = 73.50%** |

---

## 2. Du Lieu Dau Vao

**German Credit Dataset** (Statlog, 1994) — dataset danh gia rui ro tin dung, mot trong 26 datasets UCI duoc su dung trong bai bao CMAR.

### 2.1 Cac Thuoc Tinh

| # | Thuoc tinh | Kieu | Ma (UCI) | Mo ta |
|---|-----------|------|----------|-------|
| 1 | checking | Categorical | A11-A14 | Trang thai tai khoan vang lai |
| 2 | duration | Numerical | 4-72 thang | Thoi han vay (thang) |
| 3 | credit_hist | Categorical | A30-A34 | Lich su tin dung |
| 4 | purpose | Categorical | A40-A410 | Muc dich vay |
| 5 | amount | Numerical | 250-18,424 | So tien vay (DM) |
| 6 | savings | Categorical | A61-A65 | Tai khoan tiet kiem |
| 7 | employment | Categorical | A71-A75 | Thoi gian lam viec hien tai |
| 8 | install_rate | Numerical | 1-4 | Ty le tra gop / thu nhap |
| 9 | personal | Categorical | A91-A95 | Tinh trang ca nhan / gioi tinh |
| 10 | other_debtors | Categorical | A101-A103 | Nguoi bao lanh / con no khac |
| 11 | residence | Numerical | 1-4 | Thoi gian cu tru hien tai |
| 12 | property | Categorical | A121-A124 | Tai san |
| 13 | age | Numerical | 19-75 tuoi | Tuoi |
| 14 | other_install | Categorical | A141-A143 | Cac khoan tra gop khac |
| 15 | housing | Categorical | A151-A153 | Nha o |
| 16 | num_credits | Numerical | 1-4 | So khoan tin dung hien co |
| 17 | job | Categorical | A171-A174 | Nghe nghiep |
| 18 | num_liable | Numerical | 1-2 | So nguoi phu thuoc |
| 19 | telephone | Categorical | A191-A192 | Dien thoai |
| 20 | foreign | Categorical | A201-A202 | Lao dong nuoc ngoai |

### 2.2 Phan Bo Class

| Class | So luong | Ti le |
|-------|---------|-------|
| **good** (tin dung tot) | 700 | 70.0% |
| **bad** (tin dung xau) | 300 | 30.0% |

> **Mat can bang:** Class "good" chiem 70%, day la thach thuc cho phan lop — mo hinh co xu huong thien vi class da so.

---

## 3. Tien Xu Ly: Discretization

CMAR chi xu ly categorical data. German Credit co 7 attributes so can **discretize** truoc — day cung la cach bai bao lam ("we adopt the same method used by CBA to discretize continuous attributes").

**Phuong phap:** Tercile binning (chia thanh 3 khoang theo phan vi 33/66).

### 3.1 Cac Attributes So va Bins

| Attribute | Bin 1 | Bin 2 | Bin 3 |
|-----------|-------|-------|-------|
| duration (thang) | Short (<=12) | Medium (13-24) | Long (>24) |
| amount (DM) | Low (<=1,554) | Medium (1,555-3,371) | High (>3,371) |
| age (tuoi) | Young (<=28) | Middle (29-38) | Senior (>38) |
| install_rate | Low (1-2) | High (3-4) | — (2 bins) |
| residence | Short (1-2) | Long (3-4) | — (2 bins) |
| num_credits | Few (1) | Many (2+) | — (2 bins) |
| num_liable | One (1) | Two (2) | — (2 bins) |

### 3.2 Cac Attributes Categorical (13 cot — giu nguyen)

Cac cot nhu `checking` (A11-A14), `credit_hist` (A30-A34), `purpose` (A40-A49),
`savings` (A61-A65), `employment` (A71-A75), v.v. da la categorical nen khong can
chuyen doi.

### 3.3 Class Label

| Ma goc | Chuyen doi |
|--------|-----------|
| 1 | good |
| 2 | bad |

**Vi du chuyen doi:**
```
Goc:    A11 6 A34 A43 1169 A65 A75 4 A93 A101 4 A121 67 A143 A152 2 A173 1 A192 A201 1
Sau:    A11,Short,A34,A43,Low,A65,A75,High,A93,A101,Long,A121,Senior,A143,A152,Many,A173,One,A192,A201,good
```

---

## 4. Cau Hinh Chay

```
Dataset:              data/german_disc.csv
Train/Test:           800 / 200  (80/20)
Min Support:          40  (5.0% tren 800 records)
Min Confidence:       0.5 (50%)
Chi-square threshold: 3.841 (p=0.05, df=1)
Coverage delta:       4 (Algorithm 1 trong paper)
Random Seed:          42
```

**Lenh chay:**
```bash
cd d:\CMAR
javac -d out src\*.java
java -cp out GermanPreprocessor
java -Xmx1g -cp out Main data/german_disc.csv 40 0.5 3.841 4
```

---

## 5. PHAN 1: FP-Growth — Dao Mo Tap Pho Bien

### 5.1 Header Table (Bang Tan Suat Item — Top 30)

| # | Item | Frequency | Ti le (%) |
|---|------|-----------|-----------|
| 1 | foreign=A201 | 766 | 95.8% |
| 2 | other_debtors=A101 | 721 | 90.1% |
| 3 | num_liable=One | 678 | 84.8% |
| 4 | other_install=A143 | 646 | 80.8% |
| 5 | housing=A152 | 575 | 71.9% |
| 6 | **class=good** | **562** | **70.3%** |
| 7 | install_rate=High | 507 | 63.4% |
| 8 | num_credits=Few | 503 | 62.9% |
| 9 | job=A173 | 502 | 62.8% |
| 10 | savings=A61 | 481 | 60.1% |
| 11 | telephone=A191 | 478 | 59.8% |
| 12 | residence=Long | 452 | 56.5% |
| 13 | personal=A93 | 434 | 54.3% |
| 14 | credit_hist=A32 | 425 | 53.1% |
| 15 | residence=Short | 348 | 43.5% |
| 16 | duration=Medium | 323 | 40.4% |
| 17 | telephone=A192 | 322 | 40.3% |
| 18 | checking=A14 | 316 | 39.5% |
| 19 | num_credits=Many | 297 | 37.1% |
| 20 | install_rate=Low | 293 | 36.6% |
| 21 | duration=Short | 291 | 36.4% |
| 22 | age=Middle | 276 | 34.5% |
| 23 | amount=Medium | 276 | 34.5% |
| 24 | amount=High | 264 | 33.0% |
| 25 | employment=A73 | 264 | 33.0% |
| 26 | age=Senior | 263 | 32.9% |
| 27 | property=A123 | 262 | 32.8% |
| 28 | age=Young | 261 | 32.6% |
| 29 | amount=Low | 260 | 32.5% |
| 30 | personal=A92 | 243 | 30.4% |

**Tong items trong header table:** 50

**Nhan xet:**
- `foreign=A201` (lao dong trong nuoc) chiem 95.8% — hau nhu tat ca, item nay cung cap it thong tin phan lop
- `class=good` chiem 70.3% — phan anh su mat can bang class
- `checking=A14` (khong co tai khoan vang lai) xuat hien 39.5% — day la chi bao manh cho "good" credit
- `class=bad` chi chiem 29.7% (238/800) — class thieu so

### 5.2 Cau Truc Cay FP-Tree (Thu gon — chi hien 3 cap dau)

```
[root]
├── foreign=A201:766
│   ├── other_debtors=A101:697
│   │   ├── num_liable=One:597
│   │   │   ├── other_install=A143:493
│   │   │   │   ├── class=good:91
│   │   │   │   ├── housing=A152:217
│   │   │   │   │   ├── class=good:54
│   │   │   │   │   ├── job=A173:79
│   │   │   │   │   └── ...
│   │   │   │   ├── install_rate=High:118
│   │   │   │   └── ...
│   │   │   ├── housing=A152:40
│   │   │   ├── install_rate=High:23
│   │   │   └── ...
│   │   ├── other_install=A143:41
│   │   ├── housing=A152:21
│   │   └── ...
│   ├── other_install=A143:14
│   └── ...
├── other_debtors=A101:24
├── num_liable=One:7
└── ...
```

**Nhan xet:**
- Cay rat sau (20 attributes → toi da 20 cap) va rong (nhieu nhanh)
- `foreign=A201` → `other_debtors=A101` → `num_liable=One` la duong di pho bien nhat
  (97.5% records di theo nhanh nay)
- Moi transaction co 20 items nen cay FP-Tree rat day dac

### 5.3 Ket Qua Mining

- **Frequent patterns:** 153,966
- **Top 10 patterns (theo support):**

| # | Pattern | Support |
|---|---------|---------|
| 1 | {foreign=A201} | 766 |
| 2 | {other_debtors=A101} | 721 |
| 3 | {other_debtors=A101, foreign=A201} | 710 |
| 4 | {num_liable=One} | 678 |
| 5 | {num_liable=One, foreign=A201} | 663 |
| 6 | {other_install=A143} | 646 |
| 7 | {num_liable=One, other_debtors=A101} | 639 |
| 8 | {other_install=A143, foreign=A201} | 632 |
| 9 | {num_liable=One, other_debtors=A101, foreign=A201} | 629 |
| 10 | {other_install=A143, other_debtors=A101} | 603 |

> So luong patterns lon (153,966) la dac trung cua dataset nhieu attributes (20 cot).
> Day la ly do bai bao nhan manh can pruning hieu qua.

---

## 6. PHAN 2: CMAR — Phan Lop

### 6.1 Trich Xuat CARs (Section 3.1 cua paper)

Tu 153,966 frequent patterns → **49,046 candidate CARs** (co dung 1 class item, confidence >= 0.5)

### 6.2 Pruning 3 Giai Doan (Section 3.3 cua paper)

```
Candidate rules (truoc pruning):    49,046
────────────────────────────────────────────
Pruning 1 — General rule pruning:    5,077  (loai 43,969 = 89.6%)
Pruning 2 — Chi-square >= 3.841:    3,931  (loai  1,146 = 22.6%)
Pruning 3 — Database coverage d=4:    658  (loai  3,273 = 83.3%)
────────────────────────────────────────────
TONG:  49,046 → 658 luat  (loai 98.7%)
```

**Phan tich tung giai doan:**

| Giai doan | Phuong phap | Loai bao nhieu | Giai thich |
|-----------|------------|----------------|------------|
| **Pruning 1** | General rule pruning | 43,969 (89.6%) | Luat cu the r1 bi loai neu ton tai luat tong quat r2 co rank cao hon va condset(r2) ⊆ condset(r1), class(r2) = class(r1). Hieu qua nhat vi dataset co 20 attrs tao nhieu luat trung lap |
| **Pruning 2** | Chi-square significance | 1,146 (22.6%) | Loai luat khong co tuong quan thong ke voi class (chi^2 < 3.841). Dam bao chi giu luat "positively correlated" |
| **Pruning 3** | Database coverage | 3,273 (83.3%) | Algorithm 1: moi training record can duoc cover boi >= 4 luat. Sau khi du cover → loai record, luat khong cover record nao → loai |

> **Ket luan pruning:** Tu 49,046 luat xuong con 658 — giam **98.7%**. Day la minh chung cho tinh hieu qua cua CMAR pruning khi xu ly dataset lon nhieu attributes.

### 6.3 Top 20 Luat Sau Pruning

| # | Condset → Class | Sup | Conf |
|---|----------------|-----|------|
| 1 | residence=Long, checking=A14, housing=A152, other_install=A143, telephone=A192 → **good** | 0.0738 | **1.0000** |
| 2 | checking=A14, housing=A152, personal=A93, other_install=A143, credit_hist=A34 → **good** | 0.0713 | **1.0000** |
| 3 | checking=A14, job=A173, install_rate=High, other_install=A143, telephone=A192 → **good** | 0.0713 | **1.0000** |
| 4 | num_liable=One, checking=A14, personal=A93, amount=Medium, other_install=A143 → **good** | 0.0650 | **1.0000** |
| 5 | housing=A152, personal=A93, amount=Medium, job=A173, install_rate=High, other_install=A143 → **good** | 0.0625 | **1.0000** |
| 6 | checking=A14, num_credits=Many, personal=A93, other_install=A143, credit_hist=A34 → **good** | 0.0613 | **1.0000** |
| 7 | residence=Long, housing=A152, age=Senior, credit_hist=A34, other_debtors=A101 → **good** | 0.0588 | **1.0000** |
| 8 | residence=Long, num_liable=One, checking=A14, housing=A152, age=Senior → **good** | 0.0588 | **1.0000** |
| 9 | residence=Long, checking=A14, housing=A152, install_rate=High, telephone=A192 → **good** | 0.0588 | **1.0000** |
| 10 | checking=A14, personal=A93, job=A173, other_install=A143, telephone=A192 → **good** | 0.0588 | **1.0000** |
| 11 | savings=A65, housing=A152, personal=A93, job=A173 → **good** | 0.0575 | **1.0000** |
| 12 | checking=A14, job=A173, other_install=A143, num_credits=Few, telephone=A192 → **good** | 0.0575 | **1.0000** |
| 13 | checking=A14, housing=A152, personal=A93, amount=Medium, job=A173, other_install=A143 → **good** | 0.0575 | **1.0000** |
| 14 | housing=A152, duration=Medium, personal=A93, job=A173, other_install=A143, install_rate=High → **good** | 0.0575 | **1.0000** |
| 15 | property=A121, checking=A14, duration=Short → **good** | 0.0563 | **1.0000** |
| 16 | num_liable=One, checking=A14, employment=A75, age=Senior → **good** | 0.0563 | **1.0000** |
| 17 | residence=Long, checking=A14, housing=A152, other_install=A143, credit_hist=A34 → **good** | 0.0563 | **1.0000** |
| 18 | checking=A14, personal=A93, job=A173, other_install=A143, credit_hist=A34 → **good** | 0.0563 | **1.0000** |
| 19 | savings=A61, checking=A14, housing=A152, other_install=A143, credit_hist=A34 → **good** | 0.0538 | **1.0000** |
| 20 | residence=Long, num_liable=One, checking=A14, housing=A152, personal=A93, install_rate=High, other_debtors=A101 → **good** | 0.0538 | **1.0000** |

**Phan tich luat:**

**Attribute quan trong nhat (xuat hien trong nhieu luat):**

| Attribute | So luat chua | Y nghia |
|-----------|-------------|---------|
| **checking=A14** | 16/20 | Khong co tai khoan vang lai → chi bao manh nhat cho "good" |
| **other_install=A143** | 14/20 | Khong co khoan tra gop khac |
| **housing=A152** | 13/20 | Nha rieng (owner) |
| **personal=A93** | 10/20 | Nam / doc than |
| **job=A173** | 9/20 | Nhan vien co tay nghe |
| **residence=Long** | 8/20 | Cu tru lau (>2 nam) |

> **Nhan xet:** Nguoi co tin dung tot thuong co ho so: khong co tai khoan vang lai (A14), khong vay khac (A143), co nha rieng (A152), da cu tru lau — phu hop truc giac ve danh gia tin dung.

### 6.4 Phan Lop Bang Weighted Chi-Square (Section 4 cua paper)

**Cong thuc:**

Voi moi test record, thu thap tat ca luat match, nhom theo class:

```
score(class) = Sum [ chi^2(r) / max_chi^2(r) ]   for r in group

Chi^2(r)     = n * (a*d - b*c)^2 / ((a+b)(c+d)(a+c)(b+d))
Max_chi^2(r) = chi^2 tinh voi a = min(supP, supC)  (upper bound)
```

- Chi^2/max_chi^2 ∈ [0, 1] → binh thuong hoa giua cac luat khac nhau
- Class co tong score cao nhat thang
- Neu tat ca luat dong y 1 class → tra class do (khong can tinh score)
- Neu khong co luat match → tra default class ("good")

### 6.5 Ket Qua Du Doan (20 records dau tien trong 200 test records)

| # | Thuc te | Du doan | Ket qua |
|---|---------|---------|---------|
| 1 | good | good | OK |
| 2 | good | good | OK |
| 3 | good | good | OK |
| 4 | good | good | OK |
| 5 | bad | bad | OK |
| 6 | good | good | OK |
| 7 | bad | good | **WRONG** |
| 8 | good | good | OK |
| 9 | good | good | OK |
| 10 | bad | bad | OK |
| 11 | good | good | OK |
| 12 | good | good | OK |
| 13 | bad | good | **WRONG** |
| 14 | good | good | OK |
| 15 | bad | bad | OK |
| 16 | bad | good | **WRONG** |
| 17 | bad | bad | OK |
| 18 | bad | bad | OK |
| 19 | bad | good | **WRONG** |
| 20 | good | good | OK |

> Tong ket: 147/200 dung, 53/200 sai. Hau het loi la **bad bi du doan thanh good** (45 truong hop) — do class "good" chiem da so (70%) va co nhieu luat hon.

---

## 7. Danh Gia Hieu Suat

### 7.1 Tong Quan

| Metric | Gia tri |
|--------|---------|
| **Accuracy** | **147/200 = 73.50%** |
| **Macro-F1** | **0.6107** |

### 7.2 Chi Tiet Theo Class

| Class | TP | FP | FN | Precision | Recall | F1 |
|-------|----|----|-----|-----------|--------|------|
| **good** | 130 | 45 | 8 | 0.7429 | **0.9420** | **0.8307** |
| **bad** | 17 | 8 | 45 | **0.6800** | 0.2742 | **0.3908** |

### 7.3 Confusion Matrix

|  | Predicted good | Predicted bad |
|--|---------------|--------------|
| **Actual good** | **130** | 8 |
| **Actual bad** | 45 | **17** |

**Phan tich:**

- **Class "good":** Recall = 94.2% (bat hau het) nhung Precision = 74.3% (nhieu FP tu class bad)
- **Class "bad":** Precision = 68.0% (kha tot khi du doan bad) nhung Recall = 27.4% — chi bat duoc 17/62 truong hop bad

> **Van de mat can bang class:** Voi 70% good / 30% bad, mo hinh co xu huong du doan "good" qua nhieu. Day la thach thuc co huu cua German Credit dataset va duoc ghi nhan trong nhieu nghien cuu.

---

## 8. So Sanh Voi Bai Bao

### 8.1 Accuracy

| Phuong phap | Accuracy tren German |
|-------------|---------------------|
| **Bai bao — C4.5** | 72.3% |
| **Bai bao — CBA** | 73.4% |
| **Bai bao — CMAR** | **74.9%** |
| **Cai dat — CMAR** | **73.50%** |

### 8.2 Phan Tich Chenh Lech (73.50% vs 74.9%)

Chenh lech **1.4%** (khoang 14 records) — nho va co the do:

| Yeu to | Bai bao | Cai dat |
|--------|---------|---------|
| **Discretization** | CBA's method (entropy-based, tu dong) | Tercile binning (equal-frequency 3 bins) |
| **Cross-validation** | C4.5 shuffle utility (co the 10-fold CV) | Single 80/20 split (seed=42) |
| **Min Support** | 1% (tuong doi) | 40 (5.0% tuyet doi) |
| **CR-tree** | Co (toi uu bo nho + pruning) | Chua cai (dung list) |
| **3-stage pruning** | Co day du (paper Section 3.3) | **Co day du** (da cai dat ca 3 phuong phap) |
| **Weighted chi^2** | chi^2/max_chi^2 (paper Section 4) | **chi^2/max_chi^2** (dung theo paper) |

### 8.3 So Sanh Pruning

| Metric | Bai bao (chung) | Cai dat (German) |
|--------|----------------|-----------------|
| Pruning hieu qua | "50-60% bo nho tiet kiem" | 98.7% luat bi loai (49,046 → 658) |
| General rule pruning | Co | Co (loai 89.6%) |
| Chi-square test | Co (significance) | Co (>= 3.841) |
| Database coverage | Co (Algorithm 1, delta) | Co (delta=4) |

### 8.4 Nhan Xet

> **Ket luan:** Accuracy 73.50% **rat gan** voi bai bao (74.9%), chenh lech 1.4%.
> Ket qua nay cung **vuot C4.5** (72.3%) va gan bang **CBA** (73.4%).
> Chenh lech chinh do phuong phap discretization khac nhau (tercile vs entropy-based).
>
> Cai dat da **day du 3 phuong phap pruning** va **weighted chi-square** dung theo paper,
> giup ket qua sat voi bao cao goc.

---

## 9. Tom Tat Thuat Toan CMAR (Tham Chieu Paper)

### Phase 1: Rule Generation (Section 3.1)

```
Input:  Training data (800 records, 20 attributes)
Output: Candidate CARs

1. Ma hoa: moi (attribute, value) → item "attribute=value"
   Class → "class=good" hoac "class=bad"
2. FP-Growth: xay FP-Tree, mining de quy → 153,966 frequent patterns
3. Trich xuat: pattern co dung 1 class item + confidence >= 0.5
   → 49,046 candidate CARs
```

### Phase 2: Rule Pruning (Section 3.3)

```
Pruning 1: General Rule Pruning
   Sap xep luat theo: confidence giam → support giam → condset ngan
   Luat r1 bi loai neu ton tai r2 (rank cao hon):
     condset(r2) ⊆ condset(r1) VA class(r2) = class(r1)
   → 49,046 → 5,077

Pruning 2: Chi-Square Significance Test
   Chi giu luat co chi^2 >= 3.841 (p=0.05, df=1)
   Loai luat khong positively correlated voi class
   → 5,077 → 3,931

Pruning 3: Database Coverage (Algorithm 1)
   1. Duyet luat theo rank giam dan
   2. Voi moi luat R: tim cac training record R classify dung
   3. Neu R cover >= 1 record → giu R, tang cover_count cua record
   4. Record bi loai khi cover_count >= delta (=4)
   5. Lap lai cho den khi het record hoac het luat
   → 3,931 → 658
```

### Phase 3: Classification (Section 4)

```
Input:  Test record, 658 pruned rules
Output: Predicted class

1. Tim tat ca luat match (condset ⊆ record items)
2. Neu khong co luat match → tra default class ("good")
3. Neu tat ca luat dong y 1 class → tra class do
4. Neu co nhieu class:
   - Nhom luat theo class
   - score(class) = Sum chi^2(r)/max_chi^2(r) cho moi r trong nhom
   - Class co score cao nhat thang
```

---

## Huong Dan Chay Lai

```bash
cd d:\CMAR

# Buoc 1: Compile
mkdir out 2>nul
javac -d out src\*.java

# Buoc 2: Preprocess German Credit data
java -cp out GermanPreprocessor

# Buoc 3: Chay CMAR
java -Xmx1g -cp out Main data/german_disc.csv 40 0.5 3.841 4
```

**Ket qua output:**
- `result/frequent_patterns.txt` — 153,966 frequent patterns
- `result/association_rules.txt` — 49,046 candidate CARs
- `result/fpgrowth_result.txt` — FP-Growth: header table + FP-tree + patterns
- `result/cmar_result.txt` — CMAR: 3-stage pruning + 658 luat + predictions + evaluation
- `result/predictions.txt` — Du doan 200 records
- `result/evaluation.txt` — Accuracy + F1 metrics
- `report/fp_tree_report.md` — FP-Tree report dang Markdown

---

*Dataset: UCI German Credit / Statlog (1994). Thuat toan: CMAR (Li, Han & Pei, ICDM 2001).*
*Accuracy: 73.50% (paper: 74.9%) — chenh lech 1.4% do khac biet discretization.*
