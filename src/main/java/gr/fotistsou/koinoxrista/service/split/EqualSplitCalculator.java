package gr.fotistsou.koinoxrista.service.split;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EqualSplitCalculator implements SplitCalculator {

    @Override
    public SplitRule supportedRule() {
        return SplitRule.EQUAL_SPLIT;
    }

    @Override
    public Map<Apartment, BigDecimal> calculate(Bill bill, Set<Apartment> participants) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("No participants for bill " + bill.getId());
        }

        List<Apartment> ordered = participants.stream()
                .sorted(Comparator.comparing(Apartment::getId))
                .toList();

        BigDecimal amount = bill.getAmount();
        BigDecimal count = BigDecimal.valueOf(ordered.size());

        BigDecimal share = amount.divide(count, 2, RoundingMode.DOWN);
        BigDecimal remainder = amount.subtract(share.multiply(count));

        int extraCents = remainder.movePointRight(2).intValueExact();
        BigDecimal cent = BigDecimal.valueOf(extraCents >= 0 ? 1 : -1, 2);

        Map<Apartment, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            BigDecimal apartmentShare = i < Math.abs(extraCents) ? share.add(cent) : share;
            result.put(ordered.get(i), apartmentShare);
        }
        return result;
    }
}