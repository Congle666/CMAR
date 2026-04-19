import java.io.*;
import java.util.*;

/**
 * Fixes horse.csv v2:
 * 1. Re-download and re-process WITHOUT imputation (keep "?" as missing)
 * 2. Remove lesion_type1/2/3 and columns with >50% missing values
 * 3. Remove rows with "?" class
 */
public class FixHorse2 {
    public static void main(String[] args) throws Exception {
        String file = "data/horse.csv";
        List<String[]> rows = new ArrayList<>();
        String[] header;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            header = br.readLine().split(",", -1);
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty())
                    rows.add(line.split(",", -1));
            }
        }

        // Count missing values per column
        int[] missingCount = new int[header.length];
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, header.length); i++) {
                if (row[i].trim().equals("?") || row[i].trim().isEmpty()) {
                    missingCount[i]++;
                }
            }
        }

        // Remove columns with >40% missing or lesion_type
        Set<Integer> removeIdx = new HashSet<>();
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim();
            double missPct = (double) missingCount[i] / rows.size();
            if (h.startsWith("lesion_type") || missPct > 0.40) {
                removeIdx.add(i);
                System.out.printf("  Removing col %d: %-30s (%.1f%% missing)%n",
                    i, h, missPct * 100);
            }
        }

        // Build keep indices
        List<Integer> keepIdx = new ArrayList<>();
        for (int i = 0; i < header.length; i++) {
            if (!removeIdx.contains(i)) keepIdx.add(i);
        }

        String[] newHeader = new String[keepIdx.size()];
        for (int i = 0; i < keepIdx.size(); i++)
            newHeader[i] = header[keepIdx.get(i)].trim();

        List<String[]> newRows = new ArrayList<>();
        for (String[] row : rows) {
            String[] nr = new String[keepIdx.size()];
            for (int i = 0; i < keepIdx.size(); i++) {
                int idx = keepIdx.get(i);
                nr[i] = idx < row.length ? row[idx].trim() : "?";
            }
            // Keep "?" as is — DatasetLoader will skip them
            newRows.add(nr);
        }

        // Remove rows with "?" class
        newRows.removeIf(r -> r[r.length - 1].equals("?") || r[r.length - 1].isEmpty());

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(String.join(",", newHeader));
            bw.newLine();
            for (String[] r : newRows) {
                bw.write(String.join(",", r));
                bw.newLine();
            }
        }

        System.out.println("Fixed horse.csv: " + newRows.size() + " rows, " + newHeader.length + " cols");
    }
}
