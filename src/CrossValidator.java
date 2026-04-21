import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Stratified K-Fold Cross-Validation for CMAR.
 *
 * Implements the same evaluation protocol as the CMAR paper:
 *   - 10-fold stratified cross-validation
 *   - Each fold preserves class distribution
 *   - Reports average accuracy across all folds
 */
public class CrossValidator {

    /**
     * Runs stratified k-fold cross-validation and returns per-fold accuracies.
     *
     * @param data           full dataset
     * @param k              number of folds (typically 10)
     * @param minSupport     minimum support count (will be scaled per fold)
     * @param minSupportPct  minimum support as percentage of training size (0.01 = 1%)
     * @param minConfidence  minimum rule confidence
     * @param chiSqThreshold chi-square significance threshold
     * @param coverageDelta  database coverage parameter
     * @param seed           random seed for reproducibility
     * @return array of k accuracy values
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
     * Like {@link #run}, but returns full per-fold {@link EvalMetrics}
     * (accuracy + per-class Precision/Recall/F1 + Macro-F1 + Weighted-F1).
     *
     * This is the preferred method for benchmarking improvements — it lets
     * callers aggregate F1 and per-class stats across folds.
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

        // --- Stratified split: group by class, then distribute ---
        List<Transaction> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, new Random(seed));

        // Group by class label
        java.util.Map<String, List<Transaction>> byClass = new java.util.LinkedHashMap<>();
        for (Transaction t : shuffled) {
            byClass.computeIfAbsent(t.getClassLabel(), c -> new ArrayList<>()).add(t);
        }

        // Create k folds with stratification
        @SuppressWarnings("unchecked")
        List<Transaction>[] folds = new ArrayList[k];
        for (int i = 0; i < k; i++) folds[i] = new ArrayList<>();

        for (List<Transaction> classGroup : byClass.values()) {
            for (int i = 0; i < classGroup.size(); i++) {
                folds[i % k].add(classGroup.get(i));
            }
        }

        // --- Run k iterations ---
        List<EvalMetrics> results = new ArrayList<>();

        for (int fold = 0; fold < k; fold++) {
            // Build train and test sets
            List<Transaction> testData = folds[fold];
            List<Transaction> trainData = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                if (j != fold) trainData.addAll(folds[j]);
            }

            // Compute minSupport from percentage
            int minSupport = Math.max(2, (int) Math.round(trainData.size() * minSupportPct));

            // Mine CARs directly on CR-tree (class-aware FP-Growth)
            FPGrowth fpGrowth = new FPGrowth(minSupport);
            fpGrowth.setMaxPatternLength(maxPatternLength);
            List<AssociationRule> candidates = fpGrowth.mine(trainData, minConfidence);

            // Train CMAR
            CMARClassifier classifier = new CMARClassifier();
            classifier.setChiSquareThreshold(chiSqThreshold);
            classifier.setCoverageThreshold(coverageDelta);
            classifier.train(candidates, trainData);

            // Predict & evaluate
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

    /** Computes mean of an array. */
    public static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    /** Computes standard deviation of an array. */
    public static double stddev(double[] values) {
        double m = mean(values);
        double sumSq = 0;
        for (double v : values) sumSq += (v - m) * (v - m);
        return Math.sqrt(sumSq / values.length);
    }
}
