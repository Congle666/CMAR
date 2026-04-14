import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
        double[] accuracies = new double[k];

        for (int fold = 0; fold < k; fold++) {
            // Build train and test sets
            List<Transaction> testData = folds[fold];
            List<Transaction> trainData = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                if (j != fold) trainData.addAll(folds[j]);
            }

            // Compute minSupport from percentage
            int minSupport = Math.max(2, (int) Math.round(trainData.size() * minSupportPct));

            // Mine frequent patterns
            List<List<String>> transactions = trainData.stream()
                .map(Transaction::getAllItems)
                .collect(Collectors.toList());

            FPGrowth fpGrowth = new FPGrowth(minSupport);
            fpGrowth.setMaxPatternLength(maxPatternLength);
            List<FrequentPattern> patterns = fpGrowth.mine(transactions);

            // Generate CARs
            List<AssociationRule> candidates = RuleGenerator.generate(
                patterns, trainData.size(), minConfidence);

            // Train CMAR
            CMARClassifier classifier = new CMARClassifier();
            classifier.setChiSquareThreshold(chiSqThreshold);
            classifier.setCoverageThreshold(coverageDelta);
            classifier.train(candidates, trainData);

            // Evaluate
            accuracies[fold] = classifier.evaluate(testData);

            System.out.printf("    Fold %2d: accuracy=%.4f  (train=%d, test=%d, minSup=%d, rules=%d)%n",
                fold + 1, accuracies[fold], trainData.size(), testData.size(),
                minSupport, classifier.getRules().size());
        }

        return accuracies;
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
