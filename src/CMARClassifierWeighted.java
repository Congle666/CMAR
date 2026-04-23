import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hướng 1 — CMAR với điểm χ² có trọng số theo lớp (Class-weighted χ²).
 *
 * Mở rộng {@link CMARClassifier}: giữ nguyên mining + cắt tỉa 3 tầng +
 * lưu CR-tree, chỉ thay đổi bước <b>classify</b> bằng cách nhân thêm
 * <em>trọng số nghịch tần suất lớp</em> vào score mỗi nhóm:
 *
 * <pre>
 *   score(c) = weight(c) * Σ [ χ²(R)² / maxχ²(R) ]
 *   weight(c) = N / ( k * freq(c) )            -- công thức "balanced" của sklearn
 * </pre>
 *
 * Ý tưởng: lớp thiểu số (freq thấp) nhận weight lớn → khi tính score
 * tổng hợp, thiểu số không bị áp đảo bởi đa số → Macro-F1 + Recall tăng.
 *
 * Khi dataset cân bằng (freq xấp xỉ nhau): mọi weight ≈ 1.0 → hành vi
 * giống hệt baseline → an toàn khi dùng thay thế.
 */
public class CMARClassifierWeighted extends CMARClassifier {

    /** Trọng số nghịch tần suất cho mỗi nhãn lớp. Được tính trong train(). */
    private final Map<String, Double> classWeights = new HashMap<>();

    // -----------------------------------------------------------------------

    @Override
    public void train(List<AssociationRule> candidateRules,
                      List<Transaction> trainData) {
        super.train(candidateRules, trainData);
        computeClassWeights();
    }

    /**
     * Tính trọng số nghịch tần suất cân bằng:
     *   w(c) = N / (k × freq(c))
     * với N = tổng số bản ghi huấn luyện, k = số lớp.
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

    /** @return snapshot bất biến của bảng trọng số lớp (dùng để báo cáo / debug). */
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

        // Rẽ nhánh: tất cả luật khớp đều cùng một lớp → trả về lớp đó
        if (byClass.size() == 1) {
            return byClass.keySet().iterator().next();
        }

        // Tính score χ² có trọng số cho mỗi lớp
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
            // Áp dụng trọng số lớp (balanced theo sklearn)
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
