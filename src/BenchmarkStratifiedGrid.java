import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark — Grid Tuning Stratified Top-K (v9.1).
 *
 * <p>v9 WCBA-TopK với (K_min=5, K_max=15) thắng lymph + hepatitis
 * nhưng THUA german (0.6556 vs Light 0.6903).</p>
 *
 * <p>Hypothesis: german (2-class, ratio 2.3x) cần K_min cao hơn để
 * giữ đủ rules cho class majority "good" (700 records).</p>
 *
 * <p>Grid search trên 6 cấu hình:</p>
 * <pre>
 *   #1: (3, 10)   - narrow + low
 *   #2: (5, 15)   - v9 default
 *   #3: (7, 15)   - higher floor
 *   #4: (5, 20)   - wider range
 *   #5: (7, 20)   - higher floor + wider
 *   #6: (10, 25)  - permissive (gần baseline)
 * </pre>
 *
 * <p>Datasets: lymph, hepatitis, german (truly imbalanced).</p>
 */
public class BenchmarkStratifiedGrid {

    static final String[][] DATASETS = {
        { "data/lymph.csv",       "lymph",     "0.05" },
        { "data/hepatitis.csv",   "hepatitis", "0.05" },
        { "data/german_disc.csv", "german",    "0.06" },
    };

    /** {K_min, K_max} configs to test. */
    static final int[][] CONFIGS = {
        {3, 10},
        {5, 15},   // v9 default
        {7, 15},
        {5, 20},
        {7, 20},
        {10, 25},
    };

    static final double ADAPTIVE_CONF_FLOOR = 0.3;
    static final double ADAPTIVE_CONF_LIFT  = 5.0;

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=========================================================");
        System.out.println("  CMAR v9.1 — Grid Tuning Stratified Top-K (K_min, K_max)");
        System.out.println("  Goal: Find config that wins ALL 3 truly imbalanced");
        System.out.println("=========================================================\n");

