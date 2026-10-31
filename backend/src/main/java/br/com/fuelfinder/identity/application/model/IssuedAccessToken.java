package br.com.fuelfinder.identity.application.model;

import java.time.Instant;
import java.util.Objects;

public record IssuedAccessToken(
        String value,
        Instant issuedAt,
        Instant expiresAt
) {

    public IssuedAccessToken {
        Objects.requireNonNull(value, "Token de acesso nao pode ser nulo.");
        Objects.requireNonNull(issuedAt, "Emissao nao pode ser nula.");
        Objects.requireNonNull(expiresAt, "Expiracao nao pode ser nula.");

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "Expiracao deve ser posterior a emissao."
            );
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Token de acesso nao pode ser vazio."
            );
        }
    }

    @Override
    public String toString() {
        return "IssuedAccessToken[REDACTED]";
    }
}
