package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Optional;

public interface PrepareEmailVerificationUseCase {

    Optional<PreparedVerificationEmail> execute(UserId userId);
}
