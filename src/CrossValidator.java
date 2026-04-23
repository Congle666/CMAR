import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Kiểm chứng chéo K-fold có phân tầng (Stratified K-Fold CV) cho CMAR.
 *
 * Triển khai đúng giao thức đánh giá của paper CMAR:
 *   - 10-fold stratified cross-validation
 *   - Mỗi fold bảo toàn phân phối lớp
 *   - Báo cáo accuracy trung bình trên các fold
 */
public class CrossValidator {

    /**
     * Chạy stratified k-fold CV và trả về accuracy của từng fold.
     *
     * @param data           dataset đầy đủ
     * @param k              số fold (thường là 10)
     * @param minSupportPct  minSupport dưới dạng phần trăm kích thước train (0.01 = 1%)
     * @param minConfidence  confidence tối thiểu của luật
     * @param chiSqThreshold ngưỡng ý nghĩa chi-square
     * @param coverageDelta  tham số database coverage
     * @param seed           seed ngẫu nhiên để lặp lại được
     * @return mảng k giá trị accuracy
     */
    public static double[] run(List<Transaction> data, int k,
                                double minSupportPct, double minConfidence,
                                double chiSqThreshold, int coverageDelta,
                                long seed) {
        return run(data, k, minSupportPct, minConfidence,
                   chiSqThreshold, coverageDelta, seed, Integer.MAX_VALUE);
    }

    public static double[] run(List<Transaction> data, int k,
                                double minSupportPct, double minConfidence,
                                double chiSqThreshold, int coverageDelta,
                                long seed, int maxPatternLength) {
        List<EvalMetrics> foldMetrics = runWithMetrics(
            data, k, minSupportPct, minConfidence,
            chiSqThreshold, coverageDelta, seed, maxPatternLength);
        double[] accs = new double[foldMetrics.size()];
        for (int i = 0; i < foldMetrics.size(); i++) {
            accs[i] = foldMetrics.get(i).accuracy;
        }
        return accs;
    }

    /**
     * Giống {@link #run}, nhưng trả về đầy đủ {@link EvalMetrics} của từng fold
     * (accuracy + P/R/F1 theo lớp + Macro-F1 + Weighted-F1).
     *
     * Đây là phương thức được ưu tiên khi benchmark các cải tiến — người gọi
     * có thể tổng hợp F1 và chỉ số theo lớp qua các fold.
     */
    public static List<EvalMetrics> runWithMetrics(
            List<Transaction> data, int k,
            double minSupportPct, double minConfidence,
            double chiSqThreshold, int coverageDelta,
            long seed) {
        return runWithMetrics(data, k, minSupportPct, minConfidence,
            chiSqThreshold, coverageDelta, seed, Integer.MAX_VALUE);
    }

    public static List<EvalMetrics> runWithMetrics(
            List<Transaction> data, int k,
            double minSupportPct, double minConfidence,
            double chiSqThreshold, int coverageDelta,
            long seed, int maxPatternLength) {
        return runWithMetrics(data, k, minSupportPct, minConfidence,
            chiSqThreshold, coverageDelta, seed, maxPatternLength,
            CMARClassifier::new);
    }

    /**
     * Overload cho phép cắm factory classifier tuỳ ý. Truyền
     * {@code CMARClassifierWeighted::new} để benchmark biến thể có trọng số,
     * hoặc bất kỳ subclass nào của {@link CMARClassifier}.
     */
    public static List<EvalMetrics> runWithMetrics(
            List<Transaction> data, int k,
            double minSupportPct, double minConfidence,
            double chiSqThreshold, int coverageDelta,
            long seed, int maxPatternLength,
            Supplier<CMARClassifier> classifierFactory) {
        return runWithMetrics(data, k, minSupportPct, minConfidence,
            chiSqThreshold, coverageDelta, seed, maxPatternLength,
            classifierFactory, 0.0);
    }

    /**
     * Overload đầy đủ kèm hỗ trợ minSup theo từng lớp (Hướng 2).
     *
     * @param classMinSupFraction  khi > 0, ngưỡng mỗi lớp là
     *   {@code max(2, round(fraction × freq(c)))}. Giá trị điển hình trùng
     *   với {@code minSupportPct} (ví dụ 0.05). Khi ≤ 0, tắt (baseline —
     *   chỉ dùng minSupport toàn cục).
     */
    public static List<EvalMetrics> runWithMetrics(
            List<Transaction> data, int k,
            double minSupportPct, double minConfidence,
            double chiSqThreshold, int coverageDelta,
            long seed, int maxPatternLength,
            Supplier<CMARClassifier> classifierFactory,
            double classMinSupFraction) {

        // --- Chia có phân tầng: nhóm theo lớp, rồi phân phối ---
        List<Transaction> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, new Random(seed));

