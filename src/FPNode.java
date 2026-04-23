import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Một nút (node) trong CR-tree (FP-tree mở rộng dành cho CMAR).
 *
 * So với nút FP-tree chuẩn, nút này còn lưu thêm phân phối số đếm
 * theo lớp (classCount) — tức số transaction đi qua nút này được
 * tách nhỏ theo từng nhãn lớp. Đây chính là mở rộng then chốt trong
 * Li, Han & Pei (2001) Section 3.2, cho phép CMAR sinh trực tiếp
 * các Class Association Rule trong lúc đang mining FP-Growth, không
 * cần lượt duyệt thứ hai.
 */
public class FPNode {

    String item;
    int count;                         // tổng số transaction đi qua nút này
    FPNode parent;
    Map<String, FPNode> children;
    FPNode nodeLink;                   // liên kết header-table cho item này

    /** Phân phối nhãn lớp trong các transaction đi qua nút này. */
    Map<String, Integer> classCount;

    public FPNode(String item, int count, FPNode parent) {
        this.item       = item;
        this.count      = count;
        this.parent     = parent;
        this.children   = new LinkedHashMap<>();
        this.nodeLink   = null;
        this.classCount = new HashMap<>();
    }

    /** Nút gốc có item = null. */
    public boolean isRoot() {
        return item == null;
    }

    @Override
    public String toString() {
        return "[" + item + ":" + count + " " + classCount + "]";
    }
}
