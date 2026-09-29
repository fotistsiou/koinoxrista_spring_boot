package gr.fotistsou.koinoxrista.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "gas_bill")
public class GasBill extends Bill {
    @Column(name = "meter_total", nullable = false)
    private Integer meterTotal;

    @Column(name = "meter_floor", nullable = false)
    private Integer meterFloor;

    @Column(name = "fixed_charge", precision = 10, scale = 2, nullable = false)
    private BigDecimal fixedCharge;

    public Integer getMeterTotal() {
        return meterTotal;
    }

    public void setMeterTotal(Integer meterTotal) {
        this.meterTotal = meterTotal;
    }

    public Integer getMeterFloor() {
        return meterFloor;
    }

    public void setMeterFloor(Integer meterFloor) {
        this.meterFloor = meterFloor;
    }

    public BigDecimal getFixedCharge() {
        return fixedCharge;
    }

    public void setFixedCharge(BigDecimal fixedCharge) {
        this.fixedCharge = fixedCharge;
    }
}
