package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
    @Query("""
        select d from Debt d
        join fetch d.bill b
        join fetch b.category
        join fetch d.apartment a
        order by b.billDate desc, b.id desc, a.id
        """)
    List<Debt> findAllWithBillAndApartment();
}
