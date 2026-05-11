import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark — HM + Top-K Combo (v7).
 *
 * Kết hợp 2 kỹ thuật để cải tiến F1/Recall trên dataset mất cân bằng:
 *   1. HM Ranking (WCBA 2018) — sort rules theo Harmonic Mean của
 *      support và confidence: HM = 2 × sup × conf / (sup + conf).
 *      Parameter-free, balance sup và conf tự nhiên.
 *   2. Top-K Rules Per Class — mỗi class chỉ giữ K luật tốt nhất
 *      theo HM, tự cân bằng majority vs minority.
 *
 * Test trên 5 dataset MẤT CÂN BẰNG:
 *   lymph (4 cls, fibrosis 2.7%), glass (6 cls, vehicle_float 7.9%),
 *   vehicle (4 cls), hepatitis (2 cls, DIE 20.6%), german (2 cls, bad 30%)
 *
 * K = {3, 5, 7, 10} → 4 files output mỗi cấu hình.
 *
 * File kết quả:
 *   result/v7_k{K}_metrics.csv
 *   result/v7_k{K}_per_class.csv
 */
public class BenchmarkHMTopK {

    // 5 imbalanced datasets — mục tiêu chính cho cải tiến F1/Recall
    static final String[][] DATASETS = {
        // { file, name, minSupportPct, paperCMAR, paperCBA, paperC45, [maxPatternLength] }
        { "data/lymph.csv",     "lymph",     "0.05", "82.43", "77.03", "73.51" },
        { "data/glass.csv",     "glass",     "0.01", "70.09", "67.76", "68.22" },
        { "data/vehicle.csv",   "vehicle",   "0.03", "68.68", "67.73", "72.34", "5" },
        { "data/hepatitis.csv", "hepatitis", "0.05", "80.65", "81.29", "80.00" },
        { "data/german_disc.csv","german",   "0.06", "73.40", "73.40", "72.30" },
    };

    static final int[] K_VALUES = {3, 5, 7, 10};
    static final double DEFAULT_MIN_HM = 0.0;   // không filter — chỉ ranking

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=================================================================");
        System.out.println("  CMAR Benchmark — HM Ranking + Top-K Combo (v7)");
        System.out.println("  Inspired by WCBA 2018 + Top-K rules per class");
        System.out.println("  Datasets: 5 IMBALANCED (lymph, glass, vehicle, hepatitis, german)");
        System.out.println("=================================================================\n");

        // BASELINE (no HM, no TopK) — for comparison
        System.out.println(">>> BASELINE run (no HM, no Top-K) ...");
        Map<String, EvalMetrics> baselineResults = runVariant(
            "baseline", 0, false, 0.0,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        // K variants
        Map<Integer, Map<String, EvalMetrics>> kResults = new LinkedHashMap<>();
        for (int K : K_VALUES) {
            System.out.println("\n>>> Variant: Top-K=" + K + " + HM ranking ...");
            Map<String, EvalMetrics> results = runVariant(
                "k" + K, K, true, DEFAULT_MIN_HM,
                K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
            kResults.put(K, results);

            // Write CSV for this K
            writeCsvs(results, "v7_k" + K);
        }

        // Write baseline csvs
        writeCsvs(baselineResults, "v7_baseline");

        // ========== COMPARISON SUMMARY ==========
        System.out.println("\n" + "=".repeat(90));
        System.out.println("TONG KET — So sanh Baseline vs HM+TopK (Macro-F1)");
        System.out.println("=".repeat(90));
        System.out.printf("%-12s | %-10s", "Dataset", "Baseline");
        for (int K : K_VALUES) System.out.printf(" | K=%-3d ", K);
        System.out.printf(" | %-10s%n", "Best K");
        System.out.println("-".repeat(90));

        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics base = baselineResults.get(name);
            if (base == null) { continue; }
            System.out.printf("%-12s | %-10.4f", name, base.macroF1);
            int bestK = 0;
            double bestF1 = base.macroF1;
            for (int K : K_VALUES) {
                EvalMetrics r = kResults.get(K).get(name);
                double f1 = r == null ? 0 : r.macroF1;
                System.out.printf(" | %.4f", f1);
                if (f1 > bestF1) { bestF1 = f1; bestK = K; }
            }
            System.out.printf(" | K=%-3d (%.4f)%n",
                bestK == 0 ? -1 : bestK, bestF1);
        }

        // Per-class breakdown for lymph (key target)
        System.out.println("\n" + "=".repeat(90));
        System.out.println("LYMPH — Per-class F1 (key minority target)");
        System.out.println("=".repeat(90));
        printPerClassComparison("lymph", baselineResults, kResults);

        System.out.println("\nFile CSV xuat ra: result/v7_baseline_*.csv, result/v7_k{3,5,7,10}_*.csv");
    }

