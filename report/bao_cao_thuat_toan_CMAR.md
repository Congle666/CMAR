# BÁO CÁO THUẬT TOÁN CMAR

**Classification based on Multiple Association Rules**

> Tài liệu gốc: Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules.* **ICDM 2001**, pp. 369–376.
>
> Cài đặt tham chiếu: [`d:\CMAR\src`](../src/) — Java thuần, không phụ thuộc thư viện ngoài.

---

## Mục Lục

1. [Giới Thiệu Chung](#1-giới-thiệu-chung)
2. [Các Định Nghĩa Cơ Bản](#2-các-định-nghĩa-cơ-bản)
3. [Kiến Trúc Tổng Thể & Luồng Hoạt Động](#3-kiến-trúc-tổng-thể--luồng-hoạt-động)
4. [Giai Đoạn 1 — Sinh Luật (Rule Generation)](#4-giai-đoạn-1--sinh-luật-rule-generation)
   - 4.1 [Cấu trúc dữ liệu CR-tree (FP-tree mở rộng)](#41-cấu-trúc-dữ-liệu-cr-tree-fp-tree-mở-rộng)
   - 4.2 [FP-Growth có nhận diện lớp](#42-fp-growth-có-nhận-diện-lớp)
   - 4.3 [Sinh luật CAR trực tiếp trong lúc đào mỏ](#43-sinh-luật-car-trực-tiếp-trong-lúc-đào-mỏ)
5. [Giai Đoạn 2 — Cắt Tỉa Luật (Rule Pruning)](#5-giai-đoạn-2--cắt-tỉa-luật-rule-pruning)
   - 5.1 [Pruning 1: Luật tổng quát lấn át luật chi tiết](#51-pruning-1-luật-tổng-quát-lấn-át-luật-chi-tiết)
   - 5.2 [Pruning 2: Kiểm định χ² (tương quan dương)](#52-pruning-2-kiểm-định-χ²-tương-quan-dương)
   - 5.3 [Pruning 3: Database Coverage (Thuật toán 1)](#53-pruning-3-database-coverage-thuật-toán-1)
6. [Giai Đoạn 3 — Phân Lớp Bằng Weighted χ²](#6-giai-đoạn-3--phân-lớp-bằng-weighted-χ²)
7. [Các Công Thức Toán Học Đầy Đủ](#7-các-công-thức-toán-học-đầy-đủ)
8. [Pseudo-code Tổng Hợp](#8-pseudo-code-tổng-hợp)
9. [Ánh Xạ Giữa Lý Thuyết Và Cài Đặt Java](#9-ánh-xạ-giữa-lý-thuyết-và-cài-đặt-java)
10. [Tham Số Cấu Hình](#10-tham-số-cấu-hình)
11. [Phân Tích Độ Phức Tạp](#11-phân-tích-độ-phức-tạp)
12. [Ví Dụ Minh Hoạ Từng Bước](#12-ví-dụ-minh-hoạ-từng-bước)
13. [Đánh Giá & So Sánh Với Các Thuật Toán Khác](#13-đánh-giá--so-sánh-với-các-thuật-toán-khác)
14. [Kết Luận](#14-kết-luận)

---

## 1. Giới Thiệu Chung

### 1.1 Bài toán

Phân lớp (classification) là bài toán cốt lõi của khai phá dữ liệu: cho trước tập huấn luyện gồm các bản ghi có nhãn lớp, xây dựng một **bộ phân lớp** (classifier) để dự đoán nhãn lớp cho bản ghi mới chưa biết nhãn. Các phương pháp kinh điển gồm: cây quyết định (C4.5), naive Bayes, k-NN, mạng nơ-ron…

Một hướng tiếp cận khác là **phân lớp dựa trên luật kết hợp** (associative classification) — sinh toàn bộ **luật kết hợp lớp** (Class Association Rule — CAR) vượt qua ngưỡng support và confidence, sau đó chọn luật để dự đoán. Đại diện là CBA (Liu et al., 1998), CAEP, ADT.

### 1.2 Hạn chế của phương pháp liên kết truyền thống

Bài báo CMAR chỉ ra hai điểm yếu chính của CBA và các phương pháp cùng thời:

| Vấn đề | Mô tả |
|--------|-------|
| **(1) Dự đoán dựa trên một luật duy nhất** | CBA chọn luật có confidence cao nhất để phân lớp → dễ sai lệch, overfitting, nhạy cảm với nhiễu. |
| **(2) Số luật bùng nổ tổ hợp** | Khi ngưỡng support thấp, số CAR có thể lên tới hàng trăm nghìn → khó lưu trữ, truy vấn, cắt tỉa. |

### 1.3 Ba đóng góp chính của CMAR

CMAR giải quyết đồng thời cả hai vấn đề trên:

1. **Phân lớp bằng NHIỀU luật** — không chọn 1 luật, mà lấy tất cả luật khớp với bản ghi, nhóm theo class, rồi so sánh "sức mạnh tổng hợp" của từng nhóm bằng **weighted χ²** (chi-square có trọng số).
2. **CR-tree (Class-Association-Rule tree)** — cấu trúc prefix-tree để lưu luật nén, chia sẻ tiền tố giữa các luật, tiết kiệm ~50–60% bộ nhớ và hỗ trợ truy xuất luật khớp nhanh.
3. **Biến thể FP-Growth nhận diện lớp** — FP-tree truyền thống được mở rộng với phân bố lớp ngay tại mỗi node → sinh CAR trực tiếp trong lúc đào mỏ, không cần bước "tạo luật" hậu xử lý.

---

## 2. Các Định Nghĩa Cơ Bản

Các ký hiệu sau được dùng xuyên suốt báo cáo:

| Ký hiệu | Ý nghĩa |
|---------|---------|
| $\mathcal{A} = \{A_1, A_2, \dots, A_n\}$ | Tập các thuộc tính (attributes) |
| $a_{ij}$ | Một giá trị thuộc tính cụ thể (item), ví dụ `outlook=sunny` |
| $\mathcal{C} = \{c_1, c_2, \dots, c_m\}$ | Tập các nhãn lớp |
| $T$ | Tập huấn luyện — mỗi phần tử là một cặp `(itemset, class)` |
| $|T|$ | Tổng số bản ghi huấn luyện |
| $P = \{a_{i_1}, \dots, a_{i_k}\}$ | Một **pattern** (tập item) |
| $R: P \Rightarrow c$ | Một **Class Association Rule (CAR)** — nếu bản ghi khớp pattern $P$ thì dự đoán lớp $c$ |

### 2.1 Support của rule

$$
\text{sup}(R) = \#\{obj \in T \mid obj \text{ khớp } P \text{ và } \text{class}(obj) = c\}
$$

Support tuyệt đối = số bản ghi chứa cả $P$ và có lớp $c$.

Support tương đối: $\text{sup}(R)/|T|$.

### 2.2 Confidence của rule

$$
\text{conf}(R) = \frac{\text{sup}(R)}{\text{sup}(P)} = \frac{\#\{obj \mid P \subseteq obj \text{ và } c(obj) = c\}}{\#\{obj \mid P \subseteq obj\}}
$$

Confidence = xác suất có điều kiện $P(c \mid P)$. Một luật là "đáng tin" khi confidence gần 1.

### 2.3 Class Association Rule (CAR)

$R: P \Rightarrow c$ là CAR **hợp lệ** nếu:

$$
\text{sup}(R) \ge \text{minSup} \quad \text{VÀ} \quad \text{conf}(R) \ge \text{minConf}
$$

Nhiệm vụ của bước đào mỏ là tìm **toàn bộ** tập CAR hợp lệ.

### 2.4 Thứ tự ưu tiên (Rule Precedence)

Bài báo định nghĩa quan hệ $R_1 \succ R_2$ (R₁ ưu tiên hơn R₂):

$$
R_1 \succ R_2 \iff
\begin{cases}
\text{conf}(R_1) > \text{conf}(R_2), & \text{hoặc}\\
\text{conf}(R_1) = \text{conf}(R_2) \text{ và } \text{sup}(R_1) > \text{sup}(R_2), & \text{hoặc}\\
\text{bằng nhau nhưng } |P_1| < |P_2|
\end{cases}
$$

Nghĩa là: **confidence cao trước → support cao trước → condset ngắn trước (luật đơn giản hơn ưu tiên)**.

**Trong code Java** ([`AssociationRule.java:59-66`](../src/AssociationRule.java#L59-L66)):
```java
public int compareTo(AssociationRule other) {
    int cmp = Double.compare(other.confidence, this.confidence); // conf DESC
    if (cmp != 0) return cmp;
    cmp = Integer.compare(other.supportCount, this.supportCount); // sup DESC
    if (cmp != 0) return cmp;
    return Integer.compare(this.condset.size(), other.condset.size()); // size ASC
}
```

### 2.5 Luật tổng quát / chi tiết

$R_1: P_1 \Rightarrow c_1$ được gọi là **tổng quát hơn** (general) $R_2: P_2 \Rightarrow c_2$ nếu $P_1 \subseteq P_2$ (điều kiện của $R_1$ là tập con của $R_2$). Khi đó $R_2$ là **chi tiết hơn** (specific).

---

## 3. Kiến Trúc Tổng Thể & Luồng Hoạt Động

### 3.1 Sơ đồ pipeline (7 bước — theo `Main.java`)

```
┌──────────────────────────────────────────────────────────────────────┐
│  [1] LOAD DATA (DatasetLoader.java)                                  │
│      CSV → List<Transaction>                                         │
│      Mỗi dòng: items = "col=val", class = cột cuối                   │
└──────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  [2] SHUFFLE & SPLIT 80/20 (Main.java)                               │
│      seed cố định (42) → tái lập kết quả                             │
└──────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  [3] FP-GROWTH ĐÀO MỎ + SINH CAR (FPGrowth.java + FPTree.java)       │
│      3a. Đếm tần số item, lọc theo minSup                            │
│      3b. Xây CR-tree khởi tạo (mỗi node có phân bố lớp)              │
│      3c. Đào mỏ đệ quy:                                              │
│          - Duyệt header table từ item hiếm nhất                      │
│          - Với mỗi pattern P, thu phân bố lớp từ header chain        │
│          - Sinh ngay CAR nếu qua ngưỡng sup, conf                    │
│          - Xây conditional CR-tree → đệ quy                          │
│      Kết quả: List<AssociationRule> ứng viên                         │
└──────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  [4] HUẤN LUYỆN CMAR — 3 GIAI ĐOẠN PRUNING (CMARClassifier.java)    │
│      4a. Pruning 1: luật tổng quát lấn át luật chi tiết              │
│      4b. Pruning 2: giữ luật có χ² ≥ ngưỡng VÀ tương quan dương      │
│      4c. Pruning 3: Database coverage (δ lần cover)                  │
│      4d. Chèn luật sống sót vào CR-tree (CRTree.java)                │
└──────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  [5] PHÂN LỚP TẬP TEST (CMARClassifier.classify)                     │
│      Với mỗi test record:                                            │
│      a. Truy vấn CR-tree → lấy tập luật khớp                         │
│      b. Nếu rỗng → trả default class                                 │
│      c. Nếu tất cả cùng 1 class → trả class đó                       │
│      d. Ngược lại → tính weighted χ² cho mỗi class, chọn max         │
└──────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│  [6] ĐÁNH GIÁ (accuracy, precision, recall, F1 — ResultWriter)       │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 Hai giai đoạn chính

Bài báo phân chia CMAR thành hai **phase** lớn:

| Phase | Tên | Nội dung | File Java |
|-------|-----|----------|-----------|
| **Phase 1** | Rule Generation | Đào mỏ CAR, cắt tỉa, lưu vào CR-tree | `FPGrowth.java`, `CMARClassifier.java` (train), `CRTree.java` |
| **Phase 2** | Classification | Truy vấn CR-tree, tính weighted χ², quyết định class | `CMARClassifier.java` (classify) |

---

## 4. Giai Đoạn 1 — Sinh Luật (Rule Generation)

### 4.1 Cấu trúc dữ liệu CR-tree (FP-tree mở rộng)

CMAR mở rộng FP-tree của Han & Pei (2000) bằng cách **gắn phân bố lớp** cho từng node.

**Node của CR-tree** ([`FPNode.java`](../src/FPNode.java)):
```java
class FPNode {
    String item;                        // tên item (vd "outlook=sunny")
    int count;                          // tổng số transaction đi qua node
    FPNode parent;
    Map<String, FPNode> children;
    FPNode nodeLink;                    // liên kết header table
    Map<String, Integer> classCount;    // ★ PHÂN BỐ LỚP tại node này
}
```

Chính `classCount` là "thành phần mở rộng then chốt" (Section 3.2 bài báo):

> *"The difference of CMAR from other associative classification methods is that for every pattern, CMAR maintains the distribution of various class labels among data objects matching the pattern."*

**Ý nghĩa:** Tại mỗi node, chúng ta biết được có bao nhiêu bản ghi của từng class đi qua node đó. Nhờ vậy, khi tìm được một frequent pattern, ta biết **ngay** có bao nhiêu bản ghi mỗi class khớp pattern → sinh CAR trực tiếp không cần quét lại.

### 4.2 FP-Growth có nhận diện lớp

#### Bước 1 — Đếm item & lọc theo `minSup`

Duyệt toàn bộ training set, đếm tần số mỗi item (không tính class item). Loại bỏ item có `freq < minSup`.

#### Bước 2 — Sắp xếp theo tần số giảm dần (F-list)

Sắp item còn lại theo tần số **giảm dần** → giúp các tiền tố phổ biến được chia sẻ tối đa trong cây.

#### Bước 3 — Xây CR-tree khởi tạo

Với mỗi transaction $t$:
1. Trích item nằm trong F-list, sắp theo thứ tự F-list.
2. Chèn đường đi này vào cây từ gốc, tại mỗi node:
   - Tăng `count += 1`
   - `classCount[class(t)] += 1`
   - Cập nhật liên kết header table (node-link)

**Code tham chiếu** ([`FPTree.java:45-71`](../src/FPTree.java#L45-L71)):
```java
public void insertPath(List<String> path, Map<String, Integer> classDist, int count) {
    FPNode current = root;
    for (String item : path) {
        FPNode child = current.children.get(item);
        if (child == null) {
            child = new FPNode(item, 0, current);
            current.children.put(item, child);
            // ... cập nhật node-link ...
        }
        child.count += count;
        for (Map.Entry<String, Integer> e : classDist.entrySet()) {
            child.classCount.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        current = child;
    }
}
```

#### Bước 4 — Đào mỏ đệ quy

Với từng item trong header table (duyệt **theo tần số tăng dần** — quy tắc FP-Growth):

**a. Tạo pattern mới** $P' = \text{prefix} \cup \{item\}$.

**b. Thu phân bố lớp của $P'$** bằng cách đi dọc header chain của item, cộng dồn `classCount`:

$$
\text{classDist}(P') = \bigoplus_{\text{node trong chain}} \text{node.classCount}
$$

**c. Sinh CAR trực tiếp** (xem §4.3).

**d. Xây conditional pattern base** — với mỗi node trong chain, đi ngược lên gốc để lấy prefix-path, kèm theo `count` và `classCount` của node đó.

**e. Xây conditional CR-tree** từ conditional pattern base, đảm bảo lan truyền phân bố lớp.

**f. Đệ quy** đào cây con mới với prefix đã mở rộng.

### 4.3 Sinh luật CAR trực tiếp trong lúc đào mỏ

Khi đã tìm được frequent pattern $P$ với `itemSupport = sup(P)` và phân bố lớp `classDist[c] = sup(P ∪ {c})`, ta sinh CAR cho mỗi class $c$ thỏa:

$$
\text{sup}(P \Rightarrow c) = \text{classDist}[c] \ge \text{minSup}
$$

$$
\text{conf}(P \Rightarrow c) = \frac{\text{classDist}[c]}{\text{itemSupport}} \ge \text{minConf}
$$

**Code** ([`FPGrowth.java:146-161`](../src/FPGrowth.java#L146-L161)):
```java
for (Map.Entry<String, Integer> e : classDistForP.entrySet()) {
    String cls = e.getKey();
    int classSup = e.getValue();
    if (classSup < minSupport) continue;

    double confidence = (double) classSup / itemSupport;
    if (confidence < minConfidence) continue;

    double support = (double) classSup / totalTransactions;
    rules.add(new AssociationRule(
        new HashSet<>(patternSet), cls,
        support, confidence,
        classSup, itemSupport
    ));
}
```

Đây là điểm khác biệt **then chốt** so với FP-Growth gốc và CBA: CMAR **không tách biệt** giai đoạn "mine pattern" với "sinh rule" — hai việc này hợp nhất làm một.

---

## 5. Giai Đoạn 2 — Cắt Tỉa Luật (Rule Pruning)

Sau khi có tập ứng viên CAR (có thể rất lớn), CMAR áp dụng **3 tầng cắt tỉa** (Section 3.3 bài báo) để loại luật dư thừa/nhiễu.

### 5.1 Pruning 1: Luật tổng quát lấn át luật chi tiết

**Quy tắc:** Nếu $R_1$ tổng quát hơn $R_2$ ($P_1 \subseteq P_2$), cùng class, và $R_1 \succ R_2$ (ưu tiên cao hơn) thì loại $R_2$.

**Ý tưởng:** Luật đơn giản hơn (ít điều kiện) mà confidence cao hơn thì đã "bao" luật chi tiết → luật chi tiết dư thừa.

**Thuật toán:** Sắp ứng viên giảm dần theo precedence, duyệt từ đầu. Với mỗi $r$, kiểm tra $r$ có bị luật $r'$ đã giữ trước (cùng class, condset nhỏ hơn) lấn át không. Nếu có → loại.

**Code** ([`CMARClassifier.java:107-129`](../src/CMARClassifier.java#L107-L129)):
```java
private List<AssociationRule> pruneByGeneralRules(List<AssociationRule> sorted) {
    List<AssociationRule> kept = new ArrayList<>();
    Map<String, List<AssociationRule>> keptByClass = new HashMap<>();

    for (AssociationRule r1 : sorted) {
        boolean dominated = false;
        List<AssociationRule> sameClass = keptByClass.get(r1.getClassLabel());
        if (sameClass != null) {
            for (AssociationRule r2 : sameClass) {
                if (r1.getCondset().containsAll(r2.getCondset())) {
                    dominated = true;
                    break;
                }
            }
        }
        if (!dominated) {
            kept.add(r1);
            keptByClass.computeIfAbsent(r1.getClassLabel(), k -> new ArrayList<>()).add(r1);
        }
    }
    return kept;
}
```

### 5.2 Pruning 2: Kiểm định χ² (tương quan dương)

**Mục đích:** Chỉ giữ luật có **tương quan thống kê dương** giữa pattern $P$ và class $c$. Luật vô nghĩa (χ² thấp) hoặc tương quan nghịch sẽ bị loại.

#### Bảng contingency 2×2

|              | class = $c$ | class ≠ $c$ | tổng |
|--------------|:-----------:|:-----------:|:----:|
| $P$ xuất hiện | $a$ | $b$ | $a+b$ |
| $P$ không xuất hiện | $c$ | $d$ | $c+d$ |
| **tổng** | $a+c$ | $b+d$ | $n=|T|$ |

Trong đó:
- $a = \text{sup}(P \Rightarrow c)$ — số bản ghi khớp $P$ **và** có lớp $c$
- $b = \text{sup}(P) - a$ — khớp $P$ nhưng **khác** lớp $c$
- $c = \text{sup}(c) - a$ — có lớp $c$ nhưng không khớp $P$
- $d = n - a - b - c$ — không khớp $P$ và không có lớp $c$

#### Công thức χ²

$$
\chi^2 = \frac{n \cdot (ad - bc)^2}{(a+b)(c+d)(a+c)(b+d)}
$$

#### Điều kiện giữ luật

$$
\chi^2 \ge \chi^2_{\text{threshold}} \quad \text{VÀ} \quad ad > bc
$$

Điều kiện $ad > bc$ đảm bảo **tương quan dương** (observed > expected): xuất hiện $P$ làm **tăng** xác suất lớp $c$.

Giá trị mặc định $\chi^2_{\text{threshold}} = 3.841$ ứng với mức ý nghĩa $\alpha = 0.05$, bậc tự do $df = 1$.

**Code** ([`CMARClassifier.java:135-152`](../src/CMARClassifier.java#L135-L152)):
```java
for (AssociationRule r : rules) {
    double chi2 = computeChiSquare(r, r.getClassLabel());
    double a = r.getSupportCount();
    double b = r.getCondsetSupportCount() - a;
    double c = classFreq.getOrDefault(r.getClassLabel(), 0) - a;
    double d = totalTransactions - a - b - c;
    boolean positivelyCorrelated = (a * d > b * c);

    if (chi2 >= chiSquareThreshold && positivelyCorrelated) kept.add(r);
}
```

### 5.3 Pruning 3: Database Coverage (Thuật toán 1)

Bài báo (Section 3.3, Figure 3) định nghĩa **Algorithm 1**:

```
Input : tập luật R (đã sắp theo precedence), ngưỡng coverage δ
Output: tập luật con S dùng cho phân lớp
Method:
  1.  Sắp luật theo precedence giảm dần
  2.  cover_count[obj] ← 0 cho mọi obj ∈ T
  3.  While (cả T và R đều khác rỗng):
        Với mỗi rule R ∈ tập luật (duyệt theo precedence):
          Tìm mọi object trong T mà R phân lớp ĐÚNG (P(R) ⊆ obj và class(R) = class(obj))
          Nếu có ít nhất 1 object như vậy:
             Chọn R vào S
             Tăng cover_count[obj] cho mỗi object đó
             Nếu cover_count[obj] ≥ δ → loại obj khỏi T
```

**Ý nghĩa:** Khác với CBA — vốn loại bản ghi ngay khi bị 1 luật cover — CMAR cho phép **mỗi bản ghi bị cover tối đa δ lần** trước khi loại. Điều này giúp giữ lại nhiều luật hơn ⇒ khi phân lớp bản ghi mới, có nhiều luật để "hội ý" hơn ⇒ độ chính xác cao hơn.

Giá trị mặc định trong bài báo: **δ = 4**.

**Code** ([`CMARClassifier.java:158-188`](../src/CMARClassifier.java#L158-L188)):
```java
private List<AssociationRule> pruneByDatabaseCoverage(
        List<AssociationRule> rules, List<Transaction> trainData) {
    int n = trainData.size();
    int[] coverCount = new int[n];
    boolean[] removed = new boolean[n];
    int remainingCount = n;
    List<AssociationRule> selected = new ArrayList<>();

    for (AssociationRule rule : rules) {
        if (remainingCount == 0) break;
        boolean coversAny = false;
        for (int i = 0; i < n; i++) {
            if (removed[i]) continue;
            Transaction t = trainData.get(i);
            if (rule.matches(t) && rule.getClassLabel().equals(t.getClassLabel())) {
                coversAny = true;
                coverCount[i]++;
                if (coverCount[i] >= coverageThreshold) {
                    removed[i] = true;
                    remainingCount--;
                }
            }
        }
        if (coversAny) selected.add(rule);
    }
    return selected;
}
```

---

## 6. Giai Đoạn 3 — Phân Lớp Bằng Weighted χ²

Cho bản ghi test $t$, CMAR thực hiện 4 bước:

### Bước 1 — Truy vấn tập luật khớp

Sử dụng **CR-tree** để lấy tập $\mathcal{R}(t) = \{R \mid \text{condset}(R) \subseteq t\}$.

Thuật toán duyệt CR-tree (DFS có cắt tỉa):
- Bắt đầu từ root, chỉ đi xuống các con có item xuất hiện trong $t$
- Tại mỗi node, thu thập tất cả luật kết thúc tại node đó

**Code** ([`CRTree.java:90-97`](../src/CRTree.java#L90-L97)):
```java
private void collect(Node node, Set<String> items, List<AssociationRule> out) {
    out.addAll(node.rules);
    for (Node child : node.children.values()) {
        if (items.contains(child.item)) {
            collect(child, items, out);
        }
    }
}
```

Độ phức tạp: $O(|t| \cdot k)$ thay vì $O(N_{rules})$ — **nhanh hơn** duyệt phẳng danh sách luật.

### Bước 2 — Không có luật nào khớp

Trả về **default class** = class phổ biến nhất trong training set.

### Bước 3 — Tất cả luật khớp cùng class $c$

Trả về $c$ luôn — không cần tính toán gì thêm.

### Bước 4 — Luật khớp đa dạng class → Weighted χ²

Nhóm tập luật $\mathcal{R}(t)$ theo class: $\mathcal{G}_c = \{R \in \mathcal{R}(t) \mid \text{class}(R) = c\}$.

Với mỗi nhóm $\mathcal{G}_c$, tính **weighted χ² score**:

$$
\boxed{\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \frac{\chi^2(R)^2}{\max\chi^2(R)}}
$$

trong đó $\max\chi^2(R)$ là **cận trên lý thuyết** của χ² — xem §7.3. Trả về class có score lớn nhất.

**Tại sao lại dùng $\chi^2 / \max\chi^2$?**

Bài báo (Section 4) giải thích: nếu chỉ cộng $\chi^2$ đơn thuần, các class minority (ít mẫu) dễ có $\chi^2$ lớn bất thường do mẫu nhỏ → thiên vị minority. Chia cho $\max\chi^2$ tạo ra tỉ số trong khoảng $[0, 1]$, đặt mọi luật trên cùng thang đo bất kể support/class distribution.

**Code** ([`CMARClassifier.java:209-226`](../src/CMARClassifier.java#L209-L226)):
```java
for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
    String cls = entry.getKey();
    double score = 0.0;
    for (AssociationRule rule : entry.getValue()) {
        double chi2 = computeChiSquare(rule, cls);
        double maxChi2 = computeMaxChiSquare(rule, cls);
        if (maxChi2 > 0) score += (chi2 * chi2) / maxChi2;
    }
    if (score > bestScore) {
        bestScore = score;
        bestClass = cls;
    }
}
```

---

## 7. Các Công Thức Toán Học Đầy Đủ

### 7.1 Support & Confidence

$$
\text{sup}(P \Rightarrow c) = \frac{|\{obj \in T : P \subseteq obj \wedge \text{class}(obj) = c\}|}{|T|}
$$

$$
\text{conf}(P \Rightarrow c) = \frac{\text{sup}(P \cup \{c\})}{\text{sup}(P)} = \frac{\#(P, c)}{\#(P)}
$$

### 7.2 Chi-square (χ²) chuẩn

Với bảng contingency 2×2 ở §5.2:

$$
\chi^2 = \sum_{i,j} \frac{(O_{ij} - E_{ij})^2}{E_{ij}}
$$

Trong đó $O_{ij}$ là giá trị quan sát và $E_{ij}$ là giá trị kỳ vọng (dưới giả định độc lập).
Công thức gọn (Yates/standard) cho bảng 2×2:

$$
\chi^2 = \frac{n(ad - bc)^2}{(a+b)(c+d)(a+c)(b+d)}
$$

**Kỳ vọng** (để so với quan sát):
$$
E(P, c) = \frac{\text{sup}(P) \cdot \text{sup}(c)}{n}
$$

**Điều kiện tương quan dương** (observed > expected):
$$
a > E(P, c) \iff a \cdot n > \text{sup}(P) \cdot \text{sup}(c) \iff ad > bc
$$

### 7.3 max χ² (cận trên lý thuyết)

Bài báo định nghĩa `maxχ²` như sau: giả sử mọi bản ghi khớp $P$ đều có class $c$, ta đạt giá trị `a` tối đa. Khi đó:

$$
a_{\max} = \min(\text{sup}(P), \text{sup}(c))
$$

và $b, c, d$ được tính theo:
$$
b = \text{sup}(P) - a_{\max}, \quad c = \text{sup}(c) - a_{\max}, \quad d = n - a_{\max} - b - c
$$

Sau đó áp dụng cùng công thức χ² trên, thu được $\max\chi^2$.

**Code** ([`CMARClassifier.java:245-258`](../src/CMARClassifier.java#L245-L258)):
```java
private double computeMaxChiSquare(AssociationRule rule, String cls) {
    double n    = totalTransactions;
    double supP = rule.getCondsetSupportCount();
    double supC = classFreq.getOrDefault(cls, 0);
    double a = Math.min(supP, supC);
    double b = supP - a;
    double c = supC - a;
    double d = n - a - b - c;
    double denom = (a + b) * (c + d) * (a + c) * (b + d);
    if (denom == 0) return 0.0;
    return n * Math.pow(a * d - b * c, 2) / denom;
}
```

### 7.4 Weighted χ² score cho nhóm

$$
\text{score}(\mathcal{G}_c) = \sum_{R \in \mathcal{G}_c} \frac{\chi^2(R)^2}{\max\chi^2(R)}
$$

Class được chọn:
$$
\hat{c} = \arg\max_{c \in \mathcal{C}} \text{score}(\mathcal{G}_c)
$$

### 7.5 Độ đo đánh giá

Với từng class $c$:

$$
\text{Precision}(c) = \frac{TP_c}{TP_c + FP_c}, \quad \text{Recall}(c) = \frac{TP_c}{TP_c + FN_c}
$$

$$
F_1(c) = \frac{2 \cdot \text{Precision}(c) \cdot \text{Recall}(c)}{\text{Precision}(c) + \text{Recall}(c)}
$$

$$
\text{Accuracy} = \frac{\sum_c TP_c}{|T_{\text{test}}|}, \quad \text{Macro-F}_1 = \frac{1}{|\mathcal{C}|} \sum_c F_1(c)
$$

---

## 8. Pseudo-code Tổng Hợp

### 8.1 Hàm `CMAR-Train(T, minSup, minConf, χ²_thr, δ)`

```
INPUT : T — tập huấn luyện
        minSup — ngưỡng support
        minConf — ngưỡng confidence
        χ²_thr — ngưỡng chi-square (mặc định 3.841)
        δ — ngưỡng coverage (mặc định 4)
OUTPUT: CR-tree chứa luật phân lớp + defaultClass

1. Đếm freq(item) và freq(class) trên T
2. defaultClass ← argmax_c freq(c)
3. F ← {item : freq(item) ≥ minSup}
4. Sắp F theo tần số giảm dần (F-list)

// --- Xây cây khởi tạo ---
5. Khởi tạo CR-tree rỗng
6. Với mỗi transaction t ∈ T:
     path ← item(t) ∩ F, sắp theo F-list
     InsertPath(path, {class(t): 1}, 1)

// --- Đào mỏ đệ quy + sinh CAR ---
7. candidates ← []
8. MineTree(tree, prefix=[], candidates)  // xem 8.2

// --- 3 tầng cắt tỉa ---
9. Sắp candidates theo precedence
10. R1 ← PruneGeneral(candidates)          // §5.1
11. R2 ← PruneChiSquare(R1, χ²_thr)        // §5.2
12. R3 ← PruneCoverage(R2, T, δ)           // §5.3

// --- Xây CR-tree để truy vấn nhanh ---
13. crTree ← new CRTree(freq(item))
14. crTree.insertAll(R3)
15. RETURN (crTree, defaultClass)
```

### 8.2 Hàm đệ quy `MineTree(tree, prefix, candidates)`

```
1. Với mỗi item trong header(tree) theo thứ tự tần số tăng dần:
     itemSup ← tree.headerFreq[item]
     P ← prefix ∪ {item}

     // Thu phân bố class bằng header chain
     classDist ← {}
     node ← tree.headerFirst[item]
     while node != null:
         for (cls, cnt) in node.classCount:
             classDist[cls] += cnt
         node ← node.nodeLink

     // Sinh CAR trực tiếp
     for (cls, sup) in classDist:
         if sup ≥ minSup and sup/itemSup ≥ minConf:
             candidates.add(Rule(P, cls, sup, sup/itemSup))

     // Xây conditional pattern base
     condBase, condCounts, condClassDists ← []
     node ← tree.headerFirst[item]
     while node != null:
         prefixPath ← ancestors(node) (loại root)
         if prefixPath ≠ []:
             condBase.append(prefixPath)
             condCounts.append(node.count)
             condClassDists.append(copy(node.classCount))
         node ← node.nodeLink

     // Lọc và xây conditional CR-tree
     condFreq ← aggregate(condBase, condCounts)
     condFreq ← {it : freq ≥ minSup}
     if condFreq ≠ ∅:
         condTree ← buildTree(condBase, condFreq, condClassDists)
         MineTree(condTree, P, candidates)   // đệ quy
```

### 8.3 Hàm `CMAR-Classify(t, crTree, defaultClass)`

```
1. matching ← crTree.findMatching(t)    // DFS cắt tỉa
2. if matching = [] → RETURN defaultClass
3. byClass ← groupBy(matching, r → class(r))
4. if |byClass| = 1 → RETURN class duy nhất
5. // Weighted χ²
   bestCls, bestScore ← defaultClass, -∞
   for (cls, group) in byClass:
       score ← 0
       for R in group:
           chi2 ← ComputeChiSquare(R, cls)
           maxChi2 ← ComputeMaxChiSquare(R, cls)
           if maxChi2 > 0: score += chi2² / maxChi2
       if score > bestScore:
           bestScore, bestCls ← score, cls
6. RETURN bestCls
```

---

## 9. Ánh Xạ Giữa Lý Thuyết Và Cài Đặt Java

### 9.1 Bảng đối chiếu toàn bộ

| Khái niệm bài báo | File / Class Java | Dòng chính |
|-------------------|-------------------|------------|
| Transaction (obj = attribute-value) | [`Transaction.java`](../src/Transaction.java) | toàn file |
| Load CSV → transactions | [`DatasetLoader.java`](../src/DatasetLoader.java) | 18–54 |
| FP-tree / CR-tree node | [`FPNode.java`](../src/FPNode.java) | 15–33 |
| FP-tree (có phân bố class) | [`FPTree.java`](../src/FPTree.java) | toàn file |
| FP-Growth + sinh CAR trực tiếp | [`FPGrowth.java`](../src/FPGrowth.java) | 71–222 |
| Rule `P ⇒ c` | [`AssociationRule.java`](../src/AssociationRule.java) | toàn file |
| Rule precedence | [`AssociationRule.java:59`](../src/AssociationRule.java#L59) | — |
| Pruning 1: general rules | [`CMARClassifier.java:107`](../src/CMARClassifier.java#L107) | — |
| Pruning 2: χ² + tương quan dương | [`CMARClassifier.java:135`](../src/CMARClassifier.java#L135) | — |
| Pruning 3: Database coverage (Alg. 1) | [`CMARClassifier.java:158`](../src/CMARClassifier.java#L158) | — |
| CR-tree lưu luật | [`CRTree.java`](../src/CRTree.java) | toàn file |
| Truy vấn luật khớp (DFS cắt tỉa) | [`CRTree.java:83`](../src/CRTree.java#L83) | — |
| Weighted χ² classification | [`CMARClassifier.java:194`](../src/CMARClassifier.java#L194) | — |
| χ² & max χ² | [`CMARClassifier.java:233`](../src/CMARClassifier.java#L233) | — |
| Stratified k-fold CV | [`CrossValidator.java`](../src/CrossValidator.java) | toàn file |
| Pipeline chính | [`Main.java`](../src/Main.java) | 44–163 |

### 9.2 Điểm quan trọng trong cài đặt

1. **Lớp item và giá trị item** — được mã hoá thành chuỗi `"columnName=value"` (ví dụ `"outlook=sunny"`). Class là thuộc tính cuối cùng trong CSV.
2. **Class item không lẫn vào header FP-tree** — cài đặt giữ class riêng trong `classCount`, không đưa vào `items` của transaction khi xây cây → tránh hack kiểu "thêm `class=X` làm item giả" của CBA.
3. **FP-Growth đệ quy theo item tần số tăng dần** — tạo prefix ngắn trước, mở rộng dần → đảm bảo hoàn tất (tìm đủ mọi pattern).
4. **CR-tree dùng 2 lần**: (a) làm "FP-tree mở rộng" trong lúc đào mỏ ([`FPTree`](../src/FPTree.java)); (b) làm "kho luật" sau cắt tỉa ([`CRTree`](../src/CRTree.java)) — hai class khác nhau vì mục đích dùng khác nhau.
5. **Tính χ² dùng chung** cho cả giai đoạn pruning và classification — gọi một hàm `computeChiSquare` duy nhất.

---

## 10. Tham Số Cấu Hình

| Tham số | Mặc định (code) | Mặc định bài báo | Ý nghĩa |
|---------|:--------------:|:----------------:|--------|
| `minSupport` | 50 (tuyệt đối) | 1% (tương đối) | Ngưỡng support tối thiểu |
| `minConfidence` | 0.5 | 0.5 | Ngưỡng confidence tối thiểu |
| `chiSquareThreshold` | 3.841 | 3.841 | Ngưỡng χ² (α=0.05, df=1) |
| `coverageDelta` (δ) | 4 | 4 | Số lần cover tối đa cho mỗi bản ghi huấn luyện |
| `trainRatio` | 0.8 | — (dùng CV) | Tỉ lệ split train/test |
| `randomSeed` | 42 | — | Seed cho reproducibility |

### Trade-off giữa các tham số

| Tham số | Tăng → | Giảm → |
|---------|--------|--------|
| `minSupport` | Ít pattern/rule hơn; nhanh hơn; **có thể mất luật hiếm quan trọng** | Nhiều pattern; chậm hơn; **bùng nổ tổ hợp** |
| `minConfidence` | Luật đáng tin hơn; **ít luật** | Nhiều luật nhưng có thể không chính xác |
| `χ²_thr` | Chặt hơn về ý nghĩa thống kê; **ít luật** | Lỏng hơn; dễ giữ luật ngẫu nhiên |
| `δ` | Giữ nhiều luật cho phân lớp; **chính xác hơn nhưng tốn bộ nhớ** | Ít luật; dễ thiếu luật khi classify |

---

## 11. Phân Tích Độ Phức Tạp

### 11.1 Thời gian

| Bước | Độ phức tạp |
|------|-------------|
| Load + count items | $O(|T| \cdot \bar{L})$ với $\bar{L}$ = độ dài trung bình transaction |
| Xây FP-tree khởi tạo | $O(|T| \cdot \bar{L} \cdot \log F)$ với $F$ = #frequent items |
| FP-Growth đệ quy | $O(N_{pat})$ trong đó $N_{pat}$ = số frequent pattern (có thể bùng nổ) |
| Sinh CAR trực tiếp | $O(N_{pat} \cdot |\mathcal{C}|)$ |
| Pruning 1 | $O(N_R^2)$ trong trường hợp xấu |
| Pruning 2 | $O(N_R)$ — mỗi luật 1 phép tính χ² |
| Pruning 3 | $O(N_R \cdot |T|)$ — với mỗi luật duyệt toàn bộ data chưa bị xoá |
| Xây CR-tree | $O(N_R \cdot \bar{k})$ với $\bar{k}$ = kích thước trung bình condset |
| Classify 1 record | $O(|t| \cdot \bar{k})$ (DFS CR-tree) + $O(M)$ (χ² của $M$ luật khớp) |

### 11.2 Không gian

- **FP-tree**: tối đa $O(|T| \cdot \bar{L})$ node
- **CR-tree**: tối đa $O(\sum_R |\text{condset}(R)|)$ node — thường tiết kiệm **50–60%** so với lưu phẳng (Table 4 bài báo)
- **Danh sách luật**: $O(N_R)$

### 11.3 So với CBA

Table 5 bài báo cho thấy CMAR **nhanh hơn CBA** nhiều dataset lớn (Sonar: 226s → 19s). Lý do:
- FP-Growth nhanh hơn Apriori (không sinh ứng viên)
- CR-tree cắt tỉa ngay lúc chèn, không duyệt phẳng mọi luật
- Classify dùng CR-tree → $O(|t|)$ thay vì $O(N_R)$

---

## 12. Ví Dụ Minh Hoạ Từng Bước

### 12.1 Tập training 4 bản ghi, 4 thuộc tính

Theo Table 1 bài báo:

| Row | A | B | C | D | Class |
|-----|---|---|---|---|-------|
| 1 | a₁ | b₁ | c₁ | d₁ | A |
| 2 | a₁ | b₂ | c₁ | d₂ | B |
| 3 | a₁ | b₂ | c₁ | d₃ | C |
| 4 | a₁ | b₂ | — | d₃ | A |

### 12.2 Bước đếm item (minSup = 2)

| Item | Freq |
|------|-----:|
| a₁ | 4 |
| b₂ | 3 |
| c₁ | 3 |
| d₃ | 2 |

F-list (giảm dần): **a₁ → b₂ → c₁ → d₃**.

### 12.3 Xây FP-tree khởi tạo

```
root
 └── a₁ : 4  {A:2, B:1, C:1}
      ├── b₁ : 1  {A:1}
      │    └── c₁ : 1  {A:1}
      │         └── d₁ : 1  {A:1}
      └── b₂ : 3  {A:1, B:1, C:1}
           ├── c₁ : 2  {B:1, C:1}
           │    ├── d₂ : 1  {B:1}
           │    └── d₃ : 1  {C:1}
           └── d₃ : 1  {A:1}
```

### 12.4 Đào mỏ item d₃

- `itemSup = 2`, `classDist = {C:1, A:1}`
- Với class C: `sup=1 < minSup=2` → **bỏ**
- Với class A: `sup=1 < minSup=2` → **bỏ**

Xây conditional base cho d₃:
- Từ node d₃ (dưới c₁): path = [a₁, b₂, c₁], count=1, classDist={C:1}
- Từ node d₃ (dưới b₂): path = [a₁, b₂], count=1, classDist={A:1}

Tần số trong conditional base: `a₁:2, b₂:2, c₁:1`. Lọc với minSup=2 → giữ `a₁, b₂`.

Đệ quy mine với prefix = {d₃}, sẽ ra pattern **{a₁, b₂, d₃}** với itemSup=2.
Phân bố class: {A:1, C:1}. Vì minSup=2 → cả 2 class đều bỏ.

Nhưng nếu hạ minSup=1 thì sẽ ra **luật {a₁, b₂, d₃} ⇒ C** với `sup=1, conf=1/2=0.5`.

### 12.5 Ví dụ tính χ² (giả định n=500)

Với luật `job=no ⇒ rejected` (Example 3 bài báo):

|              | rejected | approved | tổng |
|--------------|:--------:|:--------:|:----:|
| job=no       | 18 | 12 | 30 |
| job=yes      | 32 | 438 | 470 |
| **tổng** | 50 | 450 | 500 |

$$
\chi^2 = \frac{500 \cdot (18 \cdot 438 - 12 \cdot 32)^2}{30 \cdot 470 \cdot 50 \cdot 450} = \frac{500 \cdot (7884 - 384)^2}{317{,}250{,}000} = \frac{500 \cdot 56{,}250{,}000}{317{,}250{,}000} \approx 88.6
$$

Vượt xa ngưỡng 3.841 → luật có **ý nghĩa thống kê** ✓
Tương quan dương: $ad - bc = 7884 - 384 = 7500 > 0$ ✓

### 12.6 Phân lớp: ví dụ weighted χ²

Giả sử record test khớp 3 luật:
- R₁: → class A, χ²=10, maxχ²=25
- R₂: → class A, χ²=15, maxχ²=30
- R₃: → class B, χ²=20, maxχ²=22

Score:
$$
\text{score}(A) = \frac{10^2}{25} + \frac{15^2}{30} = 4 + 7.5 = 11.5
$$
$$
\text{score}(B) = \frac{20^2}{22} \approx 18.18
$$

→ **Dự đoán class B** (mặc dù A có nhiều luật hơn).

Điều này minh hoạ sức mạnh của CMAR: **không đếm số luật, mà đo độ mạnh tổng hợp**.

---

## 13. Đánh Giá & So Sánh Với Các Thuật Toán Khác

### 13.1 Kết quả bài báo (Table 3 — 26 UCI datasets)

| Phương pháp | Accuracy trung bình |
|-------------|:-------------------:|
| C4.5 | 84.09% |
| CBA | 84.69% |
| **CMAR** | **85.22%** |

CMAR đạt accuracy tốt nhất trên **13/26 datasets (50%)**.

### 13.2 Ưu điểm

✅ **Độ chính xác cao** — dùng nhiều luật thay vì 1 → giảm bias, giảm overfitting.
✅ **Hiệu quả bộ nhớ** — CR-tree tiết kiệm **77.12%** bộ nhớ so với CBA (Table 4 bài báo).
✅ **Hiệu suất tốt** — FP-Growth nhanh hơn Apriori; pruning sớm giảm tải.
✅ **Xử lý tốt class imbalance** — weighted χ² normalize bằng maxχ² → công bằng với minority class.

### 13.3 Nhược điểm

❌ **Tham số nhạy** — minSup thấp → bùng nổ pattern; cao → mất luật. `δ` và χ²_thr cũng cần tuning.
❌ **Chỉ xử lý thuộc tính rời rạc** — với thuộc tính liên tục cần discretize trước.
❌ **Interpretability giảm** — không có 1 luật "rõ ràng" cho mỗi dự đoán; thay vào đó là tổng hợp nhiều luật.

### 13.4 So sánh triển khai với bài báo (tham khảo)

Cài đặt Java trong thư mục `src/` triển khai **đầy đủ cả 3 tầng pruning** và **CR-tree**, **khớp hoàn toàn** với mô tả Section 3–4 bài báo. Các thành phần cụ thể:

| Thành phần | Bài báo | Cài đặt | Trạng thái |
|------------|---------|---------|:----------:|
| FP-Growth nhận diện class | §3.1 | `FPGrowth.java` + `FPTree.java` + `FPNode.classCount` | ✅ |
| Sinh CAR trực tiếp | §3.1 | `FPGrowth.mineTree()` | ✅ |
| Rule precedence | §3.3 | `AssociationRule.compareTo()` | ✅ |
| Pruning: general vs specific | §3.3 | `pruneByGeneralRules()` | ✅ |
| Pruning: χ² + tương quan dương | §3.3 | `pruneByChiSquareSignificance()` | ✅ |
| Pruning: Database coverage (Alg. 1) | §3.3 | `pruneByDatabaseCoverage()` | ✅ |
| CR-tree lưu luật | §3.2 | `CRTree.java` | ✅ |
| Weighted χ² classification | §4 | `CMARClassifier.classify()` | ✅ |
| Default class | §4 | `defaultClass` | ✅ |

---

## 14. Kết Luận

CMAR là một thuật toán **associative classification** tinh gọn, kết hợp 3 ý tưởng lớn:

1. **FP-Growth được mở rộng với phân bố lớp tại mỗi node** → sinh Class Association Rule **trực tiếp** trong lúc đào mỏ, không cần pass thứ hai.
2. **CR-tree** (prefix-tree nén) làm cấu trúc lưu luật chia sẻ tiền tố → tiết kiệm bộ nhớ và hỗ trợ truy vấn khớp subset nhanh.
3. **Weighted χ²** cho phép phân lớp dựa trên **đồng thuận của nhiều luật**, loại bỏ điểm yếu của các phương pháp single-rule như CBA.

Ba tầng cắt tỉa (general-rule → chi-square → database-coverage) loại bỏ đến **99%+** luật dư thừa, biến tập ứng viên hàng trăm nghìn xuống còn vài trăm luật chất lượng cao — đủ để đạt độ chính xác cạnh tranh hoặc vượt trội C4.5, CBA trên nhiều benchmark UCI.

Toàn bộ pipeline thuật toán được cài đặt trong [`src/`](../src/) (Java thuần), tuân thủ sát đặc tả bài báo gốc: từ định dạng luật, thứ tự ưu tiên, cho đến công thức χ² và weighted χ².

---

## Phụ Lục A — Lệnh Chạy Chương Trình

```bash
# Compile
cd d:\CMAR
mkdir out 2>nul
javac -d out src\*.java

# Chạy với dataset mặc định (car.csv, minSup=50, minConf=0.5)
java -cp out Main

# Chạy với tham số tuỳ chỉnh
java -cp out Main data/zoo_h.csv 3 0.5 3.841 4
#                ^^^^^^^^^^^^^^^^ ^  ^^^  ^^^^^ ^
#                dataset   minSup conf χ²thr δ

# Cross-validation
java -cp out Benchmark
```

**Output files:**
- `result/frequent_patterns.txt` — tất cả frequent patterns
- `result/association_rules.txt` — CAR ứng viên
- `result/cmar_result.txt` — kết quả cuối (luật sống sót + dự đoán + accuracy)
- `result/evaluation.txt` — precision/recall/F1 theo class
- `result/predictions.txt` — dự đoán chi tiết từng test record
- `report/fp_tree_report.md` — báo cáo chi tiết FP-tree + header table

---

## Phụ Lục B — Tài Liệu Tham Khảo

1. Li, W., Han, J., & Pei, J. (2001). **CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules**. *Proceedings of the 2001 IEEE International Conference on Data Mining (ICDM'01)*, pp. 369–376.
2. Han, J., Pei, J., & Yin, Y. (2000). **Mining Frequent Patterns without Candidate Generation**. *SIGMOD 2000*.
3. Liu, B., Hsu, W., & Ma, Y. (1998). **Integrating Classification and Association Rule Mining** (CBA). *KDD'98*.
4. Agrawal, R., & Srikant, R. (1994). **Fast Algorithms for Mining Association Rules** (Apriori). *VLDB'94*.
5. Quinlan, J. R. (1993). **C4.5: Programs for Machine Learning**. Morgan Kaufmann.

---

*Báo cáo này tổng hợp đầy đủ thuật toán CMAR: công thức, pseudo-code, cấu trúc dữ liệu, luồng hoạt động, và ánh xạ sang cài đặt Java thực tế trong thư mục `src/`.*
