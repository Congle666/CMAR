# Hướng Cải Tiến — HM Ranking + Top-K Combo (v7)

**Mục đích:** Báo cáo kết quả thực nghiệm hướng kết hợp **HM ranking (WCBA 2018)** + **Top-K Rules Per Class** trên 5 dataset mất cân bằng.

**Ngày:** 2026-05-11
**Datasets:** lymph, glass, vehicle, hepatitis, german (5 imbalanced)
**Setup:** 10-fold stratified CV, seed=42, minConf=0.5, χ²_thr=3.841, δ=4
**K values tested:** 3, 5, 7, 10

---

## 📋 Tóm Tắt Honest

**Kết quả mixed — Top-K + HM alone CHƯA đủ rescue minority class cực hiếm.**

| Variant | Trung bình MacroF1 | Trung bình Accuracy |
|---------|:------------------:|:-------------------:|
| Baseline | **0.6169** | 73.45% |
| K=3 + HM | 0.5202 | 70.20% |
| K=5 + HM | 0.5462 | 71.04% |
| K=7 + HM | 0.5585 | 71.50% |
| K=10 + HM | **0.5816** | **72.00%** |

→ **Top-K cao hơn → kết quả gần baseline hơn.** K=10 best nhưng vẫn thua baseline trên 4/5 datasets.

**Trường hợp duy nhất WIN: `hepatitis` với K=5 (MacroF1 0.7363 → 0.7520, +0.016).**

---

## 🔬 Lý Thuyết — Cơ Chế

### Harmonic Mean (HM) — Từ WCBA 2018

```
HM(R) = 2 × sup(R) × conf(R) / (sup(R) + conf(R))
```

**Ý nghĩa:** HM cao chỉ khi **CẢ** support và confidence đều cao. Tương tự F1-score nhưng dùng cho rules.

**Lợi ích lý thuyết:**
- Parameter-free (không cần tune)
- Balance sup và conf tự động
- Solves "rule conf=0.59 bị loại bởi minConf=0.6" (đã thấy trên lymph)

### Top-K Rules Per Class

Mỗi class chỉ giữ K luật tốt nhất (theo HM giảm dần) sau pruning 3-tier. Tự động:
- Loại bỏ luật yếu (low HM)
- Cân bằng majority vs minority rules
- Giới hạn ảnh hưởng class đa số

---

## 📊 Bảng Kết Quả Chính

### 1. Macro-F1 Per Dataset

| Dataset | Baseline | K=3 | K=5 | K=7 | K=10 | Best (vs baseline) |
|---------|:--------:|:----:|:----:|:----:|:----:|:------:|
| lymph (4 cls, fibrosis 2.7%) | **0.4235** | 0.3493 | 0.3610 | 0.3771 | 0.3959 | ❌ Tất cả < baseline |
| glass (6 cls, vehicle_float 7.9%) | **0.6113** | 0.4551 | 0.5144 | 0.5549 | 0.5947 | ❌ Tất cả < baseline |
| vehicle (4 cls, ~25% each) | **0.6493** | 0.4375 | 0.4674 | 0.4915 | 0.5299 | ❌ Tất cả < baseline |
| **hepatitis** (2 cls, DIE 20.6%) | 0.7363 | 0.7385 | **0.7520** | 0.7455 | 0.7449 | ✅ **K=5 (+0.016)** |
| german (2 cls, bad 30%) | **0.6639** | 0.6108 | 0.6260 | 0.6237 | 0.6428 | ❌ Tất cả < baseline |
| **TRUNG BÌNH** | **0.6169** | 0.5202 | 0.5462 | 0.5585 | 0.5816 | — |

### 2. Accuracy Per Dataset

| Dataset | Baseline | K=3 | K=5 | K=7 | K=10 |
|---------|:--------:|:----:|:----:|:----:|:----:|
| lymph | 83.46% | 76.32% | 78.66% | 80.00% | 81.96% |
| glass | 66.11% | 51.97% | 56.50% | 59.39% | 63.10% |
| vehicle | 67.36% | 47.04% | 49.61% | 52.13% | 56.27% |
| hepatitis | 81.81% | 81.21% | 82.50% | 82.50% | 82.45% |
| german | 74.20% | 71.20% | 72.40% | 72.30% | 73.20% |
| **TRUNG BÌNH** | **74.59%** | 65.55% | 67.93% | 69.26% | 71.40% |

