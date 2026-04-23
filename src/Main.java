import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Điểm vào (entry point) của thuật toán phân lớp CMAR.
 *
 * Cài đặt trung thành theo bài báo:
 *   Li, Han, Pei (2001) "CMAR: Accurate and Efficient Classification
 *   Based on Multiple Class-Association Rules", ICDM 2001.
 *
 * Pipeline:
 *   1. Nạp dataset từ file CSV
 *   2. Xáo trộn và chia tập train / test
 *   3. Khai thác frequent pattern bằng FP-Growth
 *   4. Sinh các luật kết hợp theo lớp (Class Association Rules - CARs)
 *   5. Huấn luyện bộ phân lớp CMAR (cắt tỉa 3 tầng)
 *   6. Phân lớp các bản ghi test (weighted chi-square)
 *   7. Đánh giá và ghi toàn bộ kết quả vào thư mục result/
 */
public class Main {

    // ===== Cấu hình — chỉnh ở đây khi cần =====
    static String DATASET_PATH     = "data/car.csv";
    static double TRAIN_RATIO      = 0.8;    // tỉ lệ dùng cho huấn luyện
    static int    MIN_SUPPORT      = 50;     // ngưỡng support tối thiểu (số đếm tuyệt đối)
    static double MIN_CONFIDENCE   = 0.5;    // độ tin cậy (confidence) tối thiểu của luật
    static long   RANDOM_SEED      = 42;     // seed để shuffle lặp lại được

    // Tham số riêng của CMAR (theo Section 5 của paper)
    static double CHI_SQ_THRESHOLD = 3.841;  // mức ý nghĩa p=0.05, df=1
    static int    COVERAGE_DELTA   = 4;      // ngưỡng database coverage δ

    static final String OUT_PATTERNS    = "result/frequent_patterns.txt";
    static final String OUT_RULES       = "result/association_rules.txt";
    static final String OUT_PREDICTIONS = "result/predictions.txt";
    static final String OUT_EVALUATION  = "result/evaluation.txt";
    static final String OUT_REPORT      = "report/fp_tree_report.md";
    static final String OUT_FPGROWTH    = "result/fpgrowth_result.txt";
    static final String OUT_CMAR        = "result/cmar_result.txt";
    // ==================================================

    public static void main(String[] args) throws Exception {
        // --- Đọc tham số tuỳ chọn từ dòng lệnh ---
        if (args.length >= 1) {
            DATASET_PATH = args[0];
        }
        if (args.length >= 2) {
            MIN_SUPPORT = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            MIN_CONFIDENCE = Double.parseDouble(args[2]);
        }
        if (args.length >= 4) {
            CHI_SQ_THRESHOLD = Double.parseDouble(args[3]);
        }
        if (args.length >= 5) {
            COVERAGE_DELTA = Integer.parseInt(args[4]);
        }

        System.out.println("=== Thuat toan phan lop CMAR ===");
        System.out.println("    (Li, Han, Pei — ICDM 2001)\n");

        // ------------------------------------------------------------------
        // Bước 1: Nạp dataset
        // ------------------------------------------------------------------
        System.out.println("[1] Nap dataset: " + DATASET_PATH);
        List<Transaction> data = DatasetLoader.load(DATASET_PATH);
        if (data.isEmpty()) {
            System.err.println("LOI: Khong nap duoc ban ghi nao tu " + DATASET_PATH);
            return;
        }
        System.out.println("    Da nap " + data.size() + " ban ghi.\n");

        // ------------------------------------------------------------------
        // Bước 2: Xáo trộn và chia tập
        // ------------------------------------------------------------------
        System.out.println("[2] Chia du lieu (train=" + (int)(TRAIN_RATIO*100)
            + "%, test=" + (int)((1-TRAIN_RATIO)*100) + "%)");
        List<Transaction> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, new Random(RANDOM_SEED));
        int trainSize = (int) Math.round(shuffled.size() * TRAIN_RATIO);
        List<Transaction> trainData = new ArrayList<>(shuffled.subList(0, trainSize));
        List<Transaction> testData  = new ArrayList<>(shuffled.subList(trainSize, shuffled.size()));
        System.out.println("    Train: " + trainData.size() + "  |  Test: " + testData.size() + "\n");

