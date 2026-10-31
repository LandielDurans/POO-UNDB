package br.com.fuelfinder.identity.application.service;

import br.com.fuelfinder.identity.adapter.security.SecureRandomVerificationTokenGenerator;
import br.com.fuelfinder.identity.adapter.security.Sha256VerificationTokenDigester;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    private final SessionRepository sessions = mock(SessionRepository.class);

    private final SessionTokenFactory tokenFactory = new SessionTokenFactory(
            new SecureRandomVerificationTokenGenerator(),
            new Sha256VerificationTokenDigester(),
            Duration.ofDays(7)
    );

    private final LogoutService service = new LogoutService(
            sessions,
            tokenFactory,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldRevokeSessionByRefreshTokenDigest() {
        service.execute("refresh-token");

        verify(sessions).revoke(tokenFactory.digest("refresh-token"), NOW);
    }

    @Test
    void shouldIgnoreMalformedTokens() {
        service.execute((String) null);
        service.execute("  ");
        service.execute("x".repeat(513));

        verifyNoInteractions(sessions);
    }

    @Test
    void shouldRevokeAllSessionsOfUser() {
        UserId userId = UserId.newId();
        when(sessions.revokeAllForUser(userId, NOW)).thenReturn(3);

        assertEquals(3, service.execute(userId));
    }
}
