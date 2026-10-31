package br.com.fuelfinder.identity.application.service;

import java.util.Objects;

public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    public void validate(String plainPassword) {
        Objects.requireNonNull(
                plainPassword,
                "Senha nao pode ser nula"
        );

        if (plainPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Senha nao pode ser vazia"
            );
        }

        int length = plainPassword.codePointCount(
                0,
                plainPassword.length()
        );

        if (length < MIN_LENGTH || length > MAX_LENGTH){
            throw new IllegalArgumentException(
                    "Senha deve conter entre 8 e 128 caracteres"
            );
        }
    }
}
