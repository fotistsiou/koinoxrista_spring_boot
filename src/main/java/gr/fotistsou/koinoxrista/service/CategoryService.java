package gr.fotistsou.koinoxrista.service;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.Category;
import gr.fotistsou.koinoxrista.enums.SplitRule;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ApartmentRepository apartmentRepository;

    public CategoryService(CategoryRepository categoryRepository, ApartmentRepository apartmentRepository) {
        this.categoryRepository = categoryRepository;
        this.apartmentRepository = apartmentRepository;
    }

    @Transactional
    public Category save(Long id, String name, SplitRule splitRule, Set<Long> apartmentIds) {
        if (splitRule == SplitRule.METER_BASED && apartmentIds.size() != 2) {
            throw new IllegalArgumentException("A meter-based category needs exactly 2 apartments");
        }

        List<Apartment> apartments = apartmentRepository.findAllById(apartmentIds);
        if (apartments.size() != apartmentIds.size()) {
            throw new IllegalArgumentException("Unknown apartment id in " + apartmentIds);
        }

        Category category = id == null
                ? new Category()
                : categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        category.setName(name);
        category.setSplitRule(splitRule);
        category.getApartments().clear();
        category.getApartments().addAll(apartments);

        return categoryRepository.save(category);
    }
}