import java.util.ArrayList;
import java.util.List;

/**
 * Biểu diễn một bản ghi dữ liệu dưới dạng danh sách phẳng các item
 * thuộc tính-giá trị (ví dụ "outlook=sunny", "temperature=hot") kèm
 * theo nhãn lớp (class label).
 */
public class Transaction {

    private final List<String> items;
    private final String classLabel;

    public Transaction(List<String> items, String classLabel) {
        this.items = new ArrayList<>(items);
        this.classLabel = classLabel;
    }

    /** Chỉ gồm các item thuộc tính-giá trị (không kèm class). */
    public List<String> getItems() {
        return items;
    }

    /** Nhãn lớp của bản ghi này. */
    public String getClassLabel() {
        return classLabel;
    }

    /**
     * Trả về toàn bộ item bao gồm cả item lớp dạng "class=&lt;label&gt;".
     * Dùng khi xây FP-tree.
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
