package gr.fotistsou.koinoxrista.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentForm {

    @NotNull(message = "Το διαμέρισμα είναι υποχρεωτικό")
    private Long apartmentId;

    @NotNull(message = "Το ποσό είναι υποχρεωτικό")
    @Positive(message = "Το ποσό της πληρωμής πρέπει να είναι μεγαλύτερο από 0")
    @Digits(integer = 8, fraction = 2, message = "Έως 8 ακέραια και 2 δεκαδικά ψηφία")
    private BigDecimal amount;

    @NotNull(message = "Η ημερομηνία είναι υποχρεωτική")
    @PastOrPresent(message = "Η ημερομηνία δεν μπορεί να είναι στο μέλλον")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    public Long getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}