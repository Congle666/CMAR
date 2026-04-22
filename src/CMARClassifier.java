import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CMAR Classifier — faithful to Li, Han & Pei (2001) ICDM.
 *
 * Training (train):
 *   Pruning 1: General rule pruning — remove more specific rules dominated
 *              by a higher-ranked general rule with the same class.
 *   Pruning 2: Positively correlated rules — keep only rules whose χ²
 *              passes a significance threshold (default 3.841, p=0.05, df=1)
 *              AND whose observed co-occurrence exceeds the expected value.
 *   Pruning 3: Database coverage (Algorithm 1 in paper) — iteratively
 *              select rules that correctly cover training data; remove
 *              data objects once covered by δ rules.
 *
 * After pruning, surviving rules are inserted into a CR-tree (§3.3) for
 * compact storage and fast subset retrieval at classification time.
 *
 * Classification (classify):
 *   1. Retrieve all CR-tree rules matching the test record.
 *   2. If no rules match → default class.
 *   3. If all matching rules agree on one class → that class.
 *   4. Otherwise group by class, score(G) = Σ [χ²(r)]² / maxχ²(r),
 *      return the class with the largest score.
 */
public class CMARClassifier {

    private List<AssociationRule> rules;
    private CRTree crTree;
    private String defaultClass;
    private int totalTransactions;
    private Map<String, Integer> classFreq;
    private Map<String, Integer> itemFreq;   // for CR-tree path ordering

    // --- Pruning parameters (§5) ---
    private double chiSquareThreshold = 3.841;
    private int    coverageThreshold  = 4;

    // --- Statistics ---
    private int candidateCount;
    private int afterGeneralPruneCount;
    private int afterChiPruneCount;
    private int afterCoveragePruneCount;

    public void setChiSquareThreshold(double threshold) { this.chiSquareThreshold = threshold; }
    public void setCoverageThreshold(int delta)         { this.coverageThreshold = delta; }

    // -----------------------------------------------------------------------
    // Training
    // -----------------------------------------------------------------------

    public void train(List<AssociationRule> candidateRules,
                      List<Transaction> trainData) {
        this.totalTransactions = trainData.size();
        this.classFreq = new HashMap<>();
        this.itemFreq  = new HashMap<>();

        for (Transaction t : trainData) {
            classFreq.merge(t.getClassLabel(), 1, Integer::sum);
            for (String item : t.getItems()) {
                itemFreq.merge(item, 1, Integer::sum);
            }
        }

        defaultClass = classFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");

        Collections.sort(candidateRules);
        this.candidateCount = candidateRules.size();

        // --- Pruning 1: General rule pruning ---
        List<AssociationRule> afterPrune1 = pruneByGeneralRules(candidateRules);
        this.afterGeneralPruneCount = afterPrune1.size();
        System.out.println("    Pruning 1 (general rules):     "
            + candidateRules.size() + " -> " + afterPrune1.size());

        // --- Pruning 2: Chi-square significance + positive correlation ---
        List<AssociationRule> afterPrune2 = pruneByChiSquareSignificance(afterPrune1);
        this.afterChiPruneCount = afterPrune2.size();
        System.out.println("    Pruning 2 (chi-square >= " + chiSquareThreshold + "): "
            + afterPrune1.size() + " -> " + afterPrune2.size());

        // --- Pruning 3: Database coverage ---
        this.rules = pruneByDatabaseCoverage(afterPrune2, trainData);
        this.afterCoveragePruneCount = this.rules.size();
        System.out.println("    Pruning 3 (db coverage, delta=" + coverageThreshold + "):  "
            + afterPrune2.size() + " -> " + this.rules.size());

        // --- Build CR-tree for compact rule storage and fast retrieval ---
        this.crTree = new CRTree(itemFreq);
        crTree.insertAll(this.rules);

        System.out.println("    Final rules: " + this.rules.size()
            + " (from " + candidateRules.size() + " candidates; stored in CR-tree)");
    }

    // -----------------------------------------------------------------------
    // Pruning 1: General Rule Pruning
    // -----------------------------------------------------------------------

    private List<AssociationRule> pruneByGeneralRules(List<AssociationRule> sorted) {
        List<AssociationRule> kept = new ArrayList<>();
        Map<String, List<AssociationRule>> keptByClass = new HashMap<>();

        for (AssociationRule r1 : sorted) {
            boolean dominated = false;
            List<AssociationRule> sameClass = keptByClass.get(r1.getClassLabel());
            if (sameClass != null) {
                for (AssociationRule r2 : sameClass) {
                    if (r1.getCondset().containsAll(r2.getCondset())) {
                        dominated = true;
                        break;
                    }
                }
            }
            if (!dominated) {
                kept.add(r1);
                keptByClass.computeIfAbsent(r1.getClassLabel(), k -> new ArrayList<>())
                           .add(r1);
            }
        }
        return kept;
    }

    // -----------------------------------------------------------------------
    // Pruning 2: Chi-Square Significance + Positive Correlation
    // -----------------------------------------------------------------------

