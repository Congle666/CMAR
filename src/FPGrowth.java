import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thành phần khai thác luật của CMAR — Li, Han & Pei (2001) §3.2.
 *
 * Khác với FP-Growth thuần (coi item lớp như item bình thường và cần
 * một pass hậu kỳ để trích luật), bộ miner này làm việc trên CR-tree
 * có nhận thức về phân phối lớp: mỗi nút lưu số đếm theo từng lớp,
 * và các Class Association Rule được sinh trực tiếp trong lúc đệ quy
 * mà không cần đến thủ thuật chèn item "class=...".
 *
 * Pipeline:
 *   1. Đếm tần suất item và tần suất lớp; loại bỏ item dưới ngưỡng minSupport.
 *   2. Xây CR-tree ban đầu (mỗi path mang theo lớp của transaction sinh ra nó).
 *   3. Mining đệ quy: với mỗi frequent pattern P, lấy trực tiếp support theo
 *      lớp của P từ classCount tích luỹ dọc chuỗi header-table, rồi sinh một
 *      CAR cho mỗi lớp c có supCount(P, c) ≥ minSupport và confidence ≥ minConfidence.
 */
public class FPGrowth {

    private final int minSupport;
    private int maxPatternLength = Integer.MAX_VALUE;

    /**
     * Ngưỡng minSup theo từng lớp (Hướng 2). Khi có, bước sinh luật dùng
     * {@code classMinSupMap.getOrDefault(cls, minSupport)} thay cho
     * minSupport toàn cục. Null = hành vi baseline.
     */
    private Map<String, Integer> classMinSupMap;

    /**
     * Ngưỡng minConfidence theo từng lớp (Hướng 3 — Adaptive minConf).
     * Khi có, bước sinh luật dùng {@code classMinConfMap.getOrDefault(cls, minConfidence)}
     * thay cho minConfidence toàn cục. Null = hành vi baseline.
     *
     * <p>Mục đích: class thiểu số có confidence tối đa bị giới hạn bởi
     * (freq(c) / itemSup) nên ngưỡng 0.5 toàn cục là quá chặt. Adaptive
     * minConf nới lỏng cho class hiếm, giữ nguyên cho class đa số.</p>
     */
    private Map<String, Double> classMinConfMap;

    /**
     * Attribute weights cho Weighted Support (WCBA 2018).
     * Khi có, rule emission lưu weightedSupport = avg(item_weights) × support.
     * HM trong rule sẽ được tính từ weightedSupport thay vì support.
     * Null = không dùng weighted support (hành vi baseline).
     */
    private AttributeWeights attributeWeights;

    private final List<FrequentPattern> patterns = new ArrayList<>();
    private final List<AssociationRule> rules    = new ArrayList<>();

    private FPTree initialTree;

    // Cache trong mine() để mineTree() không phải nhận tham số dài dòng.
    private double minConfidence;
    private int    totalTransactions;
    
    // Hướng 2: lưu classMinSupMap để áp dụng xuyên suốt mining (không chỉ sinh luật)
    private Map<String, Integer> cachedClassMinSupMap;

    public FPGrowth(int minSupport) {
        this.minSupport = minSupport;
    }

    /** Đặt độ dài tối đa của pattern, giới hạn bộ nhớ trên dữ liệu nhiều chiều. */
    public void setMaxPatternLength(int maxLen) {
        this.maxPatternLength = maxLen;
    }

    /**
     * Đặt ngưỡng minSup theo từng lớp. Khi có, việc sinh luật sẽ kiểm tra
     * {@code classSup >= classMinSupMap.getOrDefault(cls, minSupport)}
     * thay cho minSupport toàn cục. Cho phép sinh luật cho các lớp thiểu số
     * mà support tuyệt đối không thể đạt ngưỡng toàn cục.
     *
     * <p>Truyền null (mặc định) để dùng ngưỡng toàn cục (hành vi baseline).</p>
     */
    public void setClassMinSupMap(Map<String, Integer> classMinSupMap) {
        this.classMinSupMap = classMinSupMap;
    }

    /** Trả về ngưỡng dùng để sinh luật cho lớp c (fallback về giá trị toàn cục). */
    private int classThreshold(String cls) {
        if (classMinSupMap == null) return minSupport;
        return classMinSupMap.getOrDefault(cls, minSupport);
    }

