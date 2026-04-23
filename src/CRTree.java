import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CR-tree (cây lưu Class Association Rule) dùng để lưu luật nén gọn —
 * Li, Han & Pei (2001) §3.3.
 *
 * Condset của mỗi luật được chèn như một path từ gốc, với các item được
 * sắp theo tần suất toàn cục giảm dần để các luật dùng chung item có
 * thể chia sẻ nút cây. Một nút lưu các luật có condset kết thúc tại đó
 * (luật lá, hoặc luật trung gian nếu có luật dài hơn nối tiếp path).
 *
 * Truy vấn cho một bản ghi test được thực hiện bằng DFS, cắt tỉa bất kỳ
 * subtree nào có item không xuất hiện trong bản ghi — tiết kiệm so với
 * duyệt tuyến tính toàn bộ danh sách luật.
 */
public class CRTree {

    private static class Node {
        final String item;
        final Map<String, Node> children = new HashMap<>();
        final List<AssociationRule> rules = new ArrayList<>();
        Node(String item) { this.item = item; }
    }

    private final Node root = new Node(null);
    private final Map<String, Integer> itemOrder;  // item -> thứ hạng (thấp = tần suất cao hơn)
    private int size = 0;

    /**
     * @param itemFreq tần suất toàn cục của item — dùng để sắp thứ tự item
     *                 dọc theo mỗi path (tần suất giảm dần → chia sẻ prefix tốt hơn)
     */
    public CRTree(Map<String, Integer> itemFreq) {
        this.itemOrder = new HashMap<>();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(itemFreq.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        for (int i = 0; i < entries.size(); i++) {
            itemOrder.put(entries.get(i).getKey(), i);
        }
    }

    /** Chèn một luật; condset được sắp theo tần suất item giảm dần trước. */
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

    /** Chèn tất cả các luật. */
    public void insertAll(Collection<AssociationRule> rules) {
        for (AssociationRule r : rules) insert(r);
    }

    /** Số lượng luật đang lưu. */
    public int size() { return size; }

    /**
     * Trả về mọi luật có condset là tập con các item của bản ghi test.
     *
     * DFS có cắt tỉa: tại mỗi nút con, nếu item của nó không có trong
     * bản ghi, toàn bộ subtree được bỏ qua — mọi luật đi qua nút đó đều
     * cần item này, nên không thể match.
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
