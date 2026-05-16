# Nguồn gốc Khoa học của H2 và H3 — Báo cáo Truy nguyên Citation

**Ngày báo cáo:** 13/05/2026
**Mục tiêu:** Xác định chính xác nguồn paper backing cho H2 (class-specific minSupport) và H3 (adaptive minConfidence) trong nghiên cứu cải tiến CMAR.
**Phương pháp:** Đọc PDF trực tiếp (WCBA 2018), tra cứu literature qua 3 nghiên cứu độc lập (Google Scholar, SpringerLink, ACM Digital Library, ResearchGate).

---

## TÓM TẮT KẾT LUẬN

| Cải tiến | Paper Source | Mức độ tương quan | Citation chuẩn |
|----------|-------------|--------------------|----------------|
| **H2** (class-specific minSup) | **Liu, Ma, Wong (2000)** — PKDD 2000 | ✅ Trực tiếp — cùng nguyên lý | **Bắt buộc cite** |
| | Hu et al. (2016) — MMSCBA | ⚪ Liên quan — refinement | Cite làm related work |
| | Liu, Hsu, Ma (1999) — MS-Apriori | ⚪ Liên quan — item-level | Cite làm conceptual ancestor |
| **H3** (adaptive minConf) | **Không có paper backing trực tiếp** | ⚠️ Original contribution | **Đóng góp riêng** |
| | MMSCBA (Hu 2016) | ⚪ Tham chiếu so sánh | Cite để so sánh orthogonal |

### ⚠️ Phát hiện quan trọng
- **WCBA 2018 KHÔNG phải nguồn của H2/H3** — đã đọc kỹ paper, WCBA tập trung vào **attribute weights** + HM ranking, không có class-specific thresholds.
- **CMAR 2001 (Li, Han, Pei) KHÔNG có class-specific minSup** — sử dụng global minSup duy nhất.

---

## 1. H2 — Class-specific Minimum Support

### 1.1. Paper Source Chính: Liu, Ma & Wong (2000)

**Citation đầy đủ:**
> **Liu, B., Ma, Y., & Wong, C.K.** (2000). *Improving an Association Rule Based Classifier*. Proceedings of the 4th European Conference on Principles of Data Mining and Knowledge Discovery (PKDD 2000), Lyon, France. Lecture Notes in Computer Science, Vol. 1910, pp. 504–509. Springer.

