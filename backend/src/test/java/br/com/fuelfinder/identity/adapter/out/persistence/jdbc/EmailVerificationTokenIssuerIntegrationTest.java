package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.LocalRegistrationPersistenceAdapter;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.application.port.in.PrepareEmailVerificationUseCase;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;
import br.com.fuelfinder.identity.application.port.out.EmailVerificationTokenIssuer;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EmailVerificationTokenIssuerIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("fuelfinder_token_test")
                    .withUsername("fuelfinder_test")
                    .withPassword("integration-test-only");

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );
        registry.add("spring.flyway.enabled", () -> true);
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    private LocalRegistrationPersistenceAdapter registration;

    @Autowired
    private PrepareEmailVerificationUseCase preparation;

    @Autowired
    private VerifyEmailUseCase verification;

    @Autowired
    private EmailVerificationTokenIssuer issuer;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Sha256VerificationTokenDigester digester =
            new Sha256VerificationTokenDigester();

    @Test
    void shouldPersistOnlyDigestAndActivateUser() {
        User user = createPendingUser();

        PreparedVerificationEmail prepared =
                preparation.execute(user.getId()).orElseThrow();

        String expectedDigest =
                digester.digest(prepared.token()).value();

        String storedDigest = jdbc.queryForObject(
                """
                SELECT digest
                FROM identity_email_verification_tokens
                WHERE user_id = ?
                """,
                String.class,
                user.getId().value()
        );

        assertEquals(expectedDigest, storedDigest);
        assertNotEquals(prepared.token(), storedDigest);

        assertEquals(
                "PENDING_VERIFICATION",
                statusOf(user)
        );

        verification.execute(
                new VerifyEmailCommand(prepared.token())
        );

        assertEquals("ACTIVE", statusOf(user));
        assertEquals(0L, countPendingTokens(user));
    }

    @Test
    void shouldPreserveTokensUntilVerification() {
        User user = createPendingUser();

        PreparedVerificationEmail first =
                preparation.execute(user.getId()).orElseThrow();

        PreparedVerificationEmail second =
                preparation.execute(user.getId()).orElseThrow();

        assertNotEquals(first.token(), second.token());
        assertEquals(2L, countPendingTokens(user));

        verification.execute(
                new VerifyEmailCommand(first.token())
        );

        assertEquals("ACTIVE", statusOf(user));
        assertEquals(0L, countPendingTokens(user));
    }

    @Test
    void shouldNotIssueTokenForActiveUser() {
        User user = createPendingUser();

        PreparedVerificationEmail prepared =
                preparation.execute(user.getId()).orElseThrow();

        verification.execute(
                new VerifyEmailCommand(prepared.token())
        );

        Instant now = Instant.now();

        boolean issued = issuer.issueIfPending(
                user.getId(),
                digester.digest("another-token-" + UUID.randomUUID()),
                now,
                now.plusSeconds(3600)
        );

        assertFalse(issued);
        assertTrue(preparation.execute(user.getId()).isEmpty());
        assertEquals("ACTIVE", statusOf(user));
    }

    @Test
    void shouldRejectInvalidExpiration() {
        User user = createPendingUser();
        Instant now = Instant.now();

        InvalidDataAccessApiUsageException exception =
                assertThrows(
                        InvalidDataAccessApiUsageException.class,
                        () -> issuer.issueIfPending(
                                user.getId(),
                                digester.digest(
                                        "test-" + UUID.randomUUID()
                                ),
                                now,
                                now
                        )
                );

        assertInstanceOf(
                IllegalArgumentException.class,
                exception.getCause()
        );

        assertEquals(
                "Expiracao deve ser posterior a emissao.",
                exception.getCause().getMessage()
        );

        assertEquals(0L, countPendingTokens(user));
    }

    @Test
    void shouldNotPrepareTokenForUnknownUser() {
        assertTrue(
                preparation.execute(UserId.newId()).isEmpty()
        );
    }

    @Test
    void shouldRejectDuplicateDigest() {
        User user = createPendingUser();
        Instant now = Instant.now();

        var digest = digester.digest(
                "collision-test-" + UUID.randomUUID()
        );

        assertTrue(
                issuer.issueIfPending(
                        user.getId(),
                        digest,
                        now,
                        now.plusSeconds(3600)
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> issuer.issueIfPending(
                        user.getId(),
                        digest,
                        now,
                        now.plusSeconds(3600)
                )
        );

        assertEquals(1L, countPendingTokens(user));
    }

    @Test
    void shouldWaitForConcurrentActivationBeforeIssuing()
            throws Exception {
        User user = createPendingUser();
        Instant now = Instant.now();

        TransactionTemplate activation =
                new TransactionTemplate(transactionManager);

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            // Simula a ativacao: segura a linha do usuario
            // e so faz commit apos liberacao.
            Future<?> activating = executor.submit(() ->
                    activation.executeWithoutResult(status -> {
                        jdbc.queryForObject(
                                """
                                SELECT status
                                FROM identity_users
                                WHERE id = ?
                                FOR UPDATE
                                """,
                                String.class,
                                user.getId().value()
                        );

                        locked.countDown();
                        await(release);

                        jdbc.update(
                                """
                                UPDATE identity_users
                                SET status = 'ACTIVE',
                                    updated_at = ?
                                WHERE id = ?
                                """,
                                java.sql.Timestamp.from(now),
                                user.getId().value()
                        );
                    })
            );

            assertTrue(locked.await(10, TimeUnit.SECONDS));

            Future<Boolean> issuing = executor.submit(() ->
                    issuer.issueIfPending(
                            user.getId(),
                            digester.digest(
                                    "race-" + UUID.randomUUID()
                            ),
                            now,
                            now.plusSeconds(3600)
                    )
            );

            // A emissao deve aguardar o bloqueio da ativacao.
            assertThrows(
                    TimeoutException.class,
                    () -> issuing.get(500, TimeUnit.MILLISECONDS)
            );

            release.countDown();
            activating.get(10, TimeUnit.SECONDS);

            assertFalse(issuing.get(10, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        assertEquals("ACTIVE", statusOf(user));
        assertEquals(0L, countPendingTokens(user));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Tempo de espera esgotado."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private User createPendingUser() {
        User user = User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of(
                        "issuer-" + UUID.randomUUID() + "@example.com"
                ),
                Instant.now().minusSeconds(60)
        );

        registration.register(
                user,
                new PasswordHash("integration-test-hash")
        );

        return user;
    }

    private String statusOf(User user) {
        return jdbc.queryForObject(
                """
                SELECT status
                FROM identity_users
                WHERE id = ?
                """,
                String.class,
                user.getId().value()
        );
    }

    private long countPendingTokens(User user) {
        Long count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM identity_email_verification_tokens
                WHERE user_id = ?
                  AND consumed_at IS NULL
                """,
                Long.class,
                user.getId().value()
        );

        return count == null ? 0L : count;
    }
}
