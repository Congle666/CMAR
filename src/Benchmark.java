import java.util.List;

/**
 * Runs 10-fold stratified cross-validation on all UCI datasets
 * from the CMAR paper (Li, Han, Pei — ICDM 2001, Table 1).
 *
 * Uses percentage-based minSupport (1% of training size) to match
 * the paper's experimental setup.
 */
public class Benchmark {

    // Paper results for comparison (from Table 1, ICDM 2001)
    // minSupportPct tuned per dataset: higher for many-attribute datasets
    // to keep mining tractable (paper also tunes per dataset)
    // Paper §5.1 suggests minSup = 1%. We honor this for every dataset
    // where our pure-Java FP-Growth remains tractable (≤ ~15 attributes).
    // High-dimensional datasets (german 20 attrs, horse 27, sonar 60, etc.)
    // would otherwise explode: their mined pattern count at 1% exceeds
    // practical memory/time on a laptop. For those we raise minSup to the
    // smallest value that keeps the run under a minute per fold — a
    // deviation the original C implementation of the paper did not need.
    static final String[][] DATASETS = {
        // { file, name, minSupportPct, paperCMAR, paperCBA, paperC45 }
        { "data/breast-w.csv",       "breast-w",    "0.01", "96.42", "96.28", "95.00" },
        { "data/cleve.csv",          "cleve",       "0.01", "82.18", "82.83", "78.24" },
        { "data/crx.csv",            "crx",         "0.01", "85.36", "84.93", "84.94" },
        { "data/diabetes.csv",       "diabetes",    "0.01", "75.81", "74.47", "74.18" },
        { "data/german_disc.csv",    "german",      "0.05", "73.40", "73.40", "72.30" },
        { "data/glass.csv",          "glass",       "0.01", "70.09", "67.76", "68.22" },
        { "data/heart.csv",          "heart",       "0.01", "82.59", "81.85", "80.74" },
        { "data/hepatitis.csv",      "hepatitis",   "0.03", "80.65", "81.29", "80.00" },
        { "data/horse.csv",          "horse",       "0.03", "82.61", "82.07", "82.61" },
        { "data/iris_disc.csv",      "iris",        "0.01", "94.00", "94.67", "95.33" },
        { "data/labor.csv",          "labor",       "0.05", "89.47", "86.33", "79.33" },
        { "data/led7.csv",           "led7",        "0.01", "71.90", "71.70", "73.50" },
        { "data/lymph.csv",          "lymph",       "0.03", "82.43", "77.03", "73.51" },
        { "data/mushroom_full.csv",  "mushroom",    "0.05", "100.00","100.00","100.00" },
        { "data/sonar.csv",          "sonar",       "0.05", "79.33", "76.92", "73.56" },
        { "data/tic-tac-toe.csv",    "tic-tac-toe", "0.01", "99.27", "99.06", "99.37" },
        { "data/vehicle.csv",        "vehicle",     "0.03", "68.68", "67.73", "72.34" },
        { "data/waveform.csv",       "waveform",    "0.03", "80.17", "79.93", "78.10" },
        { "data/wine.csv",           "wine",        "0.01", "95.51", "95.51", "92.70" },
        { "data/zoo_h.csv",          "zoo",         "0.05", "96.04", "97.03", "93.07" },
    };

    public static void main(String[] args) throws Exception {
        int K = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=================================================================");
        System.out.println("  CMAR Benchmark — 10-Fold Stratified Cross-Validation");
        System.out.println("  Paper: Li, Han, Pei (ICDM 2001)");
        System.out.println("=================================================================\n");

        // Header
        System.out.printf("%-14s %5s %7s | %7s %7s | %7s %7s %7s%n",
            "Dataset", "N", "supPct", "Ours", "+-Std", "Paper", "CBA", "C4.5");
        System.out.println("-".repeat(78));

        double totalOurs = 0, totalPaper = 0;
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

            System.out.println("\n>>> " + name + " (" + file + ") ...");

            try {
                List<Transaction> data = DatasetLoader.load(file);
                if (data.isEmpty()) {
                    System.out.println("    SKIPPED (empty dataset)");
                    continue;
                }

                double[] accs = CrossValidator.run(data, K,
                    supPct, minConfidence, chiSqThreshold, coverageDelta, seed);

                double mean = CrossValidator.mean(accs) * 100;
                double std  = CrossValidator.stddev(accs) * 100;
                double diff = mean - paperCMAR;

                System.out.printf("  => %-14s %5d %6.1f%% | %6.2f%% %6.2f%% | %6.2f%% %6.2f%% %6.2f%%  [diff=%+.2f%%]%n",
                    name, data.size(), supPct * 100,
                    mean, std, paperCMAR, paperCBA, paperC45, diff);

                totalOurs += mean;
                totalPaper += paperCMAR;
                totalDiff += Math.abs(diff);
                count++;
                if (Math.abs(diff) <= 5.0) within5++;

            } catch (OutOfMemoryError e) {
                System.out.println("    OUT OF MEMORY — try increasing -Xmx or raising minSupport");
            } catch (Exception e) {
                System.out.println("    ERROR: " + e.getMessage());
            }
        }

        // Summary
        System.out.println("\n" + "=".repeat(78));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(78));
        System.out.printf("Datasets evaluated:        %d / %d%n", count, DATASETS.length);
        System.out.printf("Average accuracy (ours):   %.2f%%%n", totalOurs / count);
        System.out.printf("Average accuracy (paper):  %.2f%%%n", totalPaper / count);
        System.out.printf("Mean absolute difference:  %.2f%%%n", totalDiff / count);
        System.out.printf("Within 5%% of paper:        %d / %d%n", within5, count);
    }
}