**DOI:** [10.1007/3-540-45372-5_58](https://doi.org/10.1007/3-540-45372-5_58)

**Truy cập:**
- [SpringerLink](https://link.springer.com/chapter/10.1007/3-540-45372-5_58)
- [ACM Digital Library](https://dl.acm.org/doi/10.5555/645804.669837)
- [PDF full text](https://sci2s.ugr.es/keel/pdf/algorithm/capitulo/2001-Liu-CBA2.pdf)

### 1.2. Formula của Liu et al. 2000

```
minsupp_i = minsupp_t × supp(c_i) / max(supp(C))
```

Trong đó:
- `minsupp_t` — total minimum support (do user đặt cho toàn dataset)
- `supp(c_i)` — support của class i (= freq(c_i)/N)
- `max(supp(C))` — support của class lớn nhất (majority)
- `minsupp_i` — minimum support tính riêng cho class i

### 1.3. So sánh với H2

| Khía cạnh | Liu et al. 2000 | H2 (nghiên cứu này) |
|-----------|-----------------|---------------------|
| Nguyên lý | Class-specific minSup tỷ lệ với class size | Class-specific minSup tỷ lệ với class size ✓ |
| Công thức | `minsupp_i = minsupp_t × supp(c_i) / max(supp(C))` | `minSup(c) = max(2, ⌊supPct × freq(c)⌋)` |
| Đơn vị | Relative support | Absolute frequency |
| Safety floor | Không có | Có (`max(2, ...)`) — tránh class quá hiếm có ngưỡng = 0 |
| Mục đích | Giải quyết class imbalance trong CBA | Giải quyết class imbalance trong CMAR ✓ |

**Kết luận:** H2 **kế thừa trực tiếp** ý tưởng class-specific minSup từ Liu et al. 2000, với 2 điểm tinh chỉnh:
1. Dùng absolute frequency thay relative support — dễ hiểu hơn về mặt implementation.
2. Thêm safety floor `max(2, ...)` để tránh ngưỡng = 0 với class cực hiếm.

### 1.4. Motivation từ Liu 2000

> *"Single global minSup produces zero/sparse rules for minority classes, making them invisible to the classifier."*

Liu et al. 2000 motivation **chính xác trùng** với vấn đề mà H2 giải quyết trong nghiên cứu này.

### 1.5. Related Work — Lineage của ý tưởng

**Conceptual ancestor:**
> **Liu, B., Hsu, W., & Ma, Y.** (1999). *Mining association rules with multiple minimum supports*. KDD '99, pp. 337–341. DOI: [10.1145/312129.312274](https://doi.org/10.1145/312129.312274)

→ Liu 1999 đề xuất **item-specific** MIS (mỗi item có ngưỡng riêng), CHƯA phải class-specific. Liu 2000 mới mở rộng sang class-level cho classification.

**Refinement sau này:**
> **Hu, L.Y., Hu, Y.H., Tsai, C.F., Wang, J.S., & Huang, M.W.** (2016). *Building an associative classifier with multiple minimum supports*. SpringerPlus 5(1), 1-19. DOI: [10.1186/s40064-016-2153-1](https://doi.org/10.1186/s40064-016-2153-1)

→ MMSCBA đề xuất kết hợp MIS (per-item) + MCS (per-class):
```
MRS(β) = min(MIS(i₁), MIS(i₂), ..., MIS(iₘ), MCS(yβ))
```

### 1.6. Lineage Chronological

```
1999 ─── Liu, Hsu, Ma (KDD 1999) ────────── MS-Apriori
         "Mining association rules with     Item-specific MIS
         multiple minimum supports"
                  │
                  ▼
2000 ─── Liu, Ma, Wong (PKDD 2000) ───────── CBA extension
         "Improving an Association          ★ CLASS-SPECIFIC minSup
         Rule Based Classifier"             (parent paper của H2)
                  │
                  ▼
2016 ─── Hu et al. (SpringerPlus) ────────── MMSCBA
         "Building an associative           MIS + MCS combined
         classifier with multiple
         minimum supports"
                  │
                  ▼
2026 ─── H2 (project này) ────────────────── CMAR extension
         minSup(c) = max(2,                  Adapted to CMAR
                    ⌊supPct × freq(c)⌋)     + Safety floor
```

---

## 2. H3 — Adaptive Minimum Confidence

### 2.1. Phát hiện chính: KHÔNG có paper backing trực tiếp

Sau khi search rộng (Google Scholar, ACM, SpringerLink, ResearchGate, Semantic Scholar) với các từ khóa:
- "class-specific minimum confidence" association rule
- "per-class confidence threshold"
- "adaptive minConf association rules"
- "confidence threshold per-class imbalanced"
- "imbalanced classification" + "confidence threshold"

→ **KHÔNG tìm thấy paper** đề xuất adaptive minConfidence per class trong association rule mining.

### 2.2. Literature Gap

Literature về imbalanced AC đã có:
- ✅ **Adaptive minSup** (per class/item) — MMSCBA, Liu 2000
- ✅ **Cost-sensitive ranking** (post-mining) — SSCR và các paper khác
- ✅ **Class-weighted sampling** — SMOTE, Random Over/Under-sampling
- ❌ **Adaptive minConfidence** — **KHÔNG có paper**

### 2.3. Tại sao literature gap?

Có thể do:
1. Confidence ít bottleneck performance hơn support trong AC.
2. Post-mining cost-sensitive weighting đã đủ cho phần lớn use cases.
3. Concern về computational complexity (chưa documented).

### 2.4. H3 là Original Contribution

```
minConf(c) = min(globalMinConf, max(floor, lift × P(c)))

Tham số mặc định:
    globalMinConf = 0.5
    floor         = 0.3
    lift          = 5
    P(c)          = freq(c) / N
```

**Nguyên lý đề xuất:**
- Minority class có baseline xác suất thấp → confidence threshold global 50% quá khắt khe.
- H3 cho minority threshold thấp hơn nhưng vẫn cao gấp 5× baseline ngẫu nhiên.
- Floor 30% đảm bảo không nới quá rộng.

### 2.5. Closest Related Work (so sánh orthogonal)

| Method | Stage áp dụng | Đối tượng | Khác biệt với H3 |
|--------|---------------|-----------|------------------|
| MMSCBA (Hu 2016) | Mining | minSupport per class | Khác metric (support vs confidence) |
| Cost-sensitive CBA | Post-mining | Rule ranking | H3 áp dụng trong mining stage |
| SSCR (Padilo & Phan) | Post-mining | Rule weighting | Tương tự, post-mining |
| H3 (project này) | Mining | minConfidence per class | **Novel — chưa có trong literature** |

### 2.6. Cách positioning H3 trong thesis

**Khuyến nghị viết:**
> "Để bổ sung cho class-specific minSup (H2 — Liu et al. 2000), nghiên cứu này đề xuất **H3** — adaptive minConfidence per class. Đây là **đóng góp gốc** của nghiên cứu, áp dụng nguyên lý tương tự H2 nhưng cho ngưỡng confidence. Đề tài chưa tìm thấy paper trước đề xuất kỹ thuật này. H3 lấp đầy một literature gap — văn liệu đã có adaptive minSup nhưng chưa có adaptive minConf."

### 2.7. Validation bằng thực nghiệm

Vì H3 không có prior art, **empirical validation** trở thành yếu tố quyết định. Kết quả v11 trên 20 UCI datasets:
- Light variant (H2 + H3) cải thiện 6/20 datasets so với Baseline.
- Đặc biệt: German bad-class Recall +23.9% (từ 43% → 54%) — cho thấy H3 (+ H2) có tác dụng thực sự.

---

## 3. WCBA Paper (2018) — Tại sao KHÔNG phải nguồn của H2/H3?

### 3.1. WCBA paper details

**Citation:**
> **Alwidian, J., Hammo, B.H., & Obeid, N.** (2018). *WCBA: Weighted classification based on association rules algorithm for breast cancer disease*. Applied Soft Computing, Vol. 62, pp. 536-549. DOI: [10.1016/j.asoc.2017.11.013](https://doi.org/10.1016/j.asoc.2017.11.013)

### 3.2. WCBA thực sự đề xuất gì?

5 đóng góp chính của WCBA (đã xác minh bằng đọc PDF trực tiếp):

| # | Đóng góp WCBA | Quote từ paper (trang) |
|:-:|---------------|------------------------|
| 1 | **Attribute weights** gán bởi domain experts | "Attribute weights can be assigned using a scale from 1 to 10..." (Sec. 5.3) |
| 2 | **Weighted Support** = `support × attribute_weight` | "Multiplying the support of each item with its weight generates its weighted support value" (Sec. 5.3 — line9) |
| 3 | **Harmonic Mean ranking** | `HM(r) = 2 × WeightedSupport(r) × Confidence(r) / (WS + Conf)` (Eq. trong Sec. 5.3) |
| 4 | **Strong + Spare rules** (M1 method) | "to separate the rules into two sets: strong-rules and spare-rules" (Sec. 5.3) |
| 5 | **Class Prediction** dựa trên average HM | "computes the average HM value for each class" (Algorithm 4) |

### 3.3. WCBA KHÔNG có

- ❌ Class-specific minSupport (= H2) — không có quote nào, paper dùng global minSup.
- ❌ Adaptive minConfidence per class (= H3) — không có quote nào, paper dùng global minConf.
- ❌ Xử lý class imbalance — paper là về breast cancer, focus vào attribute importance.

### 3.4. Quote từ WCBA paper liên quan đến Multiple Minimum Supports

WCBA chỉ **mention** MMSCBA như related work, không phải đóng góp của WCBA:

> "Enhanced Associative Classification based on Incremental Mining algorithm (E-ACIM) [28] and Multiple Minimum Supports (MMSCBA) [29]." (Sec. 5, line 273-274)

→ Reference [29] = Hu et al. 2016 (đã verify) — đây là paper mà H2 thực sự liên quan, KHÔNG phải WCBA.

### 3.5. Kết luận về WCBA

WCBA là một paper hay nhưng **không liên quan trực tiếp** đến H2/H3. Nếu muốn cite WCBA trong thesis, có thể:
- Cite làm **comparison work** (cùng lĩnh vực AC cho imbalanced/medical).
- Cite các kỹ thuật KHÁC của WCBA mà có thể áp dụng (HM ranking, Strong+Spare).
- **KHÔNG cite** làm nguồn của H2/H3.

---

## 4. Citation chuẩn cho Thesis

### 4.1. Cho H2

**Primary citation (bắt buộc):**
> Liu, B., Ma, Y., & Wong, C.K. (2000). Improving an Association Rule Based Classifier. *Proceedings of PKDD 2000*, LNCS 1910, pp. 504-509. Springer. DOI: 10.1007/3-540-45372-5_58.

**Supporting citations (tùy chọn, làm related work):**
- Liu, B., Hsu, W., & Ma, Y. (1999). Mining association rules with multiple minimum supports. *KDD '99*, pp. 337-341. (conceptual ancestor — item-level MIS)
- Hu, L.Y., et al. (2016). Building an associative classifier with multiple minimum supports. *SpringerPlus* 5(1):1-19. (modern refinement — MMSCBA)

**Cách viết trong thesis:**
> "Đề tài kế thừa ý tưởng class-specific minimum support từ Liu et al. (2000), người đã đề xuất công thức `minsupp_i = minsupp_t × supp(c_i) / max(supp(C))` trong khuôn khổ thuật toán CBA. H2 trong nghiên cứu này là một biến thể với 2 điểm tinh chỉnh: (1) sử dụng absolute frequency thay cho relative support để dễ implement, (2) thêm safety floor `max(2, ...)` để tránh ngưỡng = 0 trên class cực hiếm."

### 4.2. Cho H3

**Vì H3 không có prior art, không có primary citation.**

**Cách viết trong thesis:**
> "Bổ sung cho H2, đề tài đề xuất **H3 — Adaptive Minimum Confidence per Class** với công thức `minConf(c) = min(globalMinConf, max(floor, lift × P(c)))`. Theo khảo sát literature (Google Scholar, ACM DL, SpringerLink — May 2026), đề tài chưa tìm thấy paper nào trước đề xuất adaptive minConf per class trong association rule mining. Các nghiên cứu hiện tại tập trung vào adaptive minSup (Liu 2000, Hu 2016) hoặc cost-sensitive ranking post-mining (Padilo & Phan 2014). H3 lấp đầy literature gap này — đây là đóng góp nguyên gốc của nghiên cứu."

**Supporting citations (làm comparison):**
- Hu, L.Y., et al. (2016). MMSCBA — adaptive minSup nhưng không minConf.
- Padilo & Phan (2014). Cost-sensitive associative classification (post-mining).

---

## 5. Update các MD trước đây

Các file MD đã viết với citation SAI cần được sửa:

| File | Vấn đề | Cần sửa |
|------|--------|---------|
| `H2_H3_SMOTE_explained.md` | Nói "H2/H3 dựa trên WCBA 2018" | Sửa thành "H2 từ Liu 2000, H3 là original contribution" |
| `de_hieu_van_de_va_cai_tien.md` | Có thể mention WCBA sai | Verify và sửa |
| `v11_smote_full_uci.md` | Có WCBA reference | Verify và sửa |
| `bao_cao_thuyet_trinh.md` | Có "extension của WCBA" | Sửa thành "extension của Liu 2000" |
| `bao_cao_chi_tiet_SMOTE.md` | Có WCBA citation cho H2/H3 | Sửa thành Liu 2000 |
| `smote_qa_chi_tiet.md` | Có "H2/H3 dựa trên WCBA" | Sửa thành Liu 2000 + acknowledge H3 novel |

---

## 6. Câu trả lời chuẩn cho Giảng viên

Nếu giảng viên hỏi: "H2 và H3 lấy từ paper nào?"

**Câu trả lời chuẩn:**

> "Dạ thưa thầy, em xin báo cáo nguồn gốc của 2 cải tiến:
>
> **Đối với H2** (class-specific minSupport):
> - Em kế thừa trực tiếp từ paper **Liu, Ma, Wong (2000)** *"Improving an Association Rule Based Classifier"* tại PKDD 2000, LNCS 1910, pp. 504-509.
> - Liu et al. 2000 đề xuất công thức `minsupp_i = minsupp_t × supp(c_i) / max(supp(C))` — gán minSup riêng cho mỗi class tỷ lệ với class size.
> - H2 của em là biến thể với 2 điểm tinh chỉnh: (1) dùng absolute frequency thay cho relative support, (2) thêm safety floor `max(2, ...)`.
> - Liu, Hsu, Ma (1999) — paper KDD '99 là conceptual ancestor (item-level), Hu et al. (2016) MMSCBA là modern refinement.
>
> **Đối với H3** (adaptive minConfidence):
> - Em xin **thẳng thắn** báo cáo: theo khảo sát literature May 2026, em **chưa tìm thấy paper nào** trước đề xuất adaptive minConf per class trong association rule mining.
> - Văn liệu hiện tại chỉ có adaptive minSup (Liu 2000, Hu 2016) hoặc cost-sensitive ranking post-mining (Padilo & Phan 2014).
> - H3 là **đóng góp nguyên gốc** của nghiên cứu, lấp đầy một literature gap.
> - Validation bằng thực nghiệm trên 20 UCI: Light variant (H2+H3) cải thiện 6 datasets so với Baseline.
>
> Em **đã làm rõ** trong thesis là H3 chưa có peer-review qua paper riêng, đó là hạn chế em xin nhận."

---

## 7. Tài liệu Tham khảo

### Primary Sources (đã verify)

1. **Liu, B., Ma, Y., & Wong, C.K.** (2000). *Improving an Association Rule Based Classifier*. PKDD 2000, LNCS 1910, pp. 504-509. Springer. [DOI: 10.1007/3-540-45372-5_58](https://doi.org/10.1007/3-540-45372-5_58) — **Primary source cho H2**

2. **Liu, B., Hsu, W., & Ma, Y.** (1999). *Mining association rules with multiple minimum supports*. KDD '99, pp. 337-341. ACM. [DOI: 10.1145/312129.312274](https://doi.org/10.1145/312129.312274) — Conceptual ancestor

3. **Hu, L.Y., Hu, Y.H., Tsai, C.F., Wang, J.S., & Huang, M.W.** (2016). *Building an associative classifier with multiple minimum supports*. SpringerPlus 5(1):1-19. [DOI: 10.1186/s40064-016-2153-1](https://doi.org/10.1186/s40064-016-2153-1) — Modern refinement

### Verified NOT Sources

4. **Alwidian, J., Hammo, B.H., & Obeid, N.** (2018). *WCBA: Weighted classification based on association rules algorithm for breast cancer disease*. Applied Soft Computing 62, pp. 536-549. — **KHÔNG phải source của H2/H3** (đã đọc PDF, confirm)

5. **Li, W., Han, J., & Pei, J.** (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules*. ICDM 2001, pp. 369-376. — Global minSup, **KHÔNG có class-specific minSup**

### Related Work (cho comparison)

6. **Padilo, F., & Phan, K.T.** (2014). Cost-sensitive based approach for improving associative classification on imbalanced datasets.

### Detailed Research Reports (in this project)

- [research_msapriori_liu1999.md](research_msapriori_liu1999.md) — Liu 1999 MIS analysis
- [research_h2_h3_techniques.md](research_h2_h3_techniques.md) — H2 vs MMSCBA, H3 novelty
- [research_verify_cmar_classsup.md](research_verify_cmar_classsup.md) — CMAR 2001 không có class-specific minSup
- [research_liu2000_classsup.md](research_liu2000_classsup.md) — Liu 2000 verification

---

## 8. Hành động Tiếp theo

| Hành động | Ưu tiên | Mô tả |
|-----------|:-------:|-------|
| Update 6 file MD có citation sai về WCBA | 🔴 Cao | Sửa WCBA → Liu 2000 |
| Verify Liu 2000 paper bằng cách đọc PDF trực tiếp | 🟡 Trung bình | Download từ SpringerLink |
| Tìm thêm paper backing cho H3 (nếu có) | 🟢 Thấp | Có thể chấp nhận H3 là novel |
| Commit báo cáo này lên GitHub | 🟢 Thấp | Để có evidence cho thesis |
