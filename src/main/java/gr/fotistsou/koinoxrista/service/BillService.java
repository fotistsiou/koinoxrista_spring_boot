package gr.fotistsou.koinoxrista.service;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.entity.Debt;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.BillRepository;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import gr.fotistsou.koinoxrista.repository.DebtRepository;
import gr.fotistsou.koinoxrista.service.split.SplitCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BillService {

    private final Map<SplitRule, SplitCalculator> calculators;
    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;
    private final DebtRepository debtRepository;

    public BillService(List<SplitCalculator> calculators,
                       BillRepository billRepository,
                       CategoryRepository categoryRepository,
                       DebtRepository debtRepository) {
        this.calculators = calculators.stream()
                .collect(Collectors.toMap(SplitCalculator::supportedRule, Function.identity()));
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
        this.debtRepository = debtRepository;
    }

    @Transactional
    public Bill registerBill(Bill bill) {
        Category category = categoryRepository.findById(bill.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found: " + bill.getCategory().getId()));
        bill.setCategory(category);

        SplitCalculator calculator = calculators.get(bill.getSplitRule());
        if (calculator == null) {
            throw new IllegalStateException("No calculator for rule " + bill.getSplitRule());
        }

        Bill savedBill = billRepository.save(bill);
        Map<Apartment, BigDecimal> shares = calculator.calculate(savedBill, category.getApartments());

        List<Debt> debts = shares.entrySet().stream()
                .map(share -> toDebt(savedBill, share.getKey(), share.getValue()))
                .toList();
        debtRepository.saveAll(debts);

        return savedBill;
    }

    private Debt toDebt(Bill bill, Apartment apartment, BigDecimal amount) {
        Debt debt = new Debt();
        debt.setBill(bill);
        debt.setApartment(apartment);
        debt.setAmount(amount);
        return debt;
    }
}