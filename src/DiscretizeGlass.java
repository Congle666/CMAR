import java.io.*;
import java.util.*;

/**
 * Discretize the UCI Glass dataset (glass.data) using 5 equal-frequency
 * (quintile) bins. Features with very few unique values get fewer bins.
 * Output: glass.csv with header row.
 */
public class DiscretizeGlass {

    static final String[] FEATURE_NAMES = {"RI","Na","Mg","Al","Si","K","Ca","Ba","Fe"};
    static final String[] LABELS_5 = {"VL","L","M","H","VH"};
    static final String[] LABELS_4 = {"VL","L","H","VH"};
    static final String[] LABELS_3 = {"L","M","H"};
    static final String[] LABELS_2 = {"L","H"};

    static final Map<Integer,String> CLASS_MAP = new LinkedHashMap<>();
    static {
        CLASS_MAP.put(1, "building_float");
        CLASS_MAP.put(2, "building_nonfloat");
        CLASS_MAP.put(3, "vehicle_float");
        CLASS_MAP.put(5, "vehicle_nonfloat");
        CLASS_MAP.put(6, "containers");
        CLASS_MAP.put(7, "headlamps");
    }

    public static void main(String[] args) throws Exception {
        String inputPath = "data/glass.data";
        String outputPath = "data/glass.csv";

        // Read raw data
        List<double[]> features = new ArrayList<>();
        List<Integer> classes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                // Skip col 0 (ID), cols 1-9 features, col 10 class
                double[] feat = new double[9];
                for (int i = 0; i < 9; i++) {
                    feat[i] = Double.parseDouble(parts[i + 1]);
                }
                features.add(feat);
                classes.add(Integer.parseInt(parts[10]));
            }
        }

        int n = features.size();
        System.out.println("Loaded " + n + " records");

        // Discretize each feature
        String[][] discretized = new String[n][9];

        for (int fi = 0; fi < 9; fi++) {
            double[] vals = new double[n];
            for (int i = 0; i < n; i++) vals[i] = features.get(i)[fi];

            // Count zeros and unique non-zero values
            int zeroCount = 0;
            TreeSet<Double> uniqueNonZero = new TreeSet<>();
            for (double v : vals) {
                if (v == 0.0) zeroCount++;
                else uniqueNonZero.add(v);
            }
            TreeSet<Double> uniqueAll = new TreeSet<>(uniqueNonZero);
            if (zeroCount > 0) uniqueAll.add(0.0);

            // If >50% zeros, use "zero" bin + bin non-zero values separately
            boolean zeroHeavy = (zeroCount > n / 2) && uniqueNonZero.size() > 1;

            if (zeroHeavy) {
                // Collect non-zero values and their indices
                List<double[]> nonZero = new ArrayList<>(); // [index, value]
                for (int i = 0; i < n; i++) {
                    if (vals[i] > 0.0) nonZero.add(new double[]{i, vals[i]});
                }
                nonZero.sort((a, b) -> Double.compare(a[1], b[1]));
                int nz = nonZero.size();
                int nzUnique = uniqueNonZero.size();

                // Decide bins for non-zero portion
                int nBinsNZ;
                if (nzUnique <= 3) nBinsNZ = Math.min(2, nzUnique);
                else if (nzUnique <= 8) nBinsNZ = 3;
                else nBinsNZ = 4;

                String[] nzLabels;
                if (nBinsNZ == 4) nzLabels = new String[]{"L","M","H","VH"};
                else if (nBinsNZ == 3) nzLabels = new String[]{"L","H","VH"};
                else if (nBinsNZ == 2) nzLabels = new String[]{"L","H"};
                else nzLabels = new String[]{"H"};

                // Quantile boundaries on non-zero values
                double[] nzSorted = nonZero.stream().mapToDouble(x -> x[1]).toArray();
                double[] boundaries = new double[nBinsNZ - 1];
                for (int b = 1; b < nBinsNZ; b++) {
                    double idx = (double) b * nz / nBinsNZ;
                    int lo = Math.max((int) Math.floor(idx) - 1, 0);
                    int hi = Math.min((int) Math.ceil(idx), nz - 1);
                    boundaries[b - 1] = (nzSorted[lo] + nzSorted[hi]) / 2.0;
                }

                System.out.println(FEATURE_NAMES[fi] + ": " + uniqueAll.size() +
                    " unique, ZERO-HEAVY (" + zeroCount + " zeros), " +
                    nBinsNZ + " non-zero bins + zero bin");

                // Assign: zeros get "zero", non-zeros get binned
                for (int i = 0; i < n; i++) {
                    if (vals[i] == 0.0) {
                        discretized[i][fi] = "zero";
                    } else {
                        double v = vals[i];
                        String assigned = nzLabels[nzLabels.length - 1];
                        for (int bi = 0; bi < boundaries.length; bi++) {
                            if (v <= boundaries[bi]) {
                                assigned = nzLabels[bi];
                                break;
                            }
                        }
                        discretized[i][fi] = assigned;
                    }
                }
            } else {
                // Standard equal-frequency binning on all values
                int nUnique = uniqueAll.size();
                int nBins;
                if (nUnique <= 3) nBins = nUnique;
                else if (nUnique <= 6) nBins = Math.min(3, nUnique);
                else if (nUnique <= 10) nBins = 4;
                else nBins = 5;

                String[] labels;
                if (nBins >= 5) labels = LABELS_5;
                else if (nBins == 4) labels = LABELS_4;
                else if (nBins == 3) labels = LABELS_3;
                else labels = LABELS_2;

                double[] sorted = vals.clone();
                Arrays.sort(sorted);

                double[] boundaries = new double[nBins - 1];
                for (int b = 1; b < nBins; b++) {
                    double idx = (double) b * n / nBins;
                    int lo = Math.max((int) Math.floor(idx) - 1, 0);
                    int hi = Math.min((int) Math.ceil(idx), n - 1);
                    boundaries[b - 1] = (sorted[lo] + sorted[hi]) / 2.0;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(FEATURE_NAMES[fi]).append(": ").append(nUnique)
                  .append(" unique, ").append(nBins).append(" bins, boundaries=[");
                for (int b = 0; b < boundaries.length; b++) {
                    if (b > 0) sb.append(", ");
                    sb.append(String.format("%.4f", boundaries[b]));
                }
                sb.append("]");
                System.out.println(sb);

                for (int i = 0; i < n; i++) {
                    double v = vals[i];
                    String assigned = labels[labels.length - 1];
                    for (int bi = 0; bi < boundaries.length; bi++) {
                        if (v <= boundaries[bi]) {
                            assigned = labels[bi];
                            break;
                        }
                    }
                    discretized[i][fi] = assigned;
                }
            }
        }

        // Write CSV
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
            pw.println(String.join(",", FEATURE_NAMES) + ",class");
            for (int i = 0; i < n; i++) {
                StringBuilder sb = new StringBuilder();
                for (int fi = 0; fi < 9; fi++) {
                    if (fi > 0) sb.append(",");
                    sb.append(discretized[i][fi]);
                }
                sb.append(",").append(CLASS_MAP.get(classes.get(i)));
                pw.println(sb);
            }
        }

        System.out.println("Wrote " + outputPath + " with " + n + " records + header");

        // Verify line count
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(outputPath))) {
            while (br.readLine() != null) lineCount++;
        }
        System.out.println("Verification: " + lineCount + " lines (" + (lineCount - 1) + " data records)");

        // Class distribution
        Map<String, Integer> classDist = new LinkedHashMap<>();
        for (int c : classes) {
            String label = CLASS_MAP.get(c);
            classDist.merge(label, 1, Integer::sum);
        }
        System.out.println("\nClass distribution:");
        classDist.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

        // Bin distributions
        System.out.println("\nBin distributions:");
        for (int fi = 0; fi < 9; fi++) {
            Map<String, Integer> dist = new TreeMap<>();
            for (int i = 0; i < n; i++) {
                dist.merge(discretized[i][fi], 1, Integer::sum);
            }
            System.out.println("  " + FEATURE_NAMES[fi] + ": " + dist);
        }
    }
}