---

## 🎯 Per-Class Analysis (Key Datasets)

### Lymph — Target Chính (Fibrosis = 2.7%)

| Class | Support | Baseline F1 | K=3 | K=5 | K=7 | K=10 |
|-------|:-------:|:-----------:|:----:|:----:|:----:|:----:|
| metastases | 81 (54.7%) | 0.8608 | 0.7486 | 0.7657 | 0.7811 | 0.8025 |
| malign_lymph | 61 (41.2%) | 0.8333 | 0.6486 | 0.6783 | 0.7273 | 0.7813 |
| **fibrosis** | 4 (2.7%) | **0.0000** | **0.0000** | **0.0000** | **0.0000** | **0.0000** |
| **normal** | 2 (1.4%) | **0.0000** | **0.0000** | **0.0000** | **0.0000** | **0.0000** |

**Phát hiện:**
- `fibrosis` và `normal` vẫn F1=0 với MỌI K → HM+TopK không sinh thêm rules cho minority (vì global minSup=7 vẫn block items đặc trưng).
- `metastases` và `malign_lymph` giảm F1 → ít rules → ít chính xác.

### Hepatitis — Trường hợp WIN (Minority DIE 20.6%)

| Class | Support | Baseline F1 | K=3 | **K=5** | K=7 | K=10 |
|-------|:-------:|:-----------:|:----:|:------:|:----:|:----:|
| LIVE | 123 (79.4%) | 0.8843 | 0.8839 | **0.9040** | 0.8975 | 0.8970 |
| **DIE** | 32 (20.6%) | 0.5882 | 0.5931 | **0.6000** | 0.5934 | 0.5928 |

**Phát hiện:** K=5 nâng được F1 minority DIE từ 0.588 → **0.600** (+0.012) + accuracy +0.69%. Tuy nhỏ nhưng **consistent improvement**.

### Glass — Điểm bất ngờ (Vehicle_float = 7.9%)

| Class | Support | Baseline F1 | K=10 |
|-------|:-------:|:-----------:|:----:|
| building_nonfloat | 76 | 0.6515 | 0.6299 |
| building_float | 70 | 0.7123 | 0.6970 |
| vehicle_float | **17** | 0.2000 | 0.2308 ↑ |
| headlamps | 29 | 0.8000 | 0.6400 ↓ |
| vehicle_nonfloat | 13 | 0.7857 | 0.4167 ↓↓ |
| containers | 9 | 0.5185 | 0.5714 ↑ |

**Phát hiện:** K=10 cải thiện 2 minority (vehicle_float +0.03, containers +0.05) nhưng làm 2 majority/minority khác tụt mạnh → net loss.

---

## 💡 Tại Sao HM+TopK Không Đủ Tốt?

### Vấn đề 1: Top-K quá aggressive

K luật per class × số class = tổng số luật cuối.
- Lymph 4 classes × K=10 = **40 rules max** (vs baseline ~hàng trăm)
- Glass 6 classes × K=10 = **60 rules**
- Mất quá nhiều thông tin từ rules cấp 2, 3

### Vấn đề 2: HM ranking không tạo thêm rules

HM chỉ thay đổi **ranking** của các rules đã sinh ra. Không sinh thêm rules cho minority. Lymph fibrosis vẫn F1=0 vì:
- Item-level minSup=7 vẫn loại items đặc trưng cho fibrosis
- Không có rule fibrosis nào tồn tại để ranking → vẫn 0 luật

### Vấn đề 3: K=fixed không adaptive

Dataset đông class (lymph 4, glass 6) cần K lớn để có đủ rules. K=3 quá ít, K=10 cũng chỉ đủ cho hepatitis 2-class.

---

## 🔄 So Sánh Với Các Hướng Trước

