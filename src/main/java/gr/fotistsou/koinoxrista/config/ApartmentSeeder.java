package gr.fotistsou.koinoxrista.config;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApartmentSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ApartmentSeeder.class);
    private final ApartmentRepository apartmentRepository;

    public ApartmentSeeder(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (apartmentRepository.count() == 0) {
            Apartment apartment1 = new Apartment();
            Apartment apartment2 = new Apartment();
            Apartment apartment3 = new Apartment();
            apartment1.setName("Ισόγειο");
            apartment2.setName("1ος");
            apartment3.setName("2ος");
            List<Apartment> saved = apartmentRepository.saveAll(List.of(apartment1, apartment2, apartment3));
            log.info("Seeded apartments: {}", saved);
        }
    }
}
