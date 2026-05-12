import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark v11 — Full UCI suite (20 datasets) với Adaptive SMOTE.
 *
 * <p>3 variants:</p>
 * <ol>
 *   <li><b>Baseline</b> — CMAR gốc, không H2/H3, không SMOTE.</li>
 *   <li><b>Light</b> — CMAR + H2 (class-specific minSup) + H3 (adaptive minConf).</li>
 *   <li><b>Light+SMOTE</b> — Light + SMOTE-N adaptive (chỉ bật khi minority class quá nhỏ).</li>
 * </ol>
 *
 * <p>Adaptive SMOTE: bật khi {@code min_class_freq < SMOTE_TRIGGER (= 10)}.
 * Khi bật, SMOTE balance về {@code target_ratio = 1.0} (fully balanced).</p>
 */
public class BenchmarkSMOTEFull {

    /** Cùng tập 20 UCI datasets như Benchmark.java. */
    static final String[][] DATASETS = {
        // { file, name, minSupportPct, [maxPatternLength] }
        { "data/breast-w.csv",       "breast-w",    "0.02" },
        { "data/cleve.csv",          "cleve",       "0.02" },
        { "data/crx.csv",            "crx",         "0.04" },
        { "data/diabetes.csv",       "diabetes",    "0.03" },
        { "data/german_disc.csv",    "german",      "0.06" },
        { "data/glass.csv",          "glass",       "0.01" },
        { "data/heart.csv",          "heart",       "0.03" },
        { "data/hepatitis.csv",      "hepatitis",   "0.05" },
        { "data/horse.csv",          "horse",       "0.03" },
        { "data/iris_disc.csv",      "iris",        "0.03" },
        { "data/labor.csv",          "labor",       "0.05" },
        { "data/led7.csv",           "led7",        "0.03" },
        { "data/lymph.csv",          "lymph",       "0.05" },
        { "data/mushroom_full.csv",  "mushroom",    "0.15" },
        { "data/sonar.csv",          "sonar",       "0.05", "5" },
        { "data/tic-tac-toe.csv",    "tic-tac-toe", "0.003" },
        { "data/vehicle.csv",        "vehicle",     "0.03", "5" },
        { "data/waveform.csv",       "waveform",    "0.01", "5" },
        { "data/wine.csv",           "wine",        "0.03" },
        { "data/zoo_h.csv",          "zoo",         "0.03", "4" },
    };

    static final double ADAPTIVE_CONF_FLOOR = 0.3;
    static final double ADAPTIVE_CONF_LIFT  = 5.0;

    /** SMOTE bật khi minority class có ít hơn ngưỡng này records. */
    static final int    SMOTE_TRIGGER = 10;
    /** Target ratio khi SMOTE bật: 1.0 = fully balanced. */
    static final double SMOTE_RATIO   = 1.0;

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=================================================================");
        System.out.println("  CMAR v11 — Full UCI Benchmark (20 datasets) + Adaptive SMOTE");
        System.out.println("=================================================================");
        System.out.println("  Variants: Baseline / Light (H2+H3) / Light+SMOTE adaptive");
        System.out.println("  SMOTE trigger: min_class_freq < " + SMOTE_TRIGGER);
        System.out.println("  SMOTE target ratio: " + SMOTE_RATIO);
        System.out.println("=================================================================\n");

        System.out.println(">>> 1/3 BASELINE (no H2/H3, no SMOTE) ...");
        Map<String, EvalMetrics> baseline = runVariant(false, false,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        System.out.println("\n>>> 2/3 LIGHT (H2 + H3) ...");
        Map<String, EvalMetrics> light = runVariant(true, false,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        System.out.println("\n>>> 3/3 LIGHT + SMOTE (adaptive) ...");
        Map<String, EvalMetrics> smote = runVariant(true, true,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        // -------- Summary Table --------
        System.out.println("\n" + "=".repeat(95));
        System.out.println("TONG KET v11 — MacroF1 / MacroRecall (20 UCI datasets)");
        System.out.println("=".repeat(95));
        System.out.printf("%-13s | %-10s | %-10s | %-10s | %-12s | Best (Δ vs base)%n",
            "Dataset", "Baseline", "Light", "L+SMOTE", "SMOTE?");
        System.out.println("-".repeat(95));

        int winSmote = 0, winLight = 0, winBase = 0;
        double sumB = 0, sumL = 0, sumS = 0;
        int cnt = 0;
        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics b = baseline.get(name);
            EvalMetrics l = light.get(name);
            EvalMetrics s = smote.get(name);
            if (b == null || l == null || s == null) continue;
            double bf = b.macroF1, lf = l.macroF1, sf = s.macroF1;
            boolean smoteActive = isSmoteActive(s);
            String[] labels = {"Baseline", "Light", "L+SMOTE"};
            double[] f1s = {bf, lf, sf};
            int bestIdx = 0;
            for (int i = 1; i < 3; i++) if (f1s[i] > f1s[bestIdx]) bestIdx = i;
            if (bestIdx == 0) winBase++;
            else if (bestIdx == 1) winLight++;
            else winSmote++;
            sumB += bf; sumL += lf; sumS += sf; cnt++;
            System.out.printf("%-13s | %.4f    | %.4f    | %.4f    | %-12s | %s (%+.4f)%n",
                name, bf, lf, sf, (smoteActive ? "ON" : "off"),
                labels[bestIdx], f1s[bestIdx] - bf);
        }

        System.out.println("-".repeat(95));
        System.out.printf("AVG (n=%d)     | %.4f    | %.4f    | %.4f    | wins B:%d L:%d S:%d%n",
            cnt, sumB / cnt, sumL / cnt, sumS / cnt, winBase, winLight, winSmote);

        // Per-class breakdown chỉ in cho dataset SMOTE active
        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics s = smote.get(name);
            if (s == null || !isSmoteActive(s)) continue;
            System.out.println("\n" + "=".repeat(95));
            System.out.println(name.toUpperCase() + " — Per-class F1 & Recall (SMOTE active)");
            System.out.println("=".repeat(95));
            printPerClass(name, baseline, light, smote);
        }

        // Write CSVs
        writeCsvs(baseline, "v11_baseline");
        writeCsvs(light,    "v11_light");
        writeCsvs(smote,    "v11_smote");

        System.out.println("\nFile CSV xuat ra: result/v11_{baseline,light,smote}_*.csv");
    }

