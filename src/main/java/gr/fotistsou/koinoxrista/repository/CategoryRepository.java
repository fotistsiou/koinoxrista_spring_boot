package gr.fotistsou.koinoxrista.repository;

import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findBySplitRuleOrderByNameAsc(SplitRule splitRule);
}
