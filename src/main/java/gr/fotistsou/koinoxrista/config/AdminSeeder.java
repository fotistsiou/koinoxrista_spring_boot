package gr.fotistsou.koinoxrista.config;

import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.AppUser;
import gr.fotistsou.koinoxrista.enums.Role;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Order(3)
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppUserRepository appUserRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AdminSeeder(AppUserRepository appUserRepository,
                       ApartmentRepository apartmentRepository,
                       PasswordEncoder passwordEncoder,
                       AdminProperties adminProperties) {
        this.appUserRepository = appUserRepository;
        this.apartmentRepository = apartmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        if (isBlank(adminProperties.email()) || isBlank(adminProperties.password())) {
            throw new IllegalStateException("app.admin.email and app.admin.password must be set");
        }

        Apartment firstFloor = apartmentRepository.findByName("1ος")
                .orElseThrow(() -> new IllegalStateException("Apartment not found: 1ος"));

        AppUser admin = new AppUser();
        admin.setFirstName("Φώτης");
        admin.setLastName("Τσιούμας");
        admin.setEmail(adminProperties.email());
        admin.setPassword(passwordEncoder.encode(adminProperties.password()));
        admin.setRole(Role.ADMIN);
        admin.setApartment(firstFloor);
        appUserRepository.save(admin);

        log.info("Seeded admin user: {}", admin.getEmail());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}