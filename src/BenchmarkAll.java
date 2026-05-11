import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark — COMBO CUỐI: H2 + H3 + HM + Top-K (v8).
 *
 * Kết hợp 4 kỹ thuật cải tiến đồng thời trên 5 imbalanced datasets:
 *   1. H2 (Class-specific minSup): minSup(c) = max(2, minSupPct × freq(c))
 *      → sinh được rules cho minority class.
 *   2. H3 (Adaptive minConf):     minConf(c) = min(0.5, max(0.3, 5×freq(c)/N))
 *      → giữ được rules có confidence thấp cho minority.
 *   3. HM Ranking (WCBA 2018): rank rules trong Top-K theo Harmonic Mean
 *      của (sup, conf) thay vì chi-square.
 *   4. Top-K Rules Per Class: mỗi class giữ K luật HM cao nhất → balanced.
 *
 * Câu chuyện research:
 *   - H2+H3 sinh được rules cho minority (lymph fibrosis: F1 0→0.67 ở v6)
 *   - HM+TopK alone không sinh thêm rules (v7 thất bại trên lymph)
 *   - Combo: H2+H3 sinh + HM+TopK chọn balanced → kỳ vọng tối ưu cả 2 mặt
 *
 * Datasets: lymph, glass, vehicle, hepatitis, german (5 imbalanced)
 * K values: {3, 5, 7, 10}
 *
 * File kết quả:
 *   result/v8_baseline_*.csv  — baseline (không có cải tiến nào)
 *   result/v8_k{K}_*.csv       — combo với K khác nhau
 */
public class BenchmarkAll {

    // 5 imbalanced datasets — focus của thesis về F1/Recall
    static final String[][] DATASETS = {
        // { file, name, minSupportPct, paperCMAR, paperCBA, paperC45, [maxPatternLength] }
        { "data/lymph.csv",     "lymph",     "0.05", "82.43", "77.03", "73.51" },
        { "data/glass.csv",     "glass",     "0.01", "70.09", "67.76", "68.22" },
        { "data/vehicle.csv",   "vehicle",   "0.03", "68.68", "67.73", "72.34", "5" },
        { "data/hepatitis.csv", "hepatitis", "0.05", "80.65", "81.29", "80.00" },
        { "data/german_disc.csv","german",   "0.06", "73.40", "73.40", "72.30" },
    };

    static final int[] K_VALUES = {3, 5, 7, 10};

    // H3 (Adaptive minConf) parameters
    static final double ADAPTIVE_CONF_FLOOR = 0.3;
    static final double ADAPTIVE_CONF_LIFT  = 5.0;

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=================================================================");
        System.out.println("  CMAR Benchmark — COMBO CUỐI (v8): H2 + H3 + HM + Top-K");
        System.out.println("=================================================================");
        System.out.println("  H2: minSup(c) = max(2, supPct × freq(c))");
        System.out.println("  H3: minConf(c) = min(0.5, max(0.3, 5 × freq(c)/N))");
        System.out.println("  HM: rank Top-K rules by Harmonic Mean(sup, conf)");
        System.out.println("  Top-K: K = {3, 5, 7, 10}");
        System.out.println("  Datasets: lymph, glass, vehicle, hepatitis, german (5 imbalanced)");
        System.out.println("=================================================================\n");

