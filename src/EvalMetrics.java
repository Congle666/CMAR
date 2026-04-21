import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classification evaluation metrics — Accuracy, Macro-F1, Weighted-F1
 * plus per-class Precision / Recall / F1.
 *
 * Usage:
 *   EvalMetrics m = EvalMetrics.compute(testData, predictions);
 *   m.accuracy, m.macroF1, m.weightedF1, m.perClass.get("foo").f1, ...
 *
 * For cross-validation:
 *   List<EvalMetrics> perFold = ...;
 *   EvalMetrics avg = EvalMetrics.average(perFold);   // micro-over-folds
 */
public final class EvalMetrics {

    /** Overall test set size (= sum of per-class supports). */
    public int totalSupport;

    public double accuracy;
    public double macroF1;
    public double weightedF1;

    /** Per-class metrics, ordered by first appearance in test data. */
    public Map<String, ClassMetrics> perClass = new LinkedHashMap<>();

    /** Standard deviation of accuracy across folds (only set by average()). */
    public double accuracyStd;

    /** Standard deviation of macroF1 across folds (only set by average()). */
    public double macroF1Std;

    // -----------------------------------------------------------------------

    public static final class ClassMetrics {
        public String className;
        public int tp;
        public int fp;
        public int fn;
        public int support;       // = tp + fn (actual count of this class in testData)
        public double precision;
        public double recall;
        public double f1;

