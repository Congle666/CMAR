import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Entry point for the CMAR classification algorithm.
 *
 * Faithful implementation of:
 *   Li, Han, Pei (2001) "CMAR: Accurate and Efficient Classification
 *   Based on Multiple Class-Association Rules", ICDM 2001.
 *
 * Pipeline:
 *   1. Load dataset from CSV
 *   2. Shuffle and split into train / test sets
 *   3. Mine frequent patterns using FP-Growth
 *   4. Generate Class Association Rules (CARs)
 *   5. Train CMAR classifier (3-stage pruning)
 *   6. Classify test records (weighted chi-square)
 *   7. Evaluate and write all results to result/
 */
public class Main {

    // ===== Configuration — adjust these as needed =====
    static String DATASET_PATH     = "data/car.csv";
    static double TRAIN_RATIO      = 0.8;    // fraction used for training
    static int    MIN_SUPPORT      = 50;      // absolute minimum support count
    static double MIN_CONFIDENCE   = 0.5;    // minimum rule confidence
    static long   RANDOM_SEED      = 42;     // reproducible shuffle

    // CMAR-specific parameters (from paper Section 5)
    static double CHI_SQ_THRESHOLD = 3.841;  // significance level p=0.05, df=1
    static int    COVERAGE_DELTA   = 4;      // database coverage threshold δ

    static final String OUT_PATTERNS    = "result/frequent_patterns.txt";
    static final String OUT_RULES       = "result/association_rules.txt";
    static final String OUT_PREDICTIONS = "result/predictions.txt";
    static final String OUT_EVALUATION  = "result/evaluation.txt";
    static final String OUT_REPORT      = "report/fp_tree_report.md";
    static final String OUT_FPGROWTH    = "result/fpgrowth_result.txt";
    static final String OUT_CMAR        = "result/cmar_result.txt";
    // ==================================================

    public static void main(String[] args) throws Exception {
        // --- Parse optional command-line arguments ---
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

        System.out.println("=== CMAR Classification Algorithm ===");
        System.out.println("    (Li, Han, Pei — ICDM 2001)\n");

        // ------------------------------------------------------------------
        // Step 1: Load dataset
        // ------------------------------------------------------------------
        System.out.println("[1] Loading dataset: " + DATASET_PATH);
        List<Transaction> data = DatasetLoader.load(DATASET_PATH);
        if (data.isEmpty()) {
            System.err.println("ERROR: No records loaded from " + DATASET_PATH);
            return;
        }
        System.out.println("    Loaded " + data.size() + " records.\n");

        // ------------------------------------------------------------------
        // Step 2: Shuffle and split
        // ------------------------------------------------------------------
        System.out.println("[2] Splitting data (train=" + (int)(TRAIN_RATIO*100)
            + "%, test=" + (int)((1-TRAIN_RATIO)*100) + "%)");
        List<Transaction> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, new Random(RANDOM_SEED));
        int trainSize = (int) Math.round(shuffled.size() * TRAIN_RATIO);
        List<Transaction> trainData = new ArrayList<>(shuffled.subList(0, trainSize));
        List<Transaction> testData  = new ArrayList<>(shuffled.subList(trainSize, shuffled.size()));
        System.out.println("    Train: " + trainData.size() + "  |  Test: " + testData.size() + "\n");

        // ------------------------------------------------------------------
        // Step 3: Mine frequent patterns
        // ------------------------------------------------------------------
        System.out.println("[3] Mining frequent patterns (minSupport=" + MIN_SUPPORT + ")...");
        List<List<String>> transactions = trainData.stream()
            .map(Transaction::getAllItems)
            .collect(Collectors.toList());

        FPGrowth fpGrowth = new FPGrowth(MIN_SUPPORT);
        List<FrequentPattern> patterns = fpGrowth.mine(transactions);
        System.out.println("    Found " + patterns.size() + " frequent patterns.");

        ResultWriter.writeFrequentPatterns(patterns, OUT_PATTERNS);
        System.out.println("    Saved -> " + OUT_PATTERNS + "\n");

        // ------------------------------------------------------------------
        // Step 4: Generate Class Association Rules
        // ------------------------------------------------------------------
        System.out.println("[4] Generating CARs (minConfidence=" + MIN_CONFIDENCE + ")...");
        List<AssociationRule> candidates = RuleGenerator.generate(
            patterns, trainData.size(), MIN_CONFIDENCE);
        System.out.println("    Generated " + candidates.size() + " candidate rules.");

        ResultWriter.writeRules(candidates, OUT_RULES);
        System.out.println("    Saved -> " + OUT_RULES + "\n");

        // ------------------------------------------------------------------
        // Step 4b: Generate FP-Tree Markdown report
        // ------------------------------------------------------------------
        System.out.println("[4b] Generating FP-Tree report...");
        ResultWriter.writeFPTreeReport(
            DATASET_PATH, data.size(), trainData.size(),
            MIN_SUPPORT, MIN_CONFIDENCE,
            fpGrowth.getInitialTree(), patterns, candidates, OUT_REPORT);
        System.out.println("    Saved -> " + OUT_REPORT);

        // Write FP-Growth dedicated result file
        ResultWriter.writeFPGrowthResult(
            DATASET_PATH, trainData.size(), MIN_SUPPORT,
            fpGrowth.getInitialTree(), patterns, OUT_FPGROWTH);
        System.out.println("    Saved -> " + OUT_FPGROWTH + "\n");

        // ------------------------------------------------------------------
        // Step 5: Train CMAR classifier (3-stage pruning from paper)
        // ------------------------------------------------------------------
        System.out.println("[5] Training CMAR classifier...");
        System.out.println("    Chi-square threshold: " + CHI_SQ_THRESHOLD
            + " | Coverage delta: " + COVERAGE_DELTA);
        CMARClassifier classifier = new CMARClassifier();
        classifier.setChiSquareThreshold(CHI_SQ_THRESHOLD);
        classifier.setCoverageThreshold(COVERAGE_DELTA);
        classifier.train(candidates, trainData);
        System.out.println("    Default class: \"" + classifier.getDefaultClass() + "\"\n");

        // ------------------------------------------------------------------
        // Step 6: Classify test records
        // ------------------------------------------------------------------
        System.out.println("[6] Classifying " + testData.size() + " test records...");
        List<String> predictions = classifier.predict(testData);

        ResultWriter.writePredictions(testData, predictions, OUT_PREDICTIONS);
        System.out.println("    Saved -> " + OUT_PREDICTIONS + "\n");

        // ------------------------------------------------------------------
        // Step 7: Evaluate
        // ------------------------------------------------------------------
        System.out.println("[7] Evaluating...");
        double accuracy = classifier.evaluate(testData);
        System.out.printf("    Test Accuracy: %.4f (%.2f%%)%n", accuracy, accuracy * 100);

        ResultWriter.writeEvaluation(testData, predictions, OUT_EVALUATION);
        System.out.println("    Saved -> " + OUT_EVALUATION);

        // Write CMAR dedicated result file
        ResultWriter.writeCMARResult(
            DATASET_PATH, trainData.size(), testData.size(),
            MIN_SUPPORT, MIN_CONFIDENCE,
            classifier, testData, predictions, OUT_CMAR);
        System.out.println("    Saved -> " + OUT_CMAR);

        System.out.println("\n=== Done ===");
    }
}
