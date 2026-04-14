import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CMAR Classifier — faithful to the original paper:
 *   Li, Han, Pei (2001) "CMAR: Accurate and Efficient Classification
 *   Based on Multiple Class-Association Rules", ICDM 2001.
 *
 * Training (train):
 *   Pruning 1: General rule pruning — remove more specific rules that are
 *              dominated by a higher-ranked general rule with same class.
 *   Pruning 2: Positively correlated rules — keep only rules whose χ²
 *              passes a significance threshold (default 3.841, p=0.05, df=1).
 *   Pruning 3: Database coverage (Algorithm 1 in paper) — iteratively select
 *              rules that correctly cover training data; remove data objects
 *              once covered by δ rules.
 *
 * Classification (classify):
 *   1. Collect all rules matching the test record.
 *   2. If no rules match → return default class.
 *   3. If all matching rules agree on one class → return that class.
 *   4. Otherwise, group rules by class and compute:
 *        score(G) = Σ  χ²(r) / maxχ²(r)       (weighted chi-square)
 *      The class with the highest score wins.
 *
 * Weighted chi-square (Section 4 of the paper):
 *   χ²(r)    = standard 2×2 contingency table chi-square
 *   maxχ²(r) = chi-square computed with a = min(sup(P), sup(c))
 *              (the upper bound of χ² given fixed marginals)
 *   weighted χ² = χ²(r) / maxχ²(r)
 */
public class CMARClassifier {

    private List<AssociationRule> rules;
    private String defaultClass;
    private int totalTransactions;
    private Map<String, Integer> classFreq; // class label -> count in training data

    // --- Pruning parameters (from paper Section 5) ---
    private double chiSquareThreshold = 3.841; // significance level 0.05, df=1
    private int coverageThreshold = 4;         // δ in Algorithm 1

    // --- Statistics for reporting ---
    private int candidateCount;
    private int afterGeneralPruneCount;
    private int afterChiPruneCount;
    private int afterCoveragePruneCount;

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    public void setChiSquareThreshold(double threshold) {
        this.chiSquareThreshold = threshold;
    }

    public void setCoverageThreshold(int delta) {
        this.coverageThreshold = delta;
    }

    // -----------------------------------------------------------------------
    // Training
    // -----------------------------------------------------------------------

    /**
     * Trains the classifier from candidate rules and the training dataset.
     * Applies three pruning methods as described in the paper (Section 3.3).
     */
    public void train(List<AssociationRule> candidateRules,
                      List<Transaction> trainData) {
        this.totalTransactions = trainData.size();
        this.classFreq = new HashMap<>();

        for (Transaction t : trainData) {
            classFreq.merge(t.getClassLabel(), 1, Integer::sum);
        }

        // Default class = most frequent class in training data
        defaultClass = classFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");

        // Sort by precedence (should already be sorted, but ensure)
        Collections.sort(candidateRules);
        this.candidateCount = candidateRules.size();

        // --- Pruning 1: General rule pruning (Section 3.3, first method) ---
        // Remove more specific rules dominated by higher-ranked general rules.
        // r1 is pruned if ∃ r2 with higher rank such that
        //   condset(r2) ⊆ condset(r1) and class(r2) = class(r1)
        List<AssociationRule> afterPrune1 = pruneByGeneralRules(candidateRules);
        this.afterGeneralPruneCount = afterPrune1.size();

        System.out.println("    Pruning 1 (general rules):     "
            + candidateRules.size() + " -> " + afterPrune1.size());

        // --- Pruning 2: Positively correlated rules (Section 3.3, second method) ---
        // Keep only rules where χ² >= significance threshold
        List<AssociationRule> afterPrune2 = pruneByChiSquareSignificance(afterPrune1);
        this.afterChiPruneCount = afterPrune2.size();

        System.out.println("    Pruning 2 (chi-square >= " + chiSquareThreshold + "): "
            + afterPrune1.size() + " -> " + afterPrune2.size());

        // --- Pruning 3: Database coverage (Algorithm 1 in paper) ---
        this.rules = pruneByDatabaseCoverage(afterPrune2, trainData);
        this.afterCoveragePruneCount = this.rules.size();

        System.out.println("    Pruning 3 (db coverage, δ=" + coverageThreshold + "):  "
            + afterPrune2.size() + " -> " + this.rules.size());

        System.out.println("    Final rules: " + this.rules.size()
            + " (from " + candidateRules.size() + " candidates)");
    }

    // -----------------------------------------------------------------------
    // Pruning 1: General Rule Pruning
    // -----------------------------------------------------------------------

