import java.io.*;
import java.util.*;

/**
 * Fixes horse.csv:
 * 1. Remove lesion_type1/2/3 columns (noise - codes like "11300")
 * 2. Impute missing "?" values with mode (most frequent) per column
 */
public class FixHorse {
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

        // Find columns to remove: lesion_type1, lesion_type2, lesion_type3
        Set<Integer> removeIdx = new HashSet<>();
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim();
            if (h.equals("lesion_type1") || h.equals("lesion_type2") || h.equals("lesion_type3")) {
                removeIdx.add(i);
            }
        }

        // Build keep indices
        List<Integer> keepIdx = new ArrayList<>();
        for (int i = 0; i < header.length; i++) {
            if (!removeIdx.contains(i)) keepIdx.add(i);
        }

        // Project to kept columns
        String[] newHeader = new String[keepIdx.size()];
        for (int i = 0; i < keepIdx.size(); i++) newHeader[i] = header[keepIdx.get(i)].trim();

        List<String[]> newRows = new ArrayList<>();
        for (String[] row : rows) {
            String[] nr = new String[keepIdx.size()];
            for (int i = 0; i < keepIdx.size(); i++) {
                int idx = keepIdx.get(i);
                nr[i] = idx < row.length ? row[idx].trim() : "?";
            }
            newRows.add(nr);
        }

        // Impute missing values with mode per column (skip last = class)
        for (int col = 0; col < newHeader.length - 1; col++) {
            Map<String, Integer> freq = new HashMap<>();
            for (String[] r : newRows) {
                if (!r[col].equals("?") && !r[col].isEmpty()) {
                    freq.merge(r[col], 1, Integer::sum);
                }
            }
            if (freq.isEmpty()) continue;
            String mode = freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get().getKey();
            for (String[] r : newRows) {
                if (r[col].equals("?") || r[col].isEmpty()) r[col] = mode;
            }
        }

        // Remove rows with "?" class
        newRows.removeIf(r -> r[r.length - 1].equals("?") || r[r.length - 1].isEmpty());

        // Write back
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(String.join(",", newHeader));
            bw.newLine();
            for (String[] r : newRows) {
                bw.write(String.join(",", r));
                bw.newLine();
            }
        }

        System.out.println("Fixed horse.csv: " + newRows.size() + " rows, " + newHeader.length + " cols");
        System.out.println("Removed columns: " + removeIdx);
        System.out.println("Header: " + Arrays.toString(newHeader));
    }
}
