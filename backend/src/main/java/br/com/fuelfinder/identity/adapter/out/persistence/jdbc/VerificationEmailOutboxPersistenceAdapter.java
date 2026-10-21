package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxClaim;
import br.com.fuelfinder.identity.application.model.VerificationEmailOutboxResult;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailOutbox;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VerificationEmailOutboxPersistenceAdapter
        implements VerificationEmailOutbox {

    private final JdbcTemplate jdbc;

    public VerificationEmailOutboxPersistenceAdapter(
            JdbcTemplate jdbc
    ) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    @Transactional
    public Optional<VerificationEmailOutboxClaim> claimNext(
            Instant now,
            Duration leaseDuration
    ) {
        Objects.requireNonNull(
                now,
                "Instante atual nao pode ser nulo."
        );

        Objects.requireNonNull(
                leaseDuration,
                "Duracao da reserva nao pode ser nula."
        );

        if (leaseDuration.isNegative() || leaseDuration.isZero()) {
            throw new IllegalArgumentException(
                    "Duracao da reserva deve ser positiva."
            );
        }

        Timestamp time = Timestamp.from(now);

        // Elegiveis: PENDING cujo horario chegou ou
        // PROCESSING com lease expirado (worker que caiu).
        //
        // SKIP LOCKED faz workers concorrentes
        // pularem eventos ja bloqueados, em vez de
        // aguardarem ou reservarem o mesmo evento.
        //
        // A selecao e a reserva ocorrem em uma
        // unica instrucao.

        List<VerificationEmailOutboxClaim> claims = jdbc.query(
                """
                WITH candidate AS (
                    SELECT event_id
                    FROM identity_verification_email_outbox
                    WHERE (
                            (status = 'PENDING'
                                AND next_attempt_at <= ?)
                            OR (status = 'PROCESSING'
                                AND lease_expires_at <= ?)
                          )
                      AND updated_at <= ?
                    ORDER BY
                        CASE
                            WHEN status = 'PENDING'
                                THEN next_attempt_at
                            ELSE lease_expires_at
                        END,
                        created_at,
                        event_id
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE identity_verification_email_outbox o
                SET status = 'PROCESSING',
                    lease_id = ?,
                    lease_expires_at = ?,
                    attempt_count = o.attempt_count + 1,
                    updated_at = ?
                FROM candidate c
                WHERE o.event_id = c.event_id
                RETURNING
                    o.event_id,
                    o.user_id,
                    o.lease_id,
                    o.attempt_count,
                    o.lease_expires_at
                """,
                (rs, rowNum) -> new VerificationEmailOutboxClaim(
                        rs.getObject("event_id", UUID.class),
                        UserId.from(
                                rs.getObject("user_id", UUID.class)
                        ),
                        rs.getObject("lease_id", UUID.class),
                        rs.getInt("attempt_count"),
                        rs.getTimestamp("lease_expires_at")
                                .toInstant()
                ),
                time,
                time,
                time,
                UUID.randomUUID(),
                Timestamp.from(now.plus(leaseDuration)),
                time
        );

        return claims.stream().findFirst();
    }

    @Override
    @Transactional
    public boolean complete(
            UUID eventId,
            UUID leaseId,
            VerificationEmailOutboxResult result,
            Instant completedAt
    ) {
        requireLease(eventId, leaseId);

        Objects.requireNonNull(
                result,
                "Resultado nao pode ser nulo."
        );

        Objects.requireNonNull(
                completedAt,
                "Data de conclusao nao pode ser nula."
        );

        Timestamp time = Timestamp.from(completedAt);

        int updated = jdbc.update(
                """
                UPDATE identity_verification_email_outbox
                SET status = 'COMPLETED',
                    processing_result = ?,
                    completed_at = ?,
                    lease_id = NULL,
                    lease_expires_at = NULL,
                    updated_at = ?
                WHERE event_id = ?
                  AND status = 'PROCESSING'
                  AND lease_id = ?
                  AND lease_expires_at > ?
                  AND updated_at <= ?
                """,
                result.name(),
                time,
                time,
                eventId,
                leaseId,
                time,
                time
        );

        return updated == 1;
    }

    @Override
    @Transactional
    public boolean scheduleRetry(
            UUID eventId,
            UUID leaseId,
            Instant failedAt,
            Instant nextAttemptAt
    ) {
        requireLease(eventId, leaseId);

        Objects.requireNonNull(
                failedAt,
                "Data da falha nao pode ser nula."
        );

        Objects.requireNonNull(
                nextAttemptAt,
                "Data da nova tentativa nao pode ser nula."
        );

        if (!nextAttemptAt.isAfter(failedAt)) {
            throw new IllegalArgumentException(
                    "Nova tentativa deve ser posterior a falha."
            );
        }

        Timestamp time = Timestamp.from(failedAt);

        // Volta para PENDING liberando a reserva;
        // claimNext so o encontra apos nextAttemptAt.

        int updated = jdbc.update(
                """
                UPDATE identity_verification_email_outbox
                SET status = 'PENDING',
                    next_attempt_at = ?,
                    lease_id = NULL,
                    lease_expires_at = NULL,
                    updated_at = ?
                WHERE event_id = ?
                  AND status = 'PROCESSING'
                  AND lease_id = ?
                  AND lease_expires_at > ?
                  AND updated_at <= ?
                """,
                Timestamp.from(nextAttemptAt),
                time,
                eventId,
                leaseId,
                time,
                time
        );

        return updated == 1;
    }

    @Override
    @Transactional
    public boolean markExhausted(
            UUID eventId,
            UUID leaseId,
            Instant failedAt
    ) {
        requireLease(eventId, leaseId);

        Objects.requireNonNull(
                failedAt,
                "Data da falha nao pode ser nula."
        );

        Timestamp time = Timestamp.from(failedAt);

        // O evento permanece na tabela para auditoria.

        int updated = jdbc.update(
                """
                UPDATE identity_verification_email_outbox
                SET status = 'EXHAUSTED',
                    exhausted_at = ?,
                    lease_id = NULL,
                    lease_expires_at = NULL,
                    updated_at = ?
                WHERE event_id = ?
                  AND status = 'PROCESSING'
                  AND lease_id = ?
                  AND lease_expires_at > ?
                  AND updated_at <= ?
                """,
                time,
                time,
                eventId,
                leaseId,
                time,
                time
        );

        return updated == 1;
    }

    private static void requireLease(UUID eventId, UUID leaseId) {
        Objects.requireNonNull(
                eventId,
                "EventId nao pode ser nulo."
        );

        Objects.requireNonNull(
                leaseId,
                "LeaseId nao pode ser nulo."
        );
    }
}
