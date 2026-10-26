package gr.fotistsou.koinoxrista.controller;

import gr.fotistsou.koinoxrista.dto.GasBillForm;
import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.entity.GasBill;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
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
@RequestMapping("/admin/gas-bills")
public class AdminGasBillController {

    private final CategoryRepository categoryRepository;
    private final ApartmentRepository apartmentRepository;
    private final BillService billService;

    public AdminGasBillController(CategoryRepository categoryRepository,
                                  ApartmentRepository apartmentRepository,
                                  BillService billService) {
        this.categoryRepository = categoryRepository;
        this.apartmentRepository = apartmentRepository;
        this.billService = billService;
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryRepository.findBySplitRuleOrderByNameAsc(SplitRule.METER_BASED);
    }

    @ModelAttribute("apartments")
    public List<Apartment> apartments() {
        return apartmentRepository.findAll();
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("gasBillForm", new GasBillForm());
        return "admin/gas-bill-form";
    }

    @PostMapping
    public String registerGasBill(@Valid @ModelAttribute("gasBillForm") GasBillForm form,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/gas-bill-form";
        }

        GasBill bill = new GasBill();
        bill.setAmount(form.getAmount());
        bill.setConsumptionCharge(form.getConsumptionCharge());
        bill.setMeterFloor(form.getMeterFloor());
        bill.setMeterTotal(form.getMeterTotal());
        bill.setMeterApartment(apartmentRepository.getReferenceById(form.getMeterApartmentId()));
        bill.setBillDate(form.getBillDate());
        bill.setCategory(categoryRepository.getReferenceById(form.getCategoryId()));

        Bill saved;
        try {
            saved = billService.registerBill(bill);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("billError", e.getMessage());
            return "admin/gas-bill-form";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Καταχωρίστηκε λογαριασμός αερίου: " + saved.getAmount() + " €");
        return "redirect:/admin/gas-bills/new";
    }
}