    private List<AssociationRule> pruneByChiSquareSignificance(
            List<AssociationRule> rules) {
        List<AssociationRule> kept = new ArrayList<>();

        for (AssociationRule r : rules) {
            double chi2 = computeChiSquare(r, r.getClassLabel());
            double a = r.getSupportCount();
            double b = r.getCondsetSupportCount() - a;
            double c = classFreq.getOrDefault(r.getClassLabel(), 0) - a;
            double d = totalTransactions - a - b - c;
            boolean positivelyCorrelated = (a * d > b * c);

            if (chi2 >= chiSquareThreshold && positivelyCorrelated) {
                kept.add(r);
            }
        }
        return kept;
    }

    // -----------------------------------------------------------------------
    // Pruning 3: Database Coverage (Algorithm 1)
    // -----------------------------------------------------------------------

    private List<AssociationRule> pruneByDatabaseCoverage(
            List<AssociationRule> rules, List<Transaction> trainData) {

        int n = trainData.size();
        int[] coverCount = new int[n];
        boolean[] removed = new boolean[n];
        int remainingCount = n;

        List<AssociationRule> selected = new ArrayList<>();

        for (AssociationRule rule : rules) {
            if (remainingCount == 0) break;
            boolean coversAny = false;

            for (int i = 0; i < n; i++) {
                if (removed[i]) continue;
                Transaction t = trainData.get(i);
                if (rule.matches(t)
                        && rule.getClassLabel().equals(t.getClassLabel())) {
                    coversAny = true;
                    coverCount[i]++;
                    if (coverCount[i] >= coverageThreshold) {
                        removed[i] = true;
                        remainingCount--;
                    }
                }
            }
            if (coversAny) selected.add(rule);
        }
        return selected;
    }

    // -----------------------------------------------------------------------
    // Classification (§4) — CR-tree-based retrieval
    // -----------------------------------------------------------------------

    public String classify(Transaction record) {
        List<AssociationRule> matching = crTree.findMatching(record);

        if (matching.isEmpty()) return defaultClass;

        Map<String, List<AssociationRule>> byClass = new HashMap<>();
        for (AssociationRule rule : matching) {
            byClass.computeIfAbsent(rule.getClassLabel(), k -> new ArrayList<>())
                   .add(rule);
        }

        if (byClass.size() == 1) {
            return byClass.keySet().iterator().next();
        }

        // Weighted chi-square: score(G) = Σ [χ²(r)]² / maxχ²(r)
        String bestClass = defaultClass;
        double bestScore = -1.0;

        for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
            String cls = entry.getKey();
            double score = 0.0;
            for (AssociationRule rule : entry.getValue()) {
                double chi2    = computeChiSquare(rule, cls);
                double maxChi2 = computeMaxChiSquare(rule, cls);
                if (maxChi2 > 0) score += (chi2 * chi2) / maxChi2;
            }
            if (score > bestScore) {
                bestScore = score;
                bestClass = cls;
            }
        }
        return bestClass;
    }

    // -----------------------------------------------------------------------
    // Chi-square computation (shared by pruning and classification)
    // -----------------------------------------------------------------------

    protected double computeChiSquare(AssociationRule rule, String cls) {
        double n = totalTransactions;
        double a = rule.getSupportCount();
        double b = rule.getCondsetSupportCount() - a;
        double c = classFreq.getOrDefault(cls, 0) - a;
        double d = n - a - b - c;

        double denom = (a + b) * (c + d) * (a + c) * (b + d);
        if (denom == 0) return 0.0;
        return n * Math.pow(a * d - b * c, 2) / denom;
    }

    protected double computeMaxChiSquare(AssociationRule rule, String cls) {
        double n    = totalTransactions;
        double supP = rule.getCondsetSupportCount();
        double supC = classFreq.getOrDefault(cls, 0);

        double a = Math.min(supP, supC);
        double b = supP - a;
        double c = supC - a;
        double d = n - a - b - c;

        double denom = (a + b) * (c + d) * (a + c) * (b + d);
        if (denom == 0) return 0.0;
        return n * Math.pow(a * d - b * c, 2) / denom;
    }

    // -----------------------------------------------------------------------
    // Batch predict & evaluate
    // -----------------------------------------------------------------------

    public List<String> predict(List<Transaction> testData) {
        List<String> predictions = new ArrayList<>();
        for (Transaction t : testData) predictions.add(classify(t));
        return predictions;
    }

    public double evaluate(List<Transaction> testData) {
        if (testData.isEmpty()) return 0.0;
        int correct = 0;
        for (Transaction t : testData) {
            if (classify(t).equals(t.getClassLabel())) correct++;
        }
        return (double) correct / testData.size();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public List<AssociationRule> getRules()    { return rules; }
    public CRTree                getCRTree()   { return crTree; }
    public String                getDefaultClass()          { return defaultClass; }
    public int                   getCandidateCount()        { return candidateCount; }
    public int                   getAfterGeneralPruneCount(){ return afterGeneralPruneCount; }
    public int                   getAfterChiPruneCount()    { return afterChiPruneCount; }
    public int                   getAfterCoveragePruneCount(){ return afterCoveragePruneCount; }
    public double                getChiSquareThreshold()    { return chiSquareThreshold; }
    public int                   getCoverageThreshold()     { return coverageThreshold; }
    public Map<String, Integer>  getClassFreq()             { return classFreq; }
    public int                   getTotalTransactions()     { return totalTransactions; }
}
