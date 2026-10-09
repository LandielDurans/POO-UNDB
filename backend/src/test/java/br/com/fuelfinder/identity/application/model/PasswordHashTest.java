package br.com.fuelfinder.identity.application.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHashTest {

    @Test
    void shouldPreserveProvidedHashValue() {
        String encoded = "encoded-hash-test-only";

        PasswordHash passwordHash = new PasswordHash(encoded);

        assertEquals(encoded, passwordHash.value());
    }

    @Test
    void shouldRejectNullHash() {
        assertThrows(
                NullPointerException.class,
                () -> new PasswordHash(null)
        );
    }

    @Test
    void shouldRejectBlankHash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordHash("   ")
        );
    }

    @Test
    void shouldNotExposeHashInToString() {
        String encoded = "encoded-hash-test-only";

        PasswordHash passwordHash = new PasswordHash(encoded);

        assertFalse(
                passwordHash.toString().contains(encoded)
        );

        assertEquals(
                "PasswordHash[REDACTED]",
                passwordHash.toString()
        );
    }
}
