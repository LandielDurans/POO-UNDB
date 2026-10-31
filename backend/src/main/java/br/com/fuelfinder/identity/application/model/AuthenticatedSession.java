package br.com.fuelfinder.identity.application.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Resultado de login ou renovacao.
 *
 * O refresh token bruto so existe aqui, a caminho
 * do cliente; o banco guarda apenas seu digest.
 */
public record AuthenticatedSession(
        IssuedAccessToken accessToken,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {

    public AuthenticatedSession {
        Objects.requireNonNull(accessToken, "Token de acesso nao pode ser nulo.");
        Objects.requireNonNull(refreshToken, "Refresh token nao pode ser nulo.");
        Objects.requireNonNull(
                refreshTokenExpiresAt,
                "Expiracao do refresh token nao pode ser nula."
        );

        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token nao pode ser vazio."
            );
        }
    }

    @Override
    public String toString() {
        return "AuthenticatedSession[REDACTED]";
    }
}
