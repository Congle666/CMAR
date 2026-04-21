import java.util.List;

/**
 * Runs 10-fold CV on a single dataset.
 * Usage: java BenchmarkOne <file> <name> <minSupPct> <paperCMAR> <paperCBA> <paperC45>
 */
public class BenchmarkOne {
    public static void main(String[] args) throws Exception {
        String file = args[0];
        String name = args[1];
        double supPct = Double.parseDouble(args[2]);
        double paperCMAR = Double.parseDouble(args[3]);
        double paperCBA  = Double.parseDouble(args[4]);
        double paperC45  = Double.parseDouble(args[5]);

        List<Transaction> data = DatasetLoader.load(file);
        if (data.isEmpty()) {
            System.out.println("SKIP " + name);
            return;
        }

        int maxPatLen = args.length >= 7 ? Integer.parseInt(args[6]) : Integer.MAX_VALUE;

        double[] accs = CrossValidator.run(data, 10,
            supPct, 0.5, 3.841, 4, 42, maxPatLen);

        double mean = CrossValidator.mean(accs) * 100;
        double std  = CrossValidator.stddev(accs) * 100;
        double diff = mean - paperCMAR;

        // Output single result line
        System.out.printf("RESULT|%-14s|%5d|%5.1f%%|%6.2f|%5.2f|%6.2f|%6.2f|%6.2f|%+.2f%n",
            name, data.size(), supPct * 100, mean, std, paperCMAR, paperCBA, paperC45, diff);
    }
}
