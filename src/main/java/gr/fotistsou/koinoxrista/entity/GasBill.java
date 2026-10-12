package gr.fotistsou.koinoxrista.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "gas_bill")
public class GasBill extends Bill {
    @Column(name = "meter_total", nullable = false)
    private Integer meterTotal;

    @Column(name = "meter_floor", nullable = false)
    private Integer meterFloor;

    @Column(name = "consumption_charge", precision = 10, scale = 2, nullable = false)
    private BigDecimal consumptionCharge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meter_apartment_id", nullable = false)
    private Apartment meterApartment;

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

    public BigDecimal getConsumptionCharge() {
        return consumptionCharge;
    }

    public void setConsumptionCharge(BigDecimal consumptionCharge) {
        this.consumptionCharge = consumptionCharge;
    }

    public Apartment getMeterApartment() {
        return meterApartment;
    }

    public void setMeterApartment(Apartment meterApartment) {
        this.meterApartment = meterApartment;
    }

    @Override
    public String toString() {
        return "GasBill{" +
                super.toString() +
                ", meterTotal=" + meterTotal +
                ", meterFloor=" + meterFloor +
                ", consumptionCharge=" + consumptionCharge +
                ", meterApartmentId=" + (meterApartment != null ? meterApartment.getId() : null) +
                '}';
    }
}
