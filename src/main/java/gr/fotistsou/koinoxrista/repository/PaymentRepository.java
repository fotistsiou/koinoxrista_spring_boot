package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByApartmentIdOrderByPaymentDateDesc(Long apartmentId);
}
