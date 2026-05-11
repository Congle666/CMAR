import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PHASE 2: CLASS WEIGHTING TEST
 * 
 * Test class weighting approach on 5 imbalanced datasets.
 * Weight formula: weight_class = total_records / (class_frequency × num_classes)
 * 
 * Test matrix: 5 datasets × 2 configurations (unweighted vs weighted) × 10-fold CV
 */
public class BenchmarkPhase2 {
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   PHASE 2: CLASS WEIGHTING TEST                        ║");
        System.out.println("║            5 Imbalanced Datasets                       ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();

        // 5 imbalanced datasets
        String[] datasets = {"lymph", "glass", "vehicle", "hepatitis", "german"};
        Map<String, Double> minSupMap = new HashMap<>();
        minSupMap.put("lymph", 5.0);
        minSupMap.put("glass", 1.0);
        minSupMap.put("vehicle", 3.0);
        minSupMap.put("hepatitis", 5.0);
        minSupMap.put("german", 6.0);

        for (String dataset : datasets) {
            String filePath = "data/" + dataset + ".csv";
            List<Transaction> transactions = DatasetLoader.load(filePath);
            
            System.out.println("======================================================================");
            System.out.println("📊 Dataset: " + dataset.toUpperCase());
            System.out.println("======================================================================");
            System.out.printf("   Records: %d, MinSup: %,.1f%%%n%n", transactions.size(), minSupMap.get(dataset));

            // Test 1: Baseline (unweighted)
            System.out.println("   ▶ Weighting = OFF (Baseline)");
            List<EvalMetrics> metricsUnweighted = CrossValidator.runWithMetrics(
                transactions, 10,
                minSupMap.get(dataset), 0.5,  // minSupport, minConfidence
                3.841, 4,     // chiSquareThreshold, coverageDelta
                42,           // seed
                Integer.MAX_VALUE,
                () -> {
                    CMARClassifier classifier = new CMARClassifier();
                    classifier.setUseClassWeighting(false);
                    return classifier;
                }
            );
            double accUnweighted = metricsUnweighted.stream().mapToDouble(m -> m.accuracy).average().orElse(0);
            double f1Unweighted = metricsUnweighted.stream().mapToDouble(m -> m.macroF1).average().orElse(0);
            System.out.printf(" → Accuracy: %.2f%%, F1: %.4f%n%n", accUnweighted * 100, f1Unweighted);

            // Test 2: With class weighting
            System.out.println("   ▶ Weighting = ON (Minority boost)");
            List<EvalMetrics> metricsWeighted = CrossValidator.runWithMetrics(
                transactions, 10,
                minSupMap.get(dataset), 0.5,  // minSupport, minConfidence
                3.841, 4,     // chiSquareThreshold, coverageDelta
                42,           // seed
                Integer.MAX_VALUE,
                () -> {
                    CMARClassifier classifier = new CMARClassifier();
                    classifier.setUseClassWeighting(true);  // Enable class weighting
                    return classifier;
                }
            );
            double accWeighted = metricsWeighted.stream().mapToDouble(m -> m.accuracy).average().orElse(0);
            double f1Weighted = metricsWeighted.stream().mapToDouble(m -> m.macroF1).average().orElse(0);
            System.out.printf(" → Accuracy: %.2f%%, F1: %.4f%n%n", accWeighted * 100, f1Weighted);

            // Comparison
            System.out.println("   📈 IMPROVEMENT vs Baseline (Unweighted):");
            double accDelta = (accWeighted - accUnweighted) * 100;
            double f1Delta = (f1Weighted - f1Unweighted) * 100;
            System.out.printf("     Weighted: Acc %+.2f%% | F1 %+.2f%%%n", accDelta, f1Delta);
            System.out.println();
        }
    }
}
