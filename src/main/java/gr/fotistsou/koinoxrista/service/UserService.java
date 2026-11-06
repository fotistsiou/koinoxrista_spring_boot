package gr.fotistsou.koinoxrista.service;

import gr.fotistsou.koinoxrista.dto.ResidentForm;
import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.AppUser;
import gr.fotistsou.koinoxrista.enums.Role;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class UserService {

    // Χωρίς χαρακτήρες που μπερδεύονται: 0/O, 1/l/I
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 12;

    private final AppUserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public UserService(AppUserRepository userRepository,
                       ApartmentRepository apartmentRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public String createResident(ResidentForm form) {
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }
        Apartment apartment = apartmentRepository.findById(form.getApartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Apartment not found: " + form.getApartmentId()));
        if (userRepository.existsByApartmentId(apartment.getId())) {
            throw new IllegalArgumentException("Apartment already has a user: " + apartment.getId());
        }

        String rawPassword = generatePassword();

        AppUser user = new AppUser();            // προσάρμοσε σε constructor/setters του AppUser σου
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.RESIDENT);
        user.setApartment(apartment);
        userRepository.save(user);

        return rawPassword;
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
