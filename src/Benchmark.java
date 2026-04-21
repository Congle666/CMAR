import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs 10-fold stratified cross-validation on all UCI datasets
 * from the CMAR paper (Li, Han, Pei — ICDM 2001, Table 1).
 *
 * Collects per-fold {@link EvalMetrics} (Accuracy + Macro-F1 + Weighted-F1
 * + per-class P/R/F1) and emits two CSV files for later comparison:
 *   - result/baseline_metrics.csv       (1 row per dataset)
 *   - result/baseline_per_class.csv     (1 row per (dataset, class))
 *
 * Uses percentage-based minSupport (tuned per dataset) to match paper setup.
 */
public class Benchmark {

    // Paper results for comparison (from Table 1, ICDM 2001)
    // minSupportPct tuned per dataset to keep mining tractable.
    static final String[][] DATASETS = {
        // { file, name, minSupportPct, paperCMAR, paperCBA, paperC45, [maxPatternLength] }
        // Optional 7th col: cap pattern length to avoid pattern explosion on
        // high-dimensional datasets (zoo has 16 binary attrs → ~2^16 candidates)
        { "data/breast-w.csv",       "breast-w",    "0.02", "96.42", "96.28", "95.00" },
        { "data/cleve.csv",          "cleve",       "0.02", "82.18", "82.83", "78.24" },
        { "data/crx.csv",            "crx",         "0.04", "85.36", "84.93", "84.94" },
        { "data/diabetes.csv",       "diabetes",    "0.03", "75.81", "74.47", "74.18" },
        { "data/german_disc.csv",    "german",      "0.06", "73.40", "73.40", "72.30" },
        { "data/glass.csv",          "glass",       "0.01", "70.09", "67.76", "68.22" },
        { "data/heart.csv",          "heart",       "0.03", "82.59", "81.85", "80.74" },
        { "data/hepatitis.csv",      "hepatitis",   "0.05", "80.65", "81.29", "80.00" },
        { "data/horse.csv",          "horse",       "0.03", "82.61", "82.07", "82.61" },
        { "data/iris_disc.csv",      "iris",        "0.03", "94.00", "94.67", "95.33" },
        { "data/labor.csv",          "labor",       "0.05", "89.47", "86.33", "79.33" },
        { "data/led7.csv",           "led7",        "0.03", "71.90", "71.70", "73.50" },
        { "data/lymph.csv",          "lymph",       "0.05", "82.43", "77.03", "73.51" },
        { "data/mushroom_full.csv",  "mushroom",    "0.15", "100.00","100.00","100.00" },
        { "data/sonar.csv",          "sonar",       "0.05", "79.33", "76.92", "73.56" },
        { "data/tic-tac-toe.csv",    "tic-tac-toe", "0.003","99.27", "99.06", "99.37" },
        { "data/vehicle.csv",        "vehicle",     "0.03", "68.68", "67.73", "72.34" },
        { "data/waveform.csv",       "waveform",    "0.01", "80.17", "79.93", "78.10" },
        { "data/wine.csv",           "wine",        "0.03", "95.51", "95.51", "92.70" },
        { "data/zoo_h.csv",          "zoo",         "0.03", "96.04", "97.03", "93.07", "4" },
    };

    static final String OUT_METRICS_CSV   = "result/baseline_metrics.csv";
    static final String OUT_PER_CLASS_CSV = "result/baseline_per_class.csv";

    public static void main(String[] args) throws Exception {
        int K = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=================================================================");
        System.out.println("  CMAR Benchmark — 10-Fold Stratified CV + F1 Metrics");
        System.out.println("  Paper: Li, Han, Pei (ICDM 2001)");
        System.out.println("=================================================================\n");

        // Header
        System.out.printf("%-14s %5s %6s | %6s %7s %7s %5s | %6s %6s %6s  Diff%n",
            "Dataset", "N", "supPct", "Acc", "MacroF1", "WF1", "Std", "Paper", "CBA", "C4.5");
        System.out.println("-".repeat(90));

        // Aggregate containers for CSV output
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

                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K, supPct, minConfidence, chiSqThreshold, coverageDelta, seed, maxPatLen);

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

        // -----------------------------------------------------------------
        // Write CSV outputs
        // -----------------------------------------------------------------
        ResultWriter.writeMetricsCsv(aggregated, info, OUT_METRICS_CSV);
        ResultWriter.writePerClassCsv(aggregated, OUT_PER_CLASS_CSV);

        // -----------------------------------------------------------------
        // Summary
        // -----------------------------------------------------------------
        System.out.println("\n" + "=".repeat(90));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(90));
        if (count > 0) {
            System.out.printf("Datasets evaluated:          %d / %d%n", count, DATASETS.length);
            System.out.printf("Avg Accuracy (ours):         %.2f%%%n", totalOursAcc / count);
            System.out.printf("Avg Macro-F1 (ours):         %.4f%n",   totalOursF1 / count);
            System.out.printf("Avg Accuracy (paper CMAR):   %.2f%%%n", totalPaper / count);
            System.out.printf("Mean absolute diff vs paper: %.2f%%%n", totalDiff / count);
            System.out.printf("Within 5%% of paper:          %d / %d%n", within5, count);
        }
        System.out.println();
        System.out.println("CSV outputs:");
        System.out.println("  " + OUT_METRICS_CSV);
        System.out.println("  " + OUT_PER_CLASS_CSV);
    }
}
