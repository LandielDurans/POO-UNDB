package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.adapter.out.persistence.jpa.LocalRegistrationPersistenceAdapter;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxClaim;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxProcessingResult;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxResult;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailOutbox;
import br.com.fuelfinder.identity.application.service.ProcessVerificationEmailOutboxService;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VerificationEmailOutboxPersistenceIntegrationTest {

    private static final Duration LEASE = Duration.ofMinutes(5);

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("fuelfinder_outbox_test")
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
    private VerificationEmailOutbox outbox;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // Sempre posterior ao created_at real dos eventos.
    private Instant now;

    @BeforeEach
    void setUp() {
        // Cada teste enxerga somente os proprios eventos.
        jdbc.update("DELETE FROM identity_verification_email_outbox");

        now = Instant.now()
                .plus(1, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void shouldClaimPendingEventWithNewLease() {
        UserId userId = createPendingUser();
        UUID eventId = eventOf(userId);

        VerificationEmailOutboxClaim claim =
                outbox.claimNext(now, LEASE).orElseThrow();

        assertEquals(eventId, claim.eventId());
        assertEquals(userId, claim.userId());
        assertEquals(1, claim.attemptNumber());
        assertEquals(now.plus(LEASE), claim.leaseExpiresAt());

        assertEquals("PROCESSING", statusOf(eventId));
        assertEquals(1, attemptsOf(eventId));
        assertEquals(
                claim.leaseId(),
                jdbc.queryForObject(
                        """
                        SELECT lease_id
                        FROM identity_verification_email_outbox
                        WHERE event_id = ?
                        """,
                        UUID.class,
                        eventId
                )
        );
        assertEquals(
                now.plus(LEASE),
                instantOf(eventId, "lease_expires_at")
        );
    }

    @Test
    void shouldReturnEmptyWhenNoEventIsEligible() {
        assertTrue(outbox.claimNext(now, LEASE).isEmpty());

        UUID eventId = eventOf(createPendingUser());

        // next_attempt_at ainda no futuro.
        jdbc.update(
                """
                UPDATE identity_verification_email_outbox
                SET next_attempt_at = ?
                WHERE event_id = ?
                """,
                Timestamp.from(now.plusSeconds(1)),
                eventId
        );

        assertTrue(outbox.claimNext(now, LEASE).isEmpty());
        assertEquals("PENDING", statusOf(eventId));
        assertEquals(0, attemptsOf(eventId));
    }

    @Test
    void shouldNotClaimEventWithActiveLease() {
        UUID eventId = eventOf(createPendingUser());

        outbox.claimNext(now, LEASE).orElseThrow();

        assertTrue(
                outbox.claimNext(
                        now.plus(LEASE).minusSeconds(1),
                        LEASE
                ).isEmpty()
        );
        assertEquals(1, attemptsOf(eventId));
    }

    @Test
    void shouldRejectNonPositiveLeaseDuration() {
        InvalidDataAccessApiUsageException exception =
                assertThrows(
                        InvalidDataAccessApiUsageException.class,
                        () -> outbox.claimNext(now, Duration.ZERO)
                );

        assertInstanceOf(
                IllegalArgumentException.class,
                exception.getCause()
        );
    }

    @Test
    void shouldCompleteEventWithValidLease() {
        UUID eventId = eventOf(createPendingUser());

        VerificationEmailOutboxClaim claim =
                outbox.claimNext(now, LEASE).orElseThrow();

        Instant completedAt = now.plusSeconds(30);

        assertTrue(
                outbox.complete(
                        eventId,
                        claim.leaseId(),
                        VerificationEmailOutboxResult.DISPATCH_REQUESTED,
                        completedAt
                )
        );

        assertEquals("COMPLETED", statusOf(eventId));
        assertEquals(
                "DISPATCH_REQUESTED",
                columnOf(eventId, "processing_result")
        );
        assertEquals(completedAt, instantOf(eventId, "completed_at"));
        assertLeaseReleased(eventId);

        // Concluido nao volta a ser reservado.
        assertTrue(
                outbox.claimNext(
                        completedAt.plus(Duration.ofDays(1)),
                        LEASE
                ).isEmpty()
        );
    }

    @Test
    void shouldRejectCompletionWithInvalidLease() {
        UUID eventId = eventOf(createPendingUser());

        VerificationEmailOutboxClaim claim =
                outbox.claimNext(now, LEASE).orElseThrow();

        Instant at = now.plusSeconds(30);

        // Lease de outra reserva.
        assertFalse(
                outbox.complete(
                        eventId,
                        UUID.randomUUID(),
                        VerificationEmailOutboxResult.NO_ACTION_REQUIRED,
                        at
                )
        );

        // Lease expirado.
        assertFalse(
                outbox.complete(
                        eventId,
                        claim.leaseId(),
                        VerificationEmailOutboxResult.NO_ACTION_REQUIRED,
                        claim.leaseExpiresAt()
                )
        );

        // Evento inexistente.
        assertFalse(
                outbox.complete(
                        UUID.randomUUID(),
                        claim.leaseId(),
                        VerificationEmailOutboxResult.NO_ACTION_REQUIRED,
                        at
                )
        );

        assertEquals("PROCESSING", statusOf(eventId));

        assertTrue(
                outbox.complete(
                        eventId,
                        claim.leaseId(),
                        VerificationEmailOutboxResult.NO_ACTION_REQUIRED,
                        at
                )
        );

        // Segunda conclusao com o mesmo lease.
        assertFalse(
                outbox.complete(
                        eventId,
                        claim.leaseId(),
                        VerificationEmailOutboxResult.DISPATCH_REQUESTED,
                        at.plusSeconds(1)
                )
        );

        assertEquals(
                "NO_ACTION_REQUIRED",
                columnOf(eventId, "processing_result")
        );
    }

    @Test
    void shouldScheduleRetryAndReleaseLease() {
        UUID eventId = eventOf(createPendingUser());

        VerificationEmailOutboxClaim first =
                outbox.claimNext(now, LEASE).orElseThrow();

        Instant failedAt = now.plusSeconds(10);
        Instant nextAttemptAt = failedAt.plus(Duration.ofMinutes(1));

        assertTrue(
                outbox.scheduleRetry(
                        eventId,
                        first.leaseId(),
                        failedAt,
                        nextAttemptAt
                )
        );

        assertEquals("PENDING", statusOf(eventId));
        assertEquals(
                nextAttemptAt,
                instantOf(eventId, "next_attempt_at")
        );
        assertLeaseReleased(eventId);

        // O lease liberado nao altera mais o evento.
        assertFalse(
                outbox.complete(
                        eventId,
                        first.leaseId(),
                        VerificationEmailOutboxResult.DISPATCH_REQUESTED,
                        failedAt.plusSeconds(1)
                )
        );

        assertTrue(
                outbox.claimNext(
                        nextAttemptAt.minusMillis(1),
                        LEASE
                ).isEmpty()
        );

        VerificationEmailOutboxClaim second =
                outbox.claimNext(nextAttemptAt, LEASE).orElseThrow();

        assertEquals(eventId, second.eventId());
        assertEquals(2, second.attemptNumber());
        assertNotEquals(first.leaseId(), second.leaseId());
    }

    @Test
    void shouldRejectRetryNotAfterFailure() {
        UUID eventId = eventOf(createPendingUser());

        VerificationEmailOutboxClaim claim =
                outbox.claimNext(now, LEASE).orElseThrow();

        InvalidDataAccessApiUsageException exception =
                assertThrows(
                        InvalidDataAccessApiUsageException.class,
                        () -> outbox.scheduleRetry(
                                eventId,
                                claim.leaseId(),
                                now,
                                now
                        )
                );

        assertInstanceOf(
                IllegalArgumentException.class,
                exception.getCause()
        );
        assertEquals("PROCESSING", statusOf(eventId));
    }

    @Test
    void shouldRejectRetryWithExpiredLease() {
        UUID eventId = eventOf(createPendingUser());

        VerificationEmailOutboxClaim claim =
                outbox.claimNext(now, LEASE).orElseThrow();

        Instant failedAt = claim.leaseExpiresAt();

        assertFalse(
                outbox.scheduleRetry(
                        eventId,
                        claim.leaseId(),
                        failedAt,
                        failedAt.plusSeconds(60)
                )
        );
        assertEquals("PROCESSING", statusOf(eventId));
    }

    @Test
    void shouldMarkEventExhaustedAndKeepIt() {
        UUID eventId = eventOf(createPendingUser());

        VerificationEmailOutboxClaim claim =
                outbox.claimNext(now, LEASE).orElseThrow();

        Instant failedAt = now.plusSeconds(10);

        assertFalse(
                outbox.markExhausted(
                        eventId,
                        UUID.randomUUID(),
                        failedAt
                )
        );

        assertTrue(
                outbox.markExhausted(
                        eventId,
                        claim.leaseId(),
                        failedAt
                )
        );

        assertEquals("EXHAUSTED", statusOf(eventId));
        assertEquals(failedAt, instantOf(eventId, "exhausted_at"));
        assertLeaseReleased(eventId);

        assertFalse(
                outbox.markExhausted(
                        eventId,
                        claim.leaseId(),
                        failedAt.plusSeconds(1)
                )
        );

        assertTrue(
                outbox.claimNext(
                        failedAt.plus(Duration.ofDays(1)),
                        LEASE
                ).isEmpty()
        );
    }

    @Test
    void shouldRecoverEventWithExpiredLease() {
        UUID eventId = eventOf(createPendingUser());

        VerificationEmailOutboxClaim crashed =
                outbox.claimNext(now, LEASE).orElseThrow();

        // O worker caiu sem concluir a reserva.
        VerificationEmailOutboxClaim recovered =
                outbox.claimNext(crashed.leaseExpiresAt(), LEASE)
                        .orElseThrow();

        assertEquals(eventId, recovered.eventId());
        assertEquals(2, recovered.attemptNumber());
        assertNotEquals(crashed.leaseId(), recovered.leaseId());
        assertEquals(
                crashed.leaseExpiresAt().plus(LEASE),
                recovered.leaseExpiresAt()
        );

        Instant at = crashed.leaseExpiresAt().plusSeconds(1);

        // O worker antigo perdeu a reserva.
        assertFalse(
                outbox.complete(
                        eventId,
                        crashed.leaseId(),
                        VerificationEmailOutboxResult.DISPATCH_REQUESTED,
                        at
                )
        );

        assertTrue(
                outbox.complete(
                        eventId,
                        recovered.leaseId(),
                        VerificationEmailOutboxResult.DISPATCH_REQUESTED,
                        at
                )
        );
        assertEquals("COMPLETED", statusOf(eventId));
    }

    @Test
    void shouldSkipEventLockedByAnotherWorker() throws Exception {
        UUID first = eventOf(createPendingUser());
        UUID second = eventOf(createPendingUser());

        TransactionTemplate workerA =
                new TransactionTemplate(transactionManager);

        CountDownLatch claimed = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Worker A reserva e segura a transacao aberta.
            Future<VerificationEmailOutboxClaim> claimA =
                    executor.submit(() -> workerA.execute(status -> {
                        VerificationEmailOutboxClaim claim =
                                outbox.claimNext(now, LEASE)
                                        .orElseThrow();

                        claimed.countDown();
                        await(release);

                        return claim;
                    }));

            assertTrue(claimed.await(10, TimeUnit.SECONDS));

            // Worker B nao espera o bloqueio de A.
            Future<Optional<VerificationEmailOutboxClaim>> claimB =
                    executor.submit(() -> outbox.claimNext(now, LEASE));

            VerificationEmailOutboxClaim b =
                    claimB.get(5, TimeUnit.SECONDS).orElseThrow();

            assertTrue(
                    executor.submit(() -> outbox.claimNext(now, LEASE))
                            .get(5, TimeUnit.SECONDS)
                            .isEmpty()
            );

            release.countDown();
            VerificationEmailOutboxClaim a =
                    claimA.get(10, TimeUnit.SECONDS);

            assertNotEquals(a.eventId(), b.eventId());
            assertEquals(
                    new HashSet<>(List.of(first, second)),
                    new HashSet<>(List.of(a.eventId(), b.eventId()))
            );
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        assertEquals("PROCESSING", statusOf(first));
        assertEquals("PROCESSING", statusOf(second));
        assertTrue(outbox.claimNext(now, LEASE).isEmpty());
    }

    @Test
    void shouldNotClaimSameEventTwiceUnderConcurrency()
            throws Exception {
        int events = 20;
        int workers = 4;

        List<UUID> created = new ArrayList<>();

        for (int i = 0; i < events; i++) {
            created.add(eventOf(createPendingUser()));
        }

        Queue<UUID> claimed = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(workers);

        try {
            List<Future<?>> running = new ArrayList<>();

            for (int i = 0; i < workers; i++) {
                running.add(executor.submit(() -> {
                    await(start);

                    Optional<VerificationEmailOutboxClaim> claim;

                    while ((claim = outbox.claimNext(now, LEASE))
                            .isPresent()) {
                        claimed.add(claim.get().eventId());
                    }
                }));
            }

            start.countDown();

            for (Future<?> worker : running) {
                worker.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(events, claimed.size());
        assertEquals(new HashSet<>(created), new HashSet<>(claimed));

        for (UUID eventId : created) {
            assertEquals("PROCESSING", statusOf(eventId));
            assertEquals(1, attemptsOf(eventId));
        }
    }

    @Test
    void shouldCompleteEventThroughProcessingService() {
        UserId userId = createPendingUser();
        UUID eventId = eventOf(userId);

        List<UserId> dispatched = new ArrayList<>();

        ProcessVerificationEmailOutboxService service =
                new ProcessVerificationEmailOutboxService(
                        outbox,
                        user -> dispatched.add(user),
                        new MutableClock(now)
                );

        assertEquals(
                VerificationEmailOutboxProcessingResult.DISPATCH_REQUESTED,
                service.executeNext()
        );
        assertEquals(List.of(userId), dispatched);
        assertEquals("COMPLETED", statusOf(eventId));

        assertEquals(
                VerificationEmailOutboxProcessingResult.NO_EVENT,
                service.executeNext()
        );
    }

    @Test
    void shouldRetryWithBackoffUntilExhausted() {
        UUID eventId = eventOf(createPendingUser());

        MutableClock clock = new MutableClock(now);
        AtomicInteger calls = new AtomicInteger();

        ProcessVerificationEmailOutboxService service =
                new ProcessVerificationEmailOutboxService(
                        outbox,
                        user -> {
                            calls.incrementAndGet();
                            throw new IllegalStateException(
                                    "SMTP indisponivel"
                            );
                        },
                        clock
                );

        for (int attempt = 1; attempt <= 4; attempt++) {
            Instant failedAt = clock.instant();

            assertEquals(
                    VerificationEmailOutboxProcessingResult.RETRY_SCHEDULED,
                    service.executeNext()
            );
            assertEquals("PENDING", statusOf(eventId));
            assertEquals(attempt, attemptsOf(eventId));

            Instant nextAttemptAt = failedAt.plus(
                    Duration.ofMinutes(1L << (attempt - 1))
            );

            assertEquals(
                    nextAttemptAt,
                    instantOf(eventId, "next_attempt_at")
            );

            // Antes do backoff nao ha evento elegivel.
            clock.set(nextAttemptAt.minusSeconds(1));
            assertEquals(
                    VerificationEmailOutboxProcessingResult.NO_EVENT,
                    service.executeNext()
            );

            clock.set(nextAttemptAt);
        }

        assertEquals(
                VerificationEmailOutboxProcessingResult.EXHAUSTED,
                service.executeNext()
        );
        assertEquals(5, calls.get());
        assertEquals("EXHAUSTED", statusOf(eventId));
        assertEquals(5, attemptsOf(eventId));

        clock.set(clock.instant().plus(Duration.ofDays(1)));
        assertEquals(
                VerificationEmailOutboxProcessingResult.NO_EVENT,
                service.executeNext()
        );
    }

    @Test
    void shouldExhaustRecoveredEventBeyondMaxAttempts() {
        UUID eventId = eventOf(createPendingUser());

        Instant at = now;

        // Cinco workers cairam com o evento reservado.
        for (int i = 0; i < 5; i++) {
            at = outbox.claimNext(at, LEASE)
                    .orElseThrow()
                    .leaseExpiresAt();
        }

        AtomicInteger calls = new AtomicInteger();

        ProcessVerificationEmailOutboxService service =
                new ProcessVerificationEmailOutboxService(
                        outbox,
                        user -> calls.incrementAndGet() > 0,
                        new MutableClock(at)
                );

        assertEquals(
                VerificationEmailOutboxProcessingResult.EXHAUSTED,
                service.executeNext()
        );
        assertEquals(0, calls.get());
        assertEquals("EXHAUSTED", statusOf(eventId));
        assertEquals(6, attemptsOf(eventId));
    }

    private UserId createPendingUser() {
        User user = User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of(
                        "outbox-" + UUID.randomUUID() + "@example.com"
                ),
                Instant.now().minusSeconds(60)
        );

        registration.register(
                user,
                new PasswordHash("integration-test-hash")
        );

        return user.getId();
    }

    private UUID eventOf(UserId userId) {
        return jdbc.queryForObject(
                """
                SELECT event_id
                FROM identity_verification_email_outbox
                WHERE user_id = ?
                """,
                UUID.class,
                userId.value()
        );
    }

    private String statusOf(UUID eventId) {
        return columnOf(eventId, "status");
    }

    private int attemptsOf(UUID eventId) {
        Integer attempts = jdbc.queryForObject(
                """
                SELECT attempt_count
                FROM identity_verification_email_outbox
                WHERE event_id = ?
                """,
                Integer.class,
                eventId
        );

        return attempts == null ? 0 : attempts;
    }

    private String columnOf(UUID eventId, String column) {
        return jdbc.queryForObject(
                "SELECT " + column
                        + " FROM identity_verification_email_outbox"
                        + " WHERE event_id = ?",
                String.class,
                eventId
        );
    }

    private Instant instantOf(UUID eventId, String column) {
        Timestamp value = jdbc.queryForObject(
                "SELECT " + column
                        + " FROM identity_verification_email_outbox"
                        + " WHERE event_id = ?",
                Timestamp.class,
                eventId
        );

        return value == null ? null : value.toInstant();
    }

    private void assertLeaseReleased(UUID eventId) {
        assertNull(columnOf(eventId, "lease_id"));
        assertNull(instantOf(eventId, "lease_expires_at"));
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

    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
