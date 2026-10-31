package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.adapter.security.SecureRandomVerificationTokenGenerator;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;
import br.com.fuelfinder.identity.application.exception.InvalidRefreshTokenException;
import br.com.fuelfinder.identity.application.model.ActiveSession;
import br.com.fuelfinder.identity.application.model.AuthenticatedSession;
import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.application.model.VerificationTokenDigest;
import br.com.fuelfinder.identity.application.port.out.AccessTokenIssuer;
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

class RefreshSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AccessTokenIssuer accessTokens = mock(AccessTokenIssuer.class);

    private final SessionTokenFactory tokenFactory = new SessionTokenFactory(
            new SecureRandomVerificationTokenGenerator(),
            new Sha256VerificationTokenDigester(),
            REFRESH_TTL
    );

    private final UUID sessionId = UUID.randomUUID();
    private final UserId userId = UserId.newId();

    private RefreshSessionService service;

    @BeforeEach
    void setUp() {
        when(accessTokens.issue(any(), any(), any())).thenReturn(
                new IssuedAccessToken("new-jwt", NOW, NOW.plusSeconds(900))
        );

        service = new RefreshSessionService(
                sessions,
                users,
                tokenFactory,
                accessTokens,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldRotateRefreshTokenAndIssueNewAccessToken() {
        givenRotation();
        User user = givenUser(UserStatus.ACTIVE);

        AuthenticatedSession renewed = service.execute("old-refresh-token");

        ArgumentCaptor<VerificationTokenDigest> next =
                ArgumentCaptor.forClass(VerificationTokenDigest.class);

        verify(sessions).rotate(
                eq(tokenFactory.digest("old-refresh-token")),
                next.capture(),
                eq(NOW),
                eq(NOW.plus(REFRESH_TTL))
        );

        assertNotEquals("old-refresh-token", renewed.refreshToken());
        assertEquals(tokenFactory.digest(renewed.refreshToken()), next.getValue());
        assertEquals("new-jwt", renewed.accessToken().value());
        verify(accessTokens).issue(user, sessionId, NOW);
    }

    @Test
    void shouldRejectUnknownOrUsedToken() {
        when(sessions.rotate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.execute("used-token")
        );
        verifyNoInteractions(users, accessTokens);
    }

    @Test
    void shouldEndSessionWhenUserIsNoLongerActive() {
        givenRotation();
        givenUser(UserStatus.BLOCKED);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.execute("old-refresh-token")
        );

        verify(sessions).revokeById(sessionId, NOW);
        verifyNoInteractions(accessTokens);
    }

    @Test
    void shouldEndSessionWhenUserNoLongerExists() {
        givenRotation();
        when(users.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.execute("old-refresh-token")
        );
        verify(sessions).revokeById(sessionId, NOW);
    }

    @Test
    void shouldRejectMalformedTokenWithoutQueryingSessions() {
        for (String token : new String[]{null, "", "   ", "x".repeat(513)}) {
            assertThrows(
                    InvalidRefreshTokenException.class,
                    () -> service.execute(token)
            );
        }

        verifyNoInteractions(sessions);
    }

    private void givenRotation() {
        when(sessions.rotate(any(), any(), any(), any())).thenReturn(
                Optional.of(new ActiveSession(sessionId, userId))
        );
    }

    private User givenUser(UserStatus status) {
        User user = User.reconstitute(
                userId,
                "Maria Silva",
                Email.of("maria@example.com"),
                Set.of(UserRole.USER),
                status,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(3600)
        );

        when(users.findById(userId)).thenReturn(Optional.of(user));

        return user;
    }
}
