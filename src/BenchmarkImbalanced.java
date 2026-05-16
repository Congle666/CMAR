import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Benchmark v13 — Focused Imbalanced Datasets với Adaptive SMOTE Strategy.
 *
 * <p>Tập trung vào 7 datasets thực sự mất cân bằng:</p>
 * <ul>
 *   <li>Extreme (min &lt; 5): Lymph (min=2), Zoo (min=4)</li>
 *   <li>Multi-class moderate (min 5-15): Glass (min=9)</li>
 *   <li>Binary moderate (min &gt;15): Hepatitis, German, Vehicle, Breast-w</li>
 * </ul>
 *
 * <p><b>Adaptive Strategy (đề xuất gốc của nghiên cứu):</b></p>
 * <pre>
 *   if min_class_freq &lt; 5:
 *     → Borderline-SMOTE-N (Han 2005)  — extreme minority
 *   else:
 *     → H2 only (no oversampling)       — đủ tốt cho moderate
 * </pre>
 *
 * <p>Mục tiêu: <b>Acc KHÔNG giảm + F1/Recall TĂNG</b> trên tất cả imbalanced datasets.</p>
 */
public class BenchmarkImbalanced {

    /** 7 imbalanced datasets từ UCI. */
    static final String[][] DATASETS = {
        // { file, name, minSupportPct, [maxPatternLength] }
        { "data/lymph.csv",       "lymph",     "0.05" },   // 40:1 extreme
        { "data/zoo_h.csv",       "zoo",       "0.03", "4" },  // 10:1 extreme
        { "data/glass.csv",       "glass",     "0.01" },   // 8.4:1 multi-class
        { "data/hepatitis.csv",   "hepatitis", "0.05" },   // 4:1 moderate
        { "data/german_disc.csv", "german",    "0.06" },   // 2.3:1 moderate
        { "data/vehicle.csv",     "vehicle",   "0.03", "5" },  // 4-class moderate
        { "data/breast-w.csv",    "breast-w",  "0.02" },   // 2:1 moderate
    };

    /** Trigger mới: chỉ bật SMOTE khi minority CỰC ÍT (< 5). */
    static final int    SMOTE_TRIGGER = 5;
    static final double SMOTE_RATIO   = 1.0;

    public static void main(String[] args) throws Exception {
        int K_FOLD = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        System.out.println("================================================================");
        System.out.println("  CMAR v13 — Focus IMBALANCED Datasets + Adaptive Strategy");
        System.out.println("================================================================");
        System.out.println("  Strategy:");
        System.out.println("    if min_freq < 5  → Borderline-SMOTE-N (Han 2005)");
        System.out.println("    else             → H2 only (no oversampling)");
        System.out.println("================================================================\n");

        System.out.println(">>> 1/2 BASELINE (no improvement) ...");
        Map<String, EvalMetrics> baseline = runVariant(false, false,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        System.out.println("\n>>> 2/2 V13 ADAPTIVE (H2 + Borderline if min<5) ...");
        Map<String, EvalMetrics> adaptive = runVariant(true, true,
            K_FOLD, minConfidence, chiSqThreshold, coverageDelta, seed);

        // -------- Summary Table --------
        System.out.println("\n" + "=".repeat(110));
        System.out.println("KẾT QUẢ v13 — 7 IMBALANCED DATASETS");
        System.out.println("=".repeat(110));
        System.out.printf("%-12s | %-7s | %-18s | %-18s | %-8s | %-8s | %s%n",
            "Dataset", "min", "Baseline (Acc/F1)", "v13 (Acc/F1)", "ΔAcc", "ΔF1", "Strategy");
        System.out.println("-".repeat(110));

        double sumAccB = 0, sumAccV = 0, sumF1B = 0, sumF1V = 0;
        double sumRecB = 0, sumRecV = 0;
        int cnt = 0;
        int accNoDrop = 0, f1Improved = 0, recallImproved = 0;

        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics b = baseline.get(name);
            EvalMetrics v = adaptive.get(name);
            if (b == null || v == null) continue;

            int minFreq = computeMinClassFreq(loadSafely(ds[0]));
            String strategy = minFreq < SMOTE_TRIGGER ? "Borderline" : "H2 only";
            double dAcc = v.accuracy - b.accuracy;
            double dF1 = v.macroF1 - b.macroF1;
            double dRec = macroRecall(v) - macroRecall(b);

            if (dAcc >= -0.001) accNoDrop++;  // Acc không giảm
            if (dF1 > 0) f1Improved++;
            if (dRec > 0) recallImproved++;

            sumAccB += b.accuracy; sumAccV += v.accuracy;
            sumF1B += b.macroF1; sumF1V += v.macroF1;
            sumRecB += macroRecall(b); sumRecV += macroRecall(v);
            cnt++;

            System.out.printf("%-12s | %3d     | %.4f / %.4f    | %.4f / %.4f    | %+7.4f | %+7.4f | %s%n",
                name, minFreq,
                b.accuracy, b.macroF1,
                v.accuracy, v.macroF1,
                dAcc, dF1,
                strategy);
        }

        System.out.println("-".repeat(110));
        System.out.printf("AVG (n=%d)   |         | %.4f / %.4f    | %.4f / %.4f    | %+7.4f | %+7.4f |%n",
            cnt,
            sumAccB / cnt, sumF1B / cnt,
            sumAccV / cnt, sumF1V / cnt,
            (sumAccV - sumAccB) / cnt,
            (sumF1V - sumF1B) / cnt);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ĐÁNH GIÁ MỤC TIÊU");
        System.out.println("=".repeat(80));
        System.out.printf("  ✅ Accuracy KHÔNG giảm:   %d / %d datasets%n", accNoDrop, cnt);
        System.out.printf("  ✅ F1 TĂNG:               %d / %d datasets%n", f1Improved, cnt);
        System.out.printf("  ✅ Recall TĂNG:           %d / %d datasets%n", recallImproved, cnt);
        System.out.printf("  📊 AVG Recall:           %.4f → %.4f (Δ=%+.4f)%n",
            sumRecB / cnt, sumRecV / cnt, (sumRecV - sumRecB) / cnt);

        // Per-class detail
        System.out.println("\n" + "=".repeat(110));
        System.out.println("PER-CLASS DETAIL — DATASETS CÓ SMOTE ACTIVE");
        System.out.println("=".repeat(110));
        for (String[] ds : DATASETS) {
            String name = ds[1];
            EvalMetrics v = adaptive.get(name);
            EvalMetrics b = baseline.get(name);
            if (v == null || b == null) continue;
            int minFreq = computeMinClassFreq(loadSafely(ds[0]));
            if (minFreq >= SMOTE_TRIGGER) continue;
            System.out.println("\n--- " + name.toUpperCase() + " (min=" + minFreq + ", Borderline ON) ---");
            printPerClass(b, v);
        }

        // Write CSVs
        writeCsvs(baseline, "v13_baseline");
        writeCsvs(adaptive, "v13_adaptive");
        System.out.println("\nFile CSV: result/v13_{baseline,adaptive}_*.csv");
    }

