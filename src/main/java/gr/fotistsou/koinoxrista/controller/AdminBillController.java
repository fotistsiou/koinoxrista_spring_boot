package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.dto.BillForm;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import gr.fotistsou.koinoxrista.service.BillService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/bills")
public class AdminBillController {

    private final CategoryRepository categoryRepository;
    private final BillService billService;

    public AdminBillController(CategoryRepository categoryRepository, BillService billService) {
        this.categoryRepository = categoryRepository;
        this.billService = billService;
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryRepository.findBySplitRuleOrderByNameAsc(SplitRule.EQUAL_SPLIT);
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("billForm", new BillForm());
        return "admin/bill-form";
    }

    @PostMapping
    public String registerBill(@Valid @ModelAttribute("billForm") BillForm billForm,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/bill-form";
        }

        Bill bill = new Bill();
        bill.setAmount(billForm.getAmount());
        bill.setBillDate(billForm.getBillDate());
        bill.setCategory(categoryRepository.getReferenceById(billForm.getCategoryId()));

        Bill saved;
        try {
            saved = billService.registerBill(bill);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("billError", e.getMessage());
            return "admin/bill-form";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Καταχωρίστηκε: " + saved.getCategory().getName() + ", " + saved.getAmount() + " €");
        return "redirect:/admin/bills/new";
    }

    @GetMapping
    public String listBills(Model model) {
        model.addAttribute("billsWithDebts", billService.findBillsWithDebts());
        return "admin/bills";
    }
}