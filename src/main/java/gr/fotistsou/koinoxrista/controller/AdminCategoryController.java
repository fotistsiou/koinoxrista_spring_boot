package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.dto.CategoryForm;
import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import gr.fotistsou.koinoxrista.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;
    private final ApartmentRepository apartmentRepository;
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryRepository categoryRepository,
                                   ApartmentRepository apartmentRepository,
                                   CategoryService categoryService) {
        this.categoryRepository = categoryRepository;
        this.apartmentRepository = apartmentRepository;
        this.categoryService = categoryService;
    }

    @ModelAttribute("apartments")
    public List<Apartment> apartments() {
        return apartmentRepository.findAll();
    }

    @ModelAttribute("splitRules")
    public SplitRule[] splitRules() {
        return SplitRule.values();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        return "admin/categories";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return "admin/category-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findWithApartmentsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setSplitRule(category.getSplitRule());
        form.setApartmentIds(category.getApartments().stream()
                .map(Apartment::getId)
                .collect(Collectors.toSet()));

        model.addAttribute("categoryForm", form);
        model.addAttribute("categoryId", id);
        return "admin/category-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        return save(null, form, bindingResult, redirectAttributes);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        model.addAttribute("categoryId", id);
        return save(id, form, bindingResult, redirectAttributes);
    }

    private String save(Long id, CategoryForm form, BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {
        boolean nameTaken = id == null
                ? categoryRepository.existsByName(form.getName())
                : categoryRepository.existsByNameAndIdNot(form.getName(), id);
        if (nameTaken) {
            bindingResult.rejectValue("name", "duplicate", "Υπάρχει ήδη κατηγορία με αυτό το όνομα");
        }
        if (bindingResult.hasErrors()) {
            return "admin/category-form";
        }

        Category saved = categoryService.save(id, form.getName(), form.getSplitRule(), form.getApartmentIds());
        redirectAttributes.addFlashAttribute("successMessage", "Αποθηκεύτηκε η κατηγορία: " + saved.getName());
        return "redirect:/admin/categories";
    }
}