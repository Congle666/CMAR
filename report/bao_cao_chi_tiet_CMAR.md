# BÁO CÁO CHI TIẾT THUẬT TOÁN CMAR

**Classification based on Multiple Association Rules**

> **Công trình gốc:** Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* Proceedings of the 2001 IEEE International Conference on Data Mining (ICDM'01), pp. 369–376.

---

## Mục Lục

**PHẦN A — NỀN TẢNG LÝ THUYẾT**

1. [Đặt Vấn Đề & Động Cơ Nghiên Cứu](#1-đặt-vấn-đề--động-cơ-nghiên-cứu)
2. [Luật Kết Hợp — Định Nghĩa Hình Thức](#2-luật-kết-hợp--định-nghĩa-hình-thức)
3. [Support (Độ Hỗ Trợ) — Chi Tiết Đầy Đủ](#3-support-độ-hỗ-trợ--chi-tiết-đầy-đủ)
4. [Confidence (Độ Tin Cậy) — Chi Tiết Đầy Đủ](#4-confidence-độ-tin-cậy--chi-tiết-đầy-đủ)
5. [Các Độ Đo Liên Quan (Lift, Leverage, Conviction, χ²)](#5-các-độ-đo-liên-quan-lift-leverage-conviction-χ²)
6. [Ví Dụ Tính Toán Tổng Hợp](#6-ví-dụ-tính-toán-tổng-hợp-mọi-độ-đo)
7. [Ngưỡng & Quan Hệ Giữa Các Luật](#7-ngưỡng--quan-hệ-giữa-các-luật)

**PHẦN B — THUẬT TOÁN CMAR**

8. [Tổng Quan Thuật Toán CMAR](#8-tổng-quan-thuật-toán-cmar)
9. [Giai Đoạn I — Sinh Luật](#9-giai-đoạn-i--sinh-luật-phân-lớp)
10. [Giai Đoạn II — Cắt Tỉa 3 Tầng](#10-giai-đoạn-ii--cắt-tỉa-3-tầng-chi-tiết)
11. [Giai Đoạn III — Phân Lớp Weighted χ²](#11-giai-đoạn-iii--phân-lớp-bằng-weighted-χ²)
12. [Luồng Hoạt Động Tổng Thể (End-to-End)](#12-luồng-hoạt-động-tổng-thể-end-to-end)
    - 12.1–12.10 Ví dụ tổng hợp 10 bản ghi
    - 12.11 [Ví dụ thực tế — Mushroom Dataset (UCI)](#1211-ví-dụ-thực-tế--mushroom-dataset-uci)

**PHẦN C — PHÂN TÍCH & ĐÁNH GIÁ**

13. [Phân Tích Ưu – Nhược Điểm](#13-phân-tích-ưu--nhược-điểm)
14. [Tham Số & Hướng Dẫn Tinh Chỉnh](#14-tham-số--hướng-dẫn-tinh-chỉnh)
15. [Kết Luận](#15-kết-luận)
16. [Tài Liệu Tham Khảo](#16-tài-liệu-tham-khảo)
17. [Phụ Lục — Thuật Ngữ & Ký Hiệu](#17-phụ-lục--thuật-ngữ--ký-hiệu)

---

# PHẦN A — NỀN TẢNG LÝ THUYẾT

---

## 1. Đặt Vấn Đề & Động Cơ Nghiên Cứu

### 1.1 Bài toán phân lớp

**Phân lớp** (classification) là nhiệm vụ học có giám sát (supervised learning) cốt lõi của khai phá dữ liệu và học máy.

**Hình thức hóa:** Cho trước
- Lược đồ dữ liệu $(A_1, A_2, \dots, A_n)$ gồm $n$ **thuộc tính**
- Tập nhãn lớp hữu hạn $\mathcal{C} = \{c_1, c_2, \dots, c_m\}$
- Tập huấn luyện $T = \{(x^{(1)}, y^{(1)}), (x^{(2)}, y^{(2)}), \dots, (x^{(N)}, y^{(N)})\}$ gồm $N$ bản ghi đã biết nhãn, với $x^{(i)}$ là vector giá trị thuộc tính, $y^{(i)} \in \mathcal{C}$.

**Mục tiêu:** Xây dựng một hàm $\mathcal{F}: \text{dom}(A_1) \times \dots \times \text{dom}(A_n) \rightarrow \mathcal{C}$ sao cho với bản ghi mới $x$, dự đoán $\hat{y} = \mathcal{F}(x)$ chính xác cao.

### 1.2 Hướng tiếp cận "phân lớp dựa luật kết hợp"

Hướng **associative classification** (phân lớp dựa luật kết hợp) xuất phát từ khai phá luật kết hợp (Agrawal & Srikant, 1994):
- Bước 1: Đào mỏ toàn bộ luật "nếu … thì …" có nhãn lớp ở vế phải (CAR).
- Bước 2: Từ tập luật này xây dựng bộ phân lớp.

**Đại diện:** CBA (Liu et al., 1998), CAEP (Dong et al., 1999), ADT, **CMAR** (Li et al., 2001).

**Ưu điểm nổi bật:**
- Phát hiện **mối tương quan phức hợp** giữa nhiều thuộc tính (cây quyết định chỉ xét 1 thuộc tính mỗi nhánh).
- Luật tường minh → mô hình **diễn giải được** (interpretable).

### 1.3 Hai hạn chế cốt lõi CMAR muốn giải quyết

**Hạn chế 1 — Dự đoán dựa 1 luật duy nhất:**

CBA chọn luật có confidence cao nhất để phân lớp. Cách này dễ thiên lệch.

> **Ví dụ minh hoạ (bài báo):** Khách hàng `(no-job, investment-immigrant, oversea-asset > 500k)` có 3 luật khớp:
> - $R_1$: `no-job ⇒ credit-limit < 3000` (sup=3000, **conf=95%**)
> - $R_2$: `investment-immigrant ⇒ credit-limit > 3000` (sup=5000, conf=93%)
> - $R_3$: `oversea-asset ≥ 500k ⇒ credit-limit > 3000` (sup=8000, conf=91%)
>
> CBA chọn $R_1$ (conf cao nhất) → dự đoán hạn mức < 3000. Nhưng $R_2, R_3$ đều dự đoán ngược lại **và có support cao hơn đáng kể** → quyết định đa số có vẻ đáng tin hơn.

**Hạn chế 2 — Bùng nổ tổ hợp số luật:**

Khi minSup thấp, số CAR có thể bùng nổ lên hàng trăm nghìn / hàng triệu → **vấn đề lưu trữ, truy vấn, cắt tỉa, xếp hạng**.

### 1.4 Ba đóng góp của CMAR

| STT | Đóng góp | Giải quyết |
|-----|----------|-----------|
| 1 | **Weighted χ²** — dùng nhiều luật thay vì 1 | Hạn chế 1 |
| 2 | **CR-tree** — cấu trúc nén lưu luật | Hạn chế 2 |
| 3 | **Biến thể FP-Growth** — sinh luật trực tiếp | Hiệu suất |

---

## 2. Luật Kết Hợp — Định Nghĩa Hình Thức

### 2.1 Item — Đơn vị cơ sở

**Định nghĩa:** Một **item** (phần tử) là một cặp `(thuộc tính, giá trị)`, viết là `attr=value`.

**Ví dụ:**
- `outlook=sunny` — thuộc tính Outlook có giá trị "sunny"
- `age=young` — thuộc tính Age có giá trị "young"
- `income=[30k-50k]` — thuộc tính Income trong khoảng 30k–50k (đã rời rạc hoá)

**Lưu ý:** Item **không phải** là giá trị đơn lẻ ("sunny") mà là **cặp** gắn chặt với tên thuộc tính. Nếu cùng giá trị "sunny" xuất hiện ở thuộc tính khác (ví dụ `weather=sunny`), đó là hai item khác nhau.

**Tập toàn bộ item khả dĩ:**
$$
\mathcal{I} = \bigcup_{i=1}^{n} \{A_i = v \mid v \in \text{dom}(A_i)\}
$$

### 2.2 Transaction (Giao dịch / Bản ghi)

**Định nghĩa:** Một **transaction** $t$ là một tập con của $\mathcal{I}$ tương ứng với một bản ghi dữ liệu, kèm theo nhãn lớp:
$$
t = \{\text{items}(t), \text{class}(t)\}, \quad \text{items}(t) \subseteq \mathcal{I}, \quad \text{class}(t) \in \mathcal{C}
$$

**Ràng buộc:** Trong `items(t)` không thể có hai item cùng thuộc tính (một record chỉ có một giá trị cho mỗi cột).

**Ví dụ:** Bản ghi thời tiết thứ nhất (Outlook=sunny, Temp=hot, Humidity=high, Windy=false, Play=no):
$$
t_1 = \{\underbrace{\{\text{outlook=sunny}, \text{temp=hot}, \text{humidity=high}, \text{windy=false}\}}_{\text{items}(t_1)}, \underbrace{\text{play=no}}_{\text{class}(t_1)}\}
$$

**Tập huấn luyện:**
$$
T = \{t_1, t_2, \dots, t_N\}, \quad |T| = N
$$

### 2.3 Itemset / Pattern (Tập item / Mẫu)

**Định nghĩa:** Một **itemset** (còn gọi là **pattern**) là một tập con của $\mathcal{I}$:
$$
P \subseteq \mathcal{I}
$$

Nếu $|P| = k$, ta gọi $P$ là **k-itemset**.

**Ví dụ:**
- 1-itemset: $\{\text{outlook=sunny}\}$
- 2-itemset: $\{\text{outlook=sunny}, \text{humidity=high}\}$
- 3-itemset: $\{\text{outlook=sunny}, \text{humidity=high}, \text{windy=false}\}$

**Itemset rỗng:** $P = \emptyset$ là 0-itemset, hợp lệ theo lý thuyết nhưng thường bị loại.

### 2.4 Quan hệ "Khớp" (Match)

**Định nghĩa:** Bản ghi $t$ **khớp** pattern $P$ khi và chỉ khi mọi item của $P$ đều xuất hiện trong items(t):
$$
t \text{ khớp } P \iff P \subseteq \text{items}(t)
$$

**Ví dụ:** Với $t_1 = \{\text{outlook=sunny}, \text{temp=hot}, \text{humidity=high}, \text{windy=false}\}$:
- $t_1$ **khớp** $P_a = \{\text{outlook=sunny}\}$ ✓
- $t_1$ **khớp** $P_b = \{\text{outlook=sunny}, \text{humidity=high}\}$ ✓
- $t_1$ **không khớp** $P_c = \{\text{outlook=rain}\}$ ✗ (outlook của t₁ là sunny, không phải rain)
- $t_1$ **khớp** $P_d = \emptyset$ ✓ (rỗng luôn được khớp)

### 2.5 Luật Kết Hợp Tổng Quát

**Định nghĩa (dạng tổng quát):** Một **luật kết hợp** (association rule) là biểu thức:
$$
R : X \Rightarrow Y
$$
trong đó $X, Y \subseteq \mathcal{I}$, $X \cap Y = \emptyset$ và $X \ne \emptyset, Y \ne \emptyset$.

- $X$ gọi là **vế trái / tiền đề / antecedent / condset / LHS**
- $Y$ gọi là **vế phải / hệ quả / consequent / RHS**

**Đọc luật:** "Nếu bản ghi khớp $X$ thì (có khả năng cao) khớp cả $Y$."

### 2.6 Luật Kết Hợp Lớp (Class Association Rule — CAR)

**Định nghĩa:** **CAR** là trường hợp đặc biệt của luật kết hợp, trong đó vế phải chỉ chứa **duy nhất một class item**:
$$
R : P \Rightarrow c, \qquad P \subseteq \mathcal{I}, \quad c \in \mathcal{C}, \quad P \ne \emptyset
$$

- $P$ — **condset** (điều kiện)
- $c$ — **class label** (nhãn lớp dự đoán)

**Đọc luật:** "Nếu bản ghi khớp pattern $P$ thì dự đoán lớp $c$."

**Ví dụ:**
$$
R_1 : \{\text{milk=1}\} \Rightarrow \text{mammal}
$$
$$
R_2 : \{\text{outlook=sunny}, \text{humidity=high}\} \Rightarrow \text{play=no}
$$

**Bản ghi khớp luật:** $t$ khớp luật $R : P \Rightarrow c$ khi $t$ khớp $P$ (không quan tâm class của $t$).

**Luật dự đoán đúng (correctly covers):** $t$ **khớp luật và đồng thời có class đúng**:
$$
R \text{ phân lớp đúng } t \iff P \subseteq \text{items}(t) \wedge \text{class}(t) = c
$$

---

## 3. Support (Độ Hỗ Trợ) — Chi Tiết Đầy Đủ

### 3.1 Support của một itemset

**Định nghĩa (support tuyệt đối):** Số bản ghi trong $T$ khớp pattern $P$:
$$
\boxed{\text{sup}(P) = \big|\{t \in T : P \subseteq \text{items}(t)\}\big|}
$$

**Định nghĩa (support tương đối / relative support):** Tỉ lệ:
$$
\sigma(P) = \frac{\text{sup}(P)}{|T|} \in [0, 1]
$$

**Ký hiệu khác thường gặp:** $\text{supp}(P)$, $\text{count}(P)$, $\sigma(P)$, $s(P)$.

### 3.2 Support của một luật

**Định nghĩa:** Support của luật $R: X \Rightarrow Y$ là số bản ghi khớp **cả hai vế**:
$$
\boxed{\text{sup}(R) = \text{sup}(X \cup Y) = \big|\{t \in T : X \cup Y \subseteq \text{items}(t)\}\big|}
$$

**Với CAR $R: P \Rightarrow c$** (coi class như một item đặc biệt):
$$
\text{sup}(R) = \big|\{t \in T : P \subseteq \text{items}(t) \wedge \text{class}(t) = c\}\big|
$$

**Support tương đối của luật:**
$$
\sigma(R) = \frac{\text{sup}(R)}{|T|}
$$

### 3.3 Hai quan điểm về support

| Quan điểm | Cách nói | Ứng dụng |
|-----------|----------|---------|
| **Tần suất** | "Có bao nhiêu bản ghi thoả luật?" | Đánh giá độ phổ biến |
| **Xác suất** | $\sigma(R) = \hat{P}(X \cup Y)$ (xác suất thực nghiệm) | Liên kết với lý thuyết xác suất |

### 3.4 Tính chất quan trọng của support

**Tính chất 1 (Anti-monotone / Apriori property):**
$$
P \subseteq Q \implies \text{sup}(P) \ge \text{sup}(Q)
$$

**Diễn giải:** Thêm item vào pattern → support **chỉ có thể giảm hoặc bằng**, không tăng.

**Hệ quả (được dùng để cắt tỉa):** Nếu một itemset $P$ không đạt ngưỡng minSup thì **mọi siêu tập** của $P$ cũng không đạt → có thể loại bỏ ngay.

**Tính chất 2 (Giới hạn):**
$$
0 \le \text{sup}(P) \le |T|, \quad 0 \le \sigma(P) \le 1
$$

**Tính chất 3 (Itemset rỗng):**
$$
\text{sup}(\emptyset) = |T|, \quad \sigma(\emptyset) = 1
$$
(Mọi bản ghi đều khớp tập rỗng.)

### 3.5 Ngưỡng minSup

**Định nghĩa:** minSup là ngưỡng tối thiểu mà support phải vượt qua để một itemset/luật được coi là **phổ biến (frequent)**.

**Hai cách chỉ định:**
- **Tuyệt đối:** minSup = 50 (số bản ghi)
- **Tương đối:** minSupRel = 1% (tỉ lệ so với $|T|$)

**Quy đổi:**
$$
\text{minSup}_{\text{abs}} = \lceil \text{minSup}_{\text{rel}} \times |T| \rceil
$$

**Itemset phổ biến (frequent itemset):**
$$
P \text{ phổ biến} \iff \text{sup}(P) \ge \text{minSup}
$$

**Luật phổ biến:** tương tự, $\text{sup}(R) \ge \text{minSup}$.

### 3.6 Ví dụ tính support

**Dataset nhỏ (5 bản ghi):**

| ID | items | class |
|:--:|-------|:-----:|
| 1 | {a, b, c} | X |
| 2 | {a, b} | Y |
| 3 | {a, c} | X |
| 4 | {b, c} | X |
| 5 | {a, b, c, d} | X |

- $\text{sup}(\{a\}) = 4$ (bản ghi 1, 2, 3, 5)
- $\text{sup}(\{b\}) = 4$ (bản ghi 1, 2, 4, 5)
- $\text{sup}(\{a, b\}) = 3$ (bản ghi 1, 2, 5)
- $\text{sup}(\{a, b, c\}) = 2$ (bản ghi 1, 5) — giảm khi thêm item (minh hoạ anti-monotone)
- $\text{sup}(\{d\}) = 1$ (chỉ bản ghi 5)
- $\text{sup}(R: \{a\} \Rightarrow X) = 3$ (bản ghi 1, 3, 5 — đều có a **và** class X)

**Support tương đối:**
- $\sigma(\{a\}) = 4/5 = 0.8 = 80\%$
- $\sigma(R: \{a\} \Rightarrow X) = 3/5 = 0.6 = 60\%$

**Lọc theo minSup = 3:** giữ lại $\{a\}, \{b\}, \{c\}, \{a,b\}$, loại $\{d\}$ và các tập chứa $d$.

---

## 4. Confidence (Độ Tin Cậy) — Chi Tiết Đầy Đủ

### 4.1 Định nghĩa

**Công thức:**
$$
\boxed{\text{conf}(R : X \Rightarrow Y) = \frac{\text{sup}(X \cup Y)}{\text{sup}(X)} = \frac{\text{sup}(R)}{\text{sup}(X)}}
$$

**Với CAR $R: P \Rightarrow c$:**
$$
\text{conf}(R) = \frac{\text{sup}(P \cup \{c\})}{\text{sup}(P)} = \frac{\#\{t : P \subseteq t \wedge \text{class}(t) = c\}}{\#\{t : P \subseteq t\}}
$$

### 4.2 Ý nghĩa xác suất — Confidence chính là xác suất có điều kiện

Công thức trên **chính xác** là ước lượng xác suất có điều kiện theo kinh nghiệm:
$$
\text{conf}(X \Rightarrow Y) = \hat{P}(Y \mid X) = \frac{\hat{P}(X \cap Y)}{\hat{P}(X)}
$$

**Đọc luật:**
- conf = 0.90 → "Trong các bản ghi khớp $X$, **90%** có $Y$."
- conf = 0.50 → "Trong các bản ghi khớp $X$, 50% có $Y$" (mức ngẫu nhiên với 2 class cân bằng).
- conf = 1.00 → "Hễ khớp $X$ là **chắc chắn** có $Y$" (trong training set).

### 4.3 Phạm vi giá trị

$$
0 \le \text{conf}(R) \le 1
$$

**Biên:**
- conf = 0 → luật "sai hoàn toàn": không bản ghi nào khớp X có class c
- conf = 1 → luật "đúng tuyệt đối" trên tập huấn luyện

**Chú ý:** conf = 1 trên training **không có nghĩa** là đúng 100% trên tập mới (có thể overfitting, nhất là khi sup nhỏ).

### 4.4 Confidence KHÔNG đối xứng

Khác với một số độ đo tương quan (chẳng hạn lift), **confidence phụ thuộc chiều của luật**:
$$
\text{conf}(X \Rightarrow Y) \ne \text{conf}(Y \Rightarrow X)
$$

**Ví dụ:** Trong 5 bản ghi:
- $\text{sup}(\{a\}) = 4$, $\text{sup}(\{d\}) = 1$, $\text{sup}(\{a, d\}) = 1$
- $\text{conf}(\{a\} \Rightarrow \{d\}) = 1/4 = 0.25$
- $\text{conf}(\{d\} \Rightarrow \{a\}) = 1/1 = 1.00$

Chiều từ `d → a` mạnh hơn rất nhiều chiều ngược lại.

### 4.5 Hạn chế của confidence — Paradox "confidence cao nhưng luật vô nghĩa"

**Vấn đề:** Confidence chỉ xét **chiều X → Y**, không so với mức baseline của $Y$.

**Ví dụ nổi tiếng:**

| Bản ghi | Khớp "cà phê" | Không khớp | Tổng |
|---------|:-------------:|:----------:|:----:|
| Khớp "trà" | 150 | 50 | 200 |
| Không khớp | 750 | 50 | 800 |
| **Tổng** | **900** | **100** | **1000** |

- $\text{conf}(\text{trà} \Rightarrow \text{cà phê}) = 150/200 = 75\%$

Nhìn vào mình `75%` có vẻ "cao". Nhưng **xác suất nền** của "cà phê" là $P(\text{cà phê}) = 900/1000 = 90\%$. Tức là:
- Không biết gì, đoán ngẫu nhiên "có cà phê" → đúng 90%.
- Biết "có trà" → đoán "có cà phê" → chỉ đúng 75%.

**Biết "có trà" thật ra làm GIẢM cơ hội có cà phê!** Luật này sai bản chất dù confidence "cao". Đây là lý do cần các độ đo bổ sung như **lift** và **χ²**.

### 4.6 Ngưỡng minConf

**Định nghĩa:** minConf là ngưỡng confidence tối thiểu.

**Luật hợp lệ (valid rule):** vừa đạt minSup, vừa đạt minConf:
$$
R \text{ hợp lệ} \iff \text{sup}(R) \ge \text{minSup} \wedge \text{conf}(R) \ge \text{minConf}
$$

**Giá trị thường dùng:** 50% (mặc định), 70–80% (chặt), 90%+ (rất chặt).

**Chú ý:** minConf thấp → nhiều luật "yếu"; minConf cao → ít luật nhưng chắc chắn hơn → có thể thiếu luật cho minority class.

---

## 5. Các Độ Đo Liên Quan (Lift, Leverage, Conviction, χ²)

Support và confidence là hai độ đo truyền thống, nhưng **chưa đủ** để đánh giá chất lượng một luật. CMAR và các thuật toán hiện đại bổ sung thêm các độ đo sau.

### 5.1 Lift (Interest)

**Công thức:**
$$
\text{lift}(X \Rightarrow Y) = \frac{\text{conf}(X \Rightarrow Y)}{\sigma(Y)} = \frac{\sigma(X \cup Y)}{\sigma(X) \cdot \sigma(Y)}
$$

**Ý nghĩa xác suất:**
$$
\text{lift} = \frac{P(Y \mid X)}{P(Y)}
$$

**Diễn giải:**
- lift = 1 → $X$ và $Y$ **độc lập** (biết $X$ không giúp dự đoán $Y$).
- lift > 1 → tương quan **dương** ($X$ làm tăng xác suất $Y$).
- lift < 1 → tương quan **âm** ($X$ làm giảm xác suất $Y$).

**Tính chất:** **Đối xứng** — $\text{lift}(X \Rightarrow Y) = \text{lift}(Y \Rightarrow X)$.

**Quay lại ví dụ trà-cà phê:**
- $\text{lift} = 0.75 / 0.90 = 0.833 < 1$ → tương quan âm → luật bị "bỏ đi" nếu dùng ngưỡng lift ≥ 1.

### 5.2 Leverage (PS — Piatetsky-Shapiro)

**Công thức:**
$$
\text{lev}(X \Rightarrow Y) = \sigma(X \cup Y) - \sigma(X) \cdot \sigma(Y)
$$

**Ý nghĩa:** Hiệu giữa xác suất quan sát $P(X, Y)$ và xác suất kỳ vọng nếu $X, Y$ độc lập.

**Phạm vi:** $[-0.25, 0.25]$

**Diễn giải:**
- lev = 0 → độc lập
- lev > 0 → tương quan dương (càng gần 0.25 càng mạnh)
- lev < 0 → tương quan âm

### 5.3 Conviction

**Công thức:**
$$
\text{conv}(X \Rightarrow Y) = \frac{1 - \sigma(Y)}{1 - \text{conf}(X \Rightarrow Y)}
$$

**Diễn giải:**
- conv = 1 → độc lập
- conv → ∞ → luật "hoàn hảo"
- conv < 1 → tương quan âm

**Tính chất:** Không đối xứng, giúp khắc phục hạn chế của lift ở luật có confidence = 1 (lift bị vô hạn trong 1 trường hợp, conviction có thể xử lý).

### 5.4 χ² (Chi-square) — Trụ cột của CMAR

**Bảng contingency 2×2** cho luật $R : P \Rightarrow c$:

|              | class = $c$ | class ≠ $c$ | **tổng hàng** |
|--------------|:-----------:|:-----------:|:-------------:|
| Khớp $P$ | $a$ | $b$ | $a + b$ |
| Không khớp $P$ | $\gamma$ | $d$ | $\gamma + d$ |
| **tổng cột** | $a + \gamma$ | $b + d$ | $n = \|T\|$ |

Trong đó:
- $a$ = số bản ghi khớp $P$ **và** có lớp $c$ = $\text{sup}(R)$
- $b$ = số bản ghi khớp $P$ **nhưng** không có lớp $c$ = $\text{sup}(P) - a$
- $\gamma$ = số bản ghi không khớp $P$ **nhưng** có lớp $c$ = $\text{sup}(c) - a$
- $d$ = số bản ghi không khớp $P$ **và** không có lớp $c$ = $n - a - b - \gamma$

*(Dùng ký hiệu γ thay vì c để tránh nhầm với class label c)*

**Giá trị kỳ vọng (nếu $P$ và $c$ độc lập):**
$$
E_{11} = \frac{(a+b)(a+\gamma)}{n}, \quad E_{12} = \frac{(a+b)(b+d)}{n}
$$
$$
E_{21} = \frac{(\gamma+d)(a+\gamma)}{n}, \quad E_{22} = \frac{(\gamma+d)(b+d)}{n}
$$

**Công thức tổng quát:**
$$
\chi^2 = \sum_{i,j} \frac{(O_{ij} - E_{ij})^2}{E_{ij}}
$$

**Công thức gọn cho bảng 2×2:**
$$
\boxed{\chi^2 = \frac{n (ad - b\gamma)^2}{(a+b)(\gamma+d)(a+\gamma)(b+d)}}
$$

**Điều kiện tương quan dương:**
$$
ad > b\gamma \iff \frac{a}{a+b} > \frac{a+\gamma}{n}
$$

Nghĩa là: xác suất khớp class $c$ khi biết $P$ > xác suất khớp $c$ nói chung.

**Bảng ngưỡng χ² (với df = 1):**

| Mức ý nghĩa α | χ² threshold | Ý nghĩa |
|:-------------:|:------------:|---------|
| 0.10 | 2.706 | 90% tin cậy |
| **0.05** | **3.841** | **95% tin cậy (mặc định CMAR)** |
| 0.01 | 6.635 | 99% tin cậy |
| 0.001 | 10.827 | 99.9% tin cậy |

**Vì sao dùng χ² trong CMAR:**
1. **Đo ý nghĩa thống kê** — luật có đủ mạnh để không phải ngẫu nhiên.
2. **Không cần chuyển sang xác suất** — tính trực tiếp từ count.
3. **Có thể chặn trên** — tính được $\max\chi^2$ → normalize cho weighted score.

### 5.5 max χ² (Cận trên lý thuyết)

**Ý tưởng:** Với các giá trị $\text{sup}(P)$ và $\text{sup}(c)$ cho trước, tính giá trị χ² **tối đa** có thể đạt được (giả sử mọi bản ghi khớp $P$ đều thuộc class $c$).

Khi lý tưởng, $a$ đạt giá trị lớn nhất có thể:
$$
a_{\max} = \min(\text{sup}(P), \text{sup}(c))
$$

Rồi:
$$
b = \text{sup}(P) - a_{\max}, \quad \gamma = \text{sup}(c) - a_{\max}, \quad d = n - a_{\max} - b - \gamma
$$

Áp dụng công thức χ²:
$$
\max\chi^2 = \frac{n(a_{\max} d - b\gamma)^2}{(a_{\max}+b)(\gamma+d)(a_{\max}+\gamma)(b+d)}
$$

**Vai trò trong CMAR:** dùng để chuẩn hoá:
$$
\frac{\chi^2(R)}{\max\chi^2(R)} \in [0, 1]
$$

Tỉ số này được dùng trong weighted χ² để **so sánh công bằng giữa các luật** bất kể kích thước class.

---

## 6. Ví Dụ Tính Toán Tổng Hợp Mọi Độ Đo

### 6.1 Dataset

| ID | items | class |
|:--:|-------|:-----:|
| 1 | {a, b, c} | X |
| 2 | {a, b} | Y |
| 3 | {a, c} | X |
| 4 | {b, c} | X |
| 5 | {a, b, c} | X |
| 6 | {a, b, d} | Y |
| 7 | {b, c, d} | X |
| 8 | {a, c, d} | X |
| 9 | {b, d} | Y |
| 10 | {a, b, c, d} | X |

$|T| = 10$, $\text{sup}(X) = 7$, $\text{sup}(Y) = 3$.

### 6.2 Các support cần thiết

| itemset | support | $\sigma$ (tương đối) |
|:-------:|:-------:|:---------------------:|
| $\{a\}$ | 7 | 0.70 |
| $\{b\}$ | 8 | 0.80 |
| $\{c\}$ | 7 | 0.70 |
| $\{d\}$ | 5 | 0.50 |
| $\{a, b\}$ | 5 | 0.50 |
| $\{a, c\}$ | 5 | 0.50 |
| $\{a, b, c\}$ | 3 | 0.30 |
| $\{b, c\}$ | 5 | 0.50 |
| class X | 7 | 0.70 |
| class Y | 3 | 0.30 |

### 6.3 Xét luật $R_1 : \{a, b\} \Rightarrow X$

**Bước 1 — Đếm $a, b, \gamma, d$:**

| | class=X | class=Y | tổng |
|-|:-------:|:-------:|:----:|
| Khớp $\{a,b\}$ | $a = 3$ | $b = 2$ | 5 |
| Không khớp | $\gamma = 4$ | $d = 1$ | 5 |
| **tổng** | 7 | 3 | 10 |

Giải thích các giá trị:
- $a = \text{sup}(R_1) = 3$ (bản ghi 1, 5, 10 — có a, b, class X)
- $b = \text{sup}(\{a,b\}) - a = 5 - 3 = 2$ (bản ghi 2, 6 — có a, b, class Y)
- $\gamma = \text{sup}(X) - a = 7 - 3 = 4$ (bản ghi 3, 4, 7, 8 — class X nhưng không có cả a,b)
- $d = 10 - 3 - 2 - 4 = 1$ (bản ghi 9 — class Y không có cả a,b)

**Bước 2 — Support và confidence:**
$$
\text{sup}(R_1) = 3, \quad \sigma(R_1) = 0.30
$$
$$
\text{conf}(R_1) = \frac{3}{5} = 0.60 = 60\%
$$

**Bước 3 — Lift:**
$$
\text{lift}(R_1) = \frac{\text{conf}(R_1)}{\sigma(X)} = \frac{0.60}{0.70} \approx 0.857
$$

**Lift < 1** → biết $\{a, b\}$ thật ra làm **giảm** cơ hội class X so với baseline. Đây là luật **yếu**, dù conf 60%.

**Bước 4 — Leverage:**
$$
\text{lev}(R_1) = 0.30 - 0.50 \times 0.70 = 0.30 - 0.35 = -0.05
$$

Âm → tương quan âm.

**Bước 5 — Conviction:**
$$
\text{conv}(R_1) = \frac{1 - 0.70}{1 - 0.60} = \frac{0.30}{0.40} = 0.75 < 1 \rightarrow \text{tương quan âm}
$$

**Bước 6 — Chi-square:**
$$
\chi^2 = \frac{10 \cdot (3 \cdot 1 - 2 \cdot 4)^2}{5 \cdot 5 \cdot 7 \cdot 3} = \frac{10 \cdot (-5)^2}{525} = \frac{250}{525} \approx 0.476
$$

$\chi^2 = 0.476 < 3.841$ → **không** đạt ý nghĩa thống kê.

**Điều kiện tương quan dương:** $ad = 3 < b\gamma = 8$ → **tương quan âm**.

**Kết luận $R_1$:** Mặc dù conf = 60% có vẻ ổn, **mọi độ đo khác (lift, leverage, conviction, χ²)** đều cho thấy $R_1$ **yếu hoặc tương quan âm**. CMAR Pruning 2 sẽ **loại** luật này.

### 6.4 Xét luật $R_2 : \{b, d\} \Rightarrow Y$

**Sup cần thiết:**
- $\text{sup}(\{b, d\}) = 3$ (bản ghi 6, 9, 10)
- Trong đó class Y: bản ghi 6, 9 → $a = 2$

**Bảng:**

| | class=Y | class=X | tổng |
|-|:-------:|:-------:|:----:|
| Khớp $\{b,d\}$ | $a = 2$ | $b = 1$ | 3 |
| Không khớp | $\gamma = 1$ | $d = 6$ | 7 |
| **tổng** | 3 | 7 | 10 |

$\text{conf}(R_2) = 2/3 \approx 0.667 = 66.7\%$

$\text{lift}(R_2) = \frac{0.667}{0.30} \approx 2.22$

Lift > 1 → tương quan **dương**.

$\chi^2 = \frac{10 (2 \cdot 6 - 1 \cdot 1)^2}{3 \cdot 7 \cdot 3 \cdot 7} = \frac{10 \cdot 121}{441} \approx 2.74$

$\chi^2 = 2.74 < 3.841$ → chưa đạt ngưỡng 95%, nhưng đạt ngưỡng 90% (2.706).

### 6.5 Bảng tổng hợp các độ đo

| Luật | sup | σ | conf | lift | lev | conv | χ² | χ²≥3.841? | $ad>b\gamma$? |
|------|:---:|:---:|:----:|:----:|:----:|:----:|:---:|:---------:|:-------------:|
| $\{a,b\} \Rightarrow X$ | 3 | 0.30 | 0.60 | 0.857 | -0.05 | 0.75 | 0.476 | ✗ | ✗ |
| $\{b,d\} \Rightarrow Y$ | 2 | 0.20 | 0.667 | 2.22 | +0.11 | 2.10 | 2.74 | ✗ | ✓ |
| $\{a,c\} \Rightarrow X$ | 5 | 0.50 | 1.00 | 1.43 | +0.15 | ∞ | 4.29 | ✓ | ✓ |
| $\{c\} \Rightarrow X$ | 6 | 0.60 | 0.857 | 1.22 | +0.11 | 2.10 | 2.35 | ✗ | ✓ |

**Bài học:**
- $\{a,b\} \Rightarrow X$: conf trông được nhưng tất cả độ đo khác đều âm → **luật dỏm**, bị loại.
- $\{a,c\} \Rightarrow X$: conf = 100%, tất cả độ đo dương, χ² vượt 3.841 → **luật tốt**, được giữ.

---

## 7. Ngưỡng & Quan Hệ Giữa Các Luật

### 7.1 Rule Precedence (Thứ tự ưu tiên)

**Định nghĩa:** Với hai luật $R_1, R_2$, ta nói $R_1 \succ R_2$ ("R₁ ưu tiên hơn R₂") nếu thoả lần lượt 3 tiêu chí sau (tiêu chí trước quyết định trước):

$$
R_1 \succ R_2 \iff
\begin{cases}
\text{conf}(R_1) > \text{conf}(R_2), \\
\qquad \text{hoặc}\\
\text{conf}(R_1) = \text{conf}(R_2) \text{ và } \text{sup}(R_1) > \text{sup}(R_2), \\
\qquad \text{hoặc}\\
\text{conf}(R_1) = \text{conf}(R_2), \text{sup}(R_1) = \text{sup}(R_2), \\
\qquad \text{và } |P_1| < |P_2|
\end{cases}
$$

**Thứ tự (priority order):**
1. **Confidence giảm dần** (luật chắc chắn hơn được ưu tiên)
2. **Support giảm dần** (luật có nhiều bằng chứng hơn được ưu tiên)
3. **Kích thước condset tăng dần** (luật đơn giản hơn được ưu tiên — Occam's razor)

**Ví dụ so sánh:**

| Luật | conf | sup | \|P\| |
|------|:----:|:---:|:-----:|
| $R_A$ | 95% | 100 | 2 |
| $R_B$ | 95% | 100 | 3 |
| $R_C$ | 95% | 80 | 2 |
| $R_D$ | 90% | 200 | 1 |

- $R_A \succ R_B$ (cùng conf, sup → so size: A nhỏ hơn)
- $R_A \succ R_C$ (cùng conf → A có sup cao hơn)
- $R_A \succ R_D$ (A có conf cao hơn, bất kể sup và size)

### 7.2 Luật tổng quát / chi tiết

**Định nghĩa:** $R_1 : P_1 \Rightarrow c_1$ **tổng quát hơn** (more general than) $R_2 : P_2 \Rightarrow c_2$ khi và chỉ khi:
$$
P_1 \subseteq P_2
$$

Khi đó $R_2$ là **chi tiết hơn** (more specific than) $R_1$.

**Ví dụ:**

| $R_1$ (tổng quát) | $R_2$ (chi tiết) |
|-------------------|------------------|
| $\{\text{milk=1}\} \Rightarrow \text{mammal}$ | $\{\text{milk=1}, \text{hair=1}\} \Rightarrow \text{mammal}$ |
| $\{\text{a}\} \Rightarrow X$ | $\{\text{a}, \text{b}\} \Rightarrow X$ |

**Chú ý:** Quan hệ tổng quát/chi tiết **không bắt buộc cùng class** — tuy nhiên pruning 1 của CMAR chỉ xét cùng class.

### 7.3 Luật dư thừa (redundant)

**Định nghĩa:** $R_2$ được gọi là **dư thừa** đối với $R_1$ nếu:
- $R_1$ tổng quát hơn $R_2$
- Cùng class: $c_1 = c_2$
- $R_1 \succ R_2$ (ưu tiên cao hơn hoặc bằng)

**Diễn giải:** Luật đơn giản và mạnh hơn đã "thay thế" được luật chi tiết → giữ luật chi tiết là **lãng phí** + làm rối mô hình.

**Ý tưởng:** Bài báo CMAR áp dụng ý tưởng này làm **Pruning tầng 1** (xem §10.1).

### 7.4 Luật mâu thuẫn (conflicting)

**Định nghĩa:** $R_1, R_2$ **mâu thuẫn** nếu chúng có điều kiện chồng lấp và class khác nhau:
$$
P_1 \cap P_2 \ne \emptyset \text{ (hoặc } P_1 \subseteq P_2\text{)} \quad \text{nhưng} \quad c_1 \ne c_2
$$

Khi một bản ghi khớp cả hai, cần **cơ chế quyết định**.

**Chiến lược của CMAR:** Weighted χ² — nhóm luật theo class, tính tổng sức mạnh, chọn class có score cao nhất (chi tiết §11).

### 7.5 Bảng tổng hợp các quan hệ

| Quan hệ | Ký hiệu | Điều kiện |
|---------|:-------:|-----------|
| Tổng quát hơn | $R_1 \preceq_{\text{gen}} R_2$ | $P_1 \subseteq P_2$ |
| Ưu tiên hơn | $R_1 \succ R_2$ | conf / sup / size (xem §7.1) |
| Dư thừa | $R_2$ dư thừa vs $R_1$ | $P_1 \subseteq P_2$, $c_1 = c_2$, $R_1 \succ R_2$ |
| Mâu thuẫn | $R_1 \otimes R_2$ | $P_1 \cap P_2 \ne \emptyset$, $c_1 \ne c_2$ |

---

# PHẦN B — THUẬT TOÁN CMAR

---

## 8. Tổng Quan Thuật Toán CMAR

### 8.1 Triết lý thiết kế CMAR

CMAR được thiết kế dựa trên **3 nguyên tắc triết lý**:

**Nguyên tắc 1 — "Hỏi nhiều nhân chứng thay vì chỉ 1"**

Khi có nhiều luật khớp với bản ghi test, CMAR **không** chọn 1 luật "đáng tin nhất" như CBA, mà **tổng hợp ý kiến** của tất cả luật khớp. Giống như toà án: kết luận cuối dựa trên lời khai nhiều nhân chứng, không phải chỉ 1.

**Nguyên tắc 2 — "Sinh luật CÙNG LÚC với đào mỏ"**

Các phương pháp cũ (CBA dùng Apriori) tách biệt 2 pha:
- Pha 1: Đào mỏ frequent pattern
- Pha 2: Từ patterns sinh ra luật

CMAR **gộp 2 pha làm 1** bằng cách lưu phân bố class ngay tại mỗi node của FP-tree. Ưu điểm: nhanh hơn, dùng ít bộ nhớ hơn.

**Nguyên tắc 3 — "Cắt tỉa nhiều tầng, mỗi tầng một tiêu chí"**

Thay vì một ngưỡng duy nhất, CMAR dùng **3 tầng pruning** — mỗi tầng loại một loại luật "xấu" khác nhau:
- P1 loại luật **dư thừa** (bị luật tổng quát lấn át).
- P2 loại luật **không có ý nghĩa thống kê** (χ² thấp / tương quan âm).
- P3 loại luật **không còn đóng góp** (training data đã đủ cover).

Kết quả: từ hàng nghìn ứng viên còn lại vài chục luật "tinh hoa".

### 8.2 Tại sao chia thành 2 giai đoạn?

**Giai đoạn I (offline, chạy một lần):**
- Tốn thời gian nhất — đọc toàn bộ tập training, đào mỏ, pruning.
- Kết quả = một "mô hình" đóng gói sẵn (CR-tree + defaultClass).
- Chỉ chạy **một lần** khi huấn luyện.

**Giai đoạn II (online, chạy nhiều lần):**
- Nhanh — chỉ cần truy vấn mô hình đã có.
- Chạy **mỗi lần** có bản ghi mới cần dự đoán.

**Ẩn dụ:** Giống như nấu phở — chuẩn bị nước dùng (Giai đoạn I) tốn nhiều giờ nhưng làm một lần. Múc phở cho khách (Giai đoạn II) chỉ vài phút mỗi bát.

### 8.3 Sơ đồ kiến trúc 2 giai đoạn

```
╔══════════════════════════════════════════════════════════════════════╗
║  GIAI ĐOẠN I — RULE GENERATION (§9, §10)    [TRAIN: chạy 1 lần]     ║
║                                                                      ║
║  Input : Tập huấn luyện T                                            ║
║  Output: CR-tree chứa tập luật CAR tinh gọn + defaultClass           ║
║                                                                      ║
║  Các bước:                                                           ║
║  (1) Đếm tần số item, lọc theo minSup                                ║
║  (2) Xây CR-tree khởi tạo (có phân bố class tại mỗi node)           ║
║  (3) FP-Growth đệ quy → phát hiện pattern                           ║
║  (4) Sinh CAR trực tiếp (dùng classCount của header chain)          ║
║  (5) Pruning 1 — loại luật dư thừa (general dominated specific)     ║
║  (6) Pruning 2 — loại luật có χ² thấp / tương quan âm               ║
║  (7) Pruning 3 — Database coverage (thuật toán 1)                   ║
║  (8) Chèn luật sống sót vào CR-tree lưu trữ                          ║
╚══════════════════════════════════════════════════════════════════════╝
                                   │
                                   ▼
╔══════════════════════════════════════════════════════════════════════╗
║  GIAI ĐOẠN II — CLASSIFICATION (§11)       [PREDICT: chạy nhiều lần]║
║                                                                      ║
║  Input : Bản ghi mới t                                               ║
║  Output: Class dự đoán ĉ                                              ║
║                                                                      ║
║  Các bước:                                                           ║
║  (a) Truy vấn CR-tree → tập luật khớp R(t)                          ║
║  (b) R(t) = ∅  →  trả defaultClass                                   ║
║  (c) R(t) đồng nhất 1 class  →  trả class đó                        ║
║  (d) R(t) đa class  →  tính weighted χ² → argmax                    ║
╚══════════════════════════════════════════════════════════════════════╝
```

### 8.4 Bảng input/output chi tiết mỗi bước

| # | Bước | Input | Output | Độ phức tạp |
|:-:|------|-------|--------|:-----------:|
| 1 | Đếm item | T | freq[item], freq[class] | $O(\|T\| \cdot \bar{L})$ |
| 2 | Xây CR-tree | T, F-list | CR-tree khởi tạo | $O(\|T\| \cdot \bar{L})$ |
| 3 | FP-Growth | CR-tree | Frequent patterns | $O(N_{pat})$ |
| 4 | Sinh CAR | Patterns, classCount | $N_0$ ứng viên | $O(N_{pat} \cdot \|\mathcal{C}\|)$ |
| 5 | Pruning 1 | $N_0$ | $N_1$ | $O(N_0^2)$ worst |
| 6 | Pruning 2 | $N_1$ | $N_2$ | $O(N_1)$ |
| 7 | Pruning 3 | $N_2$, T | $N_3$ | $O(N_2 \cdot \|T\|)$ |
| 8 | Xây CR-tree | $N_3$ rules | CR-tree lưu trữ | $O(N_3 \cdot \bar{k})$ |
| 9 | Classify | t, CR-tree | $\hat{c}$ | $O(\|t\|) + O(M)$ |

Trong đó: $\bar{L}$ = độ dài trung bình transaction, $\bar{k}$ = độ dài trung bình condset, $M$ = số luật khớp test record.

### 8.5 So sánh kiến trúc CMAR vs các đối thủ

| Thuộc tính | C4.5 | CBA (Apriori) | **CMAR** |
|------------|:----:|:-------------:|:--------:|
| Số pass qua dữ liệu | 1 | 2 (mine → extract rule) | **1** |
| Cấu trúc đào mỏ | Cây quyết định | Candidate generation | **FP-tree mở rộng** |
| Số luật dùng classify | 1 (đường đi cây) | 1 (best rule) | **Nhiều (weighted)** |
| Lưu trữ luật | Cây quyết định | List of rules | **CR-tree nén** |
| Pruning | Giảm cây | Database cover (δ=1) | **3 tầng + δ=4** |
| Classification | Traverse cây | Best rule | **Weighted χ²** |

### 8.6 Pseudo-code tổng thể

```
// ═══════════════════════════════════════════════════════════
// HUẤN LUYỆN
// ═══════════════════════════════════════════════════════════
FUNCTION CMAR_TRAIN(T, minSup, minConf, χ²_thr, δ):
    // 1. Đếm tần số
    freqItem[] ← đếm mỗi item trên T
    freqClass[] ← đếm mỗi class trên T
    defaultClass ← class có freq cao nhất

    // 2. Lọc & sắp F-list
    F ← {item : freqItem[item] ≥ minSup}
    F-list ← F sắp theo freq giảm dần

    // 3. Xây CR-tree khởi tạo
    tree ← CR-tree rỗng
    FOR mỗi transaction t ∈ T:
        path ← items(t) ∩ F, sắp theo F-list
        tree.insertPath(path, {class(t): 1}, 1)

    // 4. FP-Growth đệ quy + sinh CAR trực tiếp
    candidates ← []
    MINE_TREE(tree, prefix=[], candidates)

    // 5, 6, 7. Ba tầng pruning
    sắp candidates theo rule precedence
    afterP1 ← PRUNE_GENERAL(candidates)
    afterP2 ← PRUNE_CHI_SQUARE(afterP1, χ²_thr)
    finalRules ← PRUNE_COVERAGE(afterP2, T, δ)

    // 8. Xây CR-tree lưu trữ
    crTree ← CR-tree(freqItem)
    crTree.insertAll(finalRules)

    RETURN (crTree, defaultClass)

// ═══════════════════════════════════════════════════════════
// PHÂN LỚP
// ═══════════════════════════════════════════════════════════
FUNCTION CMAR_CLASSIFY(t, crTree, defaultClass):
    R_t ← crTree.findMatching(t)
    IF R_t = ∅: RETURN defaultClass

    byClass ← group R_t by class
    IF |byClass| = 1: RETURN class duy nhất

    // Weighted χ²
    bestCls ← defaultClass; bestScore ← -∞
    FOR mỗi (cls, group) ∈ byClass:
        score ← 0
        FOR mỗi rule R ∈ group:
            chi2 ← χ²(R, cls)
            maxChi2 ← maxχ²(R, cls)
            IF maxChi2 > 0:
                score += (chi2 * chi2) / maxChi2
        IF score > bestScore:
            bestScore, bestCls ← score, cls

    RETURN bestCls
```

---

## 9. Giai Đoạn I — Sinh Luật Phân Lớp

### 9.0 Tổng quan Giai Đoạn I

Giai đoạn I gồm **4 bước lớn** được cài đặt nối tiếp nhau:

```
┌─────────────────────────────────────────────────────────────────┐
│  Bước 1: ĐẾM & LỌC                                              │
│    Đếm tần số mỗi item, loại item dưới ngưỡng minSup             │
│    → Đồng thời xác định defaultClass                            │
├─────────────────────────────────────────────────────────────────┤
│  Bước 2: XÂY CR-TREE KHỞI TẠO                                   │
│    Nén toàn bộ training set thành một cây prefix                │
│    Mỗi node kèm classCount (phân bố class)                      │
├─────────────────────────────────────────────────────────────────┤
│  Bước 3: FP-GROWTH ĐỆ QUY                                       │
│    Chia để trị: đào mỏ từng item, xây conditional tree,         │
│    đệ quy → phát hiện mọi frequent pattern                      │
├─────────────────────────────────────────────────────────────────┤
│  Bước 4: SINH CAR TRỰC TIẾP                                     │
│    Ngay khi tìm thấy pattern, dùng classCount đã có để          │
│    tạo luật P ⇒ c nếu vượt minSup & minConf                     │
└─────────────────────────────────────────────────────────────────┘
```

Đầu ra của Giai Đoạn I là **tập CAR ứng viên** — sẽ được cắt tỉa ở Giai Đoạn II (§10).

### 9.1 Bước 1 — Đếm tần số, lọc item, và xác định defaultClass

#### 9.1.1 Đếm tần số item và class

Duyệt toàn bộ training set $T$ một lần:
- **freq[item]**: với mỗi item (ví dụ `odor=f`), đếm số transaction chứa nó.
- **freq[class]**: với mỗi nhãn lớp (ví dụ `edible`), đếm số transaction có class đó.

**Ví dụ trên Mushroom (8124 records):**

| Item / Class | Count |
|--------------|:-----:|
| `odor=n` | 3528 |
| `odor=f` | 2160 |
| `odor=a` | 400 |
| `class=edible` | 4208 |
| `class=poisonous` | 3916 |

#### 9.1.2 `defaultClass` — Lớp mặc định

**Định nghĩa:**
$$
\text{defaultClass} = \arg\max_{c \in \mathcal{C}} \text{freq}(c)
$$

**Nghĩa đen:** `defaultClass` là nhãn lớp **phổ biến nhất** trong training set.

**Tác dụng:** Khi phân lớp một bản ghi mới mà **không có luật nào khớp** (§11), CMAR "đoán đại" bằng class phổ biến nhất — chiến lược an toàn nhất khi không có thông tin.

**Ví dụ:**
- Mushroom: `edible` có 4208/8124 ≈ 51.8% → `defaultClass = edible`.
- Zoo: `mammal` chiếm ~41% (cao nhất) → `defaultClass = mammal`.
- Dataset balanced (2 class 50/50): chọn class nào cũng được (tuỳ cài đặt).

**Tại sao cần defaultClass?**

Hãy tưởng tượng bản ghi test có các giá trị mà không luật nào trong CMAR khớp được (ví dụ tất cả item của test record đều hiếm):
- Không trả gì → bộ phân lớp "bó tay" → lỗi.
- Trả random class → rủi ro 50/50.
- **Trả defaultClass** → ít nhất đúng với xác suất = tỉ lệ majority class → chiến lược tối ưu khi không biết gì.

**Ẩn dụ:** Như đoán đội thắng trong trận bóng khi không biết thông tin gì — đoán đội mạnh hơn (giải nhì đến giờ) luôn tốt hơn đoán ngẫu nhiên.

#### 9.1.3 Lọc item theo minSup → F-list

**Bước lọc:**
$$
\text{F} = \{\text{item} : \text{freq}(\text{item}) \ge \text{minSup}\}
$$

**F-list**: tập F được sắp **giảm dần theo tần số**:
$$
\text{F-list} = [\text{item}_1, \text{item}_2, \dots, \text{item}_k] \text{ với freq}(\text{item}_i) \ge \text{freq}(\text{item}_{i+1})
$$

**Ví dụ trên Mushroom (minSup = 1097 ≈ 15%):**

```
F-list (trình tự đã sắp):
  veil-color=w (6364)
  gill-attachment=f (6343)
  ring-number=o (5924)
  bruises=f (3936)
  odor=n (3528)
  stalk-shape=t (3480)
  ...
  [loại] odor=m (36)       ← dưới minSup
  [loại] cap-color=g (825)  ← giả sử vẫn dưới minSup
```

**Tại sao phải lọc?**

Item hiếm (tần số thấp) không bao giờ tạo ra luật đạt minSup — vì support của pattern chứa item hiếm **chỉ có thể thấp hơn** (tính chất anti-monotone §3.4). Lọc sớm giúp:
- Giảm kích thước FP-tree (nhanh hơn).
- Tiết kiệm bộ nhớ.
- Tránh tính toán vô ích trong FP-Growth.

**Tại sao sắp theo tần số giảm dần?**

Quy tắc cơ bản của FP-tree: **item phổ biến ở gần gốc**. Khi chèn transaction, các item đầu F-list (phổ biến) sẽ là tiền tố chung cho nhiều transaction → **chia sẻ node** tối đa → cây nén.

**Ví dụ minh hoạ:**
- Nếu F-list là `[A(100), B(90), C(50)]`, nhiều transaction có cả A và B → cùng đi qua `root → A → B` → chia sẻ.
- Nếu F-list là `[C(50), A(100), B(90)]` (sắp ngẫu nhiên), chỉ các transaction có C mới chia sẻ path → cây phình to vô nghĩa.

### 9.2 Bước 2 — Xây CR-tree khởi tạo

#### 9.2.1 CR-tree là gì?

**CR-tree** (Class-distribution-aware FP-tree) là **cây tiền tố** (prefix tree) mở rộng, dùng để **nén toàn bộ training set** thành một cấu trúc cây.

**Đặc điểm khác biệt so với FP-tree thường:**
> Mỗi node của CR-tree **gắn thêm** một trường `classCount` lưu phân bố class của các transaction đi qua node đó.

#### 9.2.2 Cấu trúc node chi tiết

Mỗi node trong CR-tree có 6 trường:

| Trường | Kiểu | Ý nghĩa | Ví dụ |
|--------|------|---------|-------|
| **`item`** | String | Tên item tại node | `"odor=f"` |
| **`count`** | Integer | Số transaction đi qua node này | `2160` |
| **`parent`** | Node | Node cha trong cây | `root` hoặc node khác |
| **`children`** | Map<String, Node> | Các node con theo item | `{"cap-shape=x": Node, ...}` |
| **`nodeLink`** | Node | Node tiếp theo **cùng item** trong header chain | (xem §9.2.4) |
| **`classCount`** | Map<String, Int> | **PHÂN BỐ CLASS** của transactions đi qua | `{"poisonous": 2160, "edible": 0}` |

**Node gốc (root):** có `item = null`, là điểm xuất phát của cây.

#### 9.2.3 `classCount` — Trường quan trọng nhất

**Định nghĩa:** `classCount[c]` tại node $n$ = số transaction thuộc class $c$ **đi qua node $n$** trên đường đi từ gốc.

**Tại sao cần?**

Giả sử ta đang đào mỏ pattern P = {A, B, C} và cần biết bao nhiêu bản ghi khớp P có class "positive". Nếu không có classCount:
- Phải **quét lại training set** → tốn O(|T|) mỗi lần.

Với classCount:
- Cộng dồn classCount tại các node "cuối path" của P → **O(số node)**.
- Nhanh hơn gấp nhiều lần.

**Ví dụ trên Mushroom:**
Một node `odor=f` (dưới `veil-color=w → gill-attachment=f → ... → odor=f`) có:
- `count = 2160`
- `classCount = {poisonous: 2160, edible: 0}`

Nhìn vào đây, ta biết **ngay lập tức**: mọi transaction có chuỗi attributes dẫn đến node này đều là poisonous → có thể sinh luật `... ⇒ poisonous` với conf rất cao.

#### 9.2.4 Header Table và Node-link Chain

**Vấn đề:** Sau khi xây cây, muốn tìm **tất cả node chứa item X** (để thu thập classCount, xây conditional tree), cần duyệt cả cây → chậm.

**Giải pháp: Header Table + Node-link Chain**

**Header Table:** một dict ghi cho mỗi item một con trỏ đến node **đầu tiên** chứa item đó:

```
Header Table:
  odor=f        → Node(A)
  cap-shape=x   → Node(B)
  ring-number=o → Node(C)
  ...
```

**Node-link Chain:** các node có cùng item được **liên kết thành chuỗi** qua trường `nodeLink`:

```
Node(A).nodeLink → Node(A')  → Node(A'')  → null
                        [cùng là odor=f]
```

**Hình dung:**

```
           root
          /    \
      A:10      A':5
     / │ \       │
   ... │ ...   ...
       B:3       
        │       
       ...      
                 
Header Table:
  A  →  node A (count=10)  ─nodeLink→  node A' (count=5)  ─nodeLink→  null
  B  →  node B (count=3)   ─nodeLink→  null
```

**Tác dụng:** Khi cần tất cả node của item `A`, chỉ cần đi theo chain bắt đầu từ `headerTable["A"]` → thu được toàn bộ node A trong O(số node A), không phải duyệt cả cây.

#### 9.2.5 Thủ tục `insertPath` — Chèn 1 transaction vào cây

**Mục đích:** Chèn 1 transaction (đã được sắp theo F-list) vào CR-tree, cập nhật count và classCount dọc đường.

**Pseudo-code chi tiết:**

```
FUNCTION insertPath(path, classDist, count):
    // path: danh sách item đã sắp theo F-list
    // classDist: phân bố class (với insertion ban đầu là {class(t): 1})
    // count: số lần chèn (ban đầu = 1)

    current ← root

    FOR mỗi item ∈ path:
        IF current.children CÓ node con với item đó:
            child ← current.children[item]
        ELSE:
            // Tạo node mới
            child ← new Node(item, count=0, parent=current)
            current.children[item] ← child

            // Thêm vào Header Table / Node-link Chain
            IF headerTable[item] == null:
                headerTable[item] ← child
            ELSE:
                // Nối vào cuối chain
                tail ← headerTable[item]
                WHILE tail.nodeLink != null: tail ← tail.nodeLink
                tail.nodeLink ← child

        // Cập nhật count
        child.count += count

        // Cập nhật classCount
        FOR mỗi (cls, cnt) ∈ classDist:
            child.classCount[cls] += cnt

        current ← child
```

#### 9.2.6 Walkthrough xây CR-tree với 4 transactions

**Dataset (đơn giản):**

| ID | Items | Class |
|:--:|-------|:-----:|
| 1 | {a, b, c} | X |
| 2 | {a, b} | X |
| 3 | {a, c} | Y |
| 4 | {b, c} | X |

**F-list (minSup=2, giảm dần):** `a(3) → b(3) → c(3)` (giả định thứ tự ổn định).

**Chèn transaction 1: path = [a, b, c], classDist = {X:1}**
```
root
 └── a:1 {X:1}
      └── b:1 {X:1}
           └── c:1 {X:1}
```

**Chèn transaction 2: path = [a, b]**
```
root
 └── a:2 {X:2}                 ← tăng count, classCount
      └── b:2 {X:2}            ← tăng count, classCount
           └── c:1 {X:1}        ← không động tới (transaction 2 không có c)
```

**Chèn transaction 3: path = [a, c], class=Y**
```
root
 └── a:3 {X:2, Y:1}
      ├── b:2 {X:2}
      │    └── c:1 {X:1}
      └── c:1 {Y:1}             ← node mới vì chưa có nhánh a → c
```

**Chèn transaction 4: path = [b, c], class=X**
```
root
 ├── a:3 {X:2, Y:1}
 │    ├── b:2 {X:2}
 │    │    └── c:1 {X:1}
 │    └── c:1 {Y:1}
 └── b:1 {X:1}                  ← node mới, nhánh riêng
      └── c:1 {X:1}
```

**Header Table cuối:**
```
a → node a (dưới root)
b → node b (dưới a) ─nodeLink→ node b (dưới root) ─nodeLink→ null
c → node c (dưới a→b) ─nodeLink→ node c (dưới a) ─nodeLink→ node c (dưới b) ─nodeLink→ null
```

**Ý nghĩa:** Toàn bộ 4 transaction đã được **nén** vào cây có tổng cộng 7 node (thay vì lưu 4 hàng với 10 item values).

### 9.3 Bước 3 — FP-Growth đào mỏ đệ quy

#### 9.3.1 Nguyên lý chia để trị (divide-and-conquer)

**Ý tưởng trung tâm của FP-Growth:**

Muốn tìm mọi frequent pattern, thay vì thử tất cả tổ hợp 2^n (Apriori), ta **chia không gian tìm kiếm thành các phần không chồng lấp**:

> Với mỗi item X trong F-list (duyệt từ cuối lên): patterns được chia thành 2 nhóm:
> 1. Patterns **chứa** X
> 2. Patterns **không chứa** X

Nhóm 2 được đệ quy xử lý với F-list không còn X. Nhóm 1 cần thu tập "context" của X — chính là **conditional pattern base**.

#### 9.3.2 Vì sao duyệt item theo tần số TĂNG DẦN?

Khi xây cây: F-list giảm dần (phổ biến ở gốc).
Khi đào mỏ: duyệt **tăng dần** (hiếm trước).

**Lý do:**
- Item hiếm nhất: ít node trong cây → conditional base nhỏ → nhanh.
- Sau khi "xử lý xong" item hiếm, ta có thể "merge chúng vào parent" rồi đào tiếp item phổ biến hơn.
- Đào từ "lá" về "gốc" → tận dụng tối đa cấu trúc cây.

#### 9.3.3 Conditional Pattern Base (CPB) là gì?

**Định nghĩa:** CPB của item X = tập các "prefix path" từ gốc đến mỗi node của X, **kèm count của node** (số transaction đi qua path đó).

**Ẩn dụ:** CPB của X = "bối cảnh xuất hiện của X". Mỗi prefix path nói "X đã xuất hiện kèm những item gì".

**Ví dụ từ cây đã xây ở §9.2.6:**

Tìm CPB của `c`:
- Node c (dưới a→b): prefix path = `[a, b]`, count = 1, classDist = {X:1}
- Node c (dưới a): prefix path = `[a]`, count = 1, classDist = {Y:1}
- Node c (dưới b): prefix path = `[b]`, count = 1, classDist = {X:1}

**CPB của c:**
| Prefix path | Count | ClassDist |
|-------------|:-----:|-----------|
| [a, b] | 1 | {X:1} |
| [a] | 1 | {Y:1} |
| [b] | 1 | {X:1} |

Diễn giải: "Item c xuất hiện 3 lần: 1 lần kèm a và b, 1 lần kèm a, 1 lần kèm b."

#### 9.3.4 Conditional FP-tree (CFP) là gì?

**Định nghĩa:** Từ CPB, xây một **FP-tree nhỏ** đại diện cho không gian con "patterns chứa X". Đây gọi là conditional FP-tree.

**Cách xây:** Như xây FP-tree thường, nhưng:
- Input là CPB (thay vì T).
- Mỗi entry của CPB có count > 1 → chèn path với count đó (không chỉ 1).

**Ví dụ CFP của `c` (từ CPB trên, minSup=2):**

Tần số trong CPB: `a:2, b:2`. Lọc minSup=2 → giữ cả 2.

```
root
 └── a:2 {X:1, Y:1}
      └── b:1 {X:1}
 └── b:1 {X:1}
```

Trên cây này, đệ quy đào mỏ sẽ tìm được patterns chứa c + một số item khác.

#### 9.3.5 Walkthrough `mineTree` đầy đủ

**Tiếp ví dụ §9.2.6**. Đào mỏ theo thứ tự freq tăng (trong F-list đã sắp tăng dần ngược là c, b, a; nhưng giả sử tất cả freq=3 → đào theo thứ tự ngẫu nhiên, ví dụ c đầu tiên).

**Đào mỏ item c:**

1. **Pattern P = {c}**, itemSup = 3.
2. **ClassDist của c** (từ header chain): {X:2, Y:1}.
3. **Sinh CAR:**
   - `{c} ⇒ X`: sup=2, conf=2/3≈0.667 → sinh nếu minConf ≤ 0.667.
   - `{c} ⇒ Y`: sup=1 < minSup=2 → **bỏ**.
4. **Xây CPB và CFP** như mô tả ở §9.3.3–9.3.4.
5. **Đệ quy** `mineTree(CFP, prefix={c})`:
   - Trên CFP, tìm item phổ biến: `a` (sup=2).
   - Pattern {c, a}: itemSup=2, classDist trên CFP = {X:1, Y:1}.
   - Sinh CAR `{c,a} ⇒ X`: sup=1 < 2 → bỏ.
   - Sinh CAR `{c,a} ⇒ Y`: sup=1 < 2 → bỏ.
   - CPB của a trên CFP của c: rỗng (a là root children, không có ancestor).
   - Kết thúc nhánh.

**Kết quả đào mỏ c:** chỉ 1 luật `{c} ⇒ X`.

**Tiếp tục đào mỏ b, a...** — tương tự.

Tất cả luật ứng viên được thu thập vào danh sách `candidates`.

### 9.4 Bước 4 — Sinh CAR trực tiếp (trong lúc đào mỏ)

#### 9.4.1 "Trực tiếp" nghĩa là gì?

**Cách truyền thống (Apriori + CBA):**
1. Pha A: mine frequent patterns (chỉ items).
2. Pha B: với mỗi pattern, tính sup và conf để tạo luật.

→ Cần **2 pass** qua dữ liệu.

**Cách CMAR:**
1. Pha duy nhất: mine pattern **kèm sinh luật ngay**.

→ Chỉ **1 pass**, gộp mine và rule-extraction.

#### 9.4.2 Cơ chế — Nhờ `classCount`

Vì mỗi node trong CR-tree đã có classCount sẵn, khi FP-Growth phát hiện pattern P:
1. Thu classDist(P) bằng cách cộng classCount qua header chain (§9.3.3).
2. Sinh luật `P ⇒ c` cho mỗi class c có `classDist(P)[c] ≥ minSup` và `classDist(P)[c]/sup(P) ≥ minConf`.

**Điều kiện sinh luật:**
$$
\text{classDist}(P)[c] \ge \text{minSup} \quad \wedge \quad \frac{\text{classDist}(P)[c]}{\text{sup}(P)} \ge \text{minConf}
$$

#### 9.4.3 Ví dụ rõ ràng

Trên Mushroom, giả sử FP-Growth đang đào mỏ và phát hiện pattern `P = {odor=f}` với `itemSup = 2160`.

**Thu classDist:** đi qua header chain của `odor=f`, cộng classCount các node:
- Node 1: {poisonous: 1500}
- Node 2: {poisonous: 660}
- ... (các path khác nhau qua odor=f)
- **Tổng classDist({odor=f}) = {poisonous: 2160, edible: 0}**

**Sinh luật:**
- `{odor=f} ⇒ poisonous`: sup=2160, conf=2160/2160=1.00 → **sinh luật**.
- `{odor=f} ⇒ edible`: sup=0 < minSup → **bỏ**.

#### 9.4.4 So sánh cấu trúc 2 cách tiếp cận

| Khía cạnh | Apriori + CBA | CMAR FP-Growth |
|-----------|:-------------:|:--------------:|
| Số pass qua T | 2 | **1** |
| Lưu class tại node | Không | **classCount** |
| Có bước "tạo luật" riêng? | Có | **Không** |
| Xử lý class như thế nào? | Thêm `class=X` làm item ảo | **Tách riêng** qua classCount |
| Tốc độ | Chậm hơn | **Nhanh hơn ~10×** |
| Bộ nhớ | Lớn hơn | **Nhỏ hơn** |

---

## 10. Giai Đoạn II — Cắt Tỉa 3 Tầng (Chi Tiết)

Đây là phần **then chốt** tạo nên chất lượng CMAR. Ba tầng pruning được áp dụng **nối tiếp**, mỗi tầng loại bỏ một nhóm luật "xấu" khác nhau.

Thứ tự thực hiện:
$$
\underbrace{\text{CAR ứng viên}}_{\text{N}_0} \xrightarrow{\text{P1}} \underbrace{\text{sau general-rule}}_{\text{N}_1} \xrightarrow{\text{P2}} \underbrace{\text{sau χ²}}_{\text{N}_2} \xrightarrow{\text{P3}} \underbrace{\text{sau coverage}}_{\text{N}_3}
$$

Điển hình: $N_0 \gg N_1 \gg N_2 \gg N_3$, tổng cắt giảm >99%.

### 10.1 Pruning tầng 1 — General Rule Pruning

#### 10.1.1 Quy tắc chính thức

**Loại bỏ luật $R_2 : P_2 \Rightarrow c$ nếu tồn tại luật $R_1 : P_1 \Rightarrow c$ (cùng class) thỏa:**
1. $P_1 \subseteq P_2$ (R₁ tổng quát hơn)
2. $R_1 \succ R_2$ (R₁ ưu tiên cao hơn)

#### 10.1.2 Cơ sở lý thuyết

- Luật tổng quát = điều kiện ít hơn → **dễ áp dụng** hơn (khớp được nhiều bản ghi hơn).
- Nếu luật tổng quát lại có **precedence cao hơn** (conf/sup tốt hơn) → luật chi tiết **thừa**.
- Giữ luật chi tiết sẽ: (a) tốn bộ nhớ, (b) làm rối, (c) không cải thiện phân lớp.

#### 10.1.3 Thuật toán

```
FUNCTION PRUNE_GENERAL(rules):
    // Giả định: rules đã sắp theo precedence (conf↓, sup↓, size↑)
    kept ← []
    keptByClass ← {}  // class → list of kept rules
    FOR mỗi R in rules:
        dominated ← false
        sameClassRules ← keptByClass[R.class]
        IF sameClassRules ≠ null:
            FOR mỗi R' ∈ sameClassRules:
                IF P(R') ⊆ P(R):       // R' tổng quát hơn R
                    dominated ← true
                    BREAK
        IF NOT dominated:
            kept.append(R)
            keptByClass[R.class].append(R)
    RETURN kept
```

**Độ phức tạp:** $O(N_R \cdot \bar{K})$ trong đó $\bar{K}$ là số luật trung bình cùng class đã giữ lại → thực tế rất nhanh.

**Vì sao sắp theo precedence trước?** Nếu duyệt theo precedence giảm dần, khi xét $R$, mọi $R'$ ưu tiên hơn $R$ **đều đã** được kiểm tra. Nếu $R' \succ R$ và $R'$ tổng quát hơn $R$, ta loại $R$ ngay.

#### 10.1.4 Ví dụ minh hoạ chi tiết

**Tập luật ứng viên (đã sắp theo precedence):**

| # | Luật | conf | sup | size |
|:-:|------|:----:|:---:|:----:|
| 1 | $\{a\} \Rightarrow X$ | 0.90 | 9 | 1 |
| 2 | $\{b\} \Rightarrow X$ | 0.85 | 8 | 1 |
| 3 | $\{a, b\} \Rightarrow X$ | 0.85 | 6 | 2 |
| 4 | $\{a, c\} \Rightarrow X$ | 0.80 | 5 | 2 |
| 5 | $\{a, b, c\} \Rightarrow X$ | 0.80 | 3 | 3 |
| 6 | $\{a\} \Rightarrow Y$ | 0.70 | 7 | 1 |
| 7 | $\{a, d\} \Rightarrow Y$ | 0.85 | 4 | 2 |

**Duyệt:**

| Bước | Xét | Đã giữ cùng class | Kiểm tra | Kết quả |
|:----:|-----|------------------|----------|---------|
| 1 | $\{a\} \Rightarrow X$ | — | — | **Giữ** |
| 2 | $\{b\} \Rightarrow X$ | $\{a\}$ | $\{a\} \not\subseteq \{b\}$ | **Giữ** |
| 3 | $\{a,b\} \Rightarrow X$ | $\{a\}, \{b\}$ | $\{a\} \subseteq \{a,b\}$ ✓ | **Loại** |
| 4 | $\{a,c\} \Rightarrow X$ | $\{a\}, \{b\}$ | $\{a\} \subseteq \{a,c\}$ ✓ | **Loại** |
| 5 | $\{a,b,c\} \Rightarrow X$ | $\{a\}, \{b\}$ | $\{a\} \subseteq \{a,b,c\}$ ✓ | **Loại** |
| 6 | $\{a\} \Rightarrow Y$ | — (class Y riêng) | — | **Giữ** |
| 7 | $\{a,d\} \Rightarrow Y$ | $\{a\}$ (class Y) | $\{a\} \subseteq \{a,d\}$ ✓ | **Loại** (lưu ý conf 0.85 > 0.70 của luật $\{a\}\Rightarrow Y$, **nhưng** thứ tự precedence đã đặt $\{a\}\Rightarrow Y$ lên trước trong sắp ban đầu nếu conf cao hơn — ví dụ này đã giả định sắp theo chỉ số) |

**Cảnh báo:** Ví dụ trên dùng để minh hoạ cơ chế. Trong thực tế, ở luật 7 có conf = 0.85 > 0.70 của luật 6 → luật 7 **phải** được xét trước luật 6 trong thứ tự precedence. Khi đó luật 6 mới có thể bị đánh giá.

**Kết quả cuối:** Giữ luật 1, 2, 6 (có thể thêm 7 nếu precedence đặt 7 trước 6).

#### 10.1.5 Hiệu quả

Trên dataset điển hình, Pruning 1 loại ~**70–95%** luật. Ví dụ Zoo dataset: 525,376 → 1,387 (99.74%).

---

### 10.2 Pruning tầng 2 — χ² + Tương quan dương

#### 10.2.1 Quy tắc chính thức

**Giữ lại luật $R : P \Rightarrow c$ khi VÀ CHỈ KHI** đáp ứng **cả hai** điều kiện:

1. **Ý nghĩa thống kê:** $\chi^2(R) \ge \chi^2_{\text{threshold}}$ (mặc định 3.841)
2. **Tương quan dương:** $ad > b\gamma$

#### 10.2.2 Cơ sở lý thuyết

- χ² kiểm định **giả thuyết không** $H_0$: "$P$ và $c$ độc lập".
- $\chi^2$ cao → bác bỏ $H_0$ → có liên hệ thống kê giữa $P$ và $c$.
- Nhưng liên hệ đó có thể là **tương quan âm** (P làm giảm khả năng c). Cần thêm điều kiện $ad > b\gamma$ để chắc chắn $P$ **làm tăng** khả năng $c$.

#### 10.2.3 Thuật toán

```
FUNCTION PRUNE_CHI_SQUARE(rules, χ²_thr):
    kept ← []
    FOR mỗi R ∈ rules:
        // Tính contingency
        a ← sup(R)
        b ← sup(P(R)) - a
        γ ← sup(class(R)) - a
        d ← |T| - a - b - γ

        // Tính χ²
        denom ← (a+b)(γ+d)(a+γ)(b+d)
        IF denom = 0: continue
        χ² ← |T| * (a*d - b*γ)² / denom

        // Hai điều kiện
        IF χ² ≥ χ²_thr AND a*d > b*γ:
            kept.append(R)
    RETURN kept
```

**Độ phức tạp:** $O(N_R)$ — mỗi luật 1 lần tính χ² (O(1)).

#### 10.2.4 Bảng các giá trị ngưỡng χ² (df = 1)

| α | χ² threshold | Mức tin cậy | Ứng dụng |
|:-:|:------------:|:-----------:|---------|
| 0.10 | 2.706 | 90% | Lỏng — dữ liệu nhỏ, muốn giữ nhiều |
| **0.05** | **3.841** | **95%** | **Mặc định CMAR** |
| 0.01 | 6.635 | 99% | Chặt — yêu cầu luật rất mạnh |
| 0.005 | 7.879 | 99.5% | Rất chặt |
| 0.001 | 10.827 | 99.9% | Cực chặt |

#### 10.2.5 Ví dụ tính toán

**Luật $R : \{\text{outlook=overcast}\} \Rightarrow \text{play=yes}$** trên dataset thời tiết (n=14):

**Bảng contingency:**

| | yes | no | tổng |
|-|:---:|:--:|:----:|
| overcast | $a = 4$ | $b = 0$ | 4 |
| ¬overcast | $\gamma = 5$ | $d = 5$ | 10 |
| **tổng** | 9 | 5 | 14 |

**Tính χ²:**
$$
\chi^2 = \frac{14 \cdot (4 \cdot 5 - 0 \cdot 5)^2}{4 \cdot 10 \cdot 9 \cdot 5} = \frac{14 \cdot 400}{1800} \approx 3.11
$$

**Kết quả:**
- $\chi^2 = 3.11 < 3.841$ → **chưa đủ ý nghĩa** ở mức α=0.05
- $ad = 20, b\gamma = 0$ → $ad > b\gamma$ ✓ (tương quan dương)

→ **Loại** luật này ở ngưỡng 3.841, nhưng giữ ở ngưỡng 2.706 (α=0.10).

**Bài học:** Dataset nhỏ (n=14) → khó đạt ý nghĩa thống kê cao. Cần tăng số mẫu hoặc hạ ngưỡng.

#### 10.2.6 Hiệu quả

Pruning 2 loại thêm ~**50–80%** luật còn lại sau Pruning 1. Rule có conf cao nhưng support quá nhỏ thường bị loại ở bước này.

---

### 10.3 Pruning tầng 3 — Database Coverage (Thuật toán 1 bài báo)

#### 10.3.1 Ý tưởng

**Vấn đề với CBA:** CBA loại training record ngay khi bất kỳ luật nào cover → chỉ giữ **rất ít** luật → khi classify, dễ thiếu luật.

**Giải pháp CMAR:** Cho phép mỗi record bị cover tối đa **δ lần** trước khi loại → giữ **nhiều** luật dự phòng → khi classify có nhiều luật để hội ý.

#### 10.3.2 Thuật toán chính thức (Algorithm 1 bài báo)

```
INPUT : R (tập luật sắp theo precedence giảm dần), T, δ
OUTPUT: S (tập luật đã chọn)

1. FOR mỗi obj ∈ T: cover_count[obj] ← 0
2. selected ← []
3. WHILE T ≠ ∅ AND R ≠ ∅:
     FOR mỗi rule Ri ∈ R theo precedence giảm dần:
         coversAny ← false
         FOR mỗi obj ∈ T:
             IF Ri khớp obj AND class(Ri) = class(obj):
                 coversAny ← true
                 cover_count[obj] += 1
                 IF cover_count[obj] ≥ δ:
                     xoá obj khỏi T
         IF coversAny:
             selected.append(Ri)
4. RETURN selected
```

#### 10.3.3 Ý nghĩa δ

| δ | Đặc điểm |
|:-:|----------|
| δ = 1 | Giống CBA — loại record ngay khi cover → ít luật giữ lại |
| δ = 2 | Giữ mỗi record "sống" qua 2 lần cover |
| **δ = 4** | **Mặc định CMAR** — cân bằng tốt trên hầu hết dataset UCI |
| δ = 10 | Rất lỏng — giữ rất nhiều luật, có thể bao gồm luật yếu |

#### 10.3.4 Walk-through ví dụ

**Cho 5 training records, mỗi record được một số luật cover. δ = 2.**

Ký hiệu: `record(id, class) | luật cover: [R1, R2, ...]`

```
rec_1 (X) | R1, R2, R5
rec_2 (X) | R1, R3, R5
rec_3 (Y) | R4, R6
rec_4 (X) | R2, R5
rec_5 (Y) | R4, R6
```

Luật đã sắp precedence: $R_1 \succ R_2 \succ R_3 \succ R_4 \succ R_5 \succ R_6$.

**Lặp:**

| Bước | Xét luật | Cover các rec | cover_count sau | Loại rec? | selected |
|:----:|:--------:|--------------|:---------------:|:----------:|----------|
| 1 | $R_1$ | rec_1, rec_2 | rec_1=1, rec_2=1 | — | [R₁] |
| 2 | $R_2$ | rec_1, rec_4 | rec_1=2, rec_4=1 | **rec_1 loại** (=2) | [R₁, R₂] |
| 3 | $R_3$ | rec_2 | rec_2=2 | **rec_2 loại** | [R₁, R₂, R₃] |
| 4 | $R_4$ | rec_3, rec_5 | rec_3=1, rec_5=1 | — | [R₁, R₂, R₃, R₄] |
| 5 | $R_5$ | rec_4 (rec_1, rec_2 đã loại) | rec_4=2 | **rec_4 loại** | [R₁, R₂, R₃, R₄, R₅] |
| 6 | $R_6$ | rec_3, rec_5 | rec_3=2, rec_5=2 | **rec_3, rec_5 loại** | [R₁...R₆] |

**T rỗng → dừng. Tất cả 6 luật được chọn.**

**Nếu δ = 1:** Bước 1 loại rec_1, rec_2 luôn → R₃ không còn rec nào để cover → **bỏ R₃**. Tương tự ít luật hơn.

**Nếu δ = 4:** Không có rec nào đạt 4 lần cover → giữ tất cả luật (kể cả luật yếu).

#### 10.3.5 Độ phức tạp

**Worst case:** $O(N_R \cdot |T|)$ — với mỗi luật duyệt toàn bộ T.

**Thực tế** nhanh hơn nhiều vì T giảm dần khi records bị loại.

#### 10.3.6 Khác biệt với CBA

| Yếu tố | CBA | CMAR |
|--------|:---:|:----:|
| Số lần cover trước khi loại | 1 | δ (= 4) |
| Số luật giữ lại | Ít | Nhiều hơn |
| Phân lớp | Dùng 1 luật | Dùng nhiều luật (weighted χ²) |

---

### 10.4 Bảng tổng hợp 3 tầng pruning

| Tầng | Tên | Loại luật nào? | Cơ sở | Hiệu quả điển hình |
|:----:|-----|---------------|-------|:------------------:|
| **P1** | General rule | Dư thừa (bị luật đơn giản mạnh hơn lấn át) | $P_1 \subseteq P_2$, cùng class, $R_1 \succ R_2$ | Loại 70–95% |
| **P2** | χ² + correlation | Ngẫu nhiên / tương quan âm | $\chi^2 < \text{thr}$ hoặc $ad \le b\gamma$ | Loại 50–80% phần còn lại |
| **P3** | Database coverage | Dư so với các luật đã đủ cover data | coverage ≥ δ | Loại 30–70% phần còn lại |
| **Tổng** | | | | **Loại > 99%** ứng viên |

---

## 11. Giai Đoạn III — Phân Lớp Bằng Weighted χ²

### 11.1 CR-tree lưu trữ luật (sau pruning)

#### 11.1.1 Tại sao dùng CR-tree thay vì danh sách phẳng?

Sau khi hoàn tất 3 tầng pruning, ta có tập luật cuối cùng (vài chục đến vài trăm luật). Có 2 cách lưu:

**Cách 1 — Danh sách phẳng (naive):**
```
rules = [R1, R2, R3, ..., RN]
```
- Ưu: đơn giản.
- Nhược: khi classify bản ghi t, phải duyệt **toàn bộ N luật**, kiểm tra mỗi luật xem condset có ⊆ t không → O(N).

**Cách 2 — CR-tree (CMAR):**
```
root
 ├── item_A
 │    ├── item_B → [R1, R2]
 │    └── item_C → [R3]
 └── item_D → [R4]
```
- Ưu: chia sẻ tiền tố giữa các luật có condset chung, truy vấn nhanh qua DFS cắt tỉa.
- Nhược: code phức tạp hơn.

**Tiết kiệm bộ nhớ:** Bài báo báo cáo **50–60%** trong hầu hết dataset.

#### 11.1.2 Cách xây CR-tree lưu trữ

**Bước 1 — Sắp item theo tần số giảm dần:**

Trước khi chèn luật, sắp các item **GLOBAL theo tần số giảm dần** (giống F-list ở §9.1.3).

**Lý do:** item phổ biến gần gốc → nhiều luật chia sẻ tiền tố → cây nén tối ưu.

**Bước 2 — Với mỗi luật R, chèn condset như một path:**

```
FUNCTION insert(R):
    sortedItems ← sắp condset(R) theo thứ tự global
    current ← root
    FOR mỗi item ∈ sortedItems:
        IF current không có con với item đó:
            tạo node mới
        current ← node con đó
    current.rules.append(R)    // lưu rule tại node cuối (leaf)
```

**Ví dụ:**

Giả sử sau pruning còn 4 luật (từ Example 2 bài báo):
| # | Luật | Sup | Conf |
|:-:|------|:---:|:----:|
| R1 | `{a,b,c} ⇒ A` | 80 | 80% |
| R2 | `{a,b,c,d} ⇒ A` | 63 | 90% |
| R3 | `{a,b,e} ⇒ B` | 36 | 60% |
| R4 | `{b,c,d} ⇒ C` | 210 | 70% |

**CR-tree kết quả (giả định thứ tự global: a, b, c, d, e):**

```
root
 ├── a
 │    └── b
 │         ├── c
 │         │    ├── [R1: A, 80, 80%]        ← R1 kết thúc tại c
 │         │    └── d
 │         │         └── [R2: A, 63, 90%]    ← R2 kết thúc tại d
 │         └── e
 │              └── [R3: B, 36, 60%]         ← R3 kết thúc tại e
 └── b
      └── c
           └── d
                └── [R4: C, 210, 70%]         ← R4 kết thúc tại d
```

**Số node sử dụng:**
- Lưu phẳng: 3 + 4 + 3 + 3 = **13 item values** (tổng kích thước condset).
- Lưu CR-tree: **9 node** (root + 8 node trong cây).

Tiết kiệm: (13-9)/13 ≈ **31%** (ví dụ nhỏ; dataset lớn tiết kiệm 50–60%).

#### 11.1.3 Truy vấn luật khớp với DFS cắt tỉa

**Mục tiêu:** Cho bản ghi test $t$, tìm mọi luật R sao cho $\text{condset}(R) \subseteq \text{items}(t)$.

**Naive:** Duyệt mọi luật, kiểm tra từng cái → O(N × k) với k = độ dài condset.

**CMAR (DFS với cắt tỉa):**

```
FUNCTION findMatching(t):
    matching ← []
    DFS(root, items(t), matching)
    RETURN matching

FUNCTION DFS(node, testItems, matching):
    matching.extend(node.rules)     // thu các luật kết thúc tại node
    FOR mỗi child ∈ node.children:
        IF child.item ∈ testItems:   // CẮT TỈA: chỉ đi vào nhánh có item trong t
            DFS(child, testItems, matching)
```

**Cắt tỉa tại sao hiệu quả?**

Nếu test record có `t = {a, b, c}` và CR-tree có nhánh `root → d → e → [R5]`:
- Nhìn vào root's children, thấy `d ∉ t` → **bỏ toàn bộ nhánh d** ngay.
- Không cần duyệt xuống, không cần kiểm tra R5.

Ước tính độ phức tạp: **O(|t|)** thay vì O(N) trong naive.

### 11.2 Quy trình phân lớp 4 bước

```
FUNCTION CLASSIFY(t, crTree, defaultClass):
    // Bước 1: Truy vấn tập luật khớp
    R_t ← crTree.findMatching(t)

    // Bước 2: Không có luật khớp
    IF R_t = ∅: RETURN defaultClass

    // Bước 3: Nhóm theo class, nếu chỉ 1 class
    byClass ← group R_t by class
    IF |byClass| = 1: RETURN class đó

    // Bước 4: Weighted χ²
    bestCls ← defaultClass; bestScore ← -∞
    FOR mỗi (cls, group) ∈ byClass:
        score ← 0
        FOR mỗi R ∈ group:
            chi2 ← computeχ²(R, cls)
            maxChi2 ← computeMaxχ²(R, cls)
            IF maxChi2 > 0:
                score += (chi2 * chi2) / maxChi2
        IF score > bestScore:
            bestScore ← score; bestCls ← cls
    RETURN bestCls
```

### 11.3 Công thức Weighted χ² — Chi tiết

**Với mỗi nhóm luật $\mathcal{G}_c$ cùng class c:**

$$
\boxed{\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \frac{\chi^2(R)^2}{\max\chi^2(R)}}
$$

**Class được dự đoán:**
$$
\hat{c} = \arg\max_{c \in \mathcal{C}} \text{score}(\mathcal{G}_c)
$$

### 11.4 Vì sao chia cho $\max\chi^2$?

Nếu chỉ dùng $\sum \chi^2$ thô, class **minority** (ít mẫu) dễ có luật đạt $\chi^2$ lớn bất thường do kích thước class nhỏ. Chia cho $\max\chi^2$ **chuẩn hoá** về tỉ lệ [0, 1] → công bằng giữa mọi luật.

**Minh hoạ bằng Example 3 bài báo:** (đã trình bày ở §5.5) — cho thấy $R_1$ có $\chi^2$ thô = 88.4 nhưng tỉ số $\chi^2/\max\chi^2 = 0.308$; $R_2$ có $\chi^2$ thô chỉ 33.6 nhưng tỉ số = 0.908. Normalization "đảo ngược" ưu thế → $R_2$ mới thật sự mạnh.

#### Minh hoạ trực quan với 3 luật

| Luật | class | $\chi^2$ thô | $\max\chi^2$ | $\chi^2/\max\chi^2$ | Nhận xét |
|------|:-----:|:------------:|:------------:|:-------------------:|---------|
| A | minority X | 100 | 100 | **1.00** | Pure, nhỏ nhưng hoàn hảo |
| B | minority X | 50 | 100 | 0.50 | Trung bình |
| C | majority Y | 300 | 500 | 0.60 | Lớn nhưng chưa hoàn hảo |

**Theo χ² thô:** C (300) > A (100) > B (50) → C mạnh nhất.
**Theo tỉ số:** A (1.00) > C (0.60) > B (0.50) → A mạnh nhất (vì đạt đỉnh lý thuyết).

→ CMAR dùng tỉ số để đánh giá **công bằng** giữa luật class nhỏ (ít support nhưng pure) và class lớn (nhiều support nhưng không pure).

### 11.5 Vì sao bình phương $\chi^2$ trong tử số?

Bình phương **phóng đại** sự khác biệt giữa luật mạnh và luật yếu → luật mạnh đóng góp tỉ lệ nhiều hơn trong score → giảm ảnh hưởng của luật yếu.

### 11.6 Ví dụ phân lớp chi tiết

**Bản ghi test:** $t = \{a, b, c, d\}$, class chưa biết.

**Luật khớp từ CR-tree:**

| # | Luật | Class | $\chi^2$ | $\max\chi^2$ | $\chi^2/\max\chi^2$ | $\chi^2 \cdot (\chi^2/\max\chi^2)$ |
|:-:|------|:-----:|:--------:|:------------:|:--------------------:|:----------------------------------:|
| 1 | $\{a, b\} \Rightarrow X$ | X | 12.5 | 25 | 0.50 | 6.25 |
| 2 | $\{a, c\} \Rightarrow X$ | X | 8.0 | 20 | 0.40 | 3.20 |
| 3 | $\{b, d\} \Rightarrow Y$ | Y | 18.0 | 22 | 0.818 | 14.73 |
| 4 | $\{c, d\} \Rightarrow Y$ | Y | 4.5 | 15 | 0.30 | 1.35 |

**Nhóm luật:**
- $\mathcal{G}_X = \{R_1, R_2\}$
- $\mathcal{G}_Y = \{R_3, R_4\}$

**Tính score:**
$$
\text{score}(X) = \frac{12.5^2}{25} + \frac{8^2}{20} = \frac{156.25}{25} + \frac{64}{20} = 6.25 + 3.20 = 9.45
$$
$$
\text{score}(Y) = \frac{18^2}{22} + \frac{4.5^2}{15} = \frac{324}{22} + \frac{20.25}{15} \approx 14.73 + 1.35 = 16.08
$$

**So sánh:** $\text{score}(Y) = 16.08 > \text{score}(X) = 9.45$

**→ Dự đoán class Y.**

**Bài học:**
- Class X có **2 luật** khớp, Y cũng **2 luật** → đếm phiếu thô thì hoà.
- CMAR xét "sức mạnh tổng hợp" → Y thắng nhờ có luật $R_3$ rất mạnh (tỉ số 0.818).

### 11.7 Các trường hợp đặc biệt (Edge Cases)

Khi implementation CMAR, cần xử lý một số trường hợp đặc biệt:

#### 11.7.1 Test record có thuộc tính chưa từng thấy

**Tình huống:** Bản ghi test có item `color=purple` mà trong training không có `color=purple`.

**Xử lý CMAR:**
- Item này chỉ đơn giản **không khớp** với luật nào có điều kiện liên quan `color`.
- Các luật không đề cập đến `color` vẫn được xét bình thường.
- Nếu tất cả luật bị loại → trả về `defaultClass`.

#### 11.7.2 Bản ghi khớp 0 luật

**Nguyên nhân:** Có thể do:
- Test record "lạ" — tổ hợp items không tạo thành pattern nào.
- Pruning quá chặt — loại hết các luật liên quan.

**Xử lý:** Trả `defaultClass`.

**Lưu ý quan trọng:** Tỉ lệ dự đoán bằng defaultClass là một **chỉ báo chất lượng**. Nếu >10% test records phải dùng defaultClass → mô hình có vấn đề (training chưa đủ đa dạng, hoặc pruning quá tay).

#### 11.7.3 Tie trong weighted χ² score

**Tình huống:** Hai class có score bằng nhau chính xác.

**Chiến lược tie-breaking (không có trong bài báo gốc, nhưng thường dùng):**
1. Chọn class có nhiều luật khớp hơn.
2. Nếu vẫn hoà, chọn class có `defaultClass` ưu tiên.
3. Cuối cùng, chọn class có thứ tự alphabet nhỏ hơn (deterministic).

#### 11.7.4 Luật có χ² = 0 hoặc maxχ² = 0

**Tình huống:** Khi mẫu số trong công thức χ² bị 0 → chia cho 0.

**Xử lý:**
- Nếu $\max\chi^2(R) = 0$ → đóng góp của R vào score = 0 (bỏ qua R).
- Điều này hiếm xảy ra sau Pruning 2 (những luật này đã bị loại).

### 11.8 Độ phức tạp phân lớp

**Một test record:**

| Bước | Độ phức tạp |
|------|:-----------:|
| DFS trên CR-tree | $O(|t| \cdot b)$ với $b$ = branching factor |
| Tính χ² cho mỗi luật khớp | $O(M)$ với $M$ = số luật khớp |
| Tính max χ² | $O(M)$ |
| Aggregation score | $O(M)$ |
| **Tổng** | **$O(|t| + M)$** |

**Với toàn bộ test set (|T_test| records):**
$$
O(|T_{\text{test}}| \cdot (|t| + M))
$$

Thực tế rất nhanh — vài nghìn record/giây trên CPU thường.

---

## 11b. Thuật Ngữ Tổng Hợp — Bảng Tra Cứu Nhanh

Dưới đây là bảng tổng hợp **tất cả thuật ngữ và ký hiệu** xuất hiện trong Phần B, cùng giải thích ngắn gọn:

### 11b.1 Các đối tượng dữ liệu chính

| Thuật ngữ | Là gì | Ví dụ |
|-----------|-------|-------|
| **Item** | Cặp `(thuộc tính, giá trị)` | `odor=f`, `color=red` |
| **Transaction / Record** | Một bản ghi = tập items + class | `{odor=f, cap=x}, class=poisonous` |
| **Itemset / Pattern** | Tập con của items | `{odor=f, cap=x}` |
| **Condset** | Vế trái của luật (tiền đề) | Trong `{a,b}⇒X`, condset = `{a,b}` |
| **Class label** | Nhãn lớp | `poisonous`, `edible` |
| **CAR** | Luật kết hợp lớp `P ⇒ c` | `{odor=f} ⇒ poisonous` |

### 11b.2 Các cấu trúc dữ liệu

| Thuật ngữ | Là gì | Dùng ở đâu |
|-----------|-------|-----------|
| **CR-tree** | Prefix tree mở rộng (có classCount tại node) | Giai đoạn I (đào mỏ) + Giai đoạn III (lưu luật) |
| **Node** | Đỉnh của cây, lưu `item`, `count`, `classCount`, `children`, `nodeLink`, `parent` | Mọi nơi trong CR-tree |
| **`classCount`** | Dict `{class → số lượng}` tại mỗi node — **phân bố class** | Mở rộng cốt lõi CMAR vs FP-tree |
| **Header Table** | Dict `{item → con trỏ đến node đầu tiên chứa item}` | Truy xuất nhanh các node cùng item |
| **Node-link** | Con trỏ nối các node cùng item thành chain | Duyệt toàn bộ node của 1 item |
| **F-list** | Danh sách item phổ biến, **sắp giảm dần** theo tần số | Chèn transactions vào cây |
| **Prefix path** | Chuỗi item từ root xuống node (không kể root và node đó) | Xây conditional pattern base |
| **Conditional Pattern Base (CPB)** | Tập các prefix path + count + classDist của một item X | Đầu vào xây conditional tree |
| **Conditional FP-tree (CFP)** | Cây con cho không gian "patterns chứa X" | Đệ quy FP-Growth |

### 11b.3 Các tham số & hằng số

| Ký hiệu | Tên đầy đủ | Mặc định | Ý nghĩa |
|---------|------------|:--------:|--------|
| **minSup** | Minimum Support | 1% (tương đối) | Ngưỡng support tối thiểu để pattern/luật được giữ |
| **minConf** | Minimum Confidence | 0.5 (50%) | Ngưỡng confidence tối thiểu |
| **χ²_thr** | Chi-square threshold | 3.841 (α=0.05, df=1) | Ngưỡng χ² để luật "có ý nghĩa thống kê" |
| **δ** (delta) | Database Coverage threshold | 4 | Số lần cover tối đa cho mỗi training record |
| **`defaultClass`** | Lớp mặc định | class phổ biến nhất | Trả về khi không có luật khớp test record |

### 11b.4 Các đại lượng tính toán

| Ký hiệu | Là gì | Công thức |
|---------|-------|-----------|
| **sup(R)** | Support tuyệt đối | `#{t : P ⊆ t ∧ class(t)=c}` |
| **σ(R)** | Support tương đối | `sup(R) / |T|` |
| **conf(R)** | Confidence | `sup(P ∪ c) / sup(P)` |
| **χ²(R)** | Chi-square | `n(ad-bγ)² / ((a+b)(γ+d)(a+γ)(b+d))` |
| **max χ²(R)** | Cận trên χ² | χ² khi `a = min(sup(P), sup(c))` |
| **score(G_c)** | Weighted χ² score | `Σ χ²(R)² / max χ²(R)` trên nhóm class c |
| **a, b, γ, d** | Ô trong bảng contingency 2×2 | xem §5.4 |
| **R₁ ≻ R₂** | Precedence | conf↓, sup↓, size↑ |

### 11b.5 Các phép toán / thủ tục

| Thủ tục | Làm gì | Dùng ở đâu |
|---------|--------|-----------|
| **`insertPath(path, classDist, count)`** | Chèn 1 path vào CR-tree, cập nhật count + classCount | Xây cây khởi tạo + cây điều kiện |
| **`mineTree(tree, prefix)`** | Đệ quy đào mỏ pattern trên CR-tree | Giai đoạn I, bước 3 |
| **`findMatching(t)`** | DFS cắt tỉa trên CR-tree lưu trữ, trả các luật khớp t | Giai đoạn III, bước 1 |
| **`classify(t)`** | Phân lớp 1 test record | Giai đoạn III toàn phần |

### 11b.6 Bảng đối chiếu "Câu hỏi → Công thức"

| Câu hỏi | Công thức/Khái niệm |
|---------|--------------------|
| "Luật này xuất hiện bao nhiêu bản ghi?" | `sup(R)` |
| "Xác suất khớp vế trái kéo theo vế phải?" | `conf(R)` |
| "Có phải ngẫu nhiên không?" | `χ²(R) ≥ χ²_thr` |
| "P làm tăng hay giảm cơ hội c?" | `ad > bγ` (dương) hoặc `ad < bγ` (âm) |
| "Luật nào đáng tin hơn?" | `R₁ ≻ R₂` (precedence) |
| "Không có luật khớp → dự đoán gì?" | `defaultClass` |
| "Nhiều luật mâu thuẫn → chọn class nào?" | `argmax score(G_c)` |
| "Luật A có lấn át luật B không?" | `P_A ⊆ P_B ∧ class_A = class_B ∧ A ≻ B` |

---

## 12. Luồng Hoạt Động Tổng Thể (End-to-End)

Phần này minh hoạ toàn bộ pipeline từ tập huấn luyện đến dự đoán cuối cùng trên một ví dụ nhỏ, dễ theo dõi.

### 12.1 Dataset huấn luyện

**10 bản ghi**, 3 thuộc tính nhị phân (a, b, c), 2 class (X, Y):

| ID | a | b | c | class |
|:--:|:-:|:-:|:-:|:-----:|
| 1 | 1 | 1 | 1 | X |
| 2 | 1 | 1 | 0 | X |
| 3 | 1 | 0 | 1 | X |
| 4 | 1 | 1 | 0 | Y |
| 5 | 0 | 1 | 1 | X |
| 6 | 1 | 1 | 1 | X |
| 7 | 0 | 1 | 0 | Y |
| 8 | 1 | 0 | 0 | Y |
| 9 | 1 | 1 | 1 | X |
| 10 | 0 | 0 | 1 | X |

**Tham số:** minSup = 3, minConf = 0.5, $\chi^2_{\text{thr}} = 2.706$ (α=0.10), δ = 2.

Ký hiệu item: `a1, a0, b1, b0, c1, c0` (ví dụ `a1` = "a=1").

### 12.2 Bước 1 — Đếm tần số

| Item | Count |
|------|:-----:|
| a1 | 7 |
| b1 | 7 |
| c1 | 6 |
| a0 | 3 |
| b0 | 3 |
| c0 | 4 |
| class X | 7 |
| class Y | 3 |

F-list (≥ minSup=3, sắp giảm dần): `a1(7) → b1(7) → c1(6) → c0(4) → a0(3) → b0(3)`

**Default class:** X (7/10).

### 12.3 Bước 2 — Xây CR-tree khởi tạo

Chuyển mỗi transaction thành path (items ∩ F, sắp F-list):

| ID | Path | Class |
|:--:|------|:-----:|
| 1 | [a1, b1, c1] | X |
| 2 | [a1, b1, c0] | X |
| 3 | [a1, c1, b0] | X |
| 4 | [a1, b1, c0] | Y |
| 5 | [b1, c1, a0] | X |
| 6 | [a1, b1, c1] | X |
| 7 | [b1, c0, a0] | Y |
| 8 | [a1, c0, b0] | Y |
| 9 | [a1, b1, c1] | X |
| 10 | [c1, a0, b0] | X |

**CR-tree:**

```
root
├── a1:7 {X:5, Y:2}
│   ├── b1:4 {X:3, Y:1}
│   │   ├── c1:3 {X:3}             (bản ghi 1, 6, 9)
│   │   └── c0:2 {X:1, Y:1}         (bản ghi 2, 4)
│   └── c1:1 {X:1}
│   │   └── b0:1 {X:1}              (bản ghi 3)
│   └── c0:1 {Y:1}
│       └── b0:1 {Y:1}              (bản ghi 8)
├── b1:2 {X:1, Y:1}
│   ├── c1:1 {X:1}
│   │   └── a0:1 {X:1}              (bản ghi 5)
│   └── c0:1 {Y:1}
│       └── a0:1 {Y:1}              (bản ghi 7)
└── c1:1 {X:1}
    └── a0:1 {X:1}
        └── b0:1 {X:1}              (bản ghi 10)
```

### 12.4 Bước 3–4 — FP-Growth + Sinh CAR

Đào mỏ item theo tần số **tăng dần**: b0 → a0 → c0 → c1 → b1 → a1.

**Minh hoạ với item `b1` (tần số 7):**

Pattern P = {b1}. Thu classDist từ chain của b1:
- Node b1 dưới a1: {X:3, Y:1}
- Node b1 ở top level: {X:1, Y:1}
- **Tổng:** classDist = {X:4, Y:2}, itemSup = 7

Sinh CAR:
- `{b1} ⇒ X`: sup=4 ≥ 3, conf=4/7≈0.571 ≥ 0.5 → **sinh**
- `{b1} ⇒ Y`: sup=2 < 3 → bỏ

**Minh hoạ với item `a1` (tần số 7):**

Pattern P = {a1}. classDist từ node a1: {X:5, Y:2}.
- `{a1} ⇒ X`: sup=5, conf=5/7≈0.714 → **sinh**
- `{a1} ⇒ Y`: sup=2 < 3 → bỏ

**Pattern nhiều item** (sau đệ quy trên conditional trees):
- `{a1, b1} ⇒ X`: sup=3, conf=3/4=0.75 → **sinh**
- `{a1, b1} ⇒ Y`: sup=1 < 3 → bỏ
- `{a1, c1} ⇒ X`: sup=4, conf=4/4=1.00 → **sinh**
- `{b1, c1} ⇒ X`: sup=4, conf=4/4=1.00 → **sinh**

Giả sử sau bước này có **~10 luật ứng viên**:

| # | Luật | sup | conf |
|:-:|------|:---:|:----:|
| 1 | $\{a1, c1\} \Rightarrow X$ | 4 | 1.000 |
| 2 | $\{b1, c1\} \Rightarrow X$ | 4 | 1.000 |
| 3 | $\{c1\} \Rightarrow X$ | 6 | 1.000 |
| 4 | $\{a1, b1\} \Rightarrow X$ | 3 | 0.750 |
| 5 | $\{a1\} \Rightarrow X$ | 5 | 0.714 |
| 6 | $\{b1\} \Rightarrow X$ | 4 | 0.571 |
| 7 | $\{c0\} \Rightarrow Y$ | 3 | 0.750 |
| 8 | $\{b0\} \Rightarrow Y$ | 1 | 0.333 (loại vì conf<0.5) |

**Sắp theo precedence (conf↓, sup↓, size↑):**

| Thứ tự | Luật | conf | sup | size |
|:------:|------|:----:|:---:|:----:|
| 1 | $\{c1\} \Rightarrow X$ | 1.00 | 6 | 1 |
| 2 | $\{a1, c1\} \Rightarrow X$ | 1.00 | 4 | 2 |
| 3 | $\{b1, c1\} \Rightarrow X$ | 1.00 | 4 | 2 |
| 4 | $\{a1, b1\} \Rightarrow X$ | 0.75 | 3 | 2 |
| 5 | $\{c0\} \Rightarrow Y$ | 0.75 | 3 | 1 |
| 6 | $\{a1\} \Rightarrow X$ | 0.714 | 5 | 1 |
| 7 | $\{b1\} \Rightarrow X$ | 0.571 | 4 | 1 |

### 12.5 Bước 5 — Pruning 1 (General Rule)

| # | Luật | Đã giữ cùng class | Kiểm tra | Kết quả |
|:-:|------|-------------------|---------|---------|
| 1 | $\{c1\} \Rightarrow X$ | — | — | **Giữ** |
| 2 | $\{a1, c1\} \Rightarrow X$ | $\{c1\}$ | $\{c1\} \subseteq \{a1,c1\}$ ✓ | **Loại** |
| 3 | $\{b1, c1\} \Rightarrow X$ | $\{c1\}$ | $\{c1\} \subseteq \{b1,c1\}$ ✓ | **Loại** |
| 4 | $\{a1, b1\} \Rightarrow X$ | $\{c1\}$ | $\{c1\} \not\subseteq \{a1,b1\}$ | **Giữ** |
| 5 | $\{c0\} \Rightarrow Y$ | — | — | **Giữ** |
| 6 | $\{a1\} \Rightarrow X$ | $\{c1\}, \{a1,b1\}$ | Cả hai không phải ⊆ $\{a1\}$ | **Giữ** |
| 7 | $\{b1\} \Rightarrow X$ | $\{c1\}, \{a1,b1\}, \{a1\}$ | Cả 3 không ⊆ $\{b1\}$ | **Giữ** |

**Còn 5 luật:** $\{c1\}\Rightarrow X$, $\{a1,b1\}\Rightarrow X$, $\{c0\}\Rightarrow Y$, $\{a1\}\Rightarrow X$, $\{b1\}\Rightarrow X$.

### 12.6 Bước 6 — Pruning 2 (χ²)

**Tính χ² cho từng luật** (n=10, sup(X)=7, sup(Y)=3):

**$R_1 : \{c1\} \Rightarrow X$** (sup(c1)=6, sup(R)=6)

| | X | Y | tổng |
|-|:-:|:-:|:----:|
| c1 | 6 | 0 | 6 |
| ¬c1 | 1 | 3 | 4 |

$\chi^2 = \frac{10(6 \cdot 3 - 0 \cdot 1)^2}{6 \cdot 4 \cdot 7 \cdot 3} = \frac{10 \cdot 324}{504} \approx 6.43$

$ad=18 > b\gamma=0$ → **Giữ** (6.43 ≥ 2.706)

**$R_2 : \{a1, b1\} \Rightarrow X$** (sup(a1,b1)=4, sup(R)=3)

| | X | Y | tổng |
|-|:-:|:-:|:----:|
| a1,b1 | 3 | 1 | 4 |
| ¬(a1,b1) | 4 | 2 | 6 |

$\chi^2 = \frac{10(3 \cdot 2 - 1 \cdot 4)^2}{4 \cdot 6 \cdot 7 \cdot 3} = \frac{10 \cdot 4}{504} \approx 0.079$

$\chi^2 < 2.706$ → **Loại**

**$R_3 : \{c0\} \Rightarrow Y$** (sup(c0)=4, sup(R)=3)

| | Y | X | tổng |
|-|:-:|:-:|:----:|
| c0 | 3 | 1 | 4 |
| ¬c0 | 0 | 6 | 6 |

$\chi^2 = \frac{10(3 \cdot 6 - 1 \cdot 0)^2}{4 \cdot 6 \cdot 3 \cdot 7} = \frac{10 \cdot 324}{504} \approx 6.43$

$ad=18 > b\gamma=0$ → **Giữ**

**$R_4 : \{a1\} \Rightarrow X$** (sup(a1)=7, sup(R)=5)

| | X | Y | tổng |
|-|:-:|:-:|:----:|
| a1 | 5 | 2 | 7 |
| ¬a1 | 2 | 1 | 3 |

$\chi^2 = \frac{10(5 \cdot 1 - 2 \cdot 2)^2}{7 \cdot 3 \cdot 7 \cdot 3} = \frac{10 \cdot 1}{441} \approx 0.023$

→ **Loại**

**$R_5 : \{b1\} \Rightarrow X$** (sup(b1)=7, sup(R)=4)

| | X | Y | tổng |
|-|:-:|:-:|:----:|
| b1 | 4 | 3 | 7 |
| ¬b1 | 3 | 0 | 3 |

$ad = 0, b\gamma = 9$ → $ad < b\gamma$ → **tương quan âm** → **Loại** (dù χ² có thể cao)

**Sau P2:** Còn 2 luật:
- $\{c1\} \Rightarrow X$ (χ²=6.43)
- $\{c0\} \Rightarrow Y$ (χ²=6.43)

### 12.7 Bước 7 — Pruning 3 (Coverage, δ=2)

**Luật còn lại sắp theo precedence (cả 2 có conf=1.0, lấy theo sup):**

| # | Luật | sup | conf |
|:-:|------|:---:|:----:|
| 1 | $\{c1\} \Rightarrow X$ | 6 | 1.00 |
| 2 | $\{c0\} \Rightarrow Y$ | 3 | 1.00 |

**Lặp Algorithm 1:**

- **Xét R₁ = {c1}⇒X:** cover các bản ghi có c1 VÀ class X: records 1, 3, 5, 6, 9, 10 (6 records). Tất cả được cover_count = 1 (chưa ≥ 2) → giữ tất cả trong T. → selected = [R₁].
- **Xét R₂ = {c0}⇒Y:** cover các bản ghi có c0 VÀ class Y: records 4, 7 (2 records). cover_count = 1. → selected = [R₁, R₂].

Không luật nào còn. Dừng.

**T còn lại:** mọi 10 records vẫn ở T vì chưa có cái nào đạt cover ≥ 2. Nhưng set luật đã rỗng.

**Kết quả cuối:** 2 luật:
$$
R_1 : \{c=1\} \Rightarrow X
$$
$$
R_2 : \{c=0\} \Rightarrow Y
$$

### 12.8 Bước 8 — Xây CR-tree lưu trữ

Chèn 2 luật vào CR-tree. Cả hai chỉ có 1 item → CR-tree đơn giản:

```
root
├── c1 → [R1: X, sup=6, conf=1.0, χ²=6.43]
└── c0 → [R2: Y, sup=3, conf=1.0, χ²=6.43]
```

### 12.9 Bước 9 — Phân lớp bản ghi test

**Test record:** $t = \{a=1, b=0, c=1\}$, class thực tế = ?

**Truy vấn CR-tree:**
- Tại root, xét children: `c1` có trong t (vì c=1) → đi xuống, thu R₁.
- `c0` không có trong t → bỏ qua.
- `R_t = \{R_1\}$

**Bước kiểm tra:**
- $R_t \ne \emptyset$ → qua bước 2.
- $|\text{byClass}| = 1$ (chỉ class X) → trả về **X**.

**Dự đoán:** class X.

**Test record khác:** $t' = \{a=1, b=1, c=0\}$.

- Truy vấn: `c0` khớp → R₂; `c1` không khớp.
- $R_t = \{R_2\}$ → trả về class Y.

**Dự đoán:** class Y.

### 12.10 Tổng kết luồng

| Bước | Tên | Số luật/items |
|:----:|-----|:-------------:|
| 0 | Load data | 10 records |
| 1 | Đếm tần số | 6 items (sau lọc) |
| 2 | Xây CR-tree khởi tạo | 1 cây |
| 3+4 | FP-Growth + sinh CAR | 7 luật ứng viên |
| 5 | Pruning 1 | 7 → 5 |
| 6 | Pruning 2 | 5 → 2 |
| 7 | Pruning 3 | 2 → 2 |
| 8 | Xây CR-tree lưu trữ | 2 luật |
| 9 | Classify | Cho từng record test |

---

### 12.11 Ví Dụ Thực Tế — Mushroom Dataset (UCI)

Phần 12.1–12.10 minh hoạ pipeline trên dataset tổng hợp nhỏ để dễ theo dõi. Phần này áp dụng CMAR trên **Mushroom Dataset** — một dataset chuẩn UCI kinh điển để kiểm nghiệm thuật toán associative classification.

#### 12.11.1 Giới thiệu dataset

**Mushroom Dataset (Agaricus–Lepiota)** — thu thập từ The Audubon Society Field Guide to North American Mushrooms (1981).

| Thuộc tính | Giá trị |
|------------|---------|
| Nguồn | UCI Machine Learning Repository |
| Số bản ghi | **8,124** |
| Số thuộc tính | **22** (tất cả categorical) |
| Số class | **2** — edible (ăn được) vs poisonous (độc) |
| Missing values | Có (cột `stalk-root` có giá trị `?`) |
| File sử dụng | `data/mushroom_full.csv` |

**Phân bố lớp:**

| Class | Ý nghĩa | Số bản ghi | Tỉ lệ |
|:-----:|---------|:----------:|:-----:|
| `e` | edible (ăn được) | **4,208** | 51.80% |
| `p` | poisonous (độc) | **3,916** | 48.20% |
| **Tổng** | | **8,124** | 100% |

Dataset gần cân bằng → không gặp vấn đề imbalance nặng.

#### 12.11.2 Danh sách 22 thuộc tính

| # | Tên | # giá trị | Ví dụ giá trị |
|:-:|-----|:---------:|---------------|
| 1 | cap-shape | 6 | bell=b, conical=c, convex=x, flat=f, knobbed=k, sunken=s |
| 2 | cap-surface | 4 | fibrous=f, grooves=g, scaly=y, smooth=s |
| 3 | cap-color | 10 | brown=n, buff=b, cinnamon=c, gray=g, green=r, pink=p, purple=u, red=e, white=w, yellow=y |
| 4 | bruises | 2 | bruises=t, no=f |
| 5 | **odor** | **9** | **almond=a, anise=l, creosote=c, fishy=y, foul=f, musty=m, none=n, pungent=p, spicy=s** |
| 6 | gill-attachment | 2 | attached=a, free=f |
| 7 | gill-spacing | 2 | close=c, crowded=w |
| 8 | gill-size | 2 | broad=b, narrow=n |
| 9 | gill-color | 12 | ... |
| 10 | stalk-shape | 2 | enlarging=e, tapering=t |
| 11 | stalk-root | 5+`?` | bulbous=b, club=c, equal=e, rooted=r (+ missing `?`) |
| 12 | stalk-surface-above-ring | 4 | fibrous=f, scaly=y, silky=k, smooth=s |
| 13 | stalk-surface-below-ring | 4 | như trên |
| 14 | stalk-color-above-ring | 9 | ... |
| 15 | stalk-color-below-ring | 9 | ... |
| 16 | veil-type | 1 | partial=p (toàn bộ dataset cùng 1 giá trị → bỏ qua) |
| 17 | veil-color | 4 | brown=n, orange=o, white=w, yellow=y |
| 18 | ring-number | 3 | none=n, one=o, two=t |
| 19 | ring-type | 5 | evanescent=e, flaring=f, large=l, none=n, pendant=p |
| 20 | spore-print-color | 9 | ... |
| 21 | population | 6 | abundant=a, clustered=c, numerous=n, scattered=s, several=v, solitary=y |
| 22 | habitat | 7 | grasses=g, leaves=l, meadows=m, paths=p, urban=u, waste=w, woods=d |
| **class** | edible/poisonous | 2 | e, p |

Tổng số item khả dĩ (sau mã hoá `attr=value`): khoảng **115+ items**.

#### 12.11.3 Tiền xử lý & cấu hình chạy

**Tiền xử lý:**
- Chuyển mỗi ô thành item `column=value`, ví dụ `odor=f`, `cap-shape=x`.
- Bỏ qua các ô giá trị `?` (missing).
- Bỏ cột `veil-type` (tất cả = p → không mang thông tin).

**Cấu hình CMAR:**

| Tham số | Giá trị | Ghi chú |
|---------|:-------:|--------|
| Train/Test | 80/20 | 6499 train / 1625 test |
| Min Support | **15%** (≈1097 abs) | Cao để kiểm soát bộ nhớ do dataset lớn |
| Min Confidence | 50% | Mặc định |
| χ² threshold | 3.841 | α = 0.05, df = 1 |
| Coverage δ | 4 | Mặc định bài báo |
| Random seed | 42 | Tái lập |

**Lưu ý về minSup = 15%:** Dataset có 8124 records → minSup ≈ 1097. Mushroom có nhiều luật cực mạnh (conf = 100% với sup rất cao), nên dùng minSup cao vẫn đủ luật.

#### 12.11.4 Phân tích sức mạnh của thuộc tính `odor`

Mushroom dataset nổi tiếng vì thuộc tính **odor** (mùi) gần như **quyết định độc/ăn được**. Bảng phân bố `odor × class`:

| Giá trị odor | Ý nghĩa | edible | poisonous | Tổng | Luật tiềm năng |
|:------------:|---------|:------:|:---------:|:----:|----------------|
| `a` | almond | 400 | 0 | 400 | `odor=a ⇒ edible` (100%) |
| `l` | anise | 400 | 0 | 400 | `odor=l ⇒ edible` (100%) |
| `n` | none | 3408 | 120 | 3528 | `odor=n ⇒ edible` (96.6%) |
| `c` | creosote | 0 | 192 | 192 | `odor=c ⇒ poisonous` (100%) |
| `y` | fishy | 0 | 576 | 576 | `odor=y ⇒ poisonous` (100%) |
| `f` | foul | 0 | 2160 | 2160 | `odor=f ⇒ poisonous` (100%) |
| `m` | musty | 0 | 36 | 36 | `odor=m ⇒ poisonous` (100%) — sup quá thấp |
| `p` | pungent | 0 | 256 | 256 | `odor=p ⇒ poisonous` (100%) |
| `s` | spicy | 0 | 576 | 576 | `odor=s ⇒ poisonous` (100%) |
| **Tổng** | | **4208** | **3916** | **8124** | |

**Quan sát quan trọng:**
- 7/9 giá trị odor cho confidence **100%** (pure rule).
- Chỉ `odor=n` (không mùi) có lẫn lộn: 96.6% edible, 3.4% poisonous.
- Dùng riêng `odor` đã phân lớp đúng **~98.5%** dataset (3408+400+400 + 2160+576+576+192+256+36 = 8004/8124 = **98.5%**).

Đây chính là lý do Mushroom thường đạt **100% accuracy** với mọi thuật toán rule-based — dataset có "golden rules".

#### 12.11.5 Tính chi tiết các độ đo cho 3 luật điển hình

Xét **toàn bộ 8124 bản ghi** (không chia train/test để trình bày).

##### Luật L₁: `odor=f ⇒ poisonous`

**Bảng contingency:**

| | poisonous | edible | **Tổng** |
|-|:---------:|:------:|:--------:|
| `odor=f` | $a = 2160$ | $b = 0$ | 2160 |
| `odor≠f` | $\gamma = 1756$ | $d = 4208$ | 5964 |
| **Tổng** | 3916 | 4208 | **8124** |

**Các độ đo:**

| Độ đo | Công thức | Tính toán | Kết quả |
|-------|-----------|-----------|:-------:|
| sup (abs) | $a$ | — | **2160** |
| sup (rel) | $a/n$ | $2160/8124$ | **0.266** |
| conf | $a/(a+b)$ | $2160/2160$ | **1.000** |
| lift | $\text{conf}/\sigma(c)$ | $1.0 / (3916/8124)$ | **2.075** |
| leverage | $\sigma(R) - \sigma(P)\sigma(c)$ | $0.266 - 0.266 \times 0.482$ | **+0.138** |
| conviction | $(1-\sigma(c))/(1-\text{conf})$ | $(1-0.482)/(1-1)$ | **∞** |
| $ad - b\gamma$ | | $2160 \times 4208 - 0$ | **9,089,280** |

**Chi-square:**
$$
\chi^2 = \frac{8124 \cdot (2160 \cdot 4208 - 0)^2}{2160 \cdot 5964 \cdot 3916 \cdot 4208}
$$
$$
= \frac{8124 \cdot 8.26 \times 10^{13}}{2.75 \times 10^{14}} \approx \mathbf{2441}
$$

**max χ²:** $a_{\max} = \min(2160, 3916) = 2160$ → giống hệt $a$ hiện tại → $\max\chi^2 \approx 2441$.

**Tỉ số chuẩn hoá:** $\chi^2 / \max\chi^2 = 1.00$ — luật **hoàn hảo**.

**Kết luận L₁:** Mọi độ đo đều tối đa — luật chắc chắn nhất có thể.

##### Luật L₂: `odor=n ⇒ edible`

**Bảng contingency:**

| | edible | poisonous | **Tổng** |
|-|:------:|:---------:|:--------:|
| `odor=n` | $a = 3408$ | $b = 120$ | 3528 |
| `odor≠n` | $\gamma = 800$ | $d = 3796$ | 4596 |
| **Tổng** | 4208 | 3916 | **8124** |

**Các độ đo:**

| Độ đo | Tính toán | Kết quả |
|-------|-----------|:-------:|
| sup (abs) | | **3408** |
| sup (rel) | $3408/8124$ | **0.419** |
| conf | $3408/3528$ | **0.966** |
| lift | $0.966/(4208/8124)$ | **1.866** |
| leverage | $0.419 - 0.434 \times 0.518$ | **+0.195** |
| $ad - b\gamma$ | $3408 \times 3796 - 120 \times 800$ | **12,840,768** |

**Chi-square:**
$$
\chi^2 = \frac{8124 \cdot 12{,}840{,}768^2}{3528 \cdot 4596 \cdot 4208 \cdot 3916} \approx \mathbf{5021}
$$

**Max χ²:** $a_{\max} = \min(3528, 4208) = 3528$
- $b = 0$, $\gamma = 4208 - 3528 = 680$, $d = 8124 - 3528 - 0 - 680 = 3916$
$$
\max\chi^2 = \frac{8124 \cdot (3528 \cdot 3916)^2}{3528 \cdot 4596 \cdot 4208 \cdot 3916} \approx 5379
$$

**Tỉ số:** $\chi^2 / \max\chi^2 \approx 5021/5379 \approx \mathbf{0.933}$

**Kết luận L₂:** Rất mạnh nhưng không "hoàn hảo" như L₁ (vì có 120 ngoại lệ là poisonous).

##### Luật L₃: `odor=a ⇒ edible`

**Bảng contingency:**

| | edible | poisonous | **Tổng** |
|-|:------:|:---------:|:--------:|
| `odor=a` | $a = 400$ | $b = 0$ | 400 |
| `odor≠a` | $\gamma = 3808$ | $d = 3916$ | 7724 |
| **Tổng** | 4208 | 3916 | **8124** |

**Các độ đo:**

| Độ đo | Kết quả |
|-------|:-------:|
| sup (abs) | **400** |
| sup (rel) | **0.049** |
| conf | **1.000** |
| lift | $1.0/(4208/8124) \approx$ **1.931** |
| $ad - b\gamma$ | $400 \times 3916 = 1{,}566{,}400$ |

$$
\chi^2 = \frac{8124 \cdot 1{,}566{,}400^2}{400 \cdot 7724 \cdot 4208 \cdot 3916} \approx \mathbf{411}
$$

$\max\chi^2$: $a_{\max} = \min(400, 4208) = 400$ → giống hệt → $\max\chi^2 \approx 411$ → **tỉ số = 1.00**.

**Kết luận L₃:** Sup không lớn nhưng luật pure → χ²/maxχ² = 1.00.

#### 12.11.6 Tổng hợp so sánh 3 luật

| Luật | sup | conf | lift | χ² | χ²/maxχ² | Xếp hạng |
|------|:---:|:----:|:----:|:---:|:---------:|:--------:|
| L₁: `odor=f ⇒ p` | 2160 | 1.000 | 2.075 | 2441 | **1.00** | ≡ L₃ |
| L₂: `odor=n ⇒ e` | 3408 | 0.966 | 1.866 | 5021 | 0.933 | 3 |
| L₃: `odor=a ⇒ e` | 400 | 1.000 | 1.931 | 411 | **1.00** | ≡ L₁ |

**Bài học quan trọng về χ² vs χ²/maxχ²:**
- Theo **χ² thô**, L₂ (5021) > L₁ (2441) > L₃ (411) → nếu chỉ dùng χ², L₂ mạnh nhất.
- Theo **χ²/maxχ²**, L₁ và L₃ đều = 1.00 > L₂ (0.933) → đây là chuẩn CMAR dùng.

Normalization $\chi^2/\max\chi^2$ "công bằng" giữa các luật có sup khác nhau — L₃ chỉ có sup = 400 nhưng vẫn được đánh giá cao như L₁.

#### 12.11.7 FP-Growth & Sinh CAR trên Mushroom

**Đầu vào bước FP-Growth:**
- N = 6499 (train), minSup = 15% = 975 abs (nếu tính theo train size) hoặc 1097 (nếu dùng 15% × 8124).

**Items frequent sau lọc (ví dụ):**

| Item | Support (train) | Frequency |
|------|:---------------:|:---------:|
| `veil-color=w` | 6364 | 97.9% |
| `gill-attachment=f` | 6343 | 97.6% |
| `ring-number=o` | 5924 | 91.2% |
| `stalk-shape=t` | 3480 | 53.5% |
| `bruises=f` | 3936 | 60.6% |
| `odor=n` | 2828 | 43.5% |
| `odor=f` | 1739 | 26.8% |
| `gill-size=b` | 4562 | 70.2% |
| `cap-shape=x` | 3064 | 47.1% |
| … | … | … |

**Số lượng typical:**

| Chỉ số | Giá trị |
|--------|:-------:|
| Item frequent (sau lọc minSup=15%) | ~30–40 |
| Frequent patterns | **vài nghìn** |
| CAR ứng viên (conf ≥ 0.5) | **vài trăm đến vài nghìn** |

#### 12.11.8 Kết quả 3 tầng pruning trên Mushroom

**Thứ tự thực hiện:**

| Tầng | Số luật | Loại bỏ | % loại |
|:----:|:-------:|:-------:|:------:|
| Ứng viên ban đầu | ~2000 | — | — |
| Sau P1 (general) | ~300 | ~1700 | 85% |
| Sau P2 (χ²) | ~200 | ~100 | 33% |
| Sau P3 (coverage δ=4) | **~50–100** | ~100 | 50% |
| **Tổng cắt** | | | **~95%** |

**Các luật sống sót điển hình sau pruning:**

| # | Luật | sup | conf | χ² |
|:-:|------|:---:|:----:|:---:|
| 1 | `odor=f ⇒ poisonous` | 1739 | 1.000 | ~1950 |
| 2 | `odor=n ⇒ edible` | 2728 | 0.968 | ~4020 |
| 3 | `odor=a ⇒ edible` | 320 | 1.000 | ~330 |
| 4 | `odor=l ⇒ edible` | 320 | 1.000 | ~330 |
| 5 | `odor=p ⇒ poisonous` | 205 | 1.000 | ~230 |
| 6 | `odor=y ⇒ poisonous` | 461 | 1.000 | ~520 |
| 7 | `odor=s ⇒ poisonous` | 461 | 1.000 | ~520 |
| 8 | `odor=c ⇒ poisonous` | 154 | 1.000 | ~170 |
| 9 | `spore-print=r ⇒ poisonous` | ~72 | 1.000 | ~80 |
| 10 | `gill-color=b ⇒ poisonous` | ~1728 | 1.000 | ~1940 |

**Quan sát:** Luật `odor=*` chiếm chủ đạo — Pruning 1 đã loại hầu hết luật chi tiết hơn (vd `odor=f ∧ cap-shape=x ⇒ poisonous` bị lấn át bởi `odor=f ⇒ poisonous`).

#### 12.11.9 Phân lớp một bản ghi test cụ thể

**Bản ghi test mẫu (bản ghi thứ 1 trong dataset):**
```
cap-shape=x, cap-surface=s, cap-color=n, bruises=t,
odor=p, gill-attachment=f, gill-spacing=c, gill-size=n,
gill-color=k, stalk-shape=e, stalk-root=e, ...
```

Class thực: **poisonous**.

**Bước 1 — Truy vấn CR-tree:**

Các luật đã lưu, chỉ các luật có condset ⊆ bản ghi test được lấy ra:
- `odor=p ⇒ poisonous` ✓ (odor=p có trong bản ghi)
- `gill-size=n` — không có luật tương ứng hoặc bị pruning
- `gill-color=k` — tương tự

→ $\mathcal{R}(t) = \{R_{odor=p}\}$

**Bước 2–3:** $|\mathcal{R}(t)| = 1$, đồng nhất class poisonous → **trả về poisonous**. ✓

**Bản ghi test khác** — `odor=n, gill-size=b, cap-shape=x, ...`:
- Luật khớp: `odor=n ⇒ edible` (conf=96.6%), có thể cả `gill-size=b ⇒ edible` (nếu tồn tại)
- Tất cả đồng nhất class edible → trả về **edible**.

**Bản ghi đa class (hiếm trong Mushroom):** Giả sử bản ghi có `odor=n, spore-print=r`:
- `odor=n ⇒ edible` (conf=96.6%, χ²=5021, χ²/maxχ²=0.933)
- `spore-print=r ⇒ poisonous` (conf=1.00, χ²=80, χ²/maxχ²=1.00)

Hai luật → khác class → dùng **weighted χ²**:

$$
\text{score}(\text{edible}) = \frac{5021^2}{5379} \approx 4688
$$
$$
\text{score}(\text{poisonous}) = \frac{80^2}{80} = 80
$$

→ **edible** thắng (score 4688 >> 80) dù có luật poisonous "perfect" khác.

**Nhưng:** Trong thực tế mushroom, nếu `spore-print=r` xuất hiện → hầu như luôn poisonous (conf = 100%), nên CMAR có thể dự đoán sai ở đây. Đây là lý do bài báo đạt 100% còn cài đặt có thể 98–99%.

#### 12.11.10 Kết quả tổng hợp trên Mushroom

**Kết quả benchmark (theo `benchmark_20_datasets.md`):**

| Phương pháp | Accuracy | Ghi chú |
|-------------|:--------:|---------|
| **Bài báo — CMAR** | **100.00%** | Table 3 — dataset không có trong Table 3 gốc nhưng trong literature |
| **Bài báo — CBA** | 100.00% | |
| **Bài báo — C4.5** | 100.00% | |
| **Cài đặt CMAR (10-fold CV)** | **98.94%** | Chênh -1.06% |
| **Cài đặt CMAR (80/20 split)** | 89.17% | minSup=1500 (quá cao do giới hạn bộ nhớ) |

**Giải thích chênh lệch:**
1. Dataset có golden rules (odor) → mọi phương pháp đều đạt gần 100%.
2. Khi dùng minSup cao (15–23%), một số luật cho minority class bị loại → accuracy giảm nhẹ.
3. Cài đặt 10-fold CV với minSup=15% đạt **98.94%** — rất sát bài báo.

#### 12.11.11 Bài học rút ra từ Mushroom Example

**1. Tầm quan trọng của thuộc tính có tính phân biệt (discriminative features):**
- Một thuộc tính như `odor` đủ để đạt gần 100% accuracy.
- CMAR (và hầu hết thuật toán) **phát hiện tự động** các thuộc tính này qua χ² và support cao.

**2. Pruning cực kỳ hiệu quả trên dataset "dễ":**
- Từ 2000 luật ứng viên → ~100 luật cuối cùng (cắt 95%).
- Pruning 1 loại ~85% vì nhiều luật chi tiết bị luật `odor=*` đơn giản lấn át.

**3. χ²/maxχ² khiến luật nhỏ vẫn có trọng lượng công bằng:**
- Luật `odor=a ⇒ edible` (sup=400) và `odor=f ⇒ poisonous` (sup=2160) đều có tỉ số 1.00.
- CMAR không bỏ rơi luật "hiếm" nếu chúng pure.

**4. Khi dataset có golden rules, CMAR ≈ CBA ≈ C4.5:**
- Sự khác biệt giữa các thuật toán chủ yếu lộ ra trên dataset **khó** (class overlap, noise, imbalance).
- Trên Mushroom: chênh lệch < 1%.

**5. Vai trò của minSup khi dataset lớn:**
- Giảm minSup → thêm luật → accuracy tăng nhưng bộ nhớ/thời gian tăng.
- Tăng minSup → mất luật cho minority → accuracy giảm.
- Mushroom: minSup 15% (1097 abs) là trade-off hợp lý.

#### 12.11.12 Bảng so sánh tổng hợp 2 ví dụ

| Chỉ số | Ví dụ 10 bản ghi (§12.1–12.10) | Mushroom (§12.11) |
|--------|:-------------------------------:|:------------------:|
| Số bản ghi | 10 | 8,124 |
| Số thuộc tính | 3 | 22 |
| Số class | 2 | 2 |
| Items khả dĩ | 6 | ~115 |
| Frequent patterns | ~10 | vài nghìn |
| CAR ứng viên | 7 | ~2000 |
| Sau P1 | 5 | ~300 |
| Sau P2 | 2 | ~200 |
| Sau P3 | 2 | ~100 |
| Accuracy | — (synthetic) | **98.94%** |
| Đặc điểm | Dạy thuật toán | Kiểm nghiệm thực tế |

**Kết luận:** Mushroom là dataset **benchmark vàng** cho associative classification — kết quả ~99% trên mọi phương pháp → không dùng để phân biệt thuật toán, mà để **kiểm chứng tính đúng đắn** của cài đặt.

---

# PHẦN C — PHÂN TÍCH & ĐÁNH GIÁ

---

## 13. Phân Tích Ưu – Nhược Điểm

### 13.1 Ưu điểm

**★ Độ chính xác cao**
- Dùng nhiều luật → giảm bias, overfitting.
- Bài báo báo cáo: 85.22% trung bình trên 26 dataset UCI (C4.5: 84.09%, CBA: 84.69%).
- Thắng 13/26 dataset (50%).

**★ Hiệu quả bộ nhớ**
- CR-tree tiết kiệm ~77.12% bộ nhớ so với CBA (Table 4 bài báo).
- Với 500 luật, danh sách phẳng cần ~13 cell (13 item), CR-tree chỉ cần ~9 node.

**★ Hiệu suất tính toán**
- FP-Growth: không cần sinh ứng viên như Apriori → nhanh hơn 10–50 lần.
- Pruning sớm → giảm tải các bước sau.
- Ví dụ Sonar: CMAR 19s vs CBA 226s.

**★ Xử lý class imbalance**
- Normalization $\chi^2/\max\chi^2$ khắc phục bias về minority class.

**★ Giải thích được**
- Mỗi dự đoán kèm tập luật "ủng hộ" và "phản đối" → chuyên gia có thể kiểm tra.

### 13.2 Nhược điểm

**✗ Nhạy với tham số**
- minSup, minConf, δ, χ²_thr đều ảnh hưởng mạnh.
- Bài báo thừa nhận: "there seems no way to pre-determine the best threshold values".

**✗ Chỉ xử lý thuộc tính rời rạc**
- Thuộc tính liên tục cần discretize trước.

**✗ Khó với số thuộc tính lớn**
- Dataset "wide" + minSup thấp → pattern bùng nổ $O(2^n)$.

**✗ Không cập nhật tăng dần**
- Thêm dữ liệu → xây lại toàn bộ.

### 13.3 So sánh tổng hợp với các đối thủ

| Tiêu chí | C4.5 | CBA | **CMAR** |
|----------|:----:|:---:|:--------:|
| Accuracy trung bình 26 UCI | 84.09% | 84.69% | **85.22%** |
| Số luật dùng classify | 1 (path) | 1 (best) | **Nhiều** |
| Bộ nhớ (vs CBA) | Nhỏ | Lớn | **-77%** |
| Tốc độ train | Nhanh | Chậm | **Trung bình** |
| Interpretable | ✓ | ✓ | ✓ |
| Class imbalance | Kém | Kém | **Tốt** |

---

## 14. Tham Số & Hướng Dẫn Tinh Chỉnh

### 14.1 Danh sách tham số

| Tham số | Kí hiệu | Mặc định | Phạm vi hợp lý |
|---------|:-------:|:--------:|:--------------:|
| Min Support | $\text{minSup}$ | 1% | 0.5% – 10% |
| Min Confidence | $\text{minConf}$ | 50% | 30% – 90% |
| χ² threshold | $\chi^2_{\text{thr}}$ | 3.841 | 2.706 – 10.827 |
| Coverage delta | $\delta$ | 4 | 1 – 10 |

### 14.2 Hướng dẫn theo đặc điểm dataset

| Đặc điểm | minSup | minConf | δ | χ²_thr |
|----------|:------:|:-------:|:-:|:------:|
| Nhỏ (<500), cân bằng | 2 (abs) | 0.5 | 2 | 2.706 |
| Trung bình | 1–2% | 0.5 | 4 | 3.841 |
| Lớn (>5000) | 1% | 0.5 | 4 | 3.841 |
| Imbalance nặng | ↓0.5% | 0.5 | ↑6 | ↓2.706 |
| Wide data (>30 attr) | ↑3% | ↑0.6 | 4 | 3.841 |
| Noise cao | ↑2% | ↑0.7 | 4 | ↑6.635 |

### 14.3 Tinh chỉnh lặp

1. **Baseline:** minSup=1%, minConf=0.5, δ=4, χ²_thr=3.841
2. **Nếu accuracy thấp + minority class bị miss:**
   - Giảm minSup
   - Giảm χ²_thr
   - Tăng δ
3. **Nếu quá chậm / bùng nổ pattern:**
   - Tăng minSup
   - Tăng minConf
4. **Nếu overfitting (train >> test):**
   - Tăng χ²_thr (chặt hơn)
   - Tăng minConf

---

## 15. Kết Luận

### 15.1 Tóm lược đóng góp của CMAR

CMAR là thuật toán **associative classification** tiêu biểu với ba đóng góp then chốt:

1. **Phân lớp dựa nhiều luật qua weighted χ²** — khắc phục bias của single-rule CBA, xử lý tốt class imbalance.
2. **CR-tree** — cấu trúc nén luật, tiết kiệm ~77% bộ nhớ, truy vấn nhanh qua DFS cắt tỉa.
3. **FP-Growth biến thể nhận diện class** — sinh CAR trực tiếp trong lúc đào mỏ, rút gọn từ 2 pass xuống 1 pass.

### 15.2 Hệ thống luật hoàn chỉnh

Từ góc nhìn "luật":
- **Định nghĩa chặt chẽ** CAR: $R : P \Rightarrow c$
- **Độ đo đa dạng:** support, confidence, lift, leverage, conviction, χ², maxχ²
- **Ngưỡng kép:** minSup (đủ phổ biến) + minConf (đủ tin cậy) + χ²_thr (đủ ý nghĩa thống kê) + $ad > b\gamma$ (tương quan dương)
- **Thứ tự ưu tiên:** conf ↓ → sup ↓ → size ↑
- **Ba tầng pruning:** general-rule → χ² → coverage

### 15.3 Quy trình CMAR hoàn chỉnh (end-to-end)

```
Dataset T  →  Count freq  →  Build CR-tree (initial)  →  FP-Growth + Direct CAR
          →  Prune 1 (General)  →  Prune 2 (χ²)  →  Prune 3 (Coverage)
          →  Build CR-tree (storage)  →  [Model Ready]

Test record t  →  CR-tree lookup  →  Match group  →  Weighted χ²  →  Predict class
```

### 15.4 Tầm ảnh hưởng

CMAR đặt nền móng cho nhiều nghiên cứu tiếp theo về associative classification, và các ý tưởng như:
- **Mining pattern kèm metadata (class distribution)** → lan sang Stream mining
- **CR-tree** → tiền thân của nhiều index cho rule store
- **Weighted aggregation** → cảm hứng cho ensemble methods

---

## 16. Tài Liệu Tham Khảo

1. **Li, W., Han, J., & Pei, J. (2001).** *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* ICDM 2001, pp. 369–376.

2. **Han, J., Pei, J., & Yin, Y. (2000).** *Mining Frequent Patterns without Candidate Generation.* SIGMOD 2000.

3. **Liu, B., Hsu, W., & Ma, Y. (1998).** *Integrating Classification and Association Rule Mining.* KDD'98, pp. 80–86.

4. **Agrawal, R., & Srikant, R. (1994).** *Fast Algorithms for Mining Association Rules.* VLDB'94.

5. **Piatetsky-Shapiro, G. (1991).** *Discovery, Analysis, and Presentation of Strong Rules.* Knowledge Discovery in Databases.

6. **Brin, S., Motwani, R., Ullman, J., & Tsur, S. (1997).** *Dynamic Itemset Counting and Implication Rules for Market Basket Data.* SIGMOD'97.

7. **Dong, G., Zhang, X., Wong, L., & Li, J. (1999).** *CAEP: Classification by Aggregating Emerging Patterns.* DS'99.

8. **Quinlan, J. R. (1993).** *C4.5: Programs for Machine Learning.* Morgan Kaufmann.

9. **Tan, P.-N., Steinbach, M., & Kumar, V. (2006).** *Introduction to Data Mining.* Addison-Wesley (chapters on association analysis).

10. **UCI Machine Learning Repository.** https://archive.ics.uci.edu/ml/

---

## 17. Phụ Lục — Thuật Ngữ & Ký Hiệu

### 17.1 Thuật ngữ Việt – Anh

| Tiếng Việt | Tiếng Anh | Ký hiệu |
|------------|-----------|:-------:|
| Item / Phần tử | Item | — |
| Transaction / Bản ghi / Giao dịch | Transaction | $t$ |
| Itemset / Pattern / Mẫu | Itemset / Pattern | $P$ |
| Khớp | Match | $P \subseteq t$ |
| Luật kết hợp | Association rule | $R$ |
| Luật kết hợp lớp | Class Association Rule | CAR |
| Vế trái / Tiền đề / Condset | Antecedent / LHS / Condset | $P$ |
| Vế phải / Hệ quả | Consequent / RHS | $c$ |
| Độ hỗ trợ | Support | $\text{sup}, \sigma$ |
| Độ tin cậy | Confidence | $\text{conf}$ |
| Độ nâng | Lift | — |
| Kiểm định chi bình phương | Chi-square test | $\chi^2$ |
| Cận trên χ² | Max chi-square | $\max\chi^2$ |
| Chi-square có trọng số | Weighted chi-square | — |
| Bảng ngẫu nhiên 2×2 | 2×2 contingency table | — |
| Tương quan dương/âm | Positive/negative correlation | — |
| Ngưỡng | Threshold | — |
| Cắt tỉa luật | Rule pruning | — |
| Luật tổng quát/chi tiết | General/specific rule | — |
| Luật dư thừa | Redundant rule | — |
| Thứ tự ưu tiên | Rule precedence | $\succ$ |
| Độ bao phủ cơ sở dữ liệu | Database coverage | $\delta$ |
| Lớp mặc định | Default class | — |
| Cây tiền tố | Prefix tree | — |
| Bảng đầu mục | Header table | — |
| Liên kết node | Node-link | — |
| Cơ sở mẫu điều kiện | Conditional pattern base | — |
| Cây điều kiện | Conditional FP-tree | — |

### 17.2 Ký hiệu toán học

| Ký hiệu | Ý nghĩa |
|---------|---------|
| $T$ | Tập huấn luyện |
| $\|T\|$ = $N$ | Số bản ghi huấn luyện |
| $\mathcal{A}$ | Tập thuộc tính |
| $\mathcal{C}$ | Tập nhãn lớp |
| $\mathcal{I}$ | Tập mọi item khả dĩ |
| $t$ | Bản ghi |
| $\text{items}(t)$ | Tập item của bản ghi $t$ |
| $\text{class}(t)$ | Nhãn lớp của $t$ |
| $P, Q, X, Y$ | Pattern / itemset |
| $R: P \Rightarrow c$ | Luật kết hợp lớp |
| $\text{sup}(P)$, $\text{sup}(R)$ | Support tuyệt đối |
| $\sigma(P)$, $\sigma(R)$ | Support tương đối |
| $\text{conf}(R)$ | Confidence |
| $\text{lift}(R)$ | Lift |
| $\text{lev}(R)$ | Leverage |
| $\text{conv}(R)$ | Conviction |
| $\chi^2(R)$ | Chi-square |
| $\max\chi^2(R)$ | Cận trên χ² |
| $a, b, \gamma, d$ | Ô trong bảng contingency |
| $n = \|T\|$ | Tổng số bản ghi |
| $R_1 \succ R_2$ | $R_1$ ưu tiên hơn $R_2$ |
| $\mathcal{R}(t)$ | Tập luật khớp bản ghi $t$ |
| $\mathcal{G}_c$ | Nhóm luật cùng class $c$ |
| $\text{score}(\mathcal{G}_c)$ | Weighted χ² score |
| $\hat{c}$ | Class dự đoán |
| $\delta$ | Coverage threshold |
| $\text{minSup}, \text{minConf}$ | Ngưỡng support/confidence |
| $\alpha$ | Mức ý nghĩa thống kê |
| $df$ | Bậc tự do |
| $TP, FP, FN, TN$ | Confusion matrix entries |
| $P_c, R_c, F_1^{(c)}$ | Precision/Recall/F1 theo class |

### 17.3 Các công thức chính — Tổng hợp

**Support:**
$$
\text{sup}(P) = |\{t : P \subseteq t\}|, \quad \sigma(P) = \text{sup}(P)/|T|
$$

**Confidence:**
$$
\text{conf}(R: P \Rightarrow c) = \frac{\text{sup}(P \cup \{c\})}{\text{sup}(P)}
$$

**Lift:**
$$
\text{lift}(R) = \frac{\text{conf}(R)}{\sigma(c)} = \frac{\sigma(P \cup \{c\})}{\sigma(P)\sigma(c)}
$$

**Leverage:**
$$
\text{lev}(R) = \sigma(P \cup \{c\}) - \sigma(P)\sigma(c)
$$

**Conviction:**
$$
\text{conv}(R) = \frac{1 - \sigma(c)}{1 - \text{conf}(R)}
$$

**Chi-square (2×2 contingency):**
$$
\chi^2 = \frac{n(ad - b\gamma)^2}{(a+b)(\gamma+d)(a+\gamma)(b+d)}
$$

**Max chi-square:**
$$
a_{\max} = \min(\text{sup}(P), \text{sup}(c)), \quad \max\chi^2 = \chi^2 \text{ tại } a = a_{\max}
$$

**Weighted χ² score:**
$$
\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \frac{\chi^2(R)^2}{\max\chi^2(R)}
$$

**Class dự đoán:**
$$
\hat{c} = \arg\max_{c \in \mathcal{C}} \text{score}(\mathcal{G}_c)
$$

**Accuracy, Precision, Recall, F1:**
$$
\text{Accuracy} = \frac{\sum_c TP_c}{|T_{\text{test}}|}, \quad P_c = \frac{TP_c}{TP_c + FP_c}, \quad R_c = \frac{TP_c}{TP_c + FN_c}
$$
$$
F_1^{(c)} = \frac{2 P_c R_c}{P_c + R_c}, \quad F_1^{\text{macro}} = \frac{1}{|\mathcal{C}|}\sum_c F_1^{(c)}
$$

---

*Báo cáo chi tiết này trình bày đầy đủ thuật toán CMAR với trọng tâm phân tích sâu về luật kết hợp, các độ đo (support, confidence, lift, leverage, conviction, χ²), ba tầng cắt tỉa với ví dụ walk-through chi tiết, và luồng hoạt động end-to-end — phục vụ mục đích báo cáo học thuật chuẩn mực.*
