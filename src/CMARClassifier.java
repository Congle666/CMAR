import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ phân lớp CMAR — trung thành theo Li, Han & Pei (2001) ICDM.
 *
 * Huấn luyện (train):
 *   Cắt tỉa 1: General rule pruning — loại bỏ luật cụ thể hơn đang bị
 *              thống trị bởi một luật tổng quát hơn có hạng cao hơn và
 *              cùng nhãn lớp.
 *   Cắt tỉa 2: Luật có tương quan dương — chỉ giữ các luật có χ² vượt
 *              ngưỡng ý nghĩa (mặc định 3.841, p=0.05, df=1) VÀ có
 *              đồng xuất hiện quan sát được vượt giá trị kỳ vọng.
 *   Cắt tỉa 3: Database coverage (Thuật toán 1 trong paper) — lặp đi
 *              lặp lại việc chọn luật phủ đúng các bản ghi huấn luyện;
 *              loại bỏ bản ghi khi đã được phủ đủ δ luật.
 *
 * Sau cắt tỉa, các luật còn sống được chèn vào CR-tree (§3.3) để lưu
 * nén gọn và truy vấn subset nhanh khi phân lớp.
 *
 * Phân lớp (classify):
 *   1. Lấy tất cả các luật trong CR-tree khớp với bản ghi test.
 *   2. Nếu không có luật nào khớp → trả về lớp mặc định.
 *   3. Nếu tất cả luật khớp đều cùng một lớp → trả về lớp đó.
 *   4. Ngược lại nhóm theo lớp, score(G) = Σ [χ²(r)]² / maxχ²(r),
 *      trả về lớp có score lớn nhất.
 */
public class CMARClassifier {

    private List<AssociationRule> rules;
    private List<AssociationRule> spareRulesList;  // WCBA: rules không pass coverage nhưng vẫn keep
    private CRTree crTree;
    private CRTree spareCRTree;                      // WCBA: CR-tree riêng cho spare rules
    private String defaultClass;
    private int totalTransactions;
    private Map<String, Integer> classFreq;
    private Map<String, Integer> itemFreq;   // dùng để sắp thứ tự path trong CR-tree

    // --- Tham số cắt tỉa (§5) ---
    private double chiSquareThreshold = 3.841;
    private int    coverageThreshold  = 4;
    private int    topK               = 0;  // Top-K per class (0 = disable)
    private double classificationThreshold = 0.5;  // HƯỚNG 1: Threshold để filter rules
    private boolean useClassWeighting = false;  // HƯỚNG 2: Class weighting for imbalanced data
    private boolean useHMRanking      = false;  // HƯỚNG WCBA: rank Top-K by HM thay vì χ²
    private double minHM              = 0.0;    // HƯỚNG WCBA: filter rules dưới HM threshold
    private boolean useSpareRules     = false;  // WCBA: enable strong + spare 2-stage prediction
    private int    stratifiedKMin     = 0;      // Stratified Top-K min (0 = disable)
    private int    stratifiedKMax     = 0;      // Stratified Top-K max

    // --- Thống kê ---
    private int candidateCount;
    private int afterGeneralPruneCount;
    private int afterChiPruneCount;
    private int afterCoveragePruneCount;

    public void setChiSquareThreshold(double threshold) { this.chiSquareThreshold = threshold; }
    public void setCoverageThreshold(int delta)         { this.coverageThreshold = delta; }
    public void setTopK(int k)                          { this.topK = k; }
    public void setThreshold(double threshold)          { this.classificationThreshold = threshold; }  // HƯỚNG 1
    public void setUseClassWeighting(boolean use)       { this.useClassWeighting = use; }  // HƯỚNG 2
    /** HƯỚNG WCBA: nếu true, sort Top-K theo HM thay vì χ². */
    public void setUseHMRanking(boolean use)            { this.useHMRanking = use; }
    /** HƯỚNG WCBA: filter rules có HM < threshold trước khi Top-K. */
    public void setMinHM(double minHM)                  { this.minHM = minHM; }
    /** WCBA: bật chế độ 2-stage prediction strong → spare → default. */
    public void setUseSpareRules(boolean use)           { this.useSpareRules = use; }
    /**
     * Stratified Top-K: K thích nghi theo class size.
     * Minority class lấy nhiều rules (kMax), majority lấy ít (kMin).
     * Truyền (0, 0) để tắt và quay về Top-K cố định.
     */
    public void setStratifiedTopK(int kMin, int kMax) {
        this.stratifiedKMin = kMin;
        this.stratifiedKMax = kMax;
    }