    /**
     * Đặt ngưỡng minConfidence theo từng lớp (Hướng 3). Khi có, bước sinh luật
     * kiểm tra {@code conf >= classMinConfMap.getOrDefault(cls, minConfidence)}.
     * Cho phép class thiểu số có ngưỡng confidence thấp hơn (vì confidence tối
     * đa của rule cho class hiếm bị giới hạn bởi tỉ lệ freq(c)/sup(P)).
     *
     * <p>Truyền null (mặc định) = dùng ngưỡng toàn cục (baseline).</p>
     */
    public void setClassMinConfMap(Map<String, Double> classMinConfMap) {
        this.classMinConfMap = classMinConfMap;
    }

    /** Trả về minConfidence cho lớp c (fallback về giá trị toàn cục). */
    private double classMinConfidence(String cls) {
        if (classMinConfMap == null) return minConfidence;
        return classMinConfMap.getOrDefault(cls, minConfidence);
    }

    /**
     * Bật chế độ Weighted Support theo attribute weights (WCBA 2018).
     * Mỗi rule sẽ lưu weightedSupport = avg(item_weights) × support, và
     * HM của rule được tính từ weightedSupport.
     */
    public void setAttributeWeights(AttributeWeights w) {
        this.attributeWeights = w;
    }

    /** CR-tree ban đầu được xây trong lần mine() gần nhất. */
    public FPTree getInitialTree() {
        return initialTree;
    }

    /** Các frequent pattern tìm được trong lần mine() gần nhất (dùng cho báo cáo). */
    public List<FrequentPattern> getPatterns() {
        return patterns;
    }

    /** Các CAR sinh trực tiếp trong lần mine() gần nhất. */
    public List<AssociationRule> getRules() {
        return rules;
    }
    
    /** classMinSupMap được sử dụng trong lần mine() gần nhất (dùng cho báo cáo Hướng 2). */
    public Map<String, Integer> getCachedClassMinSupMap() {
        return cachedClassMinSupMap;
    }

    /**
     * Khai thác Class Association Rule từ các transaction huấn luyện.
     *
     * @param trainData     các transaction huấn luyện (item + nhãn lớp)
     * @param minConfidence ngưỡng confidence tối thiểu của luật
     * @return danh sách CAR (đã sắp theo thứ tự ưu tiên)
     */
    public List<AssociationRule> mine(List<Transaction> trainData,
                                      double minConfidence) {
        patterns.clear();
        rules.clear();
        this.minConfidence     = minConfidence;
        this.totalTransactions = trainData.size();
        // Giữ cachedClassMinSupMap từ lần setClassMinSupMap() gần nhất
        // (nếu chưa set thì là null — dùng minSupport toàn cục)

        // --- Bước 1: tần suất item toàn cục (chỉ thuộc tính, không tính class) ---
        Map<String, Integer> freq = new HashMap<>();
        for (Transaction t : trainData) {
            for (String item : t.getItems()) {
                freq.merge(item, 1, Integer::sum);
            }
        }
        freq.entrySet().removeIf(e -> e.getValue() < minSupport);
        if (freq.isEmpty()) {
            this.initialTree = new FPTree(minSupport);
            return rules;
        }

        // --- Bước 2: xây CR-tree ban đầu ---
        FPTree tree = new FPTree(minSupport);
        tree.headerFreq.putAll(freq);

        for (Transaction t : trainData) {
            List<String> path = t.getItems().stream()
                .filter(freq::containsKey)
                .sorted((a, b) -> {
                    int c = freq.get(b) - freq.get(a);
                    return c != 0 ? c : a.compareTo(b);
                })
                .collect(Collectors.toList());

            if (!path.isEmpty()) {
                Map<String, Integer> classDist = new HashMap<>();
                classDist.put(t.getClassLabel(), 1);
                tree.insertPath(path, classDist, 1);
            }
        }
        this.initialTree = tree;

        // --- Bước 3: khai thác đệ quy ---
        mineTree(tree, new ArrayList<>());

        Collections.sort(rules);  // thứ tự ưu tiên luật theo CMAR
        return rules;
    }

    // -----------------------------------------------------------------------
    // Khai thác đệ quy — có nhận thức về lớp
    // -----------------------------------------------------------------------

