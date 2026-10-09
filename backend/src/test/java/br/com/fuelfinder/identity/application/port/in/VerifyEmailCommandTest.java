package br.com.fuelfinder.identity.application.port.in;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyEmailCommandTest {

    @Test
    void shouldCreateValidCommand() {
        String token = "example-opaque-token";

        VerifyEmailCommand command =
                new VerifyEmailCommand(token);

        assertEquals(token, command.token());
    }

    @Test
    void shouldRejectNullToken() {
        assertThrows(
                NullPointerException.class,
                () -> new VerifyEmailCommand(null)
        );
    }

    @Test
    void shouldRejectBlankToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerifyEmailCommand("   ")
        );
    }

    @Test
    void shouldRejectExcessivelyLongToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerifyEmailCommand(
                        "a".repeat(513)
                )
        );
    }

    @Test
    void shouldNotExposeTokenInToString() {
        String token = "sensitive-example-token";

        VerifyEmailCommand command =
                new VerifyEmailCommand(token);

        assertFalse(
                command.toString().contains(token)
        );

        assertEquals(
                "VerifyEmailCommand[REDACTED]",
                command.toString()
        );
    }
}
