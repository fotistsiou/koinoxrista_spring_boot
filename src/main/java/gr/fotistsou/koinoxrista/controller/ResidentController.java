package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.entity.AppUser;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.AppUserRepository;
import gr.fotistsou.koinoxrista.repository.DebtRepository;
import gr.fotistsou.koinoxrista.repository.PaymentRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my")
public class ResidentController {

    private final AppUserRepository appUserRepository;
    private final ApartmentRepository apartmentRepository;
    private final DebtRepository debtRepository;
    private final PaymentRepository paymentRepository;

    public ResidentController(AppUserRepository appUserRepository,
                              ApartmentRepository apartmentRepository,
                              DebtRepository debtRepository,
                              PaymentRepository paymentRepository) {
        this.appUserRepository = appUserRepository;
        this.apartmentRepository = apartmentRepository;
        this.debtRepository = debtRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public String myAccount(@AuthenticationPrincipal UserDetails principal, Model model) {
        AppUser user = appUserRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found"));
        Long apartmentId = user.getApartment().getId();

        model.addAttribute("user", user);
        model.addAttribute("balance", apartmentRepository.findBalance(apartmentId).orElseThrow());
        model.addAttribute("debts", debtRepository.findByApartmentIdWithBill(apartmentId));
        model.addAttribute("payments", paymentRepository.findByApartmentIdOrderByPaymentDateDesc(apartmentId));
        return "my/account";
    }
}