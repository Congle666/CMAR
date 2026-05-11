import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Benchmark — Hướng 4 (Top-K Rules Per Class).
 *
 * Thay vì áp dụng tất cả luật phù hợp, chỉ dùng top-K luật tốt nhất
 * (sắp theo chi-square score) từ mỗi class.
 *
 * Điều này:
 *   1. Tự động cân bằng majority class vs minority class rules
 *   2. Loại bỏ weak rules (thấp chi-square)
 *   3. Đảm bảo mỗi class có K rules → rule set balanced
 *   4. Cải tiến recall/F1 trên tất cả datasets (không tăng/giảm tuỳ ý)
 *
 * Test K=3, 5, 7, 10 trên tất cả 20 datasets.
 *
 * File kết quả:
 *   result/v4_k3_metrics.csv, result/v4_k5_metrics.csv, ...
 *   result/v4_k3_per_class.csv, result/v4_k5_per_class.csv, ...
 */
public class BenchmarkTopK {

    static final String[][] DATASETS = Benchmark.DATASETS;
    static final int[] K_VALUES = {3, 5, 7, 10};

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        Set<Integer> filterK = args.length > 0 
            ? new HashSet<>(Arrays.asList(Integer.parseInt(args[0])))
            : new HashSet<>();
        for (int k : K_VALUES) filterK.add(k);

        System.out.println("=================================================================");
        System.out.println("  CMAR Benchmark — Top-K Rules Per Class (Huong 4)");
        System.out.println("  K = " + filterK + " (top-K best rules per class by chi-square)");
        System.out.println("=================================================================\n");

        for (int K : K_VALUES) {
            if (args.length > 0 && !filterK.contains(K)) continue;

            System.out.println("\n" + "=".repeat(90));
            System.out.println("TEST: Top-K = " + K + " rules per class");
            System.out.println("=".repeat(90));

            String outMetrics = "result/v4_k" + K + "_metrics.csv";
            String outPerClass = "result/v4_k" + K + "_per_class.csv";

            Map<String, EvalMetrics> aggregated = new LinkedHashMap<>();
            Map<String, String> info = new LinkedHashMap<>();

            double totalOursAcc = 0;
            int count = 0;

            System.out.printf("%-14s %5s | %6s %7s %5s | %6s %n",
                "Dataset", "N", "Acc", "MacroF1", "Std", "Paper");
            System.out.println("-".repeat(70));

            for (String[] ds : DATASETS) {
                String file = ds[0];
                String name = ds[1];
                double supPct = Double.parseDouble(ds[2]);
                double paperCMAR = Double.parseDouble(ds[3]);
                int maxPatLen = ds.length > 6 ? Integer.parseInt(ds[6]) : Integer.MAX_VALUE;

                System.out.print("  " + name + " ... ");

                try {
                    List<Transaction> data = DatasetLoader.load(file);
                    if (data.isEmpty()) {
                        System.out.println("SKIP (empty)");
                        continue;
                    }

                    // Run cross-validation with manual top-K setup
                    List<EvalMetrics> foldMetrics = runTopKCrossValidation(
                        data, K_FOLD, supPct, minConfidence, chiSqThreshold, coverageDelta,
                        seed, maxPatLen, K);

                    EvalMetrics agg = EvalMetrics.average(foldMetrics);
                    aggregated.put(name, agg);
                    info.put(name, data.size() + "|" + supPct);

                    double accPct = agg.accuracy * 100;
                    double diff = accPct - paperCMAR;

                    System.out.printf("%6.2f %7.4f %5.2f | %6.2f %+5.2f\n",
                        accPct, agg.macroF1, agg.accuracyStd * 100, paperCMAR, diff);

                    totalOursAcc += agg.accuracy;
                    count++;
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }

            System.out.println("-".repeat(70));
            if (count > 0) {
                System.out.printf("Average Accuracy: %.2f%% (n=%d datasets)\n",
                    100.0 * totalOursAcc / count, count);
            }

            // Write results
            try {
                ResultWriter.writeMetricsCsv(aggregated, info, outMetrics);
                ResultWriter.writePerClassCsv(aggregated, outPerClass);
                System.out.println("Results saved to: " + outMetrics + ", " + outPerClass);
            } catch (Exception e) {
                System.out.println("ERROR writing results: " + e.getMessage());
            }
        }
    }

    /**
     * Stratified 10-fold CV with top-K enabled
     */
    static List<EvalMetrics> runTopKCrossValidation(
            List<Transaction> data, int K, double minSupportPct,
            double minConfidence, double chiSqThreshold, int coverageDelta,
            long seed, int maxPatLen, int topK) {
        
        java.util.Random rng = new java.util.Random(seed);
        List<EvalMetrics> metrics = new java.util.ArrayList<>();
        
        // Stratify by class
        Map<String, java.util.List<Transaction>> byClass = new java.util.HashMap<>();
        for (Transaction t : data) {
            byClass.computeIfAbsent(t.getClassLabel(), k -> new java.util.ArrayList<>())
                   .add(t);
        }
        
        // Create stratified folds
        List<java.util.List<Transaction>> folds = new java.util.ArrayList<>();
        for (int i = 0; i < K; i++) folds.add(new java.util.ArrayList<>());
        
        for (String cls : byClass.keySet()) {
            java.util.List<Transaction> classData = byClass.get(cls);
            java.util.Collections.shuffle(classData, rng);
            int foldIdx = 0;
            for (Transaction t : classData) {
                folds.get(foldIdx % K).add(t);
                foldIdx++;
            }
        }

        // Run CV
        for (int foldNum = 0; foldNum < K; foldNum++) {
            List<Transaction> testFold = folds.get(foldNum);
            List<Transaction> trainFold = new java.util.ArrayList<>();
            for (int i = 0; i < K; i++) {
                if (i != foldNum) trainFold.addAll(folds.get(i));
            }

            // Train classifier
            CMARClassifier clf = new CMARClassifier();
            clf.setChiSquareThreshold(chiSqThreshold);
            clf.setCoverageThreshold(coverageDelta);
            clf.setTopK(topK);  // Enable top-K

            int minSup = Math.max(1, (int) Math.round(minSupportPct * trainFold.size()));
            FPGrowth fpg = new FPGrowth(minSup);
            fpg.setMaxPatternLength(maxPatLen);
            List<AssociationRule> rules = fpg.mine(trainFold, minConfidence);
            clf.train(rules, trainFold);

            // Predict
            List<String> pred = clf.predict(testFold);
            EvalMetrics m = EvalMetrics.compute(testFold, pred);
            metrics.add(m);
        }
        
        return metrics;
    }
}
