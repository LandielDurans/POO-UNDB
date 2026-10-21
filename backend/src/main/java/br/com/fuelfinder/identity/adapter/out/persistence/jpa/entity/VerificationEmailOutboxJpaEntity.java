package br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "identity_verification_email_outbox")
public class VerificationEmailOutboxJpaEntity {

    @Id
    @Column(
            name = "event_id",
            nullable = false,
            updatable = false
    )
    private UUID eventId;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VerificationEmailOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VerificationEmailOutboxJpaEntity() {
    }

    public VerificationEmailOutboxJpaEntity(
            UUID eventId,
            UUID userId,
            Instant createdAt
    ) {
        this.eventId = Objects.requireNonNull(
                eventId,
                "EventId nao pode ser nulo."
        );

        this.userId = Objects.requireNonNull(
                userId,
                "UserId nao pode ser nulo."
        );

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Data de criacao nao pode ser nula."
        );

        this.status = VerificationEmailOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public VerificationEmailOutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
