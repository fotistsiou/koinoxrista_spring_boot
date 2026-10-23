package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/apartments")
public class AdminApartmentController {

    private final ApartmentRepository apartmentRepository;

    public AdminApartmentController(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @GetMapping
    public String listBalances(Model model) {
        model.addAttribute("balances", apartmentRepository.findAllBalances());
        return "admin/apartments";
    }
}