    /**
     * Chạy 10-fold CV trên 5 imbalanced datasets với cấu hình cho trước.
     */
    private static Map<String, EvalMetrics> runVariant(
            String label, int topK, boolean useHMRanking, double minHM,
            int K_FOLD, double minConfidence, double chiSqThreshold,
            int coverageDelta, long seed) throws Exception {

        Map<String, EvalMetrics> results = new LinkedHashMap<>();

        for (String[] ds : DATASETS) {
            String file = ds[0];
            String name = ds[1];
            double supPct = Double.parseDouble(ds[2]);
            int maxPatLen = ds.length > 6 ? Integer.parseInt(ds[6]) : Integer.MAX_VALUE;

            System.out.print("  " + name + " ... ");
            try {
                List<Transaction> data = DatasetLoader.load(file);
                if (data.isEmpty()) { System.out.println("SKIP"); continue; }

                // Build classifier with HM + Top-K config
                final int tK = topK;
                final boolean uHM = useHMRanking;
                final double mHM = minHM;

                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K_FOLD, supPct, minConfidence, chiSqThreshold, coverageDelta,
                    seed, maxPatLen,
                    () -> {
                        CMARClassifier c = new CMARClassifier();
                        c.setTopK(tK);
                        c.setUseHMRanking(uHM);
                        c.setMinHM(mHM);
                        return c;
                    });

                EvalMetrics agg = EvalMetrics.average(foldMetrics);
                results.put(name, agg);
                System.out.printf("Acc=%.2f%% MacroF1=%.4f%n",
                    agg.accuracy * 100, agg.macroF1);
            } catch (OutOfMemoryError e) {
                System.out.println("OOM");
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
        return results;
    }

    private static void writeCsvs(Map<String, EvalMetrics> results, String tag) throws Exception {
        Map<String, String> info = new LinkedHashMap<>();
        for (String[] ds : DATASETS) {
            EvalMetrics m = results.get(ds[1]);
            if (m != null) {
                info.put(ds[1], "|" + m.perClass.size() + "|" + ds[2]);
            }
        }
        ResultWriter.writeMetricsCsv(results, info, "result/" + tag + "_metrics.csv");
        ResultWriter.writePerClassCsv(results, "result/" + tag + "_per_class.csv");
    }

    private static void printPerClassComparison(
            String dataset,
            Map<String, EvalMetrics> baselineResults,
            Map<Integer, Map<String, EvalMetrics>> kResults) {
        EvalMetrics base = baselineResults.get(dataset);
        if (base == null) return;

        System.out.printf("%-15s | %-10s", "Class", "Baseline");
        for (int K : K_VALUES) System.out.printf(" | K=%-3d ", K);
        System.out.println();
        System.out.println("-".repeat(80));

        for (String cls : base.perClass.keySet()) {
            System.out.printf("%-15s | %.4f    ", cls, base.perClass.get(cls).f1);
            for (int K : K_VALUES) {
                EvalMetrics r = kResults.get(K).get(dataset);
                if (r != null && r.perClass.containsKey(cls)) {
                    System.out.printf("| %.4f ", r.perClass.get(cls).f1);
                } else {
                    System.out.printf("| %-7s", "—");
                }
            }
            System.out.println();
        }
    }
}
