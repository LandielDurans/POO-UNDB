package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxClaim;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxProcessingResult;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxResult;
import br.com.fuelfinder.identity.application.port.in.DispatchVerificationEmailUseCase;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailOutbox;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessVerificationEmailOutboxServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-09T12:00:00Z");

    private final FakeOutbox outbox = new FakeOutbox();
    private final FakeDispatcher dispatcher = new FakeDispatcher();

    private ProcessVerificationEmailOutboxService service() {
        return new ProcessVerificationEmailOutboxService(
                outbox,
                dispatcher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldReturnNoEventWhenOutboxIsEmpty() {
        outbox.empty = true;

        assertEquals(
                VerificationEmailOutboxProcessingResult.NO_EVENT,
                service().executeNext()
        );

        assertEquals(0, dispatcher.calls);
        assertEquals(0, outbox.completions);
    }

    @Test
    void shouldCompleteSuccessfullyDispatchedEvent() {
        assertEquals(
                VerificationEmailOutboxProcessingResult.DISPATCH_REQUESTED,
                service().executeNext()
        );

        assertEquals(1, dispatcher.calls);
        assertEquals(outbox.claim.userId(), dispatcher.receivedUserId);
        assertEquals(1, outbox.completions);
        assertEquals(
                VerificationEmailOutboxResult.DISPATCH_REQUESTED,
                outbox.result
        );
        assertEquals(outbox.claim.eventId(), outbox.receivedEventId);
        assertEquals(outbox.claim.leaseId(), outbox.receivedLeaseId);
        assertEquals(NOW, outbox.receivedTime);
        assertEquals(Duration.ofMinutes(5), outbox.leaseDuration);
    }

    @Test
    void shouldCompleteWhenNoActionIsRequired() {
        dispatcher.dispatched = false;

        assertEquals(
                VerificationEmailOutboxProcessingResult.NO_ACTION_REQUIRED,
                service().executeNext()
        );

        assertEquals(1, outbox.completions);
        assertEquals(
                VerificationEmailOutboxResult.NO_ACTION_REQUIRED,
                outbox.result
        );
    }

    @Test
    void shouldScheduleRetryAfterFirstFailure() {
        dispatcher.fail = true;

        assertEquals(
                VerificationEmailOutboxProcessingResult.RETRY_SCHEDULED,
                service().executeNext()
        );

        assertEquals(1, outbox.retries);
        assertEquals(NOW.plusSeconds(60), outbox.nextAttemptAt);
        assertEquals(0, outbox.completions);
    }

    @Test
    void shouldApplyExponentialBackoff() {
        outbox.setAttemptNumber(3);
        dispatcher.fail = true;

        assertEquals(
                VerificationEmailOutboxProcessingResult.RETRY_SCHEDULED,
                service().executeNext()
        );

        assertEquals(
                NOW.plus(Duration.ofMinutes(4)),
                outbox.nextAttemptAt
        );
    }

    @Test
    void shouldExhaustAfterFifthFailedAttempt() {
        outbox.setAttemptNumber(5);
        dispatcher.fail = true;

        assertEquals(
                VerificationEmailOutboxProcessingResult.EXHAUSTED,
                service().executeNext()
        );

        assertEquals(1, outbox.exhaustions);
        assertEquals(0, outbox.retries);
    }

    @Test
    void shouldNotDispatchAnExpiredClaim() {
        outbox.setExpiration(NOW);

        assertThrows(
                IllegalStateException.class,
                () -> service().executeNext()
        );

        assertEquals(0, dispatcher.calls);
    }

    @Test
    void shouldRejectFailedCompletionWithoutSchedulingRetry() {
        outbox.acceptCompletion = false;

        assertThrows(
                IllegalStateException.class,
                () -> service().executeNext()
        );

        assertEquals(1, dispatcher.calls);
        assertEquals(0, outbox.retries);
    }

    @Test
    void shouldRejectFailedRetryTransition() {
        dispatcher.fail = true;
        outbox.acceptRetry = false;

        assertThrows(
                IllegalStateException.class,
                () -> service().executeNext()
        );

        assertEquals(1, outbox.retries);
    }

    @Test
    void shouldRejectFailedExhaustionTransition() {
        dispatcher.fail = true;
        outbox.setAttemptNumber(5);
        outbox.acceptExhaustion = false;

        assertThrows(
                IllegalStateException.class,
                () -> service().executeNext()
        );

        assertEquals(1, outbox.exhaustions);
    }

    private static final class FakeDispatcher
            implements DispatchVerificationEmailUseCase {

        int calls;
        boolean fail;
        boolean dispatched = true;
        UserId receivedUserId;

        @Override
        public boolean execute(UserId userId) {
            calls++;
            receivedUserId = userId;

            if (fail) {
                throw new IllegalStateException(
                        "Falha simulada de envio."
                );
            }

            return dispatched;
        }
    }

    private static final class FakeOutbox
            implements VerificationEmailOutbox {

        VerificationEmailOutboxClaim claim =
                new VerificationEmailOutboxClaim(
                        UUID.randomUUID(),
                        UserId.newId(),
                        UUID.randomUUID(),
                        1,
                        NOW.plus(Duration.ofMinutes(5))
                );

        boolean empty;
        boolean acceptCompletion = true;
        boolean acceptRetry = true;
        boolean acceptExhaustion = true;

        int completions;
        int retries;
        int exhaustions;

        UUID receivedEventId;
        UUID receivedLeaseId;

        Duration leaseDuration;
        Instant receivedTime;
        Instant nextAttemptAt;
        VerificationEmailOutboxResult result;

        void setAttemptNumber(int number) {
            claim = new VerificationEmailOutboxClaim(
                    claim.eventId(),
                    claim.userId(),
                    claim.leaseId(),
                    number,
                    claim.leaseExpiresAt()
            );
        }

        void setExpiration(Instant expiration) {
            claim = new VerificationEmailOutboxClaim(
                    claim.eventId(),
                    claim.userId(),
                    claim.leaseId(),
                    claim.attemptNumber(),
                    expiration
            );
        }

        @Override
        public Optional<VerificationEmailOutboxClaim> claimNext(
                Instant now,
                Duration leaseDuration
        ) {
            this.receivedTime = now;
            this.leaseDuration = leaseDuration;

            return empty
                    ? Optional.empty()
                    : Optional.of(claim);
        }

        @Override
        public boolean complete(
                UUID eventId,
                UUID leaseId,
                VerificationEmailOutboxResult result,
                Instant completedAt
        ) {
            completions++;
            receivedEventId = eventId;
            receivedLeaseId = leaseId;
            receivedTime = completedAt;
            this.result = result;

            return acceptCompletion;
        }

        @Override
        public boolean scheduleRetry(
                UUID eventId,
                UUID leaseId,
                Instant failedAt,
                Instant nextAttemptAt
        ) {
            retries++;
            receivedEventId = eventId;
            receivedLeaseId = leaseId;
            receivedTime = failedAt;
            this.nextAttemptAt = nextAttemptAt;

            return acceptRetry;
        }

        @Override
        public boolean markExhausted(
                UUID eventId,
                UUID leaseId,
                Instant failedAt
        ) {
            exhaustions++;
            receivedEventId = eventId;
            receivedLeaseId = leaseId;
            receivedTime = failedAt;

            return acceptExhaustion;
        }
    }
}
