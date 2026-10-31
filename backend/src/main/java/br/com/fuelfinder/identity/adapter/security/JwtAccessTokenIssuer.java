package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.application.port.out.AccessTokenIssuer;
import br.com.fuelfinder.identity.domain.model.User;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class JwtAccessTokenIssuer implements AccessTokenIssuer {

    public static final String ROLES_CLAIM = "roles";
    public static final String SESSION_CLAIM = "sid";

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;

    public JwtAccessTokenIssuer(
            JwtEncoder encoder,
            String issuer,
            Duration ttl
    ) {
        this.encoder = Objects.requireNonNull(encoder);
        this.issuer = Objects.requireNonNull(issuer);
        this.ttl = Objects.requireNonNull(ttl);

        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException(
                    "Validade do access token deve ser positiva."
            );
        }
    }

    @Override
    public IssuedAccessToken issue(
            User user,
            UUID sessionId,
            Instant issuedAt
    ) {
        Objects.requireNonNull(user, "Usuario nao pode ser nulo.");
        Objects.requireNonNull(sessionId, "SessionId nao pode ser nulo.");
        Objects.requireNonNull(issuedAt, "Data de emissao nao pode ser nula.");

        Instant expiresAt = issuedAt.plus(ttl);

        // Somente identificadores e papeis: nada de nome
        // ou e-mail, pois o JWT e legivel pelo cliente.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().value().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(
                        ROLES_CLAIM,
                        user.getRoles().stream()
                                .map(Enum::name)
                                .sorted()
                                .toList()
                )
                .claim(SESSION_CLAIM, sessionId.toString())
                .build();

        String token = encoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        return new IssuedAccessToken(token, issuedAt, expiresAt);
    }
}
