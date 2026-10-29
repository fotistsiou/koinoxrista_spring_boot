package gr.fotistsou.koinoxrista.dto;

import gr.fotistsou.koinoxrista.enums.SplitRule;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class CategoryForm {

    @NotBlank(message = "Το όνομα είναι υποχρεωτικό")
    @Size(max = 100, message = "Έως 100 χαρακτήρες")
    private String name;

    @NotNull(message = "Ο τρόπος υπολογισμού είναι υποχρεωτικός")
    private SplitRule splitRule;

    @NotEmpty(message = "Επίλεξε τουλάχιστον ένα διαμέρισμα")
    private Set<Long> apartmentIds = new HashSet<>();

    @AssertTrue(message = "Ο υπολογισμός με μετρητές απαιτεί ακριβώς 2 διαμερίσματα")
    public boolean isMeterBasedWithTwoApartments() {
        return splitRule != SplitRule.METER_BASED
                || (apartmentIds != null && apartmentIds.size() == 2);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }

    public SplitRule getSplitRule() {
        return splitRule;
    }

    public void setSplitRule(SplitRule splitRule) {
        this.splitRule = splitRule;
    }

    public Set<Long> getApartmentIds() {
        return apartmentIds;
    }

    public void setApartmentIds(Set<Long> apartmentIds) {
        this.apartmentIds = apartmentIds;
    }
}