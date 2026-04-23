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
        return runWithMetrics(data, k, minSupportPct, minConfidence,
            chiSqThreshold, coverageDelta, seed, maxPatternLength,
            classifierFactory, classMinSupFraction,
            0.0, 0.0);
    }

    /**
     * Overload đầy đủ kèm Hướng 3 — Adaptive minConf per class.
     *
     * @param adaptiveMinConfFloor  ngưỡng sàn tuyệt đối cho minConf(c). Khi
     *   > 0, enable Hướng 3 với công thức:
     *   {@code minConf(c) = min(globalMinConf, max(floor, lift × freq(c)/N))}.
     *   Giá trị điển hình: 0.3.
     * @param adaptiveMinConfLift  hệ số khuếch đại baseline class frequency
     *   (= random-guess confidence). Điển hình 5.0 — rule phải có confidence
     *   ≥ 5 lần baseline ngẫu nhiên mới được giữ. Chỉ có tác dụng khi
     *   {@code adaptiveMinConfFloor > 0}.
     */
    public static List<EvalMetrics> runWithMetrics(
            List<Transaction> data, int k,
            double minSupportPct, double minConfidence,
            double chiSqThreshold, int coverageDelta,
            long seed, int maxPatternLength,
            Supplier<CMARClassifier> classifierFactory,
            double classMinSupFraction,
            double adaptiveMinConfFloor,
            double adaptiveMinConfLift) {

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

            // Tính class frequency một lần, dùng cho cả H2 và H3
            Map<String, Integer> classFreq = null;
            if (classMinSupFraction > 0 || adaptiveMinConfFloor > 0) {
                classFreq = new HashMap<>();
                for (Transaction t : trainData) {
                    classFreq.merge(t.getClassLabel(), 1, Integer::sum);
                }
            }

            // minSup theo lớp (Hướng 2)
            Map<String, Integer> classMinSupMap = null;
            if (classMinSupFraction > 0) {
                classMinSupMap = new HashMap<>();
                for (Map.Entry<String, Integer> e : classFreq.entrySet()) {
                    int thr = Math.max(2,
                        (int) Math.round(classMinSupFraction * e.getValue()));
                    classMinSupMap.put(e.getKey(), thr);
                }
                // KHÔNG hạ global minSupport — giữ nguyên để tractable trên
                // dataset nhiều chiều. classMinSupMap chỉ ảnh hưởng sinh luật.
            }

            // minConf theo lớp (Hướng 3 — Adaptive minConf)
            Map<String, Double> classMinConfMap = null;
            if (adaptiveMinConfFloor > 0) {
                classMinConfMap = new HashMap<>();
                int N = trainData.size();
                for (Map.Entry<String, Integer> e : classFreq.entrySet()) {
                    double classRatio = (double) e.getValue() / N;
                    // min(globalMinConf, max(floor, lift × freq(c)/N))
                    double thr = Math.min(minConfidence,
                        Math.max(adaptiveMinConfFloor,
                                 adaptiveMinConfLift * classRatio));
                    classMinConfMap.put(e.getKey(), thr);
                }
            }

            // Khai thác CAR trực tiếp trên CR-tree (FP-Growth có nhận thức về lớp)
            FPGrowth fpGrowth = new FPGrowth(minSupport);
            fpGrowth.setMaxPatternLength(maxPatternLength);
            if (classMinSupMap != null) {
                fpGrowth.setClassMinSupMap(classMinSupMap);
            }
            if (classMinConfMap != null) {
                fpGrowth.setClassMinConfMap(classMinConfMap);
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
