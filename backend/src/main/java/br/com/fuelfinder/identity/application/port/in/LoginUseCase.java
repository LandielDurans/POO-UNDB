package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.application.model.AuthenticatedSession;

public interface LoginUseCase {

    AuthenticatedSession execute(LoginCommand command);
}
