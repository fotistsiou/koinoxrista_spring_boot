package gr.fotistsou.koinoxrista.config;

import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategorySeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);
    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            Category category1 = new Category();
            Category category2 = new Category();
            Category category3 = new Category();
            category1.setName("Κοινόχρηστο Ρεύμα");
            category2.setName("Φυσικό Αέριο");
            category3.setName("Απολύμανση");
            List<Category> saved = categoryRepository.saveAll(List.of(category1, category2, category3));
            log.info("Seeded categories: {}", saved);
        }
    }
}