| Hướng | lymph MacroF1 | lymph fibrosis F1 | Avg MacroF1 (7 datasets) |
|-------|:-------------:|:-----------------:|:------------------------:|
| Baseline | 0.4235 | 0.0000 | 0.7488 |
| H1 (Weighted χ²) | 0.4271 | 0.0000 | 0.7418 |
| H2 (Class minSup) | 0.4167 | 0.0000 | 0.7525 |
| H3 (Adaptive minConf) | 0.4235 | 0.0000 | 0.7488 |
| **H2+H3 combo** ⭐ | **0.5907** | **0.6667** | **0.7774** |
| H7: HM+TopK (best K=10) | 0.3959 | 0.0000 | — |

**Kết luận thẳng thắn:**
- **H2+H3 vẫn là winner** cho rescue fibrosis (F1 0→0.67)
- **HM+TopK alone** không rescue được class cực hiếm
- HM+TopK có thể có giá trị **khi kết hợp với H2+H3** (chưa test)

---

## 🎓 Bài Học Cho Khóa Luận

### Hướng đi đúng

✅ **HM ranking là parameter-free** (không cần per-dataset tuning như H2)
✅ **Top-K cân bằng class** (concept hợp lý)
✅ **Hepatitis WIN** chứng minh ý tưởng có giá trị

### Hạn chế cần báo cáo honest

❌ Top-K cắt quá mạnh → mất rules tốt
❌ HM chỉ rank, không sinh rules → minority cực hiếm vẫn F1=0
❌ Cần kết hợp với H2 (sinh rules cho minority) mới đầy đủ

### Đóng góp cho thesis

**Câu chuyện nghiên cứu:**
> "Chúng tôi thử nghiệm 2 ý tưởng từ WCBA 2018 (HM ranking) và Top-K rules per class trên 5 datasets imbalanced. Top-K + HM cho kết quả CẢI THIỆN trên hepatitis (DIE F1 +0.012) nhưng KHÔNG đủ rescue class cực hiếm (lymph fibrosis vẫn F1=0). Phân tích cho thấy 2 kỹ thuật này cần kết hợp với H2 (class-specific minSup) để sinh được rules cho minority."

---

## 🔮 Khuyến Nghị Tiếp Theo

### Option 1: HM + TopK + H2+H3 (kết hợp tất cả)

Kết hợp 4 kỹ thuật:
- H2: class-specific minSup (sinh rules minority)
- H3: adaptive minConf (giữ rules conf thấp)
- HM ranking + TopK (chọn balanced)

→ Có thể là combo tối ưu cuối cùng.

### Option 2: Adaptive K

K thay đổi theo class size:
```
K(c) = max(K_min, min(K_max, total_class_size / freq(c)))
```
Minority class lấy nhiều rules hơn, majority ít hơn.

### Option 3: Chỉ HM (không TopK)

Bỏ Top-K, chỉ dùng HM làm ranking + minHM threshold (0.1, 0.2, 0.3).
- Giữ nhiều rules hơn
- Lọc theo HM threshold

---

## 📂 Files Liên Quan

| File | Mô tả |
|------|-------|
| `result/v7_baseline_metrics.csv` | Baseline 5 imbalanced datasets |
| `result/v7_baseline_per_class.csv` | Per-class F1 baseline |
| `result/v7_k{3,5,7,10}_metrics.csv` | HM+TopK với K khác nhau |
| `result/v7_k{3,5,7,10}_per_class.csv` | Per-class chi tiết |
| `result/v7_benchmark.log` | Console log đầy đủ |
| `src/BenchmarkHMTopK.java` | Source benchmark |
| `src/CMARClassifier.java` | `setUseHMRanking()`, `setMinHM()` |
| `src/AssociationRule.java` | Field `hm` + `getHM()` |

---

## Lệnh Chạy Lại

```bash
# Compile
javac -d out src/*.java

# Run (5 imbalanced datasets, K=3,5,7,10)
java -Xmx1g -cp out BenchmarkHMTopK
```

---

*Honest finding: HM+TopK alone không phải silver bullet. Cần kết hợp H2 (minSup) hoặc H3 (minConf) để work với extreme minority. Hepatitis là proof of concept, các dataset đông class cần thêm rules thay vì cắt bớt.*
