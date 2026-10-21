package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.application.port.out.PasswordHasher;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder =
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public PasswordHash hash(String plainPassword) {
        Objects.requireNonNull(
                plainPassword,
                "Senha nao pode ser nula."
        );

        return new PasswordHash(
                encoder.encode(plainPassword)
        );
    }

    @Override
    public boolean matches(
            String plainPassword,
            PasswordHash storedHash
    ) {
        Objects.requireNonNull(
                plainPassword,
                "Senha nao pode ser nula."
        );

        Objects.requireNonNull(
                storedHash,
                "Hash armazenado nao pode ser nulo."
        );

        return encoder.matches(
                plainPassword,
                storedHash.value()
        );
    }
}
