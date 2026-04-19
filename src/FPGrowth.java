import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CMAR rule-mining engine — Li, Han & Pei (2001) §3.2.
 *
 * Unlike plain FP-Growth (which treats class items as regular items and
 * requires a post-hoc rule extraction pass), this miner works on a
 * class-distribution-aware CR-tree: every node keeps per-class counts,
 * and Class Association Rules are emitted directly during the recursive
 * mining step without an intermediate "class=..." item hack.
 *
 * Pipeline:
 *   1. Count item and class frequencies; drop items below minSupport.
 *   2. Build the initial CR-tree (each path carries its transaction's class).
 *   3. Recursively mine: for every frequent pattern P, read its per-class
 *      support directly from the header-chain's accumulated classCounts
 *      and emit one CAR per class c whose supCount(P, c) ≥ minSupport and
 *      confidence ≥ minConfidence.
 */
public class FPGrowth {

    private final int minSupport;
    private int maxPatternLength = Integer.MAX_VALUE;

    private final List<FrequentPattern> patterns = new ArrayList<>();
    private final List<AssociationRule> rules    = new ArrayList<>();

    private FPTree initialTree;

    // Cached during mine() so mineTree() can emit CARs without long parameter lists.
    private double minConfidence;
    private int    totalTransactions;

    public FPGrowth(int minSupport) {
        this.minSupport = minSupport;
    }

    /** Sets max pattern length to limit memory usage on high-dimensional data. */
    public void setMaxPatternLength(int maxLen) {
        this.maxPatternLength = maxLen;
    }

    /** Initial CR-tree built during the last mine() call. */
    public FPTree getInitialTree() {
        return initialTree;
    }

    /** Frequent patterns discovered during the last mine() call (for reports). */
    public List<FrequentPattern> getPatterns() {
        return patterns;
    }

    /** Class Association Rules produced directly during the last mine() call. */
    public List<AssociationRule> getRules() {
        return rules;
    }

    /**
     * Mines Class Association Rules from training transactions.
     *
     * @param trainData     training transactions (items + class label)
     * @param minConfidence minimum rule confidence threshold
     * @return list of CARs (sorted by rule precedence)
     */
    public List<AssociationRule> mine(List<Transaction> trainData,
                                      double minConfidence) {
        patterns.clear();
        rules.clear();
        this.minConfidence     = minConfidence;
        this.totalTransactions = trainData.size();

        // --- Step 1: global item frequencies (non-class attributes only) ---
        Map<String, Integer> freq = new HashMap<>();
        for (Transaction t : trainData) {
            for (String item : t.getItems()) {
                freq.merge(item, 1, Integer::sum);
            }
        }
        freq.entrySet().removeIf(e -> e.getValue() < minSupport);
        if (freq.isEmpty()) {
            this.initialTree = new FPTree(minSupport);
            return rules;
        }

        // --- Step 2: build initial CR-tree ---
        FPTree tree = new FPTree(minSupport);
        tree.headerFreq.putAll(freq);

        for (Transaction t : trainData) {
            List<String> path = t.getItems().stream()
                .filter(freq::containsKey)
                .sorted((a, b) -> {
                    int c = freq.get(b) - freq.get(a);
                    return c != 0 ? c : a.compareTo(b);
                })
                .collect(Collectors.toList());

            if (!path.isEmpty()) {
                Map<String, Integer> classDist = new HashMap<>();
                classDist.put(t.getClassLabel(), 1);
                tree.insertPath(path, classDist, 1);
            }
        }
        this.initialTree = tree;

        // --- Step 3: recursive mining ---
        mineTree(tree, new ArrayList<>());

        Collections.sort(rules);  // CMAR precedence ordering
        return rules;
    }

    // -----------------------------------------------------------------------
    // Recursive mining — class-aware
    // -----------------------------------------------------------------------

    private void mineTree(FPTree tree, List<String> prefix) {
        if (prefix.size() >= maxPatternLength) return;

        for (String item : tree.getItemsSortedByFreqAsc()) {
            int itemSupport = tree.headerFreq.get(item);

            // Pattern P = prefix ∪ {item}
            List<String> newPattern = new ArrayList<>(prefix);
            newPattern.add(item);
            HashSet<String> patternSet = new HashSet<>(newPattern);
            patterns.add(new FrequentPattern(patternSet, itemSupport));

            // --- Collect per-class distribution for P via header chain ---
            Map<String, Integer> classDistForP = new HashMap<>();
            FPNode node = tree.headerFirst.get(item);
            while (node != null) {
                for (Map.Entry<String, Integer> e : node.classCount.entrySet()) {
                    classDistForP.merge(e.getKey(), e.getValue(), Integer::sum);
                }
                node = node.nodeLink;
            }

            // --- Emit one CAR per class that meets minSupport and minConfidence ---
            for (Map.Entry<String, Integer> e : classDistForP.entrySet()) {
                String cls       = e.getKey();
                int    classSup  = e.getValue();
                if (classSup < minSupport) continue;

                double confidence = (double) classSup / itemSupport;
                if (confidence < minConfidence) continue;

                double support = (double) classSup / totalTransactions;
                rules.add(new AssociationRule(
                    new HashSet<>(patternSet),
                    cls,
                    support, confidence,
                    classSup, itemSupport
                ));
            }

            // --- Build conditional pattern base with class distributions ---
            List<List<String>>            condBase       = new ArrayList<>();
            List<Integer>                 condCounts     = new ArrayList<>();
            List<Map<String, Integer>>    condClassDists = new ArrayList<>();

            node = tree.headerFirst.get(item);
            while (node != null) {
                List<String> prefixPath = new ArrayList<>();
                FPNode ancestor = node.parent;
                while (!ancestor.isRoot()) {
                    prefixPath.add(0, ancestor.item);
                    ancestor = ancestor.parent;
                }
                if (!prefixPath.isEmpty()) {
                    condBase.add(prefixPath);
                    condCounts.add(node.count);
                    // Copy classCount so later mutations don't leak back to this node
                    condClassDists.add(new HashMap<>(node.classCount));
                }
                node = node.nodeLink;
            }

            if (condBase.isEmpty()) continue;

            // --- Frequency inside the conditional base ---
            Map<String, Integer> condFreq = new HashMap<>();
            for (int i = 0; i < condBase.size(); i++) {
                int cnt = condCounts.get(i);
                for (String condItem : condBase.get(i)) {
                    condFreq.merge(condItem, cnt, Integer::sum);
                }
            }
            condFreq.entrySet().removeIf(e -> e.getValue() < minSupport);
            if (condFreq.isEmpty()) continue;

            // --- Build conditional CR-tree, propagating class distributions ---
            FPTree condTree = new FPTree(minSupport);
            condTree.headerFreq.putAll(condFreq);

            for (int i = 0; i < condBase.size(); i++) {
                List<String> sortedPath = condBase.get(i).stream()
                    .filter(condFreq::containsKey)
                    .sorted((a, b) -> {
                        int c = condFreq.get(b) - condFreq.get(a);
                        return c != 0 ? c : a.compareTo(b);
                    })
                    .collect(Collectors.toList());

                if (!sortedPath.isEmpty()) {
                    condTree.insertPath(
                        sortedPath,
                        condClassDists.get(i),
                        condCounts.get(i));
                }
            }

            // --- Recurse with the extended prefix ---
            mineTree(condTree, newPattern);
        }
    }
}
