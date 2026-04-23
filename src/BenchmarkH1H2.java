import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Benchmark — Kết hợp Hướng 1 + Hướng 2 (v4).
 *
 * Dùng ĐỒNG THỜI:
 *   - {@link CMARClassifierWeighted} (H1: score có trọng số nghịch tần suất)
 *   - minSup theo từng lớp qua {@link CrossValidator#runWithMetrics} (H2)
 *
 * Giả thuyết: H2 sinh luật cho lớp thiểu số; H1 cho các luật đó một score
 * công bằng; hiệu ứng cộng hợp sẽ vượt trội so với từng hướng đơn lẻ.
 *
 * File kết quả:
 *   result/v4_metrics.csv
 *   result/v4_per_class.csv
 */
public class BenchmarkH1H2 {

    static final String[][] DATASETS = Benchmark.DATASETS;

    static final String OUT_METRICS_CSV   = "result/v4_metrics.csv";
    static final String OUT_PER_CLASS_CSV = "result/v4_per_class.csv";

    public static void main(String[] args) throws Exception {
        int K = 10;
        double minConfidence = 0.5;
        double chiSqThreshold = 3.841;
        int coverageDelta = 4;
        long seed = 42;

        Set<String> filter = args.length > 0 ? new HashSet<>(Arrays.asList(args)) : null;

        System.out.println("=================================================================");
        System.out.println("  CMAR Benchmark — Ket hop Huong 1 + Huong 2 (v4)");
        System.out.println("  Classify co trong so + minSup theo tung lop");
        if (filter != null) System.out.println("  Filter: " + filter);
        System.out.println("=================================================================\n");

        System.out.printf("%-14s %5s %6s | %6s %7s %7s %5s | %6s %6s %6s  Diff%n",
            "Dataset", "N", "supPct", "Acc", "MacroF1", "WF1", "Std", "Paper", "CBA", "C4.5");
        System.out.println("-".repeat(90));

        Map<String, EvalMetrics> aggregated = new LinkedHashMap<>();
        Map<String, String> info = new LinkedHashMap<>();

        double totalOursAcc = 0, totalOursF1 = 0, totalPaper = 0;
        double totalDiff = 0;
        int count = 0;
        int within5 = 0;

        for (String[] ds : DATASETS) {
            String file = ds[0];
            String name = ds[1];
            if (filter != null && !filter.contains(name)) continue;
            double supPct = Double.parseDouble(ds[2]);
            double paperCMAR = Double.parseDouble(ds[3]);
            double paperCBA  = Double.parseDouble(ds[4]);
            double paperC45  = Double.parseDouble(ds[5]);
            int maxPatLen    = ds.length > 6 ? Integer.parseInt(ds[6]) : Integer.MAX_VALUE;

            System.out.println("\n>>> " + name + " (" + file + ") ...");

            try {
                List<Transaction> data = DatasetLoader.load(file);
                if (data.isEmpty()) {
                    System.out.println("    BO QUA (dataset rong)");
                    continue;
                }

                // Kết hợp H1 + H2
                List<EvalMetrics> foldMetrics = CrossValidator.runWithMetrics(
                    data, K, supPct, minConfidence, chiSqThreshold, coverageDelta,
                    seed, maxPatLen,
                    CMARClassifierWeighted::new,  // H1
                    supPct);                       // H2

                EvalMetrics agg = EvalMetrics.average(foldMetrics);
                aggregated.put(name, agg);
                info.put(name, data.size() + "|" + agg.perClass.size() + "|" + supPct);

                double accPct = agg.accuracy * 100;
                double stdPct = agg.accuracyStd * 100;
                double diff   = accPct - paperCMAR;

                System.out.printf("  => %-14s %5d %5.1f%% | %6.2f %7.4f %7.4f %5.2f | %6.2f %6.2f %6.2f  %+5.2f%n",
                    name, data.size(), supPct * 100,
                    accPct, agg.macroF1, agg.weightedF1, stdPct,
                    paperCMAR, paperCBA, paperC45, diff);

                totalOursAcc += accPct;
                totalOursF1  += agg.macroF1;
                totalPaper   += paperCMAR;
                totalDiff    += Math.abs(diff);
                count++;
                if (Math.abs(diff) <= 5.0) within5++;

            } catch (OutOfMemoryError e) {
                System.out.println("    HET BO NHO");
            } catch (Exception e) {
                System.out.println("    LOI: " + e.getMessage());
                e.printStackTrace(System.out);
            }
        }

        ResultWriter.writeMetricsCsv(aggregated, info, OUT_METRICS_CSV);
        ResultWriter.writePerClassCsv(aggregated, OUT_PER_CLASS_CSV);

        System.out.println("\n" + "=".repeat(90));
        System.out.println("TONG KET — Ket hop H1+H2 (v4)");
        System.out.println("=".repeat(90));
        if (count > 0) {
            System.out.printf("So dataset danh gia:           %d / %d%n", count, DATASETS.length);
            System.out.printf("Accuracy trung binh (v4):      %.2f%%%n", totalOursAcc / count);
            System.out.printf("Macro-F1 trung binh (v4):      %.4f%n",   totalOursF1 / count);
            System.out.printf("Accuracy trung binh (paper):   %.2f%%%n", totalPaper / count);
            System.out.printf("Chenh lech tuyet doi vs paper: %.2f%%%n", totalDiff / count);
            System.out.printf("Trong khoang 5%% cua paper:    %d / %d%n", within5, count);
        }
        System.out.println();
        System.out.println("File CSV xuat ra:");
        System.out.println("  " + OUT_METRICS_CSV);
        System.out.println("  " + OUT_PER_CLASS_CSV);
    }
}
