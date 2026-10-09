package br.com.fuelfinder.identity.application.port.in;

import java.util.Objects;

public final class VerifyEmailCommand {

    private static final int MAX_TOKEN_LENGTH = 512;

    private final String token;

    public VerifyEmailCommand(String token) {
        this.token = Objects.requireNonNull(
                token,
                "Token de verificacao nao pode ser nulo."
        );

        if (token.isBlank()) {
            throw new IllegalArgumentException(
                    "Token de verificacao nao pode ser vazio."
            );
        }

        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new IllegalArgumentException(
                    "Token de verificacao excede o tamanho permitido."
            );
        }
    }

    public String token() {
        return token;
    }

    @Override
    public String toString() {
        return "VerifyEmailCommand[REDACTED]";
    }
}