        // ============ BASELINE: không cải tiến nào ============
        System.out.println(">>> BASELINE (no H2, no H3, no HM, no TopK) ...");
        Map<String, EvalMetrics> baseline = runVariant(
            0, false, 0.0,            // topK=0, no HM
            0.0, 0.0, 0.0,            // no H2, no H3
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
        writeCsvs(baseline, "v8_baseline");

        // ============ COMBO: H2 + H3 + HM + Top-K cho mỗi K ============
        Map<Integer, Map<String, EvalMetrics>> kResults = new LinkedHashMap<>();
        for (int K : K_VALUES) {
            System.out.println("\n>>> COMBO H2+H3+HM+Top-K=" + K + " ...");
            Map<String, EvalMetrics> results = runComboVariant(
                K, K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
            kResults.put(K, results);
            writeCsvs(results, "v8_k" + K);
        }

        // ============ COMPARISON ============
        System.out.println("\n" + "=".repeat(95));
        System.out.println("TONG KET v8 — COMBO CUỐI vs Baseline (Macro-F1)");
        System.out.println("=".repeat(95));
        System.out.printf("%-12s | %-10s", "Dataset", "Baseline");
        for (int K : K_VALUES) System.out.printf(" | K=%-3d   ", K);
        System.out.printf(" | %-15s%n", "Best (Δ vs base)");
        System.out.println("-".repeat(95));

        double sumBaseF1 = 0, sumBestF1 = 0;
        int count = 0;

        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics base = baseline.get(name);
            if (base == null) continue;
            System.out.printf("%-12s | %-10.4f", name, base.macroF1);
            int bestK = 0;
            double bestF1 = base.macroF1;
            for (int K : K_VALUES) {
                EvalMetrics r = kResults.get(K).get(name);
                double f1 = r == null ? 0 : r.macroF1;
                System.out.printf(" | %.4f  ", f1);
                if (f1 > bestF1) { bestF1 = f1; bestK = K; }
            }
            double delta = bestF1 - base.macroF1;
            System.out.printf(" | %s%+.4f%n",
                bestK == 0 ? "Base " : "K=" + bestK + "  ", delta);
            sumBaseF1 += base.macroF1;
            sumBestF1 += bestF1;
            count++;
        }
        System.out.println("-".repeat(95));
        System.out.printf("%-12s | %-10.4f | (best avg across K)            | Δ=%+.4f%n",
            "AVERAGE", sumBaseF1 / count, (sumBestF1 - sumBaseF1) / count);

        // ============ Per-class breakdown — LYMPH ============
        System.out.println("\n" + "=".repeat(95));
        System.out.println("LYMPH — Per-class F1 (critical: fibrosis 4 records, normal 2)");
        System.out.println("=".repeat(95));
        printPerClassComparison("lymph", baseline, kResults);

        // ============ Per-class breakdown — Glass ============
        System.out.println("\n" + "=".repeat(95));
        System.out.println("GLASS — Per-class F1");
        System.out.println("=".repeat(95));
        printPerClassComparison("glass", baseline, kResults);

        System.out.println("\nFile CSV xuat ra: result/v8_baseline_*.csv, result/v8_k{3,5,7,10}_*.csv");
    }

    /**
     * Chạy variant với combo H2+H3+HM+TopK.
     */
    private static Map<String, EvalMetrics> runComboVariant(
            int topK,
            int K_FOLD, double minConfidence, double chiSqThreshold,
            int coverageDelta, long seed) throws Exception {
        return runVariant(
            topK, true, 0.0,           // useHM=true, minHM=0
            -1.0, ADAPTIVE_CONF_FLOOR, ADAPTIVE_CONF_LIFT,  // H2 use supPct (-1 sentinel), H3 floor & lift
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
    }

    /**
     * Generic variant runner. Cho phép bật/tắt từng kỹ thuật.
     *
     * @param topK              Top-K rules per class (0 = disable)
     * @param useHM             rank top-K by HM (true) vs chi-square (false)
     * @param minHM             filter rules below HM (0 = disable)
     * @param h2Fraction        H2 class-specific minSup fraction (-1 = use supPct, 0 = disable)
     * @param h3Floor           H3 adaptive minConf floor (0 = disable H3)
     * @param h3Lift            H3 adaptive minConf lift
     */
    private static Map<String, EvalMetrics> runVariant(
            int topK, boolean useHM, double minHM,
            double h2Fraction, double h3Floor, double h3Lift,
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

                // H2 fraction: nếu = -1 thì dùng supPct của dataset
                double classMinSupFraction = (h2Fraction < 0) ? supPct : h2Fraction;

                final int tK = topK;
                final boolean uHM = useHM;
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
                    },
                    classMinSupFraction,    // H2
                    h3Floor,                // H3 floor
                    h3Lift);                // H3 lift

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
            if (m != null) info.put(ds[1], "|" + m.perClass.size() + "|" + ds[2]);
        }
        ResultWriter.writeMetricsCsv(results, info, "result/" + tag + "_metrics.csv");
        ResultWriter.writePerClassCsv(results, "result/" + tag + "_per_class.csv");
    }

    private static void printPerClassComparison(
            String dataset,
            Map<String, EvalMetrics> baseline,
            Map<Integer, Map<String, EvalMetrics>> kResults) {
        EvalMetrics base = baseline.get(dataset);
        if (base == null) return;

        System.out.printf("%-18s | %-9s", "Class", "Baseline");
        for (int K : K_VALUES) System.out.printf(" | K=%-3d   ", K);
        System.out.println();
        System.out.println("-".repeat(80));

        for (String cls : base.perClass.keySet()) {
            int sup = base.perClass.get(cls).support;
            System.out.printf("%-18s | %.4f   ", cls + "(" + sup + ")", base.perClass.get(cls).f1);
            for (int K : K_VALUES) {
                EvalMetrics r = kResults.get(K).get(dataset);
                if (r != null && r.perClass.containsKey(cls)) {
                    System.out.printf("| %.4f  ", r.perClass.get(cls).f1);
                } else {
                    System.out.printf("| %-7s ", "—");
                }
            }
            System.out.println();
        }
    }
}
