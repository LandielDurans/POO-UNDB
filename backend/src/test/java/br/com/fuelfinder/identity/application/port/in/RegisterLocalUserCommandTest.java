package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.domain.valueobject.Email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterLocalUserCommandTest {

    @Test
    void shouldCreateValidCommand() {
        Email email = Email.of("maria@example.com");

        RegisterLocalUserCommand command =
                new RegisterLocalUserCommand(
                        "Maria Silva",
                        email,
                        "SenhaSegura123!"
                );

        assertEquals("Maria Silva", command.name());
        assertEquals(email, command.email());
        assertEquals("SenhaSegura123!", command.plainPassword());
    }

    @Test
    void shouldRejectNullName() {
        assertThrows(
                NullPointerException.class,
                () -> new RegisterLocalUserCommand(
                        null,
                        Email.of("maria@example.com"),
                        "SenhaSegura123!"
                )
        );
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(
                NullPointerException.class,
                () -> new RegisterLocalUserCommand(
                        "Maria Silva",
                        null,
                        "SenhaSegura123!"
                )
        );
    }

    @Test
    void shouldRejectNullPassword() {
        assertThrows(
                NullPointerException.class,
                () -> new RegisterLocalUserCommand(
                        "Maria Silva",
                        Email.of("maria@example.com"),
                        null
                )
        );
    }

    @Test
    void shouldRejectBlankPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegisterLocalUserCommand(
                        "Maria Silva",
                        Email.of("maria@example.com"),
                        "   "
                )
        );
    }

    @Test
    void shouldNotExposeSensitiveDataInToString() {
        RegisterLocalUserCommand command =
                new RegisterLocalUserCommand(
                        "Maria Silva",
                        Email.of("maria@example.com"),
                        "SenhaSegura123!"
                );

        assertFalse(
                command.toString().contains("SenhaSegura123!")
        );

        assertFalse(
                command.toString().contains("maria@example.com")
        );
    }
}
