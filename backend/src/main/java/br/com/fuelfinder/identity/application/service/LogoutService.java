package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.port.in.LogoutAllSessionsUseCase;
import br.com.fuelfinder.identity.application.port.in.LogoutUseCase;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.time.Clock;
import java.util.Objects;

public final class LogoutService
        implements LogoutUseCase, LogoutAllSessionsUseCase {

    private final SessionRepository sessionRepository;
    private final SessionTokenFactory tokenFactory;
    private final Clock clock;

    public LogoutService(
            SessionRepository sessionRepository,
            SessionTokenFactory tokenFactory,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.tokenFactory = Objects.requireNonNull(tokenFactory);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void execute(String refreshToken) {
        // Idempotente: nao revela se o token existia.
        if (!RefreshSessionService.isPlausible(refreshToken)) {
            return;
        }

        sessionRepository.revoke(
                tokenFactory.digest(refreshToken),
                clock.instant()
        );
    }

    @Override
    public int execute(UserId userId) {
        Objects.requireNonNull(userId, "UserId nao pode ser nulo.");

        return sessionRepository.revokeAllForUser(userId, clock.instant());
    }
}
