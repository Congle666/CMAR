import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark — Hướng 1 variant (Class-weighted χ²).
 *
 * Runs the same 10-fold stratified CV as {@link Benchmark} on all 20 UCI
 * datasets, but uses {@link CMARClassifierWeighted} — inverse-frequency
 * class weighting applied to the classify() scoring step.
 *
 * Outputs:
 *   result/v2_metrics.csv          — 1 row per dataset
 *   result/v2_per_class.csv        — 1 row per (dataset, class)
 *
 * Use {@code diff baseline_metrics.csv v2_metrics.csv} to compare.
 */
public class BenchmarkWeighted {

    // Same dataset configuration as Benchmark.java so results are directly
    // comparable. Keep this in sync.
    static final String[][] DATASETS = Benchmark.DATASETS;

    static final String OUT_METRICS_CSV   = "result/v2_metrics.csv";
    static final String OUT_PER_CLASS_CSV = "result/v2_per_class.csv";

    public static void main(String[] args) throws Exception {
        int K = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=================================================================");
        System.out.println("  CMAR Benchmark — Class-weighted χ² (Hướng 1)");
        System.out.println("  Same 10-fold stratified CV as baseline, different classify()");
        System.out.println("=================================================================\n");

        // Header
        System.out.printf("%-14s %5s %6s | %6s %7s %7s %5s | %6s %6s %6s  Diff%n",
            "Dataset", "N", "supPct", "Acc", "MacroF1", "WF1", "Std", "Paper", "CBA", "C4.5");
        System.out.println("-".repeat(90));

        Map<String, EvalMetrics> aggregated = new LinkedHashMap<>();
        Map<String, String> info = new LinkedHashMap<>();

        double totalOursAcc = 0, totalOursF1 = 0, totalPaper = 0;
        double totalDiff = 0;
        int count = 0;
        int within5 = 0;

        for (String[] ds : DATASETS) {
            String file = ds[0];
            String name = ds[1];
            double supPct = Double.parseDouble(ds[2]);
            double paperCMAR = Double.parseDouble(ds[3]);
            double paperCBA  = Double.parseDouble(ds[4]);
            double paperC45  = Double.parseDouble(ds[5]);
            int maxPatLen    = ds.length > 6 ? Integer.parseInt(ds[6]) : Integer.MAX_VALUE;

            System.out.println("\n>>> " + name + " (" + file + ") ...");

            try {
                List<Transaction> data = DatasetLoader.load(file);
                if (data.isEmpty()) {
                    System.out.println("    SKIPPED (empty dataset)");
                    continue;
                }

                // Key difference from Benchmark: pass CMARClassifierWeighted::new
                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K, supPct, minConfidence, chiSqThreshold, coverageDelta,
                    seed, maxPatLen, CMARClassifierWeighted::new);

                EvalMetrics agg = EvalMetrics.average(foldMetrics);
                aggregated.put(name, agg);
                info.put(name, data.size() + "|" + agg.perClass.size() + "|" + supPct);

                double accPct = agg.accuracy * 100;
                double stdPct = agg.accuracyStd * 100;
                double diff   = accPct - paperCMAR;

                System.out.printf("  => %-14s %5d %5.1f%% | %6.2f %7.4f %7.4f %5.2f | %6.2f %6.2f %6.2f  %+5.2f%n",
                    name, data.size(), supPct * 100,
                    accPct, agg.macroF1, agg.weightedF1, stdPct,
                    paperCMAR, paperCBA, paperC45, diff);

                totalOursAcc += accPct;
                totalOursF1  += agg.macroF1;
                totalPaper   += paperCMAR;
                totalDiff    += Math.abs(diff);
                count++;
                if (Math.abs(diff) <= 5.0) within5++;

            } catch (OutOfMemoryError e) {
                System.out.println("    OUT OF MEMORY — try increasing -Xmx or raising minSupport");
            } catch (Exception e) {
                System.out.println("    ERROR: " + e.getMessage());
                e.printStackTrace(System.out);
            }
        }

        ResultWriter.writeMetricsCsv(aggregated, info, OUT_METRICS_CSV);
        ResultWriter.writePerClassCsv(aggregated, OUT_PER_CLASS_CSV);

        System.out.println("\n" + "=".repeat(90));
        System.out.println("SUMMARY — Weighted χ² (Hướng 1)");
        System.out.println("=".repeat(90));
        if (count > 0) {
            System.out.printf("Datasets evaluated:          %d / %d%n", count, DATASETS.length);
            System.out.printf("Avg Accuracy (weighted):     %.2f%%%n", totalOursAcc / count);
            System.out.printf("Avg Macro-F1 (weighted):     %.4f%n",   totalOursF1 / count);
            System.out.printf("Avg Accuracy (paper CMAR):   %.2f%%%n", totalPaper / count);
            System.out.printf("Mean absolute diff vs paper: %.2f%%%n", totalDiff / count);
            System.out.printf("Within 5%% of paper:          %d / %d%n", within5, count);
        }
        System.out.println();
        System.out.println("CSV outputs:");
        System.out.println("  " + OUT_METRICS_CSV);
        System.out.println("  " + OUT_PER_CLASS_CSV);
        System.out.println();
        System.out.println("Compare with baseline:");
        System.out.println("  diff result/baseline_metrics.csv result/v2_metrics.csv");
    }
}
