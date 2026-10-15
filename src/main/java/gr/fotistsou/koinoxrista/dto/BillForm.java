package gr.fotistsou.koinoxrista.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillForm {

    @NotNull(message = "Το ποσό είναι υποχρεωτικό")
    @Digits(integer = 8, fraction = 2, message = "Έως 8 ακέραια και 2 δεκαδικά ψηφία")
    private BigDecimal amount;

    @NotNull(message = "Η ημερομηνία είναι υποχρεωτική")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate billDate;

    @NotNull(message = "Η κατηγορία είναι υποχρεωτική")
    private Long categoryId;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}