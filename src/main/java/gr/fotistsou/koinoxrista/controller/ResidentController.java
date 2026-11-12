package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.dto.ResidentAccount;
import gr.fotistsou.koinoxrista.service.ResidentAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my")
public class ResidentController {

    private final ResidentAccountService residentAccountService;

    public ResidentController(ResidentAccountService residentAccountService) {
        this.residentAccountService = residentAccountService;
    }

    @GetMapping
    public String myAccount(@AuthenticationPrincipal UserDetails principal, Model model) {
        ResidentAccount account = residentAccountService.loadAccount(principal.getUsername());

        model.addAttribute("firstName", account.firstName());
        model.addAttribute("balance", account.balance());
        model.addAttribute("debts", account.debts());
        model.addAttribute("payments", account.payments());
        return "my/account";
    }
}