    // -----------------------------------------------------------------------
    // Huấn luyện
    // -----------------------------------------------------------------------

    public void train(List<AssociationRule> candidateRules,
                      List<Transaction> trainData) {
        this.totalTransactions = trainData.size();
        this.classFreq = new HashMap<>();
        this.itemFreq  = new HashMap<>();

        for (Transaction t : trainData) {
            classFreq.merge(t.getClassLabel(), 1, Integer::sum);
            for (String item : t.getItems()) {
                itemFreq.merge(item, 1, Integer::sum);
            }
        }

        defaultClass = classFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");

        Collections.sort(candidateRules);
        this.candidateCount = candidateRules.size();

        // --- Cắt tỉa 1: luật tổng quát ---
        List<AssociationRule> afterPrune1 = pruneByGeneralRules(candidateRules);
        this.afterGeneralPruneCount = afterPrune1.size();
        System.out.println("    Cat tia 1 (luat tong quat):     "
            + candidateRules.size() + " -> " + afterPrune1.size());

        // --- Cắt tỉa 2: chi-square + tương quan dương ---
        List<AssociationRule> afterPrune2 = pruneByChiSquareSignificance(afterPrune1);
        this.afterChiPruneCount = afterPrune2.size();
        System.out.println("    Cat tia 2 (chi-square >= " + chiSquareThreshold + "): "
            + afterPrune1.size() + " -> " + afterPrune2.size());

        // --- Cắt tỉa 3: database coverage ---
        this.rules = pruneByDatabaseCoverage(afterPrune2, trainData);
        this.afterCoveragePruneCount = this.rules.size();
        System.out.println("    Cat tia 3 (db coverage, delta=" + coverageThreshold + "):  "
            + afterPrune2.size() + " -> " + this.rules.size());

        // --- Top-K selection (tùy chọn) ---
        if (topK > 0) {
            this.rules = selectTopKRulesPerClass(this.rules, topK);
            System.out.println("    Top-K selection (k=" + topK + "): "
                + this.afterCoveragePruneCount + " -> " + this.rules.size());
        }

        // --- Xây CR-tree để lưu luật nén gọn + truy vấn nhanh ---
        this.crTree = new CRTree(itemFreq);
        crTree.insertAll(this.rules);

        // --- WCBA: xây spare CR-tree nếu có spare rules ---
        if (useSpareRules && spareRulesList != null && !spareRulesList.isEmpty()) {
            this.spareCRTree = new CRTree(itemFreq);
            this.spareCRTree.insertAll(this.spareRulesList);
            System.out.println("    Spare rules (WCBA fallback): " + spareRulesList.size());
        }

        System.out.println("    Luat cuoi cung: " + this.rules.size()
            + " (tu " + candidateRules.size() + " ung vien; luu trong CR-tree)");
    }