    /** Heuristic: SMOTE active nếu dataset có ít nhất một class với support < SMOTE_TRIGGER. */
    private static boolean isSmoteActive(EvalMetrics m) {
        if (m == null || m.perClass == null) return false;
        for (EvalMetrics.ClassMetrics c : m.perClass.values()) {
            if (c.support > 0 && c.support < SMOTE_TRIGGER) return true;
        }
        return false;
    }

    private static Map<String, EvalMetrics> runVariant(
            boolean useH2H3,
            boolean useSMOTE,
            int K_FOLD, double minConfidence, double chiSqThreshold,
            int coverageDelta, long seed) throws Exception {

        Map<String, EvalMetrics> results = new LinkedHashMap<>();

        for (String[] ds : DATASETS) {
            String file = ds[0];
            String name = ds[1];
            double supPct = Double.parseDouble(ds[2]);
            int maxLen = ds.length > 3 ? Integer.parseInt(ds[3]) : Integer.MAX_VALUE;
            System.out.print("  " + String.format("%-13s", name) + " ... ");
            try {
                List<Transaction> data = DatasetLoader.load(file);
                if (data.isEmpty()) { System.out.println("SKIP"); continue; }

                // Adaptive SMOTE: chỉ bật nếu có minority class < trigger
                double smoteRatio = 0.0;
                if (useSMOTE) {
                    int minFreq = computeMinClassFreq(data);
                    if (minFreq < SMOTE_TRIGGER) smoteRatio = SMOTE_RATIO;
                }

                double h2 = useH2H3 ? supPct : 0.0;
                double h3F = useH2H3 ? ADAPTIVE_CONF_FLOOR : 0.0;
                double h3L = useH2H3 ? ADAPTIVE_CONF_LIFT : 0.0;

                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K_FOLD, supPct, minConfidence, chiSqThreshold, coverageDelta,
                    seed, maxLen,
                    CMARClassifier::new,
                    h2, h3F, h3L, smoteRatio);

                EvalMetrics agg = EvalMetrics.average(foldMetrics);
                results.put(name, agg);
                System.out.printf("MacroF1=%.4f (SMOTE=%s)%n",
                    agg.macroF1, smoteRatio > 0 ? "ON" : "off");
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return results;
    }

    private static int computeMinClassFreq(List<Transaction> data) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (Transaction t : data) freq.merge(t.getClassLabel(), 1, Integer::sum);
        int min = Integer.MAX_VALUE;
        for (int v : freq.values()) if (v < min) min = v;
        return min;
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
                                       Map<String, EvalMetrics> smote) {
        EvalMetrics base = baseline.get(dataset);
        if (base == null) return;
        System.out.printf("%-22s | %-15s | %-15s | %-15s%n",
            "Class(support)", "Baseline F1/R", "Light F1/R", "L+SMOTE F1/R");
        System.out.println("-".repeat(85));
        for (String cls : base.perClass.keySet()) {
            int sup = base.perClass.get(cls).support;
            System.out.printf("%-22s", cls + "(" + sup + ")");
            for (Map<String, EvalMetrics> m : java.util.Arrays.asList(baseline, light, smote)) {
                EvalMetrics em = m.get(dataset);
                if (em != null && em.perClass.containsKey(cls)) {
                    System.out.printf(" | %.3f/%.3f   ",
                        em.perClass.get(cls).f1, em.perClass.get(cls).recall);
                } else {
                    System.out.printf(" | %-13s", "—");
                }
            }
            System.out.println();
        }
    }
}
