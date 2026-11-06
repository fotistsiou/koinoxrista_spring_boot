package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByApartmentId(Long apartmentId);

    @EntityGraph(attributePaths = "apartment")
    List<AppUser> findAllByOrderByApartmentIdAsc();
}