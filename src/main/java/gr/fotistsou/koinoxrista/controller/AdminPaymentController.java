package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.dto.ApartmentBalance;
import gr.fotistsou.koinoxrista.dto.PaymentForm;
import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Payment;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.PaymentRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final ApartmentRepository apartmentRepository;
    private final PaymentRepository paymentRepository;

    public AdminPaymentController(ApartmentRepository apartmentRepository,
                                  PaymentRepository paymentRepository) {
        this.apartmentRepository = apartmentRepository;
        this.paymentRepository = paymentRepository;
    }

    @ModelAttribute("apartments")
    public List<Apartment> apartments() {
        return apartmentRepository.findAll();
    }

    @GetMapping("/new")
    public String showForm(@RequestParam(required = false) Long apartmentId, Model model) {
        PaymentForm form = new PaymentForm();
        form.setPaymentDate(LocalDate.now());

        if (apartmentId != null) {
            form.setApartmentId(apartmentId);
            apartmentRepository.findAllBalances().stream()
                    .filter(row -> row.apartmentId().equals(apartmentId))
                    .map(ApartmentBalance::balance)
                    .filter(balance -> balance.signum() > 0)
                    .findFirst()
                    .ifPresent(form::setAmount);
        }

        model.addAttribute("paymentForm", form);
        return "admin/payment-form";
    }

    @PostMapping
    public String registerPayment(@Valid @ModelAttribute("paymentForm") PaymentForm form,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/payment-form";
        }

        Payment payment = new Payment();
        payment.setAmount(form.getAmount());
        payment.setPaymentDate(form.getPaymentDate());
        payment.setApartment(apartmentRepository.getReferenceById(form.getApartmentId()));
        paymentRepository.save(payment);

        redirectAttributes.addFlashAttribute("successMessage",
                "Καταχωρίστηκε πληρωμή " + form.getAmount() + " €");
        return "redirect:/admin/apartments";
    }
}