import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ghi kết quả của thuật toán vào các thư mục result/ và report/.
 * Tất cả các phương thức đều tự động tạo thư mục cha nếu chưa có.
 */
public class ResultWriter {

    /** Ghi toàn bộ frequent pattern kèm số đếm support. */
    public static void writeFrequentPatterns(
            List<FrequentPattern> patterns, String filePath) throws IOException {
        ensureParentDir(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("=== Frequent Patterns ===\n");
            bw.write("Total: " + patterns.size() + "\n\n");
            for (FrequentPattern fp : patterns) {
                bw.write(fp.toString() + "\n");
            }
        }
    }

    /** Ghi toàn bộ luật kết hợp theo lớp (CAR) kèm support và confidence. */
    public static void writeRules(
            List<AssociationRule> rules, String filePath) throws IOException {
        ensureParentDir(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("=== Class Association Rules ===\n");
            bw.write("Total: " + rules.size() + "\n\n");
            for (int i = 0; i < rules.size(); i++) {
                bw.write((i + 1) + ". " + rules.get(i).toString() + "\n");
            }
        }
    }

    /** Ghi dự đoán của từng bản ghi, đối chiếu với nhãn lớp thật. */
    public static void writePredictions(
            List<Transaction> testData,
            List<String> predictions,
            String filePath) throws IOException {
        ensureParentDir(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("=== Classification Predictions ===\n");
            bw.write(String.format("%-50s %-12s %-12s%n", "Record", "Actual", "Predicted"));
            bw.write("-".repeat(76) + "\n");

            int correct = 0;
            for (int i = 0; i < testData.size(); i++) {
                String actual    = testData.get(i).getClassLabel();
                String predicted = predictions.get(i);
                boolean ok = actual.equals(predicted);
                if (ok) correct++;
                bw.write(String.format("%-50s %-12s %-12s %s%n",
                    testData.get(i).getItems().toString(),
                    actual, predicted, ok ? "" : "<-- WRONG"));
            }

            bw.write("-".repeat(76) + "\n");
            double acc = testData.isEmpty() ? 0.0
                       : 100.0 * correct / testData.size();
            bw.write(String.format("Accuracy: %d/%d = %.2f%%%n",
                correct, testData.size(), acc));
        }
    }

