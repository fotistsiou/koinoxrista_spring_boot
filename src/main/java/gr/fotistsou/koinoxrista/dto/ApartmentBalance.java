package gr.fotistsou.koinoxrista.dto;

import java.math.BigDecimal;

public record ApartmentBalance(Long apartmentId,
                               String apartmentName,
                               BigDecimal totalDebts,
                               BigDecimal totalPayments) {

    public ApartmentBalance {
        totalDebts = totalDebts != null ? totalDebts : BigDecimal.ZERO;
        totalPayments = totalPayments != null ? totalPayments : BigDecimal.ZERO;
    }

    public BigDecimal balance() {
        return totalDebts.subtract(totalPayments);
    }
}