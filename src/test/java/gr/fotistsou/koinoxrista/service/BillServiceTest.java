package gr.fotistsou.koinoxrista.service;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.entity.Debt;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.BillRepository;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import gr.fotistsou.koinoxrista.repository.DebtRepository;
import gr.fotistsou.koinoxrista.service.split.EqualSplitCalculator;
import gr.fotistsou.koinoxrista.service.split.MeterBasedSplitCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private DebtRepository debtRepository;
    @Captor
    private ArgumentCaptor<List<Debt>> debtsCaptor;

    private BillService billService;
    private Apartment groundFloor;
    private Apartment firstFloor;

    @BeforeEach
    void setUp() {
        billService = new BillService(
                List.of(new EqualSplitCalculator(), new MeterBasedSplitCalculator()),
                billRepository, categoryRepository, debtRepository);
        groundFloor = apartment(1L, "Ισόγειο");
        firstFloor = apartment(2L, "1ος");
    }

    @Test
    void createsOneDebtPerParticipantAndSavesThem() {
        // Arrange
        Category electricity = category(10L, groundFloor, firstFloor);
        Bill bill = bill("80.00", SplitRule.EQUAL_SPLIT, categoryReference(10L));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(electricity));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Bill saved = billService.registerBill(bill);

        // Assert
        verify(debtRepository).saveAll(debtsCaptor.capture());
        List<Debt> debts = debtsCaptor.getValue();

        assertThat(debts).hasSize(2);
        assertThat(debts).extracting(Debt::getApartment).containsExactly(groundFloor, firstFloor);
        assertThat(debts).allSatisfy(debt -> {
            assertThat(debt.getBill()).isSameAs(saved);
            assertThat(debt.getAmount()).isEqualByComparingTo("40.00");
        });
        assertThat(saved.getCategory()).isSameAs(electricity);
    }

    @Test
    void savesNothingWhenCategoryDoesNotExist() {
        Bill bill = bill("80.00", SplitRule.EQUAL_SPLIT, categoryReference(99L));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billService.registerBill(bill))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(billRepository, never()).save(any());
        verifyNoInteractions(debtRepository);
    }

    @Test
    void savesNoDebtsWhenCalculatorRejectsTheBill() {
        // METER_BASED needs a GasBill; a plain Bill must be rejected by the calculator
        Category gas = category(20L, groundFloor, firstFloor);
        Bill plainBill = bill("51.61", SplitRule.METER_BASED, categoryReference(20L));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(gas));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> billService.registerBill(plainBill))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a GasBill");

        verify(billRepository).save(plainBill);
        verifyNoInteractions(debtRepository);
    }

    // --- test helpers ---

    private Apartment apartment(Long id, String name) {
        Apartment apartment = new Apartment();
        apartment.setName(name);
        ReflectionTestUtils.setField(apartment, "id", id);
        return apartment;
    }

    /** A category that only carries an id, like the one a submitted form would contain. */
    private Category categoryReference(Long id) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Category category(Long id, Apartment... apartments) {
        Category category = categoryReference(id);
        category.getApartments().addAll(List.of(apartments));
        return category;
    }

    private Bill bill(String amount, SplitRule splitRule, Category category) {
        Bill bill = new Bill();
        bill.setAmount(new BigDecimal(amount));
        bill.setSplitRule(splitRule);
        bill.setCategory(category);
        return bill;
    }
}