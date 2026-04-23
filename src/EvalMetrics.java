import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Các chỉ số đánh giá phân lớp — Accuracy, Macro-F1, Weighted-F1
 * kèm Precision / Recall / F1 theo từng lớp.
 *
 * Cách dùng:
 *   EvalMetrics m = EvalMetrics.compute(testData, predictions);
 *   m.accuracy, m.macroF1, m.weightedF1, m.perClass.get("foo").f1, ...
 *
 * Dùng cho cross-validation:
 *   List<EvalMetrics> perFold = ...;
 *   EvalMetrics avg = EvalMetrics.average(perFold);   // micro-over-folds
 */
public final class EvalMetrics {

    /** Kích thước tổng của tập test (= tổng support của tất cả các lớp). */
    public int totalSupport;

    public double accuracy;
    public double macroF1;
    public double weightedF1;

    /** Chỉ số từng lớp, theo thứ tự xuất hiện đầu tiên trong tập test. */
    public Map<String, ClassMetrics> perClass = new LinkedHashMap<>();

    /** Độ lệch chuẩn của accuracy qua các fold (chỉ có khi gọi average()). */
    public double accuracyStd;

    /** Độ lệch chuẩn của macroF1 qua các fold (chỉ có khi gọi average()). */
    public double macroF1Std;

    // -----------------------------------------------------------------------

    public static final class ClassMetrics {
        public String className;
        public int tp;
        public int fp;
        public int fn;
        public int support;       // = tp + fn (số lượng thực của lớp này trong testData)
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
     * Tính Accuracy + Macro-F1 + Weighted-F1 + P/R/F1 theo lớp từ cặp
     * (testData, predictions). Các lớp được sắp theo thứ tự xuất hiện
     * đầu tiên trong testData.
     *
     * @param testData    các transaction với nhãn lớp thật
     * @param predictions các nhãn lớp dự đoán, cùng kích thước và thứ tự
     * @return EvalMetrics đã được điền đầy đủ
     */
    public static EvalMetrics compute(List<Transaction> testData,
                                       List<String> predictions) {
        if (testData.size() != predictions.size()) {
            throw new IllegalArgumentException(
                "testData.size() != predictions.size(): "
                + testData.size() + " vs " + predictions.size());
        }

        EvalMetrics m = new EvalMetrics();

        // --- Gom các lớp theo thứ tự xuất hiện đầu tiên ---
        Map<String, ClassMetrics> byClass = new LinkedHashMap<>();
        for (Transaction t : testData) {
            byClass.computeIfAbsent(t.getClassLabel(), c -> {
                ClassMetrics cm = new ClassMetrics();
                cm.className = c;
                return cm;
            });
        }
        // Bao gồm cả các lớp được dự đoán nhưng có thể không có trong tập test
        for (String p : predictions) {
            byClass.computeIfAbsent(p, c -> {
                ClassMetrics cm = new ClassMetrics();
                cm.className = c;
                return cm;
            });
        }

        // --- Đếm TP/FP/FN ---
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

        // --- Tính P/R/F1 + support cho từng lớp ---
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
     * Tổng hợp chỉ số qua k fold bằng cách "micro-over-folds":
     *   - TP/FP/FN theo lớp được CỘNG DỒN qua các fold (ổn định với fold nhỏ).
     *   - Precision/Recall/F1 được tính lại từ các tổng đã cộng dồn.
     *   - Accuracy tổng = trung bình accuracy của các fold.
     *   - accuracyStd / macroF1Std được điền.
     *
     * Cách này vững hơn trung bình F1 trực tiếp theo từng fold, vì ở các
     * fold nhỏ có thể có lớp hoàn toàn vắng mặt (F1 không xác định).
     *
     * @param folds danh sách EvalMetrics từng fold (khác rỗng)
     * @return EvalMetrics đã tổng hợp
     */
    public static EvalMetrics average(List<EvalMetrics> folds) {
        if (folds == null || folds.isEmpty()) {
            throw new IllegalArgumentException("folds must be non-empty");
        }

        EvalMetrics agg = new EvalMetrics();

        // --- Gom toàn bộ tên lớp đã thấy qua các fold, theo thứ tự ---
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

        // --- Cộng dồn tp/fp/fn qua các fold ---
        for (EvalMetrics f : folds) {
            for (ClassMetrics foldCm : f.perClass.values()) {
                ClassMetrics aggCm = byClass.get(foldCm.className);
                aggCm.tp += foldCm.tp;
                aggCm.fp += foldCm.fp;
                aggCm.fn += foldCm.fn;
            }
        }

        // --- Tính lại P/R/F1 từng lớp từ các tổng đã cộng dồn ---
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

        // --- Trung bình accuracy + stddev qua các fold ---
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
    // Hàm tiện ích
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

    /** Trả về danh sách tên các lớp, theo thứ tự xuất hiện. */
    public List<String> classNames() {
        return new ArrayList<>(perClass.keySet());
    }

    @Override
    public String toString() {
        return String.format("EvalMetrics{acc=%.4f, macroF1=%.4f, weightedF1=%.4f, classes=%d}",
            accuracy, macroF1, weightedF1, perClass.size());
    }
}
