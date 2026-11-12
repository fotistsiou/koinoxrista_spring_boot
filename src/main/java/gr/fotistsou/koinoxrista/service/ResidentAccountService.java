package gr.fotistsou.koinoxrista.service;

import gr.fotistsou.koinoxrista.dto.ApartmentBalance;
import gr.fotistsou.koinoxrista.dto.ResidentAccount;
import gr.fotistsou.koinoxrista.entity.AppUser;
import gr.fotistsou.koinoxrista.entity.Debt;
import gr.fotistsou.koinoxrista.entity.Payment;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.AppUserRepository;
import gr.fotistsou.koinoxrista.repository.DebtRepository;
import gr.fotistsou.koinoxrista.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResidentAccountService {

    private final AppUserRepository appUserRepository;
    private final ApartmentRepository apartmentRepository;
    private final DebtRepository debtRepository;
    private final PaymentRepository paymentRepository;

    public ResidentAccountService(AppUserRepository appUserRepository,
                                  ApartmentRepository apartmentRepository,
                                  DebtRepository debtRepository,
                                  PaymentRepository paymentRepository) {
        this.appUserRepository = appUserRepository;
        this.apartmentRepository = apartmentRepository;
        this.debtRepository = debtRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public ResidentAccount loadAccount(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found: " + email));

        // getId() on a lazy proxy does not hit the database
        Long apartmentId = user.getApartment().getId();

        ApartmentBalance balance = apartmentRepository.findBalance(apartmentId)
                .orElseThrow(() -> new IllegalStateException("Apartment not found: " + apartmentId));
        List<Debt> debts = debtRepository.findByApartmentIdWithBill(apartmentId);
        List<Payment> payments = paymentRepository.findByApartmentIdOrderByPaymentDateDesc(apartmentId);

        return new ResidentAccount(user.getFirstName(), balance, debts, payments);
    }
}