package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public interface AccessTokenIssuer {

    /**
     * Emite um access token de curta duracao com a
     * identidade e os papeis atuais do usuario.
     */
    IssuedAccessToken issue(User user, UUID sessionId, Instant issuedAt);
}
