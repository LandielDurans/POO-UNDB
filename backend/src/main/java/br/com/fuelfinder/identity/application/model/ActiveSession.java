package br.com.fuelfinder.identity.application.model;

import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Objects;
import java.util.UUID;

public record ActiveSession(UUID sessionId, UserId userId) {

    public ActiveSession {
        Objects.requireNonNull(sessionId, "SessionId nao pode ser nulo.");
        Objects.requireNonNull(userId, "UserId nao pode ser nulo.");
    }
}