    private static double macroRecall(EvalMetrics m) {
        if (m == null || m.perClass == null || m.perClass.isEmpty()) return 0;
        double sum = 0;
        for (EvalMetrics.ClassMetrics c : m.perClass.values()) sum += c.recall;
        return sum / m.perClass.size();
    }

    private static List<Transaction> loadSafely(String file) {
        try { return DatasetLoader.load(file); } catch (Exception e) { return java.util.Collections.emptyList(); }
    }

    private static Map<String, EvalMetrics> runVariant(
            boolean useH2,
            boolean useAdaptiveSmote,
            int K_FOLD, double minConfidence, double chiSqThreshold,
            int coverageDelta, long seed) throws Exception {

        Map<String, EvalMetrics> results = new LinkedHashMap<>();

        for (String[] ds : DATASETS) {
            String file = ds[0];
            String name = ds[1];
            double supPct = Double.parseDouble(ds[2]);
            int maxLen = ds.length > 3 ? Integer.parseInt(ds[3]) : Integer.MAX_VALUE;
            System.out.print("  " + String.format("%-12s", name) + " ... ");
            try {
                List<Transaction> data = DatasetLoader.load(file);
                if (data.isEmpty()) { System.out.println("SKIP"); continue; }

                double smoteRatio = 0.0;
                boolean useBorderline = false;
                if (useAdaptiveSmote) {
                    int minFreq = computeMinClassFreq(data);
                    if (minFreq < SMOTE_TRIGGER) {
                        smoteRatio = SMOTE_RATIO;
                        useBorderline = true;  // Adaptive: extreme minority → Borderline
                    }
                    // else: no SMOTE (H2 only)
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
                System.out.printf("Acc=%.4f F1=%.4f (%s)%n",
                    agg.accuracy, agg.macroF1,
                    useBorderline ? "Borderline" : "H2 only");
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
        return min == Integer.MAX_VALUE ? 0 : min;
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

    private static void printPerClass(EvalMetrics baseline, EvalMetrics adaptive) {
        System.out.printf("%-22s | %-15s | %-15s | %-10s | %-10s%n",
            "Class(support)", "Baseline F1/R", "v13 F1/R", "ΔF1", "ΔRecall");
        System.out.println("-".repeat(85));
        for (String cls : baseline.perClass.keySet()) {
            EvalMetrics.ClassMetrics b = baseline.perClass.get(cls);
            EvalMetrics.ClassMetrics v = adaptive.perClass.get(cls);
            if (b == null) continue;
            int sup = b.support;
            if (v == null) {
                System.out.printf("%-22s | %.3f/%.3f    | -- / --        | --       | --%n",
                    cls + "(" + sup + ")", b.f1, b.recall);
            } else {
                System.out.printf("%-22s | %.3f/%.3f    | %.3f/%.3f    | %+7.3f  | %+7.3f%n",
                    cls + "(" + sup + ")",
                    b.f1, b.recall,
                    v.f1, v.recall,
                    v.f1 - b.f1, v.recall - b.recall);
            }
        }
    }
}
