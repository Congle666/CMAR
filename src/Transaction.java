import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single data record as a flat list of attribute-value items
 * (e.g. "outlook=sunny", "temperature=hot") plus a class label.
 */
public class Transaction {

    private final List<String> items;
    private final String classLabel;

    public Transaction(List<String> items, String classLabel) {
        this.items = new ArrayList<>(items);
        this.classLabel = classLabel;
    }

    /** Attribute-value items only (no class). */
    public List<String> getItems() {
        return items;
    }

    /** The class label for this record. */
    public String getClassLabel() {
        return classLabel;
    }

    /**
     * Returns all items including the class item "class=&lt;label&gt;".
     * Used when building the FP-tree.
     */
    public List<String> getAllItems() {
        List<String> all = new ArrayList<>(items);
        all.add("class=" + classLabel);
        return all;
    }

    @Override
    public String toString() {
        return items + " -> " + classLabel;
    }
}
