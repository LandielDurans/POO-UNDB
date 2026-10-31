package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.application.exception.AccountBlockedException;
import br.com.fuelfinder.identity.application.exception.AccountNotVerifiedException;
import br.com.fuelfinder.identity.application.exception.InvalidCredentialsException;
import br.com.fuelfinder.identity.application.model.AuthenticatedSession;
import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.in.LoginCommand;
import br.com.fuelfinder.identity.application.port.in.LoginUseCase;
import br.com.fuelfinder.identity.application.port.out.AccessTokenIssuer;
import br.com.fuelfinder.identity.application.port.out.LocalCredentialRepository;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.model.User;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final LocalCredentialRepository credentialRepository;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final SessionTokenFactory tokenFactory;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    // Comparado quando o e-mail nao existe, para que a
    // resposta leve o mesmo tempo de uma senha errada.
    private final PasswordHash dummyHash;

    public LoginService(
            UserRepository userRepository,
            LocalCredentialRepository credentialRepository,
            PasswordHasher passwordHasher,
            SessionRepository sessionRepository,
            SessionTokenFactory tokenFactory,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.credentialRepository = Objects.requireNonNull(credentialRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.tokenFactory = Objects.requireNonNull(tokenFactory);
        this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
        this.clock = Objects.requireNonNull(clock);

        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    public AuthenticatedSession execute(LoginCommand command) {
        Objects.requireNonNull(command, "Comando nao pode ser nulo.");

        Optional<User> found = userRepository.findByEmail(command.email());

        Optional<PasswordHash> storedHash = found.flatMap(
                user -> credentialRepository
                        .findPasswordHashByUserId(user.getId())
        );

        if (storedHash.isEmpty()) {
            passwordHasher.matches(command.password(), dummyHash);
            throw new InvalidCredentialsException();
        }

        if (!passwordHasher.matches(command.password(), storedHash.get())) {
            throw new InvalidCredentialsException();
        }

        // O status so e revelado a quem conhece a senha.
        User user = found.get();

        switch (user.getStatus()) {
            case ACTIVE -> { }
            case PENDING_VERIFICATION -> throw new AccountNotVerifiedException();
            case BLOCKED -> throw new AccountBlockedException();
            case DELETED -> throw new InvalidCredentialsException();
        }

        return openSession(user);
    }

    private AuthenticatedSession openSession(User user) {
        Instant now = clock.instant();
        Instant refreshExpiresAt = now.plus(tokenFactory.refreshTokenTtl());
        UUID sessionId = UUID.randomUUID();

        SessionTokenFactory.RefreshToken refreshToken =
                tokenFactory.newRefreshToken();

        sessionRepository.create(
                sessionId,
                user.getId(),
                refreshToken.digest(),
                now,
                refreshExpiresAt
        );

        IssuedAccessToken accessToken =
                accessTokenIssuer.issue(user, sessionId, now);

        return new AuthenticatedSession(
                accessToken,
                refreshToken.raw(),
                refreshExpiresAt
        );
    }
}
