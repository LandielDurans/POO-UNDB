package br.com.fuelfinder.identity.application.port.in;

import br.com.fuelfinder.identity.domain.valueobject.Email;

import java.util.Objects;

public final class ProvisionSuperAdminCommand {

    private final String name;
    private final Email email;
    private final String password;

    public ProvisionSuperAdminCommand(
            String name,
            Email email,
            String password
    ) {
        this.name = Objects.requireNonNull(name, "Nome nao pode ser nulo.");
        this.email = Objects.requireNonNull(email, "Email nao pode ser nulo.");
        this.password = Objects.requireNonNull(
                password,
                "Senha nao pode ser nula."
        );
    }

    public String name() {
        return name;
    }

    public Email email() {
        return email;
    }

    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return "ProvisionSuperAdminCommand[REDACTED]";
    }
}
