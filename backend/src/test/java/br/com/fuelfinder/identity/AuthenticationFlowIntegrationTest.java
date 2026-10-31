package br.com.fuelfinder.identity;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminCommand;
import br.com.fuelfinder.identity.application.port.in.ProvisionSuperAdminUseCase;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserCommand;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Autenticacao de ponta a ponta via HTTP, com
 * PostgreSQL real e a configuracao de seguranca real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthenticationFlowIntegrationTest {

    private static final String PASSWORD = "Senha2026";
    private static final String SUPER_ADMIN_EMAIL = "root@fuelfinder.local";
    private static final String SUPER_ADMIN_PASSWORD = "SuperSenha2026";

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("fuelfinder_auth_test")
                    .withUsername("fuelfinder_test")
                    .withPassword("integration-test-only");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // Provisionado na subida do contexto.
        registry.add("app.identity.super-admin.email", () -> SUPER_ADMIN_EMAIL);
        registry.add(
                "app.identity.super-admin.password",
                () -> SUPER_ADMIN_PASSWORD
        );
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RegisterLocalUserUseCase registration;

    @Autowired
    private PrepareEmailVerificationUseCase preparation;

    @Autowired
    private VerifyEmailUseCase verification;

    @Autowired
    private ProvisionSuperAdminUseCase provisioning;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldLoginRefreshAndLogout() throws Exception {
        String email = activeUser();

        Tokens first = login(email, PASSWORD);

        me(first.access())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles", contains("USER")))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Tokens renewed = refresh(first.refresh())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(Tokens::of);

        assertNotEquals(first.refresh(), renewed.refresh());

        // O refresh token e de uso unico.
        refresh(first.refresh())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        me(renewed.access()).andExpect(status().isOk());

        logout(renewed.refresh()).andExpect(status().isNoContent());

        // Encerrada a sessao, ate o access token ainda nao
        // expirado deixa de valer.
        me(renewed.access())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        refresh(renewed.refresh()).andExpect(status().isUnauthorized());

        // Logout repetido continua 204.
        logout(renewed.refresh()).andExpect(status().isNoContent());
    }

    @Test
    void shouldNotRevealWhichCredentialFailed() throws Exception {
        String email = activeUser();

        loginRequest(email, "SenhaErrada1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        loginRequest("ninguem-" + UUID.randomUUID() + "@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRequireVerifiedEmail() throws Exception {
        String email = uniqueEmail();
        register(email);

        loginRequest(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        assertEquals(0, sessionsOf(email));
    }

    @Test
    void shouldLogoutEverywhere() throws Exception {
        String email = activeUser();

        Tokens laptop = login(email, PASSWORD);
        Tokens phone = login(email, PASSWORD);

        mvc.perform(post("/api/v1/auth/logout-all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(laptop.access())))
                .andExpect(status().isNoContent());

        me(laptop.access()).andExpect(status().isUnauthorized());
        me(phone.access()).andExpect(status().isUnauthorized());
        refresh(phone.refresh()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldEndSessionOfBlockedUser() throws Exception {
        String email = activeUser();
        Tokens tokens = login(email, PASSWORD);

        jdbc.update(
                "UPDATE identity_users SET status = 'BLOCKED' WHERE email = ?",
                email
        );

        refresh(tokens.refresh()).andExpect(status().isUnauthorized());

        // A renovacao recusada tambem encerra a sessao.
        me(tokens.access()).andExpect(status().isUnauthorized());

        loginRequest(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
    }

    @Test
    void shouldRejectMissingOrForgedToken() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        me("not-a-jwt").andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldProvisionSuperAdminOnceAtStartup() throws Exception {
        Tokens tokens = login(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);

        me(tokens.access())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder(
                        "USER", "ADMIN", "SUPER_ADMIN"
                )));

        // Subidas seguintes nao criam outra conta.
        assertFalse(provisioning.execute(new ProvisionSuperAdminCommand(
                "Outro",
                Email.of("outro-root@fuelfinder.local"),
                SUPER_ADMIN_PASSWORD
        )));
        assertEquals(1, jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM identity_user_roles
                WHERE role = 'SUPER_ADMIN'
                """,
                Integer.class
        ));
    }

    private String activeUser() {
        String email = uniqueEmail();
        UserId userId = register(email);

        PreparedVerificationEmail prepared =
                preparation.execute(userId).orElseThrow();
        verification.execute(new VerifyEmailCommand(prepared.token()));

        return email;
    }

    private UserId register(String email) {
        return registration.execute(new RegisterLocalUserCommand(
                "Maria Silva",
                Email.of(email),
                PASSWORD
        ));
    }

    private Tokens login(String email, String password) throws Exception {
        String body = loginRequest(email, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Tokens.of(body);
    }

    private ResultActions loginRequest(String email, String password)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken": "%s"}
                        """.formatted(refreshToken)));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken": "%s"}
                        """.formatted(refreshToken)));
    }

    private ResultActions me(String accessToken) throws Exception {
        return mvc.perform(get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)));
    }

    private int sessionsOf(String email) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM identity_sessions s
                JOIN identity_users u ON u.id = s.user_id
                WHERE u.email = ?
                """,
                Integer.class,
                email
        );
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "auth-" + UUID.randomUUID() + "@example.com";
    }

    private record Tokens(String access, String refresh) {

        static Tokens of(String json) {
            return new Tokens(
                    JsonPath.read(json, "$.accessToken"),
                    JsonPath.read(json, "$.refreshToken")
            );
        }
    }
}
