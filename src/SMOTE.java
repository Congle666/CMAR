import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * SMOTE-N — Synthetic Minority Oversampling Technique cho dữ liệu Categorical.
 *
 * <p>Phiên bản categorical của SMOTE (Chawla et al. 2002). Thay vì interpolate
 * giá trị số học, SMOTE-N tạo synthetic record bằng cách:</p>
 * <ol>
 *   <li>Tìm k-nearest neighbors trong cùng class (Hamming distance).</li>
 *   <li>Với mỗi attribute: lấy <b>mode</b> giá trị từ record + neighbors.</li>
 *   <li>Tạo Transaction synthetic với các giá trị đó.</li>
 * </ol>
 *
 * <p>Mục đích: <b>balance class distribution</b> trước khi mining → minority
 * class có đủ records để sinh CAR có support đủ cao.</p>
 *
 * <p>Reference: Chawla N. V., Bowyer K. W., Hall L. O., Kegelmeyer W. P. (2002),
 * "SMOTE: Synthetic Minority Over-sampling Technique", JAIR vol 16, 321-357.</p>
 */
public final class SMOTE {

    private SMOTE() {}  // static-only

    /**
     * Áp dụng SMOTE-N: balance minority classes lên target size.
     *
     * @param data         training transactions (gồm tất cả classes)
     * @param k            số nearest neighbors (mặc định 5)
     * @param targetRatio  mỗi class hướng tới có ít nhất {@code targetRatio × maxFreq} records.
     *                     1.0 = fully balanced. 0.5 = minority đạt 50% majority size.
     * @param seed         random seed cho reproducibility
     * @return augmented training set (records gốc + synthetic)
     */
    public static List<Transaction> apply(List<Transaction> data, int k,
                                           double targetRatio, long seed) {
        if (data.isEmpty()) return new ArrayList<>(data);

        // Group records by class
        Map<String, List<Transaction>> byClass = new HashMap<>();
        for (Transaction t : data) {
            byClass.computeIfAbsent(t.getClassLabel(), c -> new ArrayList<>()).add(t);
        }

        // Tìm majority class size
        int maxFreq = 0;
        for (List<Transaction> g : byClass.values()) {
            if (g.size() > maxFreq) maxFreq = g.size();
        }

        int target = (int) Math.round(maxFreq * targetRatio);
        Random rng = new Random(seed);

        // Kết quả: tất cả records gốc + synthetic
        List<Transaction> augmented = new ArrayList<>(data);

        for (Map.Entry<String, List<Transaction>> e : byClass.entrySet()) {
            String cls = e.getKey();
            List<Transaction> records = e.getValue();

            if (records.size() >= target) continue;  // class đã đủ

            int needed = target - records.size();
            // Edge case: class quá ít records (< 2) → không thể SMOTE
            if (records.size() < 2) {
                // Duplicate đơn giản (no synthesis possible)
                for (int i = 0; i < needed; i++) {
                    augmented.add(records.get(i % records.size()));
                }
                continue;
            }

            int kEff = Math.min(k, records.size() - 1);

            for (int i = 0; i < needed; i++) {
                Transaction base = records.get(rng.nextInt(records.size()));
                List<Transaction> neighbors = kNearestNeighbors(base, records, kEff);
                Transaction synthetic = createSynthetic(base, neighbors, cls, rng);
                augmented.add(synthetic);
            }
        }

        return augmented;
    }

    /**
     * Convenience: targetRatio = 1.0 (fully balanced), k = 5, seed = 42.
     */
    public static List<Transaction> apply(List<Transaction> data) {
        return apply(data, 5, 1.0, 42);
    }

    // -----------------------------------------------------------------------

