package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.domain.valueobject.UserId;

public interface RegisterLocalUserUseCase {

    UserId execute(RegisterLocalUserCommand command);
}