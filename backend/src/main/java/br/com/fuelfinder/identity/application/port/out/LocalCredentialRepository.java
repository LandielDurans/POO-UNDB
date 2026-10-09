package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import java.util.Optional;

public interface LocalCredentialRepository {

    void save(
            UserId userId,
            PasswordHash passwordHash
    );

    Optional<PasswordHash> findPasswordHashByUserId(
            UserId userId
    );
}
