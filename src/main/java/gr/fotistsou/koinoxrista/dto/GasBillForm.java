package gr.fotistsou.koinoxrista.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GasBillForm {

    @NotNull(message = "Το ποσό είναι υποχρεωτικό")
    @Digits(integer = 8, fraction = 2, message = "Έως 8 ακέραια και 2 δεκαδικά ψηφία")
    private BigDecimal amount;

    @NotNull(message = "Η κατανάλωση (Ω) είναι υποχρεωτική")
    @Digits(integer = 8, fraction = 2, message = "Έως 8 ακέραια και 2 δεκαδικά ψηφία")
    private BigDecimal consumptionCharge;

    @NotNull(message = "Η ένδειξη του μετρητή ορόφου είναι υποχρεωτική")
    @PositiveOrZero(message = "Η ένδειξη δεν μπορεί να είναι αρνητική")
    private Integer meterFloor;

    @NotNull(message = "Η ένδειξη του γενικού μετρητή είναι υποχρεωτική")
    @Positive(message = "Η ένδειξη του γενικού μετρητή πρέπει να είναι μεγαλύτερη από 0")
    private Integer meterTotal;

    @NotNull(message = "Το διαμέρισμα του μετρητή είναι υποχρεωτικό")
    private Long meterApartmentId;

    @NotNull(message = "Η κατηγορία είναι υποχρεωτική")
    private Long categoryId;

    @NotNull(message = "Η ημερομηνία είναι υποχρεωτική")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate billDate;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getConsumptionCharge() {
        return consumptionCharge;
    }

    public void setConsumptionCharge(BigDecimal consumptionCharge) {
        this.consumptionCharge = consumptionCharge;
    }

    public Integer getMeterFloor() {
        return meterFloor;
    }

    public void setMeterFloor(Integer meterFloor) {
        this.meterFloor = meterFloor;
    }

    public Integer getMeterTotal() {
        return meterTotal;
    }

    public void setMeterTotal(Integer meterTotal) {
        this.meterTotal = meterTotal;
    }

    public Long getMeterApartmentId() {
        return meterApartmentId;
    }

    public void setMeterApartmentId(Long meterApartmentId) {
        this.meterApartmentId = meterApartmentId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }
}