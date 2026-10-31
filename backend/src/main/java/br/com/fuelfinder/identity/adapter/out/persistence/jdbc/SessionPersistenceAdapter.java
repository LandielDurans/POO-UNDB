package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.application.model.ActiveSession;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SessionPersistenceAdapter implements SessionRepository {

    private final JdbcTemplate jdbc;

    public SessionPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    @Transactional
    public void create(
            UUID sessionId,
            UserId userId,
            VerificationTokenDigest refreshTokenDigest,
            Instant createdAt,
            Instant expiresAt
    ) {
        Objects.requireNonNull(sessionId, "SessionId nao pode ser nulo.");
        Objects.requireNonNull(userId, "UserId nao pode ser nulo.");
        Objects.requireNonNull(refreshTokenDigest, "Digest nao pode ser nulo.");
        Objects.requireNonNull(createdAt, "Data de criacao nao pode ser nula.");
        Objects.requireNonNull(expiresAt, "Expiracao nao pode ser nula.");

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "Expiracao deve ser posterior a criacao."
            );
        }

        Timestamp created = Timestamp.from(createdAt);

        jdbc.update(
                """
                INSERT INTO identity_sessions
                    (id, user_id, refresh_token_digest,
                     created_at, last_refreshed_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                userId.value(),
                refreshTokenDigest.value(),
                created,
                created,
                Timestamp.from(expiresAt)
        );
    }

    @Override
    @Transactional
    public Optional<ActiveSession> rotate(
            VerificationTokenDigest currentDigest,
            VerificationTokenDigest nextDigest,
            Instant now,
            Instant expiresAt
    ) {
        Objects.requireNonNull(currentDigest, "Digest atual nao pode ser nulo.");
        Objects.requireNonNull(nextDigest, "Novo digest nao pode ser nulo.");
        Objects.requireNonNull(now, "Instante atual nao pode ser nulo.");
        Objects.requireNonNull(expiresAt, "Expiracao nao pode ser nula.");

        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Expiracao deve ser posterior ao instante atual."
            );
        }

        Timestamp time = Timestamp.from(now);

        // Um unico UPDATE condicional: entre dois pedidos
        // concorrentes com o mesmo token, so um encontra
        // o digest antigo.
        List<ActiveSession> rotated = jdbc.query(
                """
                UPDATE identity_sessions
                SET refresh_token_digest = ?,
                    last_refreshed_at = ?,
                    expires_at = ?
                WHERE refresh_token_digest = ?
                  AND revoked_at IS NULL
                  AND expires_at > ?
                  AND last_refreshed_at <= ?
                RETURNING id, user_id
                """,
                (rs, rowNum) -> new ActiveSession(
                        rs.getObject("id", UUID.class),
                        UserId.from(rs.getObject("user_id", UUID.class))
                ),
                nextDigest.value(),
                time,
                Timestamp.from(expiresAt),
                currentDigest.value(),
                time,
                time
        );

        return rotated.stream().findFirst();
    }

    @Override
    @Transactional
    public boolean revoke(VerificationTokenDigest refreshTokenDigest, Instant now) {
        Objects.requireNonNull(refreshTokenDigest, "Digest nao pode ser nulo.");
        Objects.requireNonNull(now, "Instante atual nao pode ser nulo.");

        return jdbc.update(
                """
                UPDATE identity_sessions
                SET revoked_at = GREATEST(?, created_at)
                WHERE refresh_token_digest = ?
                  AND revoked_at IS NULL
                """,
                Timestamp.from(now),
                refreshTokenDigest.value()
        ) == 1;
    }

    @Override
    @Transactional
    public boolean revokeById(UUID sessionId, Instant now) {
        Objects.requireNonNull(sessionId, "SessionId nao pode ser nulo.");
        Objects.requireNonNull(now, "Instante atual nao pode ser nulo.");

        return jdbc.update(
                """
                UPDATE identity_sessions
                SET revoked_at = GREATEST(?, created_at)
                WHERE id = ?
                  AND revoked_at IS NULL
                """,
                Timestamp.from(now),
                sessionId
        ) == 1;
    }

    @Override
    @Transactional
    public int revokeAllForUser(UserId userId, Instant now) {
        Objects.requireNonNull(userId, "UserId nao pode ser nulo.");
        Objects.requireNonNull(now, "Instante atual nao pode ser nulo.");

        return jdbc.update(
                """
                UPDATE identity_sessions
                SET revoked_at = GREATEST(?, created_at)
                WHERE user_id = ?
                  AND revoked_at IS NULL
                """,
                Timestamp.from(now),
                userId.value()
        );
    }

    @Override
    public boolean isActive(UUID sessionId, Instant now) {
        Objects.requireNonNull(sessionId, "SessionId nao pode ser nulo.");
        Objects.requireNonNull(now, "Instante atual nao pode ser nulo.");

        return Boolean.TRUE.equals(jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM identity_sessions
                    WHERE id = ?
                      AND revoked_at IS NULL
                      AND expires_at > ?
                )
                """,
                Boolean.class,
                sessionId,
                Timestamp.from(now)
        ));
    }
}
