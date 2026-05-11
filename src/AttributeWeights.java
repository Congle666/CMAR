import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WCBA — Automatic Attribute Weighting via Information Gain.
 *
 * <p>WCBA 2018 yêu cầu trọng số attribute do chuyên gia gán (1-10).
 * Để generalize cho mọi dataset (không cần chuyên gia), ta tự động
 * compute weights bằng <b>Information Gain (IG)</b> — đo "mức độ phân
 * biệt lớp" của từng thuộc tính.</p>
 *
 * <p>Công thức:</p>
 * <pre>
 *   IG(A) = H(Class) - H(Class | A)
 *
 *   H(Class)     = -Σ p(c) log₂ p(c)
 *   H(Class | A) = Σ p(a) × H(Class | A=a)
 *                = Σ p(a) × [-Σ p(c|a) log₂ p(c|a)]
 * </pre>
 *
 * <p>Sau khi tính IG cho mọi attribute, scale về [1.0, 10.0] (giống
 * WCBA paper: high=8-10, medium=4-7, low=1-3):</p>
 * <pre>
 *   weight(A) = 1 + 9 × (IG(A) - min_IG) / (max_IG - min_IG)
 * </pre>
 *
 * <p>Items có cùng attribute thì cùng weight. weight của 1 condset =
 * trung bình weight của các items.</p>
 */
public final class AttributeWeights {

    private static final double LOG2 = Math.log(2.0);

    /** Map từ tên item (attr=val) → trọng số attribute đó. */
    private final Map<String, Double> itemWeight;

    /** Map từ tên attribute (vd "odor") → trọng số. */
    private final Map<String, Double> attrWeight;

    private AttributeWeights(Map<String, Double> attrWeight,
                              Map<String, Double> itemWeight) {
        this.attrWeight = attrWeight;
        this.itemWeight = itemWeight;
    }

    /** Trọng số của 1 attribute (tên), fallback 1.0 nếu không có. */
    public double weightOfAttribute(String attr) {
        return attrWeight.getOrDefault(attr, 1.0);
    }

    /** Trọng số của 1 item (attr=val), fallback 1.0 nếu không có. */
    public double weightOfItem(String item) {
        return itemWeight.getOrDefault(item, 1.0);
    }

    /**
     * Trung bình trọng số của các items trong condset.
     * Dùng để tính weighted_support cho rule.
     */
    public double weightOfCondset(Set<String> condset) {
        if (condset.isEmpty()) return 1.0;
        double sum = 0;
        int count = 0;
        for (String item : condset) {
            sum += weightOfItem(item);
            count++;
        }
        return sum / count;
    }

    public Map<String, Double> getAttributeWeights() { return attrWeight; }
    public Map<String, Double> getItemWeights() { return itemWeight; }

    // -----------------------------------------------------------------------
    // Builder — compute từ training data
    // -----------------------------------------------------------------------

    /**
     * Tính Information Gain cho mọi attribute và scale weight về [1, 10].
     *
     * @param trainData training transactions với class labels
     * @return AttributeWeights ready to use
     */
    public static AttributeWeights computeFromTrainData(List<Transaction> trainData) {
        if (trainData.isEmpty()) {
            return new AttributeWeights(new HashMap<>(), new HashMap<>());
        }

        int N = trainData.size();

        // --- Bước 1: H(Class) ---
        Map<String, Integer> classCount = new HashMap<>();
        for (Transaction t : trainData) {
            classCount.merge(t.getClassLabel(), 1, Integer::sum);
        }
        double hClass = entropy(classCount, N);

        // --- Bước 2: Gom items theo attribute name ---
        // item format: "attr=value". Tách "attr" từ "attr=value".
        Map<String, Set<String>> attrItems = new HashMap<>();
        for (Transaction t : trainData) {
            for (String item : t.getItems()) {
                String attr = extractAttrName(item);
                attrItems.computeIfAbsent(attr, k -> new HashSet<>()).add(item);
            }
        }

        // --- Bước 3: IG cho từng attribute ---
        Map<String, Double> ig = new HashMap<>();
        for (String attr : attrItems.keySet()) {
            ig.put(attr, informationGain(trainData, attr, hClass, N));
        }

        // --- Bước 4: scale IG về [1.0, 10.0] ---
        double minIG = ig.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxIG = ig.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double range = maxIG - minIG;

        Map<String, Double> attrWeight = new HashMap<>();
        for (Map.Entry<String, Double> e : ig.entrySet()) {
            double w;
            if (range == 0) {
                w = 5.0;  // tất cả IG bằng nhau → weight trung bình
            } else {
                w = 1.0 + 9.0 * (e.getValue() - minIG) / range;
            }
            attrWeight.put(e.getKey(), w);
        }

        // --- Bước 5: propagate weight xuống items ---
        Map<String, Double> itemWeight = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : attrItems.entrySet()) {
            double w = attrWeight.get(e.getKey());
            for (String item : e.getValue()) {
                itemWeight.put(item, w);
            }
        }

        return new AttributeWeights(attrWeight, itemWeight);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Tách "attr" từ item dạng "attr=value".
     * Nếu không có dấu '=', dùng cả item làm attr name.
     */
    private static String extractAttrName(String item) {
        int idx = item.indexOf('=');
        return idx >= 0 ? item.substring(0, idx) : item;
    }

    /**
     * Entropy H = -Σ p(c) log₂ p(c) cho phân phối.
     */
    private static double entropy(Map<String, Integer> counts, int total) {
        if (total == 0) return 0;
        double h = 0;
        for (int c : counts.values()) {
            if (c == 0) continue;
            double p = (double) c / total;
            h -= p * Math.log(p) / LOG2;
        }
        return h;
    }

    /**
     * IG(A) = H(Class) - H(Class | A).
     *
     * <p>H(Class | A) tính bằng cách: với mỗi giá trị v của A, lấy
     * tập transactions có item "A=v", tính entropy class trên tập đó,
     * rồi weighted-average theo |tập| / N.</p>
     *
     * <p>Trans không chứa attribute A (vd missing value): coi như nhóm
     * "absent" riêng.</p>
     */
    private static double informationGain(List<Transaction> data, String attr,
                                           double hClass, int N) {
        // Nhóm transactions theo giá trị của attribute attr
        Map<String, Map<String, Integer>> byVal = new HashMap<>();  // value → classCount
        Map<String, Integer> valTotal = new HashMap<>();

        for (Transaction t : data) {
            String value = findAttrValue(t.getItems(), attr);  // "?" nếu missing
            byVal.computeIfAbsent(value, k -> new HashMap<>())
                 .merge(t.getClassLabel(), 1, Integer::sum);
            valTotal.merge(value, 1, Integer::sum);
        }

        // H(Class | A) = Σ p(v) × H(Class | A=v)
        double hCondA = 0;
        for (Map.Entry<String, Map<String, Integer>> e : byVal.entrySet()) {
            int subTotal = valTotal.get(e.getKey());
            double pV = (double) subTotal / N;
            double hV = entropy(e.getValue(), subTotal);
            hCondA += pV * hV;
        }

        return hClass - hCondA;
    }

    /** Tìm giá trị của attribute attr trong item list. Trả "?" nếu không có. */
    private static String findAttrValue(List<String> items, String attr) {
        String prefix = attr + "=";
        for (String item : items) {
            if (item.startsWith(prefix)) {
                return item.substring(prefix.length());
            }
        }
        return "?";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AttributeWeights{\n");
        attrWeight.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(e -> sb.append(String.format("  %-30s w=%.2f%n",
                e.getKey(), e.getValue())));
        sb.append("}");
        return sb.toString();
    }
}
