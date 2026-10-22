package br.com.fuelfinder.identity.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void shouldAcceptMinimumLengthPassword() {
        assertDoesNotThrow(
                () -> policy.validate("a".repeat(8))
        );
    }

    @Test
    void shouldAcceptMaximumLengthPassword() {
        assertDoesNotThrow(
                () -> policy.validate("a".repeat(128))
        );
    }

    @Test
    void shouldRejectShortPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.validate("a".repeat(7))
        );
    }

    @Test
    void shouldRejectExcessivelyLongPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.validate("a".repeat(129))
        );
    }

    @Test
    void shouldRejectNullPassword() {
        assertThrows(
                NullPointerException.class,
                () -> policy.validate(null)
        );
    }

    @Test
    void shouldRejectBlankPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.validate(" ".repeat(20))
        );
    }

    @Test
    void shouldAcceptPassphraseWithSpaces() {
        assertDoesNotThrow(
                () -> policy.validate(
                        "uma frase secreta grande"
                )
        );
    }

    @Test
    void shouldCountUnicodeCodePoints() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.validate("🔐".repeat(7))
        );

        assertDoesNotThrow(
                () -> policy.validate("🔐".repeat(8))
        );
    }
}
