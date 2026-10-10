package gr.fotistsou.koinoxrista.service.split;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EqualSplitCalculatorTest {

    private EqualSplitCalculator calculator;
    private Apartment groundFloor;
    private Apartment firstFloor;
    private Apartment secondFloor;

    @BeforeEach
    void setUp() {
        calculator = new EqualSplitCalculator();
        groundFloor = apartment(1L, "Ισόγειο");
        firstFloor = apartment(2L, "1ος");
        secondFloor = apartment(3L, "2ος");
    }

    @Test
    void splitsBoilerMaintenanceEquallyBetweenTwo() {
        // Arrange: boiler maintenance from the gas Excel (80.00 -> 40.00 each)
        Bill bill = bill("80.00");

        // Act
        Map<Apartment, BigDecimal> result = calculator.calculate(bill, Set.of(groundFloor, firstFloor));

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(groundFloor)).isEqualByComparingTo("40.00");
        assertThat(result.get(firstFloor)).isEqualByComparingTo("40.00");
    }

    @Test
    void givesLeftoverCentToLowestIdApartment() {
        Bill bill = bill("10.01");

        Map<Apartment, BigDecimal> result = calculator.calculate(bill, Set.of(groundFloor, firstFloor));

        assertThat(result.get(groundFloor)).isEqualByComparingTo("5.01");
        assertThat(result.get(firstFloor)).isEqualByComparingTo("5.00");
    }

    @Test
    void splitsDisinfectionAmongThreeAndKeepsTotalExact() {
        Bill bill = bill("100.00");

        Map<Apartment, BigDecimal> result =
                calculator.calculate(bill, Set.of(groundFloor, firstFloor, secondFloor));

        assertThat(result.get(groundFloor)).isEqualByComparingTo("33.34");
        assertThat(result.get(firstFloor)).isEqualByComparingTo("33.33");
        assertThat(result.get(secondFloor)).isEqualByComparingTo("33.33");
        assertThat(sum(result)).isEqualByComparingTo("100.00");
    }

    @Test
    void splitsNegativeCreditBillAndKeepsTotalExact() {
        Bill bill = bill("-4.55");

        Map<Apartment, BigDecimal> result = calculator.calculate(bill, Set.of(groundFloor, firstFloor));

        assertThat(result.get(groundFloor)).isEqualByComparingTo("-2.28");
        assertThat(result.get(firstFloor)).isEqualByComparingTo("-2.27");
        assertThat(sum(result)).isEqualByComparingTo("-4.55");
    }

    @Test
    void throwsWhenThereAreNoParticipants() {
        Bill bill = bill("10.00");

        assertThatThrownBy(() -> calculator.calculate(bill, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No participants");
    }

    // --- test helpers ---

    private Apartment apartment(Long id, String name) {
        Apartment apartment = new Apartment();
        apartment.setName(name);
        ReflectionTestUtils.setField(apartment, "id", id);
        return apartment;
    }

    private Bill bill(String amount) {
        Bill bill = new Bill();
        bill.setAmount(new BigDecimal(amount));
        return bill;
    }

    private BigDecimal sum(Map<Apartment, BigDecimal> result) {
        return result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}