import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates Class Association Rules (CARs) from a set of frequent patterns.
 *
 * A CAR is derived from any frequent pattern that contains exactly one
 * class item (an item whose key starts with "class=").
 *
 *   condset    = pattern items  \  {class item}
 *   classLabel = value extracted from the class item
 *   confidence = support(condset ∪ class) / support(condset)
 *
 * Only rules with confidence >= minConfidence and non-empty condset are kept.
 */
public class RuleGenerator {

    /**
     * Generates and returns sorted CARs (by rule precedence).
     *
     * @param patterns          all frequent patterns from FP-Growth
     * @param totalTransactions number of training transactions (for relative support)
     * @param minConfidence     minimum confidence threshold
     * @return sorted list of AssociationRules
     */
    public static List<AssociationRule> generate(
            List<FrequentPattern> patterns,
            int totalTransactions,
            double minConfidence) {

        // Build a fast-lookup map: itemset -> support count
        // Keys are HashSet<String> so equality is content-based.
        Map<Set<String>, Integer> supportMap = new HashMap<>();
        for (FrequentPattern fp : patterns) {
            // Store as a new HashSet so equals/hashCode works reliably in the map
            supportMap.put(new HashSet<>(fp.getItems()), fp.getSupport());
        }

        List<AssociationRule> rules = new ArrayList<>();

        for (FrequentPattern fp : patterns) {
            // Identify class items in this pattern
            List<String> classItemsInPattern = fp.getItems().stream()
                .filter(item -> item.startsWith("class="))
                .collect(Collectors.toList());

            // A valid CAR must have exactly one class item
            if (classItemsInPattern.size() != 1) continue;

            String classItem = classItemsInPattern.get(0);
            String classLabel = classItem.substring("class=".length());

            // condset = all items except the class item
            Set<String> condset = new HashSet<>(fp.getItems());
            condset.remove(classItem);

            // Skip rules with empty condset (no condition)
            if (condset.isEmpty()) continue;

            // Look up support of the condset alone
            Integer condsetSupport = supportMap.get(condset);
            if (condsetSupport == null) continue; // condset was not frequent

            double support    = (double) fp.getSupport() / totalTransactions;
            double confidence = (double) fp.getSupport() / condsetSupport;

            if (confidence >= minConfidence) {
                rules.add(new AssociationRule(
                    condset, classLabel,
                    support, confidence,
                    fp.getSupport(), condsetSupport
                ));
            }
        }

        Collections.sort(rules);
        return rules;
    }
}
