package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findBySplitRuleOrderByNameAsc(SplitRule splitRule);

    @EntityGraph(attributePaths = "apartments")
    List<Category> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "apartments")
    Optional<Category> findWithApartmentsById(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
