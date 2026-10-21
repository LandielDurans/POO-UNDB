package br.com.fuelfinder.identity.adapter.out.persistence.jdbc;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.EmailVerificationTokenIssuer;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;


@Repository
public class EmailVerificationTokenIssuerAdapter
        implements EmailVerificationTokenIssuer {

    private final JdbcTemplate jdbc;

    public EmailVerificationTokenIssuerAdapter(
            JdbcTemplate jdbc
    ) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    @Transactional
    public boolean issueIfPending(
            UserId userId,
            VerificationTokenDigest digest,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Objects.requireNonNull(
                userId,
                "UserId nao pode ser nulo."
        );

        Objects.requireNonNull(
                digest,
                "Digest nao pode ser nulo."
        );

        Objects.requireNonNull(
                issuedAt,
                "Data de emissao nao pode ser nula."
        );

        Objects.requireNonNull(
                expiresAt,
                "Data de expiracao nao pode ser nula."
        );

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "Expiracao deve ser posterior a emissao."
            );
        }

        // A verificacao do status e a insercao
        // devem ocorrer sob a mesma transacao.
        //
        // FOR UPDATE serializa esta operacao
        // com a ativacao da conta.

        List<String> statuses = jdbc.query(
                """
                SELECT status
                FROM identity_users
                WHERE id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getString("status"),
                userId.value()
        );

        if (statuses.isEmpty()) {
            return false;
        }

        if (!"PENDING_VERIFICATION".equals(
                statuses.get(0)
        )) {
            return false;
        }

        // Nunca armazenamos o token original.
        // Apenas seu SHA-256 hexadecimal.

        int inserted = jdbc.update(
                """
                INSERT INTO identity_email_verification_tokens
                    (digest, user_id, issued_at, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                digest.value(),
                userId.value(),
                Timestamp.from(issuedAt),
                Timestamp.from(expiresAt)
        );

        if (inserted != 1) {
            throw new IllegalStateException(
                    "Falha ao persistir token de verificacao."
            );
        }

        return true;
    }
}
