package gr.fotistsou.koinoxrista;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIT extends IntegrationTestBase {

    @Test
    void anonymousUserIsRedirectedToLogin() {
        assertThat(mvc.get().uri("/my"))
                .hasStatus(HttpStatus.FOUND)
                .redirectedUrl().endsWith("/login");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/apartments", "/admin/bills", "/admin/categories", "/admin/users"})
    @WithMockUser // ROLE_USER: any non-ADMIN role must be rejected
    void nonAdminIsForbiddenOnAdminPages(String url) {
        assertThat(mvc.get().uri(url)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/apartments",
            "/admin/bills", "/admin/bills/new",
            "/admin/gas-bills/new",
            "/admin/payments/new",
            "/admin/categories", "/admin/categories/new",
            "/admin/users", "/admin/users/new"
    })
    @WithMockUser(roles = "ADMIN")
    void adminPagesRender(String url) {
        assertThat(mvc.get().uri(url)).hasStatusOk();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postWithoutCsrfTokenIsForbidden() {
        assertThat(mvc.post().uri("/admin/categories")).hasStatus(HttpStatus.FORBIDDEN);
    }
}