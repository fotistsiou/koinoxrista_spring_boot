package gr.fotistsou.koinoxrista.service.split;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.enums.SplitRule;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface SplitCalculator {

    SplitRule supportedRule();

    Map<Apartment, BigDecimal> calculate(Bill bill, Set<Apartment> participants);
}