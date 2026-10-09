package br.com.fuelfinder.identity.application.model;

import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationEmailOutboxClaimTest {

    private final UUID eventId = UUID.randomUUID();
    private final UserId userId = UserId.newId();
    private final UUID leaseId = UUID.randomUUID();

    private final Instant expiresAt =
            Instant.parse("2026-10-09T16:00:00Z");

    @Test
    void shouldCreateValidClaim() {
        VerificationEmailOutboxClaim claim =
                new VerificationEmailOutboxClaim(
                        eventId,
                        userId,
                        leaseId,
                        1,
                        expiresAt
                );

        assertEquals(eventId, claim.eventId());
        assertEquals(userId, claim.userId());
        assertEquals(leaseId, claim.leaseId());
        assertEquals(1, claim.attemptNumber());
        assertEquals(expiresAt, claim.leaseExpiresAt());
    }

    @Test
    void shouldRejectNullEventId() {
        assertThrows(
                NullPointerException.class,
                () -> new VerificationEmailOutboxClaim(
                        null, userId, leaseId, 1, expiresAt
                )
        );
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(
                NullPointerException.class,
                () -> new VerificationEmailOutboxClaim(
                        eventId, null, leaseId, 1, expiresAt
                )
        );
    }

    @Test
    void shouldRejectNullLeaseId() {
        assertThrows(
                NullPointerException.class,
                () -> new VerificationEmailOutboxClaim(
                        eventId, userId, null, 1, expiresAt
                )
        );
    }

    @Test
    void shouldRejectNullExpiration() {
        assertThrows(
                NullPointerException.class,
                () -> new VerificationEmailOutboxClaim(
                        eventId, userId, leaseId, 1, null
                )
        );
    }

    @Test
    void shouldRejectInvalidAttemptNumber() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationEmailOutboxClaim(
                        eventId, userId, leaseId, 0, expiresAt
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationEmailOutboxClaim(
                        eventId, userId, leaseId, -1, expiresAt
                )
        );
    }
}
