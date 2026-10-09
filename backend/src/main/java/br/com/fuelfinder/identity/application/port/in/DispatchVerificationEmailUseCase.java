package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.domain.valueobject.UserId;

public interface DispatchVerificationEmailUseCase {

    /***
     * @return true se o envio for solicitado
     *          false se nao havia email a preparar.
     */
    boolean execute(UserId userId);
}
