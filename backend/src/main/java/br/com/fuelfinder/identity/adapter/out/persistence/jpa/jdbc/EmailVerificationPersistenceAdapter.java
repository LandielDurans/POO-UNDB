package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.EmailVerificationPersistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class EmailVerificationPersistenceAdapter
        implements EmailVerificationPersistence {

    private final JdbcTemplate jdbc;

    public EmailVerificationPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    @Transactional
    public boolean consumeAndActivate(
            VerificationTokenDigest digest,
            Instant verifiedAt
    ) {
        Objects.requireNonNull(digest);
        Objects.requireNonNull(verifiedAt);

        Timestamp time = Timestamp.from(verifiedAt);

        // Localizar o usuario associado.
        List<UUID> userIds = jdbc.query(
                """
                SELECT user_id
                FROM identity_email_verification_tokens
                WHERE digest = ?
                """,
                (rs, rowNum) ->
                        rs.getObject("user_id", UUID.class),
                digest.value()
        );

        if (userIds.isEmpty()) {
            return false;
        }

        UUID userId = userIds.get(0);

        List<String> statuses = jdbc.query(
                """
                SELECT status
                FROM identity_users
                WHERE id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getString("status"),
                userId
        );

        if (statuses.isEmpty()
                || !"PENDING_VERIFICATION".equals(
                statuses.get(0))) {
            return false;
        }

        Boolean validToken = jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM identity_email_verification_tokens
                    WHERE digest = ?
                      AND user_id = ?
                      AND consumed_at IS NULL
                      AND issued_at <= ?
                      AND expires_at > ?
                )
                """,
                Boolean.class,
                digest.value(),
                userId,
                time,
                time
        );

        if (!Boolean.TRUE.equals(validToken)) {
            return false;
        }

        int activated = jdbc.update(
                """
                UPDATE identity_users
                SET status = 'ACTIVE',
                    updated_at = ?
                WHERE id = ?
                  AND status = 'PENDING_VERIFICATION'
                  AND updated_at <= ?
                """,
                time,
                userId,
                time
        );

        if (activated != 1) {
            return false;
        }

        int consumed = jdbc.update(
                """
                UPDATE identity_email_verification_tokens
                SET consumed_at = ?
                WHERE digest = ?
                  AND user_id = ?
                  AND consumed_at IS NULL
                  AND issued_at <= ?
                  AND expires_at > ?
                """,
                time,
                digest.value(),
                userId,
                time,
                time
        );

        if (consumed != 1) {
            throw new IllegalStateException(
                    "Falha ao consumir token de verificacao."
            );
        }

        // Invalida outros tokens anteriormente emitidos.
        jdbc.update(
                """
                UPDATE identity_email_verification_tokens
                SET consumed_at = ?
                WHERE user_id = ?
                  AND consumed_at IS NULL
                  AND issued_at <= ?
                """,
                time,
                userId,
                time
        );

        // Descarta eventuais tokens inconsistentes
        jdbc.update(
                """
                DELETE FROM identity_email_verification_tokens
                WHERE user_id = ?
                  AND consumed_at IS NULL
                """,
                userId
        );

        return true;
    }
}