    private void mineTree(FPTree tree, List<String> prefix) {
        if (prefix.size() >= maxPatternLength) return;

        for (String item : tree.getItemsSortedByFreqAsc()) {
            int itemSupport = tree.headerFreq.get(item);

            // Pattern P = prefix ∪ {item}
            List<String> newPattern = new ArrayList<>(prefix);
            newPattern.add(item);
            HashSet<String> patternSet = new HashSet<>(newPattern);
            patterns.add(new FrequentPattern(patternSet, itemSupport));

            // --- Gom phân phối lớp của P qua chuỗi header ---
            Map<String, Integer> classDistForP = new HashMap<>();
            FPNode node = tree.headerFirst.get(item);
            while (node != null) {
                for (Map.Entry<String, Integer> e : node.classCount.entrySet()) {
                    classDistForP.merge(e.getKey(), e.getValue(), Integer::sum);
                }
                node = node.nodeLink;
            }

            // --- Sinh một CAR cho mỗi lớp đạt ngưỡng và đạt minConfidence ---
            // Hướng 2: dùng ngưỡng minSup theo từng lớp nếu classMinSupMap đã được đặt.
            // Hướng 3: dùng ngưỡng minConf theo từng lớp nếu classMinConfMap đã được đặt.
            for (Map.Entry<String, Integer> e : classDistForP.entrySet()) {
                String cls       = e.getKey();
                int    classSup  = e.getValue();
                if (classSup < classThreshold(cls)) continue;

                double confidence = (double) classSup / itemSupport;
                if (confidence < classMinConfidence(cls)) continue;

                double support = (double) classSup / totalTransactions;

                // WCBA: weightedSupport = avg(attribute weights) × support
                double weightedSup;
                if (attributeWeights != null) {
                    double wAvg = attributeWeights.weightOfCondset(patternSet);
                    weightedSup = wAvg * support;
                } else {
                    weightedSup = support;
                }

                rules.add(new AssociationRule(
                    new HashSet<>(patternSet),
                    cls,
                    support, confidence,
                    classSup, itemSupport,
                    weightedSup
                ));
            }

            // --- Xây conditional pattern base kèm phân phối lớp ---
            List<List<String>>            condBase       = new ArrayList<>();
            List<Integer>                 condCounts     = new ArrayList<>();
            List<Map<String, Integer>>    condClassDists = new ArrayList<>();

            node = tree.headerFirst.get(item);
            while (node != null) {
                List<String> prefixPath = new ArrayList<>();
                FPNode ancestor = node.parent;
                while (!ancestor.isRoot()) {
                    prefixPath.add(0, ancestor.item);
                    ancestor = ancestor.parent;
                }
                if (!prefixPath.isEmpty()) {
                    condBase.add(prefixPath);
                    condCounts.add(node.count);
                    // Copy classCount để các thao tác sau này không rò rỉ ngược về nút gốc
                    condClassDists.add(new HashMap<>(node.classCount));
                }
                node = node.nodeLink;
            }

            if (condBase.isEmpty()) continue;

            // --- Tần suất bên trong conditional base ---
            Map<String, Integer> condFreq = new HashMap<>();
            for (int i = 0; i < condBase.size(); i++) {
                int cnt = condCounts.get(i);
                for (String condItem : condBase.get(i)) {
                    condFreq.merge(condItem, cnt, Integer::sum);
                }
            }
            
            // --- Hướng 2: Nếu có classMinSupMap → áp dụng class-specific minSup cho filtering ---
            // Thay vì dùng global minSupport, tính min của class-specific ngưỡng
            final int effectiveMinSup = (cachedClassMinSupMap != null && !cachedClassMinSupMap.isEmpty())
                ? cachedClassMinSupMap.values().stream()
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElse(minSupport)
                : minSupport;
            
            condFreq.entrySet().removeIf(e -> e.getValue() < effectiveMinSup);
            if (condFreq.isEmpty()) continue;

            // --- Xây CR-tree điều kiện, lan truyền phân phối lớp ---
            FPTree condTree = new FPTree(minSupport);
            condTree.headerFreq.putAll(condFreq);

            for (int i = 0; i < condBase.size(); i++) {
                List<String> sortedPath = condBase.get(i).stream()
                    .filter(condFreq::containsKey)
                    .sorted((a, b) -> {
                        int c = condFreq.get(b) - condFreq.get(a);
                        return c != 0 ? c : a.compareTo(b);
                    })
                    .collect(Collectors.toList());

                if (!sortedPath.isEmpty()) {
                    condTree.insertPath(
                        sortedPath,
                        condClassDists.get(i),
                        condCounts.get(i));
                }
            }

            // --- Đệ quy với prefix đã mở rộng ---
            mineTree(condTree, newPattern);
        }
    }
}
