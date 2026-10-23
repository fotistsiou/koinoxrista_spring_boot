package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.dto.ApartmentBalance;
import gr.fotistsou.koinoxrista.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
    Optional<Apartment> findByName(String name);

    @Query("""
        select new gr.fotistsou.koinoxrista.dto.ApartmentBalance(
            a.id,
            a.name,
            (select sum(d.amount) from Debt d where d.apartment = a),
            (select sum(p.amount) from Payment p where p.apartment = a))
        from Apartment a
        order by a.id
        """)
    List<ApartmentBalance> findAllBalances();
}
