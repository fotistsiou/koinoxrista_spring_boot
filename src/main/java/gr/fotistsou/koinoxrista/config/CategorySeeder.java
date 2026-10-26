package gr.fotistsou.koinoxrista.config;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(2)
@Component
public class CategorySeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);
    private final CategoryRepository categoryRepository;
    private final ApartmentRepository apartmentRepository;

    public CategorySeeder(
        CategoryRepository categoryRepository,
        ApartmentRepository apartmentRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.apartmentRepository = apartmentRepository;
    }

    private Apartment findApartment(String name) {
        return apartmentRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Apartment not found: " + name));
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            Apartment groundFloor = findApartment("Ισόγειο");
            Apartment firstFloor = findApartment("1ος");
            Apartment secondFloor = findApartment("2ος");

            Category electricity = new Category();
            electricity.setName("Κοινόχρηστο Ρεύμα");
            electricity.setSplitRule(SplitRule.EQUAL_SPLIT);
            electricity.getApartments().addAll(List.of(groundFloor, firstFloor));

            Category gas = new Category();
            gas.setName("Φυσικό Αέριο");
            gas.setSplitRule(SplitRule.METER_BASED);
            gas.getApartments().addAll(List.of(groundFloor, firstFloor));

            Category boiler = new Category();
            boiler.setName("Λέβητας – Βλάβες & Συντήρηση");
            boiler.setSplitRule(SplitRule.EQUAL_SPLIT);
            boiler.getApartments().addAll(List.of(groundFloor, firstFloor));

            Category disinfection = new Category();
            disinfection.setName("Απολύμανση");
            disinfection.setSplitRule(SplitRule.EQUAL_SPLIT);
            disinfection.getApartments().addAll(List.of(groundFloor, firstFloor, secondFloor));

            List<Category> saved = categoryRepository.saveAll(List.of(electricity, gas, boiler, disinfection));

            log.info("Seeded categories: {}", saved);
        }
    }
}
