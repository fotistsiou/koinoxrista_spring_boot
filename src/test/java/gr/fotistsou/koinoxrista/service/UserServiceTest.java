package gr.fotistsou.koinoxrista.service;

import gr.fotistsou.koinoxrista.dto.ResidentForm;
import gr.fotistsou.koinoxrista.entity.Apartment;
import gr.fotistsou.koinoxrista.entity.AppUser;
import gr.fotistsou.koinoxrista.enums.Role;
import gr.fotistsou.koinoxrista.repository.ApartmentRepository;
import gr.fotistsou.koinoxrista.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    AppUserRepository userRepository;

    @Mock
    ApartmentRepository apartmentRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    void createResident_savesNormalizedEmailAndEncodedPassword() {
        Apartment apartment = new Apartment();
        when(apartmentRepository.findById(2L)).thenReturn(Optional.of(apartment));
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        String raw = userService.createResident(form("  Maria@Example.GR ", 2L));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("maria@example.gr");
        assertThat(saved.getPassword()).isEqualTo("HASH");
        assertThat(saved.getRole()).isEqualTo(Role.RESIDENT);
        assertThat(saved.getApartment()).isSameAs(apartment);
        assertThat(raw).hasSize(12).isNotEqualTo("HASH");
        verify(passwordEncoder).encode(raw);
    }

    @Test
    void createResident_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("maria@example.gr")).thenReturn(true);

        assertThatThrownBy(() -> userService.createResident(form("Maria@example.gr", 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createResident_rejectsApartmentThatAlreadyHasUser() {
        when(apartmentRepository.findById(2L)).thenReturn(Optional.of(new Apartment()));
        when(userRepository.existsByApartmentId(any())).thenReturn(true);

        assertThatThrownBy(() -> userService.createResident(form("maria@example.gr", 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a user");

        verify(userRepository, never()).save(any());
    }

    private static ResidentForm form(String email, Long apartmentId) {
        ResidentForm form = new ResidentForm();
        form.setFirstName("Μαρία");
        form.setLastName("Παπαδοπούλου");
        form.setEmail(email);
        form.setApartmentId(apartmentId);
        return form;
    }
}