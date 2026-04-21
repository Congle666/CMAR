import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CR-tree (Class-Association-Rule tree) for compact rule storage —
 * Li, Han & Pei (2001) §3.3.
 *
 * Each rule's condset is inserted as a path from the root, with items
 * sorted by global frequency descending so that rules sharing common
 * items share tree nodes. A node holds the rules whose condset ends at
 * that node (a leaf rule, or an intermediate rule if a longer rule
 * extends the path).
 *
 * Retrieval for a test record is a DFS that prunes any subtree whose
 * item is not present in the record — saving the linear scan that a
 * flat rule list would require.
 */
public class CRTree {

    private static class Node {
        final String item;
        final Map<String, Node> children = new HashMap<>();
        final List<AssociationRule> rules = new ArrayList<>();
        Node(String item) { this.item = item; }
    }

    private final Node root = new Node(null);
    private final Map<String, Integer> itemOrder;  // item -> rank (lower = more frequent)
    private int size = 0;

    /**
     * @param itemFreq global item frequencies — used to order items along
     *                 each path (frequency DESC → better prefix sharing)
     */
    public CRTree(Map<String, Integer> itemFreq) {
        this.itemOrder = new HashMap<>();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(itemFreq.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        for (int i = 0; i < entries.size(); i++) {
            itemOrder.put(entries.get(i).getKey(), i);
        }
    }

    /** Inserts a rule; its condset is sorted by item frequency DESC first. */
    public void insert(AssociationRule rule) {
        List<String> sortedItems = new ArrayList<>(rule.getCondset());
        sortedItems.sort((a, b) -> {
            Integer ra = itemOrder.get(a);
            Integer rb = itemOrder.get(b);
            int ia = (ra == null ? Integer.MAX_VALUE : ra);
            int ib = (rb == null ? Integer.MAX_VALUE : rb);
            if (ia != ib) return Integer.compare(ia, ib);
            return a.compareTo(b);
        });

        Node cur = root;
        for (String item : sortedItems) {
            cur = cur.children.computeIfAbsent(item, Node::new);
        }
        cur.rules.add(rule);
        size++;
    }

    /** Inserts all rules. */
    public void insertAll(Collection<AssociationRule> rules) {
        for (AssociationRule r : rules) insert(r);
    }

    /** Number of rules stored. */
    public int size() { return size; }

    /**
     * Returns every rule whose condset is a subset of the test record's items.
     *
     * DFS with pruning: at each child node, if its item is absent from the
     * record we skip the entire subtree — any rule passing through that
     * node requires the missing item and cannot match.
     */
    public List<AssociationRule> findMatching(Transaction record) {
        List<AssociationRule> out = new ArrayList<>();
        Set<String> items = new java.util.HashSet<>(record.getItems());
        collect(root, items, out);
        return out;
    }

    private void collect(Node node, Set<String> items, List<AssociationRule> out) {
        out.addAll(node.rules);
        for (Node child : node.children.values()) {
            if (items.contains(child.item)) {
                collect(child, items, out);
            }
        }
    }
}
