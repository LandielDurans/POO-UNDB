package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.LocalRegistrationPersistenceAdapter;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;
import br.com.fuelfinder.identity.application.model.ActiveSession;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SessionPersistenceIntegrationTest {

    private static final Duration TTL = Duration.ofDays(7);

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("fuelfinder_session_test")
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
    }

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private LocalRegistrationPersistenceAdapter registration;

    private final Sha256VerificationTokenDigester digester =
            new Sha256VerificationTokenDigester();

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Test
    void shouldRotateActiveSessionOnlyOnce() {
        UserId userId = createUser();
        UUID sessionId = UUID.randomUUID();
        VerificationTokenDigest first = digest();
        VerificationTokenDigest second = digest();

        sessions.create(sessionId, userId, first, now, now.plus(TTL));

        Optional<ActiveSession> rotated = sessions.rotate(
                first, second, now.plusSeconds(60), now.plus(TTL)
        );

        assertEquals(new ActiveSession(sessionId, userId), rotated.orElseThrow());
        assertTrue(sessions.rotate(
                first, digest(), now.plusSeconds(61), now.plus(TTL)
        ).isEmpty());
        assertTrue(sessions.isActive(sessionId, now.plusSeconds(62)));
    }

    @Test
    void shouldNotRotateRevokedOrExpiredSession() {
        UserId userId = createUser();

        VerificationTokenDigest revoked = digest();
        UUID revokedId = UUID.randomUUID();
        sessions.create(revokedId, userId, revoked, now, now.plus(TTL));
        assertTrue(sessions.revoke(revoked, now.plusSeconds(1)));
        assertFalse(sessions.revoke(revoked, now.plusSeconds(2)));

        assertTrue(sessions.rotate(
                revoked, digest(), now.plusSeconds(3), now.plus(TTL)
        ).isEmpty());
        assertFalse(sessions.isActive(revokedId, now.plusSeconds(3)));

        VerificationTokenDigest expiring = digest();
        UUID expiringId = UUID.randomUUID();
        sessions.create(expiringId, userId, expiring, now, now.plusSeconds(10));

        assertTrue(sessions.rotate(
                expiring, digest(), now.plusSeconds(10), now.plus(TTL)
        ).isEmpty());
        assertFalse(sessions.isActive(expiringId, now.plusSeconds(10)));
    }

    @Test
    void shouldRevokeEverySessionOfUser() {
        UserId userId = createUser();
        UserId otherUser = createUser();

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        sessions.create(a, userId, digest(), now, now.plus(TTL));
        sessions.create(b, userId, digest(), now, now.plus(TTL));
        sessions.create(other, otherUser, digest(), now, now.plus(TTL));

        assertEquals(2, sessions.revokeAllForUser(userId, now.plusSeconds(1)));

        assertFalse(sessions.isActive(a, now.plusSeconds(2)));
        assertFalse(sessions.isActive(b, now.plusSeconds(2)));
        assertTrue(sessions.isActive(other, now.plusSeconds(2)));
    }

    @Test
    void shouldRejectDuplicateDigest() {
        UserId userId = createUser();
        VerificationTokenDigest digest = digest();

        sessions.create(UUID.randomUUID(), userId, digest, now, now.plus(TTL));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> sessions.create(
                        UUID.randomUUID(), userId, digest, now, now.plus(TTL)
                )
        );
    }

    @Test
    void shouldLetOnlyOneConcurrentRefreshWin() throws Exception {
        UserId userId = createUser();
        VerificationTokenDigest current = digest();
        sessions.create(UUID.randomUUID(), userId, current, now, now.plus(TTL));

        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);

        try {
            List<Future<Optional<ActiveSession>>> results = new ArrayList<>();

            for (int i = 0; i < attempts; i++) {
                Callable<Optional<ActiveSession>> attempt = () -> {
                    start.await(10, TimeUnit.SECONDS);
                    return sessions.rotate(
                            current, digest(), now.plusSeconds(5), now.plus(TTL)
                    );
                };
                results.add(executor.submit(attempt));
            }

            start.countDown();

            long winners = 0;
            for (Future<Optional<ActiveSession>> result : results) {
                if (result.get(30, TimeUnit.SECONDS).isPresent()) {
                    winners++;
                }
            }

            assertEquals(1, winners);
        } finally {
            executor.shutdownNow();
        }
    }

    private VerificationTokenDigest digest() {
        return digester.digest(UUID.randomUUID().toString());
    }

    private UserId createUser() {
        User user = User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of("session-" + UUID.randomUUID() + "@example.com"),
                now.minusSeconds(60)
        );

        registration.register(user, new PasswordHash("integration-test-hash"));

        return user.getId();
    }
}
