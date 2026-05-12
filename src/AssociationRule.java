import java.util.Collections;
import java.util.Set;

/**
 * Một luật kết hợp theo lớp (Class Association Rule - CAR): {@code condset => classLabel}.
 *
 * <p>Các chỉ số được lưu:</p>
 * <ul>
 *   <li><b>support</b> — support tương đối của (condset ∪ class) trên toàn bộ tập huấn luyện</li>
 *   <li><b>confidence</b> — support(condset ∪ class) / support(condset)</li>
 *   <li><b>supportCount</b> — số tuyệt đối các transaction chứa condset ∪ class</li>
 *   <li><b>condsetSupportCount</b> — số tuyệt đối các transaction chứa condset</li>
 * </ul>
 *
 * <p>Thứ tự (Comparable): confidence giảm dần, support giảm dần, độ dài condset
 * tăng dần. Đây là thứ tự ưu tiên luật (rule precedence) mà CMAR dùng để cắt tỉa
 * và phân lớp.</p>
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

    public Set<String> getCondset()          { return condset; }
    public String getClassLabel()            { return classLabel; }
    public double getSupport()               { return support; }
    public double getConfidence()            { return confidence; }
    public int    getSupportCount()          { return supportCount; }
    public int    getCondsetSupportCount()   { return condsetSupportCount; }

    /** condset ⊆ items của transaction. */
    public boolean matches(Transaction t) {
        return t.getItems().containsAll(condset);
    }

    /**
     * Thứ tự ưu tiên luật:
     *   1. Confidence cao hơn xếp trước.
     *   2. Trùng confidence: support cao hơn xếp trước.
     *   3. Trùng tiếp: condset nhỏ hơn (luật đơn giản hơn) xếp trước.
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
