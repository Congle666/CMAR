import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark — WCBA Proper (v9) trên Truly Imbalanced Datasets.
 *
 * Áp dụng đầy đủ các kỹ thuật từ WCBA 2018 + các hướng đã có:
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │ Mining (FPGrowth)                                           │
 *   │   - H2: class-specific minSup                               │
 *   │   - H3: adaptive minConf                                    │
 *   │   - WCBA: Information Gain attribute weights → weighted_sup │
 *   └─────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │ Classification (CMARClassifier)                             │
 *   │   - WCBA: Strong + Spare rules (2-stage fallback)           │
 *   │   - WCBA: HM ranking trong Top-K (HM = 2·wsup·conf/(wsup+conf)) │
 *   │   - Stratified Top-K: K minority cao, K majority thấp        │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * Truly Imbalanced Datasets (max/min class ratio ≥ 3x):
 *   - lymph (4 cls, ratio 40.5x — extreme)
 *   - hepatitis (2 cls, ratio 3.8x — medical)
 *   - german (2 cls, ratio 2.3x — financial)
 *
 * Variants tested:
 *   - Baseline:   no improvements
 *   - WCBA-Light: H2 + H3
 *   - WCBA-Full:  H2 + H3 + WCBA weights + Strong/Spare + HM
 *   - WCBA-TopK:  WCBA-Full + Stratified Top-K (5, 15)
 *
 * File kết quả:
 *   result/v9_baseline_*.csv
 *   result/v9_light_*.csv     (H2+H3 only)
 *   result/v9_full_*.csv      (+ WCBA weights + spare + HM)
 *   result/v9_topk_*.csv      (+ stratified TopK)
 */
public class BenchmarkWCBA {

    static final String[][] DATASETS = {
        // { file, name, minSupportPct, paperCMAR, paperCBA, paperC45 }
        { "data/lymph.csv",      "lymph",     "0.05", "82.43", "77.03", "73.51" },
        { "data/hepatitis.csv",  "hepatitis", "0.05", "80.65", "81.29", "80.00" },
        { "data/german_disc.csv","german",    "0.06", "73.40", "73.40", "72.30" },
    };

    // H3 parameters
    static final double ADAPTIVE_CONF_FLOOR = 0.3;
    static final double ADAPTIVE_CONF_LIFT  = 5.0;

    // Stratified Top-K (minority gets up to KMAX, majority gets KMIN)
    static final int STRAT_K_MIN = 5;
    static final int STRAT_K_MAX = 15;

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("================================================================");
        System.out.println("  CMAR Benchmark — WCBA Proper (v9) on Truly Imbalanced Datasets");
        System.out.println("================================================================");
        System.out.println("  Datasets: lymph (40.5x), hepatitis (3.8x), german (2.3x)");
        System.out.println("  Variants: Baseline | WCBA-Light (H2+H3) | WCBA-Full | WCBA-TopK");
        System.out.println("================================================================\n");