        @Override
        public String toString() {
            return String.format("%s [tp=%d fp=%d fn=%d P=%.4f R=%.4f F1=%.4f]",
                className, tp, fp, fn, precision, recall, f1);
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Computes Accuracy + Macro-F1 + Weighted-F1 + per-class P/R/F1 from
     * (testData, predictions) pair. Classes are ordered by first appearance
     * in testData.
     *
     * @param testData    ground-truth transactions
     * @param predictions predicted class labels, same size and order
     * @return populated EvalMetrics
     */
    public static EvalMetrics compute(List<Transaction> testData,
                                       List<String> predictions) {
        if (testData.size() != predictions.size()) {
            throw new IllegalArgumentException(
                "testData.size() != predictions.size(): "
                + testData.size() + " vs " + predictions.size());
        }

        EvalMetrics m = new EvalMetrics();

        // --- Collect classes in order of first appearance ---
        Map<String, ClassMetrics> byClass = new LinkedHashMap<>();
        for (Transaction t : testData) {
            byClass.computeIfAbsent(t.getClassLabel(), c -> {
                ClassMetrics cm = new ClassMetrics();
                cm.className = c;
                return cm;
            });
        }
        // Also include predicted classes that might not be in test set
        for (String p : predictions) {
            byClass.computeIfAbsent(p, c -> {
                ClassMetrics cm = new ClassMetrics();
                cm.className = c;
                return cm;
            });
        }

        // --- Count TP/FP/FN ---
        int totalCorrect = 0;
        for (int i = 0; i < testData.size(); i++) {
            String actual = testData.get(i).getClassLabel();
            String pred   = predictions.get(i);
            if (actual.equals(pred)) {
                byClass.get(actual).tp++;
                totalCorrect++;
            } else {
                byClass.get(actual).fn++;
                byClass.get(pred).fp++;
            }
        }

        // --- Per-class P/R/F1 + support ---
        double sumF1 = 0.0;
        double sumF1Weighted = 0.0;
        int totalSupport = 0;
        for (ClassMetrics cm : byClass.values()) {
            cm.support = cm.tp + cm.fn;
            cm.precision = safeDiv(cm.tp, cm.tp + cm.fp);
            cm.recall    = safeDiv(cm.tp, cm.tp + cm.fn);
            cm.f1        = (cm.precision + cm.recall) == 0
                            ? 0.0
                            : 2 * cm.precision * cm.recall / (cm.precision + cm.recall);
            sumF1 += cm.f1;
            sumF1Weighted += cm.f1 * cm.support;
            totalSupport += cm.support;
        }

        m.perClass      = byClass;
        m.totalSupport  = totalSupport;
        m.accuracy      = testData.isEmpty() ? 0.0 : (double) totalCorrect / testData.size();
        m.macroF1       = byClass.isEmpty() ? 0.0 : sumF1 / byClass.size();
        m.weightedF1    = totalSupport == 0 ? 0.0 : sumF1Weighted / totalSupport;

        return m;
    }

    // -----------------------------------------------------------------------

    /**
     * Averages metrics across k folds using "micro-over-folds" aggregation:
     *   - Per-class TP/FP/FN are SUMMED across folds (stable for small folds).
     *   - Precision/Recall/F1 are then recomputed from the summed counts.
     *   - Overall accuracy = mean of per-fold accuracies.
     *   - accuracyStd / macroF1Std are populated.
     *
     * This is more robust than averaging per-fold F1 directly, since in small
     * folds a class may be absent entirely (F1 undefined).
     *
     * @param folds list of per-fold EvalMetrics (non-empty)
     * @return aggregated EvalMetrics
     */
    public static EvalMetrics average(List<EvalMetrics> folds) {
        if (folds == null || folds.isEmpty()) {
            throw new IllegalArgumentException("folds must be non-empty");
        }

        EvalMetrics agg = new EvalMetrics();

        // --- Collect all class names seen across folds, ordered ---
        Map<String, ClassMetrics> byClass = new LinkedHashMap<>();
        for (EvalMetrics f : folds) {
            for (String clsName : f.perClass.keySet()) {
                byClass.computeIfAbsent(clsName, c -> {
                    ClassMetrics cm = new ClassMetrics();
                    cm.className = c;
                    return cm;
                });
            }
        }

        // --- Sum tp/fp/fn across folds ---
        for (EvalMetrics f : folds) {
            for (ClassMetrics foldCm : f.perClass.values()) {
                ClassMetrics aggCm = byClass.get(foldCm.className);
                aggCm.tp += foldCm.tp;
                aggCm.fp += foldCm.fp;
                aggCm.fn += foldCm.fn;
            }
        }

        // --- Recompute per-class P/R/F1 from summed counts ---
        double sumF1 = 0.0;
        double sumF1Weighted = 0.0;
        int totalSupport = 0;
        for (ClassMetrics cm : byClass.values()) {
            cm.support = cm.tp + cm.fn;
            cm.precision = safeDiv(cm.tp, cm.tp + cm.fp);
            cm.recall    = safeDiv(cm.tp, cm.tp + cm.fn);
            cm.f1        = (cm.precision + cm.recall) == 0
                            ? 0.0
                            : 2 * cm.precision * cm.recall / (cm.precision + cm.recall);
            sumF1 += cm.f1;
            sumF1Weighted += cm.f1 * cm.support;
            totalSupport += cm.support;
        }

        agg.perClass     = byClass;
        agg.totalSupport = totalSupport;

        // --- Mean accuracy + stddev across folds ---
        double[] accs  = folds.stream().mapToDouble(f -> f.accuracy).toArray();
        double[] mf1s  = folds.stream().mapToDouble(f -> f.macroF1).toArray();
        agg.accuracy    = mean(accs);
        agg.accuracyStd = stddev(accs);
        agg.macroF1     = byClass.isEmpty() ? 0.0 : sumF1 / byClass.size();
        agg.macroF1Std  = stddev(mf1s);
        agg.weightedF1  = totalSupport == 0 ? 0.0 : sumF1Weighted / totalSupport;

        return agg;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static double safeDiv(int num, int den) {
        return den == 0 ? 0.0 : (double) num / den;
    }

    private static double mean(double[] v) {
        if (v.length == 0) return 0.0;
        double s = 0;
        for (double x : v) s += x;
        return s / v.length;
    }

    private static double stddev(double[] v) {
        if (v.length == 0) return 0.0;
        double m = mean(v);
        double sq = 0;
        for (double x : v) sq += (x - m) * (x - m);
        return Math.sqrt(sq / v.length);
    }

    /** Returns a list of all class names, in appearance order. */
    public List<String> classNames() {
        return new ArrayList<>(perClass.keySet());
    }

    @Override
    public String toString() {
        return String.format("EvalMetrics{acc=%.4f, macroF1=%.4f, weightedF1=%.4f, classes=%d}",
            accuracy, macroF1, weightedF1, perClass.size());
    }
}
