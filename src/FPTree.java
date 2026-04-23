import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CR-tree (FP-tree mở rộng có phân phối lớp) — Li, Han & Pei (2001) §3.2.
 *
 * Khác biệt so với FP-tree chuẩn:
 *   - Mỗi nút còn lưu thêm phân phối số đếm theo từng lớp.
 *   - Khi chèn một path, ta kèm theo nhãn lớp của transaction đóng
 *     góp path đó; nhờ vậy tại mỗi nút ta biết có bao nhiêu transaction
 *     của mỗi lớp đi qua nó. Đây chính là điểm cho phép FP-Growth
 *     sinh CAR trực tiếp (không cần pass hậu kỳ để quét lại).
 */
public class FPTree {

    final FPNode root;

    /** Tần suất toàn cục của mỗi item trên tất cả các path đã chèn. */
    final Map<String, Integer> headerFreq;

    /** Con trỏ tới nút đầu tiên trong chuỗi liên kết của mỗi item (header-table). */
    final Map<String, FPNode> headerFirst;

    final int minSupport;

    public FPTree(int minSupport) {
        this.root        = new FPNode(null, 0, null);
        this.headerFreq  = new HashMap<>();
        this.headerFirst = new HashMap<>();
        this.minSupport  = minSupport;
    }

    /**
     * Chèn một path đã được sắp xếp và lọc trước, với trọng số (count)
     * và phân phối lớp do các transaction đóng góp mang theo.
     *
     * Với cây ban đầu, classDist là {classLabel -> 1} cho một hàng đơn lẻ.
     * Với cây điều kiện (conditional tree), classDist đến từ nút trên chuỗi
     * header-table mà subtree của nó sinh ra prefix path này, nhờ đó phân
     * phối số đếm theo lớp được lan truyền đầy đủ vào cây điều kiện.
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

                // Nối vào cuối chuỗi header-table
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

    /** Các item sắp theo tần suất giảm dần (thứ tự chèn vào cây). */
    public List<String> getItemsSortedByFreqDesc() {
        List<String> items = new ArrayList<>(headerFreq.keySet());
        items.sort((a, b) -> headerFreq.get(b) - headerFreq.get(a));
        return items;
    }

    /** Các item sắp theo tần suất tăng dần (thứ tự khai thác của FP-Growth). */
    public List<String> getItemsSortedByFreqAsc() {
        List<String> items = new ArrayList<>(headerFreq.keySet());
        items.sort(Comparator.comparingInt(headerFreq::get));
        return items;
    }

    /** Trả về true nếu cây chỉ là một path đơn từ gốc xuống lá. */
    public boolean isSinglePath() {
        FPNode node = root;
        while (!node.children.isEmpty()) {
            if (node.children.size() > 1) return false;
            node = node.children.values().iterator().next();
        }
        return true;
    }

    /** Trả về biểu diễn văn bản nhiều dòng của cấu trúc cây. */
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

    /** Trả về bảng header dưới dạng chuỗi đã định dạng. */
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
