package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.exception.InvalidRefreshTokenException;
import br.com.fuelfinder.identity.application.model.ActiveSession;
import br.com.fuelfinder.identity.application.model.AuthenticatedSession;
import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.application.port.in.RefreshSessionUseCase;
import br.com.fuelfinder.identity.application.port.out.AccessTokenIssuer;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RefreshSessionService implements RefreshSessionUseCase {

    static final int MAX_TOKEN_LENGTH = 512;

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionTokenFactory tokenFactory;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    public RefreshSessionService(
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SessionTokenFactory tokenFactory,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.tokenFactory = Objects.requireNonNull(tokenFactory);
        this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public AuthenticatedSession execute(String refreshToken) {
        if (!isPlausible(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Instant now = clock.instant();
        Instant refreshExpiresAt = now.plus(tokenFactory.refreshTokenTtl());

        SessionTokenFactory.RefreshToken next = tokenFactory.newRefreshToken();

        // A troca do digest e atomica: dois pedidos com o
        // mesmo token nao renovam a sessao duas vezes.
        ActiveSession session = sessionRepository.rotate(
                tokenFactory.digest(refreshToken),
                next.digest(),
                now,
                refreshExpiresAt
        ).orElseThrow(InvalidRefreshTokenException::new);

        // Papeis e status sao relidos a cada renovacao.
        Optional<User> user = userRepository.findById(session.userId());

        if (user.isEmpty() || user.get().getStatus() != UserStatus.ACTIVE) {
            sessionRepository.revokeById(session.sessionId(), now);
            throw new InvalidRefreshTokenException();
        }

        IssuedAccessToken accessToken = accessTokenIssuer.issue(
                user.get(),
                session.sessionId(),
                now
        );

        return new AuthenticatedSession(
                accessToken,
                next.raw(),
                refreshExpiresAt
        );
    }

    static boolean isPlausible(String refreshToken) {
        return refreshToken != null
                && !refreshToken.isBlank()
                && refreshToken.length() <= MAX_TOKEN_LENGTH;
    }
}
