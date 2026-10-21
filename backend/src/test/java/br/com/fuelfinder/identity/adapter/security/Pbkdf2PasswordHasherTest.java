package br.com.fuelfinder.identity.adapter.security;

import br.com.fuelfinder.identity.application.model.PasswordHash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher hasher =
            new Pbkdf2PasswordHasher();

    @Test
    void shouldHashAndVerifyPassword() {
        String password = "MinhaSenhaSegura2026!";

        PasswordHash hash = hasher.hash(password);

        assertNotEquals(password, hash.value());
        assertTrue(hasher.matches(password, hash));
        assertFalse(
                hasher.matches("SenhaIncorreta2026!", hash)
        );
    }

    @Test
    void shouldUseDifferentSalts() {
        String password = "MinhaSenhaSegura2026!";

        PasswordHash first = hasher.hash(password);
        PasswordHash second = hasher.hash(password);

        assertNotEquals(first.value(), second.value());
        assertTrue(hasher.matches(password, first));
        assertTrue(hasher.matches(password, second));
    }
}
