package br.com.fuelfinder.identity.application.exception;

public final class InvalidCredentialsException
        extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha invalidos.");
    }
}
