package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;
import java.util.UUID;

/**
 * Le a identidade do usuario autenticado a partir
 * do access token ja validado pelo Spring Security.
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static UserId idOf(Jwt jwt) {
        Objects.requireNonNull(jwt, "Token autenticado nao pode ser nulo.");

        return UserId.from(UUID.fromString(jwt.getSubject()));
    }
}
