package br.com.fuelfinder.identity.application.model;

import java.util.Objects;

public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(
                value,
                "Hash de senha nao pode ser nulo."
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Hash da senha nao pode ficar vazio."
            );
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[REDACTED]";
    }
}