        // --- Variant 1: Baseline ---
        System.out.println(">>> BASELINE — no improvements ...");
        Map<String, EvalMetrics> baseline = runVariant(
            "baseline", false, false, false, 0, 0,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
        writeCsvs(baseline, "v9_baseline");

        // --- Variant 2: WCBA-Light (H2 + H3 only) ---
        System.out.println("\n>>> WCBA-Light — H2 + H3 (proven winner from v6) ...");
        Map<String, EvalMetrics> light = runVariant(
            "light", true, false, false, 0, 0,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
        writeCsvs(light, "v9_light");

        // --- Variant 3: WCBA-Full (H2 + H3 + WCBA weights + Spare + HM) ---
        System.out.println("\n>>> WCBA-Full — H2 + H3 + IG weights + Spare rules + HM ranking ...");
        Map<String, EvalMetrics> full = runVariant(
            "full", true, true, true, 0, 0,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
        writeCsvs(full, "v9_full");

        // --- Variant 4: WCBA-TopK (Full + Stratified Top-K) ---
        System.out.println("\n>>> WCBA-TopK — WCBA-Full + Stratified Top-K (" + STRAT_K_MIN + ", " + STRAT_K_MAX + ") ...");
        Map<String, EvalMetrics> topk = runVariant(
            "topk", true, true, true, STRAT_K_MIN, STRAT_K_MAX,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);
        writeCsvs(topk, "v9_topk");

        // --- Comparison table ---
        System.out.println("\n" + "=".repeat(95));
        System.out.println("TONG KET v9 — Macro-F1 Comparison");
        System.out.println("=".repeat(95));
        System.out.printf("%-12s | %-10s | %-10s | %-10s | %-10s | Best%n",
            "Dataset", "Baseline", "Light", "Full", "TopK");
        System.out.println("-".repeat(95));
        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics b = baseline.get(name);
            EvalMetrics l = light.get(name);
            EvalMetrics f = full.get(name);
            EvalMetrics t = topk.get(name);
            if (b == null) continue;
            double[] f1s = {b.macroF1, l == null ? 0 : l.macroF1,
                            f == null ? 0 : f.macroF1, t == null ? 0 : t.macroF1};
            String[] labels = {"Baseline", "Light", "Full", "TopK"};
            int bestIdx = 0;
            for (int i = 1; i < 4; i++) if (f1s[i] > f1s[bestIdx]) bestIdx = i;
            System.out.printf("%-12s | %.4f    | %.4f    | %.4f    | %.4f    | %s (%+.4f)%n",
                name, f1s[0], f1s[1], f1s[2], f1s[3],
                labels[bestIdx], f1s[bestIdx] - f1s[0]);
        }

        // --- Per-class breakdown for each dataset ---
        for (String[] ds : DATASETS) {
            String name = ds[1];
            System.out.println("\n" + "=".repeat(95));
            System.out.println(name.toUpperCase() + " — Per-class F1");
            System.out.println("=".repeat(95));
            printPerClass(name, baseline, light, full, topk);
        }

        System.out.println("\nFile CSV xuat ra: result/v9_{baseline,light,full,topk}_*.csv");
    }

    private static Map<String, EvalMetrics> runVariant(
            String label,
            boolean useH2H3,        // enable H2 + H3
            boolean useWCBA,        // enable WCBA: weights + spare + HM
            boolean useTopK,        // enable Stratified Top-K
            int kMin, int kMax,
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

                double h2 = useH2H3 ? supPct : 0.0;
                double h3Floor = useH2H3 ? ADAPTIVE_CONF_FLOOR : 0.0;
                double h3Lift = useH2H3 ? ADAPTIVE_CONF_LIFT : 0.0;
                final boolean wcba = useWCBA;
                final int tkMin = kMin, tkMax = kMax;
                final boolean uTopK = useTopK;

                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K_FOLD, supPct, minConfidence, chiSqThreshold, coverageDelta,
                    seed, Integer.MAX_VALUE,
                    () -> {
                        CMARClassifier c = new CMARClassifier();
                        if (wcba) {
                            c.setUseSpareRules(true);
                            c.setUseHMRanking(true);
                        }
                        if (uTopK) {
                            c.setStratifiedTopK(tkMin, tkMax);
                            c.setTopK(tkMax);  // upper bound for selectTopKRulesPerClass loop
                        }
                        return c;
                    },
                    h2, h3Floor, h3Lift, wcba);

                EvalMetrics agg = EvalMetrics.average(foldMetrics);
                results.put(name, agg);
                System.out.printf("Acc=%.2f%% MacroF1=%.4f%n",
                    agg.accuracy * 100, agg.macroF1);
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

    private static void printPerClass(String dataset,
                                       Map<String, EvalMetrics> baseline,
                                       Map<String, EvalMetrics> light,
                                       Map<String, EvalMetrics> full,
                                       Map<String, EvalMetrics> topk) {
        EvalMetrics base = baseline.get(dataset);
        if (base == null) return;
        System.out.printf("%-18s | %-9s | %-9s | %-9s | %-9s%n",
            "Class(support)", "Baseline", "Light", "Full", "TopK");
        System.out.println("-".repeat(80));
        for (String cls : base.perClass.keySet()) {
            int sup = base.perClass.get(cls).support;
            double f1b = base.perClass.get(cls).f1;
            double f1l = light.get(dataset) != null && light.get(dataset).perClass.containsKey(cls)
                ? light.get(dataset).perClass.get(cls).f1 : 0;
            double f1f = full.get(dataset) != null && full.get(dataset).perClass.containsKey(cls)
                ? full.get(dataset).perClass.get(cls).f1 : 0;
            double f1t = topk.get(dataset) != null && topk.get(dataset).perClass.containsKey(cls)
                ? topk.get(dataset).perClass.get(cls).f1 : 0;
            System.out.printf("%-18s | %.4f   | %.4f   | %.4f   | %.4f%n",
                cls + "(" + sup + ")", f1b, f1l, f1f, f1t);
        }
    }
}
