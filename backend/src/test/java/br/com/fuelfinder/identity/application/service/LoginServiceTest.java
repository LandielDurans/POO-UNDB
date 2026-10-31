package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.adapter.security.SecureRandomVerificationTokenGenerator;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;
import br.com.fuelfinder.identity.application.exception.AccountBlockedException;
import br.com.fuelfinder.identity.application.exception.AccountNotVerifiedException;
import br.com.fuelfinder.identity.application.exception.InvalidCredentialsException;
import br.com.fuelfinder.identity.application.model.AuthenticatedSession;
import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.in.LoginCommand;
import br.com.fuelfinder.identity.application.port.out.AccessTokenIssuer;
import br.com.fuelfinder.identity.application.port.out.LocalCredentialRepository;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.application.port.out.UserRepository;
import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Email EMAIL = Email.of("maria@example.com");
    private static final String PASSWORD = "Senha2026";
    private static final PasswordHash STORED = new PasswordHash("stored-hash");

    private final UserRepository users = mock(UserRepository.class);
    private final LocalCredentialRepository credentials =
            mock(LocalCredentialRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final AccessTokenIssuer accessTokens = mock(AccessTokenIssuer.class);

    private final SessionTokenFactory tokenFactory = new SessionTokenFactory(
            new SecureRandomVerificationTokenGenerator(),
            new Sha256VerificationTokenDigester(),
            REFRESH_TTL
    );

    private LoginService service;

    @BeforeEach
    void setUp() {
        when(hasher.hash(any())).thenReturn(new PasswordHash("dummy-hash"));
        when(hasher.matches(any(), any())).thenAnswer(call ->
                PASSWORD.equals(call.getArgument(0))
                        && STORED.equals(call.getArgument(1)));
        when(accessTokens.issue(any(), any(), any())).thenReturn(
                new IssuedAccessToken("jwt", NOW, NOW.plusSeconds(900))
        );

        service = new LoginService(
                users,
                credentials,
                hasher,
                sessions,
                tokenFactory,
                accessTokens,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldOpenSessionForActiveUser() {
        User user = givenUser(UserStatus.ACTIVE);

        AuthenticatedSession session = service.execute(login(PASSWORD));

        assertEquals("jwt", session.accessToken().value());
        assertEquals(NOW.plus(REFRESH_TTL), session.refreshTokenExpiresAt());

        ArgumentCaptor<UUID> sessionId = ArgumentCaptor.forClass(UUID.class);

        // So o digest do refresh token devolvido e gravado.
        verify(sessions).create(
                sessionId.capture(),
                eq(user.getId()),
                eq(tokenFactory.digest(session.refreshToken())),
                eq(NOW),
                eq(NOW.plus(REFRESH_TTL))
        );
        verify(accessTokens).issue(user, sessionId.getValue(), NOW);
    }

    @Test
    void shouldRejectUnknownEmailLikeWrongPassword() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.execute(login(PASSWORD))
        );

        // A senha ainda e comparada, com o hash ficticio.
        verify(hasher).matches(PASSWORD, new PasswordHash("dummy-hash"));
        verifyNoInteractions(sessions, accessTokens);
    }

    @Test
    void shouldRejectWrongPassword() {
        givenUser(UserStatus.ACTIVE);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.execute(login("SenhaErrada1"))
        );
        verifyNoInteractions(sessions, accessTokens);
    }

    @Test
    void shouldRejectAccountWithoutLocalCredential() {
        User user = givenUser(UserStatus.ACTIVE);
        when(credentials.findPasswordHashByUserId(user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.execute(login(PASSWORD))
        );
        verifyNoInteractions(sessions);
    }

    @Test
    void shouldReportPendingVerificationOnlyWithCorrectPassword() {
        givenUser(UserStatus.PENDING_VERIFICATION);

        assertThrows(
                AccountNotVerifiedException.class,
                () -> service.execute(login(PASSWORD))
        );

        // Sem a senha, o status da conta nao e revelado.
        assertThrows(
                InvalidCredentialsException.class,
                () -> service.execute(login("SenhaErrada1"))
        );
        verifyNoInteractions(sessions);
    }

    @Test
    void shouldRejectBlockedAccount() {
        givenUser(UserStatus.BLOCKED);

        assertThrows(
                AccountBlockedException.class,
                () -> service.execute(login(PASSWORD))
        );
        verifyNoInteractions(sessions);
    }

    private User givenUser(UserStatus status) {
        User user = User.reconstitute(
                UserId.newId(),
                "Maria Silva",
                EMAIL,
                Set.of(UserRole.USER),
                status,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(3600)
        );

        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(credentials.findPasswordHashByUserId(user.getId()))
                .thenReturn(Optional.of(STORED));

        return user;
    }

    private static LoginCommand login(String password) {
        return new LoginCommand(EMAIL, password);
    }
}
