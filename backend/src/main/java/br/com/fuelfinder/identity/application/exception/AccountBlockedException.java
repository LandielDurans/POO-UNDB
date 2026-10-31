package br.com.fuelfinder.identity.application.exception;

public final class AccountBlockedException
        extends RuntimeException {

    public AccountBlockedException() {
        super("Conta bloqueada.");
    }
}
