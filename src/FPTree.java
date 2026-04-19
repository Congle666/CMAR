import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CR-tree (class-distribution-aware FP-tree) — Li, Han & Pei (2001) §3.2.
 *
 * Differences vs. a plain FP-tree:
 *   - Each node additionally tracks a per-class count distribution.
 *   - Paths are inserted together with the class label of the contributing
 *     transaction, so at every node we know how many transactions of each
 *     class pass through it. This is what lets FP-Growth emit CARs directly
 *     (no post-hoc scan required).
 */
public class FPTree {

    final FPNode root;

    /** Total frequency of each item across all inserted paths. */
    final Map<String, Integer> headerFreq;

    /** First node in the linked chain for each item (header table pointer). */
    final Map<String, FPNode> headerFirst;

    final int minSupport;

    public FPTree(int minSupport) {
        this.root        = new FPNode(null, 0, null);
        this.headerFreq  = new HashMap<>();
        this.headerFirst = new HashMap<>();
        this.minSupport  = minSupport;
    }

    /**
     * Inserts a pre-sorted, pre-filtered path with a given weight (count)
     * and a class distribution carried by the contributing transactions.
     *
     * For the initial tree, classDist is {classLabel -> 1} for a single row.
     * For conditional trees, classDist comes from the header-chain node whose
     * subtree produced this prefix path, so the full per-class counts are
     * propagated into the conditional tree.
     */
    public void insertPath(List<String> path,
                           Map<String, Integer> classDist,
                           int count) {
        FPNode current = root;

        for (String item : path) {
            FPNode child = current.children.get(item);
            if (child == null) {
                child = new FPNode(item, 0, current);
                current.children.put(item, child);

                // Append to the end of the header-table chain
                FPNode tail = headerFirst.get(item);
                if (tail == null) {
                    headerFirst.put(item, child);
                } else {
                    while (tail.nodeLink != null) tail = tail.nodeLink;
                    tail.nodeLink = child;
                }
            }
            child.count += count;
            for (Map.Entry<String, Integer> e : classDist.entrySet()) {
                child.classCount.merge(e.getKey(), e.getValue(), Integer::sum);
            }
            current = child;
        }
    }

    /** Items sorted by frequency descending (tree-insertion order). */
    public List<String> getItemsSortedByFreqDesc() {
        List<String> items = new ArrayList<>(headerFreq.keySet());
        items.sort((a, b) -> headerFreq.get(b) - headerFreq.get(a));
        return items;
    }

    /** Items sorted by frequency ascending (FP-Growth mining order). */
    public List<String> getItemsSortedByFreqAsc() {
        List<String> items = new ArrayList<>(headerFreq.keySet());
        items.sort(Comparator.comparingInt(headerFreq::get));
        return items;
    }

    /** Returns true if the tree consists of a single path from root to leaf. */
    public boolean isSinglePath() {
        FPNode node = root;
        while (!node.children.isEmpty()) {
            if (node.children.size() > 1) return false;
            node = node.children.values().iterator().next();
        }
        return true;
    }

    /** Returns a multi-line text representation of the tree structure. */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[root]\n");
        printNode(root, sb, "", true);
        return sb.toString();
    }

    private void printNode(FPNode node, StringBuilder sb, String prefix, boolean isRoot) {
        List<FPNode> kids = new ArrayList<>(node.children.values());
        for (int i = 0; i < kids.size(); i++) {
            FPNode child = kids.get(i);
            boolean last = (i == kids.size() - 1);
            sb.append(prefix)
              .append(last ? "└── " : "├── ")
              .append(child.item).append(":").append(child.count)
              .append(" ").append(child.classCount)
              .append("\n");
            printNode(child, sb, prefix + (last ? "    " : "│   "), false);
        }
    }

    /** Returns the header table as a formatted string. */
    public String headerTableToString() {
        StringBuilder sb = new StringBuilder();
        List<String> sorted = getItemsSortedByFreqDesc();
        sb.append(String.format("%-30s %s%n", "Item", "Frequency"));
        sb.append("-".repeat(42)).append("\n");
        for (String item : sorted) {
            sb.append(String.format("%-30s %d%n", item, headerFreq.get(item)));
        }
        return sb.toString();
    }
}
