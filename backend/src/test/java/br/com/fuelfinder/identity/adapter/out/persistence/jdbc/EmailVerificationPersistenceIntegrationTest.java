package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.LocalRegistrationPersistenceAdapter;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;
import br.com.fuelfinder.identity.application.exception.InvalidEmailVerificationTokenException;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EmailVerificationPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("fuelfinder_verify_test")
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
    private VerifyEmailUseCase verifyEmail;

    @Autowired
    private JdbcTemplate jdbc;

    private final Sha256VerificationTokenDigester digester =
            new Sha256VerificationTokenDigester();

    @Test
    void shouldActivateUserAndConsumeToken() {
        User user = newUser();
        String token = "token-" + UUID.randomUUID();

        insertToken(
                user,
                token,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600)
        );

        verifyEmail.execute(new VerifyEmailCommand(token));

        assertEquals("ACTIVE", statusOf(user));

        Long consumed = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM identity_email_verification_tokens
                WHERE user_id = ?
                  AND consumed_at IS NOT NULL
                """,
                Long.class,
                user.getId().value()
        );

        assertEquals(1L, consumed);
    }

    @Test
    void shouldRejectExpiredToken() {
        User user = newUser();
        String token = "expired-" + UUID.randomUUID();

        insertToken(
                user,
                token,
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600)
        );

        assertThrows(
                InvalidEmailVerificationTokenException.class,
                () -> verifyEmail.execute(
                        new VerifyEmailCommand(token)
                )
        );

        assertEquals(
                "PENDING_VERIFICATION",
                statusOf(user)
        );
    }

    @Test
    void shouldRejectTokenReuse() {
        User user = newUser();
        String token = "reusable-" + UUID.randomUUID();

        insertToken(
                user,
                token,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600)
        );

        verifyEmail.execute(new VerifyEmailCommand(token));

        assertThrows(
                InvalidEmailVerificationTokenException.class,
                () -> verifyEmail.execute(
                        new VerifyEmailCommand(token)
                )
        );

        assertEquals("ACTIVE", statusOf(user));
    }

    @Test
    void shouldInvalidateOtherTokensAfterActivation() {
        User user = newUser();
        String first = "first-" + UUID.randomUUID();
        String second = "second-" + UUID.randomUUID();

        Instant issuedAt = Instant.now().minusSeconds(60);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        insertToken(user, first, issuedAt, expiresAt);
        insertToken(user, second, issuedAt, expiresAt);

        verifyEmail.execute(new VerifyEmailCommand(first));

        Long pending = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM identity_email_verification_tokens
                WHERE user_id = ?
                  AND consumed_at IS NULL
                """,
                Long.class,
                user.getId().value()
        );

        assertEquals(0L, pending);

        assertThrows(
                InvalidEmailVerificationTokenException.class,
                () -> verifyEmail.execute(
                        new VerifyEmailCommand(second)
                )
        );
    }

    private User newUser() {
        User user = User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of(
                        "verify-" + UUID.randomUUID() + "@example.com"
                ),
                Instant.now().minusSeconds(180)
        );

        registration.register(
                user,
                new PasswordHash("integration-test-hash")
        );

        return user;
    }

    private void insertToken(
            User user,
            String rawToken,
            Instant issuedAt,
            Instant expiresAt
    ) {
        jdbc.update(
                """
                INSERT INTO identity_email_verification_tokens
                    (digest, user_id, issued_at, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                digester.digest(rawToken).value(),
                user.getId().value(),
                Timestamp.from(issuedAt),
                Timestamp.from(expiresAt)
        );
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
}