        // Baseline (no TopK, just H2+H3)
        System.out.println(">>> WCBA-Light (baseline cho so sánh, không TopK) ...");
        Map<String, EvalMetrics> light = runConfig("light", 0, 0,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        // Grid search
        Map<String, Map<String, EvalMetrics>> grid = new LinkedHashMap<>();
        for (int[] cfg : CONFIGS) {
            int kMin = cfg[0];
            int kMax = cfg[1];
            String label = "K(" + kMin + "," + kMax + ")";
            System.out.println("\n>>> Stratified TopK " + label + " ...");
            Map<String, EvalMetrics> results = runConfig(label, kMin, kMax,
                K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
            grid.put(label, results);
        }

        // -------- Summary --------
        System.out.println("\n" + "=".repeat(95));
        System.out.println("MACRO-F1 GRID — Tìm config WIN ALL 3 datasets");
        System.out.println("=".repeat(95));
        System.out.printf("%-12s | %-10s", "Dataset", "Light");
        for (int[] cfg : CONFIGS) System.out.printf(" | %-9s", "K(" + cfg[0] + "," + cfg[1] + ")");
        System.out.println();
        System.out.println("-".repeat(95));

        // Track win count per config for finding "best overall"
        Map<String, Integer> winCount = new LinkedHashMap<>();
        for (int[] cfg : CONFIGS) winCount.put("K(" + cfg[0] + "," + cfg[1] + ")", 0);

        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics baseM = light.get(name);
            double baseF1 = baseM == null ? 0 : baseM.macroF1;
            System.out.printf("%-12s | %-10.4f", name, baseF1);

            double bestF1 = baseF1;
            String bestLabel = "Light";
            for (int[] cfg : CONFIGS) {
                String label = "K(" + cfg[0] + "," + cfg[1] + ")";
                EvalMetrics r = grid.get(label).get(name);
                double f1 = r == null ? 0 : r.macroF1;
                System.out.printf(" | %.4f   ", f1);
                if (f1 > bestF1) {
                    bestF1 = f1;
                    bestLabel = label;
                    winCount.merge(label, 1, Integer::sum);
                }
            }
            System.out.printf(" | best=%s%n", bestLabel);
        }

        // Win count per config
        System.out.println("\n" + "=".repeat(95));
        System.out.println("CONFIG WIN COUNT (số dataset mà config đó là best vs Light)");
        System.out.println("=".repeat(95));
        for (Map.Entry<String, Integer> e : winCount.entrySet()) {
            System.out.printf("  %-12s: %d / %d datasets%n",
                e.getKey(), e.getValue(), DATASETS.length);
        }

        // Average F1 per config across all 3 datasets
        System.out.println("\n" + "=".repeat(95));
        System.out.println("AVERAGE MacroF1 across 3 datasets (CONFIG NÀO TỐT NHẤT TỔNG?)");
        System.out.println("=".repeat(95));
        double lightAvg = light.values().stream().mapToDouble(m -> m.macroF1).average().orElse(0);
        System.out.printf("  Light                         avg=%.4f%n", lightAvg);
        String bestOverall = "Light";
        double bestOverallF1 = lightAvg;
        for (int[] cfg : CONFIGS) {
            String label = "K(" + cfg[0] + "," + cfg[1] + ")";
            double avg = grid.get(label).values().stream()
                .mapToDouble(m -> m.macroF1).average().orElse(0);
            System.out.printf("  %-12s                  avg=%.4f%n", label, avg);
            if (avg > bestOverallF1) {
                bestOverallF1 = avg;
                bestOverall = label;
            }
        }
        System.out.println("\nWINNER overall (max avg MacroF1): " + bestOverall + " = " + bestOverallF1);

        // Write CSVs
        for (int[] cfg : CONFIGS) {
            String label = "K(" + cfg[0] + "," + cfg[1] + ")";
            String tag = "v91_k" + cfg[0] + "_" + cfg[1];
            Map<String, EvalMetrics> r = grid.get(label);
            Map<String, String> info = new LinkedHashMap<>();
            for (String[] ds : DATASETS) {
                EvalMetrics m = r.get(ds[1]);
                if (m != null) info.put(ds[1], "|" + m.perClass.size() + "|" + ds[2]);
            }
            ResultWriter.writeMetricsCsv(r, info, "result/" + tag + "_metrics.csv");
            ResultWriter.writePerClassCsv(r, "result/" + tag + "_per_class.csv");
        }
    }

    private static Map<String, EvalMetrics> runConfig(
            String label, int kMin, int kMax,
            int K_FOLD, double minConfidence, double chiSqThreshold,
            int coverageDelta, long seed) throws Exception {

        Map<String, EvalMetrics> results = new LinkedHashMap<>();
        for (String[] ds : DATASETS) {
            String file = ds[0];
            String name = ds[1];
            double supPct = Double.parseDouble(ds[2]);
            System.out.print("  " + name + " ... ");
            try {
                List<Transaction> data = DatasetLoader.load(file);
                if (data.isEmpty()) { System.out.println("SKIP"); continue; }

                final int tkMin = kMin, tkMax = kMax;
                final boolean useTopK = (kMin > 0 && kMax > kMin);

                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K_FOLD, supPct, minConfidence, chiSqThreshold, coverageDelta,
                    seed, Integer.MAX_VALUE,
                    () -> {
                        CMARClassifier c = new CMARClassifier();
                        c.setUseSpareRules(true);
                        c.setUseHMRanking(true);
                        if (useTopK) {
                            c.setStratifiedTopK(tkMin, tkMax);
                            c.setTopK(tkMax);  // upper bound for the loop
                        }
                        return c;
                    },
                    supPct, ADAPTIVE_CONF_FLOOR, ADAPTIVE_CONF_LIFT, true);

                EvalMetrics agg = EvalMetrics.average(foldMetrics);
                results.put(name, agg);
                System.out.printf("MacroF1=%.4f%n", agg.macroF1);
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
        return results;
    }
}
