import java.io.*;
import java.util.*;

/**
 * Benchmark PHASE 1: Threshold Adjustment (0.3, 0.4, 0.5)
 * Chỉ test 5 Imbalanced Datasets
 */
public class BenchmarkPhase1 {
    
    private static final String[] IMBALANCED_DATASETS = {
        "lymph",
        "glass",
        "vehicle",
        "hepatitis",
        "german"
    };
    
    private static final Map<String, Double> MIN_SUP_PCT = Map.ofEntries(
        Map.entry("lymph", 0.05),
        Map.entry("glass", 0.01),
        Map.entry("vehicle", 0.03),
        Map.entry("hepatitis", 0.05),
        Map.entry("german", 0.06)
    );
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   PHASE 1: THRESHOLD ADJUSTMENT TEST (0.3, 0.4, 0.5)   ║");
        System.out.println("║            5 Imbalanced Datasets                       ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        double[] thresholds = {0.3, 0.4, 0.5};  // 0.5 = baseline
        Map<String, Map<Double, List<Double>>> allResults = new HashMap<>();
        
        for (String dataset : IMBALANCED_DATASETS) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("📊 Dataset: " + dataset.toUpperCase());
            System.out.println("=".repeat(70));
            
            List<Transaction> transactions = DatasetLoader.load("data/" + dataset + ".csv");
            double minSup = MIN_SUP_PCT.get(dataset);
            
            System.out.printf("   Records: %d, MinSup: %.1f%%\n", 
                transactions.size(), minSup * 100);
            
            Map<Double, List<Double>> datasetResults = new HashMap<>();
            
            // Test mỗi threshold
            for (double threshold : thresholds) {
                System.out.printf("\n   ▶ Threshold = %.1f", threshold);
                
                // Chạy CV với classifier tuỳ chỉnh threshold
                List<EvalMetrics> metrics = CrossValidator.runWithMetrics(
                    transactions, 10,
                    minSup, 0.5,  // minSup, minConf
                    3.841, 4,     // chiSq threshold, coverage delta
                    42,           // seed
                    Integer.MAX_VALUE,
                    () -> {
                        CMARClassifier classifier = new CMARClassifier();
                        classifier.setThreshold(threshold);  // ← Apply threshold
                        return classifier;
                    }
                );
                
                double avgAcc = metrics.stream()
                    .mapToDouble(m -> m.accuracy)
                    .average().orElse(0);
                double avgF1 = metrics.stream()
                    .mapToDouble(m -> m.macroF1)
                    .average().orElse(0);
                
                System.out.printf(" → Accuracy: %.2f%%, F1: %.4f\n", avgAcc * 100, avgF1);
                
                List<Double> result = new ArrayList<>();
                result.add(avgAcc);
                result.add(avgF1);
                datasetResults.put(threshold, result);
            }
            
            // So sánh với baseline (0.5)
            System.out.println("\n   📈 IMPROVEMENT vs Baseline (0.5):");
            List<Double> baseline = datasetResults.get(0.5);
            for (double threshold : new double[]{0.3, 0.4}) {
                List<Double> curr = datasetResults.get(threshold);
                double accImprov = (curr.get(0) - baseline.get(0)) / baseline.get(0) * 100;
                double f1Improv = (curr.get(1) - baseline.get(1)) / baseline.get(1) * 100;
                
                System.out.printf("     Threshold=%.1f: Acc %+.2f%% | F1 %+.2f%%\n",
                    threshold, accImprov, f1Improv);
            }
            
            allResults.put(dataset, datasetResults);
        }
        
        // Summary table
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 SUMMARY TABLE - All 5 Imbalanced Datasets");
        System.out.println("=".repeat(70));
        System.out.printf("%-15s | %10s | %10s | %10s | Best\n", 
            "Dataset", "T=0.3", "T=0.4", "T=0.5 (BL)");
        System.out.println("-".repeat(70));
        
        for (String dataset : IMBALANCED_DATASETS) {
            Map<Double, List<Double>> res = allResults.get(dataset);
            double f03 = res.get(0.3).get(1);
            double f04 = res.get(0.4).get(1);
            double f05 = res.get(0.5).get(1);
            
            String best = f03 >= f04 && f03 >= f05 ? "0.3" : 
                         (f04 >= f05 ? "0.4" : "0.5");
            
            System.out.printf("%-15s | %.4f | %.4f | %.4f | %s\n", 
                dataset, f03, f04, f05, best);
        }
        
        System.out.println("=".repeat(70));
        System.out.println("✅ PHASE 1 Complete!");
    }
}
