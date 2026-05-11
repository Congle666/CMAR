import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Báo cáo chi tiết về phân phối lớp và sinh luật (Hướng 2 — Class MinSup).
 * 
 * Dùng để verify rằng:
 *   - Mỗi lớp có lượng luật hợp lý
 *   - Lớp thiểu số không bị loại hoàn toàn
 *   - Luật được sinh từ pattern nào
 */
public class ClassDistributionReport {

    public String datasetName;
    public int trainSize;
    public Map<String, Integer> classFreq;       // lớp → số transaction
    public Map<String, Integer> classMinSupMap;  // lớp → ngưỡng minSup
    public int totalPatterns;
    public Map<String, Integer> patternCountByClass;  // lớp → số pattern
    public int totalRules;
    public Map<String, Integer> ruleCountByClass;     // lớp → số CAR sinh
    public Map<String, Double>  avgConfByClass;       // lớp → confidence trung bình của luật

    public ClassDistributionReport() {
        this.classFreq = new java.util.LinkedHashMap<>();
        this.classMinSupMap = new java.util.LinkedHashMap<>();
        this.patternCountByClass = new java.util.LinkedHashMap<>();
        this.ruleCountByClass = new java.util.LinkedHashMap<>();
        this.avgConfByClass = new java.util.LinkedHashMap<>();
    }

    /**
     * Sinh báo cáo từ danh sách luật và tần suất lớp.
     */
    public static ClassDistributionReport generate(
            String datasetName,
            int trainSize,
            Map<String, Integer> classFreq,
            Map<String, Integer> classMinSupMap,
            List<FrequentPattern> patterns,
            List<AssociationRule> rules) {

        ClassDistributionReport rpt = new ClassDistributionReport();
        rpt.datasetName = datasetName;
        rpt.trainSize = trainSize;
        rpt.classFreq.putAll(classFreq);
        if (classMinSupMap != null) {
            rpt.classMinSupMap.putAll(classMinSupMap);
        }
        rpt.totalPatterns = patterns.size();
        rpt.totalRules = rules.size();

        // Đếm pattern theo lớp (giải nén từ pattern condset + class label)
        // Pattern chỉ chứa itemset, nhưng rule chứa class → lấy từ rules
        for (String cls : classFreq.keySet()) {
            rpt.patternCountByClass.put(cls, 0);
            rpt.ruleCountByClass.put(cls, 0);
            rpt.avgConfByClass.put(cls, 0.0);
        }

        // Đếm luật theo lớp
        Map<String, List<AssociationRule>> rulesByClass = rules.stream()
            .collect(Collectors.groupingBy(AssociationRule::getClassLabel));

        for (String cls : classFreq.keySet()) {
            List<AssociationRule> classRules = rulesByClass.getOrDefault(cls, List.of());
            rpt.ruleCountByClass.put(cls, classRules.size());
            if (!classRules.isEmpty()) {
                double avgConf = classRules.stream()
                    .mapToDouble(AssociationRule::getConfidence)
                    .average()
                    .orElse(0.0);
                rpt.avgConfByClass.put(cls, avgConf);
            }
        }

        return rpt;
    }

    /**
     * In báo cáo ra stdout (tiếng Việt).
     */
    public void print() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BÁNG CÁO: Phân Phối Lớp & Sinh Luật (Hướng 2 — Class MinSup)");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("Dataset: " + datasetName);
        System.out.println("Kích thước train: " + trainSize);
        System.out.println("Tổng pattern: " + totalPatterns);
        System.out.println("Tổng luật (CAR) (rule): " + totalRules);

        System.out.println();
        System.out.println("Phân phối lớp:");
        System.out.printf("%-15s %8s %8s | %8s %8s %8s%n",
            "Lớp", "Freq", "%Abs", "MinSup", "Pattern", "Rule");
        System.out.println("-".repeat(80));

        for (String cls : classFreq.keySet()) {
            int freq = classFreq.get(cls);
            double pctAbs = 100.0 * freq / trainSize;
            int minSup = classMinSupMap.getOrDefault(cls, 0);
            int patCnt = ruleCountByClass.getOrDefault(cls, 0);
            double avgConf = avgConfByClass.getOrDefault(cls, 0.0);

            System.out.printf("%-15s %8d %7.1f%% | %8d %8d %8.4f%n",
                cls, freq, pctAbs, minSup, patCnt, avgConf);
        }

        System.out.println();
        System.out.println("Nhận xét:");
        int minorityCount = (int) classFreq.values().stream()
            .filter(f -> f < trainSize * 0.2)
            .count();
        if (minorityCount > 0) {
            System.out.println("  ✓ Phát hiện " + minorityCount + " lớp thiểu số");
            for (String cls : classFreq.keySet()) {
                if (classFreq.get(cls) < trainSize * 0.2) {
                    int rulesCnt = ruleCountByClass.getOrDefault(cls, 0);
                    if (rulesCnt > 0) {
                        System.out.println("    → Lớp \"" + cls + "\": "
                            + rulesCnt + " luật (hợp lý)");
                    } else {
                        System.out.println("    ⚠ Lớp \"" + cls + "\": 0 luật (cảnh báo!)");
                    }
                }
            }
        }

        System.out.println();
    }

    /**
     * Xuất báo cáo dưới dạng CSV (cho append vào result/class_dist_report.csv).
     * Format: dataset, train_size, class, freq, minsup, rules_cnt, avg_conf
     */
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        for (String cls : classFreq.keySet()) {
            sb.append(datasetName).append(",");
            sb.append(trainSize).append(",");
            sb.append(cls).append(",");
            sb.append(classFreq.get(cls)).append(",");
            sb.append(classMinSupMap.getOrDefault(cls, 0)).append(",");
            sb.append(ruleCountByClass.get(cls)).append(",");
            sb.append(String.format("%.4f", avgConfByClass.get(cls))).append("\n");
        }
        return sb.toString();
    }
}