    /**
     * k-nearest neighbors trong cùng class, dùng Hamming distance trên items.
     */
    private static List<Transaction> kNearestNeighbors(
            Transaction base, List<Transaction> pool, int k) {
        // Tính Hamming distance từ base đến mỗi candidate
        List<Map.Entry<Transaction, Integer>> distances = new ArrayList<>();
        Set<String> baseItems = new HashSet<>(base.getItems());

        for (Transaction t : pool) {
            if (t == base) continue;
            int d = hammingDistance(baseItems, t.getItems());
            distances.add(new java.util.AbstractMap.SimpleEntry<>(t, d));
        }

        distances.sort(Comparator.comparingInt(Map.Entry::getValue));

        List<Transaction> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, distances.size()); i++) {
            result.add(distances.get(i).getKey());
        }
        return result;
    }

    /**
     * Hamming distance: số items khác nhau giữa 2 sets.
     * Items có dạng "attr=value" → compare per-attribute.
     */
    private static int hammingDistance(Set<String> baseItems, List<String> other) {
        // Build attr→value map cho cả 2 records
        Map<String, String> baseMap = itemsToMap(baseItems);
        Map<String, String> otherMap = itemsToMap(other);

        Set<String> allAttrs = new HashSet<>(baseMap.keySet());
        allAttrs.addAll(otherMap.keySet());

        int diff = 0;
        for (String attr : allAttrs) {
            String bv = baseMap.get(attr);
            String ov = otherMap.get(attr);
            if (bv == null || ov == null || !bv.equals(ov)) {
                diff++;
            }
        }
        return diff;
    }

    /** Convert ["attr=val", ...] thành map attr→val. */
    private static Map<String, String> itemsToMap(Iterable<String> items) {
        Map<String, String> m = new HashMap<>();
        for (String item : items) {
            int idx = item.indexOf('=');
            if (idx > 0) {
                m.put(item.substring(0, idx), item.substring(idx + 1));
            }
        }
        return m;
    }

    /**
     * Tạo synthetic record từ base + neighbors:
     * Với mỗi attribute, lấy mode (giá trị xuất hiện nhiều nhất) trong base + neighbors.
     * Nếu tie, chọn ngẫu nhiên.
     */
    private static Transaction createSynthetic(Transaction base,
                                                 List<Transaction> neighbors,
                                                 String classLabel,
                                                 Random rng) {
        // Gom tất cả attributes
        Map<String, Map<String, Integer>> attrValueCounts = new HashMap<>();

        // Helper: add a record's items to counts
        addItemsToCount(base.getItems(), attrValueCounts);
        for (Transaction n : neighbors) {
            addItemsToCount(n.getItems(), attrValueCounts);
        }

        // Với mỗi attribute, pick mode value
        List<String> syntheticItems = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> e : attrValueCounts.entrySet()) {
            String attr = e.getKey();
            Map<String, Integer> valueCounts = e.getValue();

            // Tìm max count
            int maxCount = 0;
            for (int c : valueCounts.values()) {
                if (c > maxCount) maxCount = c;
            }

            // Collect modes (handle ties)
            List<String> modes = new ArrayList<>();
            for (Map.Entry<String, Integer> ve : valueCounts.entrySet()) {
                if (ve.getValue() == maxCount) modes.add(ve.getKey());
            }

            // Random pick if tie
            String chosen = modes.get(rng.nextInt(modes.size()));
            syntheticItems.add(attr + "=" + chosen);
        }

        return new Transaction(syntheticItems, classLabel);
    }

    private static void addItemsToCount(List<String> items,
                                          Map<String, Map<String, Integer>> counts) {
        for (String item : items) {
            int idx = item.indexOf('=');
            if (idx <= 0) continue;
            String attr = item.substring(0, idx);
            String value = item.substring(idx + 1);
            counts.computeIfAbsent(attr, a -> new HashMap<>())
                  .merge(value, 1, Integer::sum);
        }
    }

    /** Tóm tắt class distribution trước vs sau SMOTE (dùng cho debugging). */
    public static String summarize(List<Transaction> before, List<Transaction> after) {
        Map<String, Integer> beforeCount = countByClass(before);
        Map<String, Integer> afterCount = countByClass(after);

        StringBuilder sb = new StringBuilder("SMOTE summary:\n");
        for (String cls : new java.util.TreeSet<>(beforeCount.keySet())) {
            sb.append(String.format("  %-20s %4d -> %4d (+%d synthetic)%n",
                cls, beforeCount.get(cls),
                afterCount.getOrDefault(cls, 0),
                afterCount.getOrDefault(cls, 0) - beforeCount.get(cls)));
        }
        return sb.toString();
    }

    private static Map<String, Integer> countByClass(List<Transaction> data) {
        Map<String, Integer> c = new HashMap<>();
        for (Transaction t : data) c.merge(t.getClassLabel(), 1, Integer::sum);
        return c;
    }
}
