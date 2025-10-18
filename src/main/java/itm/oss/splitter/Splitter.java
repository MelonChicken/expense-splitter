package itm.oss.splitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Computes per-person net balances from a list of expenses.
 *
 * Convention (net):
 *   - Positive  : others owe this person (creditor)
 *   - Negative  : this person owes others (debtor)
 *
 * Rule (equal split):
 *   For each expense of amount A with N participants:
 *     * Each participant owes A/N.
 *     * The payer has already advanced A.
 *   Therefore:
 *     - Subtract A/N from every participant's net.
 *     - Add +A to the payer's net.
 *
 * Notes on cents rounding:
 *   Accumulate with high precision (scale 10) then round all nets to
 *   2 decimals (HALF_EVEN). If rounding drift makes the sum non-zero,
 *   push the drift onto the biggest creditor (if total > 0) or the
 *   biggest debtor (if total < 0) so that the final sum is exactly zero.
 */
public class Splitter {

  public Balance computeBalances(ArrayList<Expense> xs) {
    LinkedHashMap<String, BigDecimal> net = new LinkedHashMap<String, BigDecimal>();

    if (xs == null || xs.isEmpty()) {
      return new Balance(); // nothing to compute
    }

    final RoundingMode RM = RoundingMode.HALF_EVEN;
    final int WORK_SCALE = 10; // internal precision to minimize drift

    for (int i = 0; i < xs.size(); i++) {
      Expense e = xs.get(i);

      // Validation hook (Phase 1에서는 no-op)
      ExpenseValidator.validate(e);

      BigDecimal amount = e.getAmount();
      ArrayList<String> ps = e.getParticipants();
      if (amount == null || ps == null || ps.isEmpty()) continue;

      int n = ps.size();
      BigDecimal nBD = new BigDecimal(n);

      // Ensure payer/participants exist in the map
      ensureKey(net, e.getPayer());
      for (int k = 0; k < ps.size(); k++) ensureKey(net, ps.get(k));

      // Equal share (high precision)
      BigDecimal share = amount.divide(nBD, WORK_SCALE, RM);

      // Each participant owes 'share'
      for (int k = 0; k < ps.size(); k++) {
        String p = ps.get(k);
        net.put(p, net.get(p).subtract(share));
      }
      // Payer advanced the full amount
      String payer = e.getPayer();
      net.put(payer, net.get(payer).add(amount));
    }

    // Round to 2 decimals and package to Balance
    Balance out = new Balance();
    BigDecimal total = BigDecimal.ZERO;

    String maxCreditor = null;          // name with largest positive net
    BigDecimal maxCredVal = BigDecimal.ZERO;
    String maxDebtor = null;            // name with most negative net
    BigDecimal maxDebtVal = BigDecimal.ZERO;

    for (String name : net.keySet()) {
      BigDecimal rounded = net.get(name).setScale(2, RM);
      out.put(name, rounded);
      total = total.add(rounded);

      if (rounded.compareTo(BigDecimal.ZERO) > 0) {
        if (maxCreditor == null || rounded.compareTo(maxCredVal) > 0) {
          maxCreditor = name;
          maxCredVal = rounded;
        }
      } else if (rounded.compareTo(BigDecimal.ZERO) < 0) {
        if (maxDebtor == null || rounded.compareTo(maxDebtVal) < 0) { // "more negative"
          maxDebtor = name;
          maxDebtVal = rounded;
        }
      }
    }

    // Enforce sum == 0 by assigning rounding drift
    if (total.compareTo(BigDecimal.ZERO) != 0) {
      LinkedHashMap<String, BigDecimal> m = out.asMap();
      if (total.compareTo(BigDecimal.ZERO) > 0) {
        // too positive overall -> reduce the largest creditor
        String target = (maxCreditor != null) ? maxCreditor : anyKey(m);
        if (target != null) m.put(target, m.get(target).subtract(total));
      } else {
        // too negative overall -> increase the largest debtor
        String target = (maxDebtor != null) ? maxDebtor : anyKey(m);
        if (target != null) m.put(target, m.get(target).subtract(total)); // total is negative
      }
    }

    return out;
  }

  // --- helpers ---

  private static void ensureKey(LinkedHashMap<String, BigDecimal> m, String key) {
    if (key == null) return;
    if (!m.containsKey(key)) m.put(key, BigDecimal.ZERO);
  }

  private static String anyKey(LinkedHashMap<String, BigDecimal> m) {
    for (String k : m.keySet()) return k;
    return null;
  }
}

