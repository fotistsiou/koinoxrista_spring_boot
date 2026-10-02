package gr.fotistsou.koinoxrista.entity;

import gr.fotistsou.koinoxrista.enums.SplitRule;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "bill")
public class Bill {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_rule", nullable = false)
    private SplitRule splitRule;

    public SplitRule getSplitRule() {
        return splitRule;
    }

    public void setSplitRule(SplitRule splitRule) {
        this.splitRule = splitRule;
    }

    public Long getId() {
        return id;
    }

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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + id +
                ", amount=" + amount +
                ", billDate=" + billDate +
                ", splitRule=" + splitRule +
                ", categoryId=" + (category != null ? category.getId() : null) +
                '}';
    }
}
