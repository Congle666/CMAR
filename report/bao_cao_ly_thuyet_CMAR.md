# BÁO CÁO LÝ THUYẾT THUẬT TOÁN CMAR

**Classification based on Multiple Association Rules**

> Dựa trên công trình: Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* **Proceedings of the 2001 IEEE International Conference on Data Mining (ICDM'01)**, pp. 369–376.

---

## Mục Lục

1. [Đặt Vấn Đề & Động Cơ Nghiên Cứu](#1-đặt-vấn-đề--động-cơ-nghiên-cứu)
2. [Kiến Thức Nền](#2-kiến-thức-nền)
3. [Tổng Quan Thuật Toán CMAR](#3-tổng-quan-thuật-toán-cmar)
4. [Giai Đoạn I — Sinh Luật Phân Lớp](#4-giai-đoạn-i--sinh-luật-phân-lớp)
5. [Giai Đoạn II — Cắt Tỉa Luật](#5-giai-đoạn-ii--cắt-tỉa-luật)
6. [Giai Đoạn III — Phân Lớp Bằng Weighted χ²](#6-giai-đoạn-iii--phân-lớp-bằng-weighted-χ²)
7. [Hệ Thống Công Thức Toán Học](#7-hệ-thống-công-thức-toán-học)
8. [Ví Dụ Minh Hoạ Đầy Đủ](#8-ví-dụ-minh-hoạ-đầy-đủ)
9. [Phân Tích Ưu – Nhược Điểm](#9-phân-tích-ưu--nhược-điểm)
10. [Bảng Tham Số & Hướng Dẫn Tinh Chỉnh](#10-bảng-tham-số--hướng-dẫn-tinh-chỉnh)
11. [Kết Luận](#11-kết-luận)
12. [Tài Liệu Tham Khảo](#12-tài-liệu-tham-khảo)

---

## 1. Đặt Vấn Đề & Động Cơ Nghiên Cứu

### 1.1 Bài toán phân lớp

Phân lớp (classification) là một trong những nhiệm vụ trọng tâm của **khai phá dữ liệu** và **học máy**. Cho trước một tập dữ liệu huấn luyện, trong đó mỗi bản ghi đã được gán một nhãn lớp, mục tiêu là xây dựng một **bộ phân lớp** (classifier) có khả năng dự đoán nhãn lớp cho các bản ghi mới chưa biết.

Một cách hình thức, cho trước:
- Lược đồ $(A_1, A_2, \dots, A_n)$ gồm $n$ thuộc tính
- Tập nhãn lớp hữu hạn $\mathcal{C} = \{c_1, c_2, \dots, c_m\}$
- Tập huấn luyện $T$ — tập các bản ghi có nhãn

Cần xây dựng hàm $\mathcal{F}: (A_1, A_2, \dots, A_n) \rightarrow \mathcal{C}$ sao cho khi áp dụng lên bản ghi mới, $\mathcal{F}$ cho dự đoán chính xác cao nhất.

### 1.2 Các hướng tiếp cận kinh điển

| Hướng | Đại diện | Đặc điểm |
|-------|----------|---------|
| Cây quyết định | C4.5, ID3, CART | Chia để trị, xét 1 thuộc tính mỗi lần |
| Luật quy nạp | CN2, RIPPER | Sinh luật heuristic |
| Xác suất | Naive Bayes | Giả định độc lập có điều kiện |
| Khoảng cách | k-NN | Không có mô hình tường minh |
| Mạng nơ-ron | MLP, Perceptron | Hộp đen, khó giải thích |
| **Luật kết hợp** | **CBA, CAEP, ADT, CMAR** | **Khai thác mối tương quan giữa nhiều thuộc tính** |

### 1.3 Điểm yếu của phương pháp phân lớp dựa luật kết hợp truyền thống

Hướng **phân lớp dựa luật kết hợp** (associative classification) — tiêu biểu là CBA (Liu, Hsu, Ma — 1998) — sinh toàn bộ luật kết hợp vượt ngưỡng support & confidence, rồi dùng các luật này để phân lớp. Ưu điểm lớn: phát hiện được mối tương quan phức hợp giữa nhiều thuộc tính (điều mà cây quyết định không làm được vì chỉ xét một thuộc tính tại một thời điểm).

Tuy nhiên, bài báo CMAR chỉ ra **hai hạn chế cơ bản**:

**Hạn chế 1 — Dự đoán dựa trên MỘT luật duy nhất**

CBA và các phương pháp cùng thời chọn **một luật có confidence cao nhất** khớp với bản ghi để dự đoán. Cách làm này dễ gặp vấn đề:

> *Ví dụ đã nêu trong bài báo:* xét khách hàng với đặc điểm `(no-job, investment-immigrant, oversea-asset > 500k)` cần xác định hạn mức tín dụng. Có 3 luật khớp:
>
> - **R₁:** `no-job → credit-limit < 3000` (sup = 3000, conf = 95%)
> - **R₂:** `investment-immigrant → credit-limit > 3000` (sup = 5000, conf = 93%)
> - **R₃:** `oversea-asset ≥ 500k → credit-limit > 3000` (sup = 8000, conf = 91%)
>
> Nếu chỉ theo luật có confidence cao nhất (R₁), hệ thống sẽ dự đoán hạn mức < 3000. Nhưng R₂ và R₃ có support cao hơn nhiều và đều cho dự đoán ngược lại. Trực quan cho thấy, dự đoán dựa trên R₂ và R₃ kết hợp sẽ **đáng tin cậy hơn**.

Kết luận: chọn 1 luật dễ **thiên lệch**, **overfitting**, nhất là khi có nhiều luật mâu thuẫn cùng khớp.

**Hạn chế 2 — Số luật bùng nổ tổ hợp**

Với ngưỡng support thấp, số luật kết hợp có thể lên tới hàng trăm nghìn hoặc hàng triệu, đặc biệt khi dataset có nhiều thuộc tính nhị phân. Việc **lưu trữ**, **truy vấn**, **cắt tỉa**, và **sắp xếp** số luật khổng lồ này đòi hỏi thời gian và bộ nhớ lớn.

### 1.4 Ba đóng góp chính của CMAR

CMAR giải quyết **đồng thời** cả hai vấn đề trên qua ba đóng góp:

| STT | Đóng góp | Giải quyết vấn đề |
|-----|----------|-------------------|
| 1 | **Phân lớp bằng NHIỀU luật** qua weighted χ² | Hạn chế 1 |
| 2 | **CR-tree** — cấu trúc prefix tree nén luật | Hạn chế 2 |
| 3 | **Biến thể FP-Growth** nhận diện class, sinh luật trực tiếp | Hiệu suất khai phá |

---

## 2. Kiến Thức Nền

Trước khi đi sâu vào CMAR, ta cần nắm các khái niệm nền tảng.

### 2.1 Transaction (bản ghi giao dịch)

Một bản ghi được biểu diễn dưới dạng tập các **cặp thuộc tính–giá trị** (item).

Ví dụ bản ghi thời tiết:

| Outlook | Temperature | Humidity | Windy | Play |
|---------|-------------|----------|-------|------|
| sunny | hot | high | false | no |

Được chuyển thành transaction:
$$
t = \{\text{outlook=sunny}, \text{temperature=hot}, \text{humidity=high}, \text{windy=false}\}, \quad \text{class}(t) = \text{no}
$$

### 2.2 Pattern (mẫu)

Pattern $P$ là một tập con các item. Ví dụ:
$$
P = \{\text{outlook=sunny}, \text{humidity=high}\}
$$

**Bản ghi $t$ khớp pattern $P$** khi và chỉ khi $P \subseteq \text{items}(t)$.

### 2.3 Class Association Rule (CAR — Luật kết hợp lớp)

Luật dạng $R: P \Rightarrow c$ nghĩa là: "nếu bản ghi khớp pattern $P$ thì dự đoán lớp $c$".

Ví dụ:
$$
R: \{\text{outlook=sunny}, \text{humidity=high}\} \Rightarrow \text{play=no}
$$

### 2.4 Support và Confidence

| Đại lượng | Công thức | Ý nghĩa |
|-----------|-----------|--------|
| Support tuyệt đối | $\text{sup}(R) = \|\{t \in T : P \subseteq t \wedge \text{class}(t) = c\}\|$ | Số bản ghi "ủng hộ" luật |
| Support tương đối | $\text{sup}(R) / \|T\|$ | Tỉ lệ bản ghi ủng hộ luật |
| Confidence | $\text{conf}(R) = \frac{\text{sup}(P \cup \{c\})}{\text{sup}(P)}$ | Xác suất có điều kiện $P(c \mid P)$ |

### 2.5 Luật hợp lệ

Luật $R$ được gọi là **hợp lệ** nếu:
$$
\text{sup}(R) \ge \text{minSup} \quad \text{VÀ} \quad \text{conf}(R) \ge \text{minConf}
$$

### 2.6 Rule precedence (thứ tự ưu tiên)

Với hai luật $R_1, R_2$, ta nói $R_1 \succ R_2$ (ưu tiên hơn) nếu:

$$
R_1 \succ R_2 \iff
\begin{cases}
\text{conf}(R_1) > \text{conf}(R_2), \text{ hoặc}\\
\text{conf}(R_1) = \text{conf}(R_2) \text{ và } \text{sup}(R_1) > \text{sup}(R_2), \text{ hoặc}\\
\text{bằng hai chỉ số trên nhưng } |P_1| < |P_2|
\end{cases}
$$

**Diễn giải:**
- **Confidence cao trước** — luật chắc chắn hơn được ưu tiên.
- **Support cao trước** — luật có nhiều bằng chứng hơn.
- **Condset ngắn trước** — luật đơn giản hơn (Occam's razor).

### 2.7 Luật tổng quát / chi tiết

- $R_1: P_1 \Rightarrow c_1$ **tổng quát hơn** $R_2: P_2 \Rightarrow c_2$ nếu $P_1 \subseteq P_2$.
- Khi đó $R_2$ là **luật chi tiết hơn**.

**Ví dụ:**
- $R_1: \{\text{milk=1}\} \Rightarrow \text{mammal}$ (tổng quát)
- $R_2: \{\text{milk=1}, \text{hair=1}\} \Rightarrow \text{mammal}$ (chi tiết hơn — thêm điều kiện `hair=1`)

Nếu $R_1$ đã có confidence = 100%, thì $R_2$ là **dư thừa**.

### 2.8 Chi-square (χ²) — Kiểm định độc lập

Cho bảng contingency 2×2:

|              | class = $c$ | class ≠ $c$ | tổng |
|--------------|:-----------:|:-----------:|:----:|
| Khớp $P$ | $a$ | $b$ | $a + b$ |
| Không khớp $P$ | $c$ | $d$ | $c + d$ |
| **Tổng** | $a + c$ | $b + d$ | $n$ |

Ký hiệu:
- $a$ = số bản ghi khớp $P$ và có lớp $c$
- $b$ = số bản ghi khớp $P$ nhưng không có lớp $c$
- $c$ = số bản ghi không khớp $P$ nhưng có lớp $c$
- $d$ = số bản ghi không khớp $P$ và không có lớp $c$
- $n = a + b + c + d$ = tổng số bản ghi

**Công thức χ² gọn cho bảng 2×2:**
$$
\chi^2 = \frac{n \cdot (ad - bc)^2}{(a+b)(c+d)(a+c)(b+d)}
$$

**Ý nghĩa:**
- $\chi^2$ càng lớn → $P$ và $c$ càng **không** độc lập → mối liên hệ càng mạnh.
- Ngưỡng thông dụng: $\chi^2 \ge 3.841$ ứng với $\alpha = 0.05$, bậc tự do $df = 1$ (ý nghĩa thống kê 95%).

**Điều kiện tương quan dương (positively correlated):** $ad > bc$ — khi đó việc xuất hiện $P$ **làm tăng** xác suất lớp $c$.

---

## 3. Tổng Quan Thuật Toán CMAR

### 3.1 Kiến trúc hai giai đoạn

CMAR gồm **hai giai đoạn** (phase) nối tiếp:

```
╔═══════════════════════════════════════════════════════════════════╗
║                    GIAI ĐOẠN I — RULE GENERATION                   ║
║   Mục tiêu: Từ tập huấn luyện T, sinh ra tập luật CAR chất lượng    ║
║                                                                    ║
║   (1) Mining: FP-Growth nhận diện class → sinh CAR trực tiếp       ║
║   (2) Pruning 3 tầng: general-rule / χ² / database-coverage         ║
║   (3) Lưu trữ: CR-tree nén luật                                     ║
╚═══════════════════════════════════════════════════════════════════╝
                                  ↓
╔═══════════════════════════════════════════════════════════════════╗
║                    GIAI ĐOẠN II — CLASSIFICATION                   ║
║   Mục tiêu: Với bản ghi mới, dự đoán class dựa trên tập luật         ║
║                                                                    ║
║   (1) Truy vấn CR-tree → lấy tập luật khớp                          ║
║   (2) Nếu rỗng → trả default class                                  ║
║   (3) Nếu đồng nhất class → trả class đó                            ║
║   (4) Nếu đa dạng → tính weighted χ² score → class có score cao nhất║
╚═══════════════════════════════════════════════════════════════════╝
```

### 3.2 Luồng dữ liệu chi tiết

```
                    [ Tập huấn luyện T ]
                            │
                    (1) Đếm tần số item
                            │
                    (2) Xây CR-tree khởi tạo
                        (có phân bố class tại mỗi node)
                            │
                    (3) FP-Growth đệ quy
                            │
                ├───→  [ Frequent Patterns ]
                │
                │       (4) Sinh CAR trực tiếp
                │       (qua ngưỡng sup, conf)
                │
                └───→  [ Tập CAR ứng viên ]
                            │
                    (5) Pruning tầng 1:
                        loại luật bị luật tổng quát lấn át
                            │
                    [ Tập luật sau P1 ]
                            │
                    (6) Pruning tầng 2:
                        loại luật có χ² thấp / tương quan âm
                            │
                    [ Tập luật sau P2 ]
                            │
                    (7) Pruning tầng 3:
                        database coverage (δ = 4)
                            │
                    [ Tập luật sau P3 ]
                            │
                    (8) Lưu vào CR-tree nén
                            │
                    [ CR-tree luật — mô hình cuối ]
                            │
                    ═══════════════
                            │
                    [ Bản ghi test t ]
                            │
                    (9) Truy vấn CR-tree → tập luật khớp
                            │
                    (10) Weighted χ² aggregation
                            │
                    [ Lớp dự đoán ĉ ]
```

### 3.3 Bảng các bước và input/output

| Bước | Tên bước | Input | Output | Công thức chính |
|:---:|---------|-------|--------|-----------------|
| 1 | Đếm tần số item | $T$ | `freq[item]`, `freq[class]` | — |
| 2 | Lọc item & sắp F-list | `freq` | F-list | $\text{freq} \ge \text{minSup}$ |
| 3 | Xây CR-tree khởi tạo | $T$, F-list | CR-tree | Chèn path theo F-list |
| 4 | FP-Growth đệ quy | CR-tree | Patterns + CARs | §4 |
| 5 | Pruning 1 | CAR ứng viên | Sau P1 | §5.1 |
| 6 | Pruning 2 (χ²) | Sau P1 | Sau P2 | §5.2 |
| 7 | Pruning 3 (coverage) | Sau P2 | Sau P3 | §5.3 |
| 8 | Xây CR-tree lưu luật | Sau P3 | CR-tree cuối | §4.1 |
| 9 | Truy vấn luật | Bản ghi $t$ | Tập luật khớp | DFS trên CR-tree |
| 10 | Weighted χ² | Tập luật khớp | Class dự đoán | §6 |

---

## 4. Giai Đoạn I — Sinh Luật Phân Lớp

### 4.1 CR-tree — Mở rộng FP-tree với phân bố lớp

**FP-tree** là cấu trúc nổi tiếng do Han, Pei, Yin (2000) đề xuất — một prefix tree lưu trữ toàn bộ transactions dưới dạng nén. CMAR mở rộng FP-tree bằng cách **gắn vào mỗi node phân bố số lượng bản ghi theo từng class đi qua node đó**.

**Cấu trúc node CR-tree** (khái niệm):

| Trường | Ý nghĩa |
|--------|--------|
| `item` | Tên item tại node (ví dụ `outlook=sunny`) |
| `count` | Tổng số transaction đi qua node này |
| `parent` | Node cha |
| `children` | Các node con |
| `nodeLink` | Liên kết đến node tiếp theo cùng item (dùng cho header table) |
| **`classCount`** | **Dictionary {class → số lượng} — phân bố lớp tại node** |

> Chính trường `classCount` này là "mở rộng cốt lõi" mà bài báo nhấn mạnh (Section 3.2).

**Tại sao cần `classCount` ở mỗi node?**

Khi đào mỏ pattern $P$, nhờ `classCount` ta biết **ngay lập tức** có bao nhiêu bản ghi thuộc từng class khớp pattern $P$. Từ đó sinh luật $P \Rightarrow c$ trực tiếp mà **không cần quét lại dữ liệu**. Điều này tiết kiệm rất nhiều thời gian so với thuật toán 2-bước truyền thống (mine pattern → rồi tạo luật).

### 4.2 Xây CR-tree khởi tạo

**Thuật toán xây cây khởi tạo:**

```
INPUT : Tập huấn luyện T, ngưỡng minSup
OUTPUT: CR-tree với phân bố lớp

Bước 1. Duyệt T một lần, đếm tần số mỗi item (không tính class)
Bước 2. Loại item có freq < minSup
Bước 3. Sắp item còn lại theo tần số GIẢM DẦN → F-list
Bước 4. Khởi tạo CR-tree chỉ có root
Bước 5. Với mỗi transaction t ∈ T:
          path ← items(t) ∩ F-list, sắp theo F-list
          InsertPath(root, path, {class(t): 1}, count=1)
```

**Thủ tục `InsertPath`:**

```
Với mỗi item trong path:
    Nếu node hiện tại có con với item đó:
        Đi xuống con đó
    Ngược lại:
        Tạo node con mới với item đó
        Thêm vào header-table chain của item
    Tại node vừa tới:
        count += 1
        classCount[class(t)] += 1
```

**Lý do sắp F-list giảm dần:** các item phổ biến nằm gần gốc → nhiều transaction chia sẻ chung tiền tố → cây nén tối ưu.

### 4.3 FP-Growth đệ quy với nhận diện class

**Nguyên lý:** Duyệt header-table theo thứ tự item **tần số TĂNG DẦN** (ngược với F-list). Với mỗi item $x$:

1. Sinh pattern mới $P = \text{prefix} \cup \{x\}$.
2. Thu phân bố lớp của $P$ bằng cách cộng dồn `classCount` qua header chain của $x$:
$$
\text{classDist}(P)[c] = \sum_{\text{node } n \in \text{chain}(x)} n.\text{classCount}[c]
$$
3. **Sinh CAR trực tiếp** — xem §4.4.
4. Xây **conditional pattern base** — tập các prefix-path lấy từ mọi node của $x$ đi ngược lên gốc.
5. Xây **conditional CR-tree** từ conditional pattern base (giữ nguyên phân bố class).
6. **Đệ quy** với prefix $= P$ trên conditional CR-tree.

**Quy trình vẽ ra sơ đồ:**

```
mineTree(tree, prefix):
    for mỗi item x trong tree theo tần số TĂNG DẦN:
        P = prefix ∪ {x}
        
        // Thu phân bố class
        classDist = aggregate(chain của x)
        
        // Phát hiện pattern
        add P vào tập frequent patterns
        
        // Sinh CAR
        for mỗi (cls, sup) trong classDist:
            if sup ≥ minSup và sup/itemSup ≥ minConf:
                emit rule (P ⇒ cls) với độ đo (sup, conf)
        
        // Xây conditional CR-tree
        condBase = collect paths(chain của x)
        condTree = buildTree(condBase, minSup)
        
        // Đệ quy
        mineTree(condTree, prefix = P)
```

### 4.4 Sinh CAR trực tiếp trong lúc đào mỏ

Với frequent pattern $P$ có support tuyệt đối $s_P$ và phân bố lớp `classDist[c]`, sinh luật $P \Rightarrow c$ khi:

$$
\text{classDist}[c] \ge \text{minSup} \quad \text{VÀ} \quad \frac{\text{classDist}[c]}{s_P} \ge \text{minConf}
$$

**Đây là điểm khác biệt cốt lõi so với FP-Growth gốc và CBA:**

| Khía cạnh | FP-Growth gốc + CBA | CMAR |
|-----------|--------------------|------|
| Pass đầu | Mine frequent pattern | Mine frequent pattern **+ sinh CAR** |
| Pass thứ hai | Duyệt lại patterns, tạo luật | **Không cần** |
| Dữ liệu lớp | Không lưu tại node | Lưu tại `classCount` mỗi node |
| Hiệu suất | 2 pass | 1 pass |

### 4.5 Thuật toán "project & recurse"

Khi đã xử lý xong item $x$, ta cần đào tiếp các pattern chứa $x$ + các item khác. Cách làm:

1. Thu các **path đi từ root đến các node $x$** (không gồm $x$) — gọi là **conditional pattern base của $x$**.
2. Mỗi path đi kèm `count` của node $x$ (số transaction đi qua) và `classCount` của node $x$.
3. Xây **conditional CR-tree** từ các path này (lọc lại theo minSup).
4. Gọi đệ quy `mineTree(condTree, prefix = {x})`.

**Lưu ý:** `classCount` của node $x$ (không phải của node trong path) là thứ được "propagate" xuống conditional tree — đây mới là phân bố lớp của transactions đóng góp vào path đó.

---

## 5. Giai Đoạn II — Cắt Tỉa Luật

Sau bước sinh CAR, số luật ứng viên có thể lên đến hàng trăm nghìn. CMAR áp dụng **3 tầng cắt tỉa** theo thứ tự để lọc ra tập luật chất lượng cao.

### 5.1 Pruning 1 — General Rule Pruning

**Nguyên tắc:** Nếu $R_1$ **tổng quát hơn** $R_2$, **cùng class**, và **$R_1$ có ưu tiên cao hơn $R_2$** thì **loại $R_2$**.

**Cơ sở lý thuyết:** Luật đơn giản với confidence cao đã "bao" luật chi tiết — thêm điều kiện chỉ làm phức tạp hơn mà không cải thiện.

**Ví dụ:**
- $R_1: \{\text{milk=1}\} \Rightarrow \text{mammal}$, sup = 40%, conf = 100%
- $R_2: \{\text{milk=1}, \text{hair=1}\} \Rightarrow \text{mammal}$, sup = 35%, conf = 100%

$R_1 \succ R_2$ (cùng conf, sup cao hơn, condset nhỏ hơn) + $\{\text{milk=1}\} \subseteq \{\text{milk=1}, \text{hair=1}\}$ → **loại $R_2$**.

**Quy trình:**

```
Sắp toàn bộ luật theo precedence (conf↓ → sup↓ → |P|↑)
kept = []
Với mỗi R trong danh sách đã sắp:
    dominated = false
    Với mỗi R' ∈ kept có cùng class với R:
        Nếu P(R') ⊆ P(R):
            dominated = true; break
    Nếu không bị dominated:
        kept.append(R)
return kept
```

Kết quả: tập luật không dư thừa theo quan hệ tổng quát/chi tiết.

### 5.2 Pruning 2 — Chi-square Significance + Positive Correlation

**Nguyên tắc:** Chỉ giữ luật có:
1. **Ý nghĩa thống kê:** $\chi^2 \ge \chi^2_{\text{threshold}}$ (mặc định 3.841)
2. **Tương quan dương:** $ad > bc$ (observed > expected)

**Cơ sở lý thuyết:** Luật kết hợp chỉ có nghĩa khi $P$ và $c$ **tương quan mạnh** và theo chiều **dương**. Luật $\chi^2$ thấp → có thể là ngẫu nhiên; tương quan âm → pattern ngăn cản class → **luật phản tác dụng**.

**Công thức χ² đã cho ở §2.8.**

**Ý nghĩa ngưỡng thường dùng:**

| Ngưỡng χ² | Mức ý nghĩa $\alpha$ | df |
|----------:|:--------------------:|:--:|
| 2.706 | 0.10 | 1 |
| **3.841** | **0.05** | **1** |
| 6.635 | 0.01 | 1 |
| 10.827 | 0.001 | 1 |

Trong CMAR thường dùng 3.841 (tương đương mức tin cậy 95%).

### 5.3 Pruning 3 — Database Coverage (Thuật toán 1 bài báo)

Đây là phần độc đáo nhất của CMAR, khác biệt rõ so với CBA.

**Vấn đề CBA:** CBA loại bản ghi huấn luyện ngay khi bất kỳ luật nào "cover" nó. Cách làm này dẫn đến việc **giữ quá ít luật** — khi phân lớp bản ghi mới, rất ít luật khớp, dễ phải dùng default class.

**Giải pháp CMAR:** Cho phép mỗi bản ghi huấn luyện được cover tối đa $\delta$ lần (mặc định $\delta = 4$) trước khi loại. Nhờ vậy, nhiều luật "dự phòng" được giữ lại — khi phân lớp, có nhiều luật để "hội ý".

**Thuật toán 1 (Database Coverage):**

```
INPUT : Tập luật R (đã sắp theo precedence), ngưỡng δ
OUTPUT: Tập luật con S dùng cho phân lớp

1. Khởi tạo cover_count[obj] = 0 cho mọi obj ∈ T
2. selected = []
3. Lặp qua từng luật R theo precedence (confidence cao trước):
     coversAny = false
     Với mỗi bản ghi obj còn trong T:
         Nếu obj khớp P(R) và class(R) = class(obj):
             coversAny = true
             cover_count[obj] += 1
             Nếu cover_count[obj] ≥ δ:
                 Loại obj khỏi T
     Nếu coversAny = true:
         selected.append(R)
4. Return selected
```

**Ý nghĩa các tham số:**

| $\delta$ | Số luật giữ lại | Chất lượng phân lớp |
|:-------:|:---------------:|:-------------------:|
| 1 | Ít | Dễ thiếu luật khi classify (giống CBA) |
| **4** | **Vừa phải** | **Tốt cho đa số dataset** |
| 10+ | Nhiều | Có thể giữ cả luật nhiễu |

Bài báo đã thực nghiệm và cho thấy $\delta = 4$ tối ưu trên hầu hết dataset UCI.

### 5.4 Bảng tổng hợp hiệu quả 3 tầng pruning

Trên một dataset điển hình, hiệu quả giảm số luật:

| Bước | Số luật còn lại | % Loại bỏ |
|------|:---------------:|:---------:|
| Ứng viên ban đầu | 100,000 | — |
| Sau Pruning 1 (general) | ~10,000 | 90% |
| Sau Pruning 2 (χ²) | ~3,000 | 70% |
| Sau Pruning 3 (coverage) | ~500 | 83% |
| **Tổng giảm** | **500** | **99.5%** |

---

## 6. Giai Đoạn III — Phân Lớp Bằng Weighted χ²

### 6.1 Quy trình 4 bước

Cho bản ghi test $t$, thực hiện:

**Bước 1 — Truy vấn CR-tree:** Lấy tập $\mathcal{R}(t) = \{R \mid \text{condset}(R) \subseteq \text{items}(t)\}$ — tất cả luật mà $t$ "thoả mãn điều kiện".

**Bước 2 — Không có luật khớp:** Trả về **default class** (class phổ biến nhất trong tập huấn luyện).

**Bước 3 — Đồng nhất class:** Nếu mọi luật trong $\mathcal{R}(t)$ có cùng một class $c$ → trả về $c$.

**Bước 4 — Đa dạng class:** Tính **weighted χ²** cho từng class, chọn class có score cao nhất.

### 6.2 Công thức Weighted χ²

Giả sử $\mathcal{R}(t)$ được chia thành các nhóm $\mathcal{G}_{c_1}, \mathcal{G}_{c_2}, \dots$ theo class. Với mỗi nhóm $\mathcal{G}_c$:

$$
\boxed{\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \frac{\chi^2(R)^2}{\max\chi^2(R)}}
$$

Trong đó:
- $\chi^2(R)$ — chi-square của luật $R$ tính trên training set
- $\max\chi^2(R)$ — **cận trên lý thuyết** của χ² (xem §7.3)

Dự đoán:
$$
\hat{c}(t) = \arg\max_{c \in \mathcal{C}} \text{score}(\mathcal{G}_c)
$$

### 6.3 Tại sao phải chia cho maxχ²?

**Vấn đề:** Nếu chỉ cộng $\chi^2(R)$, class **minority** (ít mẫu) dễ có luật có χ² lớn bất thường — vì trong bảng contingency, kích thước class ảnh hưởng trực tiếp đến χ² tối đa có thể đạt được.

**Giải pháp:** Chia cho $\max\chi^2(R)$ — chuẩn hoá χ² về **tỉ lệ trong [0, 1]**:
$$
0 \le \frac{\chi^2(R)}{\max\chi^2(R)} \le 1
$$
Mọi luật được đánh giá trên **cùng thang đo**, không thiên vị class lớn/nhỏ.

**Bình phương $\chi^2$ ở tử số:** Tăng "tiếng nói" của luật mạnh, làm chênh lệch giữa luật tốt và luật yếu rõ hơn.

### 6.4 Ví dụ minh hoạ Weighted χ² (Example 3 của bài báo)

Xét bài toán phê duyệt thẻ tín dụng với 2 luật:

**Luật R₁:** `job = no ⇒ rejected` (sup = 30, conf = 60%)

Bảng contingency thực tế (n = 500):

|            | approved | rejected | tổng |
|------------|:--------:|:--------:|:----:|
| job = yes | 438 | 32 | 470 |
| job = no | 12 | 18 | 30 |
| **tổng** | 450 | 50 | 500 |

$\chi^2(R_1) = 88.4$

**Luật R₂:** `education = university ⇒ approved` (sup = 200, conf = 99.5%)

|            | approved | rejected | tổng |
|------------|:--------:|:--------:|:----:|
| ed = univ | 199 | 1 | 200 |
| ed ≠ univ | 251 | 49 | 300 |
| **tổng** | 450 | 50 | 500 |

$\chi^2(R_2) = 33.6$

**Vấn đề:** $\chi^2(R_1) > \chi^2(R_2)$ → nếu chỉ so χ² đơn thuần, $R_1$ "mạnh hơn". Nhưng $R_2$ có sup = 200 và conf = 99.5% — **rõ ràng đáng tin cậy hơn $R_1$ (sup = 30, conf = 60%)**.

**Kết luận bài báo:** Chỉ dùng χ² đơn lẻ → thiên vị minority class (`job=no` chỉ có 30/500 bản ghi). Weighted χ² với normalization $\chi^2/\max\chi^2$ khắc phục vấn đề này.

### 6.5 Sơ đồ quyết định phân lớp

```
            ┌─────────────────────┐
            │   Bản ghi test t    │
            └──────────┬──────────┘
                       │
               Truy vấn CR-tree
                       │
                       ▼
            ┌─────────────────────┐
            │  R(t) = luật khớp   │
            └──────────┬──────────┘
                       │
                       ▼
                   ┌───┴───┐
                   │ Rỗng? │───Yes──→ Default class
                   └───┬───┘
                       No
                       ▼
                   ┌───┴────────────┐
                   │ Đồng nhất class?│───Yes──→ Class đó
                   └───┬────────────┘
                       No
                       ▼
             Nhóm R(t) theo class
                       │
                       ▼
          Tính score(G_c) = Σ χ²²/maxχ²
                       │
                       ▼
                argmax_c score(G_c)
                       │
                       ▼
                 Class dự đoán
```

---

## 7. Hệ Thống Công Thức Toán Học

Phần này tập trung tổng hợp **toàn bộ công thức** dùng trong CMAR.

### 7.1 Support và Confidence

**Support tuyệt đối:**
$$
\text{sup}(P \Rightarrow c) = \Big|\{ t \in T : P \subseteq \text{items}(t) \wedge \text{class}(t) = c \}\Big|
$$

**Support tương đối:**
$$
\sigma(P \Rightarrow c) = \frac{\text{sup}(P \Rightarrow c)}{|T|}
$$

**Confidence:**
$$
\text{conf}(P \Rightarrow c) = \frac{\text{sup}(P \cup \{c\})}{\text{sup}(P)}
$$

### 7.2 Chi-square (χ²)

Cho bảng 2×2 với $a, b, c, d, n$:

**Giá trị quan sát (observed):**
$$
O_{11} = a, \quad O_{12} = b, \quad O_{21} = c, \quad O_{22} = d
$$

**Giá trị kỳ vọng (expected):**
$$
E_{ij} = \frac{(\text{tổng hàng }i)(\text{tổng cột }j)}{n}
$$

Cụ thể:
$$
E_{11} = \frac{(a+b)(a+c)}{n}, \quad E_{12} = \frac{(a+b)(b+d)}{n}
$$
$$
E_{21} = \frac{(c+d)(a+c)}{n}, \quad E_{22} = \frac{(c+d)(b+d)}{n}
$$

**Công thức χ² tổng quát:**
$$
\chi^2 = \sum_{i,j} \frac{(O_{ij} - E_{ij})^2}{E_{ij}}
$$

**Công thức gọn cho bảng 2×2:**
$$
\chi^2 = \frac{n(ad - bc)^2}{(a+b)(c+d)(a+c)(b+d)}
$$

**Tương quan dương:** $ad > bc \iff a > E_{11}$

### 7.3 Max χ² — Cận trên lý thuyết

**Ý tưởng:** Giả định "lý tưởng" — tất cả bản ghi khớp $P$ đều có lớp $c$ — để tính $\chi^2$ cực đại có thể đạt được với các giá trị $\text{sup}(P)$ và $\text{sup}(c)$ cho trước.

Khi đó, $a$ đạt giá trị lớn nhất:
$$
a_{\max} = \min\big(\text{sup}(P), \text{sup}(c)\big)
$$

Các giá trị còn lại:
$$
b = \text{sup}(P) - a_{\max}
$$
$$
c_{\text{cell}} = \text{sup}(c) - a_{\max}
$$
$$
d = n - a_{\max} - b - c_{\text{cell}}
$$

Áp dụng công thức χ² chuẩn:
$$
\max\chi^2 = \frac{n(a_{\max} \cdot d - b \cdot c_{\text{cell}})^2}{(a_{\max}+b)(c_{\text{cell}}+d)(a_{\max}+c_{\text{cell}})(b+d)}
$$

### 7.4 Weighted χ² Score

Với nhóm luật $\mathcal{G}_c$ cùng class $c$:

$$
\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \frac{\chi^2(R)^2}{\max\chi^2(R)}
$$

Class được chọn:
$$
\hat{c} = \arg\max_{c \in \mathcal{C}} \text{score}(\mathcal{G}_c)
$$

### 7.5 Các độ đo đánh giá

**Confusion matrix:** Cho mỗi class $c$:
- $TP_c$ — đúng class $c$ và dự đoán $c$
- $FP_c$ — không phải class $c$ nhưng dự đoán $c$
- $FN_c$ — class $c$ nhưng dự đoán khác
- $TN_c$ — không phải $c$ và dự đoán không phải $c$

**Precision — Độ chính xác:**
$$
P_c = \frac{TP_c}{TP_c + FP_c}
$$

**Recall — Độ bao phủ:**
$$
R_c = \frac{TP_c}{TP_c + FN_c}
$$

**F1-score:**
$$
F_1^{(c)} = \frac{2 \cdot P_c \cdot R_c}{P_c + R_c}
$$

**Accuracy tổng:**
$$
\text{Accuracy} = \frac{\sum_c TP_c}{|T_{\text{test}}|}
$$

**Macro-F1:**
$$
F_1^{\text{macro}} = \frac{1}{|\mathcal{C}|} \sum_{c \in \mathcal{C}} F_1^{(c)}
$$

---

## 8. Ví Dụ Minh Hoạ Đầy Đủ

Phần này trình bày **ba ví dụ** chi tiết, giúp hiểu rõ cơ chế CMAR.

### Ví Dụ 1 — Dataset thời tiết

**Tập huấn luyện (14 bản ghi, 4 thuộc tính, 2 class):**

| ID | Outlook | Temperature | Humidity | Windy | Play |
|:--:|---------|-------------|----------|-------|------|
| 1 | sunny | hot | high | false | no |
| 2 | sunny | hot | high | true | no |
| 3 | overcast | hot | high | false | yes |
| 4 | rain | mild | high | false | yes |
| 5 | rain | cool | normal | false | yes |
| 6 | rain | cool | normal | true | no |
| 7 | overcast | cool | normal | true | yes |
| 8 | sunny | mild | high | false | no |
| 9 | sunny | cool | normal | false | yes |
| 10 | rain | mild | normal | false | yes |
| 11 | sunny | mild | normal | true | yes |
| 12 | overcast | mild | high | true | yes |
| 13 | overcast | hot | normal | false | yes |
| 14 | rain | mild | high | true | no |

**Tham số:** minSup = 2, minConf = 0.5.

#### Bước 1 — Đếm tần số item

| Item | Tần số |
|------|:-----:|
| outlook=sunny | 5 |
| outlook=overcast | 4 |
| outlook=rain | 5 |
| temperature=hot | 4 |
| temperature=mild | 6 |
| temperature=cool | 4 |
| humidity=high | 7 |
| humidity=normal | 7 |
| windy=false | 8 |
| windy=true | 6 |

Phân bố class:
| play=yes | play=no |
|:--------:|:-------:|
| 9 | 5 |

**F-list (sắp giảm dần):**
`windy=false(8) → humidity=high(7) → humidity=normal(7) → temperature=mild(6) → outlook=sunny(5) → outlook=rain(5) → outlook=overcast(4) → temperature=hot(4) → temperature=cool(4) → windy=true(6)`

#### Bước 2 — Một số luật được sinh (minh hoạ)

| Luật | sup | conf | Ghi chú |
|------|:---:|:----:|---------|
| `{outlook=overcast} ⇒ play=yes` | 4/14 | 4/4 = 100% | Luật mạnh: cứ mây mù là chơi được |
| `{humidity=normal} ⇒ play=yes` | 6/14 | 6/7 ≈ 85.7% | Ẩm độ bình thường → chơi được |
| `{humidity=high, outlook=sunny} ⇒ play=no` | 3/14 | 3/3 = 100% | Nắng + ẩm cao → không chơi |
| `{windy=true, outlook=rain} ⇒ play=no` | 2/14 | 2/2 = 100% | Mưa + gió → không chơi |
| `{humidity=normal, windy=false} ⇒ play=yes` | 4/14 | 4/4 = 100% | Ẩm thường + không gió → chơi |
| `{outlook=sunny} ⇒ play=no` | 3/14 | 3/5 = 60% | Nắng (chung) → không chơi, nhưng chỉ 60% |
| `{outlook=rain} ⇒ play=yes` | 3/14 | 3/5 = 60% | Mưa (chung) → chơi, chỉ 60% |

Tổng số luật ứng viên (sau minSup, minConf): giả sử 25 luật.

#### Bước 3 — Pruning 1 (General Rule)

Xét `{outlook=sunny} ⇒ play=no` (sup=3, conf=60%) vs `{outlook=sunny, humidity=high} ⇒ play=no` (sup=3, conf=100%):
- Luật 1 tổng quát hơn nhưng **conf thấp hơn** → **không** lấn át luật 2.
- Luật 2 chi tiết hơn nhưng conf = 100% → giữ lại.

Xét `{humidity=normal} ⇒ play=yes` (sup=6, conf=85.7%) vs `{humidity=normal, windy=false} ⇒ play=yes` (sup=4, conf=100%):
- Luật 2 có conf cao hơn, nhưng luật 1 có sup cao hơn → precedence khác nhau, không rõ ràng lấn át.
- Theo quy tắc bài báo: nếu luật tổng quát có precedence cao hơn luật chi tiết → loại luật chi tiết. Ở đây luật 2 có conf cao hơn → giữ cả hai.

Giả sử sau Pruning 1: còn 15 luật.

#### Bước 4 — Pruning 2 (χ²)

Tính χ² cho luật `{outlook=overcast} ⇒ play=yes`:

| | play=yes | play=no | tổng |
|-|:--------:|:-------:|:----:|
| overcast | 4 | 0 | 4 |
| ¬overcast | 5 | 5 | 10 |
| **tổng** | 9 | 5 | 14 |

$a=4, b=0, c=5, d=5, n=14$

$$
\chi^2 = \frac{14 \cdot (4 \cdot 5 - 0 \cdot 5)^2}{4 \cdot 10 \cdot 9 \cdot 5} = \frac{14 \cdot 400}{1800} \approx 3.11
$$

Giá trị 3.11 < 3.841 → **loại** luật này (không đủ ý nghĩa thống kê)! Đây là hệ quả của dataset nhỏ.

Tính χ² cho `{humidity=high, outlook=sunny} ⇒ play=no`:

| | play=no | play=yes | tổng |
|-|:-------:|:--------:|:----:|
| (high, sunny) | 3 | 0 | 3 |
| ¬(high, sunny) | 2 | 9 | 11 |
| **tổng** | 5 | 9 | 14 |

$a=3, b=0, c=2, d=9, n=14$

$$
\chi^2 = \frac{14 \cdot (3 \cdot 9 - 0 \cdot 2)^2}{3 \cdot 11 \cdot 5 \cdot 9} = \frac{14 \cdot 729}{1485} \approx 6.87
$$

6.87 ≥ 3.841 → **giữ** luật. Tương quan dương: $ad = 27 > bc = 0$ ✓.

Giả sử sau Pruning 2: còn 8 luật.

#### Bước 5 — Pruning 3 (Coverage, δ=2)

Duyệt luật theo precedence. Với mỗi bản ghi training được cover đủ 2 lần → loại.

Giả sử sau Pruning 3: còn 5 luật cuối cùng.

#### Bước 6 — Phân lớp bản ghi test

**Bản ghi test:** `(outlook=sunny, temperature=cool, humidity=high, windy=true)`

Luật khớp (giả sử):
- R_A: `{outlook=sunny, humidity=high} ⇒ play=no` (χ² = 6.87)
- R_B: `{windy=true, humidity=high} ⇒ play=no` (χ² = 2.5, nhưng giả định đã qua pruning)
- R_C: `{humidity=high} ⇒ play=no` (χ² = 4.2)

Tất cả cùng dự đoán `play=no` → **trả về `play=no`** (Bước 3).

### Ví Dụ 2 — Tính toán Weighted χ² chi tiết

**Giả sử** một bản ghi test khớp **4 luật**:

| Luật | Class | $\chi^2$ | $\max\chi^2$ |
|------|-------|:--------:|:------------:|
| R₁ | A | 10 | 25 |
| R₂ | A | 8 | 20 |
| R₃ | B | 15 | 18 |
| R₄ | B | 6 | 12 |

**Tính score cho class A:**
$$
\text{score}(A) = \frac{10^2}{25} + \frac{8^2}{20} = \frac{100}{25} + \frac{64}{20} = 4.0 + 3.2 = 7.2
$$

**Tính score cho class B:**
$$
\text{score}(B) = \frac{15^2}{18} + \frac{6^2}{12} = \frac{225}{18} + \frac{36}{12} \approx 12.5 + 3.0 = 15.5
$$

**So sánh:** $\text{score}(B) = 15.5 > \text{score}(A) = 7.2$

→ **Dự đoán class B**

**Nhận xét quan trọng:**
- Class A có **2 luật** khớp, class B cũng **2 luật** khớp.
- Nếu đếm phiếu (voting đơn thuần) → hoà.
- Nhưng score cho thấy class B có luật R₃ **rất mạnh** (χ² = 15, gần chạm max = 18 → tỉ số 0.83).
- CMAR **không đếm phiếu**, mà đo **sức mạnh tổng hợp** → dự đoán B chính xác hơn.

### Ví Dụ 3 — Credit Card Approval (Lấy từ bài báo)

Bài toán: phê duyệt thẻ tín dụng dựa trên 2 thuộc tính: `job` và `education`.

**Dữ liệu** (n = 500):

|  | approved | rejected | tổng |
|--|:--------:|:--------:|:----:|
| job=yes | 438 | 32 | 470 |
| job=no | 12 | 18 | 30 |
| ed=univ | 199 | 1 | 200 |
| ed≠univ | 251 | 49 | 300 |

**Hai luật được sinh:**

| Luật | sup | conf | χ² |
|------|:---:|:----:|:---:|
| R₁: `job=no ⇒ rejected` | 18 | 60% | 88.4 |
| R₂: `ed=univ ⇒ approved` | 199 | 99.5% | 33.6 |

**Tính max χ² cho từng luật:**

**R₁ (`job=no` vs `rejected`):**
- sup(P) = 30, sup(c) = 50
- $a_{\max} = \min(30, 50) = 30$
- $b = 30 - 30 = 0$
- $c_{\text{cell}} = 50 - 30 = 20$
- $d = 500 - 30 - 0 - 20 = 450$
$$
\max\chi^2(R_1) = \frac{500 \cdot (30 \cdot 450 - 0 \cdot 20)^2}{30 \cdot 470 \cdot 50 \cdot 450} = \frac{500 \cdot 182{,}250{,}000}{317{,}250{,}000} \approx 287.2
$$

Tỉ số: $\chi^2(R_1) / \max\chi^2(R_1) = 88.4 / 287.2 \approx 0.308$

**R₂ (`ed=univ` vs `approved`):**
- sup(P) = 200, sup(c) = 450
- $a_{\max} = \min(200, 450) = 200$
- $b = 200 - 200 = 0$
- $c_{\text{cell}} = 450 - 200 = 250$
- $d = 500 - 200 - 0 - 250 = 50$
$$
\max\chi^2(R_2) = \frac{500 \cdot (200 \cdot 50 - 0 \cdot 250)^2}{200 \cdot 300 \cdot 450 \cdot 50} = \frac{500 \cdot 100{,}000{,}000}{1{,}350{,}000{,}000} \approx 37.0
$$

Tỉ số: $\chi^2(R_2) / \max\chi^2(R_2) = 33.6 / 37.0 \approx 0.908$

**So sánh hai luật sau normalization:**

| Luật | $\chi^2$ | $\max\chi^2$ | $\chi^2/\max\chi^2$ |
|------|:-------:|:------------:|:--------------------:|
| R₁: `job=no ⇒ rejected` | 88.4 | 287.2 | **0.308** |
| R₂: `ed=univ ⇒ approved` | 33.6 | 37.0 | **0.908** |

**Kết luận quan trọng:**
- $\chi^2$ thô: R₁ mạnh hơn R₂ (88.4 > 33.6)
- $\chi^2/\max\chi^2$: R₂ mạnh hơn R₁ rất nhiều (0.908 > 0.308)

**Phép normalization cho thấy R₂ là luật "hoàn hảo hơn"** — nó đã đạt tới ~91% mức χ² tối đa lý thuyết, trong khi R₁ chỉ đạt ~31%.

Đây chính là lý do CMAR chia cho $\max\chi^2$ — để không bị "lừa" bởi giá trị χ² thô cao do class minority.

### Ví Dụ 4 — Cây FP-tree đầy đủ

**Dataset 4 bản ghi:**

| ID | A | B | C | D | Class |
|:--:|---|---|---|---|:-----:|
| 1 | a₁ | b₁ | c₁ | d₁ | A |
| 2 | a₁ | b₂ | c₁ | d₂ | B |
| 3 | a₁ | b₂ | c₁ | d₃ | C |
| 4 | a₁ | b₂ | — | d₃ | A |

**Tham số:** minSup = 2.

**Bước 1 — Đếm tần số:**

| Item | Freq |
|------|:---:|
| a₁ | 4 |
| b₂ | 3 |
| c₁ | 3 |
| b₁ | 1 (loại) |
| d₁ | 1 (loại) |
| d₂ | 1 (loại) |
| d₃ | 2 |

Items còn lại sau lọc: `a₁, b₂, c₁, d₃`

**F-list:** a₁(4) → b₂(3) → c₁(3) → d₃(2)

**Bước 2 — Chuyển transaction:**

| ID | Path (theo F-list) | Class |
|:--:|--------------------|:-----:|
| 1 | [a₁, c₁] | A |
| 2 | [a₁, b₂, c₁] | B |
| 3 | [a₁, b₂, c₁, d₃] | C |
| 4 | [a₁, b₂, d₃] | A |

**Bước 3 — FP-tree sau khi chèn tất cả:**

```
root
 └── a₁ : 4  {A:2, B:1, C:1}
      ├── c₁ : 1  {A:1}          (từ bản ghi 1)
      └── b₂ : 3  {A:1, B:1, C:1}
           ├── c₁ : 2  {B:1, C:1}    (bản ghi 2, 3)
           │    └── d₃ : 1  {C:1}     (bản ghi 3)
           └── d₃ : 1  {A:1}          (bản ghi 4)
```

**Header table:**

| Item | Freq | First node |
|------|:---:|------------|
| a₁ | 4 | → node a₁ |
| b₂ | 3 | → node b₂ |
| c₁ | 3 | → node c₁ (dưới a₁) → node c₁ (dưới b₂) |
| d₃ | 2 | → node d₃ (dưới c₁) → node d₃ (dưới b₂) |

**Bước 4 — Đào mỏ d₃ (item hiếm nhất):**

Phân bố class của d₃ = {A:1 (từ node dưới b₂), C:1 (từ node dưới c₁)}

- Class A: sup = 1 < minSup = 2 → bỏ
- Class C: sup = 1 < minSup = 2 → bỏ

**Conditional pattern base của d₃:**
- Path [a₁, b₂, c₁] với count=1, classCount={C:1}
- Path [a₁, b₂] với count=1, classCount={A:1}

Tần số trong conditional base: a₁:2, b₂:2, c₁:1

Lọc minSup=2 → giữ `a₁, b₂`.

**Conditional FP-tree cho d₃:**

```
root
 └── a₁ : 2  {A:1, C:1}
      └── b₂ : 2  {A:1, C:1}
```

Đào tiếp trong condtree này:

- Pattern {a₁, b₂, d₃}: sup = 2, classDist = {A:1, C:1}
  - A: 1 < 2 → bỏ
  - C: 1 < 2 → bỏ
  - Không sinh luật.

- Pattern {a₁, d₃}: sup = 2, classDist = {A:1, C:1} → tương tự.
- Pattern {b₂, d₃}: sup = 2, classDist = {A:1, C:1} → tương tự.

**Bước 5 — Đào mỏ c₁:**

Phân bố class của c₁ = {A:1, B:1, C:1}

- Mỗi class đều < minSup = 2 → không sinh luật từ {c₁}.

Conditional base:
- Từ node c₁ dưới a₁: path [a₁], count=1, classCount={A:1}
- Từ node c₁ dưới b₂: path [a₁, b₂], count=2, classCount={B:1, C:1}

Tần số condbase: a₁:3, b₂:2.

**Conditional tree cho c₁:**
```
root
 └── a₁ : 3  {A:1, B:1, C:1}
      └── b₂ : 2  {B:1, C:1}
```

Đào tiếp:
- Pattern {a₁, c₁}: sup = 3, classDist = {A:1, B:1, C:1}. Cả 3 class < minSup=2 → bỏ.
- Pattern {a₁, b₂, c₁}: sup = 2, classDist = {B:1, C:1} → cả 2 class < minSup → bỏ.
- Pattern {b₂, c₁}: sup = 2, classDist = {B:1, C:1} → bỏ.

**Nếu hạ minSup = 1**, thì sinh luật:
- `{a₁, b₂, c₁} ⇒ B` (sup=1, conf=1/2=50%)
- `{a₁, b₂, c₁} ⇒ C` (sup=1, conf=1/2=50%)

**Kết luận ví dụ:** Với minSup=2 trên tập dữ liệu chỉ 4 bản ghi, không có luật nào đủ mạnh được sinh ra → minh hoạ tại sao cần **nhiều dữ liệu** và **tham số minSup phù hợp**.

---

## 9. Phân Tích Ưu – Nhược Điểm

### 9.1 Ưu điểm

**★ Độ chính xác cao**
- Dùng nhiều luật thay vì 1 → giảm bias, giảm overfitting.
- Kết quả thực nghiệm trên 26 dataset UCI: CMAR thắng 13/26 lần (50%), accuracy trung bình 85.22% cao hơn C4.5 (84.09%) và CBA (84.69%).

**★ Hiệu quả bộ nhớ**
- CR-tree nén luật — tiết kiệm trung bình **77.12%** bộ nhớ so với CBA (Table 4 bài báo).

**★ Hiệu suất tốt**
- FP-Growth nhanh hơn Apriori vì không sinh ứng viên.
- Pruning sớm loại bỏ luật dư thừa → ít dữ liệu để xử lý.
- Trên dataset Sonar: CMAR chạy **19s**, CBA chạy **226s** (gấp ~12 lần chậm hơn).

**★ Xử lý class imbalance tốt**
- Normalization $\chi^2/\max\chi^2$ khắc phục bias về phía class minority.

**★ Phát hiện được mối tương quan phức hợp**
- Không như cây quyết định chỉ xét 1 thuộc tính mỗi bước, CMAR xét **nhiều thuộc tính cùng lúc** qua pattern.

**★ Diễn giải được (interpretable)**
- Kết quả phân lớp dựa trên các luật tường minh dạng "nếu ... thì ...".
- Có thể xem được các luật quan trọng, giúp chuyên gia hiểu mô hình.

### 9.2 Nhược điểm

**✗ Tham số nhạy**
- `minSup` thấp → bùng nổ pattern (có thể hàng triệu); cao → mất luật hiếm quan trọng.
- `minConf`, `χ²_thr`, `δ` đều cần tuning theo dataset.
- Bài báo thừa nhận: "there seems no way to pre-determine the best threshold values".

**✗ Chỉ xử lý thuộc tính rời rạc**
- Thuộc tính liên tục phải được **discretize** trước (biến thành các khoảng).
- Cách discretize ảnh hưởng lớn đến kết quả.

**✗ Phụ thuộc vào discretization**
- Với cùng dataset, các cách discretize khác nhau có thể cho accuracy chênh lệch 5–10%.

**✗ Khó khi số thuộc tính lớn**
- Dataset "wide" (nhiều thuộc tính) + minSup thấp → số pattern có thể bùng nổ chẳng hạn $2^n$ trường hợp.
- Dataset có 100+ thuộc tính cần minSup cao → mất luật hiếm.

**✗ Khó cập nhật tăng dần**
- Khi thêm dữ liệu mới, phải xây lại toàn bộ mô hình từ đầu.

### 9.3 So sánh với các đối thủ

| Tiêu chí | C4.5 | CBA | **CMAR** |
|----------|:----:|:---:|:-------:|
| Accuracy trung bình (26 UCI) | 84.09% | 84.69% | **85.22%** |
| Số luật dùng để classify | 1 (qua cây) | 1 (best) | **Nhiều** |
| Tốc độ huấn luyện | Nhanh | Chậm | Trung bình |
| Bộ nhớ | Nhỏ | Lớn | **Nhỏ (CR-tree)** |
| Diễn giải được | ✓ | ✓ | ✓ |
| Xử lý thuộc tính liên tục | ✓ (tự discretize) | Cần preprocess | Cần preprocess |
| Xử lý class imbalance | Kém | Kém | **Tốt (weighted χ²)** |

---

## 10. Bảng Tham Số & Hướng Dẫn Tinh Chỉnh

### 10.1 Các tham số chính

| Tham số | Kí hiệu | Giá trị thường dùng | Vai trò |
|---------|:-------:|:------------------:|---------|
| Min Support | $\text{minSup}$ | 1–5% | Ngưỡng tần số tối thiểu |
| Min Confidence | $\text{minConf}$ | 50% | Ngưỡng tin cậy luật |
| χ² threshold | $\chi^2_{\text{thr}}$ | 3.841 | Mức ý nghĩa thống kê (α=0.05) |
| Coverage delta | $\delta$ | 4 | Số lần cover tối đa/bản ghi |
| Confidence difference | $\Delta_{\text{conf}}$ | 20% | (Phụ) — ngưỡng chọn rule |

### 10.2 Hướng dẫn tinh chỉnh

**Dataset nhỏ (< 500 bản ghi):**
- minSup: 2–3 tuyệt đối (khoảng 1–2%)
- minConf: 50%
- δ: 2–4
- χ²_thr: có thể hạ xuống 2.706 (α=0.10) để giữ thêm luật

**Dataset trung bình (500–5000):**
- minSup: 1–3% (tương đối)
- minConf: 50–70%
- δ: 4 (mặc định)
- χ²_thr: 3.841

**Dataset lớn (> 5000):**
- minSup: 1% (tương đối)
- minConf: 50–60%
- δ: 4–5
- χ²_thr: 3.841 hoặc cao hơn (dữ liệu nhiều → dễ đạt ý nghĩa thống kê)

**Class imbalance nặng:**
- Giảm minSup để minority class có đủ luật
- Tăng δ → giữ nhiều luật hơn cho minority
- Cân nhắc stratified k-fold cross-validation

**Số thuộc tính nhiều (wide data):**
- Tăng minSup để tránh bùng nổ pattern
- Giới hạn độ dài pattern (maxPatternLength)
- Lọc bỏ thuộc tính ít mang thông tin trước (feature selection)

### 10.3 Bảng tra nhanh giá trị khuyến nghị

| Đặc điểm dataset | minSup | minConf | δ | χ²_thr |
|------------------|:------:|:-------:|:-:|:------:|
| Nhỏ, class cân bằng | 2 abs | 0.5 | 2 | 3.841 |
| Trung bình, cân bằng | 1–2% | 0.5 | 4 | 3.841 |
| Lớn, cân bằng | 1% | 0.5 | 4 | 3.841 |
| Class imbalance | ↓0.5% | 0.5 | ↑6 | ↓2.706 |
| Wide data | ↑3% | 0.6 | 4 | 3.841 |
| Noise nhiều | ↑2% | ↑0.7 | 4 | ↑6.635 |

---

## 11. Kết Luận

### 11.1 Tóm tắt các điểm chính của CMAR

CMAR là một thuật toán **associative classification** kết hợp ba ý tưởng lớn:

1. **FP-Growth mở rộng với phân bố lớp tại mỗi node** → sinh Class Association Rules **trực tiếp** trong quá trình đào mỏ, không cần bước hậu xử lý.

2. **CR-tree** (prefix-tree nén) làm cấu trúc lưu trữ luật, **chia sẻ tiền tố** giữa các luật → tiết kiệm bộ nhớ và cho phép truy vấn luật khớp nhanh qua DFS cắt tỉa.

3. **Weighted χ²** — phương pháp tổng hợp sức mạnh của **nhiều luật khớp** để quyết định lớp, thay vì chỉ dựa trên một luật duy nhất như CBA.

### 11.2 Điểm sáng tạo vượt trội

| Thành phần | Đổi mới so với tiền nhân |
|------------|-------------------------|
| Mining | FP-Growth + class distribution tại node |
| Pruning | 3 tầng: general → χ² → coverage |
| Storage | CR-tree thay vì danh sách phẳng |
| Classification | Weighted χ² thay vì single best rule |

### 11.3 Kết quả thực nghiệm tóm lược

Trên **26 dataset UCI Machine Learning Repository:**
- Accuracy trung bình: **85.22%** (so với 84.69% của CBA, 84.09% của C4.5)
- Thắng 13/26 lần (50%)
- Tiết kiệm trung bình **77.12%** bộ nhớ so với CBA
- Nhanh hơn CBA trên nhiều dataset lớn (ví dụ Sonar: 19s vs 226s)

### 11.4 Ứng dụng thực tế

CMAR phù hợp với các bài toán phân lớp:

| Lĩnh vực | Bài toán cụ thể |
|----------|----------------|
| Y tế | Chẩn đoán bệnh từ triệu chứng, xét nghiệm |
| Tài chính | Phê duyệt tín dụng, phát hiện gian lận |
| Marketing | Phân loại khách hàng tiềm năng |
| Sinh học | Phân loại sinh vật dựa trên đặc điểm |
| An ninh mạng | Phát hiện xâm nhập, spam filter |
| Giáo dục | Dự đoán kết quả học tập |

Đặc biệt thích hợp khi:
- Dữ liệu có **nhiều thuộc tính tương quan**
- Cần **giải thích được** (interpretability) quyết định
- Có các **luật nghiệp vụ** đã biết cần tích hợp

### 11.5 Hướng nghiên cứu mở rộng

Sau CMAR, nhiều công trình tiếp tục phát triển:
- **Lazy classification** — thay vì pre-compute tất cả luật, chỉ sinh luật khi có query
- **Fuzzy CMAR** — xử lý thuộc tính mờ
- **Hierarchical CMAR** — phân lớp đa cấp
- **Parallel/distributed CMAR** — cho big data (MapReduce, Spark)
- **Kết hợp với deep learning** — dùng CMAR làm feature extractor

---

## 12. Tài Liệu Tham Khảo

1. **Li, W., Han, J., & Pei, J. (2001).** *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* Proceedings of the 2001 IEEE International Conference on Data Mining (ICDM'01), San Jose, CA, USA, pp. 369–376. IEEE Computer Society.

2. **Han, J., Pei, J., & Yin, Y. (2000).** *Mining Frequent Patterns without Candidate Generation.* Proceedings of the 2000 ACM SIGMOD International Conference on Management of Data, Dallas, Texas, pp. 1–12.

3. **Liu, B., Hsu, W., & Ma, Y. (1998).** *Integrating Classification and Association Rule Mining.* Proceedings of the 4th International Conference on Knowledge Discovery and Data Mining (KDD'98), New York, pp. 80–86.

4. **Agrawal, R., & Srikant, R. (1994).** *Fast Algorithms for Mining Association Rules.* Proceedings of the 20th International Conference on Very Large Data Bases (VLDB'94), Santiago, Chile, pp. 487–499.

5. **Quinlan, J. R. (1993).** *C4.5: Programs for Machine Learning.* Morgan Kaufmann Publishers.

6. **Dong, G., Zhang, X., Wong, L., & Li, J. (1999).** *CAEP: Classification by Aggregating Emerging Patterns.* Proceedings of the 2nd International Conference on Discovery Science (DS'99), LNCS 1721, pp. 30–42.

7. **Wang, K., Zhou, S., & He, Y. (2000).** *Growing Decision Trees on Support-less Association Rules.* Proceedings of the 6th International Conference on Knowledge Discovery and Data Mining (KDD'00), Boston, MA, pp. 265–269.

8. **Lent, B., Swami, A., & Widom, J. (1997).** *Clustering Association Rules.* Proceedings of the 13th International Conference on Data Engineering (ICDE'97), Birmingham, UK, pp. 220–231.

9. **UCI Machine Learning Repository.** https://archive.ics.uci.edu/ml/

10. **Clark, P., & Niblett, T. (1989).** *The CN2 Induction Algorithm.* Machine Learning, 3(4), pp. 261–283.

11. **Duda, R. O., & Hart, P. E. (1973).** *Pattern Classification and Scene Analysis.* John Wiley & Sons.

12. **Lim, T.-S., Loh, W.-Y., & Shih, Y.-S. (2000).** *A Comparison of Prediction Accuracy, Complexity, and Training Time of Thirty-three Old and New Classification Algorithms.* Machine Learning, 40(3), pp. 203–228.

---

## Phụ Lục A — Thuật Ngữ Việt – Anh

| Tiếng Việt | Tiếng Anh | Ký hiệu |
|------------|-----------|:-------:|
| Phân lớp | Classification | — |
| Luật kết hợp lớp | Class Association Rule | CAR |
| Bộ phân lớp | Classifier | $\mathcal{F}$ |
| Tập huấn luyện | Training set | $T$ |
| Tập kiểm thử | Test set | $T_{\text{test}}$ |
| Pattern (mẫu) | Pattern | $P$ |
| Item (phần tử) | Item | $a_{ij}$ |
| Nhãn lớp | Class label | $c$ |
| Support (độ hỗ trợ) | Support | $\text{sup}$ |
| Confidence (độ tin cậy) | Confidence | $\text{conf}$ |
| Ngưỡng | Threshold | — |
| Luật tổng quát | General rule | — |
| Luật chi tiết | Specific rule | — |
| Thứ tự ưu tiên luật | Rule precedence | $\succ$ |
| Cắt tỉa luật | Rule pruning | — |
| Tương quan dương | Positively correlated | $ad > bc$ |
| Bảng ngẫu nhiên 2×2 | 2×2 contingency table | — |
| Kiểm định chi bình phương | Chi-square test | $\chi^2$ |
| Cận trên của χ² | Max chi-square | $\max\chi^2$ |
| Chi-square có trọng số | Weighted chi-square | — |
| Cây tiền tố | Prefix tree | — |
| Đào mỏ đệ quy | Recursive mining | — |
| Cây FP có nhận diện lớp | Class-distribution-associated FP-tree | — |
| Cây lưu luật kết hợp | Class-association-rule tree | CR-tree |
| Bảng đầu mục | Header table | — |
| Liên kết node | Node-link | — |
| Cơ sở mẫu điều kiện | Conditional pattern base | — |
| Cây điều kiện | Conditional FP-tree | — |
| Độ bao phủ cơ sở dữ liệu | Database coverage | — |
| Lớp mặc định | Default class | — |
| Ma trận nhầm lẫn | Confusion matrix | — |
| Độ chính xác | Precision | $P$ |
| Độ bao phủ | Recall | $R$ |
| Điểm F1 | F1-score | $F_1$ |
| Kiểm chứng chéo | Cross-validation | CV |
| Rời rạc hoá | Discretization | — |

---

## Phụ Lục B — Danh Sách Các Ký Hiệu Toán Học

| Ký hiệu | Ý nghĩa |
|---------|---------|
| $T$ | Tập huấn luyện |
| $\|T\|$ | Số bản ghi huấn luyện |
| $\mathcal{A}$ | Tập các thuộc tính |
| $\mathcal{C}$ | Tập các nhãn lớp |
| $t$ | Một bản ghi (transaction) |
| $\text{items}(t)$ | Tập item của bản ghi $t$ |
| $\text{class}(t)$ | Nhãn lớp của bản ghi $t$ |
| $P$ | Pattern (tập item) |
| $R$ | Luật (Rule) |
| $R: P \Rightarrow c$ | Luật kết hợp lớp |
| $\text{sup}(R)$ | Support tuyệt đối của luật |
| $\text{conf}(R)$ | Confidence của luật |
| $\sigma$ | Support tương đối |
| $R_1 \succ R_2$ | $R_1$ ưu tiên hơn $R_2$ |
| $\chi^2$ | Giá trị chi-square |
| $\max\chi^2$ | Cận trên của χ² |
| $a, b, c, d$ | Các ô trong bảng contingency 2×2 |
| $n$ | Tổng số bản ghi trong bảng contingency |
| $O_{ij}, E_{ij}$ | Giá trị quan sát/kỳ vọng |
| $\mathcal{R}(t)$ | Tập luật khớp với bản ghi $t$ |
| $\mathcal{G}_c$ | Nhóm các luật có class $c$ |
| $\text{score}(\mathcal{G}_c)$ | Điểm weighted χ² của nhóm |
| $\hat{c}(t)$ | Lớp dự đoán cho $t$ |
| $\delta$ | Ngưỡng database coverage |
| $\alpha$ | Mức ý nghĩa thống kê |
| $df$ | Bậc tự do (degrees of freedom) |
| $TP, FP, FN, TN$ | Confusion matrix entries |
| $P_c, R_c, F_1^{(c)}$ | Precision/Recall/F1 theo class $c$ |

---

*Báo cáo lý thuyết này trình bày đầy đủ thuật toán CMAR: từ động cơ nghiên cứu, kiến thức nền, kiến trúc tổng thể, ba giai đoạn chính, hệ thống công thức toán học, đến các ví dụ minh hoạ và phân tích ưu nhược điểm — không đề cập đến chi tiết cài đặt, phù hợp cho mục đích báo cáo học thuật.*