        // Nhóm theo nhãn lớp
        java.util.Map<String, List<Transaction>> byClass = new java.util.LinkedHashMap<>();
        for (Transaction t : shuffled) {
            byClass.computeIfAbsent(t.getClassLabel(), c -> new ArrayList<>()).add(t);
        }

        // Tạo k fold có phân tầng
        @SuppressWarnings("unchecked")
        List<Transaction>[] folds = new ArrayList[k];
        for (int i = 0; i < k; i++) folds[i] = new ArrayList<>();

        for (List<Transaction> classGroup : byClass.values()) {
            for (int i = 0; i < classGroup.size(); i++) {
                folds[i % k].add(classGroup.get(i));
            }
        }

        // --- Chạy k vòng lặp ---
        List<EvalMetrics> results = new ArrayList<>();

        for (int fold = 0; fold < k; fold++) {
            // Xây tập train và test
            List<Transaction> testData = folds[fold];
            List<Transaction> trainData = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                if (j != fold) trainData.addAll(folds[j]);
            }

            // Tính minSupport từ phần trăm
            int minSupport = Math.max(2, (int) Math.round(trainData.size() * minSupportPct));

            // minSup theo lớp (Hướng 2)
            Map<String, Integer> classMinSupMap = null;
            if (classMinSupFraction > 0) {
                Map<String, Integer> classFreq = new HashMap<>();
                for (Transaction t : trainData) {
                    classFreq.merge(t.getClassLabel(), 1, Integer::sum);
                }
                classMinSupMap = new HashMap<>();
                int minThreshold = Integer.MAX_VALUE;
                for (Map.Entry<String, Integer> e : classFreq.entrySet()) {
                    int thr = Math.max(2,
                        (int) Math.round(classMinSupFraction * e.getValue()));
                    classMinSupMap.put(e.getKey(), thr);
                    if (thr < minThreshold) minThreshold = thr;
                }
                // KHÔNG hạ global minSupport — giữ nguyên để đảm bảo tractability
                // trên dataset nhiều chiều (sonar 60 attrs, waveform 21 attrs).
                // classMinSupMap chỉ ảnh hưởng đến bước sinh luật, không phải
                // item-level pruning. Trade-off: item hiếm chỉ xuất hiện trong
                // 1 class cực thiểu số có thể bị lọc → một phần hiệu ứng H2
                // bị hạn chế, nhưng tất cả datasets chạy được.
            }

            // Khai thác CAR trực tiếp trên CR-tree (FP-Growth có nhận thức về lớp)
            FPGrowth fpGrowth = new FPGrowth(minSupport);
            fpGrowth.setMaxPatternLength(maxPatternLength);
            if (classMinSupMap != null) {
                fpGrowth.setClassMinSupMap(classMinSupMap);
            }
            List<AssociationRule> candidates = fpGrowth.mine(trainData, minConfidence);

            // Huấn luyện — tạo classifier thông qua factory được truyền vào
            CMARClassifier classifier = classifierFactory.get();
            classifier.setChiSquareThreshold(chiSqThreshold);
            classifier.setCoverageThreshold(coverageDelta);
            classifier.train(candidates, trainData);

            // Dự đoán & đánh giá
            List<String> predictions = classifier.predict(testData);
            EvalMetrics metrics = EvalMetrics.compute(testData, predictions);
            results.add(metrics);

            System.out.printf("    Fold %2d: acc=%.4f macroF1=%.4f  (train=%d, test=%d, minSup=%d, rules=%d)%n",
                fold + 1, metrics.accuracy, metrics.macroF1,
                trainData.size(), testData.size(),
                minSupport, classifier.getRules().size());
        }

        return results;
    }

    /** Tính trung bình của một mảng. */
    public static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    /** Tính độ lệch chuẩn của một mảng. */
    public static double stddev(double[] values) {
        double m = mean(values);
        double sumSq = 0;
        for (double v : values) sumSq += (v - m) * (v - m);
        return Math.sqrt(sumSq / values.length);
    }
}
