import java.util.Collections;
import java.util.Set;

/**
 * A Class Association Rule (CAR) of the form: condset => classLabel
 *
 * Metrics stored:
 *   support          - relative support of (condset ∪ class) over all training records
 *   confidence       - support(condset ∪ class) / support(condset)
 *   supportCount     - absolute count of transactions containing condset ∪ class
 *   condsetSupportCount - absolute count of transactions containing condset
 *
 * Ordering (Comparable): confidence DESC, support DESC, condset size ASC.
 * This matches the CMAR rule precedence used for pruning and classification.
 */
public class AssociationRule implements Comparable<AssociationRule> {

    private final Set<String> condset;
    private final String classLabel;
    private final double support;
    private final double confidence;
    private final int supportCount;
    private final int condsetSupportCount;

    public AssociationRule(Set<String> condset, String classLabel,
                           double support, double confidence,
                           int supportCount, int condsetSupportCount) {
        this.condset = Collections.unmodifiableSet(condset);
        this.classLabel = classLabel;
        this.support = support;
        this.confidence = confidence;
        this.supportCount = supportCount;
        this.condsetSupportCount = condsetSupportCount;
    }

    // --- Getters ---

    public Set<String> getCondset() { return condset; }
    public String getClassLabel() { return classLabel; }
    public double getSupport() { return support; }
    public double getConfidence() { return confidence; }
    public int getSupportCount() { return supportCount; }
    public int getCondsetSupportCount() { return condsetSupportCount; }

    /**
     * Returns true if all items in this rule's condset appear in the given
     * transaction's item list (condset ⊆ transaction items).
     */
    public boolean matches(Transaction t) {
        return t.getItems().containsAll(condset);
    }

    /**
     * Rule precedence ordering:
     *   1. Higher confidence first.
     *   2. Break ties with higher support.
     *   3. Break ties with smaller condset (simpler rule).
     */
    @Override
    public int compareTo(AssociationRule other) {
        int cmp = Double.compare(other.confidence, this.confidence);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(other.supportCount, this.supportCount);
        if (cmp != 0) return cmp;
        return Integer.compare(this.condset.size(), other.condset.size());
    }

    @Override
    public String toString() {
        return condset + " => " + classLabel
            + " [sup=" + String.format("%.4f", support)
            + ", conf=" + String.format("%.4f", confidence) + "]";
    }
}
