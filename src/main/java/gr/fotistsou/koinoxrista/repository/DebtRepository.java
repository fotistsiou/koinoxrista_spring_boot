package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
}
