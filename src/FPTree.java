import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FP-Tree data structure.
 *
 * Maintains:
 *   - A root node (null item, count 0)
 *   - A header table: item -> frequency and item -> first node in linked chain
 *
 * Both the initial tree and every conditional FP-tree use this class.
 */
public class FPTree {

    final FPNode root;

    /** Total frequency of each item across all inserted paths. */
    final Map<String, Integer> headerFreq;

    /** First node in the linked chain for each item (header table pointer). */
    final Map<String, FPNode> headerFirst;

    final int minSupport;

    public FPTree(int minSupport) {
        this.root = new FPNode(null, 0, null);
        this.headerFreq = new HashMap<>();
        this.headerFirst = new HashMap<>();
        this.minSupport = minSupport;
    }

    /**
     * Inserts a pre-sorted, pre-filtered path with a given weight (count).
     * Updates the header-table node-link chains.
     */
    public void insertPath(List<String> path, int count) {
        FPNode current = root;

        for (String item : path) {
            if (current.children.containsKey(item)) {
                current.children.get(item).count += count;
            } else {
                FPNode newNode = new FPNode(item, count, current);
                current.children.put(item, newNode);

                // Append to the end of the header-table chain
                if (!headerFirst.containsKey(item)) {
                    headerFirst.put(item, newNode);
                } else {
                    FPNode tail = headerFirst.get(item);
                    while (tail.nodeLink != null) tail = tail.nodeLink;
                    tail.nodeLink = newNode;
                }
            }
            current = current.children.get(item);
        }
    }

    /**
     * Items sorted by frequency descending.
     * Used to order items when inserting into the tree (most frequent first).
     */
    public List<String> getItemsSortedByFreqDesc() {
        List<String> items = new ArrayList<>(headerFreq.keySet());
        items.sort((a, b) -> headerFreq.get(b) - headerFreq.get(a));
        return items;
    }

    /**
     * Items sorted by frequency ascending.
     * Used as the mining order in FP-Growth (least frequent / deepest first).
     */
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
