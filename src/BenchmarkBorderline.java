import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark v12 — So sánh Vanilla SMOTE vs Borderline-SMOTE trên 20 UCI datasets.
 *
 * <p>4 variants:</p>
 * <ol>
 *   <li><b>Baseline</b> — CMAR gốc, không H2, không SMOTE</li>
 *   <li><b>Light</b> — CMAR + H2 (class-specific minSup)</li>
 *   <li><b>L+SMOTE</b> — Light + Vanilla SMOTE-N (Chawla 2002)</li>
 *   <li><b>L+Borderline</b> — Light + Borderline-SMOTE-N (Han 2005)</li>
 * </ol>
 *
 * <p>Mục tiêu: chứng minh Borderline-SMOTE cho Accuracy KHÔNG giảm + F1/Recall tăng.</p>
 */
public class BenchmarkBorderline {

    static final String[][] DATASETS = {
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

    static final int    SMOTE_TRIGGER = 10;
    static final double SMOTE_RATIO   = 1.0;

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("=================================================================");
        System.out.println("  CMAR v12 — Vanilla SMOTE vs Borderline-SMOTE (Han 2005)");
        System.out.println("=================================================================");
        System.out.println("  4 variants × 20 UCI datasets × 10-fold stratified CV");
        System.out.println("  SMOTE trigger: min_class_freq < " + SMOTE_TRIGGER);
        System.out.println("=================================================================\n");

        System.out.println(">>> 1/4 BASELINE (no H2, no SMOTE) ...");
        Map<String, EvalMetrics> baseline = runVariant(false, false, false,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        System.out.println("\n>>> 2/4 LIGHT (H2 only) ...");
        Map<String, EvalMetrics> light = runVariant(true, false, false,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        System.out.println("\n>>> 3/4 LIGHT + VANILLA SMOTE (Chawla 2002) ...");
        Map<String, EvalMetrics> smote = runVariant(true, true, false,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        System.out.println("\n>>> 4/4 LIGHT + BORDERLINE-SMOTE (Han 2005) ...");
        Map<String, EvalMetrics> borderline = runVariant(true, true, true,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        // -------- Summary Table --------
        System.out.println("\n" + "=".repeat(110));
        System.out.println("TONG KET v12 — Acc / MacroF1 / MacroRecall (20 UCI datasets)");
        System.out.println("=".repeat(110));
        System.out.printf("%-13s | %-15s | %-15s | %-15s | %-15s | SMOTE? | Best F1%n",
            "Dataset", "Baseline", "Light", "L+SMOTE", "L+Borderline");
        System.out.println("-".repeat(110));

        double sumAccB = 0, sumAccL = 0, sumAccS = 0, sumAccBd = 0;
        double sumF1B = 0, sumF1L = 0, sumF1S = 0, sumF1Bd = 0;
        int cnt = 0;
        int winSmote = 0, winBorderline = 0, winLight = 0, winBase = 0, tied = 0;

        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics b = baseline.get(name);
            EvalMetrics l = light.get(name);
            EvalMetrics s = smote.get(name);
            EvalMetrics bd = borderline.get(name);
            if (b == null || l == null || s == null || bd == null) continue;

            boolean smoteActive = isSmoteActive(bd);
            double[] f1s = {b.macroF1, l.macroF1, s.macroF1, bd.macroF1};
            String[] labels = {"Baseline", "Light", "SMOTE", "Borderline"};
            int bestIdx = 0;
            for (int i = 1; i < 4; i++) if (f1s[i] > f1s[bestIdx]) bestIdx = i;
            switch (bestIdx) {
                case 0: winBase++; break;
                case 1: winLight++; break;
                case 2: winSmote++; break;
                case 3: winBorderline++; break;
            }
            // Check ties (within 0.0001)
            int bestCount = 0;
            for (double f1 : f1s) if (Math.abs(f1 - f1s[bestIdx]) < 0.0001) bestCount++;
            if (bestCount > 1) tied++;

            sumAccB += b.accuracy; sumAccL += l.accuracy; sumAccS += s.accuracy; sumAccBd += bd.accuracy;
            sumF1B += b.macroF1; sumF1L += l.macroF1; sumF1S += s.macroF1; sumF1Bd += bd.macroF1;
            cnt++;

            System.out.printf("%-13s | A=%.4f F=%.4f | A=%.4f F=%.4f | A=%.4f F=%.4f | A=%.4f F=%.4f | %-6s | %s%n",
                name,
                b.accuracy, b.macroF1,
                l.accuracy, l.macroF1,
                s.accuracy, s.macroF1,
                bd.accuracy, bd.macroF1,
                (smoteActive ? "ON" : "off"),
                labels[bestIdx]);
        }

        System.out.println("-".repeat(110));
        System.out.printf("AVG (n=%d)     | A=%.4f F=%.4f | A=%.4f F=%.4f | A=%.4f F=%.4f | A=%.4f F=%.4f%n",
            cnt,
            sumAccB / cnt, sumF1B / cnt,
            sumAccL / cnt, sumF1L / cnt,
            sumAccS / cnt, sumF1S / cnt,
            sumAccBd / cnt, sumF1Bd / cnt);

        System.out.println("\n" + "=".repeat(110));
        System.out.println("WIN COUNT (best F1)");
        System.out.println("=".repeat(110));
        System.out.printf("  Baseline:     %d wins%n", winBase);
        System.out.printf("  Light:        %d wins%n", winLight);
        System.out.printf("  SMOTE:        %d wins%n", winSmote);
        System.out.printf("  Borderline:   %d wins%n", winBorderline);
        System.out.printf("  Ties:         %d datasets%n", tied);

        // Per-class breakdown — chỉ in cho datasets SMOTE active
        System.out.println("\n" + "=".repeat(110));
        System.out.println("PER-CLASS DETAIL (datasets có SMOTE active)");
        System.out.println("=".repeat(110));
        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics bd = borderline.get(name);
            if (bd == null || !isSmoteActive(bd)) continue;
            System.out.println("\n--- " + name.toUpperCase() + " ---");
            printPerClass(name, baseline, light, smote, borderline);
        }

        // Write CSVs
        writeCsvs(baseline,   "v12_baseline");
        writeCsvs(light,      "v12_light");
        writeCsvs(smote,      "v12_smote");
        writeCsvs(borderline, "v12_borderline");

        System.out.println("\nFile CSV xuat ra: result/v12_{baseline,light,smote,borderline}_*.csv");
    }

    private static boolean isSmoteActive(EvalMetrics m) {
        if (m == null || m.perClass == null) return false;
        for (EvalMetrics.ClassMetrics c : m.perClass.values()) {
            if (c.support > 0 && c.support < SMOTE_TRIGGER) return true;
        }
        return false;
    }

    private static Map<String, EvalMetrics> runVariant(
            boolean useH2,
            boolean useSMOTE,
            boolean useBorderline,
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

                double smoteRatio = 0.0;
                if (useSMOTE) {
                    int minFreq = computeMinClassFreq(data);
                    if (minFreq < SMOTE_TRIGGER) smoteRatio = SMOTE_RATIO;
                }

                double h2 = useH2 ? supPct : 0.0;

                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K_FOLD, supPct, minConfidence, chiSqThreshold, coverageDelta,
                    seed, maxLen,
                    CMARClassifier::new,
                    h2, 0.0, 0.0, smoteRatio,
                    useBorderline);

                EvalMetrics agg = EvalMetrics.average(foldMetrics);
                results.put(name, agg);
                System.out.printf("Acc=%.4f F1=%.4f (SMOTE=%s)%n",
                    agg.accuracy, agg.macroF1, smoteRatio > 0 ? "ON" : "off");
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
                                       Map<String, EvalMetrics> smote,
                                       Map<String, EvalMetrics> borderline) {
        EvalMetrics base = baseline.get(dataset);
        if (base == null) return;
        System.out.printf("%-22s | %-13s | %-13s | %-13s | %-13s%n",
            "Class(support)", "Baseline F1/R", "Light F1/R", "SMOTE F1/R", "Borderline F1/R");
        System.out.println("-".repeat(100));
        for (String cls : base.perClass.keySet()) {
            int sup = base.perClass.get(cls).support;
            System.out.printf("%-22s", cls + "(" + sup + ")");
            for (Map<String, EvalMetrics> m : java.util.Arrays.asList(baseline, light, smote, borderline)) {
                EvalMetrics em = m.get(dataset);
                if (em != null && em.perClass.containsKey(cls)) {
                    System.out.printf(" | %.3f/%.3f ",
                        em.perClass.get(cls).f1, em.perClass.get(cls).recall);
                } else {
                    System.out.printf(" | %-11s", "—");
                }
            }
            System.out.println();
        }
    }
}
