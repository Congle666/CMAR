# GIẢI THÍCH SÂU CÁC CÔNG THỨC TRONG CMAR

**Hiểu rõ: Công thức làm gì? Tại sao cần? Tác dụng như thế nào?**

> File này đồng hành với [bao_cao_chi_tiet_CMAR.md](bao_cao_chi_tiet_CMAR.md) — thay vì chỉ liệt kê công thức, file này giải thích **tại sao** mỗi công thức tồn tại, **câu hỏi** nó trả lời, và **vai trò** của nó trong thuật toán.

---

## Mục Lục

1. [Vì sao cần nhiều công thức như vậy?](#1-vì-sao-cần-nhiều-công-thức-như-vậy)
2. [Support — Công thức "đếm tần suất"](#2-support--công-thức-đếm-tần-suất)
3. [Confidence — Công thức "xác suất có điều kiện"](#3-confidence--công-thức-xác-suất-có-điều-kiện)
4. [Lift — Công thức "đo độ tăng"](#4-lift--công-thức-đo-độ-tăng)
5. [Leverage — Công thức "chênh lệch quan sát vs kỳ vọng"](#5-leverage--công-thức-chênh-lệch-quan-sát-vs-kỳ-vọng)
6. [Conviction — Công thức "đo sức mạnh suy diễn"](#6-conviction--công-thức-đo-sức-mạnh-suy-diễn)
7. [Chi-square (χ²) — Công thức "kiểm định thống kê"](#7-chi-square-χ²--công-thức-kiểm-định-thống-kê)
8. [Max Chi-square — Công thức "chuẩn hoá về [0,1]"](#8-max-chi-square--công-thức-chuẩn-hoá-về-01)
9. [Weighted χ² Score — Công thức "bỏ phiếu có trọng số"](#9-weighted-χ²-score--công-thức-bỏ-phiếu-có-trọng-số)
10. [Rule Precedence — Công thức "xếp hạng luật"](#10-rule-precedence--công-thức-xếp-hạng-luật)
11. [Các điều kiện Pruning — Công thức cắt tỉa](#11-các-điều-kiện-pruning--công-thức-cắt-tỉa)
12. [Precision, Recall, F1 — Công thức đánh giá kết quả](#12-precision-recall-f1--công-thức-đánh-giá-kết-quả)
13. [Bảng Tổng Kết — Công Thức Nào Dùng Ở Đâu](#13-bảng-tổng-kết--công-thức-nào-dùng-ở-đâu)

---

## 1. Vì sao cần nhiều công thức như vậy?

Một câu hỏi tự nhiên khi đọc bài báo CMAR: **"Sao phải dùng tới support, confidence, lift, χ², maxχ², weighted χ² — dùng 1 công thức có được không?"**

**Câu trả lời ngắn:** Mỗi công thức trả lời một **câu hỏi khác nhau** về luật. Thiếu một công thức → luật dễ bị đánh giá sai.

**Bảng so sánh câu hỏi mỗi công thức trả lời:**

| Công thức | Câu hỏi trả lời |
|-----------|------------------|
| **Support** | "Luật có đủ phổ biến không?" (có đáng xem xét không?) |
| **Confidence** | "Khi khớp vế trái, xác suất khớp vế phải là bao nhiêu?" |
| **Lift** | "Vế trái có làm TĂNG cơ hội vế phải so với ngẫu nhiên không?" |
| **Leverage** | "Vế trái và vế phải xuất hiện cùng nhau nhiều hơn kỳ vọng bao nhiêu?" |
| **Conviction** | "Luật chắc chắn như thế nào — nếu sai thì sai bao nhiêu?" |
| **χ²** | "Mối liên hệ giữa 2 vế có phải ngẫu nhiên không?" (ý nghĩa thống kê) |
| **max χ²** | "Giá trị χ² lý thuyết tối đa của luật này?" (dùng chuẩn hoá) |
| **χ²/maxχ²** | "Luật này mạnh đến MỨC NÀO trên thang [0, 1]?" |
| **Weighted χ²** | "Khi nhiều luật mâu thuẫn, nhóm nào thắng?" |

**Ẩn dụ:** Tưởng tượng bạn chọn món ăn ở nhà hàng:
- **Support** = "Có bao nhiêu người đã ăn món này?" (có phổ biến không)
- **Confidence** = "Trong người ăn món này, bao nhiêu phần trăm khen ngon?"
- **Lift** = "Món này có NGON HƠN món trung bình không?"
- **χ²** = "Nhận xét 'ngon' có đủ tin cậy, hay chỉ có vài người review?"

Dùng một công thức duy nhất sẽ **bỏ sót thông tin quan trọng**.

---

## 2. Support — Công thức "đếm tần suất"

### 2.1 Công thức

$$
\text{sup}(R: P \Rightarrow c) = \big|\{t \in T : P \subseteq t \wedge \text{class}(t) = c\}\big|
$$

$$
\sigma(R) = \frac{\text{sup}(R)}{|T|}
$$

### 2.2 Câu hỏi nó trả lời

> **"Có bao nhiêu bản ghi ủng hộ luật này?"**

### 2.3 Tác dụng chính

**Support trả lời câu hỏi về độ PHỔ BIẾN:**
- Support cao → luật áp dụng cho nhiều bản ghi → đáng học.
- Support thấp → luật chỉ áp dụng cho vài trường hợp → có thể là **nhiễu** hoặc **trường hợp đặc biệt**.

### 2.4 Tại sao cần?

**Vấn đề nếu không có support:** Nếu chỉ xét confidence, một luật chỉ khớp với **1 bản ghi** cũng có conf = 100% → nhưng đây chỉ là **trùng hợp ngẫu nhiên**, không đáng tin.

**Ví dụ cảnh báo:**
> Dataset 1000 bản ghi. Luật $R: \{X\} \Rightarrow c$ chỉ khớp **1 record** và record đó có class c → conf = 100% nhưng sup = 1. Liệu có dám dùng luật này?

→ Support đóng vai trò **cổng chặn** — loại bỏ luật "một phát ăn may".

### 2.5 Trực giác về giá trị

| Support (tương đối) | Ý nghĩa |
|:------------------:|--------|
| 50%+ | Rất phổ biến (luật "đại chúng") |
| 10–50% | Phổ biến vừa |
| 1–10% | Hiếm nhưng có cơ sở |
| <1% | Quá hiếm, dễ ngẫu nhiên |

### 2.6 Tác dụng trong CMAR

| Nơi dùng | Tác dụng |
|----------|----------|
| Bước 1 (đếm item) | Lọc item dưới minSup ngay từ đầu — giảm kích thước cây FP |
| Bước 2 (xây FP-tree) | Chỉ giữ item có sup ≥ minSup → cây gọn hơn |
| Bước 4 (sinh CAR) | Luật nào sup < minSup → bỏ |
| Bước 7 (tính χ²) | Support dùng để tính các ô của bảng contingency |

### 2.7 Hạn chế

**Support CÀNG giảm → số pattern CÀNG BÙNG NỔ.** Ví dụ:
- minSup = 10% → 100 patterns
- minSup = 1% → 10,000 patterns
- minSup = 0.1% → có thể 1,000,000 patterns

Đó là lý do cần cân bằng: minSup quá thấp → không chạy nổi; quá cao → mất luật quan trọng cho minority class.

### 2.8 Ví dụ cụ thể

Dataset 100 bản ghi, class A = 40, class B = 60.

- Luật $\{X\} \Rightarrow A$ có sup = 30 → 30 bản ghi khớp X **và** có class A.
- $\sigma = 30/100 = 30\%$ — khá phổ biến.
- Nếu minSup = 10% (abs 10) → giữ.
- Nếu minSup = 50% → loại.

---

## 3. Confidence — Công thức "xác suất có điều kiện"

### 3.1 Công thức

$$
\text{conf}(R: P \Rightarrow c) = \frac{\text{sup}(P \cup \{c\})}{\text{sup}(P)} = \frac{\#\text{khớp cả P và c}}{\#\text{khớp P}}
$$

### 3.2 Câu hỏi nó trả lời

> **"Khi bản ghi đã khớp vế trái, xác suất có lớp c là bao nhiêu?"**

### 3.3 Tác dụng chính

Confidence **chính là xác suất có điều kiện** — công cụ chính thức nhất để đo "độ tin cậy" của dự đoán:
$$
\text{conf}(R) = \hat{P}(c \mid P)
$$

### 3.4 Tại sao cần?

**Vấn đề nếu không có confidence:** Chỉ có support, ta biết "có bao nhiêu bản ghi thoả luật", nhưng **không biết luật có đúng hay không**.

**Ví dụ:**
> Luật $\{X\} \Rightarrow A$ có sup = 30.
> - Trường hợp 1: trong 30 bản ghi khớp X, **cả 30** có class A → luật **đúng 100%**.
> - Trường hợp 2: trong 100 bản ghi khớp X, **30** có class A, **70** có class B → luật chỉ đúng 30%!
>
> Cả hai đều có sup = 30, nhưng **confidence hoàn toàn khác**.

### 3.5 Trực giác về giá trị

| Confidence | Ý nghĩa |
|:---------:|--------|
| 100% | Luật chắc chắn (trong training) |
| 80–99% | Rất tin cậy |
| 50–80% | Có cơ sở, cần kiểm chứng thêm |
| <50% | Yếu, thường bị loại |

### 3.6 Tác dụng trong CMAR

| Nơi dùng | Tác dụng |
|----------|----------|
| Bước 4 (sinh CAR) | Luật có conf < minConf → bỏ |
| Bước 10 (precedence) | Tiêu chí thứ nhất để sắp luật: **conf cao trước** |

### 3.7 Hạn chế nghiêm trọng: "Paradox trà–cà phê"

**Tình huống:** Dataset 1000 khách hàng:
- 900 khách hàng mua cà phê (90%)
- 200 khách hàng mua trà, trong đó 150 mua cả cà phê

Luật: `trà ⇒ cà phê` có conf = 150/200 = **75%**.

**Nhìn vào conf = 75% có vẻ OK?** **SAI!** Lý do:
- Xác suất nền của cà phê: 900/1000 = **90%**
- Biết khách mua trà → xác suất mua cà phê chỉ còn 75% → **GIẢM đi so với baseline!**
- Luật này thật ra **tương quan ÂM** — biết trà thật ra làm **giảm** khả năng cà phê.

**Bài học:** Confidence đơn lẻ không đủ. Cần so sánh với xác suất nền của $c$ → đây là lý do cần **lift** và **χ²**.

### 3.8 Tính chất quan trọng

**Bất đối xứng:**
$$
\text{conf}(X \Rightarrow Y) \ne \text{conf}(Y \Rightarrow X)
$$

Ví dụ: "mẹ → con" và "con → mẹ" hoàn toàn khác nhau.

---

## 4. Lift — Công thức "đo độ tăng"

### 4.1 Công thức

$$
\text{lift}(R) = \frac{\text{conf}(R)}{\sigma(c)} = \frac{P(c \mid P)}{P(c)}
$$

### 4.2 Câu hỏi nó trả lời

> **"Biết vế trái có làm TĂNG cơ hội vế phải so với ngẫu nhiên không?"**

### 4.3 Tác dụng chính

Lift là **tỉ số** giữa confidence (có biết P) và baseline (không biết gì, chỉ dự đoán theo tần suất c):
- **lift = 1** → P không ảnh hưởng c (độc lập)
- **lift > 1** → P làm **TĂNG** khả năng c (tương quan **dương**)
- **lift < 1** → P làm **GIẢM** khả năng c (tương quan **âm**)

### 4.4 Tại sao cần?

**Vấn đề confidence đã gặp:** conf = 75% nghe cao, nhưng nếu baseline của c là 90%, thì 75% **thấp hơn baseline** → luật vô nghĩa.

**Lift giải quyết:** chia conf cho baseline → tỉ số trực tiếp cho biết P có "có ích" cho dự đoán c không.

### 4.5 Trực giác về giá trị

| Lift | Ý nghĩa |
|:----:|--------|
| 1.0 | Độc lập (P không liên quan c) |
| 1.5 | P làm tăng 50% cơ hội c |
| 2.0 | P làm tăng gấp đôi cơ hội c |
| 5.0 | P làm tăng 5 lần (rất mạnh) |
| 0.5 | P làm **giảm** 50% cơ hội c (tương quan âm) |
| 0.0 | P **loại trừ** c |

### 4.6 Ví dụ

Dataset siêu thị 1000 khách:
- 100 mua tã (sup = 10%)
- 500 mua bia (sup = 50%)
- 80 mua cả tã và bia

Luật: `tã ⇒ bia`
- conf = 80/100 = **80%**
- baseline = 500/1000 = **50%**
- **lift = 80%/50% = 1.6**

Diễn giải: "Khách mua tã có khả năng mua bia **cao hơn 60%** so với khách ngẫu nhiên." — đây là mối liên hệ **có ý nghĩa marketing**.

### 4.7 Tác dụng trong CMAR

CMAR **không trực tiếp** dùng lift trong pruning (dùng χ² thay thế vì χ² kiểm định thống kê chặt hơn). Tuy nhiên, **điều kiện "tương quan dương"** trong Pruning 2 ($ad > b\gamma$) tương đương **lift > 1**.

### 4.8 Tính chất

**Đối xứng:**
$$
\text{lift}(X \Rightarrow Y) = \text{lift}(Y \Rightarrow X)
$$

(Khác confidence — lift không phân biệt chiều.)

### 4.9 Hạn chế

- **Không có ý nghĩa thống kê:** lift = 1.6 với sup = 5 và lift = 1.6 với sup = 500 **rất khác nhau** về độ tin cậy, nhưng lift không phân biệt.
- **Cần bổ sung χ²** để biết liệu lift có phải ngẫu nhiên hay không.

---

## 5. Leverage — Công thức "chênh lệch quan sát vs kỳ vọng"

### 5.1 Công thức

$$
\text{lev}(R) = \sigma(P \cup \{c\}) - \sigma(P) \cdot \sigma(c)
$$

### 5.2 Câu hỏi nó trả lời

> **"Số bản ghi khớp CẢ P và c xuất hiện nhiều hơn kỳ vọng bao nhiêu (nếu P, c độc lập)?"**

### 5.3 Tác dụng chính

Leverage đo **chênh lệch tuyệt đối** giữa:
- **Thực tế:** $\sigma(P \cup \{c\})$ — tỉ lệ bản ghi khớp cả P và c
- **Kỳ vọng (nếu độc lập):** $\sigma(P) \cdot \sigma(c)$ — tích xác suất

### 5.4 Tại sao cần?

**Lift và leverage bổ sung cho nhau:**
- Lift cho biết **tỉ lệ tăng** (gấp mấy lần) — dễ hiểu nhưng không nói về **quy mô**.
- Leverage cho biết **quy mô chênh lệch** — ví dụ "15% bản ghi hơn kỳ vọng".

**Ví dụ so sánh:**

| Luật | σ(P) | σ(c) | σ(P∩c) kỳ vọng | σ(P∩c) thực | Lift | Leverage |
|------|:----:|:----:|:--------------:|:-----------:|:----:|:--------:|
| R₁ | 0.001 | 0.001 | 0.000001 | 0.000002 | **2.0** | **0.000001** |
| R₂ | 0.5 | 0.5 | 0.25 | 0.35 | **1.4** | **0.10** |

- R₁ có lift cao hơn (2.0 vs 1.4) nhưng **leverage cực thấp** (chỉ 1 bản ghi thêm!).
- R₂ lift thấp hơn nhưng **leverage lớn** (10% bản ghi hơn kỳ vọng) → **thực tế mạnh hơn**.

### 5.5 Phạm vi giá trị

$$
-0.25 \le \text{lev}(R) \le +0.25
$$

| Leverage | Ý nghĩa |
|:--------:|--------|
| 0 | Độc lập |
| >0 | Tương quan dương (thực tế > kỳ vọng) |
| <0 | Tương quan âm |
| +0.25 | Cực đại (trường hợp hiếm) |

### 5.6 Tác dụng trong CMAR

CMAR **không trực tiếp** dùng leverage, nhưng ý tưởng tương tự xuất hiện trong điều kiện $ad > b\gamma$ của Pruning 2 (có thể viết lại thành: "quan sát > kỳ vọng").

### 5.7 Ứng dụng thực tế

Leverage được dùng nhiều trong market basket analysis vì trực giác tự nhiên: "Doanh thu chéo thêm được bao nhiêu nhờ bán combo?"

---

## 6. Conviction — Công thức "đo sức mạnh suy diễn"

### 6.1 Công thức

$$
\text{conv}(R) = \frac{1 - \sigma(c)}{1 - \text{conf}(R)}
$$

### 6.2 Câu hỏi nó trả lời

> **"Nếu bỏ qua mối liên hệ P⇒c, luật này sẽ sai nhiều HƠN bao nhiêu?"**

### 6.3 Tác dụng chính

Conviction đo **tần suất luật sai** so với tần suất kỳ vọng nếu P, c độc lập.

**Tử số** $1 - \sigma(c)$: tỉ lệ bản ghi **không có lớp c** (tỉ lệ luật có thể sai nếu độc lập).
**Mẫu số** $1 - \text{conf}(R)$: tỉ lệ bản ghi khớp P **nhưng không có lớp c** (tỉ lệ luật sai thực tế).

### 6.4 Trực giác về giá trị

| Conviction | Ý nghĩa |
|:---------:|--------|
| 1.0 | Độc lập |
| 1.5 | Luật sai ít hơn 33% so với kỳ vọng |
| 2.0 | Luật sai ít hơn 50% |
| ∞ | Confidence = 100% — không bao giờ sai (trong training) |
| <1 | Luật sai NHIỀU hơn kỳ vọng (tương quan âm) |

### 6.5 Tại sao cần Conviction?

**Hạn chế của lift:** Khi confidence = 100%, ta muốn nói "luật rất mạnh". Nhưng lift có thể không đủ phân biệt:

Ví dụ: Hai luật conf = 100%:
- R₁: σ(c) = 0.1 → lift = 10
- R₂: σ(c) = 0.9 → lift = 1.11

Với conviction:
- R₁: conv = (1-0.1)/(1-1) = **∞**
- R₂: conv = (1-0.9)/(1-1) = **∞**

Đều **∞** — nhưng thực tế R₁ mạnh hơn (biết P → giải được từ 10% → 100%, tức cải thiện 9x; R₂ chỉ cải thiện từ 90% → 100%).

**Kết luận:** Conviction có ý nghĩa tốt với luật conf gần 100% nhưng cần xét cùng lift để đầy đủ.

### 6.6 Tác dụng trong CMAR

**Không trực tiếp dùng** — CMAR dùng χ² thay thế. Nhưng ý tưởng "đánh giá luật theo mức độ vượt baseline" là chung.

---

## 7. Chi-square (χ²) — Công thức "kiểm định thống kê"

### 7.1 Công thức

$$
\chi^2 = \frac{n(ad - b\gamma)^2}{(a+b)(\gamma+d)(a+\gamma)(b+d)}
$$

với bảng contingency 2×2:

| | class=c | class≠c | tổng |
|-|:-------:|:-------:|:----:|
| khớp P | $a$ | $b$ | $a+b$ |
| không khớp P | $\gamma$ | $d$ | $\gamma+d$ |
| tổng | $a+\gamma$ | $b+d$ | $n$ |

### 7.2 Câu hỏi nó trả lời

> **"Mối liên hệ giữa P và c có phải NGẪU NHIÊN hay có ý nghĩa thống kê?"**

### 7.3 Tác dụng chính

χ² kiểm định **giả thuyết không** $H_0$: "P và c độc lập".

- **χ² CAO** → bác bỏ $H_0$ → P và c CÓ liên hệ (không phải ngẫu nhiên).
- **χ² THẤP** → không bác bỏ $H_0$ → có thể chỉ là trùng hợp.

### 7.4 Tại sao cần?

**Lift, leverage, conviction đều đo "mức độ liên hệ"** — nhưng **không nói được** liệu liên hệ đó là **thật** hay chỉ do **mẫu nhỏ**.

**Ví dụ:**
- Dataset 10 bản ghi: tung đồng xu 10 lần được 7 mặt ngửa → lift "có xu hướng" nhưng có thể chỉ do mẫu nhỏ.
- Dataset 10,000 bản ghi: 7000 ngửa → lift tương tự nhưng **chắc chắn** có bias.

χ² **tự động** điều chỉnh theo kích thước mẫu qua nhân tử $n$ ở tử số.

### 7.5 Cơ chế hoạt động chi tiết

$\chi^2$ đo **khoảng cách** giữa **quan sát** và **kỳ vọng nếu độc lập**:

**Giá trị kỳ vọng (expected) của ô $(i,j)$:**
$$
E_{ij} = \frac{(\text{tổng hàng } i) \cdot (\text{tổng cột } j)}{n}
$$

**Công thức tổng quát:**
$$
\chi^2 = \sum_{i,j} \frac{(O_{ij} - E_{ij})^2}{E_{ij}}
$$

**Diễn giải:** Tổng độ lệch bình phương (chuẩn hoá) giữa quan sát và kỳ vọng.

### 7.6 Bảng ngưỡng χ² (df = 1)

| α (mức ý nghĩa) | χ² threshold | Mức tin cậy | Ý nghĩa |
|:---------------:|:------------:|:-----------:|---------|
| 0.10 | 2.706 | 90% | Lỏng |
| **0.05** | **3.841** | **95%** | **Mặc định CMAR** |
| 0.01 | 6.635 | 99% | Chặt |
| 0.001 | 10.827 | 99.9% | Cực chặt |

**Cách đọc:** Nếu χ² ≥ 3.841, ta 95% chắc chắn mối liên hệ giữa P và c **không phải ngẫu nhiên**.

### 7.7 Tác dụng trong CMAR

| Nơi dùng | Tác dụng |
|----------|----------|
| **Pruning tầng 2** | Loại luật có χ² < ngưỡng (không đủ ý nghĩa thống kê) |
| **Classification** | Thành phần chính của weighted χ² score |

### 7.8 Ví dụ minh hoạ

**Luật `odor=f ⇒ poisonous` trên Mushroom:**

| | poisonous | edible | tổng |
|-|:---------:|:------:|:----:|
| odor=f | 2160 | 0 | 2160 |
| ≠f | 1756 | 4208 | 5964 |

$$
\chi^2 = \frac{8124 \times (2160 \times 4208 - 0)^2}{2160 \times 5964 \times 3916 \times 4208} \approx 2441
$$

χ² = 2441 **vượt xa** 3.841 → luật **cực kỳ có ý nghĩa** về mặt thống kê.

**Luật `outlook=overcast ⇒ play=yes` trên dataset thời tiết (n=14):**

| | yes | no | tổng |
|-|:---:|:--:|:----:|
| overcast | 4 | 0 | 4 |
| ≠overcast | 5 | 5 | 10 |

$$
\chi^2 = \frac{14 \times (4 \times 5 - 0)^2}{4 \times 10 \times 9 \times 5} \approx 3.11
$$

χ² = 3.11 **chưa đủ** 3.841 dù luật nhìn "có vẻ mạnh". Lý do: **dataset quá nhỏ** (n=14), có thể chỉ trùng hợp.

### 7.9 Điều kiện tương quan dương — $ad > b\gamma$

χ² **không phân biệt** tương quan dương/âm (bình phương mất dấu). Phải kiểm tra thêm:

$$
ad > b\gamma
$$

**Diễn giải:** Quan sát $a$ (khớp P và có c) cao hơn so với kỳ vọng → tương quan dương.

**Trong CMAR:** Pruning 2 yêu cầu **cả hai** điều kiện:
- χ² ≥ ngưỡng
- $ad > b\gamma$

---

## 8. Max Chi-square — Công thức "chuẩn hoá về [0,1]"

### 8.1 Công thức

$$
a_{\max} = \min(\text{sup}(P), \text{sup}(c))
$$
$$
b = \text{sup}(P) - a_{\max}, \quad \gamma = \text{sup}(c) - a_{\max}, \quad d = n - a_{\max} - b - \gamma
$$
$$
\max\chi^2 = \frac{n(a_{\max} d - b\gamma)^2}{(a_{\max}+b)(\gamma+d)(a_{\max}+\gamma)(b+d)}
$$

### 8.2 Câu hỏi nó trả lời

> **"Giá trị χ² LỚN NHẤT có thể đạt được nếu P và c gắn bó HOÀN HẢO?"**

### 8.3 Tác dụng chính

Tạo ra một **cận trên lý thuyết** cho χ² — để ta biết χ² thực tế "đang ở mức nào" so với mức tối đa có thể đạt được.

### 8.4 Tại sao cần?

**Vấn đề χ² thô:** Giá trị χ² phụ thuộc mạnh vào kích thước class:

| Luật | sup(P) | sup(c) | a | χ² thô |
|------|:------:|:------:|:-:|:------:|
| R₁ | 100 | 100 | 100 | **Lớn** |
| R₂ | 10 | 10 | 10 | **Nhỏ** (dù "hoàn hảo") |

Cả hai đều **pure** (conf = 100%), nhưng R₁ có χ² cao hơn chỉ vì sup lớn → **không công bằng** so sánh.

**Giải pháp:** Chia cho cận trên:
$$
\frac{\chi^2}{\max\chi^2} \in [0, 1]
$$

Giờ mọi luật đều trên thang [0, 1]:
- 1.0 → luật pure (quan sát đạt cận trên)
- 0.5 → trung bình
- 0.0 → độc lập

### 8.5 Cơ chế chuẩn hoá

**Trường hợp a đạt tối đa lý tưởng:**
$$
a_{\max} = \min(\text{sup}(P), \text{sup}(c))
$$

**Lý do:** a không thể vượt quá sup(P) (số bản ghi khớp P) và cũng không vượt quá sup(c) (số bản ghi có class c).

**Khi nào $a = a_{\max}$?** Khi mọi bản ghi khớp P đều có class c (hoặc ngược lại) → luật pure.

### 8.6 Tác dụng trong CMAR

**Dùng trong weighted χ² score (§9)** để chuẩn hoá các luật trước khi cộng.

**Ý nghĩa thực tế:** Luật nhỏ nhưng pure được đánh giá **công bằng** với luật lớn pure — không bị bỏ rơi do support thấp.

### 8.7 Ví dụ

Dataset Mushroom, so sánh 3 luật:

| Luật | sup | conf | χ² thô | maxχ² | χ²/maxχ² |
|------|:---:|:----:|:------:|:-----:|:--------:|
| `odor=f ⇒ p` | 2160 | 1.00 | 2441 | 2441 | **1.00** |
| `odor=a ⇒ e` | 400 | 1.00 | 411 | 411 | **1.00** |
| `odor=n ⇒ e` | 3408 | 0.966 | 5021 | 5379 | **0.933** |

**Bài học:**
- Theo χ² thô, `odor=n ⇒ e` (5021) > `odor=f ⇒ p` (2441) > `odor=a ⇒ e` (411).
- Theo χ²/maxχ², luật 1 và 2 **đều pure = 1.00**, luật 3 chỉ 0.933 (có 120 ngoại lệ).

→ Chuẩn hoá **thay đổi xếp hạng** → công bằng hơn.

---

## 9. Weighted χ² Score — Công thức "bỏ phiếu có trọng số"

### 9.1 Công thức

$$
\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \frac{\chi^2(R)^2}{\max\chi^2(R)}
$$

với $\mathcal{G}_c$ là nhóm luật có class $c$ khớp với bản ghi test.

### 9.2 Câu hỏi nó trả lời

> **"Khi nhiều luật mâu thuẫn cùng khớp một bản ghi, nhóm luật nào mạnh nhất?"**

### 9.3 Tác dụng chính

Gộp nhiều luật cùng class thành một **điểm số tổng hợp** → so sánh giữa các class → chọn class có score cao nhất.

### 9.4 Tại sao cần?

**Bối cảnh:** Bản ghi test $t$ có thể khớp với **nhiều luật** có class khác nhau:
- 3 luật dự đoán class A
- 2 luật dự đoán class B

**Câu hỏi:** Dự đoán A hay B?

**Cách 1 — Chọn luật conf cao nhất (như CBA):** Dễ sai lệch (xem §1.3 bài báo gốc).

**Cách 2 — Bỏ phiếu đếm số luật:** Có thể sai nếu 3 luật A đều yếu, 2 luật B đều rất mạnh.

**Cách 3 — CMAR: Weighted χ² Score.** Cộng sức mạnh tất cả luật trong nhóm, nhóm mạnh nhất thắng.

### 9.5 Giải thích từng thành phần

$$
\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \underbrace{\frac{\chi^2(R)^2}{\max\chi^2(R)}}_{\text{đóng góp của luật R}}
$$

**Tại sao $\chi^2(R)^2$ ở tử số (bình phương)?**
- Bình phương **phóng đại** sự khác biệt giữa luật mạnh và yếu.
- Luật có χ² = 100 đóng góp 100² = 10,000 — gấp 100× so với luật χ² = 10 (chỉ 100).
- → **Luật mạnh chi phối**, luật yếu gần như vô hình.

**Tại sao chia cho $\max\chi^2(R)$?**
- Chuẩn hoá luật về thang [0, 1] (xem §8).
- Tránh luật sup lớn "áp đảo" luật sup nhỏ bất công.

**Kết quả:** Score vừa **nhấn mạnh luật mạnh** vừa **công bằng về quy mô**.

### 9.6 Ví dụ đầy đủ

Bản ghi test $t$ khớp 4 luật:

| # | Class | $\chi^2$ | $\max\chi^2$ | Đóng góp $\chi^2^2/\max\chi^2$ |
|:-:|:-----:|:--------:|:------------:|:------------------------------:|
| R₁ | A | 10 | 25 | $100/25 = 4.0$ |
| R₂ | A | 8 | 20 | $64/20 = 3.2$ |
| R₃ | B | 15 | 18 | $225/18 \approx 12.5$ |
| R₄ | B | 6 | 12 | $36/12 = 3.0$ |

**Tính:**
- $\text{score}(A) = 4.0 + 3.2 = 7.2$
- $\text{score}(B) = 12.5 + 3.0 = 15.5$

**Kết quả:** Class B thắng.

**Đếm phiếu thuần:** 2 luật A vs 2 luật B → hoà.
**Weighted score:** B có luật R₃ rất mạnh → B thắng xứng đáng.

### 9.7 Các trường hợp "đi đường tắt"

CMAR không luôn chạy weighted χ². Có 2 trường hợp đi đường tắt trước:

**Shortcut 1:** Nếu $\mathcal{R}(t) = \emptyset$ → trả về **defaultClass**.

**Shortcut 2:** Nếu tất cả luật trong $\mathcal{R}(t)$ cùng class → trả về class đó **ngay** (không cần tính score).

Chỉ khi có **mâu thuẫn** mới cần weighted χ². Điều này tiết kiệm tính toán.

### 9.8 Tác dụng trong CMAR

**Đây là công thức TRUNG TÂM của classification.** Tất cả các công thức khác (sup, conf, χ², maxχ²) đều **dẫn đến** đây.

### 9.9 So sánh với các chiến lược khác

| Chiến lược | Cách | Ưu | Nhược |
|-----------|------|----|-------|
| Best rule (CBA) | Chọn luật conf cao nhất | Đơn giản | Dễ sai |
| Majority voting | Đếm phiếu các luật | Dân chủ | Không xét sức mạnh |
| Sum χ² | Cộng χ² thô | Đo tổng sức mạnh | Bias minority |
| **CMAR weighted χ²** | $\sum \chi^2^2/\max\chi^2$ | **Cân bằng tốt** | Tính phức tạp hơn |

---

## 10. Rule Precedence — Công thức "xếp hạng luật"

### 10.1 Công thức

$$
R_1 \succ R_2 \iff
\begin{cases}
\text{conf}(R_1) > \text{conf}(R_2), \text{ hoặc} \\
\text{conf}(R_1) = \text{conf}(R_2) \wedge \text{sup}(R_1) > \text{sup}(R_2), \text{ hoặc} \\
\text{bằng 2 tiêu chí trên và } |P_1| < |P_2|
\end{cases}
$$

### 10.2 Câu hỏi nó trả lời

> **"Luật nào ĐÁNG TIN hơn khi cả hai cùng áp dụng được?"**

### 10.3 Tác dụng chính

**3 tiêu chí xếp theo mức độ ưu tiên:**

| Ưu tiên | Tiêu chí | Lý do |
|:-------:|----------|-------|
| 1 | **conf cao trước** | Tin cậy quan trọng nhất — luật đúng thường trước |
| 2 | **sup cao trước** | Nhiều bằng chứng → luật đáng tin hơn |
| 3 | **size ngắn trước** | Occam's razor — đơn giản hơn thì tốt hơn |

### 10.4 Tại sao cần precedence?

**Bối cảnh:** Trong Pruning 1 và Database Coverage, ta duyệt luật **từ tốt nhất đến tệ nhất**. Precedence định nghĩa "tốt nhất".

**Vấn đề không có precedence:** Nếu duyệt ngẫu nhiên:
- Pruning 1: luật chi tiết có thể bị kiểm tra trước → không bị luật tổng quát lấn át → giữ luôn → kết quả phụ thuộc thứ tự.
- Coverage: luật yếu được chọn trước → "ăn mất" training record của luật mạnh → mất luật mạnh.

**Với precedence:** Luật tốt được xem xét trước → được chọn trước → kết quả **đáng tin và tái lập**.

### 10.5 Ví dụ so sánh

Cho 3 luật cùng class X:

| Luật | conf | sup | size |
|------|:----:|:---:|:----:|
| R_A: `{a}⇒X` | 0.90 | 100 | 1 |
| R_B: `{a,b}⇒X` | 0.90 | 80 | 2 |
| R_C: `{a,b}⇒X` | 0.85 | 150 | 2 |

Sắp theo precedence:
1. **R_A** (conf cao nhất)
2. **R_B** (cùng conf với A, nhưng A trước vì sup cao hơn — A đã ở vị trí 1)
3. **R_C** (conf thấp nhất)

**So sánh cặp:**
- $R_A \succ R_B$ (cùng conf 0.90, R_A có sup 100 > 80; nếu sup bằng nhau thì do R_A size nhỏ hơn)
- $R_A \succ R_C$ (R_A conf 0.90 > 0.85)
- $R_B \succ R_C$ (R_B conf 0.90 > 0.85)

### 10.6 Tại sao conf > sup > size?

**Tại sao conf đầu tiên?** Độ tin cậy quan trọng nhất — không ai muốn dùng luật dự đoán sai nhiều.

**Tại sao sau đó là sup?** Khi 2 luật cùng conf, luật có **nhiều bằng chứng** hơn (sup cao) đáng tin hơn — giảm rủi ro overfitting.

**Tại sao cuối cùng là size?** Nguyên tắc **Occam's razor** — giả thuyết đơn giản hơn thường đúng hơn. Luật `{a}⇒X` đơn giản hơn `{a,b,c}⇒X` → ưu tiên.

### 10.7 Tác dụng trong CMAR

| Nơi dùng | Mục đích |
|----------|----------|
| **Pruning 1** | Sắp để luật tổng quát mạnh **được xét trước** → lấn át luật chi tiết yếu |
| **Pruning 3 (Coverage)** | Duyệt từ luật tốt nhất → đảm bảo luật "quý" được chọn trước |

### 10.8 Không phải tiêu chí "cứng" cho classification

**Lưu ý:** Precedence chỉ dùng **trong training** để sắp luật. **Khi classify**, CMAR **không** chọn luật có precedence cao nhất (đó là CBA) — mà dùng weighted χ² tổng hợp.

---

## 11. Các Điều Kiện Pruning — Công Thức Cắt Tỉa

### 11.1 Pruning 1 — Điều kiện luật dư thừa

**Công thức:**
$$
R_2 \text{ bị loại} \iff \exists R_1 : P_1 \subseteq P_2 \wedge \text{class}(R_1) = \text{class}(R_2) \wedge R_1 \succ R_2
$$

**3 điều kiện cùng lúc:**
1. $P_1 \subseteq P_2$ — R₁ tổng quát hơn (condset là tập con)
2. $\text{class}(R_1) = \text{class}(R_2)$ — cùng class
3. $R_1 \succ R_2$ — R₁ ưu tiên hơn

**Tại sao cả 3?**
- Nếu thiếu (1): hai luật không có quan hệ tổng quát/chi tiết → không thể so sánh dư thừa.
- Nếu thiếu (2): khác class thì hai luật có mục đích khác, không thay thế được nhau.
- Nếu thiếu (3): nếu R₁ yếu hơn R₂, giữ R₁ làm gì? Phải R₁ mạnh hơn mới có tư cách "thay thế" R₂.

**Tác dụng:** Giảm 70–95% luật ứng viên.

### 11.2 Pruning 2 — Điều kiện luật có ý nghĩa

**Công thức:**
$$
R \text{ được giữ} \iff \chi^2(R) \ge \chi^2_{\text{threshold}} \wedge ad > b\gamma
$$

**Hai điều kiện:**
1. **$\chi^2 \ge$ ngưỡng (mặc định 3.841)** — liên hệ có ý nghĩa thống kê.
2. **$ad > b\gamma$** — tương quan dương (P làm TĂNG khả năng c).

**Tại sao cả 2?**
- Chỉ χ² ≥ ngưỡng chưa đủ — χ² không phân biệt dương/âm (đã bình phương).
- Chỉ tương quan dương chưa đủ — có thể chỉ do mẫu nhỏ ngẫu nhiên.

**Tác dụng:** Loại 50–80% luật sau Pruning 1.

### 11.3 Pruning 3 — Điều kiện loại bản ghi training

**Công thức (trong Algorithm 1):**
$$
\text{obj bị loại khỏi T} \iff \text{cover\_count[obj]} \ge \delta
$$

**Điều kiện luật được chọn:**
$$
R \text{ được chọn} \iff \exists \text{obj} \in T : R \text{ phân lớp đúng obj}
$$

**Hai ý cùng vận hành:**
1. Duyệt luật theo precedence giảm dần.
2. Với mỗi luật: nếu nó phân lớp đúng ít nhất 1 bản ghi còn trong T → chọn. Sau đó tăng cover_count cho các bản ghi đó. Bản ghi nào đạt cover ≥ δ → loại.

**Tại sao cần cơ chế này?**
- CBA: loại bản ghi ngay khi bị cover (δ=1) → giữ quá ít luật.
- CMAR với δ > 1: giữ nhiều luật hơn → weighted χ² có nhiều "phiếu" để tổng hợp.

**Tác dụng:** Loại 30–70% luật sau Pruning 2.

### 11.4 Bảng tổng hợp 3 tầng pruning

| Tầng | Điều kiện loại | Cơ sở trực giác |
|:----:|----------------|-----------------|
| P1 | $\exists$ R' tổng quát hơn, cùng class, mạnh hơn | "Luật đơn giản mạnh đã thay thế được → không cần luật chi tiết" |
| P2 | $\chi^2 <$ ngưỡng hoặc $ad \le b\gamma$ | "Luật không đủ ý nghĩa thống kê / tương quan âm" |
| P3 | Không phân lớp đúng thêm bản ghi nào | "Luật đã không còn đóng góp — training data đã đủ cover" |

---

## 12. Precision, Recall, F1 — Công thức đánh giá kết quả

### 12.1 Các công thức

**Precision (Độ chính xác):**
$$
P_c = \frac{TP_c}{TP_c + FP_c}
$$

**Recall (Độ bao phủ):**
$$
R_c = \frac{TP_c}{TP_c + FN_c}
$$

**F1-score:**
$$
F_1^{(c)} = \frac{2 P_c R_c}{P_c + R_c}
$$

**Accuracy:**
$$
\text{Accuracy} = \frac{\sum_c TP_c}{|T_{\text{test}}|}
$$

**Macro-F1:**
$$
F_1^{\text{macro}} = \frac{1}{|\mathcal{C}|} \sum_c F_1^{(c)}
$$

### 12.2 Câu hỏi từng công thức trả lời

| Công thức | Câu hỏi |
|-----------|---------|
| Accuracy | "Dự đoán đúng tổng thể bao nhiêu %?" |
| Precision | "Khi dự đoán class c, bao nhiêu % đúng?" |
| Recall | "Trong tất cả bản ghi thực sự là c, ta phát hiện được bao nhiêu %?" |
| F1 | "Precision và Recall cân bằng ra sao?" |
| Macro-F1 | "Trung bình F1 cho mọi class (kể cả minority)?" |

### 12.3 Tác dụng chính

**Tại sao không chỉ dùng Accuracy?**

Giả sử dataset có 95% class A, 5% class B:
- Bộ phân lớp "dự đoán A mọi lúc" → accuracy = **95%** (nghe tốt!)
- Nhưng precision/recall/F1 cho class B = **0** (không phát hiện được B nào)

→ Accuracy **che giấu** vấn đề class imbalance.

### 12.4 Giải thích trực quan

**TP, FP, FN, TN:**
- **TP (True Positive):** dự đoán c, thực tế c → đúng ✓
- **FP (False Positive):** dự đoán c, thực tế khác → sai ✗
- **FN (False Negative):** dự đoán khác, thực tế c → sai ✗
- **TN (True Negative):** dự đoán khác, thực tế khác → đúng ✓

**Precision cao = "Không kết luận bừa":** Khi nói "đây là class c", ta tự tin.

**Recall cao = "Không bỏ sót":** Mọi bản ghi thực sự là c đều được phát hiện.

**Trade-off:** Precision ↑ thường Recall ↓ và ngược lại → F1 cân bằng cả hai.

### 12.5 Ví dụ minh hoạ

Dataset test 100 bản ghi, class c có 20 bản ghi. Bộ phân lớp dự đoán 25 bản ghi là c, trong đó 18 đúng.

- TP = 18, FP = 7, FN = 2, TN = 73
- Precision = 18/(18+7) = 0.72
- Recall = 18/(18+2) = 0.90
- F1 = 2 × 0.72 × 0.90 / (0.72 + 0.90) ≈ 0.80

**Diễn giải:**
- 72% trường hợp dự đoán c là đúng (Precision)
- Phát hiện 90% bản ghi c thực sự (Recall)
- Tổng thể F1 ≈ 0.80 (tốt)

### 12.6 Tác dụng trong CMAR

**Không dùng trong thuật toán** — chỉ để **đánh giá kết quả** sau khi chạy.

**Trong báo cáo:**
- Macro-F1 để so sánh CMAR với CBA, C4.5 (công bằng hơn accuracy trên dataset imbalance).
- Precision/Recall theo class để phân tích điểm mạnh/yếu.

---

## 13. Bảng Tổng Kết — Công Thức Nào Dùng Ở Đâu

### 13.1 Bản đồ công thức theo pipeline

```
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 1 — Đếm item, lọc sơ bộ                               │
│     → sử dụng SUPPORT (minSup)                              │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 2 — Xây FP-tree khởi tạo                              │
│     → sử dụng SUPPORT (sắp F-list theo freq)                │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 3–4 — FP-Growth đào mỏ + Sinh CAR                     │
│     → sử dụng SUPPORT + CONFIDENCE (minSup + minConf)        │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 5 — Pruning 1 (General Rule)                          │
│     → sử dụng RULE PRECEDENCE (conf↓, sup↓, size↑)           │
│     → điều kiện: P₁ ⊆ P₂ + cùng class + R₁ ≻ R₂             │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 6 — Pruning 2 (Chi-square)                            │
│     → sử dụng χ² + điều kiện ad > bγ                         │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 7 — Pruning 3 (Database Coverage)                     │
│     → sử dụng RULE PRECEDENCE + đếm cover_count             │
│     → điều kiện: cover ≥ δ → loại bản ghi                    │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 8 — Xây CR-tree lưu trữ                               │
│     → sử dụng FREQ (sắp item theo freq DESC)                │
└──────────────────────────────────────────────────────────────┘
                         ↓
         ═══════════════════════════════════════
                        [ CLASSIFY ]
         ═══════════════════════════════════════
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 9 — Truy vấn luật khớp                                │
│     → sử dụng SUBSET test (P ⊆ items(t))                    │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 10 — Phân lớp Weighted χ²                             │
│     → sử dụng χ² + MAX χ² + WEIGHTED SCORE                   │
│     → Σ χ²(R)² / maxχ²(R)                                    │
└──────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 11 — Đánh giá                                         │
│     → sử dụng ACCURACY, PRECISION, RECALL, F1               │
└──────────────────────────────────────────────────────────────┘
```

### 13.2 Bảng matrix: Công thức × Vai trò

| Công thức | Training | Classify | Evaluation |
|-----------|:--------:|:--------:|:----------:|
| Support | ✓✓✓ | — | — |
| Confidence | ✓✓ | — | — |
| Lift/Leverage/Conviction | — | — | — (tham khảo) |
| χ² | ✓✓ (Pruning 2) | ✓✓✓ (weighted) | — |
| max χ² | — | ✓✓ | — |
| Weighted χ² Score | — | ✓✓✓ | — |
| Rule precedence | ✓✓ | — | — |
| Accuracy/F1 | — | — | ✓✓✓ |

**Chú thích:** ✓ = dùng, ✓✓✓ = thành phần chính.

### 13.3 Tóm lược — 5 công thức quan trọng nhất của CMAR

Nếu chỉ nhớ 5 công thức, hãy nhớ:

**1. Support:**
$$
\text{sup}(R) = \#\{t : P \subseteq t \wedge \text{class}(t) = c\}
$$
→ *Luật có đủ phổ biến không?*

**2. Confidence:**
$$
\text{conf}(R) = \frac{\text{sup}(R)}{\text{sup}(P)}
$$
→ *Luật đúng bao nhiêu %?*

**3. Chi-square:**
$$
\chi^2 = \frac{n(ad - b\gamma)^2}{(a+b)(\gamma+d)(a+\gamma)(b+d)}
$$
→ *Liên hệ có ý nghĩa thống kê không?*

**4. Rule precedence:**
$$
R_1 \succ R_2: \text{conf}\downarrow, \text{sup}\downarrow, |P|\uparrow
$$
→ *Luật nào đáng tin hơn?*

**5. Weighted χ² Score:**
$$
\text{score}(\mathcal{G}_c) = \sum_{R} \frac{\chi^2(R)^2}{\max\chi^2(R)}
$$
→ *Nhóm luật nào mạnh nhất để dự đoán class?*

---

## Kết Luận

Mỗi công thức trong CMAR **không phải ngẫu nhiên** — chúng giải quyết một vấn đề cụ thể và bổ sung cho nhau:

- **Support** chặn nhiễu quy mô nhỏ.
- **Confidence** đo độ tin cậy cục bộ (theo vế trái).
- **Chi-square** đảm bảo ý nghĩa thống kê (không phải ngẫu nhiên).
- **Max χ² và chuẩn hoá** tạo thang đo công bằng giữa các luật có quy mô khác nhau.
- **Weighted χ² Score** tổng hợp ý kiến nhiều luật để đưa ra quyết định cuối.
- **Rule precedence** định nghĩa "thứ tự tốt nhất" để pruning và coverage hoạt động hiệu quả.
- **Precision/Recall/F1** đánh giá khách quan chất lượng bộ phân lớp.

Bỏ đi bất kỳ công thức nào → CMAR sẽ **không hoạt động tốt** hoặc **mắc bẫy** (paradox trà-cà phê, bias minority class, overfitting, v.v.).

→ **Hiểu "tại sao" mỗi công thức tồn tại** quan trọng hơn học thuộc công thức.

---

*File giải thích này đi kèm [bao_cao_chi_tiet_CMAR.md](bao_cao_chi_tiet_CMAR.md) — tập trung vào ý nghĩa và động cơ đằng sau từng công thức, thay vì chỉ trình bày định nghĩa.*
