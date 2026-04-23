import java.util.Collections;
import java.util.Set;

/**
 * Một frequent itemset bất biến kèm số đếm support tuyệt đối.
 */
public class FrequentPattern {

    private final Set<String> items;
    private final int support;

    public FrequentPattern(Set<String> items, int support) {
        this.items = Collections.unmodifiableSet(items);
        this.support = support;
    }

    public Set<String> getItems() {
        return items;
    }

    public int getSupport() {
        return support;
    }

    @Override
    public String toString() {
        return items + " [sup=" + support + "]";
    }
}
