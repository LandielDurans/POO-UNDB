package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.port.out.SessionRepository;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Recusa access tokens cuja sessao foi encerrada.
 *
 * Sem esta checagem, um JWT continuaria valido ate
 * expirar mesmo apos logout, bloqueio ou perda de
 * privilegios. O custo e uma consulta por chave
 * primaria a cada requisicao autenticada.
 */
public final class ActiveSessionTokenValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESSION_ENDED = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "Sessao encerrada ou expirada.",
            null
    );

    private final SessionRepository sessionRepository;
    private final Clock clock;

    public ActiveSessionTokenValidator(
            SessionRepository sessionRepository,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sessionId = token.getClaimAsString(
                JwtAccessTokenIssuer.SESSION_CLAIM
        );

        if (sessionId == null) {
            return OAuth2TokenValidatorResult.failure(SESSION_ENDED);
        }

        try {
            boolean active = sessionRepository.isActive(
                    UUID.fromString(sessionId),
                    clock.instant()
            );

            return active
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(SESSION_ENDED);
        } catch (IllegalArgumentException malformed) {
            return OAuth2TokenValidatorResult.failure(SESSION_ENDED);
        }
    }
}
