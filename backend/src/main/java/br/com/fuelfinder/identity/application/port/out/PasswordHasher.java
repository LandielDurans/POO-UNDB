package br.com.fuelfinder.identity.application.port.out;

import br.com.fuelfinder.identity.application.model.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String plainPassword);

    boolean matches(
            String plainPassword,
            PasswordHash storedHash
    );
}
