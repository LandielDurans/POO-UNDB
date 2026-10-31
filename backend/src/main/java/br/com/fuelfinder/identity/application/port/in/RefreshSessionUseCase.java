package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.application.model.AuthenticatedSession;

public interface RefreshSessionUseCase {

    /**
     * Troca o refresh token por um novo par de tokens.
     * O token apresentado deixa de valer.
     */
    AuthenticatedSession execute(String refreshToken);
}
