package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxClaim;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxProcessingResult;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxResult;
import br.com.fuelfinder.identity.application.port.in.DispatchVerificationEmailUseCase;
import br.com.fuelfinder.identity.application.port.in.ProcessVerificationEmailOutboxUseCase;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailOutbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ProcessVerificationEmailOutboxService
        implements ProcessVerificationEmailOutboxUseCase {

    private static final int MAX_ATTEMPTS = 5;

    private static final Duration LEASE_DURATION =
            Duration.ofMinutes(5);

    private final VerificationEmailOutbox outbox;
    private final DispatchVerificationEmailUseCase dispatcher;
    private final Clock clock;

    public ProcessVerificationEmailOutboxService(
            VerificationEmailOutbox outbox,
            DispatchVerificationEmailUseCase dispatcher,
            Clock clock
    ) {
        this.outbox = Objects.requireNonNull(outbox);
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public VerificationEmailOutboxProcessingResult executeNext() {
        Instant now = clock.instant();

        Optional<VerificationEmailOutboxClaim> pending =
                Objects.requireNonNull(
                        outbox.claimNext(now, LEASE_DURATION),
                        "Outbox retornou resultado nulo."
                );

        if (pending.isEmpty()) {
            return VerificationEmailOutboxProcessingResult.NO_EVENT;
        }

        VerificationEmailOutboxClaim claim = pending.get();

        if (!clock.instant().isBefore(claim.leaseExpiresAt())) {
            throw new IllegalStateException(
                    "Outbox retornou uma reserva expirada."
            );
        }

        if (claim.attemptNumber() > MAX_ATTEMPTS) {
            boolean marked = outbox.markExhausted(
                    claim.eventId(),
                    claim.leaseId(),
                    clock.instant()
            );

            requireTransition(marked);

            return VerificationEmailOutboxProcessingResult.EXHAUSTED;
        }

        final boolean dispatched;

        try {
            dispatched = dispatcher.execute(claim.userId());
        } catch (RuntimeException failure) {
            return handleDispatchFailure(claim, failure);
        }

        VerificationEmailOutboxResult result =
                dispatched
                        ? VerificationEmailOutboxResult.DISPATCH_REQUESTED
                        : VerificationEmailOutboxResult.NO_ACTION_REQUIRED;

        boolean completed = outbox.complete(
                claim.eventId(),
                claim.leaseId(),
                result,
                clock.instant()
        );

        requireTransition(completed);

        return dispatched
                ? VerificationEmailOutboxProcessingResult.DISPATCH_REQUESTED
                : VerificationEmailOutboxProcessingResult.NO_ACTION_REQUIRED;
    }

    private VerificationEmailOutboxProcessingResult
    handleDispatchFailure(
            VerificationEmailOutboxClaim claim,
            RuntimeException failure
    ) {
        Instant failedAt = clock.instant();

        if (claim.attemptNumber() >= MAX_ATTEMPTS) {
            boolean marked = outbox.markExhausted(
                    claim.eventId(),
                    claim.leaseId(),
                    failedAt
            );

            if (!marked) {
                throw new IllegalStateException(
                        "Nao foi possivel encerrar a reserva.",
                        failure
                );
            }

            return VerificationEmailOutboxProcessingResult.EXHAUSTED;
        }

        long delayMinutes = 1L << (claim.attemptNumber() - 1);

        Instant nextAttemptAt =
                failedAt.plus(Duration.ofMinutes(delayMinutes));

        boolean scheduled = outbox.scheduleRetry(
                claim.eventId(),
                claim.leaseId(),
                failedAt,
                nextAttemptAt
        );

        if (!scheduled) {
            throw new IllegalStateException(
                    "Nao foi possivel reagendar a reserva.",
                    failure
            );
        }

        return VerificationEmailOutboxProcessingResult.RETRY_SCHEDULED;
    }

    private void requireTransition(boolean accepted) {
        if (!accepted) {
            throw new IllegalStateException(
                    "Transicao da outbox rejeitada: "
                            + "reserva invalida ou expirada."
            );
        }
    }
}
