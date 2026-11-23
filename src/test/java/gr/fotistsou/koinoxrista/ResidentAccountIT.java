package gr.fotistsou.koinoxrista;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.AppUser;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.entity.Debt;
import gr.fotistsou.koinoxrista.entity.Payment;
import gr.fotistsou.koinoxrista.enums.Role;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.AppUserRepository;
import gr.fotistsou.koinoxrista.repository.BillRepository;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import gr.fotistsou.koinoxrista.repository.DebtRepository;
import gr.fotistsou.koinoxrista.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ResidentAccountIT extends IntegrationTestBase {

    private static final String RESIDENT_A = "resident-a@test.gr";

    @Autowired private ApartmentRepository apartmentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BillRepository billRepository;
    @Autowired private DebtRepository debtRepository;
    @Autowired private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        Apartment a = apartment("Ισόγειο");
        Apartment b = apartment("1ος");
        resident(RESIDENT_A, "Άλκηστη", a);
        resident("resident-b@test.gr", "Βασίλης", b);

        Bill bill = bill(new BigDecimal("1111.10"));
        debt(bill, a, "123.45");
        debt(bill, b, "987.65");
        payment(a, "20.00");
        payment(b, "555.55");
    }

    @Test
    @WithUserDetails(value = RESIDENT_A, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void residentSeesOwnAccount() {
        assertThat(mvc.get().uri("/my"))
                .hasStatusOk()
                .bodyText()
                .contains("Καλώς ήρθες, Άλκηστη",
                        "Διαμέρισμα: Ισόγειο",
                        "123,45 €",   // own debt
                        "20,00 €",    // own payment
                        "103,45 €",   // balance = 123.45 - 20.00
                        "(οφειλή)");
    }

    @Test
    @WithUserDetails(value = RESIDENT_A, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void residentNeverSeesAnotherApartmentsData() {
        assertThat(mvc.get().uri("/my"))
                .hasStatusOk()
                .bodyText()
                .doesNotContain("1ος", "Βασίλης", "987,65", "555,55");
    }

    @Test
    @WithUserDetails(value = RESIDENT_A, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void realResidentIsForbiddenOnAdminPages() {
        assertThat(mvc.get().uri("/admin/apartments")).hasStatus(HttpStatus.FORBIDDEN);
    }

    // --- test data helpers ---

    private Apartment apartment(String name) {
        Apartment apartment = new Apartment();
        apartment.setName(name);
        return apartmentRepository.save(apartment);
    }

    private void resident(String email, String firstName, Apartment apartment) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName("Test");
        user.setPassword("unused"); // @WithUserDetails skips the login form
        user.setRole(Role.RESIDENT);
        user.setApartment(apartment);
        appUserRepository.save(user);
    }

    private Bill bill(BigDecimal amount) {
        Category category = new Category();
        category.setName("Καθαριότητα");
        category.setSplitRule(SplitRule.EQUAL_SPLIT);
        categoryRepository.save(category);

        Bill bill = new Bill();
        bill.setAmount(amount);
        bill.setBillDate(LocalDate.of(2026, 1, 31));
        bill.setCategory(category);
        bill.setSplitRule(SplitRule.EQUAL_SPLIT);
        return billRepository.save(bill);
    }

    private void debt(Bill bill, Apartment apartment, String amount) {
        Debt debt = new Debt();
        debt.setBill(bill);
        debt.setApartment(apartment);
        debt.setAmount(new BigDecimal(amount));
        debtRepository.save(debt);
    }

    private void payment(Apartment apartment, String amount) {
        Payment payment = new Payment();
        payment.setApartment(apartment);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentDate(LocalDate.of(2026, 2, 5));
        paymentRepository.save(payment);
    }
}