    /**
     * Ghi precision, recall, F1 theo từng lớp và accuracy tổng.
     * Ủy quyền tính toán chỉ số cho {@link EvalMetrics#compute}.
     */
    public static void writeEvaluation(
            List<Transaction> testData,
            List<String> predictions,
            String filePath) throws IOException {
        ensureParentDir(filePath);

        EvalMetrics m = EvalMetrics.compute(testData, predictions);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("=== Evaluation Metrics ===\n\n");
            bw.write(String.format("Overall Accuracy: %.4f (%.2f%%)%n%n",
                m.accuracy, m.accuracy * 100));

            bw.write(String.format("%-15s %4s %4s %4s %10s %10s %10s%n",
                "Class", "TP", "FP", "FN", "Precision", "Recall", "F1"));
            bw.write("-".repeat(60) + "\n");

            for (EvalMetrics.ClassMetrics cm : m.perClass.values()) {
                bw.write(String.format("%-15s %4d %4d %4d %10.4f %10.4f %10.4f%n",
                    cm.className, cm.tp, cm.fp, cm.fn,
                    cm.precision, cm.recall, cm.f1));
            }

            bw.write("-".repeat(60) + "\n");
            bw.write(String.format("Macro-F1: %.4f%n", m.macroF1));
            bw.write(String.format("Weighted-F1: %.4f%n", m.weightedF1));
        }
    }

    // -----------------------------------------------------------------------
    // File kết quả dành riêng cho FP-Growth
    // -----------------------------------------------------------------------

    /**
     * Ghi kết quả của FP-Growth vào một file riêng:
     * bảng header, cấu trúc FP-tree, các frequent pattern.
     */
    public static void writeFPGrowthResult(
            String datasetPath, int trainSize, int minSupport,
            FPTree tree, List<FrequentPattern> patterns,
            String filePath) throws IOException {
        ensureParentDir(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("==========================================================\n");
            bw.write("           FP-GROWTH — KET QUA DAO MO TAP PHO BIEN\n");
            bw.write("==========================================================\n\n");

            bw.write("Dataset:            " + datasetPath + "\n");
            bw.write("Training records:   " + trainSize + "\n");
            bw.write("Min Support:        " + minSupport + "\n");
            bw.write("Frequent patterns:  " + patterns.size() + "\n\n");

            // --- Giải thích thuật toán ---
            bw.write("----------------------------------------------------------\n");
            bw.write("THUAT TOAN FP-GROWTH\n");
            bw.write("----------------------------------------------------------\n\n");
            bw.write("Buoc 1: Dem tan suat moi item, loai bo item < minSupport.\n");
            bw.write("Buoc 2: Xay dung cay FP-Tree:\n");
            bw.write("   - Sap xep item theo tan suat giam dan.\n");
            bw.write("   - Chen tung giao dich vao cay tu goc.\n");
            bw.write("   - Nut con cung item -> tang count; chua co -> tao nut moi.\n");
            bw.write("   - Header Table lien ket cac nut cung item qua node-link.\n");
            bw.write("Buoc 3: Mining de quy:\n");
            bw.write("   - Voi moi item (theo tan suat tang dan):\n");
            bw.write("     + Ghi nhan pattern {prefix U item}\n");
            bw.write("     + Thu thap Conditional Pattern Base\n");
            bw.write("     + Xay Conditional FP-Tree -> de quy\n\n");

            // --- Bảng Header ---
            bw.write("----------------------------------------------------------\n");
            bw.write("HEADER TABLE (Bang Tan Suat Item)\n");
            bw.write("----------------------------------------------------------\n\n");
            bw.write(tree.headerTableToString());
            bw.write("\n");

            // --- Cây FP-Tree ---
            bw.write("----------------------------------------------------------\n");
            bw.write("CAU TRUC CAY FP-TREE\n");
            bw.write("----------------------------------------------------------\n");
            bw.write("(Moi nut: item:count)\n\n");
            bw.write(tree.toTreeString());
            bw.write("\n");

            // --- Toàn bộ frequent pattern ---
            bw.write("----------------------------------------------------------\n");
            bw.write("TAT CA FREQUENT PATTERNS (" + patterns.size() + " patterns)\n");
            bw.write("----------------------------------------------------------\n\n");
            List<FrequentPattern> sorted = new java.util.ArrayList<>(patterns);
            sorted.sort((a, b) -> b.getSupport() - a.getSupport());
            for (int i = 0; i < sorted.size(); i++) {
                bw.write((i + 1) + ". " + sorted.get(i).toString() + "\n");
            }
        }
    }

    // -----------------------------------------------------------------------
    // File kết quả dành riêng cho CMAR (đã cập nhật cho cắt tỉa 3 tầng)
    // -----------------------------------------------------------------------

    /**
     * Ghi kết quả của CMAR vào một file riêng:
     * thông tin cắt tỉa 3 tầng, toàn bộ luật sau cắt tỉa, dự đoán và đánh giá.
     */
    public static void writeCMARResult(
            String datasetPath, int trainSize, int testSize,
            int minSupport, double minConfidence,
            CMARClassifier classifier,
            List<Transaction> testData,
            List<String> predictions,
            String filePath) throws IOException {
        ensureParentDir(filePath);

        List<AssociationRule> prunedRules = classifier.getRules();
        String defaultClass = classifier.getDefaultClass();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("==========================================================\n");
            bw.write("       CMAR — PHAN LOP DUA TREN NHIEU LUAT KET HOP\n");
            bw.write("  (Li, Han, Pei — ICDM 2001)\n");
            bw.write("==========================================================\n\n");

            bw.write("Dataset:              " + datasetPath + "\n");
            bw.write("Training records:     " + trainSize + "\n");
            bw.write("Test records:         " + testSize + "\n");
            bw.write("Min Support:          " + minSupport + "\n");
            bw.write("Min Confidence:       " + minConfidence + "\n");
            bw.write("Chi-square threshold: " + classifier.getChiSquareThreshold()
                + " (p=0.05, df=1)\n");
            bw.write("Coverage delta:       " + classifier.getCoverageThreshold() + "\n");
            bw.write("Default class:        " + defaultClass + "\n\n");

            // --- Giải thích thuật toán ---
            bw.write("----------------------------------------------------------\n");
            bw.write("THUAT TOAN CMAR (Section 3 & 4 of the paper)\n");
            bw.write("----------------------------------------------------------\n\n");

            bw.write("Phase 1: RULE GENERATION (Section 3.1)\n");
            bw.write("   Tu frequent patterns, lay pattern chua dung 1 class item.\n");
            bw.write("   condset = cac item khong phai class, consequent = class.\n");
            bw.write("   Giu luat co confidence >= " + minConfidence + ".\n\n");

            bw.write("Phase 2: PRUNING (Section 3.3) — 3 phuong phap:\n\n");

            bw.write("   Pruning 1: General Rule Pruning\n");
            bw.write("     Sap xep theo: confidence giam -> support giam -> condset ngan.\n");
            bw.write("     Luat r1 bi loai neu ton tai r2 uu tien hon sao cho:\n");
            bw.write("       condset(r2) c= condset(r1) VA class(r2) = class(r1).\n\n");

            bw.write("   Pruning 2: Chi-Square Significance Test\n");
            bw.write("     Chi giu luat co chi-square >= " + classifier.getChiSquareThreshold());
            bw.write(" (positively correlated).\n");
            bw.write("     Loai bo luat khong co tuong quan thong ke voi class.\n\n");

            bw.write("   Pruning 3: Database Coverage (Algorithm 1)\n");
            bw.write("     Duyet luat theo thu tu uu tien. Moi luat duoc giu neu\n");
            bw.write("     no correctly classify it nhat 1 training record chua bi cover.\n");
            bw.write("     Record bi loai khi da duoc cover boi >= "
                + classifier.getCoverageThreshold() + " luat (delta).\n\n");

            bw.write("Phase 3: CLASSIFICATION (Section 4)\n");
            bw.write("   Voi moi record test:\n");
            bw.write("   - Thu thap tat ca luat match (condset c= record items).\n");
            bw.write("   - Nhom luat theo class.\n");
            bw.write("   - Tinh score(class) = Sum chi^2(r) / max_chi^2(r)\n");
            bw.write("     (weighted chi-square — Section 4 of paper)\n");
            bw.write("   - Class co score cao nhat thang.\n");
            bw.write("   - Khong co luat match -> dung default class.\n\n");

            // --- Kết quả cắt tỉa ---
            bw.write("----------------------------------------------------------\n");
            bw.write("KET QUA PRUNING (3 giai doan)\n");
            bw.write("----------------------------------------------------------\n\n");
            bw.write("Candidate rules (truoc pruning):  "
                + classifier.getCandidateCount() + "\n");
            bw.write("After Pruning 1 (general rules):  "
                + classifier.getAfterGeneralPruneCount() + "\n");
            bw.write("After Pruning 2 (chi-square):     "
                + classifier.getAfterChiPruneCount() + "\n");
            bw.write("After Pruning 3 (db coverage):    "
                + classifier.getAfterCoveragePruneCount() + "\n");
            bw.write("Total rules removed:              "
                + (classifier.getCandidateCount() - prunedRules.size()) + "\n\n");

            // --- Toàn bộ luật sau cắt tỉa ---
            bw.write("----------------------------------------------------------\n");
            bw.write("CAC LUAT SAU PRUNING (" + prunedRules.size() + " luat)\n");
            bw.write("----------------------------------------------------------\n\n");
            for (int i = 0; i < prunedRules.size(); i++) {
                bw.write((i + 1) + ". " + prunedRules.get(i).toString() + "\n");
            }
            bw.write("\n");

            // --- Dự đoán ---
            bw.write("----------------------------------------------------------\n");
            bw.write("DU DOAN TUNG BAN GHI TEST (" + testData.size() + " records)\n");
            bw.write("----------------------------------------------------------\n\n");
            bw.write(String.format("%-6s %-10s %-10s %s%n", "#", "Actual", "Predicted", ""));
            bw.write("-".repeat(40) + "\n");
            int correct = 0;
            for (int i = 0; i < testData.size(); i++) {
                String actual = testData.get(i).getClassLabel();
                String predicted = predictions.get(i);
                boolean ok = actual.equals(predicted);
                if (ok) correct++;
                bw.write(String.format("%-6d %-10s %-10s %s%n",
                    (i + 1), actual, predicted, ok ? "OK" : "WRONG"));
            }
            bw.write("-".repeat(40) + "\n\n");

            // --- Đánh giá ---
            bw.write("----------------------------------------------------------\n");
            bw.write("DANH GIA HIEU SUAT\n");
            bw.write("----------------------------------------------------------\n\n");

            EvalMetrics m = EvalMetrics.compute(testData, predictions);
            bw.write(String.format("Accuracy: %d/%d = %.4f (%.2f%%)%n%n",
                correct, testData.size(), m.accuracy, m.accuracy * 100));

            bw.write(String.format("%-12s %4s %4s %4s %10s %10s %10s%n",
                "Class", "TP", "FP", "FN", "Precision", "Recall", "F1"));
            bw.write("-".repeat(56) + "\n");
            for (EvalMetrics.ClassMetrics cm : m.perClass.values()) {
                bw.write(String.format("%-12s %4d %4d %4d %10.4f %10.4f %10.4f%n",
                    cm.className, cm.tp, cm.fp, cm.fn,
                    cm.precision, cm.recall, cm.f1));
            }
            bw.write("-".repeat(56) + "\n");
            bw.write(String.format("Macro-F1: %.4f%n", m.macroF1));
            bw.write(String.format("Weighted-F1: %.4f%n", m.weightedF1));
        }
    }

    // -----------------------------------------------------------------------

    private static void ensureParentDir(String filePath) {
        File f = new File(filePath);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
    }

    /**
     * Ghi báo cáo FP-Growth dạng Markdown bao gồm cấu trúc cây,
     * bảng header, cấu hình và phần giải thích thuật toán.
     */
    public static void writeFPTreeReport(
            String datasetPath,
            int datasetSize,
            int trainSize,
            int minSupport,
            double minConfidence,
            FPTree tree,
            List<FrequentPattern> patterns,
            List<AssociationRule> rules,
            String filePath) throws IOException {
        ensureParentDir(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("# Bao Cao Ket Qua FP-Growth & CMAR\n\n");

            // --- 1. Cấu hình ---
            bw.write("## 1. Thong Tin Cau Hinh\n\n");
            bw.write("| Tham so | Gia tri |\n");
            bw.write("|---------|--------|\n");
            bw.write("| Dataset | `" + datasetPath + "` |\n");
            bw.write("| Tong so ban ghi | " + datasetSize + " |\n");
            bw.write("| So ban ghi huan luyen | " + trainSize + " |\n");
            bw.write("| Min Support | " + minSupport + " |\n");
            bw.write("| Min Confidence | " + minConfidence + " |\n\n");

            // --- 2. Giải thích thuật toán ---
            bw.write("## 2. Giai Thich Thuat Toan FP-Growth\n\n");
            bw.write("### Buoc 1: Dem tan suat cac item\n");
            bw.write("Duyet qua toan bo tap giao dich, dem so lan xuat hien cua moi item.\n");
            bw.write("Loai bo cac item co tan suat < **minSupport** (" + minSupport + ").\n\n");

            bw.write("### Buoc 2: Xay dung cay FP-Tree\n");
            bw.write("- Sap xep cac item trong moi giao dich theo tan suat **giam dan**.\n");
            bw.write("- Chen lan luot tung giao dich vao cay, bat dau tu nut goc (root).\n");
            bw.write("- Neu nut con cung item da ton tai -> tang count; neu chua -> tao nut moi.\n");
            bw.write("- Duy tri **Header Table** lien ket tat ca nut cung item qua node-link.\n\n");

            bw.write("### Buoc 3: Dao mo (Mining) de quy\n");
            bw.write("Voi moi item trong header table (theo thu tu tan suat **tang dan**):\n");
            bw.write("1. Ghi nhan pattern `{prefix U item}` voi support tuong ung.\n");
            bw.write("2. Thu thap **Conditional Pattern Base** — cac duong di tien to tu nut ");
            bw.write("chua item len den goc.\n");
            bw.write("3. Xay dung **Conditional FP-Tree** tu conditional pattern base.\n");
            bw.write("4. Goi de quy tren conditional FP-tree voi prefix moi.\n\n");

            bw.write("### Buoc 4: Trich xuat luat ket hop phan lop (CARs)\n");
            bw.write("Tu cac frequent pattern, loc ra cac pattern chua dung **mot item class** ");
            bw.write("(`class=label`). Phan con lai la condset (dieu kien), class item la consequent.\n");
            bw.write("Chi giu cac luat co confidence >= **minConfidence** (" + minConfidence + ").\n\n");

            // --- 3. Bảng header ---
            bw.write("## 3. Header Table (Bang Tan Suat Item)\n\n");
            bw.write("```\n");
            bw.write(tree.headerTableToString());
            bw.write("```\n\n");

            // --- 4. Cấu trúc cây FP-Tree ---
            bw.write("## 4. Cau Truc Cay FP-Tree\n\n");
            bw.write("Moi nut hien thi dang `item:count` — ten item va so lan xuat hien ");
            bw.write("tren nhanh do.\n\n");
            bw.write("```\n");
            bw.write(tree.toTreeString());
            bw.write("```\n\n");

            // --- 5. Tóm tắt kết quả ---
            bw.write("## 5. Tom Tat Ket Qua\n\n");
            bw.write("| Ket qua | So luong |\n");
            bw.write("|---------|----------|\n");
            bw.write("| Frequent patterns | " + patterns.size() + " |\n");
            bw.write("| Class Association Rules (CARs) | " + rules.size() + " |\n\n");

            // --- 6. Top pattern ---
            int topN = Math.min(20, patterns.size());
            bw.write("## 6. Top " + topN + " Frequent Patterns (theo support)\n\n");
            List<FrequentPattern> sorted = new java.util.ArrayList<>(patterns);
            sorted.sort((a, b) -> b.getSupport() - a.getSupport());
            bw.write("| # | Pattern | Support |\n");
            bw.write("|---|---------|--------|\n");
            for (int i = 0; i < topN; i++) {
                FrequentPattern fp = sorted.get(i);
                bw.write("| " + (i + 1) + " | " + fp.getItems() + " | " + fp.getSupport() + " |\n");
            }
            bw.write("\n");

            // --- 7. Top luật ---
            int topR = Math.min(20, rules.size());
            bw.write("## 7. Top " + topR + " Class Association Rules\n\n");
            bw.write("| # | Condset -> Class | Support | Confidence |\n");
            bw.write("|---|-----------------|---------|------------|\n");
            for (int i = 0; i < topR; i++) {
                AssociationRule r = rules.get(i);
                bw.write("| " + (i + 1) + " | " + r.getCondset() + " -> " + r.getClassLabel()
                    + " | " + r.getSupport() + " | "
                    + String.format("%.4f", r.getConfidence()) + " |\n");
            }
            bw.write("\n");

            // --- 8. Hướng dẫn đổi dataset ---
            bw.write("## 8. Huong Dan Thay Doi Dataset\n\n");
            bw.write("### Cach 1: Dung tham so dong lenh\n\n");
            bw.write("```bash\n");
            bw.write("java -cp out Main <file.csv> [minSupport] [minConfidence] [chiSqThreshold] [coverageDelta]\n");
            bw.write("```\n\n");
            bw.write("Vi du:\n");
            bw.write("```bash\n");
            bw.write("java -cp out Main data/car.csv 50 0.5 3.841 4\n");
            bw.write("java -cp out Main data/weather.csv 2 0.5 3.841 4\n");
            bw.write("```\n\n");
            bw.write("### Cach 2: Sua truc tiep trong Main.java\n\n");
            bw.write("Thay doi cac hang so o dau file `src/Main.java`:\n");
            bw.write("```java\n");
            bw.write("static String DATASET_PATH     = \"data/ten_file.csv\";\n");
            bw.write("static int    MIN_SUPPORT      = 2;\n");
            bw.write("static double MIN_CONFIDENCE   = 0.5;\n");
            bw.write("static double CHI_SQ_THRESHOLD = 3.841;\n");
            bw.write("static int    COVERAGE_DELTA   = 4;\n");
            bw.write("```\n\n");
            bw.write("### Yeu cau file CSV\n\n");
            bw.write("- Dong dau tien la **header** (ten cot).\n");
            bw.write("- Cot cuoi cung la **class label** (nhan phan lop).\n");
            bw.write("- Cac cot con lai la thuoc tinh.\n");
            bw.write("- Gia tri thieu (`?`) se duoc bo qua tu dong.\n\n");
            bw.write("Vi du:\n");
            bw.write("```csv\n");
            bw.write("outlook,temperature,humidity,windy,play\n");
            bw.write("sunny,hot,high,false,no\n");
            bw.write("overcast,hot,high,false,yes\n");
            bw.write("```\n");
        }
    }

    // -----------------------------------------------------------------------
    // Xuất CSV — dùng để so sánh giữa các thí nghiệm (baseline vs cải tiến)
    // -----------------------------------------------------------------------

    /**
     * Ghi một dòng cho mỗi dataset kèm chỉ số tổng hợp:
     *   dataset, records, classes, minSupPct, accuracy, accStd, macroF1, macroF1Std, weightedF1
     * Dùng Locale.US (dấu chấm thập phân) để parse ổn định.
     *
     * @param byDataset   map có thứ tự tên_dataset -> EvalMetrics đã tổng hợp
     * @param datasetInfo map tuỳ chọn tên_dataset -> "records|classes|minSupPct" (có thể null)
     * @param filePath    đường dẫn file CSV đầu ra
     */
    public static void writeMetricsCsv(
            Map<String, EvalMetrics> byDataset,
            Map<String, String> datasetInfo,
            String filePath) throws IOException {
        ensureParentDir(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("dataset,records,classes,minSupPct,accuracy,accStd,macroF1,macroF1Std,weightedF1\n");
            for (Map.Entry<String, EvalMetrics> e : byDataset.entrySet()) {
                String name = e.getKey();
                EvalMetrics m = e.getValue();
                String info = datasetInfo != null ? datasetInfo.getOrDefault(name, "|||") : "|||";
                String[] parts = info.split("\\|", -1);
                String records    = parts.length > 0 ? parts[0] : "";
                String classes    = parts.length > 1 ? parts[1] : String.valueOf(m.perClass.size());
                String minSupPct  = parts.length > 2 ? parts[2] : "";
                bw.write(String.format(java.util.Locale.US,
                    "%s,%s,%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                    name, records, classes, minSupPct,
                    m.accuracy, m.accuracyStd,
                    m.macroF1, m.macroF1Std,
                    m.weightedF1));
            }
        }
    }

    /**
     * Ghi một dòng cho mỗi cặp (dataset, lớp) kèm chỉ số theo lớp:
     *   dataset, class, support, tp, fp, fn, precision, recall, f1
     *
     * @param byDataset map có thứ tự tên_dataset -> EvalMetrics đã tổng hợp
     * @param filePath  đường dẫn file CSV đầu ra
     */
    public static void writePerClassCsv(
            Map<String, EvalMetrics> byDataset,
            String filePath) throws IOException {
        ensureParentDir(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("dataset,class,support,tp,fp,fn,precision,recall,f1\n");
            for (Map.Entry<String, EvalMetrics> e : byDataset.entrySet()) {
                String name = e.getKey();
                EvalMetrics m = e.getValue();
                for (EvalMetrics.ClassMetrics cm : m.perClass.values()) {
                    bw.write(String.format(java.util.Locale.US,
                        "%s,%s,%d,%d,%d,%d,%.4f,%.4f,%.4f%n",
                        name, cm.className,
                        cm.support, cm.tp, cm.fp, cm.fn,
                        cm.precision, cm.recall, cm.f1));
                }
            }
        }
    }
}
