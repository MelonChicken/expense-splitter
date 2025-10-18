package itm.oss.splitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Splitter {

  public Balance computeBalances(ArrayList<Expense> xs) {
    // TODO (Issue 4): equal split math.
    // Sum nets so total = 0; scale(2) rounding HALF_EVEN when needed.
    Balance out = new Balance();
    if (xs == null || xs.isEmpty()) return out;

    LinkedHashMap<String, BigDecimal> net = out.asMap(); // 결과 누적

    for (Expense e : xs) {
      if (e == null) continue;

      BigDecimal amount = safe2(e.getAmount());                // 금액은 scale=2 고정
      ArrayList<String> parts = e.getParticipants();
      if (parts == null || parts.isEmpty()) continue;          // 참여자 없으면 스킵(또는 예외)

      int n = parts.size();

      // 1) 1인당 기본 몫 (반올림: HALF_EVEN)
      BigDecimal baseShare = amount
          .divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_EVEN);

      // 2) 모두 baseShare로 더했을 때와 실제 금액의 차이를 센트로 계산
      BigDecimal sumBase = baseShare.multiply(BigDecimal.valueOf(n)).setScale(2, RoundingMode.HALF_EVEN);
      BigDecimal diff = amount.subtract(sumBase);              // ex) 0.01, -0.02 등
      int cents = diff.movePointRight(2).intValue();           // 보정해야 할 센트 개수(+/-)

      // 3) 각 참여자 몫 배열을 만들고, 앞에서부터 diff만큼 1센트씩 배분/회수
      BigDecimal[] shares = new BigDecimal[n];
      for (int i = 0; i < n; i++) shares[i] = baseShare;

      int i = 0;
      while (cents > 0) {                                      // 아직 총합이 모자라면 일부에게 +0.01
        shares[i] = shares[i].add(c01());
        cents--;
        i = (i + 1) % n;
      }
      while (cents < 0) {                                      // 총합이 넘치면 일부에게 -0.01
        shares[i] = shares[i].subtract(c01());
        cents++;
        i = (i + 1) % n;
      }

      // 4) 지불자(payer)는 전액 +, 참여자들은 자기 몫만큼 -
      String payer = e.getPayer();
      net.put(payer, safe2(net.getOrDefault(payer, BigDecimal.ZERO).add(amount)));

      for (int k = 0; k < n; k++) {
        String name = parts.get(k);
        BigDecimal cur = net.getOrDefault(name, BigDecimal.ZERO);
        net.put(name, safe2(cur.subtract(shares[k])));
      }
    }

    // 5) 안전장치: 누적 반올림으로 총합이 ±0.01 남는 경우 보정
    BigDecimal total = BigDecimal.ZERO;
    for (BigDecimal v : net.values()) total = total.add(v);
    total = safe2(total);
    if (total.compareTo(BigDecimal.ZERO) != 0 && !net.isEmpty()) {
      // 맨 앞 사람에게 -total을 더해 총합을 정확히 0으로 맞춤(결과 재현성을 위해 LinkedHashMap 순서 사용)
      String first = net.keySet().iterator().next();
      net.put(first, safe2(net.get(first).subtract(total)));
    }

    return out;
  }

  private static BigDecimal safe2(BigDecimal x) {
    if (x == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    return x.setScale(2, RoundingMode.HALF_EVEN);
  }
  private static BigDecimal c01() {
    return new BigDecimal("0.01");
  }
}