        // ------------------------------------------------------------------
        // Bước 3 & 4: Khai thác frequent pattern và sinh CAR trực tiếp trên CR-tree
        // ------------------------------------------------------------------
        System.out.println("[3] Khai thac frequent pattern tren CR-tree "
            + "(minSupport=" + MIN_SUPPORT
            + ", minConfidence=" + MIN_CONFIDENCE + ")...");

        FPGrowth fpGrowth = new FPGrowth(MIN_SUPPORT);
        List<AssociationRule> candidates = fpGrowth.mine(trainData, MIN_CONFIDENCE);
        List<FrequentPattern> patterns   = fpGrowth.getPatterns();
        System.out.println("    Tim duoc " + patterns.size() + " frequent pattern.");
        System.out.println("    Sinh " + candidates.size() + " CAR ung vien (truc tiep tu qua trinh mining CR-tree).");

        ResultWriter.writeFrequentPatterns(patterns, OUT_PATTERNS);
        System.out.println("    Da luu -> " + OUT_PATTERNS);

        ResultWriter.writeRules(candidates, OUT_RULES);
        System.out.println("    Da luu -> " + OUT_RULES + "\n");

        // ------------------------------------------------------------------
        // Bước 4b: Sinh báo cáo Markdown về FP-Tree
        // ------------------------------------------------------------------
        System.out.println("[4b] Sinh bao cao FP-Tree...");
        ResultWriter.writeFPTreeReport(
            DATASET_PATH, data.size(), trainData.size(),
            MIN_SUPPORT, MIN_CONFIDENCE,
            fpGrowth.getInitialTree(), patterns, candidates, OUT_REPORT);
        System.out.println("    Da luu -> " + OUT_REPORT);

        // Ghi file kết quả riêng cho giai đoạn FP-Growth
        ResultWriter.writeFPGrowthResult(
            DATASET_PATH, trainData.size(), MIN_SUPPORT,
            fpGrowth.getInitialTree(), patterns, OUT_FPGROWTH);
        System.out.println("    Da luu -> " + OUT_FPGROWTH + "\n");

        // ------------------------------------------------------------------
        // Bước 5: Huấn luyện bộ phân lớp CMAR (cắt tỉa 3 tầng theo paper)
        // ------------------------------------------------------------------
        System.out.println("[5] Huan luyen bo phan lop CMAR...");
        System.out.println("    Nguong chi-square: " + CHI_SQ_THRESHOLD
            + " | Coverage delta: " + COVERAGE_DELTA);
        CMARClassifier classifier = new CMARClassifier();
        classifier.setChiSquareThreshold(CHI_SQ_THRESHOLD);
        classifier.setCoverageThreshold(COVERAGE_DELTA);
        classifier.train(candidates, trainData);
        System.out.println("    Lop mac dinh: \"" + classifier.getDefaultClass() + "\"\n");

        // ------------------------------------------------------------------
        // Bước 6: Phân lớp các bản ghi test
        // ------------------------------------------------------------------
        System.out.println("[6] Phan lop " + testData.size() + " ban ghi test...");
        List<String> predictions = classifier.predict(testData);

        ResultWriter.writePredictions(testData, predictions, OUT_PREDICTIONS);
        System.out.println("    Da luu -> " + OUT_PREDICTIONS + "\n");

        // ------------------------------------------------------------------
        // Bước 7: Đánh giá
        // ------------------------------------------------------------------
        System.out.println("[7] Danh gia...");
        double accuracy = classifier.evaluate(testData);
        System.out.printf("    Do chinh xac tren test: %.4f (%.2f%%)%n", accuracy, accuracy * 100);

        ResultWriter.writeEvaluation(testData, predictions, OUT_EVALUATION);
        System.out.println("    Da luu -> " + OUT_EVALUATION);

        // Ghi file kết quả riêng cho CMAR
        ResultWriter.writeCMARResult(
            DATASET_PATH, trainData.size(), testData.size(),
            MIN_SUPPORT, MIN_CONFIDENCE,
            classifier, testData, predictions, OUT_CMAR);
        System.out.println("    Da luu -> " + OUT_CMAR);

        System.out.println("\n=== Hoan tat ===");
    }
}
