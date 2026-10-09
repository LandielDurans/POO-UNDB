package br.com.fuelfinder.identity.application.model;

import br.com.fuelfinder.identity.domain.valueobject.Email;

import java.time.Instant;
import java.util.Objects;

public final class PreparedVerificationEmail {

    private final Email recipient;
    private final String token;
    private final Instant expiresAt;

    public PreparedVerificationEmail(
            Email recipient,
            String token,
            Instant expiresAt
    ) {
        this.recipient = Objects.requireNonNull(
                recipient, "Destinatario nao pode ser nulo."
        );

        this.token = Objects.requireNonNull(
                token, "Token nao pode ser nulo."
        );

        this.expiresAt = Objects.requireNonNull(
                expiresAt, "Expiracao nao pode ser nula."
        );

        if (token.isBlank()) {
            throw new IllegalArgumentException(
                    "Token nao pode estar vazio."
            );
        }
    }

    public Email recipient() {
        return recipient;
    }

    public String token() {
        return token;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "PreparedVerificationEmail[REDACTED]";
    }
}
