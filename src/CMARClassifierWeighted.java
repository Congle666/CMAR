import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hướng 1 — CMAR với Class-weighted χ² score.
 *
 * Mở rộng {@link CMARClassifier}: giữ nguyên mining + 3-tier pruning +
 * CR-tree storage, chỉ thay đổi bước <b>classify</b> bằng cách nhân
 * thêm <em>inverse-frequency class weight</em> vào score mỗi nhóm:
 *
 * <pre>
 *   score(c) = weight(c) * Σ [ χ²(R)² / maxχ²(R) ]
 *   weight(c) = N / ( k * freq(c) )            -- sklearn "balanced"
 * </pre>
 *
 * Ý tưởng: class minority (freq thấp) nhận weight lớn → khi score tổng
 * hợp, minority không bị áp đảo bởi majority → Macro-F1 + Recall tăng.
 *
 * Khi dataset cân bằng (freq xấp xỉ nhau): mọi weight ≈ 1.0 → hành vi
 * giống hệt baseline → an toàn làm fallback.
 */
public class CMARClassifierWeighted extends CMARClassifier {

    /** Inverse-frequency weight per class label. Populated in train(). */
    private final Map<String, Double> classWeights = new HashMap<>();

    // -----------------------------------------------------------------------

    @Override
    public void train(List<AssociationRule> candidateRules,
                      List<Transaction> trainData) {
        super.train(candidateRules, trainData);
        computeClassWeights();
    }

    /**
     * Computes balanced inverse-frequency weights:
     *   w(c) = N / (k × freq(c))
     * where N = total training records, k = number of classes.
     */
    private void computeClassWeights() {
        classWeights.clear();
        Map<String, Integer> freq = getClassFreq();
        int n = getTotalTransactions();
        int k = freq.size();
        if (k == 0 || n == 0) return;
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            int f = e.getValue();
            double w = (f == 0) ? 1.0 : (double) n / (k * f);
            classWeights.put(e.getKey(), w);
        }
    }

    /** @return unmodifiable snapshot of class weights (for reporting/debug). */
    public Map<String, Double> getClassWeights() {
        return java.util.Collections.unmodifiableMap(classWeights);
    }

    // -----------------------------------------------------------------------

    @Override
    public String classify(Transaction record) {
        List<AssociationRule> matching = getCRTree().findMatching(record);

        if (matching.isEmpty()) return getDefaultClass();

        Map<String, List<AssociationRule>> byClass = new HashMap<>();
        for (AssociationRule rule : matching) {
            byClass.computeIfAbsent(rule.getClassLabel(), k -> new ArrayList<>())
                   .add(rule);
        }

        // Shortcut: all matching rules agree → return that class
        if (byClass.size() == 1) {
            return byClass.keySet().iterator().next();
        }

        // Weighted χ² score per class
        String bestClass = getDefaultClass();
        double bestScore = -1.0;

        for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
            String cls = entry.getKey();
            double score = 0.0;
            for (AssociationRule rule : entry.getValue()) {
                double chi2    = computeChiSquare(rule, cls);
                double maxChi2 = computeMaxChiSquare(rule, cls);
                if (maxChi2 > 0) score += (chi2 * chi2) / maxChi2;
            }
            // Apply class weight (sklearn "balanced")
            double weight = classWeights.getOrDefault(cls, 1.0);
            score *= weight;

            if (score > bestScore) {
                bestScore = score;
                bestClass = cls;
            }
        }
        return bestClass;
    }
}
