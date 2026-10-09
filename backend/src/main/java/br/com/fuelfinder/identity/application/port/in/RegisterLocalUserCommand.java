package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.domain.valueobject.Email;

import java.util.Objects;

public class RegisterLocalUserCommand {

    private final String name;
    private final Email email;
    private final String plainPassword;

    public RegisterLocalUserCommand(
            String name,
            Email email,
            String plainPassword
    ) {
        this.name = Objects.requireNonNull(
                name, "Nome nao pode ser nulo."
        );

        this.email = Objects.requireNonNull(
                email, "Email nao pode ser nulo."
        );

        this.plainPassword = Objects.requireNonNull(
                plainPassword, "Senha nao pode ser nula."
        );

        if (plainPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Senha nao pode ficar vazia."
            );
        }
    }

    public String name() {
        return name;
    }

    public Email email() {
        return email;
    }

    public String plainPassword() {
        return plainPassword;
    }

    @Override
    public String toString() {
        return "RegisterLocalUserCommand[REDACTED]";
    }
}