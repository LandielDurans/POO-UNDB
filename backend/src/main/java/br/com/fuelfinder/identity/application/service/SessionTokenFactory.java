package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenDigester;
import br.com.fuelfinder.identity.application.port.out.VerificationTokenGenerator;

import java.time.Duration;
import java.util.Objects;

/**
 * Gera refresh tokens e seus digests.
 *
 * Reutiliza o gerador SecureRandom e o digest SHA-256
 * dos tokens de verificacao: ambos sao tokens
 * aleatorios de alta entropia.
 */
public final class SessionTokenFactory {

    private final VerificationTokenGenerator generator;
    private final VerificationTokenDigester digester;
    private final Duration refreshTokenTtl;

    public SessionTokenFactory(
            VerificationTokenGenerator generator,
            VerificationTokenDigester digester,
            Duration refreshTokenTtl
    ) {
        this.generator = Objects.requireNonNull(generator);
        this.digester = Objects.requireNonNull(digester);
        this.refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl);

        if (refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalArgumentException(
                    "Validade do refresh token deve ser positiva."
            );
        }
    }

    public RefreshToken newRefreshToken() {
        String raw = Objects.requireNonNull(
                generator.generate(),
                "Gerador retornou token nulo."
        );

        return new RefreshToken(raw, digester.digest(raw));
    }

    public VerificationTokenDigest digest(String rawRefreshToken) {
        return digester.digest(rawRefreshToken);
    }

    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

    public record RefreshToken(String raw, VerificationTokenDigest digest) {

        @Override
        public String toString() {
            return "RefreshToken[REDACTED]";
        }
    }
}
