package gr.fotistsou.koinoxrista.dto;

import gr.fotistsou.koinoxrista.entity.Debt;
import gr.fotistsou.koinoxrista.entity.Payment;

import java.util.List;

/**
 * Everything the resident account page needs, loaded in one read-only transaction.
 */
public record ResidentAccount(String firstName,
                              ApartmentBalance balance,
                              List<Debt> debts,
                              List<Payment> payments) {
}