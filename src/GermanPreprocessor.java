import java.io.*;
import java.util.*;

/**
 * Preprocesses the UCI German Credit dataset:
 *   - Reads space-separated format (no header, 20 attrs + 1 class)
 *   - Adds meaningful column names
 *   - Discretizes numerical attributes into bins
 *   - Outputs as CSV (comma-separated with header)
 *
 * German Credit Dataset (Statlog):
 *   1000 records, 20 attributes (7 numerical, 13 categorical), 2 classes
 *   Class: 1 = Good credit, 2 = Bad credit
 *
 * Numerical attributes and their discretization:
 *   Col 1:  Duration (months)       → Short/Medium/Long
 *   Col 4:  Credit amount           → Low/Medium/High
 *   Col 7:  Installment rate (1-4)  → Low/High
 *   Col 10: Residence since (1-4)   → Short/Long
 *   Col 12: Age                     → Young/Middle/Senior
 *   Col 15: Num credits (1-4)       → Few/Many
 *   Col 17: Num liable (1-2)        → One/Two
 */
public class GermanPreprocessor {

    static final String[] HEADERS = {
        "checking",      // 0  - Status of existing checking account
        "duration",      // 1  - Duration in months (numerical)
        "credit_hist",   // 2  - Credit history
        "purpose",       // 3  - Purpose
        "amount",        // 4  - Credit amount (numerical)
        "savings",       // 5  - Savings account/bonds
        "employment",    // 6  - Present employment since
        "install_rate",  // 7  - Installment rate % of disposable income (numerical 1-4)
        "personal",      // 8  - Personal status and sex
        "other_debtors", // 9  - Other debtors/guarantors
        "residence",     // 10 - Present residence since (numerical 1-4)
        "property",      // 11 - Property
        "age",           // 12 - Age in years (numerical)
        "other_install", // 13 - Other installment plans
        "housing",       // 14 - Housing
        "num_credits",   // 15 - Number of existing credits (numerical 1-4)
        "job",           // 16 - Job
        "num_liable",    // 17 - Number of people being liable (numerical 1-2)
        "telephone",     // 18 - Telephone
        "foreign",       // 19 - Foreign worker
        "class"          // 20 - Class (1=good, 2=bad)
    };

    // Indices of numerical columns
    static final int COL_DURATION    = 1;
    static final int COL_AMOUNT      = 4;
    static final int COL_INSTALL     = 7;
    static final int COL_RESIDENCE   = 10;
    static final int COL_AGE         = 12;
    static final int COL_NUMCREDITS  = 15;
    static final int COL_NUMLIABLE   = 17;
    static final int COL_CLASS       = 20;

    public static void main(String[] args) throws IOException {
        String input  = args.length >= 1 ? args[0] : "data/german.csv";
        String output = args.length >= 2 ? args[1] : "data/german_disc.csv";

        List<String[]> rows = new ArrayList<>();

        // Read all rows
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length == 21) {
                    rows.add(parts);
                }
            }
        }

        System.out.println("Read " + rows.size() + " records from " + input);

        // Collect numerical values for statistics
        double[] durations = rows.stream().mapToDouble(r -> Double.parseDouble(r[COL_DURATION])).toArray();
        double[] amounts   = rows.stream().mapToDouble(r -> Double.parseDouble(r[COL_AMOUNT])).toArray();
        double[] ages      = rows.stream().mapToDouble(r -> Double.parseDouble(r[COL_AGE])).toArray();

        Arrays.sort(durations);
        Arrays.sort(amounts);
        Arrays.sort(ages);

        // Use tercile boundaries for 3-bin discretization
        double durQ1 = percentile(durations, 33.3), durQ2 = percentile(durations, 66.7);
        double amtQ1 = percentile(amounts, 33.3),   amtQ2 = percentile(amounts, 66.7);
        double ageQ1 = percentile(ages, 33.3),       ageQ2 = percentile(ages, 66.7);

        System.out.printf("Duration bins: <=%.0f (Short), <=%.0f (Medium), >%.0f (Long)%n", durQ1, durQ2, durQ2);
        System.out.printf("Amount bins:   <=%.0f (Low), <=%.0f (Medium), >%.0f (High)%n", amtQ1, amtQ2, amtQ2);
        System.out.printf("Age bins:      <=%.0f (Young), <=%.0f (Middle), >%.0f (Senior)%n", ageQ1, ageQ2, ageQ2);

        // Write CSV
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(output))) {
            // Header
            bw.write(String.join(",", HEADERS));
            bw.newLine();

            for (String[] row : rows) {
                String[] out = new String[21];

                for (int i = 0; i < 21; i++) {
                    out[i] = row[i]; // default: keep as-is
                }

                // Discretize numerical columns
                double dur = Double.parseDouble(row[COL_DURATION]);
                out[COL_DURATION] = dur <= durQ1 ? "Short" : dur <= durQ2 ? "Medium" : "Long";

                double amt = Double.parseDouble(row[COL_AMOUNT]);
                out[COL_AMOUNT] = amt <= amtQ1 ? "Low" : amt <= amtQ2 ? "Medium" : "High";

                int inst = Integer.parseInt(row[COL_INSTALL]);
                out[COL_INSTALL] = inst <= 2 ? "Low" : "High";

                int res = Integer.parseInt(row[COL_RESIDENCE]);
                out[COL_RESIDENCE] = res <= 2 ? "Short" : "Long";

                double age = Double.parseDouble(row[COL_AGE]);
                out[COL_AGE] = age <= ageQ1 ? "Young" : age <= ageQ2 ? "Middle" : "Senior";

                int ncred = Integer.parseInt(row[COL_NUMCREDITS]);
                out[COL_NUMCREDITS] = ncred <= 1 ? "Few" : "Many";

                int nliable = Integer.parseInt(row[COL_NUMLIABLE]);
                out[COL_NUMLIABLE] = nliable <= 1 ? "One" : "Two";

                // Class label: 1 -> good, 2 -> bad
                out[COL_CLASS] = row[COL_CLASS].equals("1") ? "good" : "bad";

                bw.write(String.join(",", out));
                bw.newLine();
            }
        }

        System.out.println("Wrote " + rows.size() + " records to " + output);

        // Print class distribution
        long good = rows.stream().filter(r -> r[COL_CLASS].equals("1")).count();
        long bad  = rows.stream().filter(r -> r[COL_CLASS].equals("2")).count();
        System.out.println("Class distribution: good=" + good + ", bad=" + bad);
    }

    private static double percentile(double[] sorted, double p) {
        double index = (p / 100.0) * (sorted.length - 1);
        int lo = (int) Math.floor(index);
        int hi = (int) Math.ceil(index);
        if (lo == hi) return sorted[lo];
        return sorted[lo] + (index - lo) * (sorted[hi] - sorted[lo]);
    }
}
