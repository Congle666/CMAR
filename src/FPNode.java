import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single node in the FP-Tree.
 * Each node stores an item name, its count in the tree, a pointer to its
 * parent, a map of child nodes, and a horizontal node-link used by the
 * header table to chain all nodes sharing the same item.
 */
public class FPNode {

    String item;
    int count;
    FPNode parent;
    Map<String, FPNode> children;
    FPNode nodeLink; // links nodes with the same item together (header chain)

    public FPNode(String item, int count, FPNode parent) {
        this.item = item;
        this.count = count;
        this.parent = parent;
        this.children = new LinkedHashMap<>();
        this.nodeLink = null;
    }

    /** The root node has a null item. */
    public boolean isRoot() {
        return item == null;
    }

    @Override
    public String toString() {
        return "[" + item + ":" + count + "]";
    }
}
