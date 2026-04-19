import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single node in the CR-tree (extended FP-tree used by CMAR).
 *
 * Compared to a plain FP-tree node, this node additionally stores a
 * per-class count distribution — the number of transactions that pass
 * through this node broken down by class label. This is the key
 * extension in Li, Han & Pei (2001) Section 3.2 that lets CMAR generate
 * Class Association Rules directly during FP-Growth mining without a
 * separate pass.
 */
public class FPNode {

    String item;
    int count;                         // total transactions through this node
    FPNode parent;
    Map<String, FPNode> children;
    FPNode nodeLink;                   // header-table chain for this item

    /** Class-label distribution among transactions passing through this node. */
    Map<String, Integer> classCount;

    public FPNode(String item, int count, FPNode parent) {
        this.item       = item;
        this.count      = count;
        this.parent     = parent;
        this.children   = new LinkedHashMap<>();
        this.nodeLink   = null;
        this.classCount = new HashMap<>();
    }

    /** The root node has a null item. */
    public boolean isRoot() {
        return item == null;
    }

    @Override
    public String toString() {
        return "[" + item + ":" + count + " " + classCount + "]";
    }
}
