package gr.fotistsou.koinoxrista.service.split;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Bill;
import gr.fotistsou.koinoxrista.entity.GasBill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeterBasedSplitCalculatorTest {

    private MeterBasedSplitCalculator calculator;
    private Apartment groundFloor;
    private Apartment firstFloor;
    private Apartment secondFloor;

    @BeforeEach
    void setUp() {
        calculator = new MeterBasedSplitCalculator();
        groundFloor = apartment(1L, "Ισόγειο");
        firstFloor = apartment(2L, "1ος");
        secondFloor = apartment(3L, "2ος");
    }

    // Real months from the 2025 gas Excel. Expected values are the Excel X1 / X0, rounded to cents.
    @ParameterizedTest(name = "{index}: amount={0}, consumption={1} -> meter={4}, other={5}")
    @CsvSource({
            "51.61,  48.69,  115588, 291955, 20.74, 30.87",   // January
            "100.25, 101.81, 118367, 294985, 40.07, 60.18",   // February: consumption above amount
            "41.21,  0,      120634, 298341, 20.61, 20.60",   // March: no consumption (Ω = 0)
            "-0.17,  2.50,   121233, 299161, -0.32,  0.15"    // December: negative bill
    })
    void matchesExcelResults(BigDecimal amount, BigDecimal consumptionCharge,
                             Integer meterFloor, Integer meterTotal,
                             BigDecimal expectedMeterShare, BigDecimal expectedOtherShare) {
        GasBill bill = gasBill(amount, consumptionCharge, meterFloor, meterTotal, firstFloor);

        Map<Apartment, BigDecimal> result = calculator.calculate(bill, Set.of(groundFloor, firstFloor));

        assertThat(result.get(firstFloor)).isEqualByComparingTo(expectedMeterShare);
        assertThat(result.get(groundFloor)).isEqualByComparingTo(expectedOtherShare);
        assertThat(sum(result)).isEqualByComparingTo(amount);
    }

    @Test
    void throwsWhenBillIsNotAGasBill() {
        Bill plainBill = new Bill();
        plainBill.setAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> calculator.calculate(plainBill, Set.of(groundFloor, firstFloor)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a GasBill");
    }

    @Test
    void throwsWhenParticipantsAreNotExactlyTwo() {
        GasBill bill = gasBill(new BigDecimal("51.61"), new BigDecimal("48.69"), 115588, 291955, firstFloor);

        assertThatThrownBy(() -> calculator.calculate(bill, Set.of(groundFloor, firstFloor, secondFloor)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 2 participants");
    }

    @Test
    void throwsWhenMeterApartmentIsNotAParticipant() {
        GasBill bill = gasBill(new BigDecimal("51.61"), new BigDecimal("48.69"), 115588, 291955, secondFloor);

        assertThatThrownBy(() -> calculator.calculate(bill, Set.of(groundFloor, firstFloor)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a participant");
    }

    // --- test helpers ---

    private Apartment apartment(Long id, String name) {
        Apartment apartment = new Apartment();
        apartment.setName(name);
        ReflectionTestUtils.setField(apartment, "id", id);
        return apartment;
    }

    private GasBill gasBill(BigDecimal amount, BigDecimal consumptionCharge,
                            Integer meterFloor, Integer meterTotal, Apartment meterApartment) {
        GasBill bill = new GasBill();
        bill.setAmount(amount);
        bill.setConsumptionCharge(consumptionCharge);
        bill.setMeterFloor(meterFloor);
        bill.setMeterTotal(meterTotal);
        bill.setMeterApartment(meterApartment);
        return bill;
    }

    private BigDecimal sum(Map<Apartment, BigDecimal> result) {
        return result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}