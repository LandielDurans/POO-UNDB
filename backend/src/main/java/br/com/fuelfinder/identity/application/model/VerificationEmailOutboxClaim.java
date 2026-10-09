package br.com.fuelfinder.identity.application.model;

import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record VerificationEmailOutboxClaim(
        UUID eventId,
        UserId userId,
        UUID leaseId,
        int attemptNumber,
        Instant leaseExpiresAt
) {

    public VerificationEmailOutboxClaim {
        Objects.requireNonNull(
                eventId,
                "EventId nao pode ser nulo."
        );

        Objects.requireNonNull(
                userId,
                "UserId nao pode ser nulo."
        );

        Objects.requireNonNull(
                leaseId,
                "LeaseId nao pode ser nulo."
        );

        Objects.requireNonNull(
                leaseExpiresAt,
                "Expiracao da reserva nao pode ser nula."
        );

        if (attemptNumber < 1) {
            throw new IllegalArgumentException(
                    "Numero da tentativa deve ser positivo."
            );
        }
    }
}
