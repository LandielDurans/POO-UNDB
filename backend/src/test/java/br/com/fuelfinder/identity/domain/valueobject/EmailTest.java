package br.com.fuelfinder.identity.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = Email.of("user@example.com");

        assertEquals("user@example.com", email.value());
    }

    @Test
    void shouldNormalizeEmail() {
        Email email = Email.of("    user@example.com");

        assertEquals("user@example.com", email.value());
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Email.of("email invalido")
        );
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Email.of("   ")
        );
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(
                NullPointerException.class,
                () -> Email.of(null)
        );
    }
}