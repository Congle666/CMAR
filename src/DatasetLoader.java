import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Nạp dataset từ CSV thành danh sách Transaction.
 *
 * Định dạng:
 *   - Dòng đầu = header (tên cột)
 *   - Cột cuối = nhãn lớp
 *   - Các cột khác trở thành item: "tên_cột=giá_trị"
 *   - Giá trị thiếu đánh dấu "?" sẽ bị bỏ qua
 */
public class DatasetLoader {

    public static List<Transaction> load(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) return transactions;

            String[] headers = headerLine.split(",", -1);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] values = line.split(",", -1);
                List<String> items = new ArrayList<>();

                for (int i = 0; i < values.length - 1; i++) {
                    String val = values[i].trim();
                    if (!val.isEmpty() && !val.equals("?")) {
                        String col = (i < headers.length) ? headers[i] : "col" + i;
                        items.add(col + "=" + val);
                    }
                }

                String classLabel = values[values.length - 1].trim();
                if (!classLabel.isEmpty()) {
                    transactions.add(new Transaction(items, classLabel));
                }
            }
        }

        return transactions;
    }
}
