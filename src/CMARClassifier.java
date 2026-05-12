import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ phân lớp CMAR — Li, Han & Pei (2001) ICDM.
 *
 * <p>Huấn luyện (train):</p>
 * <ul>
 *   <li><b>Cắt tỉa 1</b> — General rule pruning: loại bỏ luật cụ thể hơn đang
 *       bị thống trị bởi một luật tổng quát hơn có hạng cao hơn và cùng nhãn.</li>
 *   <li><b>Cắt tỉa 2</b> — Chi-square + tương quan dương: chỉ giữ luật có χ²
 *       vượt ngưỡng ý nghĩa (mặc định 3.841, p=0.05) VÀ tương quan dương.</li>
 *   <li><b>Cắt tỉa 3</b> — Database coverage (Thuật toán 1): lặp chọn luật phủ
 *       các bản ghi training; bản ghi được phủ đủ δ luật thì loại.</li>
 *   <li><b>Top-K per class</b> (tùy chọn): chọn K luật mạnh nhất mỗi lớp.</li>
 * </ul>
 *
 * <p>Phân lớp (classify):</p>
 * <ol>
 *   <li>Lấy tất cả luật trong CR-tree khớp với bản ghi test.</li>
 *   <li>Không có luật khớp → trả về lớp mặc định.</li>
 *   <li>Tất cả luật khớp cùng một lớp → trả về lớp đó.</li>
 *   <li>Ngược lại: score(c) = Σ [χ²(r)]² / maxχ²(r); lớp có score lớn nhất.</li>
 * </ol>
 */
public class CMARClassifier {

    private List<AssociationRule> rules;
    private CRTree crTree;
    private String defaultClass;
    private int totalTransactions;
    private Map<String, Integer> classFreq;
    private Map<String, Integer> itemFreq;   // dùng để sắp thứ tự path trong CR-tree

    // --- Tham số cắt tỉa ---
    private double chiSquareThreshold = 3.841;
    private int    coverageThreshold  = 4;
    private int    topK               = 0;  // Top-K per class (0 = disable)

    // --- Thống kê ---
    private int candidateCount;
    private int afterGeneralPruneCount;
    private int afterChiPruneCount;
    private int afterCoveragePruneCount;

    public void setChiSquareThreshold(double threshold) { this.chiSquareThreshold = threshold; }
    public void setCoverageThreshold(int delta)         { this.coverageThreshold = delta; }
    public void setTopK(int k)                          { this.topK = k; }

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
    // Cắt tỉa 2: chi-square + tương quan dương
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
    // Top-K Selection Per Class
    // -----------------------------------------------------------------------

    private List<AssociationRule> selectTopKRulesPerClass(
            List<AssociationRule> rules, int k) {
        Map<String, List<AssociationRule>> byClass = new HashMap<>();
        List<AssociationRule> result = new ArrayList<>();

        for (AssociationRule r : rules) {
            byClass.computeIfAbsent(r.getClassLabel(), cls -> new ArrayList<>())
                   .add(r);
        }

        for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
            String cls = entry.getKey();
            List<AssociationRule> rulesForClass = entry.getValue();

            rulesForClass.sort((r1, r2) -> Double.compare(
                computeChiSquare(r2, cls),
                computeChiSquare(r1, cls)
            ));

            int limit = Math.min(k, rulesForClass.size());
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

        for (AssociationRule rule : rules) {
            if (remainingCount == 0) break;
            boolean coversAny = false;

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

            if (coversAny) selected.add(rule);
        }

        return selected;
    }

    // -----------------------------------------------------------------------
    // Phân lớp — truy vấn qua CR-tree
    // -----------------------------------------------------------------------

    public String classify(Transaction record) {
        List<AssociationRule> matching = crTree.findMatching(record);
        if (matching.isEmpty()) return defaultClass;

        Map<String, List<AssociationRule>> byClass = new HashMap<>();
        for (AssociationRule rule : matching) {
            byClass.computeIfAbsent(rule.getClassLabel(), k -> new ArrayList<>())
                   .add(rule);
        }

        if (byClass.size() == 1) {
            return byClass.keySet().iterator().next();
        }

        // Weighted chi-square: score(c) = Σ [χ²(r)]² / maxχ²(r)
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
        return n * Math.pow(a * d - b * c, 2) / denom;
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
