package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.adapter.config.IdentitySecurityConfiguration;
import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.application.port.out.SessionRepository;
import br.com.fuelfinder.identity.domain.enums.UserRole;
import br.com.fuelfinder.identity.domain.enums.UserStatus;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import javax.crypto.SecretKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JwtAccessTokenIssuerTest {

    private static final String SECRET =
            "unit-test-jwt-secret-with-at-least-32-bytes";
    private static final String ISSUER = "fuelfinder";

    private final IdentitySecurityConfiguration config =
            new IdentitySecurityConfiguration();

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final SecretKey key = config.jwtSigningKey(SECRET);

    private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(
            config.jwtEncoder(key),
            ISSUER,
            Duration.ofMinutes(15)
    );

    private final JwtDecoder decoder = config.jwtDecoder(
            key,
            ISSUER,
            sessions,
            Clock.systemUTC()
    );

    private final UUID sessionId = UUID.randomUUID();

    @Test
    void shouldIssueTokenWithIdentityRolesAndSession() {
        when(sessions.isActive(eq(sessionId), any())).thenReturn(true);
        User user = admin();
        Instant issuedAt = Instant.now();

        IssuedAccessToken token = issuer.issue(user, sessionId, issuedAt);
        Jwt jwt = decoder.decode(token.value());

        assertEquals(user.getId().value().toString(), jwt.getSubject());
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals(
                List.of("ADMIN", "USER"),
                jwt.getClaimAsStringList(JwtAccessTokenIssuer.ROLES_CLAIM)
        );
        assertEquals(
                sessionId.toString(),
                jwt.getClaimAsString(JwtAccessTokenIssuer.SESSION_CLAIM)
        );
        assertEquals(issuedAt.plus(Duration.ofMinutes(15)), token.expiresAt());

        // Dados pessoais nao vao para o JWT.
        assertNull(jwt.getClaim("email"));
        assertNull(jwt.getClaim("name"));
    }

    @Test
    void shouldRejectTokenOfEndedSession() {
        when(sessions.isActive(eq(sessionId), any())).thenReturn(false);

        IssuedAccessToken token = issuer.issue(admin(), sessionId, Instant.now());

        assertThrows(
                JwtValidationException.class,
                () -> decoder.decode(token.value())
        );
    }

    @Test
    void shouldRejectExpiredToken() {
        when(sessions.isActive(eq(sessionId), any())).thenReturn(true);

        IssuedAccessToken token = issuer.issue(
                admin(),
                sessionId,
                Instant.now().minus(Duration.ofHours(1))
        );

        assertThrows(
                JwtValidationException.class,
                () -> decoder.decode(token.value())
        );
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        SecretKey otherKey = config.jwtSigningKey(
                "another-secret-that-is-also-32-bytes-long"
        );
        JwtAccessTokenIssuer forger = new JwtAccessTokenIssuer(
                config.jwtEncoder(otherKey),
                ISSUER,
                Duration.ofMinutes(15)
        );

        IssuedAccessToken forged = forger.issue(admin(), sessionId, Instant.now());

        assertThrows(BadJwtException.class, () -> decoder.decode(forged.value()));
        verifyNoInteractions(sessions);
    }

    @Test
    void shouldRequireStrongSecret() {
        assertThrows(
                IllegalStateException.class,
                () -> config.jwtSigningKey("short-secret")
        );
    }

    private static User admin() {
        return User.reconstitute(
                UserId.newId(),
                "Admin",
                Email.of("admin@example.com"),
                Set.of(UserRole.USER, UserRole.ADMIN),
                UserStatus.ACTIVE,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
