package gr.fotistsou.koinoxrista.service.split;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.entity.GasBill;
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
public class MeterBasedSplitCalculator implements SplitCalculator {

    @Override
    public SplitRule supportedRule() {
        return SplitRule.METER_BASED;
    }

    @Override
    public Map<Apartment, BigDecimal> calculate(Bill bill, Set<Apartment> participants) {
        if (!(bill instanceof GasBill gasBill)) {
            throw new IllegalArgumentException("METER_BASED requires a GasBill, got bill " + bill.getId());
        }
        if (participants.size() != 2) {
            throw new IllegalArgumentException(
                    "METER_BASED needs exactly 2 participants, got " + participants.size());
        }

        Long meterApartmentId = gasBill.getMeterApartment().getId();
        boolean meterApartmentParticipates = participants.stream()
                .anyMatch(apartment -> apartment.getId().equals(meterApartmentId));
        if (!meterApartmentParticipates) {
            throw new IllegalArgumentException(
                    "Meter apartment " + meterApartmentId + " is not a participant of bill " + bill.getId());
        }

        BigDecimal amount = gasBill.getAmount();
        BigDecimal fixedCharge = gasBill.getFixedCharge();
        BigDecimal variableCost = amount.subtract(fixedCharge); // Ω

        BigDecimal meterShare = variableCost
                .multiply(BigDecimal.valueOf(gasBill.getMeterFloor()))
                .divide(BigDecimal.valueOf(gasBill.getMeterTotal()), 10, RoundingMode.HALF_UP)
                .add(fixedCharge.divide(BigDecimal.TWO))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherShare = amount.subtract(meterShare);

        Map<Apartment, BigDecimal> result = new LinkedHashMap<>();
        participants.stream()
                .sorted(Comparator.comparing(Apartment::getId))
                .forEach(apartment -> result.put(apartment,
                        apartment.getId().equals(meterApartmentId) ? meterShare : otherShare));
        return result;
    }
}
