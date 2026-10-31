package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.application.model.UserProfile;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Optional;

public interface GetUserProfileUseCase {

    Optional<UserProfile> execute(UserId userId);
}
