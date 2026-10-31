package br.com.fuelfinder.identity;

import br.com.fuelfinder.identity.adapter.out.email.SmtpVerificationEmailSender;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxProcessingResult;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.in.ProcessVerificationEmailOutboxUseCase;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserCommand;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailOutbox;
import br.com.fuelfinder.identity.application.service.DispatchVerificationEmailService;
import br.com.fuelfinder.identity.application.service.ProcessVerificationEmailOutboxService;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fluxo completo com PostgreSQL e SMTP reais:
 * cadastro -> outbox -> e-mail -> link -> conta ativa.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VerificationEmailFlowIntegrationTest {

    private static final String LINK_BASE =
            "http://localhost:3000/verificar-email";

    private static final Pattern MESSAGE_ID =
            Pattern.compile("\"ID\":\"([^\"]+)\"");

    private static final Pattern TOKEN =
            Pattern.compile(
                    Pattern.quote(LINK_BASE) + "\\?token=([A-Za-z0-9_-]+)"
            );

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("fuelfinder_email_flow_test")
                    .withUsername("fuelfinder_test")
                    .withPassword("integration-test-only");

    @Container
    static final GenericContainer<?> mailpit =
            new GenericContainer<>(
                    DockerImageName.parse("axllent/mailpit:v1.27.10")
            ).withExposedPorts(1025, 8025);

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

        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
        registry.add(
                "app.identity.verification-email.link-base-url",
                () -> LINK_BASE
        );
    }

    private final HttpClient http = HttpClient.newHttpClient();

    private final Sha256VerificationTokenDigester digester =
            new Sha256VerificationTokenDigester();

    @Autowired
    private RegisterLocalUserUseCase registration;

    @Autowired
    private ProcessVerificationEmailOutboxUseCase processing;

    @Autowired
    private VerifyEmailUseCase verification;

    @Autowired
    private PrepareEmailVerificationUseCase preparation;

    @Autowired
    private VerificationEmailOutbox outbox;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // Cada teste processa somente o proprio evento.
        jdbc.update("DELETE FROM identity_verification_email_outbox");
    }

    @Test
    void shouldDeliverVerificationEmailAndActivateAccount()
            throws Exception {
        String email = uniqueEmail();
        UserId userId = register(email);

        assertEquals(
                VerificationEmailOutboxProcessingResult.DISPATCH_REQUESTED,
                processing.executeNext()
        );
        assertEquals("COMPLETED", outboxStatusOf(userId));
        assertEquals("PENDING_VERIFICATION", userStatusOf(userId));

        String message = fetchMessageTo(email);

        assertTrue(message.contains(
                "\"Subject\":\"Confirme seu e-mail no FuelFinder\""
        ));

        String token = extractToken(message);

        // O banco guarda apenas o digest do token enviado.
        assertEquals(
                digester.digest(token).value(),
                jdbc.queryForObject(
                        """
                        SELECT digest
                        FROM identity_email_verification_tokens
                        WHERE user_id = ?
                        """,
                        String.class,
                        userId.value()
                )
        );

        verification.execute(new VerifyEmailCommand(token));

        assertEquals("ACTIVE", userStatusOf(userId));
        assertEquals(
                VerificationEmailOutboxProcessingResult.NO_EVENT,
                processing.executeNext()
        );
    }

    @Test
    void shouldScheduleRetryWhenSmtpIsUnavailable() throws Exception {
        String email = uniqueEmail();
        UserId userId = register(email);

        JavaMailSenderImpl unreachable = new JavaMailSenderImpl();
        unreachable.setHost("localhost");
        unreachable.setPort(closedPort());

        ProcessVerificationEmailOutboxUseCase offline =
                new ProcessVerificationEmailOutboxService(
                        outbox,
                        new DispatchVerificationEmailService(
                                preparation,
                                new SmtpVerificationEmailSender(
                                        unreachable,
                                        "no-reply@fuelfinder.local",
                                        LINK_BASE
                                )
                        ),
                        Clock.systemUTC()
                );

        assertEquals(
                VerificationEmailOutboxProcessingResult.RETRY_SCHEDULED,
                offline.executeNext()
        );
        assertEquals("PENDING", outboxStatusOf(userId));
        assertEquals("PENDING_VERIFICATION", userStatusOf(userId));
        assertEquals(0, countMessagesTo(email));
    }

    private UserId register(String email) {
        return registration.execute(
                new RegisterLocalUserCommand(
                        "Maria Silva",
                        Email.of(email),
                        "Senha2026"
                )
        );
    }

    private String fetchMessageTo(String email) throws Exception {
        for (int i = 0; i < 50; i++) {
            Matcher id = MESSAGE_ID.matcher(searchTo(email));

            if (id.find()) {
                return get("/api/v1/message/" + id.group(1));
            }

            Thread.sleep(100);
        }

        throw new AssertionError("E-mail nao chegou ao Mailpit.");
    }

    private int countMessagesTo(String email) throws Exception {
        return (int) MESSAGE_ID.matcher(searchTo(email))
                .results()
                .count();
    }

    private String searchTo(String email) throws Exception {
        return get(
                "/api/v1/search?query="
                        + URLEncoder.encode(
                                "to:" + email,
                                StandardCharsets.UTF_8
                        )
        );
    }

    private String get(String path) throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(
                        "http://" + mailpit.getHost() + ":"
                                + mailpit.getMappedPort(8025) + path
                )).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());

        return response.body();
    }

    private static String extractToken(String message) {
        Matcher matcher = TOKEN.matcher(message);

        assertTrue(matcher.find(), "Link de verificacao ausente.");

        return matcher.group(1);
    }

    private String outboxStatusOf(UserId userId) {
        return jdbc.queryForObject(
                """
                SELECT status
                FROM identity_verification_email_outbox
                WHERE user_id = ?
                """,
                String.class,
                userId.value()
        );
    }

    private String userStatusOf(UserId userId) {
        return jdbc.queryForObject(
                "SELECT status FROM identity_users WHERE id = ?",
                String.class,
                userId.value()
        );
    }

    private static String uniqueEmail() {
        return "flow-" + UUID.randomUUID() + "@example.com";
    }

    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
