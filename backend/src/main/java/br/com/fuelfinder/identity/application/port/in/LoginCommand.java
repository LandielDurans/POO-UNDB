package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.domain.valueobject.Email;

import java.util.Objects;

public final class LoginCommand {

    // Limita o custo do hash em requisicoes abusivas.
    private static final int MAX_PASSWORD_LENGTH = 512;

    private final Email email;
    private final String password;

    public LoginCommand(Email email, String password) {
        this.email = Objects.requireNonNull(
                email,
                "Email nao pode ser nulo."
        );

        this.password = Objects.requireNonNull(
                password,
                "Senha nao pode ser nula."
        );

        if (password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Senha nao pode ser vazia."
            );
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Senha excede o tamanho permitido."
            );
        }
    }

    public Email email() {
        return email;
    }

    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return "LoginCommand[REDACTED]";
    }
}