    // -----------------------------------------------------------------------
    // Cắt tỉa 1: General Rule Pruning
    // -----------------------------------------------------------------------

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
                keptByClass.computeIfAbsent(r1.getClassLabel(), k -> new ArrayList<>())
                           .add(r1);
            }
        }
        return kept;
    }

    // -----------------------------------------------------------------------
    // Cắt tỉa 2: ý nghĩa thống kê chi-square + tương quan dương
    // -----------------------------------------------------------------------

    private List<AssociationRule> pruneByChiSquareSignificance(
            List<AssociationRule> rules) {
        List<AssociationRule> kept = new ArrayList<>();

        for (AssociationRule r : rules) {
            double chi2 = computeChiSquare(r, r.getClassLabel());
            double a = r.getSupportCount();
            double b = r.getCondsetSupportCount() - a;
            double c = classFreq.getOrDefault(r.getClassLabel(), 0) - a;
            double d = totalTransactions - a - b - c;
            boolean positivelyCorrelated = (a * d > b * c);

            if (chi2 >= chiSquareThreshold && positivelyCorrelated) {
                kept.add(r);
            }
        }
        return kept;
    }

    // -----------------------------------------------------------------------
    // Top-K Selection Per Class (Hướng 4)
    // -----------------------------------------------------------------------

    private List<AssociationRule> selectTopKRulesPerClass(
            List<AssociationRule> rules, int k) {
        Map<String, List<AssociationRule>> byClass = new HashMap<>();
        List<AssociationRule> result = new ArrayList<>();

        // Lọc theo minHM trước (HƯỚNG WCBA)
        List<AssociationRule> filtered = rules;
        if (useHMRanking && minHM > 0) {
            filtered = new ArrayList<>();
            for (AssociationRule r : rules) {
                if (r.getHM() >= minHM) filtered.add(r);
            }
            if (filtered.isEmpty()) filtered = rules;
        }

        // Group by class
        for (AssociationRule r : filtered) {
            byClass.computeIfAbsent(r.getClassLabel(), cls -> new ArrayList<>())
                   .add(r);
        }

        // Stratified K: K(c) thích nghi theo class size
        // Minority class lấy K_max, majority class lấy K_min
        boolean useStratified = (stratifiedKMin > 0 && stratifiedKMax > stratifiedKMin);
        int maxFreq = 0;
        int minFreq = Integer.MAX_VALUE;
        if (useStratified && classFreq != null) {
            for (int f : classFreq.values()) {
                if (f > maxFreq) maxFreq = f;
                if (f < minFreq) minFreq = f;
            }
        }

        for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
            String cls = entry.getKey();
            List<AssociationRule> rulesForClass = entry.getValue();

            if (useHMRanking) {
                rulesForClass.sort((r1, r2) -> Double.compare(r2.getHM(), r1.getHM()));
            } else {
                rulesForClass.sort((r1, r2) -> Double.compare(
                    computeChiSquare(r2, cls),
                    computeChiSquare(r1, cls)
                ));
            }

            // Compute K cho class này
            int effectiveK = k;
            if (useStratified && classFreq != null && classFreq.containsKey(cls)) {
                int freq = classFreq.get(cls);
                if (maxFreq != minFreq) {
                    // Minority freq thấp → K cao; majority freq cao → K thấp
                    double t = (double) (maxFreq - freq) / (maxFreq - minFreq);
                    effectiveK = stratifiedKMin
                        + (int) Math.round(t * (stratifiedKMax - stratifiedKMin));
                } else {
                    effectiveK = (stratifiedKMin + stratifiedKMax) / 2;
                }
            }

            int limit = Math.min(effectiveK, rulesForClass.size());
            for (int i = 0; i < limit; i++) {
                result.add(rulesForClass.get(i));
            }
        }

        Collections.sort(result);
        return result;
    }

    // -----------------------------------------------------------------------
    // Cắt tỉa 3: Database Coverage (Thuật toán 1)
    // -----------------------------------------------------------------------

    private List<AssociationRule> pruneByDatabaseCoverage(
            List<AssociationRule> rules, List<Transaction> trainData) {

        int n = trainData.size();
        int[] coverCount = new int[n];
        boolean[] removed = new boolean[n];
        int remainingCount = n;

        List<AssociationRule> selected = new ArrayList<>();
        List<AssociationRule> spare = new ArrayList<>();  // WCBA: rules không cover ai mới

        for (AssociationRule rule : rules) {
            boolean coversAny = false;

            // Vẫn loop qua toàn bộ rules để compute spare (không break sớm)
            if (remainingCount > 0) {
                for (int i = 0; i < n; i++) {
                    if (removed[i]) continue;
                    Transaction t = trainData.get(i);
                    if (rule.matches(t)
                            && rule.getClassLabel().equals(t.getClassLabel())) {
                        coversAny = true;
                        coverCount[i]++;
                        if (coverCount[i] >= coverageThreshold) {
                            removed[i] = true;
                            remainingCount--;
                        }
                    }
                }
            }

            if (coversAny) {
                selected.add(rule);
            } else if (useSpareRules) {
                // WCBA spare: rule không cover record mới, nhưng có thể vẫn match
                // bản ghi đã được cover δ lần. Giữ lại cho fallback prediction.
                spare.add(rule);
            }
        }

        this.spareRulesList = spare;
        return selected;
    }

    // -----------------------------------------------------------------------
    // Phân lớp (§4) — truy vấn qua CR-tree
    // -----------------------------------------------------------------------

    public String classify(Transaction record) {
        // 1. Truy vấn strong CR-tree
        List<AssociationRule> matching = crTree.findMatching(record);

        // 2. WCBA: nếu strong empty → fallback sang spare
        if (matching.isEmpty() && useSpareRules && spareCRTree != null) {
            matching = spareCRTree.findMatching(record);
        }

        if (matching.isEmpty()) return defaultClass;

        // HƯỚNG 1: Threshold Adjustment - filter rules theo confidence
        matching = filterRulesByThreshold(matching, classificationThreshold);
        if (matching.isEmpty()) return defaultClass;

        Map<String, List<AssociationRule>> byClass = new HashMap<>();
        for (AssociationRule rule : matching) {
            byClass.computeIfAbsent(rule.getClassLabel(), k -> new ArrayList<>())
                   .add(rule);
        }

        if (byClass.size() == 1) {
            return byClass.keySet().iterator().next();
        }

        // Weighted chi-square: score(G) = Σ [χ²(r)]² / maxχ²(r)
        String bestClass = defaultClass;
        double bestScore = -1.0;

        for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
            String cls = entry.getKey();
            double score = 0.0;
            for (AssociationRule rule : entry.getValue()) {
                double chi2    = computeChiSquare(rule, cls);
                double maxChi2 = computeMaxChiSquare(rule, cls);
                if (maxChi2 > 0) score += (chi2 * chi2) / maxChi2;
            }
            if (score > bestScore) {
                bestScore = score;
                bestClass = cls;
            }
        }
        return bestClass;
    }

    // -----------------------------------------------------------------------
    // HƯỚNG 1: Filter Rules By Threshold
    // -----------------------------------------------------------------------
    
    private List<AssociationRule> filterRulesByThreshold(
            List<AssociationRule> rules, double threshold) {
        if (threshold >= 1.0) return rules;  // No filtering if threshold >= 1.0
        
        List<AssociationRule> filtered = new ArrayList<>();
        for (AssociationRule rule : rules) {
            // Confidence = support(A ∩ B ∩ C) / support(A ∩ B)
            double confidence = (double) rule.getSupportCount() / 
                               Math.max(1, rule.getCondsetSupportCount());
            if (confidence >= threshold) {
                filtered.add(rule);
            }
        }
        return filtered.isEmpty() ? rules : filtered;  // Fallback if none pass
    }

    // -----------------------------------------------------------------------
    // Tính chi-square (dùng chung cho cắt tỉa và phân lớp)
    // -----------------------------------------------------------------------

    protected double computeChiSquare(AssociationRule rule, String cls) {
        double n = totalTransactions;
        double a = rule.getSupportCount();
        double b = rule.getCondsetSupportCount() - a;
        double c = classFreq.getOrDefault(cls, 0) - a;
        double d = n - a - b - c;

        double denom = (a + b) * (c + d) * (a + c) * (b + d);
        if (denom == 0) return 0.0;
        
        double chi2 = n * Math.pow(a * d - b * c, 2) / denom;
        
        // HƯỚNG 2: Class weighting - boost minority classes
        if (useClassWeighting && classFreq.size() > 0) {
            int classFreqVal = classFreq.getOrDefault(cls, 1);
            double weight = (double) n / (classFreqVal * classFreq.size());
            chi2 *= weight;
        }
        
        return chi2;
    }

    protected double computeMaxChiSquare(AssociationRule rule, String cls) {
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

    // -----------------------------------------------------------------------
    // Dự đoán hàng loạt & đánh giá
    // -----------------------------------------------------------------------

    public List<String> predict(List<Transaction> testData) {
        List<String> predictions = new ArrayList<>();
        for (Transaction t : testData) predictions.add(classify(t));
        return predictions;
    }

    public double evaluate(List<Transaction> testData) {
        if (testData.isEmpty()) return 0.0;
        int correct = 0;
        for (Transaction t : testData) {
            if (classify(t).equals(t.getClassLabel())) correct++;
        }
        return (double) correct / testData.size();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public List<AssociationRule> getRules()    { return rules; }
    public CRTree                getCRTree()   { return crTree; }
    public String                getDefaultClass()          { return defaultClass; }
    public int                   getCandidateCount()        { return candidateCount; }
    public int                   getAfterGeneralPruneCount(){ return afterGeneralPruneCount; }
    public int                   getAfterChiPruneCount()    { return afterChiPruneCount; }
    public int                   getAfterCoveragePruneCount(){ return afterCoveragePruneCount; }
    public double                getChiSquareThreshold()    { return chiSquareThreshold; }
    public int                   getCoverageThreshold()     { return coverageThreshold; }
    public int                   getTopK()                  { return topK; }
    public Map<String, Integer>  getClassFreq()             { return classFreq; }
    public int                   getTotalTransactions()     { return totalTransactions; }
}
