package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.dto.ResidentForm;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.AppUserRepository;
import gr.fotistsou.koinoxrista.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserService userService;

    public AdminUserController(AppUserRepository userRepository, ApartmentRepository apartmentRepository, UserService userService) {
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAllByOrderByApartmentIdAsc());
        return "admin/users";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ResidentForm());
        model.addAttribute("apartments", apartmentRepository.findWithoutUser());
        return "admin/user-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ResidentForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!result.hasFieldErrors("email")
                && userRepository.existsByEmail(UserService.normalizeEmail(form.getEmail()))) {
            result.rejectValue("email", "duplicate", "Το email χρησιμοποιείται ήδη");
        }
        if (form.getApartmentId() != null && userRepository.existsByApartmentId(form.getApartmentId())) {
            result.rejectValue("apartmentId", "taken", "Το διαμέρισμα έχει ήδη χρήστη");
        }
        if (result.hasErrors()) {
            model.addAttribute("apartments", apartmentRepository.findWithoutUser());
            return "admin/user-form";
        }

        String tempPassword = userService.createResident(form);
        redirectAttributes.addFlashAttribute("createdEmail", UserService.normalizeEmail(form.getEmail()));
        redirectAttributes.addFlashAttribute("tempPassword", tempPassword);
        return "redirect:/admin/users";
    }
}