    /**
     * Removes any rule r1 for which a higher-ranked rule r2 exists with:
     *   class(r2) == class(r1)  AND  condset(r2) ⊆ condset(r1)
     *
     * Since the input is sorted by precedence, every rule already in `kept`
     * has equal or higher rank than the current candidate.
     */
    private List<AssociationRule> pruneByGeneralRules(List<AssociationRule> sorted) {
        List<AssociationRule> kept = new ArrayList<>();
        // Index kept rules by class for faster lookup
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
    // Pruning 2: Chi-Square Significance Test
    // -----------------------------------------------------------------------

    /**
     * Keeps only rules that are positively correlated with their class,
     * i.e., χ²(rule) >= chiSquareThreshold.
     *
     * Paper Section 3.3: "Only the rules that are positively correlated,
     * i.e., those with χ² value passing a significance level threshold,
     * are used for later classification. All the other rules are pruned."
     */
    private List<AssociationRule> pruneByChiSquareSignificance(
            List<AssociationRule> rules) {
        List<AssociationRule> kept = new ArrayList<>();

        for (AssociationRule r : rules) {
            double chi2 = computeChiSquare(r, r.getClassLabel());
            // Paper Section 3.3: keep only POSITIVELY correlated rules
            // Positive correlation means observed (a) > expected E(a)
            // Equivalently: a*d > b*c in the 2x2 contingency table
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

    /**
     * Algorithm 1 from the paper (Section 3.3):
     *
     *   1. Sort rules in rank descending order (already sorted).
     *   2. For each data object in training, set cover-count to 0.
     *   3. While both training set and rule set are not empty:
     *      - For each rule R in rank descending order:
     *        - Find all remaining objects that R correctly classifies.
     *        - If R correctly classifies at least one object → select R,
     *          increment cover-count for those objects.
     *        - Remove objects whose cover-count >= δ.
     *   4. Return selected rules.
     */
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
                // Rule must MATCH the record AND correctly classify it
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

            if (coversAny) {
                selected.add(rule);
            }
        }

        return selected;
    }

    // -----------------------------------------------------------------------
    // Classification (Section 4 of the paper)
    // -----------------------------------------------------------------------

    /**
     * Classifies a single test record using the weighted chi-square method.
     *
     * Paper Section 4:
     *   1. Collect all rules matching the test record.
     *   2. If no rules match → return default class.
     *   3. If all rules agree on one class → return that class.
     *   4. Otherwise, group rules by class.
     *      For each group, score = Σ χ²(r) / maxχ²(r).
     *      Return the class with the highest score.
     */
    public String classify(Transaction record) {
        // Collect all matching rules
        List<AssociationRule> matching = new ArrayList<>();
        for (AssociationRule rule : rules) {
            if (rule.matches(record)) {
                matching.add(rule);
            }
        }

        if (matching.isEmpty()) {
            return defaultClass;
        }

        // Group by class label
        Map<String, List<AssociationRule>> byClass = new HashMap<>();
        for (AssociationRule rule : matching) {
            byClass.computeIfAbsent(rule.getClassLabel(), k -> new ArrayList<>())
                   .add(rule);
        }

        // Unanimous class
        if (byClass.size() == 1) {
            return byClass.keySet().iterator().next();
        }

        // Multiple classes: weighted chi-square voting (Section 4)
        // Paper: weight(r) = χ²(r)/maxχ²(r)
        //        weighted_χ²(r) = χ²(r) × weight(r) = [χ²(r)]² / maxχ²(r)
        //        score(G) = Σ [χ²(r)]² / maxχ²(r)
        String bestClass = defaultClass;
        double bestScore = -1.0;

        for (Map.Entry<String, List<AssociationRule>> entry : byClass.entrySet()) {
            String cls = entry.getKey();
            double score = 0.0;

            for (AssociationRule rule : entry.getValue()) {
                double chi2    = computeChiSquare(rule, cls);
                double maxChi2 = computeMaxChiSquare(rule, cls);
                if (maxChi2 > 0) {
                    score += (chi2 * chi2) / maxChi2;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestClass = cls;
            }
        }

        return bestClass;
    }

    // -----------------------------------------------------------------------
    // Chi-Square Computation
    // -----------------------------------------------------------------------

    /**
     * Computes χ² for a single rule using the 2×2 contingency table.
     *
     *   χ² = n × (a·d − b·c)² / ((a+b)(c+d)(a+c)(b+d))
     *
     * where:
     *   a = sup(P ∩ C)      = rule.supportCount
     *   b = sup(P ∧ ¬C)     = condsetSupportCount − a
     *   c = sup(¬P ∧ C)     = classFreq[C] − a
     *   d = sup(¬P ∧ ¬C)    = n − a − b − c
     */
    private double computeChiSquare(AssociationRule rule, String cls) {
        double n = totalTransactions;
        double a = rule.getSupportCount();
        double b = rule.getCondsetSupportCount() - a;
        double c = classFreq.getOrDefault(cls, 0) - a;
        double d = n - a - b - c;

        double denom = (a + b) * (c + d) * (a + c) * (b + d);
        if (denom == 0) return 0.0;

        return n * Math.pow(a * d - b * c, 2) / denom;
    }

    /**
     * Computes maxχ² — the upper bound of χ² for fixed marginals.
     *
     * The maximum χ² occurs when the observed cell (P ∩ C) is as far
     * from the expected value as possible. This happens when:
     *   a = min(sup(P), sup(C))
     *
     * With a = min(sup(P), sup(C)):
     *   b = sup(P) − a
     *   c = sup(C) − a
     *   d = n − sup(P) − sup(C) + a
     *
     * Then apply the same 2×2 χ² formula.
     */
    private double computeMaxChiSquare(AssociationRule rule, String cls) {
        double n = totalTransactions;
        double supP = rule.getCondsetSupportCount();  // sup(P)
        double supC = classFreq.getOrDefault(cls, 0); // sup(C)

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

    /** Predicts a class label for each record in testData. */
    public List<String> predict(List<Transaction> testData) {
        List<String> predictions = new ArrayList<>();
        for (Transaction t : testData) {
            predictions.add(classify(t));
        }
        return predictions;
    }

    /** Returns classification accuracy on testData. */
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
    public String getDefaultClass()           { return defaultClass; }
    public int getCandidateCount()            { return candidateCount; }
    public int getAfterGeneralPruneCount()    { return afterGeneralPruneCount; }
    public int getAfterChiPruneCount()        { return afterChiPruneCount; }
    public int getAfterCoveragePruneCount()   { return afterCoveragePruneCount; }
    public double getChiSquareThreshold()     { return chiSquareThreshold; }
    public int getCoverageThreshold()         { return coverageThreshold; }
    public Map<String, Integer> getClassFreq() { return classFreq; }
    public int getTotalTransactions()         { return totalTransactions; }
}
