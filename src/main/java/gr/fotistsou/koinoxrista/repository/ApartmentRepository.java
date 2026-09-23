package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
}
