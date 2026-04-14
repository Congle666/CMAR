import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FP-Growth algorithm for mining all frequent itemsets.
 *
 * Items passed in are already labeled (e.g. "outlook=sunny", "class=yes").
 * The miner treats class items the same as regular items; the caller
 * (RuleGenerator) is responsible for extracting CARs from the results.
 *
 * Algorithm outline:
 *   1. Count item frequencies; remove items below minSupport.
 *   2. Build initial FP-tree (items sorted by freq DESC within each transaction).
 *   3. For each item in the header table (processed in freq ASC order):
 *        a. Record the pattern {prefix ∪ item} with its support.
 *        b. Collect all prefix paths (conditional pattern base).
 *        c. Build a conditional FP-tree from those prefix paths.
 *        d. Recurse with {prefix ∪ item} as the new prefix.
 */
public class FPGrowth {

    private final int minSupport;
    private int maxPatternLength = Integer.MAX_VALUE; // default: unlimited
    private final List<FrequentPattern> result;
    private FPTree initialTree;  // keep reference to the initial FP-tree

    public FPGrowth(int minSupport) {
        this.minSupport = minSupport;
        this.result = new ArrayList<>();
    }

    /** Sets max pattern length to limit memory usage on high-dimensional data. */
    public void setMaxPatternLength(int maxLen) {
        this.maxPatternLength = maxLen;
    }

    /** Returns the initial FP-tree built during the last call to mine(). */
    public FPTree getInitialTree() {
        return initialTree;
    }

    /**
     * Mines all frequent itemsets from a list of transactions.
     * Each transaction is a list of item strings.
     *
     * @param transactions list of item lists (each item list = one transaction)
     * @return all frequent patterns with support >= minSupport
     */
    public List<FrequentPattern> mine(List<List<String>> transactions) {
        result.clear();

        // --- Step 1: Count global item frequencies ---
        Map<String, Integer> freq = new HashMap<>();
        for (List<String> trans : transactions) {
            for (String item : trans) {
                freq.merge(item, 1, Integer::sum);
            }
        }
        freq.entrySet().removeIf(e -> e.getValue() < minSupport);
        if (freq.isEmpty()) return result;

        // --- Step 2: Build the initial FP-tree ---
        FPTree tree = buildTree(transactions, freq);
        this.initialTree = tree;  // save for report generation

        // --- Step 3: Mine recursively ---
        mineTree(tree, new ArrayList<>());

        return result;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Builds an FP-tree from transactions filtered and sorted by freq. */
    private FPTree buildTree(List<List<String>> transactions,
                             Map<String, Integer> freq) {
        FPTree tree = new FPTree(minSupport);
        tree.headerFreq.putAll(freq);

        for (List<String> trans : transactions) {
            // Keep only frequent items; sort by freq DESC for tree sharing
            List<String> path = trans.stream()
                .filter(freq::containsKey)
                .sorted((a, b) -> { int c = freq.get(b) - freq.get(a); return c != 0 ? c : a.compareTo(b); })
                .collect(Collectors.toList());

            if (!path.isEmpty()) {
                tree.insertPath(path, 1);
            }
        }

        return tree;
    }

    /**
     * Recursive mining step.
     *
     * @param tree   the (conditional) FP-tree to mine
     * @param prefix items already chosen in the current recursion path
     */
    private void mineTree(FPTree tree, List<String> prefix) {
        // Stop recursion if pattern length limit reached
        if (prefix.size() >= maxPatternLength) return;

        // Process items from least frequent to most frequent (bottom-up)
        for (String item : tree.getItemsSortedByFreqAsc()) {
            int itemSupport = tree.headerFreq.get(item);

            // Record the new frequent pattern: prefix ∪ {item}
            List<String> newPattern = new ArrayList<>(prefix);
            newPattern.add(item);
            Set<String> patternSet = new HashSet<>(newPattern);
            result.add(new FrequentPattern(patternSet, itemSupport));

            // --- Build conditional pattern base ---
            List<List<String>> condBase = new ArrayList<>();
            List<Integer> condCounts = new ArrayList<>();

            FPNode node = tree.headerFirst.get(item);
            while (node != null) {
                // Trace the path from this node up to (not including) root
                List<String> prefixPath = new ArrayList<>();
                FPNode ancestor = node.parent;
                while (!ancestor.isRoot()) {
                    prefixPath.add(0, ancestor.item);
                    ancestor = ancestor.parent;
                }
                if (!prefixPath.isEmpty()) {
                    condBase.add(prefixPath);
                    condCounts.add(node.count);
                }
                node = node.nodeLink;
            }

            if (condBase.isEmpty()) continue;

            // --- Count item frequencies in the conditional database ---
            Map<String, Integer> condFreq = new HashMap<>();
            for (int i = 0; i < condBase.size(); i++) {
                int cnt = condCounts.get(i);
                for (String condItem : condBase.get(i)) {
                    condFreq.merge(condItem, cnt, Integer::sum);
                }
            }
            condFreq.entrySet().removeIf(e -> e.getValue() < minSupport);
            if (condFreq.isEmpty()) continue;

            // --- Build conditional FP-tree ---
            FPTree condTree = new FPTree(minSupport);
            condTree.headerFreq.putAll(condFreq);

            for (int i = 0; i < condBase.size(); i++) {
                List<String> sortedPath = condBase.get(i).stream()
                    .filter(condFreq::containsKey)
                    .sorted((a, b) -> { int c = condFreq.get(b) - condFreq.get(a); return c != 0 ? c : a.compareTo(b); })
                    .collect(Collectors.toList());

                if (!sortedPath.isEmpty()) {
                    condTree.insertPath(sortedPath, condCounts.get(i));
                }
            }

            // --- Recurse with the extended prefix ---
            mineTree(condTree, newPattern);
        }
    }
}
