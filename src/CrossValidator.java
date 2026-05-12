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
 * <p>Đúng giao thức đánh giá của paper CMAR 2001:</p>
 * <ul>
 *   <li>10-fold stratified cross-validation</li>
 *   <li>Mỗi fold bảo toàn phân phối lớp</li>
 *   <li>Báo cáo accuracy + Macro-F1 + Weighted-F1 + per-class P/R/F1</li>
 * </ul>
 *
 * <p>Hỗ trợ 3 cải tiến cho dữ liệu mất cân bằng:</p>
 * <ul>
 *   <li><b>H2</b> — Class-specific minSup: {@code minSup(c) = supPct × freq(c)}</li>
 *   <li><b>H3</b> — Adaptive minConf: {@code minConf(c) = min(globalMinConf, max(floor, lift × freq(c)/N))}</li>
 *   <li><b>SMOTE</b> — Synthetic Minority Over-sampling (categorical N-variant)</li>
 * </ul>
 */
public class CrossValidator {

    /** Convenience: chỉ trả về accuracy array. */
    public static double[] run(List<Transaction> data, int k,
                                double minSupportPct, double minConfidence,
                                double chiSqThreshold, int coverageDelta,
                                long seed) {
        List<EvalMetrics> foldMetrics = runWithMetrics(
            data, k, minSupportPct, minConfidence,
            chiSqThreshold, coverageDelta, seed, Integer.MAX_VALUE);
        double[] accs = new double[foldMetrics.size()];
        for (int i = 0; i < foldMetrics.size(); i++) accs[i] = foldMetrics.get(i).accuracy;
        return accs;
    }

    /** Baseline CMAR (không H2/H3/SMOTE). */
    public static List<EvalMetrics> runWithMetrics(
            List<Transaction> data, int k,
            double minSupportPct, double minConfidence,
            double chiSqThreshold, int coverageDelta,
            long seed, int maxPatternLength) {
        return runWithMetrics(data, k, minSupportPct, minConfidence,
            chiSqThreshold, coverageDelta, seed, maxPatternLength,
            CMARClassifier::new, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Overload đầy đủ — bật/tắt từng cải tiến qua tham số.
     *
     * @param classMinSupFraction    H2 — > 0 để bật class-specific minSup. Điển hình = {@code minSupportPct}.
     * @param adaptiveMinConfFloor   H3 — > 0 để bật adaptive minConf. Điển hình 0.3.
     * @param adaptiveMinConfLift    H3 — hệ số khuếch đại baseline freq. Điển hình 5.0.
     * @param smoteTargetRatio       SMOTE — > 0 để bật. 1.0 = balance hoàn toàn. 0 = tắt.
     */
    public static List<EvalMetrics> runWithMetrics(
            List<Transaction> data, int k,
            double minSupportPct, double minConfidence,
            double chiSqThreshold, int coverageDelta,
            long seed, int maxPatternLength,
            Supplier<CMARClassifier> classifierFactory,
            double classMinSupFraction,
            double adaptiveMinConfFloor,
            double adaptiveMinConfLift,
            double smoteTargetRatio) {

        // --- Chia có phân tầng: nhóm theo lớp, rồi phân phối ---
        List<Transaction> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, new Random(seed));

        Map<String, List<Transaction>> byClass = new java.util.LinkedHashMap<>();
        for (Transaction t : shuffled) {
            byClass.computeIfAbsent(t.getClassLabel(), c -> new ArrayList<>()).add(t);
        }

        @SuppressWarnings("unchecked")
        List<Transaction>[] folds = new ArrayList[k];
        for (int i = 0; i < k; i++) folds[i] = new ArrayList<>();
        for (List<Transaction> classGroup : byClass.values()) {
            for (int i = 0; i < classGroup.size(); i++) {
                folds[i % k].add(classGroup.get(i));
            }
        }

        List<EvalMetrics> results = new ArrayList<>();

        for (int fold = 0; fold < k; fold++) {
            List<Transaction> testData = folds[fold];
            List<Transaction> trainData = new ArrayList<>();
            for (int j = 0; j < k; j++) if (j != fold) trainData.addAll(folds[j]);

            int minSupport = Math.max(2, (int) Math.round(trainData.size() * minSupportPct));

            // --- SMOTE: áp dụng oversampling minority class TRƯỚC khi tính thresholds ---
            if (smoteTargetRatio > 0) {
                int beforeSize = trainData.size();
                trainData = SMOTE.apply(trainData, 5, smoteTargetRatio, seed + fold);
                minSupport = Math.max(2, (int) Math.round(trainData.size() * minSupportPct));
                if (fold == 0) {
                    System.out.println("    SMOTE applied (fold 0): " + beforeSize
                        + " -> " + trainData.size() + " records");
                }
            }

            // --- Tính classFreq cho H2/H3 (sau SMOTE nếu có) ---
            Map<String, Integer> classFreq = null;
            if (classMinSupFraction > 0 || adaptiveMinConfFloor > 0) {
                classFreq = new HashMap<>();
                for (Transaction t : trainData) {
                    classFreq.merge(t.getClassLabel(), 1, Integer::sum);
                }
            }

            // --- H2: minSup(c) = supPct × freq(c) ---
            Map<String, Integer> classMinSupMap = null;
            if (classMinSupFraction > 0) {
                classMinSupMap = new HashMap<>();
                for (Map.Entry<String, Integer> e : classFreq.entrySet()) {
                    int thr = Math.max(2,
                        (int) Math.round(classMinSupFraction * e.getValue()));
                    classMinSupMap.put(e.getKey(), thr);
                }
            }

            // --- H3: minConf(c) = min(globalMinConf, max(floor, lift × freq(c)/N)) ---
            Map<String, Double> classMinConfMap = null;
            if (adaptiveMinConfFloor > 0) {
                classMinConfMap = new HashMap<>();
                int N = trainData.size();
                for (Map.Entry<String, Integer> e : classFreq.entrySet()) {
                    double classRatio = (double) e.getValue() / N;
                    double thr = Math.min(minConfidence,
                        Math.max(adaptiveMinConfFloor,
                                 adaptiveMinConfLift * classRatio));
                    classMinConfMap.put(e.getKey(), thr);
                }
            }

            // --- Mining CAR ---
            FPGrowth fpGrowth = new FPGrowth(minSupport);
            fpGrowth.setMaxPatternLength(maxPatternLength);
            if (classMinSupMap != null)  fpGrowth.setClassMinSupMap(classMinSupMap);
            if (classMinConfMap != null) fpGrowth.setClassMinConfMap(classMinConfMap);
            List<AssociationRule> candidates = fpGrowth.mine(trainData, minConfidence);

            // --- Huấn luyện classifier ---
            CMARClassifier classifier = classifierFactory.get();
            classifier.setChiSquareThreshold(chiSqThreshold);
            classifier.setCoverageThreshold(coverageDelta);
            classifier.train(candidates, trainData);

            // --- Dự đoán & đánh giá ---
            List<String> predictions = classifier.predict(testData);
            EvalMetrics metrics = EvalMetrics.compute(testData, predictions);
            results.add(metrics);

            System.out.printf("    Fold %2d: acc=%.4f macroF1=%.4f  (train=%d, test=%d, minSup=%d, rules=%d)%n",
                fold + 1, metrics.accuracy, metrics.macroF1,
                trainData.size(), testData.size(), minSupport,
                classifier.getRules().size());
        }

        return results;
    }
}
