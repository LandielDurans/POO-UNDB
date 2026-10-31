package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.domain.valueobject.UserId;

public interface LogoutAllSessionsUseCase {

    /**
     * Encerra todas as sessoes do usuario.
     *
     * @return quantidade de sessoes encerradas
     */
    int execute(UserId userId);
}
